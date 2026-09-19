# ADR 023: app update distribution

**Status:** Accepted  
**Date:** 2026-09-19

## Context

WakeMyWay currently has two distribution phases that must coexist cleanly:

1. founder/trusted-device builds installed directly as APKs;
2. Google Play distribution, beginning with Internal Testing and later broader release.

WakeMyWay is also an alarm product. Update convenience may never weaken the Alarm Kernel, become a Wake Ready dependency, or restart the process at an unsafe point near an active wake.

A direct APK build also needs Android's package-install permission, while a Play-distributed build must use Google Play's supported in-app update path and must not ship a self-update permission that conflicts with Play policy.

## Decision

### One product identity, two distribution flavors

The Android application keeps one package identity:

```text
com.wakemyway.app
```

and builds two distribution flavors:

- `direct` for trusted APK distribution;
- `play` for Google Play.

The direct-only manifest owns `INTERNET`, `REQUEST_INSTALL_PACKAGES` and the narrow FileProvider used for downloaded APK installation. The Play flavor does not inherit those direct-install capabilities.

### Shared update product layer

Both flavors use one application-level update contract and state model. Distribution-specific providers stay behind that seam:

```text
UpdateCoordinator
├── DirectUpdateProvider
└── PlayUpdateProvider
```

This seam exists because the external installer/provider behavior, permissions, recovery states and failure domains are genuinely different. It is not a speculative cross-platform abstraction.

### Direct distribution

Direct builds check a small `update.json` asset attached to the latest GitHub Release.

A direct update is accepted only after all of the following pass:

1. HTTPS metadata/download;
2. manifest schema/channel validation;
3. monotonically newer `versionCode`;
4. SHA-256 APK integrity check;
5. package name equals `com.wakemyway.app`;
6. APK version matches metadata;
7. downloaded APK signing identity intersects the installed app's signing lineage.

The APK is exposed only through a cache-scoped FileProvider URI to Android's package installer. WakeMyWay never silently installs an APK.

### Google Play distribution

Play builds use Google's In-App Updates library and default to a flexible update flow.

Download may proceed while the user continues using the app. Final completion/restart remains a separate WakeMyWay action so the wake-safety policy can run immediately before restart.

Immediate updates are intentionally not the default, including for high-priority releases. A later change may introduce them only with explicit evidence that the wake reliability tradeoff is acceptable.

### Wake-safety gate

Update installation/restart is blocked when:

- an Active Wake Execution exists; or
- the next Wake Occurrence is less than 90 minutes away.

Downloading/checking does not become alarm authority and is skipped automatically while an active wake exists.

When the safety condition clears, the update remains ready for user confirmation rather than installing itself silently.

### Post-update recovery

Existing `MY_PACKAGE_REPLACED` handling in `AlarmReconcileReceiver` remains the authoritative post-package-replacement hook. The update subsystem does not reimplement scheduling recovery.

### Versioning

`apps/android/version.properties` is the canonical Android app version source.

`VERSION_CODE` is strictly monotonic. `VERSION_NAME` is the consumer-facing semantic version. `UPDATE_PRIORITY` is release metadata on a 0-5 scale and does not override the wake-safety gate.

### Signing

Debug signing is not a production identity.

Direct production APKs use a stable WakeMyWay app-signing key. Google Play uses a separate upload key for AAB submission. When Play App Signing is provisioned, its app-signing identity must be chosen to preserve the intended direct-to-Play upgrade path.

Signing key material is never committed. The release workflow consumes encrypted repository secrets only.

The current debug-installed founder build may require one intentional uninstall/reinstall when the stable production signing identity is introduced. After that transition, version-compatible releases signed by the stable identity update in place.

### Release automation

Android release CI runs only for `v*` tags.

It:

- validates the tag against `version.properties`;
- runs the focused release quality gate;
- builds unsigned direct APK and Play AAB variants;
- signs the direct APK and Play AAB with their distinct identities;
- verifies signatures;
- calculates the direct APK SHA-256;
- generates `update.json`;
- publishes all three assets to a GitHub Release.

This keeps normal PR CI inexpensive and avoids adding a backend merely for update metadata.

## Consequences

Positive:

- one coherent update UX across current dogfood and future Play distribution;
- no update-network dependency in the critical wake path;
- Play builds do not contain direct-install permission;
- direct updates are cryptographically bound to WakeMyWay's installed identity;
- no Vercel/Supabase service is required for distribution metadata;
- release work runs only when a release tag is created.

Costs:

- two Android distribution variants must compile in CI;
- stable signing identities must be provisioned and protected before the first production direct release;
- the existing debug-signed installation cannot necessarily upgrade directly into the production signing identity;
- direct distribution still requires explicit Android user consent for unknown-source installation.

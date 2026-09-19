# ADR 024: package updates preserve durable WakeMyWay state

**Status:** Accepted  
**Date:** 2026-09-19

## Context

WakeMyWay supports direct in-app APK updates and Google Play updates. Android package replacement normally preserves an app's private data when the package id and signing identity remain unchanged, but alarm reliability cannot rely on that platform fact alone without a product-level compatibility contract.

WakeMyWay stores different classes of durable state in different Android storage domains:

- consumer alarm definitions in credential-protected app files;
- consumer preferences in credential-protected app files;
- Wake history in credential-protected app files;
- Wake Learning in app no-backup storage;
- Alarm Kernel critical state in device-protected no-backup storage.

An app update must preserve all of those files. Schema evolution must also remain backward-readable, otherwise the bytes may survive while the new app can no longer interpret them.

## Decision

### Stable package and signing identity

All direct and Play updates retain:

```text
applicationId = com.wakemyway.app
```

and must use the established WakeMyWay app-signing identity.

A normal update is always package replacement. Release/install automation must never uninstall or clear the app as part of an update.

### Product truth survives package replacement

Saved AlarmDefinitions are the durable consumer source of truth. An in-place update must preserve them byte-for-byte unless an explicit schema migration rewrites them atomically.

Consumer preferences and Wake history follow the same rule.

Alarm Kernel Direct Boot state also survives package replacement. `MY_PACKAGE_REPLACED` remains the post-update reconciliation hook that repairs Android registrations from durable critical state.

A safety policy may disable critical Android registration when required platform presentation access is missing. That does not authorize deletion of the consumer AlarmDefinition. Product truth remains repairable.

### Schema compatibility

Durable schemas are append/migrate contracts, not disposable implementation details.

When a schema version changes:

1. the writer may advance the current version;
2. decoders for supported historical versions remain present;
3. migration is deterministic and local;
4. writes remain atomic;
5. no migration may silently replace an unreadable alarm store with an empty one.

AlarmDefinition schema-v1 is now explicitly retained as a permanent decoder branch. Consumer preferences use the same versioned-decoder pattern. Wake History and Critical Wake state already support multiple historical schema versions.

### Executable upgrade contract

A dedicated Android CI lane proves package-update persistence with two APKs signed by the same debug identity:

```text
baseline APK
  -> seed real WakeMyWay durable state
  -> adb install -r candidate APK
  -> verify firstInstallTime did not change
  -> verify alarm/preferences/history/critical state in candidate
```

The test uses a real API 36 emulator and never calls uninstall or pm clear between seed and verification.

To contain CI cost, this lane runs only when persistence, alarm, update, manifest, versioning or wake-core files change, plus manual dispatch.

## Consequences

Positive:

- future APK/Play updates are protected by an executable state-preservation contract;
- an accidental package id/signing/install-flow regression is caught before release;
- alarm schema changes cannot silently assume old files disappear;
- product AlarmDefinitions remain distinct from Android scheduling state;
- update testing now exercises the same package-replacement primitive used by the direct updater.

Costs:

- relevant persistence/alarm PRs run one additional emulator lane and build a baseline APK;
- future schema changes must implement and test migrations rather than resetting local state;
- signing-key loss remains unrecoverable for direct in-place updates, so the production signing backup is critical.

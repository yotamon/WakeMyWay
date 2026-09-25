# Android release operations

**Status:** Canonical production release procedure  
**Workflow:** `Release WakeMyWay`  
**Last updated:** 2026-09-25

## Product rule

Merging a pull request must never publish a WakeMyWay version.

The normal development flow is:

```text
feature / fix PR
      ↓
CI
      ↓
merge to main
      ↓
no release
```

A production release happens only when a human explicitly starts the manual GitHub Actions workflow:

```text
Actions
  ↓
Release WakeMyWay
  ↓
patch / minor / major
  ↓
Run workflow
```

## One-click release contract

The manual workflow owns the complete production release transaction.

It:

1. requires the selected ref to be the current `main`;
2. reads the latest stable GitHub Release and its `update.json`;
3. calculates the next semantic version from `patch`, `minor`, or `major`;
4. increments `versionCode` monotonically;
5. generates release notes from merged PRs since the previous release unless an override was supplied;
6. runs the release-planning unit tests;
7. runs Wake core/app unit tests and Android lint;
8. runs API-36 device reliability;
9. runs upgrade-persistence from the previous released tag to the selected `main`;
10. builds Direct APK and Play AAB with release-only version overrides;
11. signs Direct and Play artifacts with the stable production signing identities stored in GitHub Actions Secrets;
12. verifies package name, version, non-debuggable state, Direct certificate, AAB signature, and artifact size;
13. creates `update.json` from the verified signed APK;
14. uploads a signed release candidate as a temporary Actions artifact;
15. creates the git tag and a **draft** GitHub Release only after all earlier gates are green;
16. re-downloads the draft assets and verifies their SHA/version;
17. publishes the verified draft as the latest release;
18. re-downloads the public latest manifest/APK and verifies SHA, version, package, certificate, and non-debuggable state.

No tag or public release is created before the release candidate is completely built, signed, and verified.

## Inputs

### Release type

- `patch`: `0.2.4 → 0.2.5`
- `minor`: `0.2.4 → 0.3.0`
- `major`: `0.2.4 → 1.0.0`

`versionCode` is independent from semantic versioning and always increments by one from the latest published `update.json`.

### Release notes

The default is automatic GitHub release notes generated from merged PRs between the previous release tag and the selected `main`.

An optional manual release-note override is available for unusually user-facing releases.

### Dry run

`dry_run=true` executes planning, tests, device gates, build, production signing, metadata verification, and candidate upload but creates **no tag and no GitHub Release**.

Use it when changing the release system itself or when explicitly validating production signing.

Normal releases leave `dry_run` disabled.

## Version authority

`apps/android/version.properties` is a **local/default developer fallback**, not production version authority.

Production release builds receive:

```text
WMW_VERSION_CODE
WMW_VERSION_NAME
```

as Gradle/environment overrides calculated by the release workflow.

The authoritative published version is the latest stable GitHub Release plus its `update.json`.

This avoids release-only version PRs and allows many product PRs to merge before one intentional release.

## Signing

Two signing roles remain separate:

- Direct APK application signing identity;
- Google Play upload signing identity.

Signing material is never committed.

Required GitHub Actions Secrets:

```text
WMW_DIRECT_KEYSTORE_B64
WMW_DIRECT_KEY_ALIAS
WMW_DIRECT_STORE_PASSWORD
WMW_DIRECT_KEY_PASSWORD

WMW_PLAY_UPLOAD_KEYSTORE_B64
WMW_PLAY_UPLOAD_KEY_ALIAS
WMW_PLAY_UPLOAD_STORE_PASSWORD
WMW_PLAY_UPLOAD_KEY_PASSWORD
```

The Direct APK must verify against the pinned public production certificate SHA-256 before publication.

The local DPAPI signing bundle remains an offline recovery path, not the normal release mechanism.

## Failure semantics

### Before draft creation

Any failure means nothing is published.

No tag and no GitHub Release are created.

### Draft creation or draft verification fails

The release must remain non-public. An unpublished draft/tag may be cleaned automatically; it must never become the latest public update.

### Public verification fails

Do not silently delete a release that may already have been observed by clients.

The workflow fails visibly and the release is treated as an incident requiring explicit operator action.

### main changes while release is validating

The workflow fails before publication.

Run it again so releasing an older `main` state is always intentional rather than accidental.

## Direct self-update contract

A successful release must expose:

```text
/releases/latest/download/update.json
/releases/latest/download/WakeMyWay-direct.apk
```

The manifest SHA must match the public APK.

The Direct app independently verifies downloaded SHA, package identity, and signing certificate before offering install.

## Play artifact

The one-click workflow produces and signs `WakeMyWay-play.aab`, but it does not automatically roll the bundle out in Google Play.

Play track/staged rollout remains a separate launch operation under `38-play-launch-operations.md`.

## Operator checklist

For a normal patch release:

1. Confirm the desired PRs are merged to `main`.
2. Open **Actions → Release WakeMyWay**.
3. Leave branch as `main`.
4. Choose **patch**.
5. Leave release notes empty for automatic notes.
6. Leave **dry run** off.
7. Click **Run workflow**.
8. Wait for the workflow to report the public endpoint as verified.

That is the complete normal release procedure.

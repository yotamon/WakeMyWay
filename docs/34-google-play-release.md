# Google Play release readiness

This document is the operational source of truth for shipping the Android app through Google Play.

## Current release boundary

The current production Android variant is intentionally local-first:

- application ID: `com.wakemyway.app`
- minimum SDK: 29
- target SDK: 36
- compile SDK: 37
- default version: `0.1.0` / version code `1`
- release variant has no `INTERNET` permission
- the experimental realtime/OpenAI transport remains debug-only
- critical alarm delivery remains local and must not depend on network availability
- raw morning microphone audio is not stored by Wake My Way

The release build uses R8 code shrinking and resource shrinking. CI builds an unsigned release AAB on Android changes so release-only shrinker/lint failures are caught before a store build is attempted.

## Production bundle flow

```text
manual GitHub Actions release
        |
        v
version name + version code
        |
        v
unit tests + release lint
        |
        v
R8/resource shrinking
        |
        v
release keystore from GitHub secrets
        |
        v
signed app-release.aab
        |
        v
signature verification + SHA-256
        |
        v
GitHub Actions artifact
        |
        v
Google Play Console
```

The release workflow is `.github/workflows/android-release.yml`.

## Signing model

Never commit the release keystore or any password to the repository.

The Gradle release configuration reads these environment variables when all four are present:

- `WMW_RELEASE_STORE_FILE`
- `WMW_RELEASE_STORE_PASSWORD`
- `WMW_RELEASE_KEY_ALIAS`
- `WMW_RELEASE_KEY_PASSWORD`

The GitHub Actions release workflow additionally expects the keystore itself as a base64-encoded GitHub Actions secret:

- `WMW_RELEASE_KEYSTORE_BASE64`

Create the upload key locally and keep an offline backup in a password manager / encrypted backup location. Google Play App Signing should hold the app-signing key; this repository workflow should use the Play upload key.

Example local key creation:

```bash
keytool -genkeypair \
  -v \
  -keystore wake-my-way-upload.jks \
  -alias wakemyway-upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Example base64 conversion on Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("wake-my-way-upload.jks"))
```

Do not paste the resulting base64 value into issues, pull requests, docs, chat logs, or source files. Store it only in the GitHub Actions secret.

## Versioning

The checked-in defaults remain suitable for local/CI verification:

```text
versionName = 0.1.0
versionCode = 1
```

Production release values are supplied by the manual release workflow through:

- `WMW_VERSION_NAME`
- `WMW_VERSION_CODE`

Rules:

1. Every Play upload must have a version code greater than every version code previously uploaded for the app.
2. Version names are user-facing and may follow semantic versioning.
3. Never reuse a version code after a Play upload, even if that release is discarded.

## Current permission inventory

The production manifest requests:

| Permission | Product reason | Store/compliance note |
|---|---|---|
| `USE_EXACT_ALARM` | alarm must fire at a user-selected exact time | Core alarm functionality; complete the Play declaration accurately |
| `USE_FULL_SCREEN_INTENT` | present the active alarm over the lock screen | Core alarm functionality; complete the Play declaration accurately |
| `RECEIVE_BOOT_COMPLETED` | restore/reconcile scheduled alarms after reboot | Required for alarm reliability |
| `POST_NOTIFICATIONS` | foreground alarm notification / user-visible state | Runtime permission on modern Android |
| `FOREGROUND_SERVICE` | active wake execution | Required for foreground service use |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | alarm audio playback | Match service type and actual use |
| `RECORD_AUDIO` | local conversational wake interaction | Explain clearly in onboarding and privacy policy |

The release app currently does **not** request `INTERNET`. Debug-only realtime experimentation does.

## Data Safety draft for the first local-first release

This is an engineering draft, not a substitute for reviewing the exact wording in the Play Console at submission time.

### Developer-side collection

For the current release boundary:

- Wake schedule and Tomorrow Contract content are stored locally on the device.
- Wake history/derived local behavior state is local unless a future cloud feature explicitly changes that boundary.
- Wake My Way does not intentionally upload raw microphone recordings.
- The production app has no `INTERNET` permission, so the app process cannot directly transmit application data to Wake My Way servers in this release.
- No analytics or crash SDK is currently part of the production Android dependency set.

### Speech recognition caveat

Wake My Way invokes the Android speech-recognition service for local voice interaction. The recognition implementation is supplied by the user's device. Depending on the installed recognition provider and the user's device settings, that provider may process speech on-device or through its own service. Wake My Way should disclose this without claiming control over the device provider's processing policy.

Before Play submission, verify the Data Safety answers against the exact shipping binary and current Google definitions.

## Privacy/reliability hardening already applied

The production application manifest now:

- disables Android application backup for Wake My Way private local state
- disallows cleartext traffic by default
- keeps non-launcher activities/services/receivers non-exported unless Android requires otherwise
- keeps experimental network configuration isolated to the debug source set

R8 keeps WorkManager worker identities stable so persisted work remains recreatable across process restarts/updates.

## Play Console declarations to prepare

Before production submission, complete and review:

1. App access / account requirement: base use currently requires no account.
2. Ads declaration: no ads unless product scope changes.
3. Data Safety: use the shipping binary, not roadmap/future cloud plans.
4. Content rating questionnaire.
5. Target audience / children declaration.
6. Exact alarm permission declaration.
7. Full-screen intent declaration.
8. Foreground service declarations where requested by Play Console.
9. Privacy policy URL.
10. Store listing contact/support details.

## Store listing draft

### App name

**Wake My Way**

### Short description

**An alarm that learns what helps you actually get up.**

### Positioning

Wake My Way is a personal wake-up system built around reliable alarms, conversational guidance, movement, and learning what works for each morning. The alarm remains reliable offline; personalization sits on top of the alarm rather than replacing it.

Do not make absolute claims such as "never miss an alarm" or imply delivery through Android Force Stop, device power-off, uninstall, or revoked alarm permissions.

## Remaining release blockers

These are intentionally not papered over by build automation:

- [ ] Google Play Developer account created and identity verification complete.
- [ ] Google Play app entry created for `com.wakemyway.app`.
- [ ] Play App Signing enabled and upload-key ownership/backup procedure confirmed.
- [ ] Upload keystore created locally.
- [ ] Four signing secrets configured in GitHub Actions.
- [ ] Final production launcher/adaptive icon added to the Android app; current brand exploration is not yet final production artwork.
- [ ] Play Store 512x512 icon prepared.
- [ ] 1024x500 feature graphic prepared.
- [ ] Phone screenshots captured from the production/release experience.
- [ ] Public privacy-policy URL deployed.
- [ ] Support/contact URL or page deployed.
- [ ] Data Safety form reviewed against the exact release binary.
- [ ] Exact alarm and full-screen intent declarations completed.
- [ ] Internal testing release uploaded through Play Console.
- [ ] Physical-device alarm matrix passed with the Play-installed build.
- [ ] Closed-testing requirement completed if the developer account is subject to it.

## Required physical release QA

The Play-installed build must be tested for at least:

- locked screen
- screen off
- silent/vibrate mode
- Do Not Disturb behavior within the permitted product envelope
- battery saver
- Doze / long idle
- app process killed normally
- reboot before alarm
- reboot while locked / Direct Boot path
- timezone change
- manual clock change
- app update with an existing scheduled alarm
- notification permission denied
- exact alarm capability denied/revoked where Android permits it
- full-screen intent capability unavailable
- microphone permission denied
- speech recognizer unavailable
- Text-to-Speech unavailable
- no network connectivity
- Bluetooth connected/disconnected
- repeated snooze/dismiss flows

An alarm app should not move from internal testing to production based only on emulator/CI evidence.

## Release procedure

1. Merge only after normal Android CI passes, including `lintRelease` and `bundleRelease`.
2. Run **Android Play Release Bundle** manually in GitHub Actions.
3. Enter the intended public version name and a new monotonically increasing version code.
4. Download the generated artifact privately.
5. Verify the included SHA-256 file against the AAB before upload.
6. Upload the AAB to Play Console internal testing first.
7. Install from Google Play on physical devices and execute the release QA matrix.
8. Promote the same tested artifact through the chosen Play tracks rather than rebuilding an untested binary.

## Important release invariant

The exact `.aab` that passes final physical-device testing should be the artifact promoted toward production. Do not rebuild "the same version" after QA and assume it is identical.

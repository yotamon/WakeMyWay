# Wake My Way Android

This directory is the Android-first implementation workspace.

## Toolchain

- JDK 17
- Gradle 9.6.1
- Android Gradle Plugin 9.4.0
- Kotlin 2.4.20
- `compileSdk = 37`
- `targetSdk = 36` until Android 17 runtime behavior is intentionally adopted
- `minSdk = 29`

AGP 9 built-in Kotlin is used for Android modules. Do not add `org.jetbrains.kotlin.android`.

## Modules

- `:app` — Android composition root and UI
- `:wake-core` — pure Kotlin domain behavior
- `:benchmark` — macrobenchmark host

## Local validation

Until the standard Gradle wrapper JAR is committed, use Gradle 9.6.1 directly:

```bash
gradle :wake-core:test :app:testDirectDebugUnitTest :app:lintDirectDebug :app:assembleDirectDebug
```

CI pins Gradle 9.6.1 explicitly. The standard wrapper JAR is tracked as a small M0 follow-up and must be added before M0 closes.

## Branded wake-sound assets

The consumer wake-sound catalog is intentionally all-or-nothing. A checkout may contain none of the approved branded WAVs, in which case critical execution uses the emergency fallback. A partial or checksum-drifted branded bundle is invalid and fails Android CI.

The approved source filenames are exactly:

```text
Morning Light.wav
Soft Start.wav
Morning Pulse.wav
```

Put all three files in one local directory, then run the importer from `apps/android`:

```bash
python scripts/wake_sound_assets.py --import "C:\path\to\approved-sounds"
```

On macOS/Linux the same command works with a normal POSIX path. The importer validates the RIFF/WAVE structure before mutating Android resources, copies the assets to:

```text
app/src/main/res/raw/morning_light.wav
app/src/main/res/raw/soft_start.wav
app/src/main/res/raw/morning_pulse.wav
```

and writes `app/src/main/wake-sound-assets.json` with SHA-256 checksums and audio metadata. Verify the repository state at any time with:

```bash
python scripts/wake_sound_assets.py --check
```

Do not manually commit one or two branded files, rename a different track into one of these resource names, or edit the generated checksum manifest by hand. The Alarm Editor sound picker remains hidden until the exact approved bundle is committed and real preview/on-device playback validation is complete.

## Visual regression

`ProductVisualRegressionTest` renders the curated product states at the canonical 393 × 852 viewport. The approved visual contract is stored as exact SHA-256 hashes in:

```text
app/src/test/visual-goldens.sha256
```

CI regenerates the PNG renders with Roborazzi and fails if any canonical render differs from that manifest. The generated PNGs and Roborazzi diagnostics are uploaded as workflow artifacts for visual review; they are intentionally not committed to the repository.

Do not update the manifest merely to make CI green. A visual hash change is an explicit design acceptance decision and should be made only after reviewing the rendered screens.

## Distribution variants

WakeMyWay has two Android distribution flavors with the same application id:

- `direct` — trusted APK distribution with verified in-app APK download/install support;
- `play` — Google Play distribution using Play In-App Updates.

Normal local/CI development uses `directDebug`. Play compilation is also validated in CI so provider drift is caught before release.

The canonical app version is stored in:

```text
version.properties
```

A production release tag must match `VERSION_NAME`, for example `v0.2.0`.

### Release signing

The tag-only Android Release workflow expects these encrypted GitHub Actions secrets:

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

The direct key is the stable WakeMyWay app-signing identity. The Play upload key is deliberately separate. Never commit either keystore or any password.

A release publishes:

```text
WakeMyWay-direct.apk
WakeMyWay-play.aab
update.json
```

The direct app reads the latest `update.json`, verifies the APK SHA-256, package/version and signing identity, then hands the verified package to Android's installer. Installation remains explicit and is wake-safety gated.

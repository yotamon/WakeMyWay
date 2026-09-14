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
gradle test lint assembleDebug
```

CI pins Gradle 9.6.1 explicitly. The standard wrapper JAR is tracked as a small M0 follow-up and must be added before M0 closes.

## Visual regression

`ProductVisualRegressionTest` renders the curated product states at the canonical 393 × 852 viewport. The approved visual contract is stored as exact SHA-256 hashes in:

```text
app/src/test/visual-goldens.sha256
```

CI regenerates the PNG renders with Roborazzi and fails if any canonical render differs from that manifest. The generated PNGs and Roborazzi diagnostics are uploaded as workflow artifacts for visual review; they are intentionally not committed to the repository.

Do not update the manifest merely to make CI green. A visual hash change is an explicit design acceptance decision and should be made only after reviewing the rendered screens.

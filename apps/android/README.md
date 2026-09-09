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

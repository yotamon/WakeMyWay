# WakeMyWay release performance evidence

This module measures the shipped product from outside the app process with AndroidX Macrobenchmark.

## What the first 1.0 benchmark measures

`StartupBenchmark.coldStartupWithoutPrecompile` measures cold startup of the configured WakeMyWay app with:

- the Play distribution flavor;
- a non-debuggable build derived from `release`;
- local debug signing only so the benchmark can be installed without production signing material;
- `CompilationMode.None()` for a conservative no-precompile baseline;
- 10 cold-start iterations;
- `StartupTimingMetric`.

The benchmark deliberately does **not** clear app data. Before an accepted physical run, configure the app normally and leave it on the ordinary consumer journey you want to measure.

## Physical-device run

Use a consistent supported Android phone, connected through ADB:

```bash
cd apps/android
./gradlew :benchmark:connectedPlayBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.wakemyway.benchmark.StartupBenchmark
```

Retain the generated benchmark JSON/traces and record:

```text
device/model:
Android build:
WakeMyWay commit:
WakeMyWay version:
benchmark:
compilation mode:
startup mode:
iterations:
timeToInitialDisplayMs min / median / max:
timeToFullDisplayMs min / median / max (when available):
notes:
```

Use the median as the primary comparison signal.

## CI policy

CI only compiles/packages the benchmark contract. Do not introduce a pass/fail startup-millisecond threshold from a GitHub-hosted emulator; those numbers share host resources and are not accepted product-performance evidence.

## Optimization rule

Measure before changing release behavior. A Baseline Profile, shrinking/minification, dependency removal, or startup restructuring needs a measured before/after result and must preserve alarm reliability, offline assets and launch correctness.

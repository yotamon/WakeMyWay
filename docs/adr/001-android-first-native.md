# ADR-001: Android-first native implementation

**Status:** Accepted
**Date:** 2026-09-09

## Context

Wake My Way depends deeply on alarms, system presentation, audio, microphone lifecycle, sensors, permissions, and background execution. The founder can dogfood Android daily.

## Decision

Build Android only initially using Kotlin + Jetpack Compose.

Do not use React Native or Flutter for V1.

Future iOS readiness is preserved through canonical product/domain concepts, deterministic behavior tests, data/API contracts, and clean local boundaries. Do **not** pre-build iOS-shaped interfaces merely for hypothetical portability.

## Consequences

Positive:

- direct platform control
- simpler alarm-reliability diagnosis
- daily dogfood
- fewer platform variables during product discovery
- Android implementation can use platform-native capabilities deeply

Negative:

- iOS is delayed
- future iOS UI/platform code will be native/separate
- some pure domain behavior may later be ported or extracted once its real size is known

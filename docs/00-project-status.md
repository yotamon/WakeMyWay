# Project status

**Last updated:** 2026-09-09  
**Product:** Wake My Way (WMW)  
**Platform:** Android first  
**Current phase:** Pre-implementation architecture + plan hardening complete; implementation not started

## Executive status

Wake My Way has completed initial discovery, product definition, UX/brand direction, a deep-module pre-development architecture review, and a final plan-hardening pass focused on the remaining highest-risk gaps before code.

The first review simplified speculative architecture. The hardening pass then addressed four concrete risks:

1. scheduled alarm reliability did not yet explicitly own **active alarm execution/recovery** after the trigger
2. the core adaptive moat, **Wake Learning**, was scheduled too late behind realtime voice
3. the product metric allowed the runtime's own activation criterion to become circular proof of success
4. several Android platform choices were still presented as fully open even though current 2026 Play/platform requirements provide a strong baseline

The project is now ready to begin **M0 Foundation**, followed immediately by **M1 Deep Alarm Kernel + Active Wake Execution**.

## Architecture state

### Canonical domain context

Root [`../CONTEXT.md`](../CONTEXT.md) owns canonical vocabulary and invariants.

New hardening terms include:

- Active Wake Execution
- Activation Completion
- Confirmed Wake Success
- Safety Backup

### Deep Alarm Kernel

The Alarm Kernel now owns the whole trust-critical lifecycle:

```text
accepted Wake Schedule / next Wake Occurrence
→ durable normal state
→ minimal Critical Wake Snapshot
→ exact Android alarm registration
→ readiness/reconciliation
→ occurrence fires
→ Active Wake Execution
→ safe local alarm playback + controls
→ stop / durable snooze / completion
```

Callers do not coordinate those steps themselves.

`WakeActivity` is presentation, not critical playback lifetime authority. ADR-014 defines the initial foreground alarm execution direction and recovery invariants.

### Wake Runtime

Canonical phases remain:

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

Escalation, fallback mode, movement requests, and First Move are not phases.

The legacy term **Verified Awake** remains removed. WMW observes behavioral activation; it cannot medically verify consciousness.

### Wake Learning

Wake Learning stays separate from in-session Wake Runtime but is now a **core earlier milestone**.

M7 implements local deterministic Wake Learning v0 before realtime voice. It must demonstrate bounded, explainable, reversible future-policy adaptation without ML, backend, or network dependency.

### Outcome model

The metrics now distinguish:

- **Activation Completion** — runtime's phone-observable activation criterion is reached
- **Confirmed Wake Success** — calibrated evidence indicates the user actually achieved the intended wake result

Product-level Wake Success uses confirmed calibration where available. Missing feedback is not silently counted as confirmation.

### Minimal physical module topology

M0 still starts with only:

```text
:app
:wake-core
:benchmark
```

No Hilt, Navigation framework, backend skeleton, feature-module forest, testkit module, or generic provider hierarchy is required before implementation proves the need.

### V1 schedule scope

V1 still supports **one active adaptive Wake Schedule at a time**. It may represent weekday-specific times and always yields one next Wake Occurrence.

A later conventional **Safety Backup** may be tested during founder/trusted dogfood while trust is built. It is explicitly not another adaptive schedule and must not create generic multi-alarm architecture.

### Direct Boot

A minimal non-sensitive Critical Wake Snapshot lives in device-protected storage so a wake occurrence can be recovered before first unlock after reboot. Private contextual content remains credential-protected.

### Reliability envelope

Explicit user Force Stop can make scheduled/active delivery impossible when Android intentionally stops the package. Wake Ready must communicate/recover what the app can actually guarantee rather than make an impossible promise.

Normal Activity recreation/process churn is different from Force Stop and is now explicitly part of the M1/M2 active-execution reliability design/testing.

## Current Android implementation baseline

As of 2026-09-09:

```text
targetSdk = 36 baseline
compileSdk = 36+ using current stable M0 toolchain
minSdk = still to be selected deliberately during M0
```

Google Play currently requires new phone/tablet apps and updates submitted after 2026-08-31 to target Android 16 / API 36 or higher.

Exact-alarm direction:

- `AlarmManager.setAlarmClock()` remains the planned user-facing wake primitive
- `USE_EXACT_ALARM` is the preferred manifest strategy because WMW is a dedicated alarm-clock app whose core functionality needs exact timing
- current Play restricted-permission eligibility must be revalidated when implemented and again before submission
- if evidence forces a different permission path, record the change rather than silently diverging

Android 17/API-37 background-audio changes are a forward-compatibility test concern, not the M0 target SDK. Active alarm playback is designed so a future target-SDK upgrade has a clear foreground execution/audio owner.

## Testing and deployment

The Android app is validated through:

```text
pure Kotlin tests
→ Wake Lab
→ Android instrumentation
→ Firebase Test Lab
→ Google Play Internal Testing
→ real overnight dogfood
```

M2 now explicitly includes:

- `WakeActivity` recreation while alarm is sounding
- playback-owner recreation
- controlled process recreation while active
- duplicate-start suppression
- stop then recreation (must not resurrect)
- durable snooze then recreation (old occurrence must not resurrect)
- Android 17 background-audio compatibility

Vercel remains the preferred future host for non-critical web/API workloads. Supabase remains the preferred future managed cloud data platform. Neither is needed for M0–M7 core local behavior.

Realtime voice deployment/transport is now selected by the **M8 measured spike**, after local Wake Learning v0. See [`32-testing-and-deployment-topology.md`](32-testing-and-deployment-topology.md), ADR-008, ADR-012, ADR-013, and ADR-014.

## What remains decided

### Product

- WMW is an adaptive conversational alarm.
- Primary job: help the user become meaningfully active around the time they intentionally chose.
- Differentiation: learn the minimum effective wake intervention for the person/context.
- Adaptation must be proven locally before realtime voice becomes central.
- It is not a general assistant, task manager, sleep tracker, habit tracker, or engagement product.

### Platform

- Android only for the initial product/beta.
- Native Kotlin + Jetpack Compose.
- No React Native/Flutter.
- No KMP now.
- Future iOS portability is preserved via domain concepts/behavior/contracts, not speculative mobile interfaces.

### Reliability

- Android exact alarm path is local/native.
- Alarm Kernel authority covers scheduling **and Active Wake Execution**.
- `WakeActivity` does not own critical audio lifetime.
- Cloud/AI is outside the Alarm Kernel critical path.
- Vercel is the preferred initial host for future non-critical web/API workloads only.
- Supabase is the preferred future managed data platform; no cloud database is authoritative for current wake delivery or local M7 learning.
- Prepared local behavior and bundled emergency audio provide graceful degradation.

### UX

- UX consciousness model: Asleep → Emerging → Engaged → Active → Oriented.
- This is a design model, not a 1:1 code-state model.
- Audio first, screen second.
- One cognitive demand at a time.
- Snooze is intentional, not morally prohibited.
- Stop always exists locally/accessibly but should not be an oversized reflex target.
- Movement is preferred over arbitrary puzzles.
- No shame or infantilization.
- Outcome calibration is occasional and happens after the alarm moment, not every morning during waking.

### Brand

- **Wake My Way** / **WMW**
- **Wake up your way.**
- **An alarm that learns what works for you.**
- Initial brand board exists under `docs/brand/assets/`.

## Milestone status

| Milestone | Status |
|---|---|
| Discovery / product definition | Complete v1 |
| UX psychology / flows | Complete v1 |
| Brand direction | Complete v1 |
| Architecture review | Complete |
| Plan hardening review | **Complete** |
| M0 Foundation | **Next** |
| M1 Deep Alarm Kernel + Active Wake Execution | Not started |
| M2 Reliability Harness / Direct Boot / active recovery | Not started |
| M3 Wake Runtime | Not started |
| M4 Motion evidence | Not started |
| M5 Alfred local experience | Not started |
| M6 Tomorrow Contract | Not started |
| M7 Wake Learning v0 | Not started |
| M8 Voice architecture spike | Not started |
| M9 Realtime conversation | Not started |
| M10 Useful context | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Bootstrap native Android project.
2. Create only `:app`, `:wake-core`, `:benchmark`.
3. Set M0 Android baseline: target API 36, current stable compile/toolchain, deliberate `minSdk`.
4. Establish CI/lint/tests and enforce pure-Kotlin `:wake-core` boundary.
5. Implement `WakeSchedule` / `WakeOccurrence` / recurrence / DST behavior in `:wake-core`.
6. Revalidate current `USE_EXACT_ALARM` eligibility/manifest requirements before adding the permission.
7. Begin M1 with the **Alarm Kernel caller contract**, exact scheduling transaction, and ADR-014 Active Wake Execution vertical slice.
8. During M2, establish Wake Lab/instrumentation timing capture and a small Firebase Test Lab matrix; later distribute dogfood through Play Internal Testing.

## Current blockers

No implementation blocker.

Still required before wider Play distribution:

- revalidate `USE_EXACT_ALARM` declaration/restricted-permission policy under the then-current Play rules
- finalize supported `minSdk` and OEM/device matrix
- trademark clearance / domain registration confirmation
- privacy/provider legal review before cloud/realtime beta

These do not block M0/M1 local development.

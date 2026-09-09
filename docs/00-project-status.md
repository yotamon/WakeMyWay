# Project status

**Last updated:** 2026-09-09
**Product:** Wake My Way (WMW)
**Platform:** Android first
**Current phase:** Pre-implementation architecture review complete; implementation not started

## Executive status

Wake My Way has completed initial discovery, product definition, UX/brand direction, and a second-pass pre-development architecture review using deep-module/codebase-design principles.

That review deliberately **simplified** the planned implementation before any code was written. The project is now ready to begin M0 without carrying speculative module/interface complexity into the codebase.

The next task is **M0 Foundation**, followed immediately by the first vertical slice of **M1 Deep Alarm Kernel**.

## What changed in the pre-development architecture review

### 1. Canonical domain context

Root [`../CONTEXT.md`](../CONTEXT.md) is now the canonical vocabulary and invariant source. The previous separate glossary was retired.

### 2. Deep Alarm Kernel

The Alarm Kernel owns the whole reliability transaction:

```text
accepted Wake Schedule / next Wake Occurrence
→ durable normal state
→ minimal Critical Wake Snapshot
→ exact Android alarm registration
→ readiness/reconciliation
→ audible wake start
```

Callers do not coordinate those steps themselves.

### 3. Simpler runtime

Canonical phases are now:

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

Escalation, fallback mode, movement requests, and First Move are not phases.

The legacy term **Verified Awake** was removed. WMW observes behavioral activation; it cannot medically verify consciousness.

### 4. Wake Confidence is no longer a public architecture seam

Activation evidence/confidence may exist internally, but it belongs inside Wake Runtime. Wake Learning is separate and operates between sessions to derive future policy.

### 5. Minimal physical module topology

M0 starts with only:

```text
:app
:wake-core
:benchmark
```

No Hilt, Navigation framework, backend skeleton, feature-module forest, testkit module, or generic provider hierarchy is required before implementation proves the need.

### 6. V1 schedule scope is explicit

V1 supports **one active Wake Schedule at a time**. It may represent weekday-specific times and always yields one next Wake Occurrence. Multiple independent alarms are deferred.

### 7. Direct Boot is part of M1/M2 design

A minimal non-sensitive Critical Wake Snapshot lives in device-protected storage so a wake occurrence can be recovered before first unlock after reboot. Private contextual content remains credential-protected.

### 8. Reliability envelope is honest about Android constraints

Explicit user Force Stop can make scheduled delivery impossible on Android versions that cancel pending intents. Wake Ready must communicate/recover what the app can actually guarantee rather than make an impossible promise.

### 9. Testing and deployment topology is explicit

The Android app is validated through a ladder of pure-Kotlin tests, Wake Lab, instrumentation, Firebase Test Lab, Google Play Internal Testing, and real overnight dogfood.

Vercel is the preferred future host for non-critical web/API workloads, while Supabase is the preferred managed cloud data platform (PostgreSQL first; Auth/Storage only when justified). Both remain outside the Alarm Kernel critical path. Realtime voice deployment/transport is still selected by the M7 measured spike. See [`32-testing-and-deployment-topology.md`](32-testing-and-deployment-topology.md), ADR-012, and ADR-013.

## What remains decided

### Product

- WMW is an adaptive conversational alarm.
- Primary job: help the user become meaningfully active around the time they intentionally chose.
- Differentiation: learn the minimum effective wake intervention for the person/context.
- It is not a general assistant, task manager, sleep tracker, habit tracker, or engagement product.

### Platform

- Android only for the initial product/beta.
- Native Kotlin + Jetpack Compose.
- No React Native/Flutter.
- No KMP now.
- Future iOS portability is preserved via domain concepts/behavior/contracts, not speculative mobile interfaces.

### Reliability

- Android exact alarm path is local/native.
- `AlarmManager.setAlarmClock()` is the planned user-facing wake primitive.
- Cloud/AI is outside the Alarm Kernel critical path.
- Vercel is the preferred initial host for future non-critical web/API workloads only.
- Supabase is the preferred future managed data platform; no cloud database is authoritative for current wake delivery.
- Prepared local behavior and bundled emergency audio provide graceful degradation.

### UX

- UX consciousness model: Asleep → Emerging → Engaged → Active → Oriented.
- This is a **design model**, not a 1:1 code-state model.
- Audio first, screen second.
- One cognitive demand at a time.
- Snooze is intentional, not morally prohibited.
- Movement is preferred over arbitrary puzzles.
- No shame or infantilization.

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
| Architecture review | **Complete** |
| M0 Foundation | **Next** |
| M1 Deep Alarm Kernel | Not started |
| M2 Reliability Harness / Direct Boot | Not started |
| M3 Wake Runtime | Not started |
| M4 Motion evidence | Not started |
| M5 Alfred local experience | Not started |
| M6 Tomorrow Contract | Not started |
| M7 Voice architecture spike | Not started |
| M8 Realtime conversation | Not started |
| M9 Context | Not started |
| M10 Learning | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Bootstrap native Android project.
2. Create only `:app`, `:wake-core`, `:benchmark`.
3. Establish CI/lint/tests and enforce pure-Kotlin `:wake-core` boundary.
4. Implement `WakeSchedule` / `WakeOccurrence` / recurrence / DST behavior in `:wake-core`.
5. Begin M1 with the **Alarm Kernel caller contract** and its first end-to-end scheduling transaction.
6. Re-verify current Android SDK/Play requirements before manifest/toolchain decisions.
7. During M2, establish Wake Lab/instrumentation timing capture and a small Firebase Test Lab matrix; later distribute dogfood through Play Internal Testing.

## Current blockers

No implementation blocker.

Before Play distribution, still required:

- final exact-alarm declaration/special-access strategy under current Play policy
- trademark clearance / domain registration confirmation
- privacy/provider legal review

These do not block M0/M1 local development.

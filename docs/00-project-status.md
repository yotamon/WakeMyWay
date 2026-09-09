# Project status

**Last updated:** 2026-09-09  
**Product:** Wake My Way (WMW)  
**Platform:** Android first  
**Current phase:** M0 Foundation implemented and CI-verified; M1 Deep Alarm Kernel + Active Wake Execution is next

## Executive status

Wake My Way has moved from planning into implementation.

The pre-development architecture review and final plan-hardening pass remain authoritative. The hardening work added explicit ownership of **Active Wake Execution**, moved deterministic **Wake Learning v0** ahead of realtime voice, separated Activation Completion from Confirmed Wake Success, and clarified Android platform/reliability decisions.

M0 now implements the first production-quality Android foundation on top of that plan:

- a real native Android project
- intentionally minimal physical modules
- pure-Kotlin Wake Schedule / Wake Occurrence domain
- deterministic recurrence and DST semantics with tests
- committed Gradle wrapper
- GitHub CI for docs, tests, lint, APK assembly, and downloadable debug APK artifacts

The next milestone is **M1 Deep Alarm Kernel + Active Wake Execution**. It must turn an accepted Wake Occurrence into an exact, recoverable, locally audible alarm whose critical playback lifetime does not depend on `WakeActivity`, cloud services, AI, or normal app navigation.

No physical-device alarm reliability claim has been made yet. That evidence belongs to M1/M2 and must be measured.

## Architecture state

### Canonical domain context

Root [`../CONTEXT.md`](../CONTEXT.md) owns canonical vocabulary and invariants.

Important hardening terms include:

- Active Wake Execution
- Activation Completion
- Confirmed Wake Success
- Safety Backup

### Deep Alarm Kernel

The Alarm Kernel owns the whole trust-critical lifecycle:

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

`WakeActivity` is presentation, not critical playback lifetime authority. ADR-014 defines the initial Active Wake Execution direction and recovery invariants.

### Wake Runtime

Canonical phases remain:

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

Escalation, fallback mode, movement requests, and First Move are not phases.

The legacy term **Verified Awake** remains removed. WMW observes behavioral activation; it cannot medically verify consciousness.

### Wake Learning

Wake Learning stays separate from in-session Wake Runtime and is now an earlier core milestone.

M7 implements local deterministic Wake Learning v0 before realtime voice. It must demonstrate bounded, explainable, reversible future-policy adaptation without ML, backend, or network dependency.

### Outcome model

Metrics distinguish:

- **Activation Completion** — the runtime's phone-observable activation criterion is reached
- **Confirmed Wake Success** — calibrated evidence indicates the intended wake result was actually achieved

Missing feedback is not silently counted as confirmation.

### V1 schedule scope

V1 supports **one active adaptive Wake Schedule at a time**. It may contain weekday-specific times and always yields one next Wake Occurrence.

A later conventional **Safety Backup** may be evaluated during founder/trusted dogfood while trust is being established. It is not a second adaptive schedule and must not force generic multi-alarm architecture.

## Implemented in M0

### Minimal Android module topology

```text
:app
:wake-core
:benchmark
```

No Hilt, Navigation framework, backend skeleton, feature-module forest, testkit module, generic provider hierarchy, Room, analytics, or realtime voice infrastructure was added speculatively.

### Android toolchain baseline

```text
Android Gradle Plugin  9.4.0
Kotlin                 2.4.20
Gradle                 9.6.1 via committed wrapper
JDK                    17
Compose BOM            2026.08.00
compileSdk             37
targetSdk              36
minSdk                 29
```

GitHub CI installs Android 17 using `platforms;android-37.0` with build tools 37.0.0.

`targetSdk = 36` satisfies the current 2026 Play target baseline while API-37 behavior can be compiled and tested deliberately before a future target upgrade.

### Pure Kotlin scheduling domain

`:wake-core` now contains:

- `WakeScheduleId`
- `WakeOccurrenceId`
- `WakeSchedule`
- `WakeOccurrence`
- `WakeOccurrenceKind`
- `LocalTimeResolution`
- `NextWakeOccurrenceResolver`

The resolver produces the next PRIMARY Wake Occurrence strictly after `now`.

### Timezone and DST semantics

Current deterministic policy:

- ordinary local time: use the single valid offset
- spring-forward gap: move to the first valid local time after the gap
- fall-back overlap: use the earlier physical occurrence so the alarm is not delayed by an extra hour
- if today's selected time already passed, resolve the next active recurrence

Tests cover same-day scheduling, recurrence rollover, spring-forward gaps, fall-back overlaps, and exact-time rollover.

### Android shell

`:app` launches a minimal Wake My Way Compose surface using the documented brand direction. It is intentionally only a foundation shell. Normal scheduling UX and the dedicated `WakeActivity` belong to M1 and later UX work.

### CI and build hygiene

The repository now validates:

```text
documentation
    ↓
:wake-core tests
    ↓
Android lint
    ↓
debug APK assembly
    ↓
APK artifact upload
```

A clean GitHub-hosted build has passed the Android 17 SDK setup, pure-Kotlin tests, Android lint, and debug APK assembly. The repository contains the official Gradle wrapper so local/CI builds do not require a globally installed Gradle.

## Current Android permission / execution direction

`AlarmManager.setAlarmClock()` remains the exact user-facing wake primitive.

WMW is a dedicated alarm-clock app whose core function genuinely requires exact timing, so `USE_EXACT_ALARM` is the preferred exact-alarm manifest strategy under current Play policy. The policy must still be revalidated at implementation/submission time and declared correctly in Play Console.

For full-screen alarm presentation, WMW's alarm core use case fits the platform/Play eligibility category for `USE_FULL_SCREEN_INTENT`; M1 must still expose truthful capability state and degrade gracefully when the capability is unavailable.

Android 17 background-audio hardening makes Active Wake Execution ownership especially important. M1 must use an appropriate foreground execution path for critical alarm playback and `USAGE_ALARM` audio semantics rather than relying on Activity lifetime.

## Direct Boot and reliability

A minimal non-sensitive Critical Wake Snapshot belongs in device-protected storage so a Wake Occurrence can be recovered before first unlock after reboot.

Private contextual content remains credential-protected, including:

- Tomorrow Contract text
- calendar content
- transcripts
- prompts
- tokens/secrets
- personalized private generated speech

Explicit user Force Stop remains outside the deliverable-alarm reliability envelope when Android intentionally cancels pending work. Normal Activity/process recreation is different and is explicitly part of M1/M2 testing.

## Testing and deployment

The evidence ladder remains:

```text
pure Kotlin tests
→ Wake Lab
→ Android instrumentation
→ Firebase Test Lab
→ Google Play Internal Testing
→ real overnight dogfood
```

M2 must include:

- repeated T+30s / T+2m cycles
- process and Activity recreation
- playback-owner recreation
- Doze/idle
- locked vs already-unlocked presentation
- notification permission denied
- full-screen intent unavailable/revoked
- reboot and Direct Boot before first unlock
- timezone/wall-clock changes
- duplicate start suppression
- stop then recreation: must not resurrect
- durable snooze then recreation: old occurrence must not resurrect
- Android 17 background-audio compatibility
- trigger → audible and trigger → accessible-controls latency measurements

Vercel remains the preferred future non-critical web/API host. Supabase remains the preferred future managed cloud data platform. Neither is needed for M0–M7 core local behavior or current wake authority.

Realtime voice deployment/transport is selected by the **M8 measured spike**, after local Wake Learning v0.

## Milestone status

| Milestone | Status |
|---|---|
| Discovery / product definition | Complete v1 |
| UX psychology / flows | Complete v1 |
| Brand direction | Complete v1 |
| Architecture review | Complete |
| Plan hardening review | Complete |
| M0 Foundation | **Implemented and CI-verified; merging** |
| M1 Deep Alarm Kernel + Active Wake Execution | **Next** |
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

## Exact next work: M1

1. Define one small caller-facing Alarm Kernel operation that commits the one active Wake Schedule and its next Wake Occurrence atomically from the caller's perspective.
2. Revalidate and document the exact-alarm, foreground-service, alarm-audio, notification, and full-screen-intent manifest/Play strategy before implementation is merged.
3. Add durable normal local state for schedule + active occurrence without leaking orchestration into UI code.
4. Implement a minimal versioned Critical Wake Snapshot in device-protected storage with atomic writes and integrity validation.
5. Register the next Wake Occurrence with `AlarmManager.setAlarmClock()` using stable PendingIntent identity/version semantics.
6. Implement Active Wake Execution as the critical local playback owner, independent of `WakeActivity` lifecycle.
7. Add a thin alarm receiver, alarm notification/full-screen presentation path, and bundled emergency audio.
8. Add a dedicated `WakeActivity` independent of MainActivity navigation state, with accessible Stop and Snooze controls.
9. Make Stop and Snooze durable. Snooze must create a new exact Wake Occurrence before the current execution ends.
10. Add boot/time/timezone/package reconciliation, including Direct Boot.
11. Expose truthful Wake Ready / Alarm Health facts.
12. Add automated integration coverage where Android APIs can be exercised, then move into the M2 device/reliability harness.

## Known risks and boundaries

Not yet proven on physical devices:

- exact alarm delivery while locked
- process-death behavior around an active wake attempt
- Direct Boot re-registration before first unlock
- OEM-specific behavior across supported device families
- final Android 17 background-audio behavior
- full-screen presentation under every permission/lock-state combination
- real trigger → audible and trigger → controls latency percentiles

These are M1/M2 evidence requirements, not M0 blockers.

Still required before wider Play distribution:

- exact-alarm / full-screen / foreground-service Play declarations under then-current policy
- release signing and Play Internal Testing setup
- trademark clearance / domain ownership confirmation
- privacy/provider legal review before cloud/realtime beta

## Current blocker

There is no codebase blocker to M1 after M0 merges. Physical-device reliability evidence will require actual Android devices or an appropriate device-testing service during M2.

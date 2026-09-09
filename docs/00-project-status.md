# Project status

**Last updated:** 2026-09-09  
**Product:** Wake My Way (WMW)  
**Platform:** Android first  
**Current phase:** M2 Reliability Harness in progress  
**Current implementation branch:** `feat/m2-reliability-harness`  
**Current PR:** #10

## Executive status

Wake My Way is in active Android development.

M0 Foundation and M1 Deep Alarm Kernel are merged into `main`. The project now has a real exact local Android alarm path with durable Direct-Boot state, local alarm playback, dedicated wake presentation, crash-safe cancellation, durable snooze semantics, reconciliation, Wake Ready diagnostics, and local timing instrumentation.

M2 is now converting that build-valid Alarm Kernel into an evidence-backed alarm system. The first Wake Alarm Lab slice adds repeatable one-shot T+2m tests, a bounded replayable reliability journal, exportable reports, service/reconciliation/capability evidence, and Android instrumentation tests.

No physical-device reliability claim has been made yet. A green CI build proves compilation, domain tests, lint, APK packaging, and instrumentation-test packaging. It does not prove locked-screen, Doze, reboot-before-unlock, OEM power-management, full-screen presentation, or real audio latency behavior.

## Canonical architecture

Root [`../CONTEXT.md`](../CONTEXT.md) owns vocabulary and invariants.

The trust-critical path remains local:

```text
Wake Schedule
    ↓
Alarm Kernel
    ↓
Critical Wake Snapshot
(device-protected, non-sensitive)
    ↓
AlarmManager.setAlarmClock()
    ↓
AlarmReceiver
    ↓
Active Wake Execution
AlarmPlaybackService
    ├─ USAGE_ALARM bundled local audio
    ├─ notification / full-screen wake intent
    └─ durable Stop / Snooze
             ↓
        WakeActivity
```

Cloud, Vercel, Supabase, AI, calendar, weather, and realtime voice do not participate in this path.

`WakeActivity` is presentation. It does not own critical playback lifetime.

## Implemented: M0 Foundation

- native Android project
- intentionally minimal modules: `:app`, `:wake-core`, `:benchmark`
- Kotlin / Compose baseline
- committed Gradle wrapper
- pure Kotlin Wake Schedule / Wake Occurrence domain
- deterministic recurrence and DST handling
- GitHub Actions for docs, domain tests, lint, APK assembly, and artifacts

Current toolchain:

```text
Android Gradle Plugin  9.4.0
Kotlin                 2.4.20
Gradle                 9.6.1
JDK                    17
Compose BOM            2026.08.00
compileSdk             37
targetSdk              36
minSdk                 29
```

## Implemented and merged: M1 Deep Alarm Kernel

M1 merged in PR #8.

Implemented:

- `AlarmManager.setAlarmClock()` exact alarm registration
- `USE_EXACT_ALARM` direction for the dedicated alarm use case
- `USE_FULL_SCREEN_INTENT` alarm presentation path
- foreground Active Wake Execution with `mediaPlayback`
- bundled local emergency WAV using `AudioAttributes.USAGE_ALARM`
- ToneGenerator secondary fallback
- dedicated Direct-Boot-aware `WakeActivity`
- versioned Critical Wake Snapshot
- atomic device-protected persistence
- persisted OS-registration confirmation for truthful Wake Ready
- crash-safe schedule replacement
- durable disabled tombstone for cancellation
- stale occurrence rejection
- collision-free occurrence-specific PendingIntent identity
- idempotent active-occurrence transition
- foreground-service recovery from persisted active state
- durable Stop
- durable Snooze replacement scheduling before current playback ends
- boot / locked-boot / time / timezone / package reconciliation
- graceful exact-alarm capability loss
- non-sensitive target → receiver → foreground → audio → UI timing trace

M1's implementation is build-valid and CI-verified. Its physical-device exit criterion remains open and is being satisfied through M2 evidence.

## In progress: M2 Reliability Harness

Canonical runbook: [`implementation/m2-reliability-harness.md`](implementation/m2-reliability-harness.md).

The first M2 slice currently includes:

- hidden/founder Wake Alarm Lab screen
- one-shot T+2m lab schedules
- bounded device-protected reliability history
- replayable ordered event timeline per occurrence
- expected wake records before target time
- target, receiver, foreground, audio, UI and terminal timestamps
- elapsed-realtime stage latency calculation
- derived failure states such as missed receiver, audio timeout, and UI timeout
- service recreation count/event
- reconciliation event evidence
- exact alarm / notification / full-screen capability evidence
- Stop / Snooze terminal journaling
- snooze replacement expected-wake journaling
- shareable human-readable reliability report
- isolated device-test storage so instrumentation cannot overwrite founder alarm state/history
- Android instrumentation tests for critical snapshot persistence, cancellation resurrection prevention, one-shot snooze completion, and reliability-report/event replay
- CI compilation and artifact upload for the instrumentation APK

## M2 evidence status

### Proven by CI

- documentation validation
- pure Kotlin scheduling tests
- Android compile/lint
- debug APK packaging
- instrumentation test source compilation/package generation once PR #10 is green

### Not yet proven

- instrumentation execution on an emulator/device
- locked-screen T+2m delivery
- physical receiver → audio latency
- physical receiver → WakeActivity latency
- Doze/idle delivery
- process/service recreation during active playback
- reboot and Direct Boot before first unlock
- wall-clock/timezone change repair
- exact-alarm capability loss/restoration behavior on-device
- full-screen intent degradation behavior on-device
- OEM-specific power management
- Android 17 physical background-audio behavior

## Reliability targets

Targets remain:

- no silent expected wake failures without diagnosable evidence
- receiver → audible local alarm P99 < 1 second on supported devices
- receiver → WakeActivity visible P95 < 1 second when full-screen presentation is permitted
- stale/cancelled occurrences never become active
- snooze replacement is durable before current execution ends
- service/process recreation does not silence an active wake
- reboot-before-unlock can recover a future wake without credential-protected/private data

These are targets, not current claims.

## Privacy boundary for reliability data

Reliability evidence may contain only operational information such as technical occurrence IDs, scenario IDs, timestamps, terminal action, capability booleans, service recovery counts, device model, and SDK version in exported reports.

It must never contain:

- Tomorrow Contract text
- calendar content
- transcripts or microphone audio
- prompts
- secrets/tokens
- personalized private generated speech

## Milestone status

| Milestone | Status |
|---|---|
| Discovery / product definition | Complete v1 |
| UX psychology / flows | Complete v1 |
| Brand direction | Complete v1 |
| Architecture review | Complete |
| Plan hardening review | Complete |
| M0 Foundation | **Merged / complete** |
| M1 Deep Alarm Kernel + Active Wake Execution | **Merged / implementation complete; physical evidence delegated to M2** |
| M2 Reliability Harness | **In progress, PR #10** |
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

1. Get PR #10 fully green, including instrumentation APK compilation.
2. Review the reliability journal/report path for failure isolation and privacy.
3. Merge the first M2 harness slice once CI is green.
4. Execute instrumentation tests on a dedicated Android device/emulator path.
5. Install the latest debug APK on a real Android phone and run repeated locked-screen T+2m cycles.
6. Export and inspect measured trigger → audio / UI evidence.
7. Exercise Stop/Snooze resurrection, service recreation, Doze, reboot/Direct Boot, timezone/time changes, and capability-loss scenarios.
8. Record OEM/device results and only then close issue #5's physical-device criterion and M2 issue #9.
9. Begin M3 Wake Runtime only after the Alarm Kernel has sufficient reliability evidence to support higher-level behavior safely.

## Cloud / future stack status

Vercel remains the preferred future non-critical web/API host. Supabase remains the preferred managed PostgreSQL/Auth/Storage platform when cloud features become necessary.

Neither is required for M0–M7 local wake authority.

Realtime voice transport remains an M8 measured decision, after local deterministic Wake Learning v0.

## Current blocker

There is no codebase blocker to completing the first M2 harness slice.

The remaining proof boundary is environmental: physical-device and/or device-testing infrastructure is required for lock-screen, audio, Doze, reboot, Direct Boot, OEM, and real-latency evidence.

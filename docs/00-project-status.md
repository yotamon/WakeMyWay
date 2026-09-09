# Project status

**Last updated:** 2026-09-09  
**Product:** Wake My Way (WMW)  
**Platform:** Android first  
**Current engineering phase:** M4 Motion Evidence in progress, while M2 physical-device reliability evidence remains open  
**Current implementation branch:** `feat/m4-motion-evidence`  
**Current PR:** #15

## Executive status

Wake My Way is in active Android development.

M0 Foundation, M1 Deep Alarm Kernel, the first M2 reliability harness/emulator lane, and M3 deterministic Wake Runtime are merged into `main`.

The product now has:

- a real exact local Android alarm path
- durable Direct-Boot critical state
- foreground local alarm playback independent of WakeActivity lifetime
- crash-safe cancellation and snooze semantics
- replayable reliability diagnostics
- Android instrumentation tests that execute successfully in a dedicated emulator workflow
- a deep pure-Kotlin Wake Runtime that owns intervention progression and activation evidence
- deterministic no-response escalation that cannot manufacture wake-success evidence

M4 is adding conservative physical motion evidence as a thin platform input to Wake Runtime. The first slice contains a pure deterministic extractor plus a thin `SensorManager` adapter. Thresholds are tuning hypotheses and are not yet calibrated on physical founder/OEM devices.

**No physical-device reliability percentile claim has been made yet.** Emulator/device-test evidence is real and useful, but it does not prove real locked-screen audio latency, Doze, reboot-before-unlock, OEM power management, or Android 17 physical behavior.

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
```

Behavior above the kernel is deterministic and local:

```text
platform/user facts
       ↓
   typed WakeInput
       ↓
    WakeRuntime
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
       ↓
 typed WakeDirective
```

M4 motion direction:

```text
Android sensors
      ↓
AndroidMotionObserver
      ↓
MotionEvidenceExtractor
      ↓
MotionObserved(kind)
      ↓
WakeRuntime
```

Cloud, Vercel, Supabase, AI, calendar, weather, and realtime voice do not participate in alarm delivery or Wake Runtime authority.

## Implemented: M0 Foundation

- native Android project
- intentionally minimal modules: `:app`, `:wake-core`, `:benchmark`
- committed Gradle wrapper
- pure Kotlin Wake Schedule / Wake Occurrence domain
- deterministic recurrence and DST behavior
- GitHub Actions for docs, domain tests, lint, instrumentation compilation, APK assembly and artifacts

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
- `USE_EXACT_ALARM` / full-screen alarm presentation direction
- foreground Active Wake Execution with `mediaPlayback`
- bundled local emergency WAV using `AudioAttributes.USAGE_ALARM`
- secondary ToneGenerator fallback
- dedicated Direct-Boot-aware `WakeActivity`
- versioned atomic device-protected Critical Wake Snapshot
- persisted OS-registration confirmation for truthful Wake Ready
- crash-safe schedule replacement and durable cancellation tombstone
- stale occurrence rejection and occurrence-specific PendingIntent identity
- foreground-service recovery from persisted active state
- durable Stop and Snooze replacement-before-stop semantics
- boot / locked-boot / time / timezone / package reconciliation
- graceful exact-alarm capability loss
- non-sensitive target → receiver → foreground → audio → UI timing trace

M1 implementation is CI-verified. Physical reliability evidence remains part of M2.

## M2 Reliability Harness: merged automated evidence, physical proof still open

PR #10 merged the first Wake Alarm Lab/reliability slice. PR #11 merged the dedicated Android device-test workflow.

Available tooling/evidence:

- hidden/founder Wake Alarm Lab
- one-shot T+2m lab schedules
- bounded device-protected reliability history
- replayable ordered occurrence timeline
- target, receiver, foreground, audio, UI and terminal timestamps
- derived missed-receiver/audio/UI failure states
- service-recreation, reconciliation and capability evidence
- Stop/Snooze terminal journaling
- shareable sanitized reliability reports
- isolated instrumentation-test storage
- instrumentation tests for critical snapshot persistence, cancellation resurrection prevention, one-shot snooze completion, event ordering and reporting
- dedicated API-36 emulator workflow with KVM
- `:app:connectedDebugAndroidTest` successfully executed on the emulator lane

### Proven by automated CI/device lane

- documentation validation
- pure Kotlin tests
- Android compile/lint
- debug APK packaging
- instrumentation APK compilation/package
- current Android instrumentation suite executes successfully on a known emulator image

### Still not proven on physical devices

- repeated locked-screen T+2m delivery
- receiver → real audible alarm latency distribution
- receiver → WakeActivity latency distribution
- Doze/idle behavior
- active playback process/service recreation
- reboot and Direct Boot before first unlock
- wall-clock/timezone repair
- exact-alarm / full-screen capability loss and restoration on-device
- OEM-specific power management
- Android 17 physical background-audio behavior

Reliability targets remain targets, not claims:

- no silent expected wake failure without diagnosable evidence
- receiver → audible local alarm P99 < 1 second on supported devices
- receiver → WakeActivity visible P95 < 1 second when full-screen presentation is permitted
- stale/cancelled occurrences never become active
- snooze replacement is durable before current execution ends
- service/process recreation does not silence an active wake
- reboot-before-unlock can recover a future wake without private-data dependency

Issue #9 remains open until physical evidence supports closing it.

## Implemented and merged: M3 Wake Runtime

M3 merged in PR #13.

Implemented in pure Kotlin:

- durable phases: `ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED`
- typed Wake Inputs and Wake Directives
- versioned Wake Policy
- internal Activation Evidence authority
- deterministic escalation/intervention progression
- explicit no-response progression
- silence/time can strengthen intervention but never count as Activation Evidence
- capability degradation
- durable Snooze scheduling handshake
- durable Stop handshake
- serialization of competing destructive effects
- bounded duplicate-input memory
- deterministic replay and diagnostics
- terminal session invariants

Android/AI callers cannot calculate a separate confidence score and overrule the runtime.

## In progress: M4 Motion Evidence

Canonical implementation note: [`implementation/m4-motion-evidence.md`](implementation/m4-motion-evidence.md).

Current PR #15 contains:

- pure `MotionEvidenceExtractor`
- `DEVICE_PICKUP`, `ORIENTATION_CHANGE`, `SUSTAINED_MOVEMENT` emissions
- bounded rolling feature state only
- threshold/cooldown/debounce hypotheses
- correlated-evidence protection so one physical pickup callback cannot also earn orientation credit
- conservative sensor fallbacks
- thin idempotent `AndroidMotionObserver`
- no raw sensor persistence
- pure extractor tests
- runtime integration test showing the same Wake Policy stays `ACTIVATING` without motion and can reach `ORIENTING` with distinct physical evidence

Production wiring into Active Wake Execution is intentionally deferred until the reliability boundary is ready for that integration.

## Privacy boundaries

Critical/reliability/motion operational storage must never contain:

- Tomorrow Contract text
- calendar content
- transcripts or microphone audio
- prompts
- secrets/tokens
- personalized private generated speech
- raw accelerometer / rotation / gravity / gyroscope streams

Derived motion evidence may contain only technical evidence type, monotonic timing, bounded derived reason and sensor-source availability.

## Milestone status

| Milestone | Status |
|---|---|
| Discovery / product definition | Complete v1 |
| UX psychology / flows | Complete v1 |
| Brand direction | Complete v1 |
| Architecture review / plan hardening | Complete |
| M0 Foundation | **Merged / complete** |
| M1 Deep Alarm Kernel + Active Wake Execution | **Merged / implementation complete** |
| M2 Reliability Harness | **Automated harness + emulator lane merged; physical-device evidence still open (#9)** |
| M3 Wake Runtime | **Merged / pure runtime complete** |
| M4 Motion evidence | **In progress, PR #15** |
| M5 Alfred local experience | Not started |
| M6 Tomorrow Contract | Not started |
| M7 Wake Learning v0 | Not started |
| M8 Voice architecture spike | Not started |
| M9 Realtime conversation | Not started |
| M10 Useful context | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Get PR #15 fully green and review conservative evidence semantics.
2. Merge the isolated M4 extractor/adapter slice once CI is clean.
3. Continue building higher-level local components only where they do not weaken the critical alarm path.
4. In parallel, run physical founder-device M2 scenarios when a device execution path is available: locked T+2m, Stop/Snooze resurrection, service recreation, Doze, reboot/Direct Boot, time/timezone changes and capability degradation.
5. Calibrate motion thresholds with real traces before treating them as production values.
6. Only wire adaptive runtime/motion behavior into Active Wake Execution after reliability evidence shows the integration is safe.

## Cloud / future stack status

Vercel remains the preferred future non-critical web/API host. Supabase remains the preferred managed PostgreSQL/Auth/Storage platform when cloud features become necessary.

Neither is required for M0–M7 local wake authority.

Realtime voice transport remains an M8 measured decision after deterministic local behavior and Wake Learning v0.

## Current blocker

There is no blocker to continuing isolated local-domain/product development.

The main unresolved proof boundary is environmental: **physical Android device evidence** is still required before Wake My Way can honestly claim production-grade alarm reliability or before richer adaptive behavior is allowed to endanger the trust-critical path.

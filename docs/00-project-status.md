# Project status

**Last updated:** 2026-09-09  
**Product:** Wake My Way (WMW)  
**Platform:** Android first  
**Current engineering phase:** M6 Tomorrow Contract + Prepared Wake Plan next, while M2 physical-device reliability evidence remains open  
**Current implementation branch:** none yet  
**Current PR:** none

## Executive status

Wake My Way is in active Android development.

Merged into `main`:

- M0 Foundation
- M1 Deep Alarm Kernel + Active Wake Execution
- M2 automated reliability harness + dedicated emulator lane
- M3 deterministic Wake Runtime
- M4 bounded Motion Evidence extraction + thin Android sensor adapter
- M5 Alfred deterministic local character + offline-only Android speech lab

The product now has:

- a real exact local Android alarm path;
- durable Direct-Boot critical state;
- foreground local alarm playback independent of `WakeActivity` lifetime;
- crash-safe cancellation and durable snooze semantics;
- replayable reliability diagnostics;
- Android instrumentation tests executing in a dedicated emulator workflow;
- a deep pure-Kotlin Wake Runtime that owns intervention progression and Activation Evidence;
- deterministic no-response escalation that cannot manufacture wake success;
- conservative derived motion evidence with no raw sensor persistence;
- a versioned deterministic Alfred renderer with bounded curated copy;
- an offline-only Android TextToSpeech adapter and silent character fallback in Wake Alarm Lab.

**No physical-device reliability percentile claim has been made.** Emulator/device-test evidence is useful, but it does not prove real locked-screen audio latency, Doze, reboot-before-unlock, OEM power management, audio coexistence, or Android 17 physical behavior.

## Canonical architecture

Root [`../CONTEXT.md`](../CONTEXT.md) owns vocabulary and invariants.

### Trust-critical wake path

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

Cloud, Vercel, Supabase, AI, calendar, weather and realtime voice do not participate in alarm delivery.

### Deterministic behavior path

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

Wake Runtime remains behavioral authority. Android adapters, characters and future AI cannot calculate a separate confidence score or overrule it.

### Motion evidence path

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

Current thresholds are tuning hypotheses until calibrated on physical devices.

### Character path

```text
WakeDirective.Speak(SpeechIntent)
           ↓
      AlfredCharacter
 deterministic curated wording
           ↓
    RenderedWakeLine
           ↓
 OfflineVoiceSelector
           ↓
 LocalCharacterSpeaker
 Android TextToSpeech
           ↓
 verified offline voice
```

Character speech currently exists in Wake Alarm Lab only. It does not own critical alarm audio and is not yet production-wired into Active Wake Execution.

## Implemented and merged: M0 Foundation

- native Android project;
- intentionally minimal modules: `:app`, `:wake-core`, `:benchmark`;
- committed Gradle wrapper;
- pure Kotlin Wake Schedule / Wake Occurrence domain;
- deterministic recurrence and DST behavior;
- GitHub Actions for docs, domain tests, lint, instrumentation compilation, APK assembly and artifacts.

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

- `AlarmManager.setAlarmClock()` exact registration;
- `USE_EXACT_ALARM` / full-screen alarm presentation direction;
- foreground Active Wake Execution with `mediaPlayback`;
- bundled local emergency WAV using `AudioAttributes.USAGE_ALARM`;
- secondary ToneGenerator fallback;
- dedicated Direct-Boot-aware `WakeActivity`;
- versioned atomic device-protected Critical Wake Snapshot;
- persisted OS-registration confirmation for truthful Wake Ready;
- crash-safe schedule replacement and durable cancellation tombstone;
- stale occurrence rejection and occurrence-specific PendingIntent identity;
- foreground-service recovery from persisted active state;
- durable Stop and Snooze replacement-before-stop semantics;
- boot / locked-boot / time / timezone / package reconciliation;
- graceful exact-alarm capability loss;
- non-sensitive target → receiver → foreground → audio → UI timing trace.

M1 is CI-verified. Physical reliability evidence remains part of M2.

## M2 Reliability Harness: automated evidence merged, physical proof open

PR #10 merged the Wake Alarm Lab/reliability slice. PR #11 merged the dedicated Android device-test workflow.

Available tooling/evidence:

- founder Wake Alarm Lab;
- one-shot T+2m schedules;
- bounded device-protected reliability history;
- replayable ordered occurrence timeline;
- target, receiver, foreground, audio, UI and terminal timestamps;
- derived missed-receiver/audio/UI failure states;
- service-recreation, reconciliation and capability evidence;
- Stop/Snooze terminal journaling;
- shareable sanitized reliability reports;
- isolated instrumentation-test storage;
- instrumentation tests for critical snapshot persistence, cancellation resurrection prevention, one-shot snooze completion, event ordering and reporting;
- dedicated API-36 emulator workflow with KVM;
- successful `:app:connectedDebugAndroidTest` execution on the emulator lane.

### Proven by automated CI/device lane

- documentation validation;
- pure Kotlin tests;
- Android compile/lint;
- debug APK packaging;
- instrumentation APK compilation/package;
- current Android instrumentation suite executes on a known emulator image.

### Still not proven on physical devices

- repeated locked-screen T+2m delivery;
- receiver → real audible alarm latency distribution;
- receiver → `WakeActivity` latency distribution;
- Doze/idle behavior;
- active playback process/service recreation;
- reboot and Direct Boot before first unlock;
- wall-clock/timezone repair;
- exact-alarm / full-screen capability loss and restoration on-device;
- OEM-specific power management;
- Android 17 physical background-audio behavior;
- coexistence of critical alarm audio with optional local character TTS.

Reliability targets remain targets, not claims:

- no silent expected wake failure without diagnosable evidence;
- receiver → audible local alarm P99 < 1 second on supported devices;
- receiver → `WakeActivity` visible P95 < 1 second when full-screen presentation is permitted;
- stale/cancelled occurrences never become active;
- snooze replacement is durable before current execution ends;
- service/process recreation does not silence an active wake;
- reboot-before-unlock can recover a future wake without private-data dependency.

Issue #9 remains open until physical evidence supports closing it.

## Implemented and merged: M3 Wake Runtime

M3 merged in PR #13.

Implemented in pure Kotlin:

- durable phases `ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED`;
- typed Wake Inputs and Wake Directives;
- versioned Wake Policy;
- internal Activation Evidence authority;
- deterministic escalation/intervention progression;
- no-response progression without synthetic wake evidence;
- capability degradation;
- durable Snooze scheduling handshake;
- durable Stop handshake;
- serialization of competing destructive effects;
- bounded duplicate-input memory;
- deterministic replay and diagnostics;
- terminal session invariants.

## Implemented and merged: M4 Motion Evidence

M4 merged in PR #15. Canonical implementation note: [`implementation/m4-motion-evidence.md`](implementation/m4-motion-evidence.md).

Implemented:

- pure `MotionEvidenceExtractor`;
- `DEVICE_PICKUP`, `ORIENTATION_CHANGE`, `SUSTAINED_MOVEMENT` emissions;
- bounded ephemeral rolling state only;
- threshold/cooldown/debounce hypotheses;
- correlated-evidence protection;
- conservative sensor fallbacks;
- thin idempotent `AndroidMotionObserver`;
- no raw sensor persistence;
- pure extractor tests;
- runtime integration test showing distinct physical evidence can advance the same Wake Policy.

Production wiring remains reliability-gated, and thresholds are not considered calibrated yet.

## Implemented and merged: M5 Alfred local character experience

M5 merged in PR #17. Issue #16 is complete. Canonical implementation note: [`implementation/m5-alfred-local.md`](implementation/m5-alfred-local.md).

Implemented:

- versioned `CharacterSpec` and `RenderedWakeLine`;
- Alfred v1: dry, composed, concise, direct;
- deterministic FNV-based bounded variant selection;
- curated coverage for every current `SpeechIntent`;
- bounded `ReEngage` escalation language;
- semantic guardrails preventing premature snooze-success claims and unsupported awake/posture claims;
- automated brevity and hostile/shaming-language checks;
- pure `OfflineVoiceSelector` rejecting network-required and unrelated-language voices;
- Android `LocalCharacterSpeaker` using TextToSpeech only after verified offline voice selection;
- explicit initialization, interruption, failure and shutdown behavior;
- immediate silent character fallback when speech is unavailable;
- Alfred preview and voice diagnostics in Wake Alarm Lab;
- no network, AI, microphone or transcript persistence.

The final PR #17 head passed docs, pure tests, Android lint, instrumentation compilation, debug APK assembly and both APK artifact uploads.

Production character-speech integration remains intentionally deferred until physical reliability work shows it is safe.

## Next: M6 Tomorrow Contract + Prepared Wake Plan

M6 adds the first sensitive personalized morning content while preserving the alarm trust boundary.

Planned scope:

- optional night-before Tomorrow Contract text;
- credential-protected local storage;
- typed/versioned Prepared Wake Plan;
- deterministic local preparation;
- deferrable WorkManager preparation only;
- local prepared safe lines/audio where useful;
- checksum/version validation and deterministic fallback;
- no Tomorrow Contract/private content in Direct-Boot Critical Wake Snapshot;
- offline-at-wake behavior remains complete.

Prepared content is presentation/context enrichment. It never owns Wake Runtime policy/state or Alarm Kernel authority.

## Privacy boundaries

Critical/reliability/motion/character operational paths must never persist private content in device-protected storage.

The following remain prohibited from the Critical Wake Snapshot and reliability logs:

- Tomorrow Contract raw text;
- calendar content;
- transcripts or microphone audio;
- prompts;
- secrets/tokens;
- private generated speech;
- raw accelerometer / rotation / gravity / gyroscope streams.

Derived motion evidence may contain only technical evidence type, monotonic timing, bounded derived reason and sensor-source availability.

M5 character rendering consumes typed `SpeechIntent` plus a non-sensitive render key and retains no transcript history.

M6 private content must remain credential-protected and optional. A pre-unlock wake must fall back to generic local content.

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
| M4 Motion evidence | **Merged / isolated evidence layer complete; physical calibration open** |
| M5 Alfred local experience | **Merged / complete, PR #17** |
| M6 Tomorrow Contract | **Next** |
| M7 Wake Learning v0 | Not started |
| M8 Voice architecture spike | Not started |
| M9 Realtime conversation | Not started |
| M10 Useful context | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Create the M6 issue/branch from current `main`.
2. Define pure Tomorrow Contract / Prepared Wake Plan models and invariants in `:wake-core`.
3. Add credential-protected local persistence without contaminating Direct-Boot critical storage.
4. Add deterministic preparation + WorkManager orchestration with checksum/fallback behavior.
5. Build a concise night-before editing/preview flow in the founder app.
6. Prove preparation and wake-time plan loading work with network absent.
7. Continue physical M2 founder-device scenarios when a device execution path is available.
8. Keep production wiring of motion/character/prepared personalization behind the reliability gate.

## Cloud / future stack status

Vercel remains the preferred future non-critical web/API host. Supabase remains the preferred managed PostgreSQL/Auth/Storage platform when cloud features become necessary.

Neither is required for M0–M7 local wake authority.

Realtime voice transport remains an M8 measured decision after deterministic local behavior and Wake Learning v0.

## Current blocker

There is no blocker to continuing isolated local-domain/product development.

The main unresolved proof boundary is environmental: **physical Android device evidence** is still required before Wake My Way can honestly claim production-grade alarm reliability or before richer adaptive behavior is allowed to endanger the trust-critical path.

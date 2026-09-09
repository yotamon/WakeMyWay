# Project status

**Last updated:** 2026-09-09  
**Product:** Wake My Way (WMW)  
**Platform:** Android first  
**Current engineering phase:** M6 Tomorrow Contract + Prepared Wake Plan in review, while M2 physical-device reliability evidence remains open  
**Current implementation branch:** `m6/tomorrow-contract-prepared-plan`  
**Current PR:** #20  
**Current issue:** #19

## Executive status

Wake My Way is in active native Android development.

Merged into `main`:

- M0 Foundation
- M1 Deep Alarm Kernel + Active Wake Execution
- M2 automated reliability harness + dedicated emulator lane
- M3 deterministic Wake Runtime
- M4 bounded Motion Evidence extraction + thin Android sensor adapter
- M5 Alfred deterministic local character + offline-only Android speech lab

M6 is implemented as a PR candidate. It introduces private night-before context and deterministic local morning preparation without moving any new authority or startup dependency into the critical alarm path.

**No physical-device reliability percentile claim has been made.** Emulator/device-test evidence is useful, but it does not prove real locked-screen audio latency, Doze, reboot-before-unlock, OEM power management, Android 17 physical behavior, or coexistence of critical alarm audio with optional character speech.

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

Cloud, Vercel, Supabase, AI, WorkManager, Tomorrow Contract and Prepared Wake Plan do not participate in alarm delivery. WorkManager's default App Startup initializer is removed, so M6 deferrable preparation does not initialize ahead of a cold-start `AlarmReceiver`.

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

Current thresholds remain tuning hypotheses until calibrated on physical devices.

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
```

Character speech remains a Wake Alarm Lab capability and does not own critical alarm audio.

### M6 private preparation path

```text
next Wake Occurrence
        ↓
Tomorrow Contract
        ↓
WakePreparationManager
        ├─ credential-protected noBackupFilesDir
        ├─ AtomicFile persistence
        ├─ deterministic local preparation
        ├─ SHA-256 integrity validation
        └─ on-demand WorkManager refresh
        ↓
Prepared Wake Plan
        ↓
valid + user/keyguard unlocked
        → optional WakeActivity text enrichment

missing / stale / corrupt / locked / Direct Boot
        → generic local wake UI
```

Private prepared content is presentation enrichment only. It never owns Wake Runtime policy/state, alarm firing, Stop, Snooze, Wake Ready, or Active Wake Execution.

## Implemented and merged milestones

### M0 Foundation

- native Android project;
- physical modules remain `:app`, `:wake-core`, `:benchmark`;
- pure Kotlin Wake Schedule / Wake Occurrence domain;
- deterministic recurrence and DST behavior;
- GitHub Actions for docs, domain tests, Android lint/compile, APK assembly and artifacts.

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

### M1 Deep Alarm Kernel

Merged in PR #8.

Implemented:

- `AlarmManager.setAlarmClock()` exact registration;
- foreground Active Wake Execution using alarm-appropriate local audio;
- dedicated Direct-Boot-aware `WakeActivity`;
- versioned atomic device-protected Critical Wake Snapshot;
- crash-safe schedule replacement and cancellation tombstone;
- stale occurrence rejection;
- idempotent active playback recovery;
- durable Stop and Snooze replacement-before-stop semantics;
- boot / locked-boot / time / timezone / package reconciliation;
- exact-alarm capability degradation;
- non-sensitive wake timing trace.

### M2 Reliability Harness

PR #10 merged the Wake Alarm Lab/reliability slice. PR #11 merged the dedicated Android device-test workflow.

Automated evidence includes:

- founder T+2m Wake Alarm Lab;
- bounded reliability history;
- target/receiver/foreground/audio/UI/terminal timing facts;
- service recovery/reconciliation evidence;
- sanitized shareable reports;
- Android instrumentation coverage;
- dedicated API-36 emulator workflow.

Issue #9 remains open for physical-device evidence covering locked-screen delivery, real audible latency, Doze, process/service recreation, reboot-before-unlock, time/timezone repair, capability loss/restoration, OEM power behavior and audio coexistence.

### M3 Wake Runtime

Merged in PR #13.

Implemented in pure Kotlin:

- durable phases `ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED`;
- typed Wake Inputs and Wake Directives;
- versioned Wake Policy;
- internal Activation Evidence authority;
- deterministic escalation;
- capability degradation;
- durable Stop/Snooze handshakes;
- replay and terminal invariants.

### M4 Motion Evidence

Merged in PR #15. See [`implementation/m4-motion-evidence.md`](implementation/m4-motion-evidence.md).

Implemented:

- pure bounded `MotionEvidenceExtractor`;
- pickup, orientation-change and sustained-movement evidence;
- ephemeral rolling state only;
- correlated-evidence protection;
- conservative sensor fallback;
- thin Android sensor observer;
- no raw sensor persistence.

Production thresholds remain uncalibrated and reliability-gated.

### M5 Alfred local character

Merged in PR #17. See [`implementation/m5-alfred-local.md`](implementation/m5-alfred-local.md).

Implemented:

- versioned deterministic character model;
- Alfred v1 curated copy for current `SpeechIntent`s;
- bounded escalation language and semantic guardrails;
- offline voice selection;
- Android local TextToSpeech adapter with explicit silent fallback;
- founder character preview/diagnostics;
- no network, AI, microphone or transcript persistence.

Production speech integration remains physical-reliability gated.

## M6 in review: Tomorrow Contract + Prepared Wake Plan

Tracks issue #19 and PR #20. Canonical implementation note: [`implementation/m6-tomorrow-contract.md`](implementation/m6-tomorrow-contract.md).

Implementation candidate includes:

- pure/versioned `TomorrowContract` and `PreparedWakePlan` models in `:wake-core`;
- contract and First Move bounds;
- deterministic local preparation using the existing Alfred Orientation renderer;
- source occurrence/revision binding;
- SHA-256 integrity checksum over canonical prepared content;
- explicit stale/wrong-occurrence/unsupported/corrupt validation failure;
- credential-protected private persistence under `noBackupFilesDir`;
- `AtomicFile` replacement semantics;
- explicit rejection of device-protected Contexts;
- AndroidX WorkManager 2.11.2 for redundant deferrable local refresh only;
- removal of WorkManager's default App Startup initializer;
- `WakeMyWayApplication : Configuration.Provider` for explicit on-demand WorkManager initialization;
- in-process serialization of UI/worker two-file commits;
- founder Tomorrow Contract edit/clear/preview flow;
- local offline wake-time plan read/fallback diagnostics;
- unlocked-only Prepared Wake Plan enrichment in `WakeActivity`;
- no private prepared text read/rendered during Direct Boot or while keyguard is locked;
- `FLAG_SECURE` when private morning text is visible;
- pure preparation tests including occurrence/revision/tamper validation;
- Android persistence/privacy/fail-closed/on-demand-WorkManager instrumentation tests.

Not introduced:

- cloud/backend preparation;
- account/auth dependency;
- prepared private content in Critical Wake Snapshot;
- WorkManager alarm firing or cold-start initialization dependency;
- TTS/private prepared speech in production Active Wake Execution;
- Wake Runtime policy/state inside the plan.

M6 is not considered merged/complete until PR #20 CI and review are green. The dedicated API-36 emulator lane is manual for this branch and must be executed separately before merge if the connected GitHub Actions interface permits it; otherwise the limitation must be recorded rather than silently claimed.

## Privacy boundaries

The Critical Wake Snapshot and reliability logs must never contain:

- Tomorrow Contract raw text;
- calendar content;
- transcripts or microphone audio;
- prompts;
- secrets/tokens;
- private generated speech;
- raw high-frequency motion streams.

M6 private state is credential-protected and excluded from Auto Backup through `noBackupFilesDir`. Pre-unlock wake remains generic and locally actionable.

## Milestone status

| Milestone | Status |
|---|---|
| Discovery / product definition | Complete v1 |
| UX psychology / flows | Complete v1 |
| Brand direction | Complete v1 |
| Architecture review / plan hardening | Complete |
| M0 Foundation | **Merged / complete** |
| M1 Deep Alarm Kernel + Active Wake Execution | **Merged / implementation complete** |
| M2 Reliability Harness | **Automated harness + emulator lane merged; physical-device evidence open (#9)** |
| M3 Wake Runtime | **Merged / pure runtime complete** |
| M4 Motion Evidence | **Merged / isolated evidence layer; physical calibration open** |
| M5 Alfred local experience | **Merged / complete, PR #17** |
| M6 Tomorrow Contract | **In review, PR #20** |
| M7 Wake Learning v0 | Not started |
| M8 Voice architecture spike | Not started |
| M9 Realtime conversation | Not started |
| M10 Useful context | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Make PR #20 fully green across docs, pure tests, Android lint/compile and instrumentation compilation.
2. Execute the API-36 emulator instrumentation lane for the M6 head if an Actions dispatch path is available; otherwise record the execution limitation without treating compilation as execution.
3. Resolve any M6 review/CI defects without weakening the private/critical storage boundary.
4. Merge M6 and advance canonical status to M7 Wake Learning v0.
5. Begin M7 as bounded, local, explainable off-session policy derivation only.
6. In parallel, continue issue #9 physical-device reliability scenarios when a real device execution path is available.
7. Keep character speech, motion thresholds and richer prepared personalization behind the physical reliability gate.

## Cloud / future stack status

Vercel remains the preferred future non-critical web/API host. Supabase remains the preferred managed PostgreSQL/Auth/Storage platform when a real cloud capability requires them.

Neither is required for M0–M7 local wake authority.

Realtime voice transport remains an M8 measured decision after deterministic local behavior and Wake Learning v0.

## Current blockers / risks

There is no blocker to isolated local product/domain development.

Open proof/risk boundaries:

- physical Android reliability evidence (#9);
- motion threshold calibration on real devices;
- coexistence of critical alarm audio with optional local character TTS;
- final M6 CI and emulator execution evidence before merge.

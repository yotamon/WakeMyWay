# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** M7 Wake Learning v0 is next; M2 physical-device reliability evidence remains open  
**Current implementation branch:** `feat/vercel-ai-platform`  
**Current PR:** #23  
**Current side-track:** optional Vercel AI platform foundation, with no Android wake-path dependency

## Executive status

Wake My Way is in active native Android development.

Merged into `main`:

- M0 Foundation
- M1 Deep Alarm Kernel + Active Wake Execution
- M2 automated reliability harness + dedicated emulator lane
- M3 deterministic Wake Runtime
- M4 bounded Motion Evidence extraction + thin Android sensor adapter
- M5 Alfred deterministic local character + offline-only Android speech lab
- M6 Tomorrow Contract + Prepared Wake Plan

PR #23 introduces an isolated `apps/cloud` foundation for future non-critical AI work using Vercel AI SDK 7 + AI Gateway. It does **not** connect Android to cloud AI, move M7 learning to the backend, or select the M9 realtime transport. ADR-008 remains the authority for the M8 measured voice spike; ADR-016 records the default cloud AI access layer.

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

Cloud, Vercel, Supabase, AI, WorkManager, Tomorrow Contract and Prepared Wake Plan do not participate in alarm delivery. WorkManager's default App Startup initializer is removed, so deferrable preparation does not initialize ahead of a cold-start `AlarmReceiver`.

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

### Private preparation path

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

### Optional cloud AI foundation

```text
future non-critical WMW feature
             ↓
       apps/cloud
             ↓
      AI platform
      ├─ fast/smart text policy
      ├─ structured outputs
      ├─ embeddings
      ├─ STT / TTS
      └─ M8 realtime token spike
             ↓
   Vercel AI SDK 7
             ↓
   Vercel AI Gateway
```

There is currently no Android production call to this service. Provider/network/cloud failure therefore cannot block a wake attempt. See [`implementation/vercel-ai-platform.md`](implementation/vercel-ai-platform.md) and ADR-016.

## Implemented and merged milestones

### M0 Foundation

- native Android project;
- physical Android modules remain `:app`, `:wake-core`, `:benchmark`;
- pure Kotlin Wake Schedule / Wake Occurrence domain;
- deterministic recurrence and DST behavior;
- GitHub Actions for docs, domain tests, Android lint/compile, APK assembly and artifacts.

Current Android toolchain:

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

### M6 Tomorrow Contract + Prepared Wake Plan

Merged in PR #20. Canonical implementation note: [`implementation/m6-tomorrow-contract.md`](implementation/m6-tomorrow-contract.md).

Implemented:

- pure/versioned `TomorrowContract` and `PreparedWakePlan` models in `:wake-core`;
- contract and First Move bounds;
- deterministic local preparation using the existing Alfred Orientation renderer;
- source occurrence/revision binding;
- SHA-256 integrity checksum over canonical prepared content;
- explicit stale/wrong-occurrence/unsupported/corrupt validation failure;
- credential-protected private persistence under `noBackupFilesDir`;
- `AtomicFile` replacement semantics;
- explicit rejection of device-protected Contexts;
- AndroidX WorkManager for redundant deferrable local refresh only;
- removal of WorkManager's default App Startup initializer;
- explicit on-demand WorkManager initialization;
- serialized UI/worker two-file commits;
- founder Tomorrow Contract edit/clear/preview flow;
- local offline wake-time plan read/fallback diagnostics;
- unlocked-only Prepared Wake Plan enrichment in `WakeActivity`;
- no private prepared text read/rendered during Direct Boot or while keyguard is locked;
- `FLAG_SECURE` when private morning text is visible;
- pure preparation tests plus Android persistence/privacy/fail-closed instrumentation coverage.

M6 did not introduce cloud/backend preparation, account/auth dependency, private content in Critical Wake Snapshot, WorkManager alarm firing, private production TTS, or Wake Runtime state inside the plan.

## Optional AI platform foundation in PR #23

The current side-track adds:

- isolated framework-less Vercel Functions service under `apps/cloud`;
- Vercel AI SDK 7 + AI Gateway as the default optional cloud model layer;
- centralized `fast` and `smart` model policy with cross-provider fallbacks;
- language generation, streaming and server-owned structured outputs;
- embeddings, transcription and speech generation;
- short-lived realtime credential minting for M8 experiments only;
- operator-only diagnostic endpoints with bounded request sizes;
- no generic prompt/transcript/audio persistence;
- metadata-only error logging;
- strict TypeScript/unit-test CI;
- ADR-016 plus implementation documentation.

This foundation is not a roadmap milestone completion. M7 remains local/offline and M8 remains evidence-gated.

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

The optional cloud service has no persistence/database dependency in PR #23 and must not log prompts, transcripts, raw audio or generated private speech. Its operator key must never be embedded in Android.

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
| M6 Tomorrow Contract | **Merged / complete, PR #20** |
| Optional Vercel AI platform foundation | **In review, PR #23; no Android dependency** |
| M7 Wake Learning v0 | **Next roadmap milestone** |
| M8 Voice architecture spike | Not started |
| M9 Realtime conversation | Not started |
| M10 Useful context | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Make PR #23 green on strict TypeScript/unit tests and existing repository checks; fix SDK/runtime mismatches rather than weakening types.
2. Merge PR #23 only if the cloud boundary remains non-critical and no Android operator credential is introduced.
3. Begin M7 as bounded, local, explainable off-session Wake Policy derivation with immutable per-session policy versions.
4. In parallel, continue issue #9 physical-device reliability scenarios when a real-device execution path is available.
5. Keep character speech, motion thresholds and richer prepared personalization behind the physical reliability gate.
6. At M8, benchmark realtime transport candidates and use the new Vercel AI platform only as one candidate/control plane, not as a preselected transport.

## Cloud / future stack status

Vercel is the preferred non-critical cloud/web host. PR #23 establishes `apps/cloud` as the isolated implementation root and AI SDK + AI Gateway as the default cloud AI access layer.

Supabase remains the preferred managed PostgreSQL/Auth/Storage platform when a real persistence or identity capability requires it. No Supabase dependency is added by the AI platform foundation.

Neither Vercel nor Supabase is required for local wake authority or M7 Wake Learning v0.

Realtime voice transport remains an M8 measured decision after deterministic local behavior and Wake Learning v0.

## Current blockers / risks

There is no blocker to isolated local product/domain development.

Open proof/risk boundaries:

- physical Android reliability evidence (#9);
- motion threshold calibration on real devices;
- coexistence of critical alarm audio with optional local character TTS;
- PR #23 cloud type/runtime validation and eventual authenticated Android-facing API design;
- realtime voice latency/transport/provider choice remains unproven until M8.

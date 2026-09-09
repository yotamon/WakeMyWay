# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** M7 Wake Learning v0 core merged; live Android integration remains reliability-gated  
**Current implementation branch:** `main`  
**Current PR:** none  
**Current integration track:** issue #27, sequenced after physical reliability gate #9  
**Current side-track:** optional Vercel AI platform foundation merged in PR #23; no Android wake-path dependency

## Executive status

Wake My Way is in active native Android development.

Merged into `main`:

- M0 Foundation
- M1 Deep Alarm Kernel + Active Wake Execution
- M2 automated reliability harness + dedicated API-36 emulator lane
- M3 deterministic Wake Runtime
- M4 bounded Motion Evidence extraction + thin Android sensor adapter
- M5 Alfred deterministic local character + offline-only Android speech lab
- M6 Tomorrow Contract + Prepared Wake Plan
- M7 deterministic local Wake Learning v0 core (PR #26)
- optional Vercel AI SDK 7 + AI Gateway cloud foundation (PR #23)

PR #26 merged the first M7 pure-Kotlin adaptive loop: compact Wake Outcome derivation from real Wake Runtime replay, sparse calibration semantics, bounded deterministic policy derivation, annoyance/agency guardrails, versioned self-validating policy snapshots, fail-closed learned-policy resolution and explicit reset.

M7 is not yet connected to real morning sessions. The current `WakeActivity` still attaches directly to Active Wake Execution and is not the Android directive executor for `WakeRuntime`. That richer M3 Android integration remains intentionally behind the physical reliability gate. Issue #27 tracks the later local Wake journal, outcome persistence, calibration, learned-policy selection and Wake Lab inspection/reset work.

PR #23 established an isolated `apps/cloud` foundation for future non-critical AI work using Vercel AI SDK 7 + AI Gateway. It does **not** connect Android to cloud AI, move M7 learning to the backend, or select the M9 realtime transport.

Private text, structured generation and embeddings fail closed to Gateway routes that satisfy Zero Data Retention. Current default Gateway STT/TTS/realtime routes do not satisfy WMW's ZDR requirement and are disabled by default behind an explicit synthetic-spike gate.

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

Cloud, Vercel, Supabase, AI, WorkManager, Tomorrow Contract, Prepared Wake Plan and Wake Learning do not participate in alarm delivery.

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

Wake Runtime remains behavioral authority. Android adapters, characters, learning and future AI cannot calculate a separate confidence score or overrule it.

### Off-session learning path

```text
finished typed Wake timeline
          +
optional calibration / friction feedback
          ↓
   WakeOutcomeSummary
          ↓
      WakeLearning
          ↓
 versioned WakePolicySnapshot
          ↓
 future Wake Session only
```

Wake Learning never mutates an active Wake Session and cannot learn Alarm Kernel delivery, safety, privacy or critical execution boundaries.

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

Character speech remains a Wake Alarm Lab capability and does not own critical alarm audio. Alfred currently has explicit re-engagement copy through level 3 and safely clamps higher runtime escalation levels to the firmest existing wording.

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
      ├─ STT / TTS spike adapters
      └─ M8 realtime token spike
             ↓
   Vercel AI SDK 7
             ↓
   Vercel AI Gateway
```

There is currently no Android production call to this service. Provider/network/cloud failure therefore cannot block a wake attempt. See [`implementation/vercel-ai-platform.md`](implementation/vercel-ai-platform.md) and ADR-016.

## Implemented milestones

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

Implemented exact `setAlarmClock()` registration, foreground Active Wake Execution, bundled local alarm audio, Direct-Boot-aware wake UI, atomic device-protected Critical Wake Snapshot, durable Stop/Snooze, stale occurrence rejection, boot/time/timezone/package reconciliation, exact-alarm capability degradation and non-sensitive timing traces.

### M2 Reliability Harness

PR #10 merged the Wake Alarm Lab/reliability slice. PR #11 merged the dedicated Android device-test workflow.

Automated evidence includes founder T+2m tests, bounded timing history, target/receiver/foreground/audio/UI/terminal timing facts, recovery/reconciliation evidence, sanitized reports, instrumentation coverage and a dedicated API-36 emulator workflow.

Issue #9 remains open for physical-device evidence covering locked-screen delivery, real audible latency, Doze, process/service recreation, reboot-before-unlock, time/timezone repair, capability loss/restoration, OEM power behavior and audio coexistence.

### M3 Wake Runtime

Merged in PR #13.

Pure Kotlin implementation includes durable phases `ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED`, typed Wake Inputs/Directives, versioned Wake Policy, internal Activation Evidence authority, deterministic escalation, capability degradation, durable Stop/Snooze handshakes, replay and terminal invariants.

The pure runtime is complete, but its richer Android directive-executor integration remains intentionally reliability-gated. The production `WakeActivity` is not yet a parallel behavioral authority.

### M4 Motion Evidence

Merged in PR #15. See [`implementation/m4-motion-evidence.md`](implementation/m4-motion-evidence.md).

Implemented bounded pickup/orientation/sustained-movement extraction, ephemeral rolling state, correlated-evidence protection, conservative sensor fallback, a thin Android sensor observer and no raw sensor persistence. Production thresholds remain uncalibrated and reliability-gated.

### M5 Alfred local character

Merged in PR #17. See [`implementation/m5-alfred-local.md`](implementation/m5-alfred-local.md).

Implemented a versioned deterministic character model, curated Alfred v1 copy, bounded escalation language, offline voice selection, local Android TTS with silent fallback, founder preview/diagnostics and no network/AI/microphone/transcript persistence. Production speech integration remains physical-reliability gated.

### M6 Tomorrow Contract + Prepared Wake Plan

Merged in PR #20. Canonical implementation note: [`implementation/m6-tomorrow-contract.md`](implementation/m6-tomorrow-contract.md).

Implemented pure/versioned contract and plan models, deterministic local preparation, occurrence/revision binding, SHA-256 integrity validation, credential-protected `noBackupFilesDir` storage, atomic replacement, on-demand WorkManager refresh, founder edit/clear/preview flow, local wake-time fallback diagnostics, unlocked-only enrichment and `FLAG_SECURE` while private morning text is visible.

M6 did not introduce cloud/backend preparation, account/auth dependency, private content in the Critical Wake Snapshot, WorkManager alarm firing, private production TTS or Wake Runtime state inside the plan.

### M7 Wake Learning v0 core

Merged in PR #26. Canonical implementation note: [`implementation/m7-wake-learning-v0.md`](implementation/m7-wake-learning-v0.md).

Implemented in pure Kotlin:

- `WakeOutcomeSummary` derived by replaying timestamped semantic inputs through the real Wake Runtime;
- Activation Completion timing at the actual transition into `ORIENTING`;
- optional `GOT_UP`, `RETURNED_TO_BED`, `GOT_UP_LATER`, `SKIPPED` calibration semantics;
- missing calibration remains unknown rather than implicit success;
- annoyance and perceived-agency feedback;
- deterministic learning from only the current immutable policy version;
- v0 learnable surface limited to runtime-effective `activationThreshold` and `maxEscalationLevel`;
- minimum-evidence/hysteresis rules;
- one parameter / one bounded step per derivation;
- safe ranges plus friction/agency guardrails;
- human-readable explanation for every change or non-change;
- full-source, self-validating policy snapshots that reject undeclared changes;
- fail-closed learned-policy resolution and explicit reset;
- deterministic tests for false-positive activation, incomplete activation, excess friction, ordering, fallback and snapshot integrity.

The critical false-positive fixture is covered: Activation Completion can be true while calibration says `RETURNED_TO_BED`; the learner treats that as evidence that the operational activation criterion may be too permissive rather than calling the morning a Confirmed Wake Success.

M7 is not yet product-integrated. Issue #27 tracks local semantic timeline/outcome persistence, calibration collection, learned-policy storage/application and Wake Lab inspection/reset. That work is sequenced after the M3 Android runtime path is safe to connect under the physical reliability gate in #9.

## Optional AI platform foundation

Merged in PR #23.

Implemented an isolated framework-less Vercel Functions service under `apps/cloud`, Vercel AI SDK 7 + AI Gateway, centralized fast/smart model policy, structured generation, streaming, embeddings, STT/TTS/realtime spike adapters, strict request bounds, metadata-only error logging, and no generic prompt/transcript/audio persistence.

Private text/embedding calls require ZDR and fail closed. STT/TTS/realtime remain disabled by default for private wake data because the current selected Gateway audio models do not provide the required ZDR guarantee.

## Privacy boundaries

The Critical Wake Snapshot and reliability logs must never contain Tomorrow Contract raw text, calendar content, transcripts/microphone audio, prompts, secrets/tokens, private generated speech or raw high-frequency motion streams.

M6 private state is credential-protected and excluded from Auto Backup through `noBackupFilesDir`. Pre-unlock wake remains generic and locally actionable.

M7 consumes compact semantic outcomes and optional structured calibration/friction feedback. It does not require raw audio, transcripts, raw high-frequency motion streams, Tomorrow Contract text or cloud identity.

The optional cloud service has no persistence/database dependency and must not log prompts, transcripts, raw audio or generated private speech. Its operator key must never be embedded in Android.

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
| M3 Wake Runtime | **Pure runtime merged; Android execution integration reliability-gated** |
| M4 Motion Evidence | **Merged / isolated evidence layer; physical calibration open** |
| M5 Alfred local experience | **Merged / complete core, PR #17; production integration gated** |
| M6 Tomorrow Contract | **Merged / complete, PR #20** |
| Optional Vercel AI platform foundation | **Merged / complete, PR #23; no Android dependency** |
| M7 Wake Learning v0 | **Core merged, PR #26; live integration tracked by #27 and gated by #9** |
| M8 Voice architecture spike | Not started |
| M9 Realtime conversation | Not started |
| M10 Useful context | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Continue issue #9 physical-device reliability evidence so the deferred M3 Android Wake Runtime/directive-executor path can be connected without weakening the Alarm Kernel.
2. Once that gate is satisfied, implement issue #27: authoritative local timed semantic Wake journal, compact Wake Outcome history, occasional calibration/friction attachment, fail-closed learned-policy persistence/selection for future sessions, and Wake Lab inspect/reset tooling.
3. Calibrate M7's initial evidence thresholds and safe ranges through deterministic fixtures and later real dogfood. The current numbers are engineering hypotheses, not product truth.
4. Keep character speech, motion thresholds and richer prepared personalization behind the physical reliability gate.
5. Keep optional cloud services out of wake authority; deploy/connect them only when a concrete non-critical feature justifies the API/auth/privacy boundary.
6. At M8, benchmark realtime transport candidates and use the Vercel AI platform as one candidate/control plane, not as a preselected transport. Privacy eligibility is part of acceptance.

## Cloud / future stack status

Vercel is the preferred non-critical cloud/web host. PR #23 established `apps/cloud` as the isolated implementation root and AI SDK + AI Gateway as the default optional cloud AI access layer.

Private cloud text/embedding use requires a Vercel environment/plan that can enforce the configured ZDR policy. Current selected Gateway STT/TTS/realtime models remain unsuitable for private wake data because they do not provide WMW's required ZDR guarantee.

Supabase remains the preferred managed PostgreSQL/Auth/Storage platform when a real persistence or identity capability requires it. No Supabase dependency is currently required by M7.

Neither Vercel nor Supabase is required for local wake authority or Wake Learning v0.

Realtime voice transport remains an M8 measured decision after deterministic local behavior and Wake Learning v0.

## Current blockers / risks

There is no blocker to isolated local product/domain development.

Open proof/risk boundaries:

- physical Android reliability evidence (#9) is still required before richer live Wake Runtime/character/learning integration;
- the initial M7 evidence thresholds and safe ranges are engineering hypotheses until dogfood calibration;
- M7 local journal/persistence/application wiring is intentionally not implemented yet (#27);
- motion threshold calibration on real devices remains open;
- coexistence of critical alarm audio with optional local character TTS remains unproven on representative physical devices;
- future Android-facing cloud APIs need real installation/account/session authorization before product use;
- private Gateway use depends on an environment/plan that supports the configured ZDR policy;
- current selected Gateway audio/realtime routes do not meet WMW's ZDR requirement;
- realtime voice latency/transport/provider choice remains unproven until M8.

# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** M8 voice architecture spike in progress; provider-neutral harness and first direct WebRTC candidate merged  
**Current product-design track:** Adaptive Dawn production UI foundation merged in PR #30  
**Current implementation branch:** `main`  
**Current integration track:** issue #27, sequenced after physical reliability gate #9  
**Current M8 track:** issue #28; measurement harness PR #29 and direct OpenAI WebRTC candidate PR #31 merged; comparative physical-device evidence remains open  
**Current side-track:** optional Vercel AI platform foundation merged in PR #23; no Android wake-path dependency

## Executive status

Wake My Way is in active native Android development.

Implemented/merged:

- M0 Foundation
- M1 Deep Alarm Kernel + Active Wake Execution
- M2 automated reliability harness + dedicated API-36 emulator lane
- M3 deterministic Wake Runtime
- M4 bounded Motion Evidence extraction + thin Android sensor adapter
- M5 Alfred deterministic local character + offline-only Android speech lab
- M6 Tomorrow Contract + Prepared Wake Plan
- M7 deterministic local Wake Learning v0 core (PR #26)
- optional Vercel AI SDK 7 + AI Gateway cloud foundation (PR #23)
- Adaptive Dawn production design foundation (PR #30)
- M8 provider-neutral voice measurement harness (PR #29)
- M8 direct OpenAI Realtime WebRTC debug candidate (PR #31; synthetic-only)

PR #30 established the first production product-design foundation without changing alarm authority. It introduced the Adaptive Dawn visual system, centralized Compose tokens, semantic WMW components, a native geometric WMW Presence, stable Navigation 3 for normal app destinations, a polished Tonight product home, a rebuilt presentation-only Wake surface, canonical previews and a dark pre-Compose window baseline that avoids bright cold-start flashes.

The design work is intentionally a **parallel presentation track**, not a new behavioral roadmap milestone. M7/M8 numbering and reliability gates remain unchanged.

PR #26 merged the first M7 pure-Kotlin adaptive loop: compact Wake Outcome derivation from real Wake Runtime replay, sparse calibration semantics, bounded deterministic policy derivation, annoyance/agency guardrails, versioned self-validating policy snapshots, fail-closed learned-policy resolution and explicit reset.

M7 is not yet connected to real morning sessions. The current production wake path is still reliability-gated before richer Wake Runtime/directive execution, character speech, learned policy application or state-responsive wake presentation can become authoritative live behavior. Issue #27 tracks the later local Wake journal, outcome persistence, calibration, learned-policy selection and Wake Lab inspection/reset work.

PR #23 established an isolated `apps/cloud` foundation for future non-critical AI work using Vercel AI SDK 7 + AI Gateway. It does **not** connect Android production wake behavior to cloud AI, move M7 learning to the backend, or select a realtime transport.

Private text, structured generation and embeddings fail closed to Gateway routes that satisfy Zero Data Retention. Current default Gateway STT/TTS/realtime routes do not satisfy WMW's ZDR requirement and are disabled by default behind an explicit synthetic-spike gate.

PR #29 added the provider-neutral M8 evidence contract: deterministic p50/p95 summaries, failure-stage accounting, network/audio-route coverage, cost evidence, privacy eligibility and objective operational-footprint facts. It deliberately produces comparison readiness and missing-evidence reasons, never an automatic winner.

PR #31 added the first runnable M8 transport candidate. A founder-only Android debug lab obtains a short-lived OpenAI Realtime client secret through a WMW broker and exchanges SDP directly with OpenAI over WebRTC. The standard OpenAI API key remains server-side. The RTC dependency and microphone/network permissions are debug-only, event payloads are not persisted, and the configuration remains `synthetic-only` until the exact OpenAI project/account data-control posture is independently verified.

PR #31 passed Cloud typecheck/tests, Android core tests/lint/instrumentation compilation/debug APK assembly, and the dedicated API-36 emulator reliability workflow before merge. These checks prove build/integration compatibility, not physical-device realtime performance.

**No physical-device reliability percentile claim has been made.** Emulator/device-test evidence is useful, but it does not prove real locked-screen audio latency, Doze, reboot-before-unlock, OEM power management, Android 17 physical behavior, coexistence of critical alarm audio with optional character speech, or M8 realtime quality under physical network/audio-route changes.

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

Cloud, Vercel, Supabase, AI, WorkManager, Tomorrow Contract, Prepared Wake Plan, Wake Learning and visual/navigation frameworks do not participate in alarm delivery.

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

Wake Runtime remains behavioral authority. Android adapters, UI, characters, learning and future AI cannot calculate a separate confidence score or overrule it.

### Presentation path

```text
normal application
      ↓
 Navigation 3
      ↓
 Tonight / setup / history / settings / developer lab

active Wake Occurrence
      ↓
 dedicated WakeActivity
      ↓
 Adaptive Dawn presentation
```

`WakeActivity` is deliberately **not** a normal Navigation 3 destination. Its visual richness may degrade without changing Active Wake Execution, Stop, Snooze or Wake Runtime authority.

The UX consciousness language `Emerging → Engaged → Active → Oriented` remains a presentation/cognition model only. It must not become a second behavioral state machine.

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

Character speech remains a Wake Alarm Lab capability and does not own critical alarm audio. Production integration remains physical-reliability gated.

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

Private prepared content is presentation enrichment only. PR #30 preserves the existing user-unlocked/keyguard gate and `FLAG_SECURE` protection while changing the visual shell.

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
      └─ M8 realtime token experiments
             ↓
   Vercel AI SDK 7 / direct provider spike adapters
```

There is currently no Android production call to this service. Provider/network/cloud failure therefore cannot block a wake attempt.

### M8 direct realtime spike path

```text
Android debug Voice Spike Lab
            ↓
 WMW internal operator authorization
            ↓
WMW direct OpenAI client-secret broker
            ↓
server-only OPENAI_API_KEY
            ↓
OpenAI /v1/realtime/client_secrets
            ↓
short-lived ephemeral credential
            ↓
Android debug Voice Spike Lab
            ↓
SDP to OpenAI /v1/realtime/calls
            ↓
WebRTC media + bounded event-type metadata
```

This path is an engineering measurement surface only. It is not an alarm dependency, does not own Wake Runtime state, and remains `synthetic-only` until explicit privacy evidence exists.

## Current Android toolchain

```text
Android Gradle Plugin    9.4.0
Kotlin                   2.4.20
Gradle                   9.6.1
JDK                      17
Compose BOM              2026.08.00
compileSdk               37
targetSdk                36
minSdk                   29
Navigation 3             1.1.7
AndroidX graphics-shapes 1.1.0
WebRTC spike             150.7871.01 (debug-only M8)
```

## Product design foundation

Canonical implementation/design note: [`implementation/product-design-foundation.md`](implementation/product-design-foundation.md).

Merged in PR #30:

- **Adaptive Dawn** as the canonical production visual direction;
- centralized WMW color, spacing, size and motion tokens;
- expanded Material 3 typography and shape system;
- `WmwCircadianSurface` for local, presentation-only dawn illumination;
- semantic components such as time display, status pill, card and critical actions;
- native `WmwPresence` built with AndroidX `graphics-shapes` rather than an AI-orb/robot metaphor;
- stable Navigation 3 for normal app destinations;
- typed/saveable navigation keys with Kotlin serialization;
- Tonight as the normal `MainActivity` product surface;
- Wake Alarm Lab preserved as a developer destination;
- `WakeActivity` rebuilt visually while preserving Alarm Kernel Stop/Snooze calls, Direct Boot behavior, private-content gating and `FLAG_SECURE`;
- dark Android window baseline to prevent a bright frame before Compose renders;
- edge-to-edge normal app shell;
- resource-backed product-facing copy;
- canonical synthetic Compose previews for Tonight ready/empty and Wake Emerging.

Explicitly **not** introduced:

- Rive or Lottie as UI architecture;
- Haze/blur as required infrastructure;
- Coil/image-loading dependency;
- Vico before a concrete M7 history visualization exists;
- cloud/network rendering dependency;
- a separate design-system Gradle module;
- a second Wake state machine.

### Validation for PR #30

Passed before merge:

- documentation validation;
- `:wake-core:test`;
- Android lint / Compose compilation;
- instrumentation-test compilation;
- debug APK assembly;
- dedicated API-36 reliability instrumentation.

The first CI pass exposed one concrete compile defect in the initial WMW Presence implementation: the `graphics-shapes` `Morph.toPath` extension required an explicit import. The defect was corrected and the complete validation lane then passed before merge.

### Visual-regression next gate

Roborazzi remains the preferred selected Compose visual-regression tool. It is intentionally introduced only after canonical previews have been reviewed as stable design truth, so the repository does not encode arbitrary first-draft pixels as permanent goldens.

Visual fixtures must be synthetic and must never include private wake content.

## Implemented roadmap milestones

### M0 Foundation

Native Android project, minimal physical modules `:app`, `:wake-core`, `:benchmark`, deterministic schedule/recurrence/DST behavior and CI baseline.

### M1 Deep Alarm Kernel

Exact `setAlarmClock()` registration, foreground Active Wake Execution, bundled local alarm audio, Direct-Boot-aware wake UI, atomic device-protected Critical Wake Snapshot, durable Stop/Snooze, stale occurrence rejection and reconciliation.

### M2 Reliability Harness

Founder T+2m tests, bounded timing history, trigger/audio/UI/terminal facts, sanitized reports, instrumentation coverage and a dedicated API-36 emulator workflow. Physical-device evidence remains open in issue #9.

### M3 Wake Runtime

Pure Kotlin durable phases, typed Wake Inputs/Directives, versioned Wake Policy, internal Activation Evidence authority, deterministic escalation, capability degradation, durable Stop/Snooze handshakes and replay invariants.

### M4 Motion Evidence

Bounded pickup/orientation/sustained-movement extraction, correlated-evidence protection, thin Android sensor observer and no raw sensor persistence. Production thresholds remain uncalibrated.

### M5 Alfred local character

Versioned deterministic character model, curated Alfred copy, bounded escalation language, offline voice selection, Android TTS adapter with silent fallback and founder diagnostics. Production speech integration remains gated.

### M6 Tomorrow Contract + Prepared Wake Plan

Versioned contract/plan models, deterministic preparation, occurrence/revision binding, integrity validation, credential-protected atomic storage, on-demand WorkManager refresh and unlocked-only enrichment.

### M7 Wake Learning v0 core

Merged in PR #26. Implements deterministic compact outcome derivation, optional calibration, bounded policy adaptation, safe ranges, agency/friction guardrails, immutable/self-validating policy snapshots, fail-closed resolution and reset.

The critical false-positive fixture remains covered: Activation Completion can be true while calibration says `RETURNED_TO_BED`; this is not treated as Confirmed Wake Success.

### M8 Voice architecture spike

In progress under issue #28.

PR #29 merged the provider-neutral evidence harness. PR #31 merged the first runnable transport candidate: direct OpenAI Realtime over WebRTC from a debug-only Android founder lab with server-minted ephemeral credentials.

The current evidence state is intentionally incomplete. Before ADR-008 can choose an architecture, M8 still requires trustworthy first-audible-speech timing, repeated cold-connect samples, barge-in/reconnect measurements, Wi-Fi/mobile transition evidence, Bluetooth behavior, cost estimates, exact privacy/data-control evidence and at least one meaningful comparison configuration.

No M8 transport is production-selected yet.

## Milestone / track status

| Milestone / track | Status |
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
| M5 Alfred local experience | **Merged core; production integration gated** |
| M6 Tomorrow Contract | **Merged / complete** |
| Optional Vercel AI platform foundation | **Merged / complete, PR #23; no Android dependency** |
| M7 Wake Learning v0 | **Core merged, PR #26; live integration tracked by #27 and gated by #9** |
| Product design D0/D1 foundation | **Merged / complete, PR #30** |
| Product design visual-regression gate | **Next after canonical preview review** |
| Product design setup experience | Planned next product-UI slice |
| M8 Voice architecture spike | **In progress: harness PR #29 + direct OpenAI WebRTC candidate PR #31 merged; comparative measured evidence open (#28)** |
| M9 Realtime conversation | Not started |
| M10 Useful context | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Add a trustworthy M8 first-audible-speech measurement seam to the direct OpenAI debug lab, without inferring audible latency from protocol/control events or persisting raw audio/transcripts.
2. Run repeated synthetic physical-device direct-OpenAI measurements for cold connection, audible response, barge-in, reconnect, Wi-Fi/mobile, network transitions and Bluetooth; feed only metadata into the provider-neutral evidence contract.
3. Introduce a second realtime comparison configuration only when it can materially test a different operational shape; LiveKit remains the likely RTC-layer comparison if direct WebRTC interruption/reconnect complexity justifies it. Do not select a winner before comparison readiness.
4. Review canonical Tonight/Wake previews on representative device dimensions; then record selected Roborazzi baselines and add visual-regression CI.
5. Build the production setup/edit flow behind the Tonight shell: wake time, initial wake difficulty, character, Tomorrow Contract and Wake Readiness repair, without moving alarm authority into UI.
6. Continue issue #9 physical-device reliability evidence so the deferred M3 Android Wake Runtime/directive-executor path can be connected safely. Once that gate is satisfied, implement issue #27 local Wake journal/outcome/calibration/learned-policy wiring.
7. Keep character speech, motion thresholds, state-responsive Wake UI, learned policy application and optional realtime AI outside critical alarm authority until their separate gates are satisfied.

## Privacy boundaries

The Critical Wake Snapshot and reliability logs must never contain Tomorrow Contract raw text, calendar content, transcripts/microphone audio, prompts, secrets/tokens, private generated speech or raw high-frequency motion streams.

M6 private state is credential-protected and excluded from Auto Backup through `noBackupFilesDir`. Pre-unlock wake remains generic and locally actionable.

M7 consumes compact semantic outcomes and optional structured calibration/friction feedback. It does not require raw audio, transcripts, raw high-frequency motion streams, Tomorrow Contract text or cloud identity.

The product-design track adds no telemetry. Screenshot/visual fixtures are synthetic only. Private prepared wake content retains existing `FLAG_SECURE` handling.

M8 direct OpenAI experiments remain `synthetic-only`. The standard OpenAI API key stays server-side, Android receives only short-lived credentials, and the debug lab does not persist microphone audio, generated audio, transcripts, prompts, complete Realtime event payloads or private wake context. A successful connection is not privacy evidence.

## Current blockers / risks

There is no blocker to isolated local product/domain, product-design or M8 measurement development.

Open proof/risk boundaries:

- physical Android reliability evidence (#9) is still required before richer live Wake Runtime/character/learning integration;
- current product setup remains founder/lab-driven behind the new Tonight shell until the next UI slice;
- canonical visual previews exist, but reviewed screenshot goldens/visual-regression CI are not yet established;
- final bundled typography/icon assets remain a controlled later design decision rather than a remote dependency;
- the initial M7 evidence thresholds and safe ranges are engineering hypotheses until dogfood calibration;
- M7 local journal/persistence/application wiring is intentionally not implemented yet (#27);
- motion threshold calibration on real devices remains open;
- coexistence of critical alarm audio with optional local character TTS remains unproven on representative physical devices;
- future Android-facing cloud APIs need real installation/account/session authorization before product use;
- current selected Gateway audio/realtime routes do not meet WMW's ZDR requirement;
- M8 has a provider-neutral harness and one runnable direct WebRTC candidate, but first-audible latency, barge-in, reconnect, network transitions, Bluetooth, cost, exact privacy posture and a meaningful comparison configuration remain unproven.

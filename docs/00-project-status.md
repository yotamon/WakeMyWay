# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** production local voice-wake integration in PR #36; M8 realtime comparison remains a separate measurement track  
**Current product-design track:** Adaptive Dawn foundation + production setup + curated visual gate merged; premium Tonight refinement continues separately  
**Current implementation branch:** `feature/production-voice-wake-session`  
**Current integration track:** PR #36 connects M3/M4/M5 into the real morning WakeActivity while preserving Alarm Kernel authority  
**Current M8 track:** issue #28; provider-neutral harness PR #29 and direct OpenAI WebRTC candidate PR #31 merged; comparative physical-device evidence remains open  
**Current side-track:** optional Vercel AI platform foundation merged in PR #23; no critical wake-path dependency

## Executive status

Wake My Way is in active native Android development.

The product already has a local-first alarm core, deterministic wake behavior, motion evidence, a deterministic Alfred character, private Tomorrow Contract preparation, local Wake Learning v0, production setup UI, a curated visual-regression gate and a debug-only realtime measurement path.

PR #36 is the first production integration of the **local conversational wake**. It changes the real morning experience from “critical alarm + passive presentation” to:

```text
critical local alarm
      +
Alfred speaks locally
      ↓
Wake Runtime requests a reply
      ↓
on-device speech recognition
      ↓
typed VoiceResponseObserved evidence
      +
motion evidence
      ↓
Wake Runtime progression
      ↓
orientation / completion
```

The critical alarm still does not depend on voice, microphone, AI, network, WakeActivity, motion, Tomorrow Contract, Wake Learning or cloud infrastructure.

### Implemented / merged foundations

- M0 Foundation
- M1 Deep Alarm Kernel + Active Wake Execution
- M2 automated reliability harness + dedicated API-36 emulator lane
- M3 deterministic Wake Runtime
- M4 bounded Motion Evidence extraction + thin Android sensor adapter
- M5 Alfred deterministic local character + offline-only Android speech adapter
- M6 Tomorrow Contract + Prepared Wake Plan
- M7 deterministic local Wake Learning v0 core (PR #26)
- optional Vercel AI SDK 7 + AI Gateway cloud foundation (PR #23)
- Adaptive Dawn production design foundation (PR #30)
- production Wake Schedule + Tomorrow Contract setup flow (PR #32)
- curated Adaptive Dawn visual-regression implementation (PR #34)
- M8 provider-neutral voice measurement harness (PR #29)
- M8 direct OpenAI Realtime WebRTC debug candidate (PR #31; synthetic-only)

### Active PR #36 production voice work

PR #36 adds:

- real `WakeActivity` execution through `WakeVoiceSessionController`;
- production Alfred speech through the existing verified offline-only TTS path;
- Alfred v2 copy that explicitly asks for a spoken answer when a listen turn follows;
- typed `ListenForVoiceResponse` / `StopListeningForVoiceResponse` Wake Runtime directives;
- Android on-device-only speech recognition for short wake replies;
- transcript minimization: only `VoiceResponseObserved(coherent)` crosses the adapter boundary;
- simultaneous bounded motion evidence;
- speaking/listening/moving/orienting/degraded Wake Surface states;
- product microphone-permission primer with explicit local/privacy wording;
- `RECORD_AUDIO` in the product manifest while release builds retain no `INTERNET` permission;
- service-owned fail-safe alarm ducking during voice turns;
- deterministic degradation when local TTS, on-device STT, permission or sensors are unavailable;
- ADR 017 and a dedicated implementation note.

The production voice path is local and deterministic. It is **not** the M8 realtime provider selection and does not make a cloud model authoritative.

### Validation boundary

PR #36 automated checks are running on the active branch. Earlier branch heads have already passed the new pure-Kotlin voice-turn tests and documentation validation, but final-head Android lint/build/visual/device-workflow results must be reviewed before merge.

**No physical-device reliability claim has been made for PR #36.** Real locked-screen TTS/STT coexistence, alarm ducking, Bluetooth routing, OEM power management, silence recovery and Stop/Snooze during a voice turn remain physical-device gates.

Emulator/device-test evidence is useful but cannot prove the physical morning experience.

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
    ├─ durable Stop / Snooze
    └─ bounded voice-window volume lease
```

`AlarmPlaybackService` remains the owner of critical alarm audio. `WakeActivity` may enrich the experience but cannot become recovery authority.

Cloud, Vercel, Supabase, AI, WorkManager, Tomorrow Contract, Prepared Wake Plan, Wake Learning, speech recognition and visual/navigation frameworks do not participate in alarm delivery.

### Production local conversational path

Canonical decision: [`adr/017-local-production-voice-wake.md`](adr/017-local-production-voice-wake.md).  
Implementation note: [`implementation/m8-local-production-voice-wake.md`](implementation/m8-local-production-voice-wake.md).

```text
Active Wake Occurrence
        ↓
WakeActivity
        ↓
WakeVoiceSessionController
        ↓
WakeRuntime
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
        ↓
 typed WakeDirective
  ├─ Speak(intent)
  ├─ ListenForVoiceResponse
  ├─ ObserveMotion
  ├─ PresentOrientation
  └─ CompleteSession
        ↓
Android adapters
  ├─ AlfredCharacter → LocalCharacterSpeaker
  ├─ LocalVoiceListener → on-device SpeechRecognizer
  └─ AndroidMotionObserver
```

Wake Runtime remains behavioral authority. Android adapters emit facts and execute typed directives; they do not maintain a second confidence model.

### Local voice privacy boundary

```text
microphone
    ↓
Android on-device SpeechRecognizer
    ↓
ephemeral recognized candidate text
(adapter scope only)
    ↓
coherent? boolean
    ↓
VoiceResponseObserved
    ↓
WakeRuntime
```

The production local voice adapter does not persist raw microphone audio or recognized transcript text. Release builds do not gain network access for this baseline.

If on-device recognition is unavailable, the app fails closed to motion/critical-alarm behavior rather than silently using a remote recognizer.

### Alarm / voice coexistence

A full-volume siren makes speech output/input difficult, but muting critical audio from the Activity would create a reliability hazard. The foreground service therefore owns a bounded lease:

```text
normal alarm playback     100%
        ↓ voice turn
bundled alarm bed          12%
        ↓ max 12 seconds
automatic restoration     100%
```

The Activity may request/refresh the window. It cannot create an indefinite mute. Activity loss requests immediate restoration, lease expiry restores without an Activity callback, and service recovery starts at full volume.

The emergency `ToneGenerator` fallback is never ducked.

The 12% / 12-second values are tuning hypotheses until physical dogfood confirms intelligibility and safety.

### Character path

```text
WakeDirective.Speak(SpeechIntent)
           ↓
      AlfredCharacter v2
 deterministic curated wording
           ↓
    RenderedWakeLine
           ↓
 OfflineVoiceSelector
           ↓
 LocalCharacterSpeaker
 Android TextToSpeech
   USAGE_ALARM + speech content
```

Alfred owns wording only. He cannot advance the session, calculate wake confidence, stop the alarm or claim durable snooze success.

Character-copy changes that alter replay semantics require a character-version bump.

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

### Private preparation path

```text
next Wake Occurrence
        ↓
Tomorrow Contract product screen
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

Private preparation is optional enrichment and cannot make a critical schedule commit or alarm fire fail.

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

M7 core is merged but is not yet wired to real morning-session persistence/application. Wake Learning never mutates an active Wake Session and never learns Alarm Kernel delivery/safety boundaries.

### Product setup / presentation path

```text
normal application
      ↓
 Navigation 3
      ↓
 Tonight
   ├─ Wake Setup
   │    └─ AlarmKernel.commitSchedule() / cancelSchedule()
   ├─ Tomorrow Contract
   │    └─ WakePreparationManager
   └─ developer Wake Alarm Lab
        (debuggable app only)

active Wake Occurrence
      ↓
 dedicated WakeActivity
      ↓
 Adaptive Dawn + live voice-state presentation
```

`WakeActivity` is deliberately not a normal Navigation 3 destination.

The UX language `Emerging → Engaged → Active → Oriented` remains presentation/cognition language only; it is not a second behavioral state machine.

### Visual-regression path

```text
synthetic deterministic product state
             ↓
      Compose presentation
             ↓
 Roborazzi + Robolectric Native Graphics
             ↓
 reviewed PNG baseline
             ↓
 read-only PR verification
```

The existing Wake Emerging golden remains intentionally unchanged in PR #36. Production `WakeActivity` explicitly supplies live voice state, while the golden fixture renders the previously reviewed state until new dynamic voice-state PNGs receive separate visual review. No test automatically records new truth.

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
   Vercel AI SDK 7 / provider adapters
```

There is no Android production dependency on this service for alarm delivery or PR #36 local voice turns.

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
short-lived ephemeral credential
            ↓
SDP to OpenAI /v1/realtime/calls
            ↓
WebRTC media + bounded event-type metadata
```

This remains an engineering measurement surface only. It does not own wake behavior and remains `synthetic-only` until the required privacy and physical evidence exists.

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
Roborazzi                1.74.0 (test-only)
Robolectric              4.16.1 (test-only)
WebRTC spike             150.7871.01 (debug-only M8)
```

## Product-design foundation

Canonical notes:

- [`implementation/product-design-foundation.md`](implementation/product-design-foundation.md)
- [`implementation/d2-visual-regression.md`](implementation/d2-visual-regression.md)
- [`implementation/d3-product-setup.md`](implementation/d3-product-setup.md)

The current production design system is Adaptive Dawn with centralized Compose tokens, WMW semantic components, native WMW Presence, Navigation 3 for normal product destinations and a dark pre-Compose window baseline.

Design infrastructure must remain presentation-only. It cannot become alarm authority or invent durable product settings that do not yet affect real behavior.

## Implemented roadmap milestones

### M0 Foundation

Native Android project, minimal modules, deterministic schedule/recurrence/DST behavior and CI baseline.

### M1 Deep Alarm Kernel

Exact `setAlarmClock()` registration, foreground Active Wake Execution, bundled local alarm audio, Direct-Boot-aware wake UI, atomic device-protected Critical Wake Snapshot, durable Stop/Snooze, stale occurrence rejection and reconciliation.

### M2 Reliability Harness

Founder T+2m tests, bounded timing history, trigger/audio/UI/terminal facts, sanitized reports, instrumentation coverage and dedicated API-36 emulator workflow. Physical-device evidence remains open in issue #9.

### M3 Wake Runtime

Pure Kotlin phases, typed Wake Inputs/Directives, versioned Wake Policy, internal Activation Evidence authority, deterministic escalation, capability degradation, Stop/Snooze handshakes and replay invariants.

PR #36 is the first real-morning Android directive execution path; Alarm Kernel ownership remains unchanged.

### M4 Motion Evidence

Bounded pickup/orientation/sustained-movement extraction, correlated-evidence protection, thin Android sensor observer and no raw sensor persistence. PR #36 connects the observer to a real Wake Runtime session. Threshold calibration remains open.

### M5 Alfred local character

Versioned deterministic character model, curated Alfred copy, bounded escalation, offline voice selection and Android TTS adapter. PR #36 promotes this from founder lab to optional production wake enrichment and bumps Alfred to v2 for explicit reply prompts.

### M6 Tomorrow Contract + Prepared Wake Plan

Versioned contract/plan models, deterministic preparation, occurrence/revision binding, integrity validation, credential-protected atomic storage, on-demand refresh and unlocked-only enrichment. Production UI is merged.

### M7 Wake Learning v0 core

Merged in PR #26. Deterministic compact outcome derivation, optional calibration, bounded policy adaptation, agency/friction guardrails, immutable/self-validating policy snapshots, fail-closed resolution and reset are implemented. Live journal/application wiring remains open.

### M8 Voice architecture spike

In progress under issue #28.

PR #29 merged the provider-neutral evidence harness. PR #31 merged the first runnable transport candidate: direct OpenAI Realtime over WebRTC in a debug-only Android founder lab with server-minted ephemeral credentials.

PR #36 does **not** select or replace M8. It establishes the production local voice baseline against which later realtime enrichment must justify its latency, quality, privacy, operational and failure-mode costs.

### Later milestones

M9 realtime conversation, M10 useful context, M11 dogfood hardening and M12 closed beta remain future work. Realtime AI may enrich wording/turn quality but cannot replace Wake Runtime or Alarm Kernel authority.

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
| M3 Wake Runtime | **Core merged; real-morning local execution implemented in PR #36, physical gate pending** |
| M4 Motion Evidence | **Core merged; production-session connection in PR #36, calibration open** |
| M5 Alfred local experience | **Core merged; production local speech integration in PR #36, physical gate pending** |
| M6 Tomorrow Contract | **Core + production UI merged; PR #20 + PR #32** |
| Optional Vercel AI platform | **Merged / no critical Android dependency** |
| M7 Wake Learning v0 | **Core merged; live persistence/application open (#27)** |
| Product design D0/D1 foundation | **Merged / complete, PR #30** |
| Product design D2 visual regression | **Merged / read-only curated gate active, PR #34** |
| Product design D3 setup experience | **Merged, PR #32** |
| Local production voice wake | **Implemented in draft PR #36; automated + physical validation pending** |
| M8 realtime voice architecture spike | **In progress; comparative measured evidence open (#28)** |
| M9 Realtime conversation | Not started |
| M10 Useful context | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Get PR #36 to a clean final-head documentation, core-test, Android lint/build, visual-regression and API-36 reliability result.
2. Install the PR #36 APK on a representative physical phone and run scheduled locked-screen morning dogfood with voice permission granted, denied, silence, Activity interruption, Stop/Snooze and common audio routes.
3. Tune the 12% alarm-bed level, 9-second listen turn and 12-second fail-safe lease only from physical evidence; do not tune from aesthetics alone.
4. Add deliberately reviewed dynamic Wake visual fixtures after production states are physically inspected. Do not auto-record goldens.
5. Continue M8 first-audible-speech, barge-in, reconnect, network-transition, Bluetooth, cost and privacy measurements independently of the local production path.
6. Introduce a second realtime comparison only when it materially tests another operational shape; do not select a winner before comparison readiness.
7. Continue issue #27 local Wake journal/outcome/calibration/learned-policy wiring only after the active-session reliability boundary is proven.
8. Build consumer-facing Wake Readiness repair for recoverable notification/full-screen/exact-alarm capability problems without moving capability authority into UI.

## Privacy boundaries

The Critical Wake Snapshot and reliability logs must never contain Tomorrow Contract raw text, calendar content, transcripts/microphone audio, prompts, secrets/tokens, private generated speech or raw high-frequency motion streams.

M6 private state remains credential-protected and excluded from Auto Backup. Pre-unlock wake remains generic and locally actionable.

M7 consumes compact semantic outcomes and optional structured calibration/friction feedback. It does not require raw audio, transcript text, raw sensor streams or cloud identity.

PR #36 local voice recognition is intentionally transcript-minimized. Raw audio is owned transiently by the Android recognizer; recognized text is discarded inside `LocalVoiceListener`; Wake Runtime receives only compact typed evidence. There is no release `INTERNET` permission for this baseline.

M8 direct OpenAI experiments remain separate and `synthetic-only`. Standard provider credentials stay server-side and complete realtime event/audio/transcript payloads are not persisted by WMW's debug measurement path.

A successful voice connection or automated build is never treated as privacy/reliability evidence by itself.

## Current blockers / risks

There is no blocker to isolated implementation work, but PR #36 is not ready for an unconditional reliability claim until final CI and physical dogfood are complete.

Open proof/risk boundaries:

- physical Android reliability evidence (#9) remains open;
- local TTS + critical alarm coexistence is implemented but still unproven on representative physical devices;
- on-device speech-recognition behavior while locked, under OEM power management and across common audio routes needs physical evidence;
- the alarm-bed ducking level/lease duration are deliberate fail-safe hypotheses, not calibrated constants;
- motion thresholds remain uncalibrated on real devices;
- M7 local journal/persistence/application wiring remains open (#27);
- consumer-facing readiness repair remains open;
- broader accessibility/responsive visual fixtures remain deliberately outside the initial curated golden set;
- M8 still lacks comparative measured evidence sufficient for ADR-008 provider/transport selection;
- selected cloud audio/realtime routes remain subject to WMW privacy/data-control requirements and cannot become critical alarm dependencies.

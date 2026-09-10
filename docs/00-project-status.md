# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** local production voice wake merged in PR #36; physical-device dogfood is the next reliability gate  
**Current product-design track:** Adaptive Dawn foundation, production setup and curated visual regression are merged; dynamic voice-state visual review remains open  
**Current implementation branch:** `main`  
**Current realtime track:** M8 issue #28 remains separate; provider-neutral harness PR #29 and direct OpenAI WebRTC debug candidate PR #31 are merged, comparative physical evidence remains open  
**Current learning track:** M7 deterministic core is merged; real morning journal/persistence/application wiring remains open under issue #27

## Executive status

Wake My Way is now a real local-first conversational Android wake product in founder dogfood form.

PR #36 merged the first production connection of the deterministic Wake Runtime, Alfred local speech, on-device voice replies and motion evidence into the real `WakeActivity` while preserving the existing Alarm Kernel / Active Wake Execution reliability boundary.

The production morning path is now:

```text
Wake Schedule
    ↓
Alarm Kernel
    ↓
AlarmManager.setAlarmClock()
    ↓
AlarmReceiver
    ↓
AlarmPlaybackService
critical local alarm starts first
    ↓
WakeActivity
    ↓
WakeVoiceSessionController
    ↓
WakeRuntime
    ├─ Speak(InitialWake)
    ├─ Speak(AskToSitUp)
    ├─ ListenForVoiceResponse
    ├─ ObserveMotion
    ├─ Speak(AskToMove / ReEngage)
    └─ PresentOrientation / CompleteSession
```

Critical alarm delivery still does **not** depend on voice, microphone permission, AI, network, WakeActivity lifetime, motion, Tomorrow Contract, Wake Learning, Vercel or Supabase.

## What is implemented

### M0–M2: foundation, Alarm Kernel and reliability harness

Implemented and merged:

- native Android application and pure-Kotlin `:wake-core`;
- one active Wake Schedule with exact local Wake Occurrences;
- `AlarmManager.setAlarmClock()` registration;
- device-protected Critical Wake Snapshot;
- Direct-Boot-aware recovery/reconciliation;
- foreground Active Wake Execution;
- bundled `USAGE_ALARM` audio;
- durable Stop and Snooze replacement occurrences;
- stale-trigger rejection and idempotent active execution;
- automated API-36 reliability instrumentation and timing evidence.

Physical-device evidence remains distinct from emulator evidence.

### M3: deterministic Wake Runtime

Merged and now connected to the real morning path.

Wake Runtime remains the only in-session behavioral authority:

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

It owns:

- typed Wake Inputs and Wake Directives;
- Activation Evidence and thresholding;
- bounded escalation;
- capability degradation;
- Stop/Snooze handshakes;
- deterministic replay behavior.

PR #36 added typed voice-listening directives:

```text
ListenForVoiceResponse
StopListeningForVoiceResponse
```

### Mandatory spoken-reply gate

When the device can both speak locally and listen locally, reaching the numeric activation threshold is no longer sufficient by itself.

Wake Runtime requires:

```text
activation score >= threshold
        +
at least one coherent spoken reply
        ↓
ORIENTING
```

This prevents motion events from silently completing the wake while Alfred is still speaking or before the user has actually answered.

If local TTS or on-device recognition becomes unavailable, the voice requirement is removed deliberately so degraded capability cannot trap an active alarm forever. If the score was already sufficient when voice input disappears, Wake Runtime may orient immediately without waiting for another sensor event.

### M4: motion evidence

The existing bounded motion extractor and Android sensor adapter are now connected to production Wake Sessions.

Current evidence types include:

- device pickup;
- orientation change;
- sustained movement.

Raw high-frequency sensor streams are not persisted. Thresholds remain tuning hypotheses until physical-device calibration.

### M5: Alfred local character

Alfred is now production-connected rather than lab-only.

Alfred v2:

- uses deterministic curated wording;
- uses a verified offline Android TTS voice when available;
- explicitly asks the user to answer when a listening turn follows;
- remains concise, non-shaming and non-authoritative;
- cannot advance Wake Runtime, dismiss the alarm or decide Wake Success.

Example interaction shape:

```text
Alfred: “Sit up, then tell me when you're sitting.”
        ↓
LISTENING
        ↓
spoken reply
        ↓
VoiceResponseObserved(coherent)
```

`LocalCharacterSpeaker` uses `USAGE_ALARM` with speech content attributes and still fails closed when no suitable offline voice exists.

### Production local voice input

PR #36 added `LocalVoiceListener`, a one-turn adapter around Android on-device speech recognition.

Properties:

- Android API 31+ on-device recognizer only;
- `RECORD_AUDIO` permission required;
- no fallback to the platform default recognizer when that could cross the network/privacy boundary;
- one bounded recognition turn at a time;
- nine-second turn timeout;
- no transcript persistence;
- no raw microphone-audio persistence;
- only a compact `VoiceResponseObserved(coherent)` fact crosses into Wake Runtime.

The release Android path still has no `INTERNET` permission for this local voice baseline.

### Voice / critical-alarm coexistence

`AlarmPlaybackService` remains owner of critical sound.

To make Alfred and the microphone intelligible without allowing UI/voice code to mute the alarm indefinitely, the service owns a bounded voice-window lease:

```text
normal bundled alarm       100%
       ↓ voice turn
voice window                12%
       ↓ max 12 seconds
automatic restoration      100%
```

Important invariants:

- the Activity can request/refresh a voice window but cannot create an indefinite mute;
- lease expiry restores full volume independently of Activity callbacks;
- Activity loss cancels microphone capture and restores critical volume;
- the emergency `ToneGenerator` fallback is never ducked;
- service/process recovery begins from critical full-volume ownership;
- Stop, Snooze and successful completion use terminal-safe voice shutdown so a late restore command cannot race/restart the foreground service.

The 12% level, nine-second recognition turn and 12-second lease are engineering hypotheses pending physical dogfood tuning.

### Voice permission and readiness UX

Tonight now exposes persistent `Voice replies` readiness rather than showing a one-shot launch permission dialog.

States:

- **Ready** — on-device speech recognition is exposed and microphone permission is granted;
- **Setup needed** — on-device recognition exists but microphone permission is not granted;
- **Unavailable** — the device/API does not expose the required on-device recognizer.

The user explicitly taps **Enable voice replies** before the privacy primer and Android permission request.

If permission is denied, the setup affordance remains visible. If Android no longer permits another in-app request, the same action opens the app's Android Settings page. Returning to the app refreshes readiness.

Voice permission never determines `Wake Ready`; the critical alarm remains independently usable.

### Wake Surface

The production `WakeActivity` now has live presentation modes:

```text
STARTING
SPEAKING
LISTENING
MOVING
ORIENTING
DEGRADED
COMPLETE
```

The UI shows Alfred's product-owned scripted line but never displays a microphone-derived transcript.

The previously reviewed Wake Emerging Roborazzi golden remains unchanged intentionally. Production always supplies live voice state; the synthetic legacy fixture preserves the existing golden until dynamic voice states receive their own explicit visual review.

### M6: Tomorrow Contract + Prepared Wake Plan

Merged and production-connected before PR #36.

Private night-before context remains:

- credential-protected;
- stored outside the Direct-Boot critical snapshot;
- integrity-validated;
- optional enrichment only;
- unavailable pre-unlock without affecting the alarm.

### M7: Wake Learning v0

The deterministic local learning core is merged.

It supports:

- compact Wake Outcome derivation;
- optional calibration;
- bounded deterministic policy updates;
- annoyance/agency guardrails;
- versioned policy snapshots;
- fail-closed learned-policy resolution;
- reset/reversibility.

Live morning journal/persistence/application wiring is still not connected. Wake Learning must never mutate an active Wake Session or become required for alarm delivery.

### Product design

Merged product-design foundations include:

- Adaptive Dawn visual system;
- centralized Compose tokens;
- semantic WMW components;
- native WMW Presence;
- Navigation 3 for normal product destinations;
- production Tonight, Wake Setup and Tomorrow Contract flows;
- curated Roborazzi visual regression with read-only reviewed goldens.

Canonical design notes:

- [`implementation/product-design-foundation.md`](implementation/product-design-foundation.md)
- [`implementation/d2-visual-regression.md`](implementation/d2-visual-regression.md)
- [`implementation/d3-product-setup.md`](implementation/d3-product-setup.md)

## Canonical architecture

Root [`../CONTEXT.md`](../CONTEXT.md) owns domain vocabulary and invariants.

### Trust-critical path

```text
Wake Occurrence
      ↓
Alarm Kernel
      ↓
Critical Wake Snapshot
      ↓
AlarmManager
      ↓
AlarmReceiver
      ↓
AlarmPlaybackService
      ├─ critical local audio
      ├─ notification / full-screen intent
      └─ durable Stop / Snooze
```

### Behavioral path

```text
platform/user facts
      ↓
typed WakeInput
      ↓
WakeRuntime
      ↓
typed WakeDirective
      ↓
Android adapters
```

Android adapters execute decisions and report facts. They do not own a second activation score or behavioral state machine.

### Local conversational path

```text
WakeRuntime
   ├─ Speak(intent)
   │      ↓
   │ AlfredCharacter v2
   │      ↓
   │ LocalCharacterSpeaker
   │      ↓
   │ verified offline TTS
   │
   ├─ ListenForVoiceResponse
   │      ↓
   │ LocalVoiceListener
   │      ↓
   │ on-device SpeechRecognizer
   │      ↓
   │ coherent? boolean only
   │
   └─ ObserveMotion
          ↓
     AndroidMotionObserver
```

Canonical production voice decision: [`adr/017-local-production-voice-wake.md`](adr/017-local-production-voice-wake.md).  
Implementation note: [`implementation/m8-local-production-voice-wake.md`](implementation/m8-local-production-voice-wake.md).

### Optional realtime path

Realtime remains a separate M8 measurement track:

```text
Android debug Voice Spike Lab
        ↓
short-lived credential broker
        ↓
direct OpenAI Realtime WebRTC candidate
        ↓
provider-neutral measurement evidence
```

It is debug/synthetic measurement work, not production alarm authority and not selected as the production conversational transport.

## Automated validation for PR #36

The final PR #36 head `6a9694a959de1eff4a1f3125c00ed11838bfde3a` passed before merge:

- documentation validation;
- all `:wake-core` tests, including mandatory spoken-reply gate coverage;
- Android lint;
- instrumentation-test compilation;
- debug APK assembly;
- APK artifact upload;
- curated visual-regression verification;
- API-36 reliability instrumentation.

PR #36 was squash-merged to `main` as commit `0f5ca78777a8282d17cf4d3fe0df00f218efff51`.

The Vercel GitHub status observed during the PR hit the Hobby deployment daily-rate limit after more than 100 deployments. That status was unrelated to the Android build/voice implementation and did not block the merge. Vercel remains outside the critical Android wake path.

## Reliability truth

**No physical-device reliability claim is made from PR #36's CI results.**

Automated tests prove deterministic behavior, integration compatibility and emulator coverage. They do not prove the real sleeping-user experience under locked-screen audio routing, OEM power management, local TTS/STT coexistence or Bluetooth conditions.

Before promoting this behavior beyond founder dogfood or claiming physical reliability, validate on representative real phones.

## Exact next work

1. Install the PR #36 debug APK on a representative physical Android phone.
2. Open Wake My Way and enable **Voice replies** from Tonight; confirm readiness becomes **Ready**.
3. Schedule a near-term exact wake and lock the phone before fire time.
4. Verify critical alarm starts first and the full-screen Wake Surface appears.
5. Verify Alfred becomes audible and the alarm bed ducks without becoming unsafe.
6. Verify Alfred explicitly requests a spoken reply and the session does not complete from movement alone while two-way voice is healthy.
7. Answer aloud and confirm the response is accepted without rendering/persisting transcript text.
8. Verify silence/no-match restores/re-escalates safely and the alarm returns to full volume when the voice lease expires.
9. Exercise Stop and Snooze while Alfred is speaking and while the microphone is listening.
10. Exercise Activity interruption/backgrounding during a voice window and verify critical audio ownership returns to the service.
11. Repeat with microphone permission denied/revoked and confirm safe degraded alarm + motion behavior.
12. Test normal speaker plus common Bluetooth/audio-route conditions.
13. Tune alarm-bed level, listen timeout and lease duration only from measured physical evidence.
14. After physical voice reliability is established, continue M8 realtime comparison and M7 live learning/journal integration as separate tracks.

## Current risks / open proof boundaries

- physical-device wake reliability evidence remains open;
- on-device recognition availability varies by Android device/API and installed speech components;
- local TTS voice availability/quality varies by device;
- alarm-bed ducking and listen/lease timing need real-device calibration;
- Bluetooth/audio-route behavior is unproven for the new production voice path;
- motion thresholds remain uncalibrated on representative phones;
- M7 live journal/persistence/application wiring remains open under issue #27;
- M8 realtime comparison lacks sufficient physical comparative evidence for provider/transport selection;
- dynamic production voice UI states do not yet have separate reviewed golden screenshots;
- consumer-facing repair UX for all recoverable critical Android readiness problems remains future work.

## Privacy boundaries

Never place any of the following in Critical Wake Snapshot or reliability/analytics logs:

- raw microphone audio;
- recognized transcript text;
- Tomorrow Contract raw text;
- calendar descriptions;
- prompts;
- secrets/tokens;
- private generated speech;
- raw high-frequency motion streams.

The production local voice baseline intentionally minimizes microphone-derived data to a compact typed coherence observation. Network/cloud/realtime enrichment must remain optional and must not weaken Alarm Kernel or Wake Runtime authority.

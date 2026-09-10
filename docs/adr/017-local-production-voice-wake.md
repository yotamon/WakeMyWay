# ADR 017 — Local production voice wake baseline

**Status:** Accepted for implementation; physical-device morning validation remains required before reliability claims  
**Date:** 2026-09-10

## Context

The Alarm Kernel and Active Wake Execution already deliver a reliable local critical alarm, while Alfred, Wake Runtime, motion evidence, and realtime voice work were intentionally isolated from the production morning path. The result is safe but product-incomplete: a real alarm can fire without the conversational wake behavior the product is designed around.

Connecting voice directly to critical playback would create the wrong failure boundary. A TTS engine, speech recognizer, microphone permission, UI process, network provider, or future AI model must never become capable of suppressing the durable alarm indefinitely. At the same time, continuous full-volume siren audio makes speech output and speech recognition unusable.

## Decision

Wake My Way adopts a **local-first production voice wake baseline** with the following ownership model:

1. `AlarmPlaybackService` remains owner of critical `USAGE_ALARM` playback, notification/full-screen delivery, and durable Stop/Snooze.
2. `WakeRuntime` remains the only behavioral authority. Android voice and sensor adapters emit typed `WakeInput`; they do not compute a parallel wake score or outcome.
3. Alfred production speech uses the existing offline-only `LocalCharacterSpeaker` and is routed with `USAGE_ALARM` / speech content attributes.
4. Voice replies use Android **on-device** `SpeechRecognizer` when available. Production code uses `createOnDeviceSpeechRecognizer`; it does not fall back to the default recognizer, because the default recognizer may use a remote service.
5. `RECORD_AUDIO` is a product permission, but release builds retain **no `INTERNET` permission** for this local baseline.
6. Recognized text is ephemeral inside the Android adapter. The adapter emits only `VoiceResponseObserved(coherent = …)`; raw microphone audio and transcripts are not persisted, logged, journaled, or placed in Wake Runtime state.
7. Wake Runtime gains typed `ListenForVoiceResponse` and `StopListeningForVoiceResponse` directives. A spoken prompt may therefore explicitly require a user reply rather than assuming interaction from elapsed time.
8. Motion evidence continues in parallel. Losing voice input degrades to motion / critical alarm behavior rather than blocking the session.
9. Microphone capture is scoped to a visible active `WakeActivity`. Losing the surface cancels recognition and immediately restores critical alarm volume.
10. Production wake remains functional on Android versions/devices where on-device recognition is unavailable; those devices use the existing non-voice-input degradation path.

## Fail-safe voice-window lease

Conversational turns need the bundled alarm bed to become quiet enough for Alfred and the microphone to be intelligible. This is implemented as a **service-owned bounded lease**, not as a UI-owned mute:

```text
WakeRuntime Speak / Listen directive
              ↓
WakeActivity-side adapter requests voice window
              ↓
AlarmPlaybackService
    ├─ bundled alarm MediaPlayer → 12% volume
    ├─ max lease → 12 seconds
    └─ automatic restore → 100%
```

Each active voice turn may refresh that bounded lease. If the Activity, TTS callback, recognizer, or orchestration stalls, the service restores full alarm volume without needing another callback. The emergency `ToneGenerator` fallback is never ducked.

A service/process recreation also reconstructs Active Wake Execution from durable Alarm Kernel state and starts critical playback at full volume. Voice state is therefore never recovery authority.

## Permission UX

The normal application presents a privacy primer before requesting `RECORD_AUDIO`. The primer states that short wake replies are recognized on-device and that raw audio/transcripts are not saved. Permission is requested before the morning wake where possible; the wake-time Activity does not block critical alarm behavior waiting for a permission dialog.

## Consequences

### Positive

- The real morning product now supports speak → listen → evidence → respond behavior.
- Critical alarm reliability stays independent from TTS, STT, network, and AI.
- The production path has a strong privacy boundary and does not require a cloud account/provider.
- Realtime/AI voice can later enrich expression behind the same typed Wake Runtime boundary rather than redesigning wake authority.

### Trade-offs

- On-device speech recognition is not available on every supported Android device/API, so voice replies are capability-dependent.
- The first production conversation remains deterministic/curated rather than open-ended AI conversation.
- Audio coexistence, locked-screen recognition behavior, OEM power behavior, and intelligibility must be validated on physical devices before claiming reliability.
- Emergency tone fallback prioritizes wake reliability over conversational intelligibility and remains full-volume.

## Rejected alternatives

### Make realtime AI voice the first production path

Rejected. It would make network/provider/account/data-control conditions part of the first live morning experience before the local behavioral path is proven.

### Use the default Android speech recognizer as fallback

Rejected. The platform default may stream audio to a remote recognition service. WMW must fail closed to motion/alarm behavior rather than silently cross the privacy boundary.

### Let `WakeActivity` mute critical audio while speaking

Rejected. An Activity lifecycle failure could leave the alarm inaudible. The foreground alarm service owns both ducking and automatic restoration.

### Persist transcripts for later Wake Learning

Rejected. Wake Learning needs compact typed evidence/outcomes, not morning microphone transcripts. Transcript retention is unnecessary for the current product goal and would materially enlarge the privacy surface.

## Validation required

Before this baseline is promoted as physically reliable, test on representative real devices with:

- screen locked at fire time,
- app process cold / reclaimed,
- Doze and OEM battery-management conditions,
- on-device recognizer installed and absent,
- microphone permission granted and denied,
- local TTS voice installed and absent,
- speaker and common Bluetooth routes,
- voice-window stall / Activity loss,
- repeated silence / no-match recognition,
- Stop and Snooze during an active voice turn.

Automated unit/lint/emulator checks remain necessary but are not substitutes for those physical morning tests.

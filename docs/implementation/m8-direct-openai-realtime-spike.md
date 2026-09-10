# M8 direct OpenAI Realtime WebRTC spike

**Status:** runnable debug candidate + smoke-measurement seam; no architecture decision  
**Date:** 2026-09-10  
**Tracking:** issue #28; transport candidate merged in PR #31  
**Measurement contract:** [`m8-voice-spike-harness.md`](m8-voice-spike-harness.md)  
**Decision authority:** ADR-008

## Purpose

This work implements the first isolated realtime voice candidate required by M8 and the first trustworthy-enough smoke seam for observing user-perceived first speech. It exists to gather comparable evidence. It is not production wake behavior and it does not select OpenAI as the final provider or transport.

Current candidate identity:

```text
candidate        direct-openai
configuration   direct-openai:webrtc-ephemeral-v1
media transport WebRTC
credential      short-lived OpenAI client secret
control plane   WMW cloud token broker only
privacy state   synthetic-only until exact project data controls are verified
```

## Why WebRTC first

OpenAI's current Realtime documentation recommends WebRTC rather than WebSocket when a realtime model is connected from a browser or mobile client because it provides more consistent client media performance.

The ephemeral-token flow also keeps the standard OpenAI API key off the Android device:

```text
Android debug lab
      |
      | POST + WMW internal operator auth
      v
WMW cloud token broker
      |
      | standard OPENAI_API_KEY, server only
      v
OpenAI /v1/realtime/client_secrets
      |
      | short-lived client secret
      v
Android debug lab
      |
      | SDP + ephemeral Bearer
      v
OpenAI /v1/realtime/calls
      |
      v
WebRTC media + oai-events data channel
```

The standard OpenAI key is never returned to Android and must never be added to Gradle, resources, BuildConfig, Intent extras, logs, or persisted app storage.

## Cloud broker

Implemented under:

- `apps/cloud/src/voice-spike/direct-openai.ts`
- `apps/cloud/api/internal/voice-spike/direct-openai-token.ts`
- `apps/cloud/test/direct-openai-realtime.test.ts`

The broker:

- is separate from the Vercel AI Gateway realtime spike;
- requires `WMW_ENABLE_DIRECT_OPENAI_REALTIME_SPIKE=true`;
- requires a server-side `OPENAI_API_KEY`;
- requires `WMW_OPENAI_SAFETY_IDENTIFIER`;
- creates a `gpt-realtime-2.1` session by default;
- uses `marin` as the default experimental voice;
- requests a short-lived client secret from `/v1/realtime/client_secrets`;
- returns only bounded session metadata and the ephemeral value;
- applies a 15 second provider timeout;
- does not expose provider error response bodies;
- remains `synthetic-only` until privacy eligibility is independently verified.

The safety identifier must be a stable pseudonymous/one-way engineering identity. Do not use a raw name, email address, account ID, Tomorrow Contract value, or other personal content.

## Android isolation

The Android implementation is deliberately in the debug source set:

```text
apps/android/app/src/debug/
  AndroidManifest.xml
  java/com/wakemyway/app/voice/
    VoiceSpikeActivity.kt
    DirectOpenAiWebRtcSpike.kt
    VoiceSpikeMeasurementSession.kt
```

`io.github.webrtc-sdk:android` is declared with `debugImplementation`. `INTERNET`, `RECORD_AUDIO`, and `MODIFY_AUDIO_SETTINGS` are declared only in the debug manifest.

Consequences:

- release builds do not package the WebRTC dependency;
- release manifests do not gain M8 microphone/network permissions from this spike;
- WakeActivity, AlarmPlaybackService and Alarm Kernel do not depend on realtime code;
- the lab can fail or disappear without changing wake correctness.

The debug Activity is exported only so a founder build can launch it explicitly with adb. It accepts no secret Intent extras. Broker URL and operator token are entered interactively, the window uses `FLAG_SECURE`, and the operator token is cleared from the editable UI immediately after connect begins.

## Android connection sequence

```text
request RECORD_AUDIO at use time
        |
        v
mint ephemeral client secret
        |
        v
PeerConnectionFactory + microphone AudioTrack
        |
        v
create oai-events DataChannel
        |
        v
create + set local SDP offer
        |
        v
POST SDP directly to OpenAI /v1/realtime/calls
        |
        v
set remote SDP answer
        |
        v
WebRTC media / data channel
```

Network responses are bounded. The returned realtime call URL and configuration ID must exactly match the expected candidate before the ephemeral token is used.

### Connection-generation guard

Every connect attempt receives a monotonically increasing local generation id. `disconnect()` invalidates the current generation before WebRTC resources are released.

Credential minting, SDP exchange, peer callbacks and data-channel callbacks must match the current generation before they can create/use a peer or update the lab. This prevents an old asynchronous credential request from reconnecting the debug client after the Activity leaves the foreground or a newer attempt has started.

## Data minimization

The Android spike intentionally does not persist or log:

- microphone audio;
- generated audio;
- transcripts;
- complete Realtime server events;
- user-provided prompts/instructions;
- Tomorrow Contract content;
- calendar/context data;
- Wake Outcomes;
- OpenAI/WMW secrets.

The data channel parses only a bounded `type` field from each JSON server event and immediately discards the rest of the payload.

The first-speech probe sends one fixed source-controlled synthetic instruction only. It contains no user or wake context and creates an out-of-band response (`conversation = none`).

## Current measurement behavior

The lab keeps measurement state in memory only.

### Cold connection

```text
Connect starts
    ↓
credential + SDP + WebRTC setup
    ↓
oai-events DataChannel OPEN
```

`coldConnectionMs` is the elapsed monotonic time between those two local events.

### First audible speech smoke seam

The first-speech probe has an explicit local origin and an explicit audible observation:

```text
connected data channel
      ↓
operator stays quiet
      ↓
button 1: send fixed synthetic response.create
      ↓
monotonic request timestamp
      ↓
model response reaches physical output
      ↓
button 2: operator taps at first audible syllable
      ↓
firstSpeechMs
```

Measurement identity:

```text
protocolId              m8-direct-openai-manual-audible-v1
observation method      operator-tap-upper-bound-v1
```

The operator tap includes human reaction time. It is therefore a **conservative smoke upper bound**, not a precision audio benchmark. It is still preferable to treating `response.output_audio.*` or another protocol event as if sound had physically reached the listener.

A new synthetic request invalidates the previous first-speech observation. Invalid temporal ordering and duplicate audible observations are rejected by `VoiceSpikeMeasurementSession`.

No timing result is automatically persisted or converted into a canonical `VoiceSpikeSample`. Promotion into architecture-decision evidence requires an explicit later capture/review step and a comparable measurement method for every candidate.

## Privacy gate

OpenAI documentation can describe endpoint-level data-control capabilities, but WMW must verify the exact organization/project configuration used by this candidate before private wake data is allowed.

Until that evidence exists:

```text
privacyEligibility = synthetic-only
```

A successful network connection is not privacy evidence.

## Running the spike

Required server environment:

```text
WMW_INTERNAL_API_KEY=<32+ random characters>
OPENAI_API_KEY=<server-side key>
WMW_ENABLE_DIRECT_OPENAI_REALTIME_SPIKE=true
WMW_OPENAI_REALTIME_MODEL=gpt-realtime-2.1
WMW_OPENAI_REALTIME_VOICE=marin
WMW_OPENAI_SAFETY_IDENTIFIER=<stable pseudonymous hash/id>
```

Build/install a debug APK, then launch:

```text
adb shell am start -n com.wakemyway.app/.voice.VoiceSpikeActivity
```

For the first-audible probe, remain quiet after connection, press the fixed synthetic-response button, then press the audible-observation button at the first syllable you actually hear. The fixed phrase is source-controlled; do not replace it with personal content.

The broker URL should be HTTPS. `http://10.0.2.2` is accepted only for Android emulator development.

## Evidence still required

This candidate does not complete M8. Before ADR-008 can choose an architecture we still need comparable physical-device runs covering:

- repeated cold connection samples;
- repeated first-audible observations using a declared comparable protocol;
- a decision on whether manual upper-bound timing is sufficient or should be replaced/supplemented by a more precise non-content audio-output signal;
- barge-in timing;
- reconnect behavior;
- Wi-Fi and cellular;
- Wi-Fi to cellular and cellular to Wi-Fi transitions;
- Bluetooth route behavior;
- failure distributions;
- cost estimates;
- verified privacy/data-flow evidence;
- operational complexity for all compared candidates;
- at least one meaningful comparison configuration.

The measurement harness should continue to return `missing evidence` rather than a winner until those requirements are met.

## Explicit non-goals

This work does not:

- connect realtime voice to an alarm firing path;
- change WakeRuntime state or Activation Evidence;
- generate a WakePolicy;
- persist realtime conversations;
- move critical alarm audio to WebRTC;
- make cloud availability a wake dependency;
- select OpenAI over LiveKit/Vercel/other candidates;
- treat a protocol audio event as proof of audible output;
- claim production privacy readiness.

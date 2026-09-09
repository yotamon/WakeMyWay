# M8 direct OpenAI Realtime WebRTC spike

**Status:** implementation spike, no architecture decision  
**Date:** 2026-09-10  
**Tracking:** issue #28, PR #31  
**Measurement contract:** [`m8-voice-spike-harness.md`](m8-voice-spike-harness.md)  
**Decision authority:** ADR-008

## Purpose

This slice implements the first isolated realtime voice candidate required by M8. It exists to gather comparable evidence. It is not production wake behavior and it does not select OpenAI as the final provider or transport.

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

## Data minimization

The Android spike intentionally does not persist or log:

- microphone audio;
- generated audio;
- transcripts;
- complete Realtime server events;
- prompts/instructions;
- Tomorrow Contract content;
- calendar/context data;
- Wake Outcomes;
- OpenAI/WMW secrets.

The data channel parses only a bounded `type` field from each JSON server event and immediately discards the rest of the payload.

## Current measurement behavior

The lab currently displays one smoke metric in memory:

- cold connection time from Connect initiation through `oai-events` data-channel open.

This value is **not automatically persisted as an M8 evidence sample**. First audible speech latency is not inferred from an audio-delta/control event. We will add a trustworthy audible-output measurement seam before recording that field in the canonical harness.

This distinction prevents easy-to-measure protocol timestamps from masquerading as user-perceived audio latency.

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

Use only a neutral synthetic phrase during this stage.

The broker URL should be HTTPS. `http://10.0.2.2` is accepted only for Android emulator development.

## Evidence still required

This prototype does not complete M8. Before ADR-008 can choose an architecture we still need comparable physical-device runs covering:

- repeat cold connection samples;
- trustworthy first audible speech latency;
- barge-in timing;
- reconnect behavior;
- Wi-Fi and cellular;
- Wi-Fi to cellular and cellular to Wi-Fi transitions;
- Bluetooth route behavior;
- failure distributions;
- cost estimates;
- verified privacy/data-flow evidence;
- operational complexity for all compared candidates.

The measurement harness should continue to return `missing evidence` rather than a winner until those requirements are met.

## Explicit non-goals

This slice does not:

- connect realtime voice to an alarm firing path;
- change WakeRuntime state or Activation Evidence;
- generate a WakePolicy;
- persist realtime conversations;
- move critical alarm audio to WebRTC;
- make cloud availability a wake dependency;
- select OpenAI over LiveKit/Vercel/other candidates;
- claim production privacy readiness.

# Conversational Wake

**Status:** founder dogfood implementation  
**Branch:** `feature/conversational-wake`  
**Date:** 2026-09-10

## Goal

Turn the real Wake Session into a natural two-way conversation without moving alarm delivery, activation authority, Stop/Snooze, or completion into a cloud model.

## Architecture

```text
Alarm Kernel
   ↓
WakeRuntime
   ↓ typed SpeechIntent / activation evidence
WakeConversationEnrichment
   ├─ founder Realtime WebRTC enrichment (debug)
   └─ deterministic local Alfred fallback
```

The Realtime provider owns wording/audio quality only. WakeRuntime remains authoritative for session progression and completion.

## Conversational loop

WakeRuntime now emits `KeepEngaging` after a coherent voice response whenever activation is still below threshold. This creates an explicit loop:

```text
Alfred speaks
   ↓
user replies
   ↓
VoiceResponseObserved
   ↓
WakeRuntime
   ├─ threshold not met → Speak(KeepEngaging)
   │                     ↓
   │                  listen again
   └─ threshold met → orientation
```

## Realtime founder path

The debug build can optionally connect to OpenAI Realtime over WebRTC using a short-lived token from the WMW cloud broker.

Properties:

- audio-to-audio WebRTC conversation;
- server VAD observes speech turn boundaries;
- automatic provider response creation is disabled so the model cannot advance the wake autonomously;
- interruption/barge-in is enabled;
- WakeRuntime explicitly requests each assistant turn;
- transcripts/model payloads are not persisted by WMW;
- Tomorrow Contract, calendar and other private wake context are not sent in this founder slice;
- failure or unavailable configuration falls back to local Alfred without delaying critical alarm behavior.

## Founder configuration

Debug builds expose one-time configuration through the voice spike/founder lab. The broker URL and operator token are stored locally; the token is encrypted with a non-exportable Android Keystore AES-GCM key.

This is founder dogfood authentication only and must not be promoted as consumer authentication.

## Safety boundary

Realtime cannot:

- schedule or cancel alarms;
- stop or snooze an alarm;
- mutate WakePolicy;
- directly write activation evidence;
- decide that the user is awake;
- finish a Wake Session.

If Realtime fails, the current `SpeechIntent` is rendered through deterministic local Alfred and the existing local STT/motion path remains available.

## Validation

Before promoting Realtime beyond founder/debug dogfood, validate on a physical phone:

1. locked-screen alarm presentation still works;
2. Realtime joins without delaying initial alarm audio;
3. user can interrupt Alfred naturally;
4. multiple `you → Alfred → you` turns occur before activation completion;
5. network loss falls back to local Alfred without silence/deadlock;
6. Stop and Snooze remain immediate and local;
7. audio route restoration is correct after completion/interruption;
8. no transcript/private wake context appears in WMW persistence or diagnostics.

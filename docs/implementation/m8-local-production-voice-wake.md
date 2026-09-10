# M8 local production voice wake

**Status:** implementation in PR #36; automated and physical validation pending  
**Date:** 2026-09-10  
**Decision:** [`../adr/017-local-production-voice-wake.md`](../adr/017-local-production-voice-wake.md)

## Goal

Replace the real morning experience of “critical alarm + passive screen” with a deterministic, two-way wake conversation while preserving the existing Alarm Kernel reliability boundary.

The first production slice is intentionally local. It proves that the product can speak, wait for an answer, observe movement, re-engage after silence, and finish only through typed Wake Runtime evidence without requiring network or realtime AI.

## Production flow

```text
AlarmManager.setAlarmClock()
        ↓
AlarmReceiver
        ↓
AlarmPlaybackService
critical local alarm remains active
        ↓
WakeActivity full-screen surface
        ↓
WakeVoiceSessionController
        ↓
WakeRuntime
        ├─ Speak(InitialWake)
        ├─ Speak(AskToSitUp)
        ├─ ListenForVoiceResponse
        ├─ ObserveMotion
        ├─ Speak(AskToMove / ReEngage)
        ├─ ListenForVoiceResponse
        └─ PresentOrientation / CompleteSession
```

The controller executes directives; it does not decide wake progression.

## Voice output

`LocalCharacterSpeaker` remains offline-only and uses the versioned Alfred catalog. Production speech is configured with Android alarm audio usage so it follows the alarm-volume path rather than media-volume assumptions.

Alfred v2 changes the conversational turns so prompts that will be followed by a microphone turn explicitly ask for a spoken response. This matters during sleep inertia when the user may not be looking at the display.

Example shape:

```text
Alfred: “Sit up, then tell me when you're sitting.”
        ↓
listening
        ↓
short local response
        ↓
VoiceResponseObserved(coherent)
```

The wording is still curated/deterministic. Open-ended model-generated morning dialog remains outside this production baseline.

## Voice input

`LocalVoiceListener` is a one-turn adapter around Android on-device speech recognition.

Properties:

- API 31+ on-device recognizer only;
- `RECORD_AUDIO` required;
- no fallback to a recognizer that may require network;
- one active recognition turn at a time;
- nine-second bounded turn timeout;
- no partial-result persistence;
- transcript discarded before leaving the adapter;
- Wake Runtime receives only a boolean coherent-response observation.

The initial coherence heuristic is deliberately small and conservative: non-trivial alphanumeric content plus an acceptable confidence value when Android supplies one. It is evidence, not semantic truth and not a wake score.

## Runtime changes

`WakeCapabilities` now distinguishes:

```text
speechAvailable
voiceInputAvailable
motionAvailable
```

`voiceInputAvailable` defaults to false so pre-existing callers and replay fixtures do not silently assume a microphone capability.

New directives:

```text
ListenForVoiceResponse
StopListeningForVoiceResponse
```

When voice input is available, completion of the sit-up prompt keeps the session in `ENGAGING` and requests a listening turn. A coherent response contributes typed activation evidence and advances toward `ACTIVATING`. An unclear/no response adds no voice evidence and causes bounded re-engagement. If the recognizer disappears/fails, capability degradation removes future listen directives and motion/critical alarm behavior continues.

## Alarm / conversation coexistence

Full-volume critical audio makes spoken interaction difficult, but muting from the Activity would be unsafe. `AlarmPlaybackService` therefore owns a fail-safe voice-window lease:

```text
normal critical alarm           100%
voice speak/listen window        12%
maximum lease                    12 s
lease expiry / explicit restore 100%
```

The Activity can request or refresh a voice window, but cannot create an indefinite mute. Activity loss calls explicit restore, lease expiry restores independently, and service recovery starts critical playback at full volume. The emergency tone fallback never ducks.

These values are product hypotheses until physical-device testing confirms intelligibility and wake safety.

## Wake Surface states

The real `WakeActivity` now surfaces the active conversational state using the existing WMW Presence language:

| Mode | User meaning |
|---|---|
| `STARTING` | local wake voice is preparing |
| `SPEAKING` | Alfred is talking |
| `LISTENING` | a spoken answer is expected |
| `MOVING` | motion evidence is being gathered |
| `ORIENTING` | activation threshold was reached; session is closing |
| `DEGRADED` | voice input is unavailable; alarm/motion continue |
| `COMPLETE` | deterministic runtime completed the wake |

No transcript is rendered. The screen may show Alfred's current scripted line because that content is product-owned, not microphone-derived.

## Permission experience

The ordinary app asks for microphone permission before the morning wake where possible. A privacy primer explains:

- why the microphone is useful;
- recognition is on-device for this production baseline;
- raw audio and transcripts are not saved.

Denying permission does not invalidate the wake schedule and cannot prevent the alarm from firing.

## Failure matrix

| Failure | Result |
|---|---|
| offline TTS voice missing | critical alarm + motion path remain |
| microphone permission denied | no listening directives; critical alarm + motion remain |
| on-device recognizer unavailable | same local degradation; no remote fallback |
| recognizer timeout/no match | no evidence; deterministic re-engagement |
| sensor unavailable | voice/critical alarm path remain |
| Activity loses focus | microphone stops; alarm returns to full volume |
| controller/TTS callback stalls | service voice lease expires to full alarm |
| service process recreated | durable Active Wake Execution recovers at full alarm volume |
| no network | no effect on this production baseline |

## Automated validation

PR #36 adds pure-Kotlin coverage for:

- sit-up speech completion producing a listen directive;
- coherent reply becoming typed evidence;
- incoherent reply earning no evidence and producing bounded re-engagement;
- voice-input-unavailable fallback preserving movement progression.

Existing runtime/reliability tests remain authoritative for Alarm Kernel invariants. Roborazzi's Wake Emerging fixture is updated to represent the listening product state, but any new visual baseline must go through the repository's review/record gate rather than being silently overwritten.

## Physical-device merge gate

Before calling this production behavior reliable, dogfood at least one real scheduled wake on a representative phone and explicitly exercise:

1. locked-screen fire with microphone permission granted;
2. audible Alfred greeting and sit-up prompt;
3. successful spoken response while the alarm bed is ducked;
4. alarm volume restoration after intentional silence;
5. motion evidence while voice is active;
6. Stop and Snooze during speaking/listening;
7. permission denied / on-device recognizer unavailable fallback;
8. app backgrounding or Activity interruption during a voice window;
9. speaker and common Bluetooth-route behavior.

Do not convert those checks into percentile/reliability claims from emulator evidence alone.

# M5 Alfred local character experience

**Status:** merged in PR #17, CI-verified  
**Tracks:** issue #16 (completed)  
**Depends on:** M3 Wake Runtime, M4 motion evidence  
**Does not depend on:** network, backend, Vercel, Supabase, realtime AI, microphone

## Goal

Prove that Wake My Way can feel like a character-led conversational wake product before realtime AI exists.

Alfred is the first implemented character. He is dry, composed, concise and increasingly firm when Wake Runtime asks for stronger re-engagement, but he is never shaming, threatening, infantilizing or behaviorally authoritative.

## Architecture

```text
WakeRuntime
   │
   └─ WakeDirective.Speak(SpeechIntent)
                    │
                    ▼
             AlfredCharacter
       deterministic local renderer
                    │
             RenderedWakeLine
                    │
                    ▼
       LocalCharacterSpeaker
       Android TextToSpeech adapter
                    │
          verified offline voice
                    │
                    ▼
              local speech
```

The renderer and the Android speaker have deliberately different responsibilities:

- `AlfredCharacter` decides wording only.
- `LocalCharacterSpeaker` decides whether a safe local Android voice can render that wording.
- `WakeRuntime` remains the only owner of behavioral progression.
- Alarm Kernel / Active Wake Execution remains the owner of critical alarm audibility.

## Alfred specification v1

```text
id              alfred
version         1
display name    Alfred
voice locale    en-GB
speech rate     0.92
pitch           0.94
```

The locale/rate/pitch are presentation preferences, not a promise that every Android device ships the same voice. M5 deliberately uses a suitable installed offline voice when one exists.

## Deterministic rendering

Every line is derived from:

```text
character id
+ character version
+ normalized SpeechIntent
+ WakeLineKey
       ↓
stable FNV-1a selection
       ↓
bounded curated variant
```

This provides deterministic replay for the same render key while allowing bounded variation across different keys.

Character-copy changes require a character-version bump when replay equivalence would otherwise become misleading.

## Current intent coverage

The implementation covers every current runtime `SpeechIntent`:

- `InitialWake`
- `AskToSitUp`
- `AskToMove`
- `ReEngage(level)` with bounded levels 0–3
- `SnoozeConfirmation`
- `SnoozeFailed`
- `Orientation`

`ReEngage` wording becomes firmer with escalation but does not gain behavioral authority.

Semantic guardrails additionally ensure:

- `SnoozeConfirmation` cannot claim the replacement alarm has already been durably scheduled;
- `Orientation` cannot claim biological wakefulness or unsupported posture.

## Copy constraints

Curated Alfred lines are intentionally short for sleep inertia.

Automated tests enforce:

- every current intent renders;
- no blank line;
- hard maximum of 120 characters;
- current catalog expectation of at most 16 words per line;
- deterministic same-key replay;
- bounded variation;
- escalation clamping;
- no defined shame/insult/threat vocabulary;
- no premature snooze-success claim;
- no unsupported wake/posture claim.

These tests are guardrails, not a substitute for human copy review or dogfood feedback.

## Offline voice policy

`LocalCharacterSpeaker` fails closed.

It enumerates Android `TextToSpeech` voices and maps them into pure `LocalVoiceCandidate` values. `OfflineVoiceSelector` rejects:

- every voice where Android reports `isNetworkConnectionRequired == true`;
- unrelated-language voices.

Selection priority is:

1. verified local/offline only;
2. exact preferred locale;
3. same-language fallback;
4. voice quality as tie-breaker;
5. deterministic id tie-break.

If no suitable offline voice exists:

```text
character speech unavailable
        ↓
no queued network request
        ↓
no waiting spinner
        ↓
critical alarm audio continues unchanged
```

The app currently declares no `INTERNET` permission, providing an additional structural boundary for M5.

## Speech lifecycle

The Android adapter exposes explicit states:

```text
Initializing
   ↓
Ready(voice id, language)

or

Unavailable(reason)
```

A speech request before `Ready` fails immediately. It is not queued for a later network-capable voice.

Utterance lifecycle handles completion, interruption, engine errors, explicit stop and shutdown. Callbacks retain no transcript history.

## Wake Alarm Lab integration

M5 adds an `Alfred · local character lab` section to the existing founder diagnostics screen.

The lab can:

- cycle through current speech intents;
- render bounded Alfred variants;
- display character version + variant index;
- show selected offline voice diagnostics;
- speak the rendered line when a local voice is ready;
- visibly report silent fallback when speech is unavailable.

This remains deliberately **not wired into Active Wake Execution**.

Production integration is gated by physical reliability testing. We must prove that adding character speech does not interfere with critical alarm audibility, foreground-service lifetime, audio routing/focus, lock-screen presentation, Stop/Snooze, or latency targets.

## Privacy

M5 stores or transmits none of the following:

- microphone audio;
- transcripts;
- user wake context;
- Tomorrow Contract text;
- calendar data;
- raw character speech history;
- cloud TTS requests.

The character renderer consumes only typed `SpeechIntent` plus a non-sensitive render key.

## Merge evidence

The final PR #17 head passed:

- documentation validation;
- all `:wake-core` tests including Alfred copy/voice guardrails;
- Android lint;
- instrumentation compilation;
- debug APK assembly;
- debug APK artifact upload;
- instrumentation APK artifact upload.

The PR changed no Alarm Kernel, receiver, critical playback service, `WakeActivity`, manifest, motion authority or Wake Runtime authority file.

## Remaining evidence boundary

Physical-device voice quality and TTS/critical-audio coexistence remain dogfood evidence, not CI claims. M2 issue #9 remains open for the broader physical reliability envelope.

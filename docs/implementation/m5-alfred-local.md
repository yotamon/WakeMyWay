# M5 Alfred local character experience

**Status:** implementation candidate on `feat/m5-alfred-local`  
**Tracks:** issue #16  
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

The locale/rate/pitch are presentation preferences, not a promise that every Android device ships the same voice. M5 deliberately uses whatever suitable **installed offline** voice is available.

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

This gives two useful properties:

1. the same render key replays the same line for diagnostics/tests;
2. different keys provide bounded variation without generative unpredictability.

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

`ReEngage` wording becomes firmer with escalation but does not gain new behavioral authority.

## Copy constraints

Curated Alfred lines are intentionally short for sleep inertia.

Automated tests enforce:

- every current intent renders;
- no blank line;
- `RenderedWakeLine` hard maximum of 120 characters;
- M5 catalog expectation of at most 16 words per line;
- deterministic same-key replay;
- bounded variation;
- escalation clamping;
- absence of defined shame/insult/threat vocabulary.

These tests are guardrails, not a complete substitute for human copy review.

## Offline voice policy

`LocalCharacterSpeaker` fails closed.

It enumerates Android `TextToSpeech` voices and maps them into pure `LocalVoiceCandidate` values. `OfflineVoiceSelector` rejects every voice where Android reports `isNetworkConnectionRequired == true`.

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

The app currently declares no `INTERNET` permission, providing an additional practical boundary for M5.

## Speech lifecycle

The Android adapter exposes explicit states:

```text
Initializing
   ↓
Ready(voice id, language)

or

Unavailable(reason)
```

A speech request made before `Ready` fails immediately. It is not queued for a later network-capable voice.

Utterance callbacks report only local completion/failure and retain no transcript history.

## Wake Alarm Lab integration

M5 adds an `Alfred · local character lab` section to the existing founder diagnostics screen.

The lab can:

- cycle through current speech intents;
- render bounded Alfred variants;
- display character version + variant index;
- show selected offline voice diagnostics;
- speak the rendered line when a local voice is ready;
- visibly report silent fallback when speech is unavailable.

This is deliberately **not wired into Active Wake Execution yet**.

That production integration remains gated by the reliability boundary. We first want to prove on physical devices that adding character speech does not interfere with alarm audibility, foreground-service lifetime, audio focus, lock-screen presentation, Stop/Snooze, or timing targets.

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

## Exit criteria

M5 implementation is ready to merge when:

- pure character tests pass;
- existing Wake Runtime tests remain green;
- Android lint passes;
- instrumentation compilation remains green;
- debug APK assembles;
- Wake Alarm Lab exposes deterministic Alfred preview;
- Android adapter contains no network-required voice fallback;
- critical alarm execution code remains untouched by the character feature.

Physical voice quality and audio coexistence remain dogfood evidence, not CI claims.

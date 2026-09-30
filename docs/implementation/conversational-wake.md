# Conversational Wake

**Status:** Direct account-authenticated dogfood implementation  
**Updated:** 2026-09-30

## Goal

Turn the real Wake Session into a natural two-way wake conversation without moving alarm delivery,
activation authority, Stop/Snooze, or completion into a cloud model.

The experience target is not “chat with an assistant.” It is:

> one short wake prompt → one sleepy reply → one small next action → repeat only as needed.

The product succeeds when the user becomes meaningfully engaged with minimum effective friction.

## Authority

```text
Alarm Kernel                         always local
   ↓
WakeRuntime                          deterministic wake authority
   ↓ SpeechIntent / typed evidence
WakeConversationEnrichment           language + live audio only
   ↓ observation
usable/unusable spoken turn          never direct completion authority
```

Realtime may render language and observe whether a committed audio turn contains usable spoken
engagement. It cannot decide that a physical action happened, mark the user awake, mutate WakePolicy,
stop an alarm, schedule snooze, or finish a Wake Session.

## Direct Realtime path

The Direct build uses account-authenticated OpenAI Realtime over WebRTC with short-lived credentials.

Current interaction policy:

- the critical alarm starts locally and independently of Realtime;
- the opening is one combined greeting + sit-up request + short spoken confirmation;
- after the opening audio finishes, WakeRuntime listens instead of playing a second prompt;
- WakeRuntime, not Realtime, sequences the physiological wake path: feet down → brief seated upper-body activation → stand-if-safe/seated alternative → small reachable environmental activation;
- early turns minimize cognitive load: no open-ended morning questions, briefings, puzzles or stacked tasks;
- semantic VAD uses low eagerness so a just-woken user can pause and trail off without aggressive
  turn chunking;
- Realtime reasoning effort is low because wake turns are latency-sensitive and intentionally simple;
- provider auto-response creation stays disabled;
- interruption/barge-in stays enabled;
- Alfred speaks slightly below normal speed for wake-state intelligibility;
- the alarm melody is ducked, never surrendered, during voice/listening windows;
- provider/network/session failure degrades to alarm-only rather than substituting generic TTS.

## Usable spoken-turn gate

A VAD event is not Activation Evidence.

After Realtime commits an audio item, WakeMyWay performs a hidden out-of-band classification of only
that audio item. The classifier produces the smallest possible signal:

```text
USABLE
UNUSABLE
```

Examples treated as **usable** when intelligible and intentional:

- yes / no / yeah / done;
- a complaint;
- bargaining such as “five more minutes”;
- a joke;
- refusal or profanity.

Examples treated as **unusable**:

- silence / breathing;
- coughs or groans without speech;
- humming;
- accidental noise;
- background media or side conversation;
- speech too unclear or mumbled to confidently treat as a reply.

Classification never decides whether the requested physical action happened. It only decides whether
the user produced usable spoken engagement.

The parser fails closed: malformed output, missing item ids, classification timeout, and very short
noise-like commits produce `coherent = false`, which WakeRuntime handles through bounded
re-engagement. They never manufacture activation evidence.

Raw audio and classifier text are not persisted by WakeMyWay.

## Wake conversation loop

```text
Alfred: one short action
        ↓
user audio
        ↓
Realtime VAD commits audio item
        ↓
hidden usable-turn classification
        ↓
VoiceResponseObserved(coherent = true/false)
        ↓
WakeRuntime
   ├─ unusable → bounded ReEngage
   ├─ usable + more evidence needed → next one-action prompt
   └─ activation gate satisfied → Orientation
```

Motion remains independent typed evidence. When two-way voice is available, movement alone cannot
silently bypass the required spoken engagement gate.

## Alfred interaction contract

Alfred should feel like a composed person beside the bed, not a general assistant.

- one or two short sentences;
- exactly one small physical wake action per turn;
- optionally one short spoken confirmation after that action;
- never skip ahead from verbal fluency alone; follow the exact current runtime intent;
- standing is conditional and always has a seated alternative;
- never introduce squats, jumping, balance challenges, strenuous exercise or rapid/forced breathing;
- after unusable audio, repeat/reframe the current safe action rather than inventing a harder one;
- no checklists;
- no long motivational monologues;
- acknowledge the user's actual words briefly, then move forward;
- complaints, bargaining and jokes are engagement, not a reason to argue;
- never fill silence with chatter;
- immediately yield when interrupted;
- never pretend unclear audio was understood;
- never claim unsupported posture, sensor state, biological wakefulness, snooze success or alarm
  completion.

## Alfred identity continuity

Alfred's identity is session-scoped, not turn-scoped.

The Realtime session receives one stable character definition that owns accent, conversational
distance, humour, energy range, anti-AI mannerisms and delivery. Runtime turns do **not** redefine
the character; they provide only the current action plus a bounded style modifier.

Production character target:

- composed British presence with dry, understated humour;
- familiar morning companion, not customer service, therapist, motivational coach or theatrical
  butler;
- warm without being sweet or fawning;
- no praise for ordinary compliance;
- no generic assistant phrases such as "Absolutely", "Great job", "You've got this" or
  "I'm here to help";
- stable accent, apparent personality and conversational distance from first turn to last;
- firmer re-engagement becomes more direct, never louder, chirpier or like a different actor;
- motivational and minimal styles remain modifiers of the same Alfred identity.

The Direct client logs the Alfred persona version when the Realtime session becomes ready so
dogfood reports can distinguish character revisions without persisting conversation content.

## Failure behavior

Any Realtime failure returns to the honest baseline:

```text
local alarm audio + haptics + local Stop/Snooze
```

Realtime availability is never a Wake Ready predicate.

## What still requires physical evidence

Repository tests can prove state-machine and parser behavior. They cannot prove the final morning
experience. Before broader launch, overnight physical-device dogfood must validate:

- locked-screen cold start;
- first audible voice latency;
- soft / slow / one-word sleepy replies;
- long pauses inside an utterance;
- cough / groan / TV false-positive resistance;
- interruption while Alfred is speaking;
- speaker and Bluetooth routing;
- Wi-Fi ↔ cellular / provider failure;
- Stop/Snooze during any live turn;
- alarm ducking and restoration;
- no listening deadlock after a rejected short turn.

See [conversational-wake-testing.md](conversational-wake-testing.md).

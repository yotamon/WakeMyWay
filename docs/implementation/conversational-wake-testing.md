# Conversational Wake physical dogfood checklist

Use a Direct dogfood build on a physical phone with a signed-in WakeMyWay account.

## Reliability baseline

1. Schedule a production-path T+2m wake and lock the phone.
2. Verify critical alarm audio starts independently of Realtime connection.
3. Verify WakeActivity appears with local Stop/Snooze.
4. Verify the opening contains one concise Alfred prompt and then listening; there must not be two
   back-to-back assistant prompts.
5. Kill connectivity before a wake and verify the local alarm remains fully usable.
6. Drop connectivity during a later live turn and verify the wake degrades to alarm-only without
   silence or deadlock.
7. Verify Stop and Snooze remain immediate regardless of Realtime state.
8. Verify audio routing/mode and alarm volume restore after terminal action.

## Sleepy-turn behavior

Run each case from the real wake surface:

1. Say a short “yeah” or “yes”. It should count as spoken engagement.
2. Say “five more minutes”. Alfred may acknowledge it but should continue with one small wake action.
3. Complain or joke. Alfred should react briefly and continue rather than restart a canned script.
4. Speak softly with a natural mid-sentence pause. The turn should not be cut aggressively.
5. Interrupt Alfred halfway through a sentence. Alfred should yield and respond to the interruption.
6. Cough only. It must not count as coherent spoken activation evidence.
7. Groan without intelligible words. It must not count as coherent spoken activation evidence.
8. Play nearby TV/podcast speech without addressing Alfred. It should not advance wake activation.
9. Give deliberately unintelligible/mumbled speech. It should trigger concise re-engagement.
10. Produce an extremely short noise-like utterance. The app must re-engage or keep progressing; it
    must never remain stuck in Listening.

## Wake progression

1. Verify motion by itself cannot complete a voice-capable wake without one usable spoken response.
2. Verify a usable spoken response by itself does not fake movement evidence.
3. Verify activation completion occurs only when WakeRuntime's configured evidence gate is reached.
4. Verify Alfred never claims “you are awake,” a posture, alarm stop, or snooze success unless the
   deterministic product state actually permits the corresponding statement.
5. Verify a normal wake needs only a small number of concise turns rather than conversation for its
   own sake.

## Audio experience

1. Voice must be clearly intelligible over the ducked alarm melody.
2. Melody must return to critical volume after the voice/listening lease ends.
3. Voice should sound calm and deliberate rather than rushed.
4. Verify built-in speaker, wired audio if available, and Bluetooth behavior separately.
5. Repeat with the phone beside the bed rather than held near the mouth.

## Character continuity dogfood

Run one uninterrupted wake session through at least six assistant turns and intentionally vary the
user's behaviour: cooperative reply, bargaining, profanity, joke, silence/re-engage and completion.

Pass only when:

- Alfred sounds like the same person on every turn;
- accent and conversational distance remain stable;
- motivational or firmer turns do not become a new upbeat/coach persona;
- no generic assistant praise or customer-service language appears;
- humour stays sparse and dry rather than becoming a running comedy bit;
- re-engagement after unclear audio does not sound like a reset or reintroduction;
- an interruption resumes the same conversational thread and character;
- a Realtime reconnect is recorded as a new technical session rather than silently being mistaken
  for continuous character identity during evaluation.

When reporting a failure, note the assistant turn number, runtime intent, selected VoiceStyle and
logged Alfred persona version. Do not persist raw microphone audio or full transcripts in routine
telemetry.

## Evidence to record

Record only non-sensitive product telemetry or manual notes:

- cold Realtime connection / first audible voice latency;
- number of Alfred turns before completion or fallback;
- false usable-turn observations;
- missed usable one-word replies;
- accidental interruptions;
- fallback stage;
- any audio-route or listening deadlock.

Do not persist raw microphone audio or full wake transcripts as routine dogfood telemetry.

Do not treat emulator success as proof of physical audio-route, lock-screen, Bluetooth, VAD, OEM, or
overnight behavior.

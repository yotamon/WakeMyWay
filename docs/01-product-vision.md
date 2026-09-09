# Product vision

## One-line definition

**Wake My Way is an adaptive conversational alarm that learns the most effective way to help each user move from sleep inertia toward meaningful activity and the start of their morning.**

## The problem

A traditional alarm solves a scheduling problem: make a sound at a specific time.

For many people, that is not the real problem. The real problem is the transition between sleeping and acting. A user may hear the alarm, dismiss it reflexively, snooze repeatedly, answer while still half asleep, or technically wake up and then return to bed.

The user did not set the alarm because they wanted to hear a ringtone at 08:00. They set it because they wanted a specific real-world outcome at 08:00.

> **I genuinely want to be out of bed at this time. Help me succeed.**

## Product thesis

Different people wake differently.

Some respond to:

- a calm voice
- direct instructions
- humor
- a reminder of why today matters
- conversation
- movement
- a small amount of snooze
- stronger escalation

The same person may also need different strategies depending on day, sleep, urgency, schedule and previous behavior.

Wake My Way learns the combination of interventions that works for the individual and uses the **minimum effective friction** required that morning.

## Why AI belongs here

AI is valuable because spoken wake-up interaction benefits from variation, context, natural language, memory and personality. The AI can make a strategy feel human rather than robotic.

AI is **not** the product controller.

The deterministic wake strategy decides the goal, such as:

```text
REQUEST_SIT_UP
REQUEST_MOVEMENT
REENGAGE
SNOOZE_NEGOTIATION
MORNING_ORIENTATION
```

The character/voice layer decides how to express it.

Example:

```text
Strategy intent: REQUEST_STAND

Alfred:
"Feet on the floor, sir."

Sam:
"Okay. Feet down first."

Chaos:
"Today's first boss fight is gravity. Let's go."
```

## Core loop

```text
NIGHT
  |
  v
Set wake time
  |
Tell Wake My Way what matters tomorrow
  |
Wake Plan prepared locally
  |
  v
MORNING
  |
Native alarm fires
  |
Character engages user
  |
Wake Strategy observes response + movement
  |
Adapt / escalate only as needed
  |
Activation criterion met
  |
Short orientation
  |
Choose First Move
  |
Session complete
  |
  v
LEARNING
  |
Outcome becomes signal for future mornings
```

## User promise

Wake My Way should become able to say, in effect:

> "I do not only know that you want to wake at 08:00. I have learned how you behave at 08:00 and what usually gets you moving."

## Target initial user

The first target is a **habitual snoozer who genuinely wants to wake on time**.

Typical statements:

- "I set five alarms."
- "I turn alarms off without remembering."
- "If someone actually talks to me, I wake up."
- "I wake up, but I stay in bed."
- "On important mornings I ask someone to call me."

The product should automate the useful part of a trusted person making sure the user really gets up, without becoming controlling or humiliating.

## V1 scope

V1 contains:

- reliable native Android alarm
- one active Wake Schedule in V1, with weekday-specific times if needed
- one scripted character first, Alfred
- conversational wake runtime
- Tomorrow Contract
- adaptive escalation
- motion-based wake signals
- behavioral activation evidence inside the deterministic Wake Runtime
- prepared local wake plan and offline fallback
- short morning orientation
- Calendar and weather later in V1 sequence
- history and basic learning

## Explicitly out of scope for V1

- iOS implementation
- full sleep tracking
- sleep-stage alarm scheduling
- general task management
- journaling
- news briefings
- Gmail/email briefing
- social wake messages
- smart-home integrations
- Apple Watch / Wear OS companion
- QR/barcode/photo missions
- step permission unless evidence proves it is necessary
- celebrity voices
- dozens of characters
- gamification economy

## Long-term possibility

If Wake My Way proves that adaptive wake strategy improves real wake outcomes, the concept can expand to:

- iOS
- wearables
- bedside hardware
- smart lights/speakers
- trusted-person wake messages
- richer contextual routines

These are optional extensions. The core identity remains waking effectively and personally.

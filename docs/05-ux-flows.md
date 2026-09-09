# UX flows

## Philosophy

The app has two primary temporal contexts:

```text
TONIGHT -> TOMORROW MORNING
```

The normal evening setup should take roughly 20-40 seconds. The morning runtime is audio-led and progressively reveals information as the user becomes more awake.

The UX must preserve a critical distinction:

- WMW may determine **Activation Completion** from behavioral evidence during the wake session.
- It may later ask occasionally whether the user actually got up, calibrating **Confirmed Wake Success**.

Do not force a fully awake analytics question into the alarm moment.

# First-run / alarm setup

## 1. Entry

```text
Wake My Way

An alarm that learns
what works for you.

[ Set tomorrow's alarm ]

No account required.
```

## 2. Wake time

```text
When do you need
to be up?

        08 : 00

Tomorrow - Thursday

[ Continue ]
```

Optional contextual line:

> That gives you 7h 42m until wake-up.

Do not turn this into sleep tracking.

## 3. Initial wake difficulty

```text
How much should I trust morning-you?

🙂 Pretty reliable
   I usually get up.

😴 Snoozer
   I need some convincing.

💀 Don't trust me
   I will try to go back to sleep.
```

This initializes strategy only. Learned behavior overrides the crude self-assessment over time.

## 4. Character

Initial public concepts:

### Sam
Warm, calm, persistent.

> "You don't need to do the whole day yet. Just sit up."

### Alfred
Dry, polite, direct.

> "Good morning, sir. We have already agreed that eight means eight."

### Chaos
Funny, unpredictable, bounded.

> "Unfortunately, consciousness has been renewed for another day."

Implementation begins with Alfred only.

Character selection should communicate behavioral presence, not merely "Voice 1 / Voice 2."

## 5. Tomorrow Contract

```text
What's worth getting up for tomorrow?

Tell Wake My Way anything you don't want
morning-you to forget.

              🎙
```

Example:

> "I have an interview at ten and I want time to shower and eat."

Structured summary:

```text
Tomorrow matters because:

Interview at 10:00.
You want enough time to shower and eat.

[ Looks right ]
```

The user may skip.

## 6. Context permission

Calendar is optional.

```text
Want Wake My Way to understand tomorrow?

Calendar
Know what's coming up.
[ Connect ]

Weather
Useful automatically when location/city is available.
```

Never block base alarm setup on context permissions.

## 7. Wake Readiness setup

The current implementation direction prefers `USE_EXACT_ALARM` for WMW's dedicated alarm-clock use case, but UI must still be driven by **actual platform capability**, not by documentation assumptions.

The product explains the outcome, then opens a system repair flow only when the implemented Android path actually requires one:

```text
08:00 should mean 08:00.

Wake My Way checks that Android can deliver
your next wake occurrence on time.

[ Check wake readiness ]
```

If repair is required, explain one capability at a time with platform-specific copy. Examples include exact-alarm capability, notifications, or full-screen alarm presentation. Never imply that a normal full-screen Activity is guaranteed in every unlocked/locked device state.

Optional microphone/calendar/weather permissions are separate and never block base Wake Ready status.

## 8. Optional Safety Backup during trust-building

For founder/trusted dogfood, or possibly very early onboarding if evidence supports it, WMW may offer a later conventional Safety Backup alarm while the user builds trust.

Example:

```text
New alarm apps take trust.

Keep a safety backup at 08:20
for your first few mornings?

[ Add safety backup ]
[ No, I'm good ]
```

Rules:

- optional, never fear-based
- clearly described as temporary/trust-building
- not another adaptive Wake Schedule
- WMW's own reliability is measured independently even if backup exists
- easy to remove later
- do not architect generic multi-alarm coordination around this feature

Whether this reaches public V1 is evidence-driven. It may remain dogfood-only.

## 9. Ready screen

```text
              08:00

            Tomorrow

             Alfred

--------------------------------
Interview at 10:00
Shower first.
--------------------------------

           Wake Ready ✓

             [ Edit ]
```

If a Safety Backup is enabled during dogfood, show it quietly as secondary reassurance rather than a second primary schedule.

This is the primary home state.

# Morning runtime

## Critical interaction rule

The alarm's critical playback lifetime is independent from the visible `WakeActivity`. UI recreation must not silence the alarm. The user experience should reconnect to the active occurrence without showing a fake restart or starting duplicate sound.

## State A: Emerging

At alarm time, local audio begins without waiting for network.

Audio:

```text
[ short Wake Motif ]

"Yotam."

[pause]

"Morning."
```

Screen:

```text
              08:00

              WMW

             Alfred

        [ subtle presence ]
```

No calendar. No paragraph. No complex choices.

## State B: Engaged

After voice/touch response:

```text
              08:00

        "Morning, Yotam."

           ● listening
```

If user says "no...":

> "A compelling argument. Unfortunately, you have an interview at ten. Sit up first."

The strategy goal is explicit internally:

```text
TARGET: USER_SITTING_UP
```

## State C: Active / movement

```text
          Feet on the floor.

         ----------------

              ● moving
```

Meaningful motion triggers a concise acknowledgement:

> "Good."

If the user claims to be awake but no relevant signals are present, character copy may gently call out the mismatch:

> "An ambitious interpretation of 'up'. Your phone has not moved."

The Wake Runtime, not AI, determines whether additional activation evidence is needed.

## Escalation

Escalation is behaviorally justified, not dramatic for entertainment.

Example levels:

1. voice re-engagement
2. stronger/direct prompt
3. explicit movement request
4. sustained movement evidence

QR/photo/math missions are not V1.

## Intentional Stop

The user must always have a real way to stop the alarm.

Do not make Stop the giant default reflex target, but do not hide it or turn it into a coercive puzzle.

Preferred interaction direction:

```text
secondary visible action:
Stop alarm
```

Depending on accessibility/dogfood testing, tapping it may use one lightweight intentional step such as:

```text
Stop waking me?

[ Stop alarm ]
[ Keep going ]
```

or a simple deliberate hold/gesture **only if** it remains easy with TalkBack, motor limitations, large text, lock-screen presentation, and sleepy use.

Hard rules:

- no math/QR challenge to stop
- no network/AI dependency
- no hidden close affordance
- accessible from touch even if microphone is unavailable
- emergency/clear exit always exists
- once the kernel accepts Stop, component recreation cannot resurrect the occurrence

The purpose is to prevent unconscious reflex taps where possible, not to trap the user.

## Snooze

Secondary action:

```text
I need a little longer
```

Then:

```text
Five more minutes?

[ Yes, five ]
[ Keep waking me ]
```

If accepted, WMW schedules a real exact Snooze Occurrence **before** the old active alarm execution is terminated.

At return:

> "Our negotiated five minutes have expired."

## Orientation

Only after sufficient activation evidence:

> "Okay. You're moving. Your interview is at ten. Nothing else before then. It's raining, so don't leave too late."

Keep roughly 10-20 seconds unless context truly matters.

Avoid treating a phone-observable threshold as omniscient proof. The system can confidently move the session forward without claiming medical certainty.

## First Move

```text
What's first?

[ Shower ]     [ Coffee ]
```

User chooses:

> "Shower."

Character:

> "Good. Go shower. I'm done."

## Completion

Runtime completion is an **Activation Completion** event.

User-facing copy can remain natural and concise:

```text
          MORNING STARTED ✓

             08:06

      6m 14s to get moving

--------------------------------
Next:
🚿 Shower

[ Finish ]
```

Alternative copy such as `YOU'RE UP` may be tested, but analytics/domain state must still record Activation Completion rather than pretending the phone has confirmed the rest of the morning.

The product stops talking.

# Example 90-second wake

```text
08:00:00  Local motif + "Yotam. Morning. It's eight."
08:00:09  User: "No..."
08:00:11  "Interview at ten. You wanted time to shower and eat."
08:00:20  "Sit up."
08:00:31  Device picked up
08:00:32  "Good."
08:00:38  "Feet on the floor."
08:00:54  User: "I'm up."
08:00:56  Activation evidence still insufficient
08:01:02  User laughs
08:01:04  "Yes, hilarious. Stand up."
08:01:16  Meaningful movement detected
08:01:18  "Much better."
08:01:25  Short morning orientation
08:01:36  "Coffee or shower first?"
08:01:41  User: "Shower."
08:01:43  "Excellent. Go shower."
08:01:46  Activation Completion / session completion
```

# History

Keep history emotionally legible, not analytics-heavy.

Do not show a misleading precision-heavy success percentage until enough calibration exists.

Early shape:

```text
THIS WEEK

Mon  08:03 ✓
Tue  08:11 ~
Wed  07:59 ✓
Thu  08:05 ✓
Fri  08:02 ✓

Average time moving   4m 38s
1m 12s faster than last week
```

Once Confirmed Wake Success coverage is meaningful, insights can distinguish:

```text
Started moving on time     82%
Confirmed got up           74%  (based on 19 answered mornings)
```

Do not present missing feedback as confirmed success.

Derived insight:

> Movement works better when we ask earlier.

or:

> One short snooze seems fine. A second usually makes this morning harder.

Learning explanations should be specific but not overclaim causality.

# Feedback

There are two different feedback jobs.

## Wake style feedback

Occasionally:

```text
How was that wake-up?

😵 Too much
🙂 Good
😴 Too gentle
```

or:

```text
Gentler  ·  More like this  ·  Stronger
```

This calibrates friction/agency.

## Outcome calibration

Occasionally later, when the user is actually awake enough to answer meaningfully:

```text
Did that wake actually get you up?

[ Yep, I got up ]
[ I went back to bed ]
[ I got up later ]
[ Skip ]
```

Do not ask this every morning and do not ask it while the alarm is still active.

This calibration distinguishes Confirmed Wake Success from Activation Completion and gives Wake Learning v0 a way to detect when the runtime is stopping too early.

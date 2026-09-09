# UX flows

## Philosophy

The app has two primary temporal contexts:

```text
TONIGHT -> TOMORROW MORNING
```

The normal evening setup should take roughly 20-40 seconds. The morning runtime is audio-led and progressively reveals information as the user becomes more awake.

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

Do not hard-code a permission button until the implementation has selected the exact Android capability path for that OS/Play configuration. Some alarm capabilities are manifest/policy based; others may require user-granted special access.

The product UI explains the **outcome**, then opens a system flow only when one is actually required:

```text
08:00 should mean 08:00.

Wake My Way checks that Android can deliver
your next wake occurrence on time.

[ Check wake readiness ]
```

If repair is required, explain one capability at a time with platform-specific copy. Examples include exact-alarm access, notifications, or full-screen alarm presentation. Never imply that a normal full-screen Activity is guaranteed in every unlocked/locked device state.

Optional microphone/calendar/weather permissions are separate and never block base Wake Ready status.

## 8. Ready screen

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

This is the primary home state.

# Morning runtime

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

If accepted, WMW schedules a real exact snooze occurrence and the current process may terminate safely.

At return:

> "Our negotiated five minutes have expired."

## Orientation

Only after sufficient activity:

> "Okay. You're up. Your interview is at ten. Nothing else before then. It's raining, so don't leave too late."

Keep roughly 10-20 seconds unless context truly matters.

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

```text
          YOU'RE UP ✓

             08:06

        6m 14s to wake

--------------------------------
Next:
🚿 Shower

[ Finish morning ]
```

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
08:01:46  Activation criterion met / session completion
```

# History

Keep history emotionally legible, not analytics-heavy.

```text
THIS WEEK

Mon  08:03 ✓
Tue  08:11 ~
Wed  07:59 ✓
Thu  08:05 ✓
Fri  08:02 ✓

Wake success      82%
Average time up   4m 38s

1m 12s faster than last week
```

Derived insight:

> Conversation works better than loud escalation for you.

# Feedback

Occasionally, not every morning:

```text
How was that wake-up?

😵 Too much
🙂 Good
😴 Too gentle
```

or a smaller directional control:

```text
Gentler  ·  More like this  ·  Stronger
```

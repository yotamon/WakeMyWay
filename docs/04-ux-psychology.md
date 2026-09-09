# UX psychology of waking

## Design premise

The wake screen is not designed for a fully awake person.

Immediately after waking, users can experience **sleep inertia**: temporarily impaired alertness, reaction speed, attention and decision-making. The strongest impairment can occur in the first minutes after abrupt awakening.

Therefore:

> **Never design the first wake interaction as if the user has normal cognition.**

## Five consciousness states

The UI/behavior model follows psychological state rather than conventional app screens.

```text
ASLEEP
  |
  v
EMERGING
  |
  v
ENGAGED
  |
  v
ACTIVE
  |
  v
ORIENTED
```
> **Architecture note:** these five labels are a UX/consciousness model. They are intentionally not mapped 1:1 to runtime code states. Canonical runtime phases are defined in [`../CONTEXT.md`](../CONTEXT.md).


### Asleep

The user is not interacting. The system has one responsibility: begin a reliable wake stimulus.

### Emerging

The user may hear sound but has limited language/decision capability.

UX rules:

- almost no information
- dark visual state
- wake motif first
- short name/greeting
- pauses
- no decisions
- no briefing

### Engaged

The user has responded, touched, or spoken. They may still return to sleep.

UX rules:

- one instruction at a time
- short language
- conversational acknowledgment
- request first physical transition such as sitting up

### Active

The user is moving and cognitive capacity is rising.

UX rules:

- UI becomes brighter and more defined
- motion can react to physical movement
- direct movement cue
- limited context can begin

### Oriented

The user is sufficiently active to make small choices and understand the day.

UX rules:

- short calendar/weather orientation
- reason from Tomorrow Contract
- First Move choice
- end the session promptly

## Sleep inertia implications

Avoid in the first seconds:

- dense text
- multiple buttons
- setup questions
- motivational paragraphs
- complex choices
- calendar lists
- news
- puzzles unless explicitly required by a future strategy

Prefer:

- sound recognition
- short voice
- repeated identity cue
- simple physical request
- deliberate pauses

## Snooze psychology

WMW does not adopt the simplistic rule "snooze is always bad."

Research is mixed. Repeated snoozing can extend fragmented sleep and sleep inertia, but limited snooze may not always worsen immediate cognition for habitual snoozers.

Product interpretation:

- allow snooze
- make it intentional rather than reflexive
- measure the person's outcome
- learn whether one snooze helps or predicts further delay
- do not moralize the result

Example interaction:

```text
I need a little longer

"Five more minutes?"

[ Yes, five ]
[ Keep waking me ]
```

The system can contextualize without coercion:

> "You asked for enough time before your interview. Still want five?"

## Implementation intentions and Tomorrow Contract

Behavioral research on implementation intentions suggests that specifying a future cue and intended action can improve goal attainment.

WMW applies this gently:

```text
Tomorrow at 08:00
When Wake My Way begins -> sit up
First Move -> shower
Reason -> interview at 10:00
```

This should not look like a therapy worksheet. The user simply tells WMW what tomorrow matters for and what the first action should be.

## Auditory design

Research suggests melodic alarm sounds may reduce perceived sleep inertia or improve vigilance compared with some neutral/beeping alarms, although evidence is not definitive enough to claim a universal optimal sound.

Product direction:

1. stable sonic identity
2. short melodic Wake Motif
3. pause
4. character voice
5. subtle environmental bed if useful

This avoids jumping instantly from silence to a long AI monologue.

## Habituation

A fixed alarm can become easy to dismiss automatically. WMW balances:

- stable identity: same sonic signature and character personality
- controlled novelty: varied dialogue, context and escalation

Goal:

> familiar enough to recognize, novel enough not to ignore.

## Respectful persistence

Tone should be firm without shame.

Good:

> "Feet on the floor, sir."

> "We're moving now."

> "You don't need to feel ready. Just sit up."

Bad:

> "You're lazy again."

> "Come on sleepyhead, you can do it!"

The first attacks the person. The second infantilizes them.

## Physical activation

WMW prefers movement because the product goal is not merely consciousness. It is behavioral transition.

V1 uses low-permission signals:

- device pickup
- accelerometer movement
- orientation change
- sustained movement
- touch interaction

Step counting is intentionally excluded initially because it requires `ACTIVITY_RECOGNITION` and is not yet necessary.

## Perceived agency

A wake product can become adversarial quickly. The user should perceive WMW as something they configured **for their own future intention**, not an external authority.

Tomorrow Contract reinforces:

> past-you is asking morning-you to do this.

This is stronger and more respectful than generic motivation.

## Morning failure

Never use streak-loss shame.

If the user struggled:

> **That one took a little longer. Tomorrow we'll start movement earlier.**

The system treats difficulty as information for adaptation.

## Research references

- Sleep inertia and cognition: https://pubmed.ncbi.nlm.nih.gov/10389091/
- Sleep inertia overview / performance research: https://pmc.ncbi.nlm.nih.gov/articles/PMC3832615/
- Alarm sound and sleep inertia review: https://pmc.ncbi.nlm.nih.gov/articles/PMC7445849/
- Snooze-related sleep inertia research: https://link.springer.com/article/10.1186/s40101-022-00317-w
- Implementation intentions meta-analysis: https://doi.org/10.1016/S0065-2601(06)38002-1

These sources inform hypotheses. WMW must test its own behavioral outcomes rather than marketing medical/scientific certainty.

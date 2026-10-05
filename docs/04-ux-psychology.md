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

Early conversation is **low-pressure guidance for waking, not content to consume and not a compliance test**. The default pattern is:

```text
one safe invitation -> pause / observe -> another intervention only if it is still useful
```

The user does not need to verbally confirm every step. Speech may contribute engagement evidence, but movement or other bounded evidence can carry the wake forward without forced spoken proof. Do not ask open-ended planning questions such as "What are your plans?" while the user is still Emerging/early Engaged. Do not use arithmetic, trivia, memory tests or CAPTCHA-like puzzles as the default wake mechanism. Conversation becomes richer only after behavioral evidence increases.

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

Tone should be clear without becoming controlling. The companion can be persistent without making the user feel managed.

Good:

> "Morning. Easy start. Come up to sitting when you're ready."

> "Feet toward the floor when you're ready."

> "Fair enough. The bed has had its say."

Bad:

> "Answer me now."

> "You need to sit up and tell me when it's done."

> "You're lazy again."

> "Come on sleepyhead, you can do it!"

The first two turn the wake into a compliance exchange. The third attacks the person. The fourth infantilizes them.

## Physical activation

WMW prefers movement because the product goal is not merely consciousness. It is behavioral transition.

A 2026 exploratory randomized study found that five minutes of respiratory/muscular physiological activation after waking improved several sleep-inertia measures more consistently than cognitive stimulation after severe sleep restriction. That is promising evidence for **physiological-first** wake assistance, not proof of one universal exercise sequence.

The physical toolbox is deliberately conservative, but it is **not** a checklist that every conversational wake must complete:

1. sit upright;
2. feet down, or an equivalent safe shift out of sleep posture;
3. optional brief seated upper-body activation;
4. optional standing only if safe and normal for the user, with a seated alternative;
5. optional reachable environmental activation such as light or curtains.

The default conversational path uses a gentle sit-up invitation and at most one follow-up movement cue before shifting toward natural HoldEngagement. Later physical intents remain available for bounded strategy/tuning rather than being automatically chained after every reply.

Rules:

- at most one physical invitation per turn;
- do not require a spoken confirmation after each invitation;
- the runtime chooses interventions; the language model only renders the approved current intent;
- verbal fluency alone never gives the model permission to invent or escalate physical tasks;
- no squats, jumping, balance challenges, strenuous exercise, or rapid/forced breathing;
- standing is conditional and never a compliance requirement;
- after unclear audio/silence, make calm contact or restate one safe invitation instead of inventing a harder one;
- a spoken "done" is engagement evidence, not proof that a physical action occurred;
- sufficient motion/interaction evidence may complete activation without a spoken turn.

V1 uses low-permission signals:

- device pickup
- accelerometer movement
- orientation change
- sustained movement
- touch interaction

Step counting is intentionally excluded initially because it requires `ACTIVITY_RECOGNITION` and is not yet necessary.

The exact progression and thresholds remain dogfood hypotheses. Physical overnight evidence, return-to-bed calibration, annoyance and perceived agency decide whether they should change.

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

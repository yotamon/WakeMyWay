# Wake psychology research notes

**Status:** product research, not medical guidance  
**Updated:** 2026-09-30

WakeMyWay should not optimize for the moment a person dismisses an alarm. It should optimize for a
safe transition from sleep into sustained behavioral activation while preserving agency and minimizing
unnecessary friction.

The evidence base is uneven. Sleep inertia itself is well established. Some countermeasures are
promising but still supported by small or context-specific studies. Product behavior must therefore be
measurable and reversible rather than marketed as scientifically "perfect."

## Evidence hierarchy used by the product

- **Established:** repeated literature supports the underlying phenomenon strongly enough to shape
  default product constraints.
- **Promising:** controlled evidence supports a product hypothesis, but sample size/context/generalizability
  are not strong enough to prescribe one universal behavior.
- **Exploratory:** biologically or behaviorally plausible and useful for testing, but not a launch claim.

## Sleep inertia — established

Sleep inertia is a temporary post-awakening state in which alertness, reaction speed, sustained
attention, decision-making and subjective clarity may be impaired. Severity varies with prior sleep
loss, time of day, sleep stage and the individual.

Product consequences:

- do not assume normal cognition in the first wake turns;
- keep early language short and concrete;
- avoid open-ended questions, dense context and multi-step choices;
- distinguish **responsive** from **functionally active**;
- do not treat alarm dismissal as confirmed wake success;
- let information density increase only after behavioral activation.

References:

- https://pubmed.ncbi.nlm.nih.gov/10389091/
- https://pmc.ncbi.nlm.nih.gov/articles/PMC3832615/

## Auditory awakening — promising, not universal

A systematic review of auditory countermeasures found insufficient evidence for firm conclusions
about adult voice alarms in abrupt awakening, but positive evidence for musical treatments in
non-emergency adult awakenings, including preferred popular music and melodic alarm qualities.

A small ecological experiment (two groups of 10) found that a melodic treatment reduced attentional
lapses and false starts and improved an overall vigilance score compared with a control condition.
Reaction time itself did not significantly improve.

Product consequences:

- preserve a recognizable melodic Wake Motif as the reliable first stimulus;
- let voice emerge from/alongside the alarm rather than replace local alarm authority;
- do not assume louder, harsher or more annoying automatically means more effective;
- measure individual outcomes rather than claiming one optimal sound.

References:

- https://pmc.ncbi.nlm.nih.gov/articles/PMC7711682/
- https://pmc.ncbi.nlm.nih.gov/articles/PMC7445849/

## Physiological activation — promising and directly relevant

A 2026 randomized exploratory study of 42 healthy participants compared control, cognitive
stimulation and five minutes of respiratory/muscular "dynamization" immediately after waking.
Following severe sleep restriction, physiological dynamization produced faster psychomotor
vigilance reaction times, fewer lapses and lower subjective sleep-inertia scores within the first
20 minutes than control. Cognitive stimulation showed less consistent benefit.

This is highly relevant to WakeMyWay, but it does **not** establish one universal exercise protocol,
nor does it justify strenuous movement immediately after waking.

Product hypothesis:

> Conversation should conduct a gradual physical transition instead of trying to talk the user
> into wakefulness.

Safe default progression for conversational dogfood:

1. sit upright;
2. feet down / equivalent safe shift out of sleep posture;
3. brief seated upper-body activation;
4. stand only if safe and normal for the user, with a seated alternative;
5. simple reachable environmental activation such as light/curtains.

Guardrails:

- one action per turn;
- no squats, jumping, balance tasks or strenuous exercise;
- no rapid/forced breathing protocol;
- standing is never required when unsafe or inappropriate;
- AI cannot claim an action happened; WakeRuntime owns progression and activation evidence.

Reference:

- Beauchamps V, Sauvet F, Boyer S, Nemmi F, Buratto F. *Evaluate the effectiveness of cognitive
  stimulation and physiological dynamization on sleep inertia*. Frontiers in Physiology. 2026.
  https://doi.org/10.3389/fphys.2026.1855408

## Cognitive challenges — insufficient as the primary strategy

Wake puzzles and arithmetic have intuitive appeal because they require mental effort, but current
evidence does not justify making them WakeMyWay's primary wake mechanism. The 2026 comparison above
found physiological activation more consistent than cognitive stimulation.

Product consequence:

- do not use arithmetic/trivia/CAPTCHA-like tasks as the default wake path;
- cognitive interaction should remain low-load until the user is already behaviorally active;
- a future optional strategy would require direct outcome evidence and an agency/annoyance review.

## Personally salient voice cues — exploratory for awakening

EEG/event-related-potential studies show that a sleeping brain can differentiate the subject's own
name from other names during stage 2 and REM sleep. That establishes preserved processing of salient
auditory information; it does **not** prove that speaking a person's name wakes them faster.

Product consequence:

- a stored preferred name may be used sparingly as a personal contact cue when available;
- never market name use as a proven awakening countermeasure;
- avoid repeating the name mechanically, which would damage naturalness and may habituate.

References:

- https://pubmed.ncbi.nlm.nih.gov/10616121/
- https://pubmed.ncbi.nlm.nih.gov/12445951/

## Snoozing — mixed evidence

The product must not moralize snoozing.

One small laboratory study associated snooze alarms with greater sleep inertia. A later study of
31 habitual snoozers found that a 30-minute snooze period improved or did not impair immediate
cognitive performance compared with abrupt waking, with about six minutes less total sleep and fewer
awakenings from N3. These findings apply to specific samples and do not establish a universal ideal
snooze strategy.

Product consequences:

- keep snooze intentional and explicit;
- measure whether snooze predicts Confirmed Wake Success or return-to-bed for the individual;
- do not label snooze as failure;
- do not silently learn aggressive anti-snooze behavior from weak evidence.

References:

- https://pubmed.ncbi.nlm.nih.gov/36587230/
- https://pubmed.ncbi.nlm.nih.gov/37849039/

## Conversation psychology

The voice agent talks to a person whose cognitive capacity may be temporarily impaired. The useful
conversation pattern is therefore:

```text
contact -> one simple action -> short reply -> next action -> repeat only as needed
```

Early-turn rules:

- yes/no or one-short-reply prompts;
- no "How are you feeling?" or "What are your plans?";
- no long motivational framing;
- no praise for routine compliance;
- acknowledge bargaining/complaints without arguing;
- repeat the current action after unclear audio instead of escalating to a new one;
- increase personality and context only as behavioral evidence accumulates.

The goal is cooperative persistence:

> user + wake companion vs sleep inertia

not:

> user vs alarm

## What the product can truthfully infer

WakeMyWay can observe bounded behavioral evidence such as:

- intentional interaction;
- usable spoken engagement;
- device pickup;
- orientation change;
- sustained phone movement;
- explicit calibration after the fact.

It cannot medically verify wakefulness, posture, gait safety, sleep stage or whether a spoken
"done" proves a requested action happened.

Therefore:

- WakeRuntime, not the model, owns activation thresholds and phase transitions;
- model language must never claim unsupported physical state;
- Activation Completion remains separate from Confirmed Wake Success;
- return-to-bed calibration is especially valuable for detecting false-positive activation.

## Success metric

The product should optimize for a sustained morning outcome rather than conversation length.

Primary product evidence:

- Activation Completion;
- Confirmed Wake Success / returned-to-bed calibration.

Supporting measures:

- time to first meaningful engagement;
- time to sustained movement;
- time to Orientation;
- intervention/escalation depth;
- snooze count;
- annoyance;
- perceived agency;
- conversation/provider failure rate.

## Research principles for WakeMyWay

- Do not overclaim scientific certainty in marketing.
- Treat physiological activation as a promising product hypothesis, not medical advice.
- Prefer safe bounded behavior over model improvisation.
- Separate subjective annoyance from objective activation evidence.
- Expect large individual variation.
- Tune from real overnight dogfood/beta evidence.
- Change one learned parameter at a time and preserve reversibility.
- Keep raw audio/transcripts/high-frequency motion out of retained learning data.

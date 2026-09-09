# Product metrics and experiments

## North star

### Wake Success

The percentage of intended Wake Occurrences where the configured behavioral activation criterion is reached within the target wake window.

This is more aligned with the job than DAU, alarm count, or conversation length. It is a behavioral product proxy, not a claim of medically verified wakefulness.

## Core metrics

| Metric | Why it matters |
|---|---|
| Time to first engagement | stimulus → meaningful interaction |
| Time to first meaningful movement | cognitive → physical activation |
| Time to activation criterion | primary outcome latency |
| Snoozes per wake | behavioral pattern |
| Wake Success after snooze | whether snooze helps this person |
| Intervention depth | amount of friction required |
| Premature-success / return-to-bed proxy | whether we stop too early |
| Voice turns before activation | conversational efficiency |
| Fallback rate | richness/provider reliability |
| Alarm trigger/audio delay | core trust |
| Morning annoyance | retention constraint |
| Perceived agency | supportive vs coercive experience |
| D7/D30 retention | sustained trust/habit |

## Wake Friction

WMW optimizes effectiveness subject to acceptable friction/annoyance.

```text
normal alarm      → low friction, often insufficient for target segment
punishment alarm  → high friction, potentially effective but adversarial
WMW target        → minimum effective friction for this person/context
```

## Dogfood baseline

Compare WMW against the user's normal alarm behavior over enough mornings to reduce anecdotal noise.

Possible framing only:

```text
Normal alarm: target → meaningful activity delay
WMW:          target → activation criterion delay
```

Do not hardcode assumed improvement before measurement.

## Early behavioral experiments

### Sound-first sequence

Compare variants only after alarm reliability is stable:

- conventional alarm stimulus
- melodic Wake Motif
- voice immediately
- motif → pause → voice

### First intervention

- question
- short request/command
- self-supplied reason/context reminder

### Movement timing

- movement request early
- after first engagement
- only after failed engagement

### Tomorrow Contract

Measure impact on engagement, movement, Wake Success, and annoyance.

### Snooze

Measure per-user effects of:

- one intentional snooze
- repeated snooze
- fixed vs later personalized duration

### Character style

Measure outcome + annoyance + agency, not preference alone.

## Experiment guardrails

- Never intentionally reduce critical alarm-delivery reliability for an experiment.
- Never A/B test inaccessible dismiss controls, deceptive urgency, shame, or coercive dark patterns.
- Keep a policy version fixed for the entire Wake Session so results are interpretable.
- Do not infer medical sleep/wake states from behavioral telemetry.

# Product metrics and experiments

## Measurement model

Wake My Way must not allow its own internal activation threshold to become circular proof that the product worked.

There are therefore two distinct outcome layers.

### Activation Completion

The percentage of intended Wake Occurrences where the Wake Runtime's configured behavioral activation criterion is reached within the target wake window.

This is the immediate operational metric used by the deterministic runtime. It is observable from phone-side behavioral evidence and is useful for latency, policy and reliability analysis.

### Confirmed Wake Success

The calibrated product outcome: evidence indicates the user actually achieved the intended real-world wake result rather than merely satisfying phone-observable activation and then returning to bed.

Evidence may include occasional lightweight user feedback and future privacy-safe return-to-bed proxies. Confirmation is deliberately sparse; WMW should not ask every morning.

### North star

**Wake Success** is the real-world product goal. During early dogfood, report both Activation Completion and the available Confirmed Wake Success calibration rather than collapsing them into one number.

This is more aligned with the job than DAU, alarm count, conversation length, or an internally self-certified score. Neither metric is a claim of medically verified wakefulness.

## Core metrics

| Metric | Why it matters |
|---|---|
| Alarm delivery success | foundational trust |
| Active execution survival | alarm remains actionable through UI/process churn |
| Time to first engagement | stimulus → meaningful interaction |
| Time to first meaningful movement | cognitive → physical activation |
| Time to Activation Completion | runtime outcome latency |
| Confirmed Wake Success | calibrated real-world outcome |
| Activation → return-to-bed rate | whether the runtime stops too early |
| Snoozes per wake | behavioral pattern |
| Confirmed success after snooze | whether snooze helps this person |
| Intervention depth | amount of friction required |
| Voice turns before activation | conversational efficiency |
| Fallback rate | richness/provider reliability |
| Alarm trigger/audio delay | core trust |
| Morning annoyance | retention constraint |
| Perceived agency | supportive vs coercive experience |
| Safety Backup usage | whether users trust WMW enough to remove the transition aid |
| D7/D30 retention | sustained trust/habit |

## Calibration feedback

Occasionally ask a lightweight question after the morning or later in the day, not during the cognitively vulnerable alarm moment.

Example semantic options:

```text
GOT_UP
RETURNED_TO_BED
GOT_UP_LATER
NOT_SURE / SKIPPED
```

The exact UI is a product decision. The important rule is that calibration feedback remains optional and low-friction.

Use these answers to measure false-positive Activation Completion and to tune future policies/activation thresholds. Do not reinterpret an internal runtime event as confirmation simply because feedback is missing.

## Wake Friction

WMW optimizes effectiveness subject to acceptable friction/annoyance.

```text
normal alarm      → low friction, often insufficient for target segment
punishment alarm  → high friction, potentially effective but adversarial
WMW target        → minimum effective friction for this person/context
```

Useful analysis should consider a tuple rather than a single score:

```text
Confirmed Wake Success
+ activation delay
+ intervention depth
+ annoyance
+ perceived agency
```

## Dogfood baseline

Compare WMW against the user's normal alarm behavior over enough mornings to reduce anecdotal noise.

Possible framing only:

```text
Normal alarm: target → actually up / later self-report
WMW:          target → Activation Completion → Confirmed Wake Success calibration
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

Measure impact on engagement, movement, Activation Completion, Confirmed Wake Success calibration, and annoyance.

### Snooze

Measure per-user effects of:

- one intentional snooze
- repeated snooze
- fixed vs later personalized duration

### Wake Learning v0

After enough local outcomes exist, compare the versioned learned policy to the prior/default policy using bounded, explainable changes.

Measure:

- whether Confirmed Wake Success improves
- whether activation delay falls
- whether intervention depth/annoyance rises
- whether adaptations remain stable instead of oscillating

Learning must never optimize Activation Completion alone if calibration indicates the user frequently returns to bed afterward.

### Character style

Measure outcome + annoyance + agency, not preference alone.

### Safety Backup

During founder/trusted dogfood, observe whether users keep or remove a later conventional safety alarm. The goal is not to maximize removal; it is to understand when trust is earned.

## Experiment guardrails

- Never intentionally reduce critical alarm-delivery or active-execution reliability for an experiment.
- Never A/B test inaccessible dismiss controls, deceptive urgency, shame, or coercive dark patterns.
- Keep a policy version fixed for the entire Wake Session so results are interpretable.
- Do not infer medical sleep/wake states from behavioral telemetry.
- Do not redefine the activation criterion mid-experiment merely to make reported success improve.
- Preserve the distinction between Activation Completion and Confirmed Wake Success in historical analysis.

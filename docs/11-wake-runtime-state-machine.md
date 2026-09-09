# Wake Runtime

Canonical domain terms are defined in [`../CONTEXT.md`](../CONTEXT.md).

## Responsibility

Wake Runtime is the deep deterministic module for one active Wake Session:

> Given the current session snapshot, a typed Wake Input, and a versioned Wake Policy, decide the next session snapshot and Wake Directives.

The caller should not coordinate activation scoring, escalation rules, snooze rules, and phase transitions separately.

## Runtime model

The original design had too many states that mixed lifecycle, interventions, evidence, and capability modes. The simplified model keeps only durable session phases.

```text
ALERTING
   |
   v
ENGAGING
   |
   v
ACTIVATING
   |
   v
ORIENTING
   |
   v
FINISHED
```

A session may finish with an outcome such as completed, intentionally snoozed, cancelled, or unrecoverable termination.

## Phase meaning

### Alerting

Reliable audible wake stimulus is active. The runtime is trying to obtain first meaningful interaction.

### Engaging

The user has shown initial interaction, but the product does not yet have sufficient behavioral activation evidence. Short conversation and simple physical requests are appropriate.

### Activating

The runtime is actively seeking stronger behavioral evidence, normally movement/continued interaction. Escalation may change here, but **Escalating is not a phase**.

### Orienting

Sufficient activation evidence exists to introduce small amounts of context and, optionally, a First Move.

### Finished

The wake attempt no longer occupies the active runtime. The outcome records whether it completed, snoozed, cancelled, or terminated unexpectedly.

## Concepts intentionally not modeled as phases

### Prepared

Preparation/readiness belongs to Wake Occurrence / Alarm Kernel state before a Wake Session exists.

### Escalating

Escalation is a policy decision/parameter, not a lifecycle phase. A session may increase or change intervention while remaining Engaging or Activating.

### Mobility Check

Movement observation is a Wake Directive/strategy action, not a phase.

### Verified Awake

Removed. Phone sensors cannot medically verify wakefulness. Runtime uses internal **Activation Evidence** to decide when to advance to Orienting.

### First Action

First Move is optional orientation data/interaction, not a lifecycle phase.

### Emergency Fallback

Fallback level is capability/runtime mode, not lifecycle state.

## Wake Inputs

Inputs are typed facts, not generic bags and not commands from AI.

Representative categories:

```text
AlarmFired
WakeSurfacePresented
UserInteracted
VoiceResponseObserved
MotionObserved
SilenceElapsed
SpeechFinished / SpeechFailed
SnoozeRequested
SnoozeConfirmed
SnoozeScheduled
CapabilityChanged
```

Exact type design is an implementation decision, but inputs should make invalid combinations hard and should not rely on nullable `textValue` / `numericValue` fields in domain code.

## Wake Directives

Representative categories:

```text
EnsureAlarmAudible
Speak(SpeechIntent)
ObserveMotion
StopObservingMotion
OfferSnooze
RequestSnoozeSchedule
PresentOrientation
CompleteSession
```

Android/application code executes directives and feeds behavior-relevant results back as inputs.

The exact public contract should be designed from M3 usage scenarios, not frozen from documentation pseudocode.

## Wake Policy

Wake Runtime receives a versioned Wake Policy containing deterministic parameters such as:

- intervention timing
- activation evidence thresholds
- escalation preferences
- snooze parameters
- context timing
- character-independent behavioral rules

For a new user this is the default policy. Later Wake Learning can derive a personalized policy.

## Activation Evidence

Wake Runtime may internally combine:

- meaningful interaction
- device pickup
- sustained movement
- orientation change
- coherent/continued response
- silence/stationary time
- snooze behavior

An internal scalar/probability may be useful for diagnostics and tuning, but it is **not a public module or authority seam**. Callers ask Wake Runtime what to do next; they do not calculate a score and then decide what to do with it.

## AI rule

AI can render a constrained Speech Intent. It cannot:

- choose Wake Phase
- mark the session successfully activated
- dismiss/finish the alarm
- approve snooze without explicit user confirmation and successful exact rescheduling
- invent product facts/context

If generated language says "you're up" before the runtime has enough activation evidence, that is a rendering defect, not a state transition.

## Replay and timeline

Persist enough typed inputs and execution observations to replay/diagnose a session against its recorded runtime/policy version.

Do not make the persistence schema itself the domain interface.

Replay should answer:

- what facts entered the runtime?
- what phase/policy version was active?
- what directives were selected?
- which external effects succeeded/failed?

## Behavior invariants

Examples:

- a Finished session never becomes active again
- network/provider availability never prevents the Alerting path from starting
- AI output never directly changes Wake Phase
- snooze does not finish the active alarm until the replacement exact occurrence is successfully scheduled
- duplicate inputs/effect results do not cause duplicate destructive behavior
- Orienting is reachable only after the configured activation criterion is met
- fallback richness can decrease without invalidating the Wake Session lifecycle

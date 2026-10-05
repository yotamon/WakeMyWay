# Wake Runtime

Canonical domain terms are defined in [`../CONTEXT.md`](../CONTEXT.md).

Implementation detail and current M3 status live in [`implementation/m3-wake-runtime.md`](implementation/m3-wake-runtime.md).

## Responsibility

Wake Runtime is the deep deterministic module for one active Wake Session:

> Given the current session snapshot, a typed Wake Input, and a versioned Wake Policy, decide the next session snapshot and Wake Directives.

The caller should not coordinate activation scoring, escalation rules, snooze/stop completion, and phase transitions separately.

## Runtime model

The simplified model keeps only durable session phases:

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

A session may finish with an outcome such as completed, intentionally snoozed, stopped, or unrecoverable termination.

## Phase meaning

### Alerting

Reliable audible wake stimulus is active. The runtime begins the wake attempt and establishes the first minimal intervention. This phase does not imply that the user has responded.

### Engaging

The runtime is making a simple engagement attempt such as asking the user to sit up or beginning motion observation. It may be entered because the user responded, because initial speech completed, because speech failed, or because a no-response timer elapsed. **Entering Engaging is intervention progression, not evidence that the user is awake.**

### Activating

The runtime is actively seeking stronger behavioral evidence, normally movement/continued interaction. Escalation may change here, but **Escalating is not a phase**. No-response progression can move the intervention here, but silence/time never contributes Activation Evidence.

### Orienting

Sufficient activation evidence exists to introduce small amounts of context and, optionally, a First Move. Late activation callbacks are treated as stale here so delayed timers, speech completions, or sensor events cannot drag the session backward or re-present orientation.

### Finished

The wake attempt no longer occupies the active runtime. The outcome records whether it completed, snoozed, stopped, or terminated unexpectedly.

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

The first M3 implementation uses:

```text
AlarmFired
WakeSurfacePresented
UserInteracted
VoiceResponseObserved
MotionObserved
SilenceElapsed
SpeechFinished
SpeechFailed
SnoozeRequested
SnoozeConfirmed
SnoozeScheduled
SnoozeSchedulingFailed
CapabilitiesChanged
OrientationCompleted
StopRequested
StopCompleted
StopFailed
UnrecoverableFailure
```

Each input has a stable `WakeInputId` so recent redelivery can be rejected deterministically. Inputs do not use nullable generic value bags.

## Wake Directives

The first M3 implementation uses:

```text
EnsureAlarmAudible
Speak(SpeechIntent)
ObserveMotion
StopObservingMotion
OfferSnooze
RequestSnoozeSchedule
RequestStopExecution
PresentOrientation
CompleteSession
```

Android/application code executes directives and feeds behavior-relevant success/failure results back as typed inputs.

## Wake Policy

Wake Runtime receives a versioned Wake Policy containing deterministic parameters such as:

- activation evidence threshold
- evidence weights
- escalation ceiling
- bounded verbal re-engagement prompt allowance
- default snooze duration
- bounded duplicate-input memory

For a new user this is the default policy. Later Wake Learning can derive a personalized policy while preserving the same reducer authority boundary. A Wake Session never silently changes policy version mid-session.

## Activation Evidence

Wake Runtime currently combines typed counts for:

- meaningful interaction
- device pickup
- sustained movement
- orientation change
- coherent response

The weighted scalar is available only through runtime diagnostics for replay/tuning. It is **not** a public authority seam. Callers ask Wake Runtime what to do next; they do not calculate a score and then decide what to do with it.

Silence, timer expiry, escalation level, speech completion, and speech failure are **not** Activation Evidence. They may cause a stronger intervention, but they cannot move a session into Orienting on their own.

## Conversational physiological progression

When two-way voice is available, Wake Runtime still owns intervention selection and the activation gate. Realtime does not decide what physical step comes next, but conversation is **not** a compliance channel.

The default dogfood path is intentionally shallow:

```text
InitialWake       -> gentle invitation to sit upright
1st coherent turn -> one small movement invitation: feet down / safe equivalent
later coherent turns -> HoldEngagement; no automatic physical checklist
```

Upper-body activation, conditional standing and reachable environmental activation remain bounded safe intents in the runtime vocabulary, but the default conversation does not automatically march through them simply because the user keeps replying.

A coherent spoken reply contributes bounded Activation Evidence, while touch and motion remain independent evidence channels. The activation gate is based on the configured evidence score, not on a mandatory voice turn. If movement or other bounded evidence already reaches the threshold, Wake Runtime may enter Orienting even when the microphone is available and the user has said nothing.

A spoken reply is therefore useful evidence, not proof of obedience and not proof that a requested physical action occurred. Silence or unusable audio earns no activation evidence. The default policy permits at most **one spoken re-engagement prompt per Wake Session** across silence and unusable-audio events. After that, the alarm and motion observation continue without further verbal solicitation unless the user voluntarily re-enters conversation. Re-engagement must never frame silence as refusal, demand verbal proof, or invent a harder task.

This keeps a useful separation:

```text
WakeRuntime: which bounded intervention / whether enough evidence exists
AI renderer: natural low-pressure wording for that approved intent
Sensors/turn gate: typed observations only
```

## Snooze transaction

Snooze uses a durable-effect handshake:

```text
SnoozeRequested
   ↓
SnoozeConfirmed
   ↓
RequestSnoozeSchedule
   ↓
Alarm Kernel durably creates replacement occurrence
   │
   ├─ SnoozeScheduled ───────→ FINISHED / SNOOZED
   │
   └─ SnoozeSchedulingFailed → remain active + audible
```

Out-of-order `SnoozeConfirmed` or `SnoozeScheduled` inputs are no-ops. The runtime never declares snooze complete before exact replacement scheduling succeeds.

While exact snooze scheduling is in flight, it is the exclusive destructive lifecycle transaction. Competing Stop/orientation completion inputs cannot race it. The directive executor must bound the scheduling attempt and feed back success or failure so the session cannot remain stuck indefinitely.

## Stop transaction

Stop follows the same discipline instead of treating a button tap as durable completion:

```text
StopRequested
   ↓
STOPPING
   ↓
RequestStopExecution
   ↓
Alarm Kernel performs durable active-wake stop/advance
   │
   ├─ StopCompleted → FINISHED / STOPPED
   │
   └─ StopFailed    → remain active + audible
```

While Stop is in flight, unrelated behavioral inputs may be recorded for idempotency but cannot mutate the lifecycle or emit competing actions. An out-of-order `StopCompleted` cannot finish the session.

## AI rule

AI can render a constrained Speech Intent. It cannot:

- choose Wake Phase
- mark the session successfully activated
- dismiss/finish the alarm
- approve snooze without explicit user confirmation and successful exact rescheduling
- claim Stop succeeded before the Alarm Kernel confirms it
- invent product facts/context

If generated language says "you're up" before the runtime has enough activation evidence, that is a rendering defect, not a state transition.

## Replay and timeline

`WakeRuntime.replay()` feeds recorded typed inputs through the same reducer used live and returns every transition plus the final snapshot.

Replay should answer:

- what facts entered the runtime?
- what phase/policy version was active?
- what directives were selected?
- which external effects succeeded/failed?

Do not make the persistence schema itself the domain interface.

## Behavior invariants

- a Finished session never becomes active again
- network/provider availability never prevents the Alerting path from starting
- AI output never directly changes Wake Phase
- no-response may strengthen intervention but never counts as Activation Evidence or wake success
- snooze does not finish until the replacement exact occurrence is successfully scheduled
- an in-flight snooze scheduling transaction must resolve before competing destructive lifecycle actions proceed
- stop does not finish until durable Alarm Kernel execution is confirmed
- duplicate inputs do not cause duplicate destructive behavior
- stale activation callbacks cannot re-engage an Orienting session
- Orienting is reachable only after the configured activation criterion is met
- fallback richness can decrease without invalidating the Wake Session lifecycle
- escalation level is bounded by policy and never becomes a lifecycle state

# M3 Wake Runtime implementation

## Status

The first pure-Kotlin Wake Runtime core is implemented on `feat/m3-wake-runtime` / PR #13.

M3 is intentionally being developed in parallel with the remaining M2 physical-device evidence. The runtime core may be merged after its own deterministic tests are green, but wiring richer behavior into the critical Android wake path remains gated by Alarm Kernel reliability evidence.

## Responsibility

`WakeRuntime` is the single behavioral authority for one active Wake Session:

```text
WakeSessionSnapshot + WakeInput + WakePolicy
                    │
                    ▼
               WakeRuntime
                    │
                    ▼
            WakeTransition
          ├─ next snapshot
          └─ Wake Directives
```

Callers do not independently calculate activation confidence, decide phase transitions, or coordinate snooze completion.

## Implemented phases

```text
ALERTING
   ↓
ENGAGING
   ↓
ACTIVATING
   ↓
ORIENTING
   ↓
FINISHED
```

`Escalating`, movement checks, fallback mode, and First Move are not lifecycle phases.

## Implemented typed inputs

- `AlarmFired`
- `WakeSurfacePresented`
- `UserInteracted`
- `VoiceResponseObserved`
- `MotionObserved`
- `SilenceElapsed`
- `SpeechFinished`
- `SpeechFailed`
- `SnoozeRequested`
- `SnoozeConfirmed`
- `SnoozeScheduled`
- `SnoozeSchedulingFailed`
- `CapabilitiesChanged`
- `OrientationCompleted`
- `StopRequested`
- `UnrecoverableFailure`

Each input has a stable `WakeInputId`. Recently processed IDs are retained in the session snapshot so redelivery does not repeat destructive directives.

## Implemented directives

- `EnsureAlarmAudible`
- `Speak(SpeechIntent)`
- `ObserveMotion`
- `StopObservingMotion`
- `OfferSnooze`
- `RequestSnoozeSchedule`
- `PresentOrientation`
- `CompleteSession`

Speech and motion directives are filtered by current capabilities, but capability degradation never takes authority away from the runtime or prevents the local audible path from continuing.

## Activation evidence

The runtime snapshot records typed evidence counts:

- meaningful interactions
- coherent voice responses
- device pickup
- orientation changes
- sustained movement

Weights and threshold live in the versioned `WakePolicy`.

The scalar activation score is intentionally **not** a field callers can use as a second decision seam. `WakeRuntime.diagnostics(snapshot, policy)` exposes it only for replay/tuning diagnostics. Phase advancement remains inside the runtime.

## Snooze transaction

Snooze is modeled as a handshake:

```text
SnoozeRequested
      ↓
OFFERED
      ↓
SnoozeConfirmed
      ↓
RequestSnoozeSchedule(duration)
      ↓
Android Alarm Kernel attempts durable exact replacement
      │
      ├─ SnoozeScheduled ───────→ FINISHED / SNOOZED
      │
      └─ SnoozeSchedulingFailed → remain active + audible
```

The runtime therefore cannot claim a snooze outcome before the Alarm Kernel confirms that a replacement occurrence exists.

## Replay

`WakeRuntime.replay()` folds a recorded ordered input stream through the same reducer and returns every transition plus the final snapshot.

This is the basis for later Wake Lab session replay, behavior debugging, and versioned learning analysis.

## Current deterministic test coverage

- normal Alerting → Engaging → Activating → Orienting → Finished path
- activation threshold behavior
- duplicate input idempotency
- snooze request/confirm/schedule success
- snooze scheduling failure keeps the wake active
- speech capability degradation
- deterministic silence escalation bounded by policy
- Finished is terminal
- full replay determinism

## Deferred from this slice

- Android wiring from Active Wake Execution into Wake Runtime
- real motion sensor adapter
- session persistence/event journal integration
- character-specific deterministic rendering
- Tomorrow Contract
- Wake Learning policy derivation
- realtime voice

Those remain later milestones and must preserve the same authority boundaries.

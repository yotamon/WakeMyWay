# M4 Motion Evidence implementation

## Status

M4 is being implemented on `feat/m4-motion-evidence` / issue #14.

This slice deliberately separates **motion feature extraction** from **critical-path integration**. Pure extraction and the thin Android sensor adapter may be implemented/tested while M2 physical reliability evidence remains open. Wiring richer runtime behavior into Active Wake Execution must not weaken alarm delivery or bypass the M2 evidence gate.

## Responsibility

Motion Evidence translates short-lived Android sensor observations into bounded typed facts for Wake Runtime:

```text
Android sensors
     │
     ▼
AndroidMotionObserver
     │
     ▼
MotionEvidenceExtractor
     │
     ├─ DEVICE_PICKUP
     ├─ ORIENTATION_CHANGE
     └─ SUSTAINED_MOVEMENT
              │
              ▼
      WakeInput.MotionObserved
              │
              ▼
          WakeRuntime
```

The sensor adapter and extractor do **not** decide Wake Phase, Activation Completion, Stop, Snooze, or wake success.

## Android sources

Preferred sensor sources:

- linear acceleration: `TYPE_LINEAR_ACCELERATION`
- orientation: `TYPE_ROTATION_VECTOR`

Fallbacks when preferred sensors are unavailable:

- acceleration: `TYPE_ACCELEROMETER`, reduced to a conservative gravity-adjusted magnitude estimate
- orientation: `TYPE_GRAVITY`

The fallback accelerometer estimate is intentionally simple and less expressive than a dedicated linear-acceleration sensor. It is a degraded evidence source, not a reason to make stronger wake claims.

Continuous motion observation is only valid while explicitly active. The eventual production integration belongs under the existing foreground Active Wake Execution lifecycle; M4 does not create another long-lived service.

## Evidence definitions

### Device pickup

A pickup is emitted only when a sufficiently strong acceleration event and a meaningful tilt change occur within a bounded pairing window.

This reduces false pickup evidence from isolated vibration/translation or a tilt-only screen reposition.

### Orientation change

A meaningful device tilt relative to the current orientation anchor emits `ORIENTATION_CHANGE` after thresholding and cooldown.

### Sustained movement

Sustained movement requires multiple above-threshold acceleration samples spanning a minimum active duration inside a bounded rolling window.

One spike cannot satisfy sustained movement.

## Initial tuning values

The current thresholds are **engineering hypotheses for Wake Lab/device calibration**, not validated human-wake constants:

```text
pickup acceleration        1.6 m/s²
pickup tilt delta           12°
pickup pair window          1200 ms
pickup cooldown             3 s
orientation delta           25°
orientation cooldown        2 s
sustained acceleration      0.75 m/s²
sustained window            1600 ms
minimum active span         700 ms
minimum active samples      4
sustained cooldown          4 s
```

Do not market or document these as scientifically proven thresholds. They should be tuned against real device traces and false-positive/false-negative behavior during M2/M4 dogfood.

## Privacy and storage boundary

Raw accelerometer, gravity, rotation-vector, or gyroscope streams must not be persisted.

The extractor retains only bounded ephemeral state needed for current feature extraction:

- recent qualifying movement timestamps within the rolling window
- one orientation anchor
- one recent high-acceleration timestamp
- one pending tilt-change timestamp/delta
- last evidence-emission timestamps for cooldown

`reset()` clears this state when observation stops.

Allowed diagnostics may contain only derived operational facts such as:

- evidence kind
- monotonic evidence timestamp
- derived reason (`tilt_delta_degrees=...`, sample count/span)
- sensor source availability

No raw sample arrays belong in reliability journals, analytics, cloud payloads, or Wake Session persistence.

## Lifecycle

`AndroidMotionObserver.start()` is idempotent and registers only available sensor sources.

`stop()` unregisters the listener, resets extraction state, and is idempotent.

Future Wake Runtime integration must map:

```text
WakeDirective.ObserveMotion
        ↓
observer.start()

WakeDirective.StopObservingMotion
        ↓
observer.stop()
```

Service/Activity destruction must always stop observation unless ownership has explicitly moved to the Active Wake Execution foreground owner.

## Runtime authority

Motion evidence is just a typed input.

The default M3 Wake Policy currently weights:

```text
DEVICE_PICKUP         1
ORIENTATION_CHANGE    1
SUSTAINED_MOVEMENT    2
```

A test proves the same policy remains `ACTIVATING` with no motion but can reach `ORIENTING` after the extractor produces pickup + orientation + sustained movement.

This is behavior evidence, not proof that the user did not return to bed. `Confirmed Wake Success` remains a later calibrated product metric.

## Current deterministic tests

- pickup requires acceleration + meaningful tilt close in time
- orientation change can be emitted independently
- small orientation jitter does not emit evidence
- sustained movement requires multiple samples spanning time
- old samples are pruned from the bounded window
- cooldown prevents repeated pickup floods from one physical action
- reset removes anchors and recent temporal state
- same Wake Policy differs for no-motion vs sustained-motion timelines

## Deferred from this slice

- production wiring into `AlarmPlaybackService` / Active Wake Execution
- session persistence for runtime inputs
- Wake Lab live motion visualization
- stationary-period policy input
- physical calibration across founder/OEM devices
- gyroscope fusion if real traces prove it necessary
- activity-recognition / step-counter permission
- any machine-learned motion classifier

Those additions require measured evidence rather than speculative complexity.

# M7 Wake Learning v0 implementation

## Status

Wake Learning v0 is now product-integrated locally.

The pure-Kotlin learner was originally implemented as an isolated M7 core. The Android product path now also:

- records bounded semantic Wake behavior into credential-protected Wake history;
- attaches sparse outcome calibration without retaining raw speech/audio;
- derives and persists one bounded learned policy from comparable completed sessions;
- resolves that policy exactly once before a future interactive Wake Session starts;
- keeps the selected policy immutable for that active session;
- fails closed to the stable default when learned state is missing, corrupt or unsupported;
- exposes current policy/evidence in Insights and founder/debug inspection tooling;
- supports a non-destructive learned-policy reset that preserves source Wake history.

Alarm Kernel and WakeRuntime authority boundaries remain unchanged.

## Responsibility

Wake Learning v0 owns one off-session transformation:

```text
completed typed Wake timelines
          +
optional calibration / friction feedback
          ↓
   WakeOutcomeSummary
          ↓
      WakeLearning
          ↓
versioned WakePolicySnapshot
          ↓
 future Wake Sessions only
```

Wake Runtime remains the only in-session behavioral authority. Alarm Kernel reliability, alarm delivery, Stop/Snooze durability, safety/privacy rules and critical audio are outside the learning surface.

## Outcome derivation

`WakeOutcomeDeriver` replays timestamped semantic `WakeInput`s through the real deterministic `WakeRuntime` and derives a compact summary rather than trusting a parallel analytics interpretation.

Current summary facts are:

- whether Activation Completion occurred;
- whether it occurred inside the supplied activation window;
- time to first meaningful engagement;
- time to first sustained movement;
- time to the actual transition into `ORIENTING`;
- successful snooze count;
- maximum intervention/escalation depth;
- terminal runtime reason;
- optional calibration;
- optional annoyance/agency feedback.

The timeline uses monotonic elapsed durations from the alarm start and must be ordered. Outcome derivation requires the full Wake Session from `ALERTING` through a terminal `FINISHED` state. It refuses to invent an outcome for an unfinished session.

Activation Completion is detected at the transition into `ORIENTING`, not merely by observing a later snapshot that remains in `ORIENTING`. This prevents delayed callbacks from corrupting the activation timestamp.

## Calibration semantics

V0 supports the semantic calibration outcomes already defined in the product/data model:

```text
GOT_UP            → Confirmed Wake Success = true
RETURNED_TO_BED   → Confirmed Wake Success = false
GOT_UP_LATER      → Confirmed Wake Success = false
SKIPPED           → unknown
missing feedback  → unknown
```

Missing calibration never becomes implicit success. This keeps Activation Completion and Confirmed Wake Success separate and prevents the learner from optimizing a metric that the runtime can make easier for itself.

## First learnable surface

V0 deliberately learns only parameters that already affect `WakeRuntime` behavior today:

```text
activationThreshold
maxEscalationLevel
```

Timing parameters, snooze behavior, Tomorrow Contract timing, evidence weights and alarm behavior are not learnable in this slice.

This is intentional. Adding a field to a learned profile before a real execution path consumes it would create adaptation that exists only on paper. The learning surface expands only after dogfood evidence and implementation pressure justify it.

## Deterministic update rules

The default v0 rule set is a versioned engineering hypothesis, not product truth:

- require at least 4 outcomes from the current immutable policy version before any change;
- require at least 3 calibrated Activation Completions before treating return-to-bed as a repeated false-positive pattern;
- require at least 3 sessions for another repeated pattern;
- change at most one parameter per derivation;
- change that parameter by exactly one step;
- keep `activationThreshold` inside `3..8`;
- keep `maxEscalationLevel` inside `1..4`.

Rule priority is intentionally small and explicit rather than a generic rules engine:

1. If calibrated Activation Completion repeatedly ends in `RETURNED_TO_BED`, increase `activationThreshold` by one.
2. Otherwise, if activation repeatedly does not complete inside the target window, increase `maxEscalationLevel` by one.
3. Otherwise, if Confirmed Wake Success repeatedly coexists with high annoyance or low agency, decrease `maxEscalationLevel` by one.
4. Otherwise keep the current policy unchanged.

The numeric sample thresholds and bounds remain dogfood hypotheses. They are concentrated in `WakeLearning.Rules` so later evidence can change them without scattering policy constants.

## Friction and agency guardrail

A stricter policy is blocked when enough recent rated sessions show a majority pattern of either:

- high annoyance; or
- low perceived agency.

This prevents learning from converging on "always harder" simply because more force can improve an operational activation metric.

The inverse is also implemented: if Confirmed Wake Success is already present while friction is repeatedly excessive, the learner can reduce maximum intervention depth by one bounded step.

## Versioning and snapshot integrity

Every `WakePolicySnapshot` contains:

- learning algorithm version;
- complete source `WakePolicy`;
- resulting `WakePolicy`;
- deterministically ordered source Wake Session IDs;
- at most one declared change with a human-readable explanation.

The snapshot validates that the resulting policy is exactly the source policy plus its one declared change. An undeclared second parameter change is rejected by construction.

A changed policy advances the source policy version exactly once. An unchanged decision preserves the exact source policy and version.

This supports the existing invariant that every Wake Session snapshots one immutable policy version. Learning never mutates a policy being used by an active session.

## Safe loading, fallback and reset

`resolveLearnedPolicy()` fails closed to the supplied stable default when a candidate is:

- missing;
- from an unsupported learning algorithm;
- based on an older incompatible baseline;
- outside v0 learning bounds;
- different from the default in non-learnable fields;
- encoded with a change larger than one step.

The full source policy in the snapshot makes it possible to verify that evidence weights, snooze duration and duplicate-input memory were not silently changed by Wake Learning.

`resetToDefault()` is explicit and returns the stable default policy without deleting source Wake Outcomes.

## Determinism

Before deriving a policy, relevant outcomes are grouped by Wake Session ID and sorted by that stable ID. Duplicate identical outcomes collapse safely; conflicting duplicate outcomes are rejected instead of allowing list order to decide which morning wins.

Given the same current policy and semantic outcomes, input ordering therefore does not change the learned result.

## Test coverage in this slice

Pure-Kotlin tests cover:

- real `WakeRuntime` replay → compact outcome derivation;
- first engagement, sustained movement and true activation-transition timing;
- delayed callback after `ORIENTING` not moving the activation timestamp;
- unfinished and out-of-order timeline rejection;
- missing calibration remains unknown;
- Activation Completion + repeated `RETURNED_TO_BED` tightens the activation criterion;
- repeated incomplete activation raises bounded escalation;
- annoyance/agency blocking stricter behavior;
- Confirmed Wake Success with excess friction reducing escalation;
- deterministic result regardless of outcome input order;
- conflicting duplicate outcome rejection;
- valid learned-policy resolution;
- unsupported/invalid candidate fallback;
- undeclared extra policy-change rejection;
- explicit reset to the stable default.

## Deliberately deferred

The 1.0 product intentionally does not expand Wake Learning into:

- timing/snooze/Tomorrow Contract adaptation;
- opaque ML/AI optimization;
- cloud/account-dependent learning;
- raw semantic input, microphone/audio or high-frequency motion retention;
- mid-session policy mutation.

The earlier plan for a persistent full timed WakeInput journal was superseded by a more privacy-minimized implementation: the product persists only the compact behavioral evidence needed by Insights and deterministic learning.

Physical beta/dogfood evidence still needs to validate the current v0 thresholds and safe ranges before broader tuning is justified.

## M7 completion boundary

M7 engineering integration is complete when the device-backed persistence/corruption/reset contract is green. Product tuning remains evidence work under the 1.0 TRUST / PROVE gates, not unfinished M7 feature scope.

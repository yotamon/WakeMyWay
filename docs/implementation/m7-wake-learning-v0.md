# M7 — Wake Learning v0

**Status:** implementation candidate in PR #22  
**Issue:** #21  
**Branch:** `m7/wake-learning-v0`

## Purpose

M7 proves Wake My Way's adaptive product thesis **before** realtime voice becomes central.

The milestone answers one narrow question:

> Can prior mornings produce a small, deterministic, explainable change to a future Wake Policy without letting the system game its own success metric or weaken alarm reliability?

The v0 answer is deliberately local and conservative. No cloud, AI, ML model, generic rule engine, transcript, Tomorrow Contract text, calendar content or analytics pipeline participates.

## Architecture

```text
semantic Wake Session facts
(no transcript/private content)
          ↓
   WakeOutcomeDeriver
          ↓
    WakeOutcomeRecord
 activation completion
 + timing
 + intervention depth
 + snooze facts
 + optional calibration
 + annoyance / agency
          ↓
      WakeLearning
 deterministic + bounded
          ↓
  NoChange OR PolicyUpdated
          ↓
  LearnedWakeProfile
 credential-protected local state
          ↓
future Wake Session selects policy snapshot
          ↓
WakeRuntime pins policy version for entire session
```

Wake Learning runs **between** sessions. It cannot mutate an active Wake Session.

## Wake Outcome

`WakeOutcomeRecord` is a compact, privacy-minimized summary derived from typed semantic timeline facts.

Current fields include:

- source `WakeSessionId`;
- policy version used by that session;
- start timestamp;
- time to first engagement;
- time to first sustained/meaningful movement;
- time to Activation Completion;
- terminal runtime outcome;
- snooze count;
- maximum intervention depth;
- optional lightweight calibration;
- optional 1–5 annoyance and agency ratings.

It stores no speech text, transcript, prompt, calendar title, Tomorrow Contract content, microphone audio or raw motion stream.

## Activation Completion is not Confirmed Wake Success

M7 encodes the distinction in the data model rather than relying on documentation alone.

```text
Activation Completion = true
Calibration = RETURNED_TO_BED

→ activationCompleted = true
→ confirmedWakeSuccess = NOT_CONFIRMED
```

Calibration values in v0:

- `GOT_UP` → `CONFIRMED`;
- `RETURNED_TO_BED` → `NOT_CONFIRMED`;
- `GOT_UP_LATER` → `NOT_CONFIRMED` for the intended wake window;
- missing calibration → `UNKNOWN`.

Missing calibration is never silently converted to success.

## Learnable surface

V0 learns exactly one behavioral parameter:

```text
WakePolicy.movementPromptDelay
```

Default: 15 seconds.

The field controls how long the runtime waits before escalating from engagement toward an explicit movement request when activation has not progressed enough.

M7 intentionally does **not** learn:

- `activationThreshold`;
- activation-evidence weights;
- alarm volume/delivery;
- Stop availability;
- Snooze authority;
- privacy settings;
- safety rules;
- exact-alarm behavior;
- Direct-Boot behavior;
- arbitrary character prompts.

This prevents the first learning loop from making its own success criterion easier and then claiming improvement.

## Learning policy

`WakeLearningPolicy` is versioned independently from `WakePolicy`.

Default v0 thresholds are hypotheses for founder dogfood and remain easy to change/version:

- observation window: 6 outcomes from the **current policy version**;
- minimum evidence before any change: 4 relevant outcomes;
- repeated pattern required: 3 outcomes;
- quick engagement: ≤30s;
- slow meaningful movement: missing or ≥75s;
- fast meaningful movement: ≤45s;
- one policy step: 5s;
- safe movement-prompt bounds: 5s–45s.

A new learned policy is evaluated only against outcomes produced by its own version. Old-policy history cannot immediately trigger a second change. This creates natural hysteresis and keeps experiments interpretable.

## Earlier movement prompt rule

If repeated mornings show:

- quick engagement;
- slow or missing meaningful movement;
- acceptable annoyance/agency feedback;

then v0 may move the movement prompt 5 seconds earlier.

Example:

```text
Policy v1
movementPromptDelay = 15s

4 recent v1 wakes
3+ engaged quickly
3+ moved only after ≥75s / not at all
annoyance + agency guardrails acceptable

→ Policy v2
movementPromptDelay = 10s
```

## Friction-reduction rule

If repeated mornings show fast movement but users also repeatedly report high annoyance or low agency, v0 may move the prompt 5 seconds later.

A wake calibrated as `RETURNED_TO_BED` or `GOT_UP_LATER` is **not** counted as positive evidence for this relaxation rule, even if Activation Completion occurred.

## Guardrails

An earlier prompt is blocked when repeated high-annoyance or low-agency feedback indicates that adding friction is unsafe or undesirable.

V0 never changes policy from one anomalous morning.

Every decision returns one of:

- `PolicyUpdated` with previous policy, next policy, exact parameter change, source outcome IDs and explanation;
- `NoChange` with a typed reason and explanation.

No hidden score or opaque optimizer exists.

## Policy version immutability

The existing M3 invariant remains authoritative:

```text
WakeRuntime.initial(... policy vN)
        ↓
WakeSessionSnapshot.policyVersion = N
        ↓
all reduce() calls require policy vN
```

If Wake Learning derives policy vN+1 while a session exists, that session cannot consume the new policy. `WakeRuntime` rejects a mismatched policy version.

The learned policy is only eligible for a future session.

## Android persistence

`PrivateWakeLearningStore` uses one atomic file:

```text
credential-protected Context
        ↓
noBackupFilesDir/wake-learning/
        ↓
wake-learning-v1.bin
```

The file contains:

- bounded Wake Outcome history, maximum 30;
- optional versioned `LearnedWakeProfile`;
- complete future `WakePolicy` snapshot;
- source outcome IDs;
- human-readable explanation.

Properties:

- credential-protected only;
- explicitly rejects device-protected Contexts;
- excluded from Android Auto Backup through `noBackupFilesDir`;
- single `AtomicFile` prevents profile/history split-brain writes;
- bounded list decoding fails closed;
- unsupported/corrupt derived state never blocks wake delivery.

## Fallback and reset

`WakeLearningManager.activePolicy()` always has a safe answer.

```text
valid learned profile
    → learned future policy

no learned profile
    → stable default policy

corrupt store
unsupported profile/algorithm
    → stable default policy
```

Reset deletes only derived Wake Learning state and restores the stable default policy. It does not delete the Wake Schedule, Alarm Kernel state, Tomorrow Contract or reliability history.

## Delayed calibration

Calibration can arrive after the Wake Outcome was first stored.

`WakeLearningManager.applyCalibration(...)` replaces the matching outcome by ID while preserving all existing semantic timing facts. The learning decision is then re-evaluated deterministically.

This supports occasional low-friction feedback instead of requiring a question every morning.

## Founder Wake Learning Lab

Wake Alarm Lab contains a dedicated Wake Learning v0 section showing:

- active future policy version;
- current movement-prompt delay;
- explicit statement that activation threshold is not learnable;
- storage/learning status;
- latest learned explanation;
- bounded recent outcome history;
- Activation Completion vs confirmed-success state.

Founder fixtures include:

- repeated slow-movement morning;
- smooth but annoying morning;
- `Activation Complete → returned to bed` false-positive calibration;
- full Wake Learning reset.

These fixtures prove behavior and make the policy loop inspectable. They are not a substitute for real overnight dogfood evidence.

## Tests

Pure Kotlin coverage verifies:

- Activation Completion + `RETURNED_TO_BED` remain contradictory facts rather than fake success;
- one unusual morning cannot change policy;
- repeated quick-engagement/slow-movement outcomes produce an earlier bounded movement prompt;
- identical evidence produces identical decisions regardless of input order;
- annoyance/agency can block added friction;
- fast movement + repeated friction can move the prompt later;
- return-to-bed outcomes cannot justify relaxing friction;
- new policy versions wait for evidence gathered under that same version;
- a learned policy cannot mutate an already-started Wake Session.

Android instrumentation covers:

- learned policy persistence across manager/store recreation;
- reset to default policy;
- corrupt-state fallback;
- device-protected storage rejection;
- delayed calibration preserving Activation Completion vs Confirmed Wake Success disagreement.

## Reliability boundary

M7 does not modify:

- `AlarmManager` registration;
- `AlarmReceiver`;
- `AlarmPlaybackService`;
- Critical Wake Snapshot;
- device-protected reliability journal;
- Stop/Snooze durability;
- WorkManager preparation behavior.

Wake Learning state is not needed to make sound, show alarm controls, recover an active wake or reschedule after reboot.

## Intentionally deferred

M7 does not claim that the initial thresholds are calibrated. They are founder-dogfood hypotheses.

Also deferred:

- ML;
- contextual/multi-dimensional policies;
- cloud sync;
- account-level profile sync;
- realtime voice adaptation;
- automatic production Wake Outcome capture from richer runtime/motion signals until those adapters are production-wired and physically validated;
- any physical-device Wake Success claim.

M8 remains the realtime voice architecture spike only after M7's deterministic local loop is merged and inspectable.

## Exit evidence required before merge

- docs validation green;
- all `:wake-core` tests green;
- Android lint/compile green;
- instrumentation APK compilation green;
- API-36 emulator instrumentation green;
- PR review finds no path from learning state into alarm-delivery authority;
- physical-device/dogfood validation remains evidence to gather after merge and is not fabricated by emulator success.

# Data model

Canonical domain terms are defined in [`../CONTEXT.md`](../CONTEXT.md).

## Design goals

- one active adaptive Wake Schedule in V1
- local authority for wake-critical state
- Alarm Kernel owns critical scheduling **and Active Wake Execution** consistency
- typed session history sufficient for replay/learning
- Activation Completion remains distinguishable from Confirmed Wake Success calibration
- privacy by minimizing raw text/audio/sensor retention
- derived profiles/policies can be rebuilt from source outcomes
- persistence representation does not become the domain interface

# Local model

## WakeSchedule

Reusable V1 wake intent.

Representative fields:

```text
id
weekly/local-time rule
followCurrentTimezone
isEnabled
characterId
createdAt
updatedAt
revision
```

V1 allows one active adaptive schedule. Do not design a generic many-alarm coordinator until product evidence requires it.

An optional dogfood Safety Backup, if implemented, is a separate trust-transition mechanism and must not distort the WakeSchedule domain into arbitrary multi-alarm coordination.

## WakeOccurrence

Concrete next execution.

```text
id
wakeScheduleId
kind = PRIMARY | SNOOZE
scheduledLocalDateTime
scheduledInstant
zoneId
scheduleRevision
status
createdAt
parentOccurrenceId?   // snooze lineage
```

Possible lifecycle statuses are persistence concerns and should match actual Alarm Kernel behavior rather than being invented prematurely.

The implemented status model must still be able to represent the invariants required by ADR-014, including the distinction between scheduled, currently active, intentionally stopped, durably replaced/snoozed, and completed/terminated occurrences.

## Active Wake Execution state

Do not create a public domain object merely because Android uses a foreground service. Persist only the minimum implementation state the Alarm Kernel proves necessary for idempotent active-alarm recovery.

Conceptually useful facts may include:

```text
activeWakeOccurrenceId?
activeExecutionRevision / generation
playbackStartedAtElapsed?
terminalReason?
replacementOccurrenceId?
```

The exact schema belongs to M1 implementation.

Hard requirements:

- one occurrence cannot recover into duplicate critical playback owners
- Stop must remain terminal after component recreation
- successful snooze replacement makes the old occurrence non-authoritative
- stale execution identity cannot attach to a newer occurrence
- private conversation/context is not required to recover critical playback

## TomorrowContract

```text
id
wakeOccurrenceId
rawText
firstMove?
createdAt
```

A minimized/structured summary may exist only if needed for preparation. Raw text is sensitive and local by default.

## PreparedWakePlan

Non-critical richer local content prepared for the next occurrence.

Representative data:

```text
id
wakeOccurrenceId
formatVersion
safe opening intent/text
local prepared audio references
fallback lines
minimized context facts
characterVersion
preparedAt
checksum
```

Do not put Wake Runtime policy/state authority inside PreparedWakePlan. It is presentation/context enrichment, not the behavioral controller.

## WakeSession

```text
id
wakeOccurrenceId
runtimeVersion
policyVersion
startedAtWall
startedAtElapsed
finishedAtWall?
finishReason?
```

Every session snapshots one immutable Wake Policy version for replay/experiments.

Derived timing such as engagement/movement can come from typed timeline facts rather than duplicated nullable columns unless query performance later justifies denormalized summary fields.

## Typed wake timeline

Do not build a domain model around a generic event table like:

```text
kind + numericValue? + textValue? + metadataJson?
```

That representation invites invalid combinations and makes event contracts impossible to reason about.

Domain code uses typed Wake Inputs/directives/observations. Persistence may serialize them into a versioned envelope, for example conceptually:

```text
id
sessionId
sequence
recordKind
schemaVersion
wallTimestamp
elapsedMs
payload
```

`payload` is validated/decoded according to `recordKind + schemaVersion`; it is not an arbitrary analytics bag.

Persist only records needed for replay, reliability diagnosis, and safe learning.

## WakeOutcome

Derived compact summary, for example:

```text
activationCompleted
metActivationWindow
secondsToFirstEngagement
secondsToMeaningfulMovement
secondsToActivationCompletion
snoozeCount
maxInterventionDepth
fallbackLevel
finishReason
```

The exact activation criterion is versioned with the policy/experiment so historical outcomes remain interpretable.

Do not name `activationCompleted` as `wakeSuccess` in persistence. It is an operational phone-observable outcome, not automatically confirmed real-world success.

## WakeCalibration

Optional later feedback used to calibrate whether Activation Completion corresponded to the real-world wake result.

Conceptual semantic shape:

```text
sessionId
outcome = GOT_UP | RETURNED_TO_BED | GOT_UP_LATER | SKIPPED
createdAt
source = USER_FEEDBACK | future_privacy_safe_proxy
```

Rules:

- calibration is optional and sparse
- missing calibration is `unknown`, not success
- do not require it during the alarm session
- preserve source/provenance so a future proxy is not treated as equivalent to direct user feedback without evaluation

## WakeFeedback

Feedback about friction/style is distinct from outcome calibration.

```text
sessionId
direction = TOO_MUCH | GOOD | TOO_GENTLE | ...
notes?        // sensitive, local unless explicitly uploaded
createdAt
```

This separation prevents "the wake felt good" from being treated as proof the user actually got up.

## WakeProfileSnapshot / WakePolicySnapshot

Derived, versioned data. Not source of truth.

```text
id
algorithmVersion
policyVersion
generatedAt
sourceSessionRange/count
safe derived parameters
safe debug explanation metadata
```

For M7 Wake Learning v0, a policy snapshot must be:

- locally derivable
- bounded/validated
- immutable once used by a Wake Session
- resettable to the stable default
- recoverable to default if corrupt/unsupported

Do not persist raw sensitive morning content simply because a learner might want it.

# Critical Wake Snapshot

Separate from the rich Room schema and owned by Alarm Kernel.

It is stored in **device-protected storage** and intentionally contains only non-sensitive critical recovery state, conceptually:

```text
formatVersion
wakeOccurrenceId
scheduledInstant
scheduleRevision / trigger identity
safe alarm tone configuration
checksum
```

It must be sufficient to re-register/identify/start a generic safe alarm before first unlock after reboot.

If M1 proves a tiny additional non-sensitive fact is necessary to prevent active-execution duplication during Direct Boot, document and threat-model it explicitly before extending this snapshot.

It must **not** contain:

- Tomorrow Contract content
- calendar facts
- transcripts
- private generated contextual speech
- auth tokens
- learned explanations containing private source facts

# Credential-protected storage

Normal Room/DataStore files remain credential-protected unless a very small item is explicitly proven necessary during Direct Boot.

Prepared personalized audio derived from private context should remain credential-protected; a pre-unlock wake falls back to generic bundled/brand audio.

Wake Outcomes, calibration, feedback and learned policy snapshots are normal credential-protected local data.

# Preferences

Use DataStore for small app/user configuration when needed, for example:

```text
onboardingComplete
language
analyticsConsent
diagnosticsEnabled
lastSelectedWakeTime
safetyBackupEnabled?   // only if the dogfood feature exists
```

Avoid duplicating authoritative Wake Schedule/session/active-execution state in DataStore.

# Cloud model

Cloud storage is introduced only with a feature that needs it. Possible later concepts:

```text
installations
users (optional/later)
wake_sessions / safe outcome summaries
wake_calibration
wake_feedback
wake_profiles
character_preferences
subscriptions
```

The server never owns Android exact alarm scheduling, Active Wake Execution, or local M7 policy derivation.

# Sync outbox

Add a durable outbox only when backend sync exists. It is not an M0–M7 dependency.

When introduced, uploads must be retry-safe and must never block the Wake Session or policy fallback.

# Raw sensor policy

Do not persist high-frequency accelerometer/gyro streams by default.

Compute local features such as:

- pickup
- acceleration/movement intensity
- orientation change
- sustained motion duration
- stationary duration

Then feed typed observations to Wake Runtime and retain only the minimal safe timeline facts needed for replay/learning.

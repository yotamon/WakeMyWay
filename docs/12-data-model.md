# Data model

Canonical domain terms are defined in [`../CONTEXT.md`](../CONTEXT.md).

## Design goals

- one active Wake Schedule in V1
- local authority for wake-critical state
- Alarm Kernel owns critical scheduling consistency
- typed session history sufficient for replay/learning
- privacy by minimizing raw text/audio/sensor retention
- derived profiles can be rebuilt from source outcomes
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

V1 allows one active schedule. Do not design a generic many-alarm coordinator until product evidence requires it.

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
metWakeWindow
secondsToFirstEngagement
secondsToMeaningfulMovement
snoozeCount
maxInterventionDepth
fallbackLevel
finishReason
```

The exact activation criterion is versioned with the policy/experiment so historical outcomes remain interpretable.

## WakeFeedback

```text
sessionId
rating/direction?
notes?        // sensitive, local unless explicitly uploaded
createdAt
```

## WakeProfileSnapshot / WakePolicySnapshot

Derived, versioned data. Not source of truth.

```text
id
algorithmVersion
generatedAt
sourceSessionRange/count
safe derived parameters/insights
```

Do not persist raw sensitive morning content simply because a future learner might want it.

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

It must **not** contain:

- Tomorrow Contract content
- calendar facts
- transcripts
- private generated contextual speech
- auth tokens

# Credential-protected storage

Normal Room/DataStore files remain credential-protected unless a very small item is explicitly proven necessary during Direct Boot.

Prepared personalized audio derived from private context should remain credential-protected; a pre-unlock wake falls back to generic bundled/brand audio.

# Preferences

Use DataStore for small app/user configuration when needed, for example:

```text
onboardingComplete
language
analyticsConsent
diagnosticsEnabled
lastSelectedWakeTime
```

Avoid duplicating authoritative Wake Schedule/session state in DataStore.

# Cloud model

Cloud storage is introduced only with a feature that needs it. Possible later concepts:

```text
installations
users (optional/later)
wake_sessions / safe outcome summaries
wake_feedback
wake_profiles
character_preferences
subscriptions
```

The server never owns Android exact alarm scheduling.

# Sync outbox

Add a durable outbox only when backend sync exists. It is not an M0 dependency.

When introduced, uploads must be retry-safe and must never block the Wake Session.

# Raw sensor policy

Do not persist high-frequency accelerometer/gyro streams by default.

Compute local features such as:

- pickup
- acceleration/movement intensity
- orientation change
- sustained motion duration
- stationary duration

Then feed typed observations to Wake Runtime and retain only the minimal safe timeline facts needed for replay/learning.

# Observability and analytics

## Principle

Measure whether WMW wakes people **reliably and respectfully**. Do not collect private morning content for analytics convenience.

Observability enters when it helps diagnose dogfood/reliability. It is not part of the first-audible or Active Wake Execution critical path.

## Critical reliability events

```text
wake_schedule_committed
wake_occurrence_registered
wake_ready_changed
alarm_reconciled
alarm_trigger_received
active_wake_execution_started
alarm_audio_started
wake_surface_available
wake_surface_recreated
active_playback_recovered
active_playback_duplicate_suppressed
active_wake_stopped
active_wake_snooze_replaced
critical_snapshot_fallback_used
direct_boot_wake_started
prepared_plan_used
fallback_level_changed
```

These events must remain semantic. Do not encode private content into event properties.

## Behavioral semantic events

```text
user_engaged
snooze_requested
snooze_confirmed
meaningful_movement_detected
activation_completion_reached
first_move_selected
session_finished
wake_calibration_submitted
```

Possible calibration values are semantic only, for example:

```text
GOT_UP
RETURNED_TO_BED
GOT_UP_LATER
SKIPPED
```

These are behavioral facts, not medical claims about consciousness.

## Wake Learning events

Once M7 exists:

```text
wake_policy_derived
wake_policy_changed
wake_policy_unchanged
wake_policy_reset
wake_policy_fallback_to_default
```

Safe properties may include:

```text
fromPolicyVersion
toPolicyVersion
algorithmVersion
changedParameterNames
sourceOutcomeCount
```

Do not send private source text, transcripts, calendar facts, or raw rule input.

## Voice events

```text
voice_connect_started
voice_connected
voice_first_response
voice_interrupted
voice_reconnect
voice_failed
voice_fallback
```

## Capability/system events

```text
exact_alarm_unavailable
full_screen_unavailable
notification_permission_missing
microphone_denied
calendar_denied
locked_boot_reconciled
boot_reconciled
post_force_stop_reconciled
clock_change_reconciled
timezone_reconciled
```

## Core latency metrics

### Trigger delay

```text
triggerReceivedAt - scheduledInstant
```

### Active execution start latency

```text
activeExecutionStartedAt - triggerReceivedAt
```

### Audio start latency

```text
audioStartedAt - triggerReceivedAt
```

### Usable presentation latency

```text
wakeControlsAvailableAt - triggerReceivedAt
```

### Active recovery latency

```text
recoveredActionableAt - recoveryStartAt
```

Use when testing Activity/playback-owner/process recreation.

### Time to engagement

```text
firstMeaningfulEngagement - alarmTriggered
```

### Time to movement

```text
firstMeaningfulMovement - alarmTriggered
```

### Time to Activation Completion

```text
activationCompletionAt - alarmTriggered
```

## Product outcomes

### Activation Completion

Percentage of intended Wake Occurrences where the configured runtime activation criterion is reached inside the target wake window.

This is an operational behavioral metric. It is not sufficient by itself to claim the user achieved the intended real-world wake outcome.

### Confirmed Wake Success

Calibrated share of wakes where available evidence indicates the user actually got up rather than satisfying activation evidence and then returning to bed.

Because feedback is intentionally sparse, report denominator/coverage alongside this metric. Missing calibration must not silently become success.

### Wake Success

In product-level reporting, Wake Success refers to the real-world product goal and should use Confirmed Wake Success where calibrated evidence exists. Keep Activation Completion available separately for runtime/engineering analysis.

Supporting metrics:

- alarm delivery success
- Active Wake Execution survival/recovery rate
- snoozes per wake
- confirmed wake-after-snooze success
- intervention/escalation depth
- engagement/movement/activation latency
- fallback rate
- Activation Completion → returned-to-bed rate
- annoyance rating
- perceived agency
- Safety Backup usage/removal during early dogfood
- D7/D30 retention

## Wake Friction

Track intervention cost together with effectiveness. A strategy that is maximally aggressive but causes mistrust/uninstall is not optimal.

A useful Wake Learning evaluation is not a single score but a tuple:

```text
Confirmed Wake Success
activation delay
intervention depth
annoyance
perceived agency
```

Do not expose an opaque "Wake Score" to users unless it becomes demonstrably useful.

## Reliability diagnostics

For active alarm lifecycle failures, keep enough **non-sensitive local diagnostic timeline** to answer:

- which occurrence fired?
- did Active Wake Execution start?
- when did safe audio begin?
- did UI/playback/process recreation occur?
- was a duplicate start suppressed?
- did the user stop or durably snooze?
- did any stale occurrence try to resurrect?

This diagnostic timeline may be richer locally than uploaded analytics. Upload only sanitized semantic facts with user-consent/legal controls as appropriate.

## Tooling direction

### Sentry

Potential use:

- crashes
- ANRs
- selected wake-path performance traces
- active playback/service lifecycle failures once dogfood justifies it

Session replay is off initially. Sanitize breadcrumbs/exceptions.

### PostHog

Manual semantic events only. No broad autocapture/session replay by default.

## Never send to telemetry

- raw microphone audio
- full transcripts
- Tomorrow Contract raw text
- calendar titles/descriptions/locations
- full generated prompts/model context
- secrets/tokens
- raw high-frequency motion streams
- personalized Wake Learning explanations containing private source facts

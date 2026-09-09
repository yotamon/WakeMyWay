# Observability and analytics

## Principle

Measure whether WMW wakes people **reliably and respectfully**. Do not collect private morning content for analytics convenience.

Observability enters when it helps diagnose dogfood/reliability. It is not part of the first-audible Alarm Kernel critical path.

## Critical reliability events

```text
wake_schedule_committed
wake_occurrence_registered
wake_ready_changed
alarm_reconciled
alarm_trigger_received
wake_surface_available
alarm_audio_started
critical_snapshot_fallback_used
direct_boot_wake_started
prepared_plan_used
fallback_level_changed
```

## Behavioral semantic events

```text
user_engaged
snooze_requested
snooze_confirmed
meaningful_movement_detected
activation_threshold_reached
first_move_selected
session_finished
wake_feedback_submitted
```

These are behavioral facts, not medical claims about consciousness.

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

### Audio start latency

```text
audioStartedAt - triggerReceivedAt
```

### Usable presentation latency

```text
wakeControlsAvailableAt - triggerReceivedAt
```

### Time to engagement

```text
firstMeaningfulEngagement - alarmTriggered
```

### Time to movement

```text
firstMeaningfulMovement - alarmTriggered
```

### Time to activation criterion

```text
activationThresholdReachedAt - alarmTriggered
```

## Product outcome

### Wake Success

Percentage of intended Wake Occurrences where the configured behavioral activation criterion is reached inside the target wake window.

Wake Success is a product proxy. It must never be described as proof that the person is biologically awake or physically out of bed.

Supporting metrics:

- snoozes per wake
- wake-after-snooze success
- intervention/escalation depth
- engagement/movement/activation latency
- fallback rate
- premature-success/return-to-bed proxy when a privacy-safe proxy exists
- annoyance rating
- perceived agency
- D7/D30 retention

## Wake Friction

Track intervention cost together with effectiveness. A strategy that is maximally aggressive but causes mistrust/uninstall is not optimal.

Do not expose an opaque "Wake Score" to users unless it becomes demonstrably useful.

## Tooling direction

### Sentry

Potential use:

- crashes
- ANRs
- selected wake-path performance traces

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

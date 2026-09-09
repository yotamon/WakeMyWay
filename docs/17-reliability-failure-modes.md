# Reliability and failure modes

## Reliability promise and envelope

Wake My Way is designed so rich intelligence can fail without turning the scheduled wake into silence.

Reliability has two trust-critical stages:

```text
1. scheduled delivery
   Wake Occurrence actually fires

2. active wake execution
   safe alarm audio + controls remain actionable until intentional termination
```

It should remain functional across normal process death/recreation, Activity recreation, Doze/idle, network outage, backend/provider failure, and failure of non-critical personalized content.

It cannot truthfully guarantee alarm delivery/execution when Android/hardware intentionally makes it impossible, including:

- user explicitly Force Stops the app (modern Android stopped-state behavior)
- app uninstall/disable
- device powered off at wake time
- required exact-alarm/system capabilities revoked or prevented
- catastrophic OS/device failure

The product must make the recoverable parts of this envelope visible through Wake Ready rather than over-promising.

## Graceful richness degradation

```text
LEVEL 0  Full realtime conversational wake
   |
LEVEL 1  Prepared/local personalized speech
   |
LEVEL 2  Prepared scripted character plan
   |
LEVEL 3  Deterministic local Wake Runtime
   |
LEVEL 4  Standard branded alarm + basic interaction
   |
LEVEL 5  Bundled emergency alarm
```

Fallback level is not a Wake Phase. Every reachable level continues the wake attempt.

## Failure matrix

| Failure | Expected behavior |
|---|---|
| No internet | local plan/runtime |
| Backend down | local plan/runtime |
| Realtime voice unavailable/slow | prepared/scripted speech; no spinner on critical path |
| Weather unavailable | omit weather |
| Calendar unavailable/denied | omit calendar |
| Prepared personalized content unavailable | deterministic local script |
| All prepared audio unavailable | bundled emergency alarm |
| Normal database initialization fails at trigger | device-protected Critical Wake Snapshot + generic safe alarm path |
| Device rebooted and not yet unlocked | Direct-Boot reschedule + generic bundled/brand alarm; no sensitive personalized context |
| `WakeActivity` destroyed/recreated while sounding | critical playback continues; new UI attaches to active occurrence |
| Playback service/controller recreated | recover one active execution; do not duplicate overlapping alarm audio |
| Process recreated during active alarm | recover safe active occurrence state where Android permits; never depend on old Activity instance |
| Stop completed then component recreates | alarm stays stopped; stale trigger cannot resurrect it |
| Snooze replacement completed then component recreates | old occurrence stays terminated; new exact Snooze Occurrence remains authoritative |
| Mic unavailable | touch + motion behavior |
| Motion sensor unavailable | voice/touch behavior |
| Full-screen intent unavailable | audible alarm + allowed high-priority notification/presentation fallback |
| Notification permission denied | behavior must be explicitly tested; Alarm Health must not assume normal notification presentation |
| Backend auth/subscription broken | base alarm unaffected |
| Analytics/crash reporting broken | no user-facing effect |
| User Force Stops app | scheduled/active delivery cannot be guaranteed; repair/reconcile on next user start and show not-ready until restored |

## Active Wake Execution invariants

Once a Wake Occurrence fires, the Alarm Kernel owns a critical execution that outlives ordinary UI component lifetime.

Required invariants:

- `WakeActivity` lifetime never owns the only critical alarm audio instance
- exactly one active playback owner exists for one active occurrence
- duplicate receiver/service starts converge on the same active occurrence
- stop is idempotent and terminal for that occurrence
- durable snooze replacement terminates the old active occurrence only after the new exact occurrence is safely scheduled
- stale `PendingIntent`/component recreation cannot revive an older occurrence
- critical playback remains local and does not depend on Room/network/AI initialization
- resources such as foreground-service state, audio focus, and any proven-needed wake lock are released on terminal paths

See ADR-014.

## Android background-audio compatibility

Android 17 hardens background audio interactions. WMW must test the active-alarm path using alarm-appropriate `USAGE_ALARM` attributes and a valid foreground execution shape.

Do not treat a currently visible Activity as sufficient architecture for the entire wake attempt.

Target-API upgrades must rerun these lifecycle/audio tests before release.

## Critical latency metrics

Targets are hypotheses until measured on supported devices:

- OS trigger → safe audible local alarm
- OS trigger → active playback owner established
- OS trigger → visible wake surface when platform presentation allows
- component/process recovery → safe actionable active alarm restored
- prepared speech start latency
- Wake Runtime input → directive latency

Do not write a false universal 1-second SLO before measuring OEM/device distributions. Establish baseline in M2, then commit to percentile targets.

## Cold-start rule

First safe alarm output must not wait for:

- analytics/Sentry/PostHog initialization
- remote config
- cloud auth
- AI/voice SDK session
- weather/calendar
- full Room graph if critical snapshot recovery is enough
- image/font/network assets

## Direct Boot

Reliability after reboot requires an explicit Direct-Boot design:

- critical receiver can run before first unlock
- critical schedule snapshot lives in device-protected storage
- sensitive personalized/contextual content stays credential-protected
- pre-unlock alarm uses safe generic local audio
- richer state reconciles after unlock

## Duplicate/idempotent behavior

Platform events and retries may occur more than once. Alarm Kernel and Wake Runtime effect handling must be idempotent for destructive operations such as:

- scheduling/cancelling an occurrence
- starting/attaching to active playback
- creating a Wake Session
- stopping an active occurrence
- scheduling snooze
- finishing the session

Duplicate delivery is tested as a reliability property, not treated as an impossible platform bug.

## Full-screen presentation reality

A full-screen activity is not the only valid alarm presentation on modern Android. Locked-device, unlocked-device, permission, notification, and OEM states differ.

Tests must assert the **outcome** (audible alarm + accessible user control) rather than hard-coding that WakeActivity always steals focus.

## Intentional stop

The user must always be able to stop the alarm without coercion or trapping.

The product may make stop more deliberate than a giant reflexive button, but reliability/safety requires that the path be:

- visible/touch-accessible
- usable with TalkBack
- available without microphone/network
- not dependent on AI understanding the user
- idempotent if invoked twice

UX experimentation cannot remove or obscure the existence of a real stop path.

## Safety Backup

During early dogfood/onboarding, a later conventional Safety Backup alarm may be useful while trust is being earned.

This is explicitly a transition mechanism, not evidence that the Alarm Kernel should support multiple adaptive schedules. Track whether users voluntarily stop needing it.

A Safety Backup must not mask WMW reliability failures in diagnostics: record whether WMW itself fired and completed before any backup was used.

## Force Stop recovery

On modern Android versions, Force Stop can cancel pending intents and place the package in a stopped state.

On first post-force-stop launch:

- detect the condition where supported
- reconcile the active Wake Schedule
- clear/repair stale active-execution state
- re-register the next Wake Occurrence
- update Wake Ready only after successful restoration

Do not market delivery through explicit Force Stop as supported.

## Time and reboot recovery

Alarm Kernel must reconcile after:

- locked boot / boot completed
- user unlock when richer state becomes available
- timezone change
- wall-clock change
- schedule edit/cancel
- exact-alarm capability change
- app update/start

Session durations use monotonic elapsed time so a wall-clock adjustment does not distort escalation.

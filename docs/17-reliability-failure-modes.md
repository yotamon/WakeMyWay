# Reliability and failure modes

## Reliability promise and envelope

Wake My Way is designed so rich intelligence can fail without turning the scheduled wake into silence.

It should remain functional across normal process death, Doze/idle, network outage, backend/provider failure, and failure of non-critical personalized content.

It cannot truthfully guarantee alarm delivery when Android/hardware intentionally makes delivery impossible, including:

- user explicitly Force Stops the app (Android 15+ cancels pending intents in stopped state)
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
| Normal database initialization fails | device-protected Critical Wake Snapshot + generic safe alarm path |
| Device rebooted and not yet unlocked | Direct-Boot reschedule + generic bundled/brand alarm; no sensitive personalized context |
| Mic unavailable | touch + motion behavior |
| Motion sensor unavailable | voice/touch behavior |
| Full-screen intent unavailable | audible alarm + allowed high-priority notification/presentation fallback |
| Notification permission denied | behavior must be explicitly tested; Alarm Health must not assume normal notification presentation |
| Backend auth/subscription broken | base alarm unaffected |
| Analytics/crash reporting broken | no user-facing effect |
| User Force Stops app | scheduled delivery cannot be guaranteed; repair/reconcile on next user start and show not-ready until restored |

## Critical latency metrics

Targets are hypotheses until measured on supported devices:

- OS trigger -> safe audible local alarm
- OS trigger -> visible wake surface when platform presentation allows
- prepared speech start latency
- Wake Runtime input -> directive latency

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
- creating a Wake Session
- scheduling snooze
- finishing the session

## Full-screen presentation reality

A full-screen activity is not the only valid alarm presentation on modern Android. Locked-device, unlocked-device, permission, notification, and OEM states differ.

Tests must assert the **outcome** (audible alarm + accessible user control) rather than hard-coding that WakeActivity always steals focus.

## Force Stop recovery

On Android 15+, Force Stop cancels the app's pending intents. Android may notify the app via normal startup/boot-completed semantics when the user later removes it from stopped state.

On first post-force-stop launch:

- detect the condition where supported
- reconcile the active Wake Schedule
- re-register the next Wake Occurrence
- update Wake Ready only after successful restoration

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

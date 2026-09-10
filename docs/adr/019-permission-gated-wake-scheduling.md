# ADR 019 — Voice Wake scheduling is permission-gated before commit

**Status:** Accepted  
**Date:** 2026-09-10  
**Evidence:** second founder physical-device regression after PR #37

## Context

A second physical test reproduced the dangerous audible-only failure: critical alarm audio started, the Wake Surface did not appear, Alfred did not start, and dismissing the application task did not stop playback. The user ultimately had to uninstall the app to terminate the alarm.

The persistence was explainable by the existing alarm reliability design: `AlarmPlaybackService` is independent from the recent-app task and is intentionally restartable. That is correct only when the active wake always has reachable terminal controls.

Android 13+ can run a foreground service even when notification permission is denied; the foreground-service notice may not be visible in the notification drawer. Therefore a design that persists an alarm first and asks the user to repair notification/full-screen access later can still create an uncontrollable alarm.

## Decision

For the current product, scheduling a Voice Wake is a **preflight transaction**.

No Wake Occurrence may be committed from the normal product flow until all of these are true:

```text
exact-alarm capability
        +
notifications enabled
        +
active-wake channel >= HIGH
        +
full-screen alarm access
        +
RECORD_AUDIO permission
        +
on-device speech recognition available
        ↓
Voice Wake may be scheduled
```

The product repairs one missing prerequisite at a time. The schedule is not written first and then repaired.

## Defense in depth

Permissions can be revoked after scheduling, old builds can leave durable occurrences behind, and Android may recreate a foreground service after process death. Therefore preflight is not sufficient by itself.

### Alarm fire

`AlarmReceiver` re-checks critical alarm presentation access before `beginActive()`. If the wake is no longer controllable, the schedule is invalidated and no playback service is started.

### Service recovery

`AlarmPlaybackService.ensureActiveWake()` independently performs the same critical presentation check. A redelivered `ACTION_START` or service recreation cannot resurrect critical audio after the required alarm controls have become unavailable.

### Reconciliation / package replacement / boot

`AlarmReconcileReceiver` invalidates planned or active unsafe occurrences. If an unsafe active service is running, the service component is explicitly stopped after durable authority is cleared.

### Manual app-open recovery

If the user opens Wake My Way while an occurrence is active and presentation access is unhealthy, `MainActivity` sends the terminal Stop command instead of attempting another Wake Surface launch.

## Why task dismissal does not stop a healthy alarm

Swiping Wake My Way from Android Recents is not a terminal alarm action. A real alarm clock must survive accidental task dismissal. The foreground service therefore remains independent from the UI task.

The safety invariant is not “task dismissal stops the alarm.” The invariant is:

> An alarm service may survive task dismissal only when the user has immediate, verified terminal controls.

## Consequences

- A user must explicitly complete required Android access before a Voice Wake can be scheduled.
- Existing schedules from older builds may be invalidated after upgrade if current prerequisites are incomplete.
- Revoking critical presentation access after scheduling may cause the affected wake to be cancelled rather than produce an uncontrollable siren.
- On-device voice recognition is required by the current Voice Wake product contract. A future explicit non-voice alarm mode may define a separate prerequisite set.
- Critical alarm reliability remains local, but controllability is now treated as a safety precondition rather than post-commit enrichment.

## Validation

Automated coverage must verify scheduling blocker priority and existing Alarm Kernel behavior. Physical validation must start from a clean install/upgrade state, grant each requested capability, confirm that scheduling remains impossible until all required access is ready, then exercise a locked-screen near-term wake.

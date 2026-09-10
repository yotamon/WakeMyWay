# ADR 019 — Voice Wake scheduling is permission-gated before commit

**Status:** Accepted  
**Date:** 2026-09-10  
**Evidence:** second founder physical-device regression after PR #37

## Context

A second physical test reproduced the dangerous audible-only failure: critical alarm audio started, the Wake Surface did not appear, Alfred did not start, and dismissing the application task did not stop playback. The user ultimately had to uninstall the app to terminate the alarm.

The persistence was explainable by the existing alarm reliability design: `AlarmPlaybackService` is independent from the recent-app task and is intentionally restartable. That is correct only when the active wake always has reachable terminal controls.

Android 13+ can run a foreground service even when notification permission is denied; the foreground-service notice may not be visible in the notification drawer. Therefore a design that persists an alarm first and asks the user to repair notification/full-screen access later can still create an uncontrollable alarm.

The second regression also exposed a testing bypass: the developer Wake Alarm Lab could directly call `AlarmKernel.commitSchedule()` while checking only exact-alarm capability. A fresh install could therefore arm a T+2m wake before notification, full-screen and microphone setup was complete even though the normal product UI had become stricter.

## Decision

For the current product, scheduling a Voice Wake is a **preflight transaction**.

No Wake Occurrence may be committed from any user-accessible scheduling path until all of these are true:

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

Wake My Way currently declares `USE_EXACT_ALARM` because precise alarms are core product functionality. On supported Android versions that permission is granted automatically rather than requested as a normal runtime permission. Exact-alarm capability remains a fail-closed health fact in case device/platform policy makes it unavailable.

The developer Wake Alarm Lab consumes the same `WakeSchedulingGate` as the production setup flow, receives the same current voice readiness, and re-checks immediately before its T+2m commit. Diagnostic surfaces are not allowed to bypass production safety invariants.

## Defense in depth

Permissions can be revoked after scheduling, old builds can leave durable occurrences behind, and Android may recreate a foreground service after process death. Therefore preflight is not sufficient by itself.

### Alarm fire

`AlarmReceiver` re-checks critical alarm presentation access before `beginActive()`. If the wake is no longer controllable, the schedule is invalidated and no playback service is started.

### Service recovery

`AlarmPlaybackService.ensureActiveWake()` independently performs the same critical presentation check. A redelivered `ACTION_START` or service recreation cannot resurrect critical audio after the required alarm controls have become unavailable.

### Reconciliation / package replacement / boot

`AlarmReconcileReceiver` invalidates planned or active unsafe occurrences. If an unsafe active service is running, durable authority is cleared first and the service component is explicitly stopped so playback resources are released.

### Manual app-open recovery

If the user opens Wake My Way while an occurrence is active and presentation access is unhealthy, `MainActivity` clears durable schedule authority and stops `AlarmPlaybackService` directly. It does not use the normal recurring Stop path, because an unsafe wake must not create a replacement occurrence, and it does not attempt another Wake Surface launch that Android may still refuse to present.

If presentation access is healthy, manual app-open remains a foreground rescue into the real `WakeActivity`.

## Why task dismissal does not stop a healthy alarm

Swiping Wake My Way from Android Recents is not a terminal alarm action. A real alarm clock must survive accidental task dismissal. The foreground service therefore remains independent from the UI task.

The safety invariant is not “task dismissal stops the alarm.” The invariant is:

> An alarm service may survive task dismissal only when the user has immediate, verified terminal controls.

## Consequences

- A user must explicitly complete required Android access before a Voice Wake can be scheduled.
- Existing schedules from older builds may be invalidated after upgrade if current prerequisites are incomplete.
- Revoking critical presentation access after scheduling may cause the affected wake to be cancelled rather than produce an uncontrollable siren.
- On-device voice recognition is required by the current Voice Wake product contract. A future explicit non-voice alarm mode may define a separate prerequisite set.
- Offline TTS availability remains a separately observed runtime capability for now; it is not falsely described as a permission. Whether to hard-gate scheduling on an installed Alfred-compatible offline voice requires a separate product decision after presentation reliability is physically re-proven.
- Critical alarm reliability remains local, but controllability is now treated as a safety precondition rather than post-commit enrichment.

## Validation

Automated coverage must verify scheduling blocker priority, the shared production/lab preflight, existing Alarm Kernel behavior, lint/build, visual regression and API-36 device reliability. Physical validation must start from a clean install/upgrade state, verify that both production setup and the Wake Alarm Lab refuse to schedule with missing prerequisites, grant each requested capability, then exercise a locked-screen near-term wake.

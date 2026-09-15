# ADR 019 — Voice Wake scheduling is permission-gated before commit

**Status:** Accepted  
**Date:** 2026-09-10  
**Amended:** 2026-09-15  
**Evidence:** founder physical-device regressions after PR #37

## Context

A founder physical test reproduced a dangerous audible-only failure: critical alarm audio started, the Wake Surface did not appear, Alfred did not start, and dismissing the application task did not stop playback. The user ultimately had to uninstall the app to terminate the alarm.

The persistence was explainable by the existing alarm reliability design: `AlarmPlaybackService` is independent from the recent-app task and is intentionally restartable. That is correct only when the active wake always has reachable terminal controls.

Android 13+ can run a foreground service even when notification permission is denied; the foreground-service notice may not be visible in the notification drawer. Therefore a design that persists an alarm first and asks the user to repair notification/full-screen access later can still create an uncontrollable alarm.

The original hardening correctly made Voice Wake creation fail closed, but it treated future scheduling capability, active execution safety, and voice enrichment as one readiness concept. That created two undesirable consequences:

- losing microphone/on-device recognition after a valid schedule was created could silently delete an otherwise safe alarm;
- losing exact-alarm capability after Android had already delivered a wake could stop an active alarm even though Stop remained immediately reachable.

Those are different questions and must not share one predicate.

## Decision

For the current product, **creating a new Voice Wake remains a strict preflight transaction**.

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
new Voice Wake may be scheduled
```

The product repairs one missing prerequisite at a time. The schedule is not written first and then repaired.

However, Wake My Way now distinguishes four concepts:

```text
Future scheduling readiness
  exact alarm + safe presentation

Active execution safety
  notifications + high-priority channel + full-screen presentation

Voice conversation readiness
  microphone + on-device recognition (+ optional Realtime enrichment)

Snooze readiness
  active execution safety + exact scheduling at the moment Snooze is requested
```

Wake My Way currently declares `USE_EXACT_ALARM` because precise alarms are core product functionality. Exact-alarm capability remains a fail-closed requirement for creating/reconciling future occurrences and for durable Snooze replacement. It is not a reason to silence an occurrence that Android already delivered and that still exposes safe terminal controls.

The developer Wake Alarm Lab consumes the same `WakeSchedulingGate` as the production setup flow, receives the same current voice readiness, and re-checks immediately before its T+2m commit. Diagnostic surfaces are not allowed to bypass production scheduling invariants.

## Capability loss after scheduling

A previously valid schedule does not become the same thing as a new scheduling transaction.

- Losing notification permission, required channel importance, or full-screen alarm access makes the wake potentially uncontrollable. A future affected occurrence is invalidated; an unsafe active service is stopped after durable authority is cleared.
- Losing exact-alarm capability invalidates/requires repair of a future occurrence. If the occurrence has already fired, the active wake continues as long as presentation remains safe. Snooze fails closed because a durable exact replacement cannot be guaranteed.
- Losing microphone permission or on-device recognition after scheduling does **not** delete the alarm. The morning experience degrades to the remaining local capabilities: critical alarm, Wake Surface, deterministic local presentation where available, and motion evidence. A newly created Voice Wake still requires voice readiness at commit time.
- Optional Realtime availability is never part of Wake Ready and never controls alarm survival.

## Defense in depth

Permissions can be revoked after scheduling, old builds can leave durable occurrences behind, and Android may recreate a foreground service after process death. Therefore preflight is not sufficient by itself.

### Alarm fire

`AlarmReceiver` re-checks **active execution safety** before `beginActive()`. If notification/channel/full-screen presentation can no longer expose immediate controls, the schedule is invalidated and no playback service is started. Exact-alarm and voice readiness are intentionally not re-litigated after Android has already delivered the occurrence.

### Service recovery

`AlarmPlaybackService.ensureActiveWake()` independently performs the same active execution-safety check. A redelivered `ACTION_START` or service recreation cannot resurrect critical audio after terminal controls become unavailable, but loss of future exact scheduling does not silence a safely controllable active wake.

### Reconciliation / package replacement / boot

`AlarmReconcileReceiver` applies the predicate appropriate to the durable state:

- planned occurrence → future scheduling readiness;
- active occurrence → active execution safety.

If an unsafe active service is running, durable authority is cleared first and the service component is explicitly stopped so playback resources are released.

### Manual app-open recovery

If the user opens Wake My Way while an occurrence is active and presentation access is unhealthy, `MainActivity` clears durable schedule authority and stops `AlarmPlaybackService` directly. It does not use the normal recurring Stop path, because an unsafe wake must not create a replacement occurrence.

If presentation access is healthy, manual app-open remains a foreground rescue into the real `WakeActivity`, even if exact-alarm access or voice capability has since degraded.

## Why task dismissal does not stop a healthy alarm

Swiping Wake My Way from Android Recents is not a terminal alarm action. A real alarm clock must survive accidental task dismissal. The foreground service therefore remains independent from the UI task.

The safety invariant is not “task dismissal stops the alarm.” The invariant is:

> An alarm service may survive task dismissal only when the user has immediate, verified terminal controls.

## Consequences

- A user must explicitly complete required Android access before a new Voice Wake can be scheduled.
- Existing schedules from older builds may be invalidated after upgrade if future scheduling/presentation prerequisites are incomplete.
- Revoking critical presentation access after scheduling may cause the affected wake to be cancelled rather than produce an uncontrollable siren.
- Revoking microphone/on-device recognition after scheduling degrades voice behavior instead of silently deleting a safe alarm.
- Losing exact-alarm access after a wake has fired does not stop the active wake; Snooze remains unavailable/fail-closed until exact scheduling is restored.
- On-device voice recognition remains required by the current **new Voice Wake** product contract. A future explicit non-voice alarm mode may define a separate creation prerequisite set.
- Offline TTS availability remains a runtime capability rather than a permission or alarm-survival requirement.
- Critical alarm reliability remains local, with controllability treated as an execution-safety precondition and voice treated as degradable experience capability.

## Validation

Automated coverage must verify:

- scheduling blocker priority for new wakes;
- active execution safety ignoring future exact-alarm capability;
- voice capability degradation not deleting an already valid future alarm;
- Snooze failure preserving the current active wake when exact scheduling is unavailable;
- shared production/lab preflight;
- existing Alarm Kernel behavior, lint/build, visual regression and API-36 device reliability.

Physical validation must start from a clean install/upgrade state, verify that production setup and Wake Alarm Lab refuse new scheduling with missing prerequisites, then exercise locked-screen near-term wakes while independently revoking voice, exact-alarm and presentation capabilities to prove the intended degradation/cancellation behavior.

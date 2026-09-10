# Permission-gated wake safety

**Status:** implementation in progress  
**Date:** 2026-09-10  
**Decision:** [`../adr/019-permission-gated-wake-scheduling.md`](../adr/019-permission-gated-wake-scheduling.md)

## Why this exists

A physical founder test reproduced an audible-only wake where the foreground alarm service continued independently of the dismissed application task. Because notification/full-screen controls were unavailable, the user had no practical terminal action and ultimately uninstalled the app to stop playback.

Android foreground-service behavior makes task dismissal the wrong safety boundary. Wake My Way therefore treats **verified terminal controllability before scheduling** as the boundary.

## Normal product scheduling

```text
Tap “Make tomorrow ready”
        ↓
WakeSchedulingGate
        ├─ exact alarm capability
        ├─ notifications enabled
        ├─ active-wake channel HIGH
        ├─ full-screen alarm access
        ├─ microphone permission
        └─ on-device recognition availability
        ↓
all ready?
   ├─ no → open next repair / permission flow
   │         no occurrence committed
   └─ yes → AlarmKernel.commitSchedule()
```

## Runtime defense in depth

Scheduling-time checks are not trusted forever.

```text
Alarm fires
   ↓
AlarmReceiver re-checks critical presentation access
   ├─ unsafe → cancel schedule, do not start service
   └─ safe   → begin active execution
                  ↓
            AlarmPlaybackService
                  ↓
       re-check again before playback
           ├─ unsafe → cancel + stop
           └─ safe   → foreground alarm
```

The service-side check also applies to Android process/service recreation and redelivered start intents.

## Reconciliation

Boot, timezone/time changes and package replacement can expose previously persisted occurrences after capabilities changed. `AlarmReconcileReceiver` invalidates those unsafe occurrences. If unsafe active playback exists, durable authority is cleared first and the service component is then stopped so local audio resources are released immediately.

## Manual recovery

Opening Wake My Way while an occurrence is active is treated as an explicit recovery action.

- presentation healthy → route to the real `WakeActivity`;
- presentation unhealthy → terminal Stop instead of another presentation attempt.

## Why swiping Recents still does not stop a healthy alarm

A real alarm must survive accidental task dismissal. `AlarmPlaybackService` therefore remains independent from the recent-app task. The correction is not to couple alarm lifetime back to UI lifetime; the correction is to guarantee reachable terminal controls before allowing that independent service to exist.

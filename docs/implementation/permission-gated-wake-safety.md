# Permission-gated wake safety

**Status:** implementation in PR #38  
**Date:** 2026-09-10  
**Decision:** [`../adr/019-permission-gated-wake-scheduling.md`](../adr/019-permission-gated-wake-scheduling.md)

## Why this exists

A physical founder test reproduced an audible-only wake where the foreground alarm service continued independently of the dismissed application task. Because notification/full-screen controls were unavailable, the user had no practical terminal action and ultimately uninstalled the app to stop playback.

Android foreground-service behavior makes task dismissal the wrong safety boundary. Wake My Way therefore treats **verified terminal controllability before scheduling** as the boundary.

The developer Wake Alarm Lab was also found to bypass the normal product readiness flow: its T+2m action checked only exact-alarm capability. On a fresh install this could reproduce the unsafe alarm even after the production setup UI had been hardened.

## Shared scheduling preflight

Both normal Wake Setup and Wake Alarm Lab now consume `WakeSchedulingGate`:

```text
Schedule request
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

The lab additionally re-evaluates the gate immediately inside its T+2m click handler. A stale Compose state cannot authorize the commit.

`USE_EXACT_ALARM` is declared for the core alarm use case and is not presented as an ordinary user runtime permission. Its capability remains part of fail-closed health checks.

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

Opening Wake My Way while an occurrence is active is an explicit recovery action.

- presentation healthy → route to the real `WakeActivity`;
- presentation unhealthy → call `AlarmKernel.cancelSchedule()` and stop `AlarmPlaybackService` directly.

The unsafe path intentionally does not use normal `stopActive()`, because a recurring wake must not schedule a replacement after the product has already determined that immediate controls are unsafe.

## Why swiping Recents still does not stop a healthy alarm

A real alarm must survive accidental task dismissal. `AlarmPlaybackService` therefore remains independent from the recent-app task. The correction is not to couple alarm lifetime back to UI lifetime; the correction is to guarantee reachable terminal controls before allowing that independent service to exist.

## Current voice boundary

`RECORD_AUDIO` and on-device recognition are hard pre-scheduling requirements for the current Voice Wake product. Alfred's offline TTS engine/voice is still an asynchronous runtime capability. It is not a permission and remains separately degradable for this regression fix; hard-gating on a verified installed Alfred-compatible offline voice is a separate product decision after locked-screen presentation is physically re-proven.

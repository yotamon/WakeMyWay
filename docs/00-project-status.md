# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** second physical regression hardening: permission-gated scheduling + unsafe-service stop safety  
**Active implementation:** `fix/wake-preflight-and-stop-safety`  
**Merged foundations:** PR #36 local conversational wake; PR #37 modern Android presentation/BAL hardening  
**Current reliability priority:** an alarm may not be created or resurrected unless immediate terminal controls are verified  
**Realtime track:** M8 issue #28 remains separate and non-authoritative  
**Learning track:** M7 deterministic core merged; live journal/policy wiring remains open under issue #27

## Current physical truth

A second founder-phone test on 2026-09-10 reproduced the unacceptable audible-only failure:

```text
alarm audio started
      ↓
WakeActivity did not appear
      ↓
Alfred did not start
      ↓
no practical Stop / Snooze surface
      ↓
dismissing the application task did not stop the foreground service
      ↓
user uninstalled the app to terminate playback
```

This demonstrates that **post-commit permission repair is not sufficient**.

The foreground alarm service is intentionally independent from the Recent Apps task, which is normally correct for an alarm clock. Android 13+ can also continue a foreground service even when notification permission is denied, while its notification is absent from the normal notification drawer. Therefore an alarm must never be persisted first and repaired later when those controls are missing.

Canonical decision: [`adr/019-permission-gated-wake-scheduling.md`](adr/019-permission-gated-wake-scheduling.md).  
Implementation note: [`implementation/permission-gated-wake-safety.md`](implementation/permission-gated-wake-safety.md).

## New scheduling invariant

The current product schedules a **Voice Wake** only after explicit preflight:

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
all ready
        ↓
AlarmKernel.commitSchedule()
```

If any prerequisite is missing, `WakeSchedulingGate` blocks commit. The next relevant Android repair/permission flow opens, and **no Wake Occurrence is written first**.

A future explicit non-voice alarm mode may define a separate prerequisite set. The current Voice Wake flow does not silently degrade at scheduling time.

## Defense in depth after scheduling

Preflight is not trusted forever because permissions, special access and OEM state can change after a schedule was created.

### Alarm fire

`AlarmReceiver` re-checks critical presentation access before `beginActive()`. If exact-alarm/notification/channel/full-screen controllability is no longer valid, the schedule is invalidated and `AlarmPlaybackService` is not started.

### Foreground-service recreation

`AlarmPlaybackService.ensureActiveWake()` performs the same critical presentation check before starting or resurrecting playback. This covers Android process recreation and redelivered service start intents that may not pass through a fresh receiver path.

### Boot / package replacement / time reconciliation

`AlarmReconcileReceiver` invalidates planned or active unsafe occurrences. If an unsafe active service exists, durable authority is cleared first and the service component is explicitly stopped, releasing `MediaPlayer` / `ToneGenerator` resources immediately.

### Manual app-open recovery

If the user opens Wake My Way while an occurrence is active:

- critical presentation access healthy → foreground rescue into the real `WakeActivity`;
- critical presentation access unhealthy → terminal Stop rather than retrying a presentation path Android may not permit.

## Why swiping Recents still does not stop a healthy alarm

`AlarmPlaybackService` remains independent from the application task. A real alarm must survive accidental task dismissal.

The corrected safety invariant is:

> A long-lived/restartable alarm service may exist only when immediate terminal controls have been verified, and it must refuse to start or resurrect if that becomes false.

Task dismissal is therefore not a Stop action. Stop/Snooze/Wake Surface remain explicit terminal/control paths for healthy alarms.

## Current architecture

```text
Wake Setup
    ↓
WakeSchedulingGate
    ├─ alarm presentation prerequisites
    └─ voice prerequisites
    ↓
Alarm Kernel commit
    ↓
AlarmManager.setAlarmClock()
    ↓
AlarmReceiver safety recheck
    ↓
AlarmPlaybackService safety recheck
    ├─ persistent immediate Stop/Snooze notification
    ├─ critical local alarm audio
    └─ Android 15/16 BAL-safe full-screen PendingIntent
                         ↓
                    WakeActivity
                         ↓
              WakeVoiceSessionController
                         ↓
                    WakeRuntime
```

The PR #36 spoken-reply gate remains unchanged: when healthy two-way voice is available, at least one coherent spoken reply is required in addition to activation evidence before completion.

## Automated validation required for this fix

- documentation validation;
- `WakeSchedulingGateTest`;
- existing wake-core tests;
- Android lint;
- instrumentation-test compilation;
- debug APK assembly;
- curated visual regression;
- API-36 device reliability lane.

## Next physical test

After the fix is green and merged:

1. install the exact new build;
2. open Wake My Way before scheduling anything;
3. attempt to save a wake with required access missing and verify that no occurrence is created;
4. complete each requested Android permission/special-access repair;
5. verify Voice replies is Ready;
6. only then create a near-term wake;
7. lock the phone before fire time;
8. verify WakeActivity opens and Stop/Snooze are immediately reachable;
9. verify Alfred speaks, asks for a reply, listens and accepts the reply;
10. verify an unsafe stale/legacy occurrence cannot start or resurrect indefinite audio;
11. verify opening the app during any unsafe active legacy wake terminates playback without requiring uninstall.

No emulator pass is treated as proof of the physical OnePlus/OxygenOS behavior.

## Open proof boundaries

- corrected locked-screen behavior still requires another founder-phone test;
- OxygenOS may impose OEM-specific lock-screen/full-screen policy beyond standard Android APIs;
- on-device recognition and local TTS availability vary by device;
- Bluetooth/audio routing remains unproven;
- motion thresholds remain uncalibrated;
- M7 live learning/journal wiring remains open;
- M8 realtime selection remains evidence-gated and separate.

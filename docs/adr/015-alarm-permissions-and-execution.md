# ADR-015: Exact alarms, full-screen presentation, and foreground playback

**Status:** Accepted  
**Date:** 2026-09-09

## Context

Wake My Way is a dedicated alarm-clock product. M1 needs a current Android permission/execution baseline that is consistent with the reliability architecture rather than a collection of unrelated manifest flags.

## Decision

For the Android-first product:

- use `AlarmManager.setAlarmClock()` for the next user-facing Wake Occurrence
- declare `USE_EXACT_ALARM` because exact wake timing is core alarm functionality
- declare `USE_FULL_SCREEN_INTENT` for eligible alarm presentation
- treat full-screen presentation as a capability, not a reliability prerequisite
- run critical active alarm playback in a foreground service with `foregroundServiceType="mediaPlayback"`
- declare `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- play critical local audio with `AudioAttributes.USAGE_ALARM`
- declare `RECEIVE_BOOT_COMPLETED` and use Direct-Boot-aware receivers only for lightweight reconciliation/re-registration
- request `POST_NOTIFICATIONS` where required, while retaining truthful degraded behavior when it is denied

The boot receiver must **not** start the media playback foreground service merely because the device booted. It only repairs future exact-alarm registration. Active playback begins when the actual exact Wake Occurrence fires.

## Reliability boundary

`WakeActivity` is presentation only. Failure or recreation of that activity must not own or terminate critical audio.

The OS registration sequence remains:

```text
persist next occurrence with registration unconfirmed
→ call setAlarmClock()
→ persist registration confirmation
```

If the process dies between those steps, reconciliation can re-register from durable state instead of reporting a false Wake Ready condition.

## Current policy assumptions to revalidate before release

As of September 2026, dedicated alarm/timer apps are an intended exact-alarm use case and alarm apps are within the restricted full-screen-intent eligibility category. These assumptions must be checked again before Play submission because store policy can change independently of the Android API contract.

## Sources

- Android exact alarms: https://developer.android.com/develop/background-work/services/alarms
- Full-screen intent limits: https://developer.android.com/about/versions/14/behavior-changes-14#secure-fsi
- Foreground-service types: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android 17 background-audio changes: https://developer.android.com/about/versions/17/changes/bg-audio

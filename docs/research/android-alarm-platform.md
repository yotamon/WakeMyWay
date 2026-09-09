# Android alarm platform research snapshot

**Snapshot date:** 2026-09-09

This document records time-sensitive Android platform facts that materially affect Wake My Way. Re-verify before changing target SDK, Play declarations, or public reliability claims.

## Exact alarm primitive

Wake My Way plans to use `AlarmManager.setAlarmClock()` for the concrete next user-facing Wake Occurrence.

Android documents this form as a precise, highly visible alarm clock event; the system does not adjust its delivery time and may leave low-power modes to deliver it.

Source:

- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/reference/android/app/AlarmManager

## Exact-alarm capability / permissions

For modern target SDKs, exact alarm use requires one of Android's alarms/reminders permission paths.

Android distinguishes `USE_EXACT_ALARM` and `SCHEDULE_EXACT_ALARM`; they represent similar exact-alarm capability but have different grant/policy behavior. Alarm-clock apps are an intended exact-alarm use case, but Google Play policy applies.

Decision intentionally deferred to M0/M1:

- final manifest declaration
- whether a user special-access repair flow is needed
- exact onboarding copy

Source:

- https://developer.android.com/develop/background-work/services/alarms

## Full-screen intent

Full-screen intent is intended for highly time-sensitive use cases such as alarms and incoming calls. Android 14+ restricts default eligibility more tightly and provides `NotificationManager.canUseFullScreenIntent()` plus a settings flow.

Wake My Way must not equate "alarm fired" with "WakeActivity always forcibly becomes full screen". Lock state, capability, notification behavior, Android version, and OEM behavior matter.

Source:

- https://developer.android.com/about/versions/14/behavior-changes-14

## Direct Boot

Before first unlock after reboot, credential-encrypted storage is unavailable. Android provides device-encrypted/device-protected storage and allows Direct-Boot-aware components to receive `ACTION_LOCKED_BOOT_COMPLETED`.

Alarm-clock apps are explicitly listed as a Direct Boot use case.

WMW design consequence:

- Critical Wake Snapshot only in device-protected storage
- sensitive/private context remains credential-protected
- generic safe local alarm before first unlock

Source:

- https://developer.android.com/privacy-and-security/direct-boot

## Explicit Force Stop on Android 15+

Android 15 cancels all pending intents when an app enters the stopped state through Force Stop. When user action later removes the app from stopped state, Android can deliver `ACTION_BOOT_COMPLETED` so pending intents can be re-registered. `ApplicationStartInfo.wasForceStopped()` can help identify this start condition.

WMW design consequence:

- do not promise alarm delivery after explicit Force Stop
- show/repair readiness on next user start
- test the limitation and recovery rather than asserting impossible delivery

Source:

- https://developer.android.com/about/versions/15/behavior-changes-all

## Android 17 background-audio hardening

Android 17 adds stricter rules for background audio interactions. Apps need a visible activity or suitable foreground service for background audio interactions. For apps targeting API 37, the while-in-use foreground-service requirement has an exception when the app has exact-alarm capability and operates on `USAGE_ALARM` audio streams.

WMW design consequence:

- critical alarm audio uses alarm-appropriate audio attributes
- realtime microphone/conversation begins only from an allowed visible/foreground lifecycle
- Android 17 behavior belongs in compatibility testing before targeting API 37

Source:

- https://developer.android.com/about/versions/17/changes/bg-audio
- https://developer.android.com/about/versions/17/behavior-changes-17

## Revalidation triggers

Refresh this research when any of these occur:

- target/compile SDK major update
- Google Play exact-alarm/full-screen policy update
- public beta/production submission
- wake delivery differs on a new Android release/OEM
- foreground-service/audio behavior changes
- manifest permission strategy changes

# Android alarm platform research snapshot

**Snapshot date:** 2026-09-09

This document records time-sensitive Android/Google Play facts that materially affect Wake My Way. Re-verify before changing target SDK, Play declarations, active-audio architecture, or public reliability claims.

## Google Play target API baseline

Starting 2026-08-31, Google Play requires new Android phone/tablet apps and app updates to target **Android 16 / API level 36 or higher**.

WMW M0 consequence:

```text
targetSdk = 36 baseline
compileSdk = 36+ with the current stable toolchain
minSdk = deliberate project decision during M0
```

Do not treat API 37 as the initial target merely because Android 17 exists; instead test Android 17 behavior and raise target SDK deliberately when required/ready.

Source:

- https://support.google.com/googleplay/android-developer/answer/11926878

## Exact alarm primitive

Wake My Way plans to use `AlarmManager.setAlarmClock()` for the concrete next user-facing Wake Occurrence.

Android documents exact alarms as appropriate when an app's core functionality depends on precisely timed behavior such as an alarm clock, and `setAlarmClock()` represents a highly visible user alarm-clock event.

Source:

- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/reference/android/app/AlarmManager

## Exact-alarm capability / permissions

Android distinguishes `USE_EXACT_ALARM` and `SCHEDULE_EXACT_ALARM`.

Current Android/Play guidance states that `USE_EXACT_ALARM` is restricted to applications whose **core, user-facing functionality genuinely requires precise timing**, with dedicated alarm/timer applications explicitly fitting the intended category. Unlike `SCHEDULE_EXACT_ALARM`, it does not require the user-granted special-access flow, but Play policy/review applies.

### Current WMW direction

Because Wake My Way is fundamentally a dedicated alarm-clock product, the implementation hypothesis is now:

```text
USE_EXACT_ALARM
+
AlarmManager.setAlarmClock()
```

This is a preferred direction, not permission to ignore policy drift.

Before manifest implementation and again before Play submission:

- re-check current Play restricted-permission policy
- verify WMW's store listing/product still clearly qualifies as an alarm-clock use case
- verify current target SDK behavior
- switch to `SCHEDULE_EXACT_ALARM` or another path only if current policy/platform evidence requires it
- record a reversal in the decisions/ADR trail

Do not build a special-access onboarding screen if the implemented permission path does not need one.

Sources:

- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/reference/kotlin/android/Manifest.permission#USE_EXACT_ALARM
- https://support.google.com/googleplay/android-developer/answer/16558241

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

Android 15 cancels pending intents when an app enters the stopped state through explicit Force Stop. When user action later removes the app from stopped state, Android can provide startup/boot-related recovery signals; `ApplicationStartInfo.wasForceStopped()` can help identify the condition on supported versions.

WMW design consequence:

- do not promise alarm delivery after explicit Force Stop
- show/repair readiness on next user start
- clear/repair stale active-execution state
- test the limitation and recovery rather than asserting impossible delivery

Source:

- https://developer.android.com/about/versions/15/behavior-changes-all

## Android 17 background-audio hardening

Android 17 enforces stricter rules for background audio interactions including playback, audio focus requests, and volume changes.

For apps running on Android 17, background audio generally requires a visible Activity or a foreground service that is not `SHORT_SERVICE`.

For apps targeting API 37, the background foreground-service requirement becomes stricter, but Android documents an exception to the while-in-use capability requirement when:

```text
app has exact-alarm permission
+
audio interaction uses USAGE_ALARM
```

WMW design consequence:

- the Alarm Kernel owns an **Active Wake Execution** after the trigger
- critical alarm audio uses alarm-appropriate `USAGE_ALARM` attributes
- initial implementation direction uses a foreground alarm playback service/controller rather than making `WakeActivity` the only lifetime owner
- `WakeActivity` may recreate without being allowed to silence critical playback
- duplicate playback-owner starts must converge on one active occurrence
- realtime microphone/conversation begins only from an allowed visible/foreground lifecycle
- Android 17 behavior belongs in M2 compatibility testing even while target SDK remains 36
- rerun tests before eventually targeting API 37

Sources:

- https://developer.android.com/about/versions/17/changes/bg-audio
- https://developer.android.com/about/versions/17/behavior-changes-17

## Active alarm lifecycle implication

Scheduling research alone does not prove alarm reliability.

The implementation must separately prove:

```text
occurrence delivered
→ Active Wake Execution established
→ safe local audio starts
→ UI can recreate
→ playback owner can recover idempotently
→ stop remains terminal
→ durable snooze replacement remains authoritative
```

See ADR-014 and `docs/18-testing-quality.md`.

## Revalidation triggers

Refresh this research when any of these occur:

- target/compile SDK major update
- Google Play target API requirement update
- Google Play exact-alarm/full-screen policy update
- public beta/production submission
- wake delivery differs on a new Android release/OEM
- foreground-service/audio behavior changes
- manifest permission strategy changes
- `USE_EXACT_ALARM` eligibility/review guidance changes

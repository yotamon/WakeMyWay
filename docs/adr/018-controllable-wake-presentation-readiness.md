# ADR 018 — Controllable wake presentation is part of Wake Ready

**Status:** Accepted  
**Date:** 2026-09-10  
**Evidence:** founder physical-device dogfood after PR #36

## Context

PR #36 proved that the Alarm Kernel could start critical local audio while the new local conversational Wake Session remained isolated behind `WakeActivity`.

Physical dogfood then exposed a dangerous product gap: the alarm audio fired, but Android did not present `WakeActivity`; no Alfred voice session started; visible Stop/Snooze controls were not reachable; the user had to terminate the app to silence the alarm.

The implementation was technically following the previous readiness definition. `AlarmKernel.health().ready` required exact-alarm registration but treated notification and full-screen presentation failures as degraded optional presentation.

That definition was wrong for Wake My Way.

A fallback alarm that can make sound is valuable as the final safety layer, but a scheduled wake must not be called **Wake Ready** when the user may be left with critical audio and no immediately reachable wake surface or terminal controls.

Modern Android also places explicit constraints on full-screen alarm presentation. On Android 14+ the app must check `NotificationManager.canUseFullScreenIntent()`, and full-screen intent delivery requires a notification channel at `IMPORTANCE_HIGH` or higher. Android 13+ notification permission is separately relevant to visible notification controls.

## Decision

Wake Ready now requires all of the following presentation capabilities in addition to exact alarm registration:

```text
exact alarm capability + registered occurrence
                +
notifications enabled
                +
active-wake notification channel >= IMPORTANCE_HIGH
                +
full-screen alarm access where Android exposes that special access
                ↓
             Wake Ready
```

These are **critical controllability capabilities**, not optional personalization.

Microphone permission, on-device speech recognition, Alfred TTS availability, Tomorrow Contract, calendar, weather, realtime AI and cloud access remain optional richness and do not determine base Wake Ready.

## Repair behavior

The normal Tonight surface must expose the exact missing critical capability and a direct repair action before bedtime.

- missing `POST_NOTIFICATIONS` / disabled notifications → explanatory primer or app notification settings;
- active-wake channel below high importance → that channel's Android settings;
- missing full-screen alarm access on Android 14+ → `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`;
- returning from Android Settings → immediately recompute Alarm Health.

Saving a schedule may persist/register the occurrence first, but if the resulting Alarm Health is not Wake Ready the setup flow continues directly into repair instead of presenting success as complete readiness.

## Active-alarm rescue

Full-screen intent remains the correct background alarm presentation mechanism. Wake My Way does not attempt to bypass Android background-activity restrictions.

However, if Android/OEM presentation still fails and the user manually opens Wake My Way while a Wake Occurrence is active, `MainActivity` must route immediately to the real `WakeActivity` for that occurrence. The user must not land on Tonight while critical audio continues elsewhere.

This rescue path restores access to Wake Surface, Stop/Snooze and the local voice controller from a user-foregrounded context.

## Fallback hierarchy

```text
full WakeActivity + local conversational wake
              ↓
high-priority persistent alarm notification + Stop/Snooze
              ↓
manual app-open rescue → WakeActivity
              ↓
critical bundled alarm audio
              ↓
emergency tone fallback
```

Lower layers preserve wake safety, but the existence of an audible fallback does not make a missing higher-level controllability capability "Ready".

## Consequences

### Positive

- Wake Ready becomes truthful to the sleeping-user experience rather than only alarm registration.
- The exact physical failure observed on 2026-09-10 is detectable before the next wake.
- Notification/full-screen problems become repairable product states instead of morning surprises.
- The user retains at least two control recovery paths even if full-screen presentation fails: notification actions and manual-open rescue.
- Voice failure remains decoupled from critical audio reliability.

### Trade-offs

- A schedule can be successfully stored and exactly registered while Tonight still says **Needs attention** until presentation access is repaired.
- Sideloaded/debug builds and OEM Android variants may require explicit user action for special access.
- Android may intentionally show a heads-up alarm notification rather than force a full-screen takeover while the device is already actively in use. That is not considered a locked-screen wake failure if controls remain reachable.

## Validation

Automated gates must cover build/lint, the stricter Alarm Health semantics, visual repair states and API-36 alarm-kernel regression tests.

Physical validation remains required on the representative founder phone:

1. complete every Wake system repair until Tonight reports Wake Ready;
2. enable Voice replies separately;
3. schedule a near-term Wake Occurrence;
4. lock the device before fire time;
5. verify WakeActivity appears and Stop/Snooze are reachable;
6. verify Alfred speaks, listens and requires a real reply when local voice is healthy;
7. verify notification controls and manual-open rescue still terminate/control an active alarm if full-screen presentation is intentionally disabled for a fallback test.

No emulator pass alone proves OEM locked-screen behavior.

# ADR 018 — Controllable wake presentation is part of Wake Ready

**Status:** Accepted  
**Date:** 2026-09-10  
**Evidence:** founder physical-device dogfood after PR #36

## Context

Physical dogfood exposed a dangerous gap after PR #36:

```text
critical alarm audio started
        ↓
WakeActivity did not appear
        ↓
Alfred never started
        ↓
Stop / Snooze were not reachable
        ↓
user terminated the app to stop audio
```

The implementation had two independent platform/readiness gaps:

1. `AlarmKernel.health().ready` required exact-alarm registration but treated notification and full-screen presentation failures as optional degradation.
2. Wake My Way targets SDK 36. Android 15+ no longer grants a `PendingIntent` creator background-activity-launch privilege by default, while the full-screen wake PendingIntent had not explicitly opted into creator BAL.

That combination allowed critical audio to work while the wake surface failed to appear.

A bundled audible fallback remains essential, but a scheduled wake must not be called **Wake Ready** when the sleeping user may have no immediately reachable wake surface or terminal controls.

## Decision

Wake Ready requires all critical controllability capabilities in addition to a valid registered/active occurrence:

```text
exact alarm capability
        +
registered / active occurrence
        +
notifications enabled
        +
active-wake channel >= IMPORTANCE_HIGH
        +
full-screen alarm access where required
        ↓
     Wake Ready
```

Microphone permission, on-device recognition, Alfred TTS availability, Tomorrow Contract, calendar, weather, realtime AI and cloud access remain optional richness and do not determine base Wake Ready.

## Android 15+ full-screen PendingIntent creation

The full-screen PendingIntent targeting `WakeActivity` explicitly opts into creator background-activity-start privilege:

```text
API < 34     default legacy creation
API 34–35    MODE_BACKGROUND_ACTIVITY_START_ALLOWED
API 36+      MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
```

`ALLOW_IF_VISIBLE` is not suitable because Wake My Way is intentionally expected to be non-visible immediately before a scheduled locked-screen wake. The privilege is scoped only to the PendingIntent that opens `WakeActivity`; it is not granted to arbitrary app navigation.

## Presentation capability ownership

`AlarmPresentationAccess` owns platform inspection and channel creation for:

- notification permission/system enablement;
- `active-wake` notification-channel existence and importance;
- Android 14+ `NotificationManager.canUseFullScreenIntent()`.

The channel is created before readiness evaluation, not for the first time after the alarm is already firing.

## Canonical repair target

When multiple capabilities are missing, the UI copy and the Settings destination must use one shared priority. `AlarmHealth.repairTarget()` is the single source of truth:

```text
exact alarm
   ↓
notifications
   ↓
active-wake channel
   ↓
full-screen intent
   ↓
none
```

This prevents a CTA from naming one problem while opening a different Settings screen.

## Repair behavior

For a scheduled occurrence that is not Wake Ready, Tonight promotes the Wake system repair card above optional content and exposes the canonical repair action.

- missing notifications → explanatory runtime-permission flow or app notification settings;
- active-wake channel below high importance → that channel's Android settings;
- missing full-screen alarm access on Android 14+ → `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`;
- exact-alarm anomaly → platform/app settings review;
- returning from Android Settings → immediately recompute Alarm Health.

When the wake is healthy, the approved normal Tonight hierarchy remains unchanged. A schedule may be persisted before presentation access is complete, but successful save continues directly into repair until readiness becomes truthful.

## Active-alarm rescue

Full-screen intent remains the correct background alarm mechanism. Wake My Way does not bypass Android restrictions with raw background `startActivity()`.

If Android/OEM presentation still fails and the user manually opens Wake My Way while an occurrence is active, the now-foreground `MainActivity` immediately routes that occurrence to the real `WakeActivity`.

```text
manual app open during active alarm
             ↓
active occurrence detected
             ↓
WakeActivity rescue
             ↓
Stop / Snooze + voice session reachable
```

The user must not be stranded on Tonight while critical audio continues.

## Fallback hierarchy

```text
WakeActivity + local conversational wake
              ↓
persistent alarm notification + Stop/Snooze
              ↓
manual app-open rescue → WakeActivity
              ↓
critical bundled alarm audio
              ↓
emergency tone fallback
```

Lower layers preserve wake safety. Their existence does not make missing controllability capabilities Ready.

## Consequences

Positive consequences:

- Wake Ready reflects the sleeping-user experience rather than only scheduling state.
- The physical failure observed on 2026-09-10 is detectable before the next wake.
- The full-screen PendingIntent follows Android 15/16 creator-BAL rules.
- Notification/full-screen problems become repairable product states.
- Stop/Snooze remain reachable through multiple recovery paths.
- Voice remains decoupled from critical audio reliability.

Trade-offs:

- a schedule may be stored/exactly registered while Tonight still reports **Needs attention**;
- sideloaded/debug builds and OEM Android variants may require explicit special-access setup;
- Android may show a heads-up alarm notification instead of a forced full-screen takeover while the device is already actively in use;
- creator BAL privilege is deliberately granted only to the scheduled wake-surface PendingIntent.

## Validation

Automated gates must cover:

- Android build/lint and instrumentation compilation;
- stricter Alarm Health semantics;
- canonical repair-target priority;
- existing curated product visual goldens;
- API-36 Alarm Kernel/device regression tests.

Visual repair presentation was manually inspected during investigation, but this decision does not weaken the curated visual gate by blanket-accepting new binary baselines. The normal approved Ready/empty goldens remain stable.

Physical validation remains mandatory on the representative founder phone:

1. complete every Wake system repair until Tonight reports Wake Ready;
2. enable Voice replies separately;
3. schedule a real near-term occurrence through the production Alarm Kernel;
4. lock the device before fire time;
5. verify `WakeActivity` appears and Stop/Snooze are reachable;
6. verify Alfred speaks, listens and requires a real reply when local voice is healthy;
7. intentionally test notification/manual-open rescue with full-screen access removed.

No emulator pass alone proves OEM locked-screen behavior.

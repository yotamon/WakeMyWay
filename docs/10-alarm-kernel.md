# Alarm Kernel

Canonical domain terms are defined in [`../CONTEXT.md`](../CONTEXT.md).

## Responsibility

The Alarm Kernel is the deep Android module that owns this invariant:

> An accepted next Wake Occurrence becomes a recoverable exact Android alarm with a safe audible path, and remains so across process death, snooze, reboot, time changes, and recoverable capability changes.

Its promise remains:

> **Intelligence may fail. The alarm may not.**

That promise applies inside the documented Android reliability envelope; explicit user Force Stop, uninstall/disable, powered-off hardware, or revoked required system capabilities can prevent delivery and must be communicated honestly.

## Primary Android primitive

Use `AlarmManager.setAlarmClock()` for the user-set Wake Occurrence. Android documents `setAlarmClock()` as the most critical exact-alarm form and does not adjust its delivery time. Re-verify exact-alarm and Play policy at implementation/release time.

Do not use WorkManager to fire the wake alarm.

## V1 scheduling model

V1 supports **one active Wake Schedule** producing one next Wake Occurrence at a time.

The schedule may express different wake times by weekday, but Wake My Way does not initially behave like a generic stack-of-many-independent-alarms utility.

Why:

- the product is intended to replace repeated backup alarms with one adaptive wake attempt
- one next occurrence simplifies readiness, reconciliation, snooze, and user trust
- multiple independent alarms can be added later if dogfood/beta evidence shows a real need

A Snooze Occurrence temporarily replaces the current wake attempt with a new exact occurrence.

## Wake Schedule vs Wake Occurrence

### Wake Schedule

Reusable user intent, for example:

```text
Weekdays: 08:00
Weekend: 09:00
Follow current local timezone
Character: Alfred
```

### Wake Occurrence

Concrete next execution:

```text
2026-09-10 08:00 Europe/Berlin
-> resolved Instant
-> scheduled exactly with Android
```

Tomorrow Contract and Prepared Wake Plan are associated with the next Wake Occurrence, not with Android's alarm object itself.

## Deep scheduling transaction

The following ordering is **internal Alarm Kernel implementation knowledge**:

```text
accept next Wake Occurrence
       |
       v
persist/update rich schedule state
       |
       v
write Critical Wake Snapshot atomically
       |
       v
schedule exact OS alarm
       |
       v
verify/reconcile readiness state
```

No UI/ViewModel/use-case caller should manually coordinate those steps.

If a step fails, the module owns rollback/reconciliation semantics and returns a truthful readiness result.

## Critical Wake Snapshot

The Critical Wake Snapshot is a deliberately small recovery representation, stored in **device-protected storage** so it can be available during Direct Boot.

Keep only what is needed to re-register/identify/start a safe alarm, for example:

```text
formatVersion
wakeOccurrenceId
scheduledInstant
scheduleRevision / trigger identity
safe alarm tone configuration
checksum
```

Possible optional data must remain non-sensitive and genuinely necessary for critical recovery.

Do **not** place in device-protected critical storage:

- Tomorrow Contract raw text/summary
- calendar content
- transcripts
- AI prompts
- personalized contextual generated speech
- secrets/tokens

Those remain credential-protected.

The bundled emergency alarm asset is packaged with the app and does not need a sensitive file path in the snapshot.

## Direct Boot

Android clears `AlarmManager` alarms when the device powers off/reboots. Wake My Way must explicitly handle the period before first user unlock.

Plan:

- declare the relevant boot receiver Direct-Boot aware
- receive `ACTION_LOCKED_BOOT_COMPLETED`
- read only the device-protected Critical Wake Snapshot
- re-register the next exact alarm if still applicable
- if the wake occurs before first unlock, use the bundled/generic branded alarm path
- after user unlock, normal credential-protected plan/context can be used again

This is a deliberate privacy/reliability tradeoff: a pre-unlock wake remains audible, but sensitive personalized content is not moved into Direct-Boot storage.

## Bundled emergency audio

Ship a permanent local resource such as:

```text
res/raw/emergency_alarm.ogg
```

It must not depend on download, cache, TTS, provider SDK, Room, or network.

## Trigger flow

```text
OS exact alarm fires
      |
      v
thin AlarmReceiver
      |
      +--> identify occurrence from trusted trigger identity/snapshot
      +--> ensure high-priority alarm presentation path
      +--> enter Alarm Kernel wake-start path
      |
      v
safe audible alarm begins first
      |
      v
WakeActivity / Wake Runtime enrich the experience
```

Cloud and normal app initialization must not delay the first safe audible frame.

## Receiver responsibilities

Keep the receiver intentionally thin:

- validate/identify the trigger
- delegate to Alarm Kernel wake-start behavior
- post required alarm notification/presentation

Do not perform:

- AI calls
- weather/calendar work
- analytics uploads
- generated speech requests
- long database scans
- product policy decisions

## WakeActivity and full-screen behavior

`WakeActivity` is the dedicated alarm-session UI shell.

Android presentation is capability/lock-state dependent. Do not assume full-screen activity appears in every condition:

- when the device is locked and full-screen intent capability is allowed, a full-screen alarm surface is appropriate
- when the device is already unlocked/in use, modern Android may show an expanded heads-up notification rather than forcibly replacing the current screen
- if full-screen intent permission/capability is unavailable, the audible alarm and high-importance notification path remain important fallbacks

Alarm Health must reflect real capability rather than pretending the UI is guaranteed to take over every screen.

## Notification permission

Android 13+ notification permission is a separate readiness concern from exact-alarm capability/full-screen intent. The implementation must explicitly test the chosen alarm notification behavior when `POST_NOTIFICATIONS` is denied and must not discover this edge case on a real morning.

The exact onboarding copy/flow remains implementation-time work after the chosen manifest strategy is verified.

## Exact-alarm permission strategy

Wake My Way is genuinely an alarm-clock app. Android provides `USE_EXACT_ALARM` for apps whose core functionality depends on exact alarms, subject to app-store policy; `SCHEDULE_EXACT_ALARM` uses a different special-access flow.

M0/M1 must choose the final declaration based on current Play policy and document it in an ADR before public distribution.

Do not hard-code a user permission screen in UX specifications until that choice is made.

## Snooze invariant

Snooze is not a delay held in process memory.

```text
user confirms snooze
      |
      v
Alarm Kernel creates Snooze Occurrence
      |
      v
persist critical state + exact schedule
      |
      v
ONLY AFTER success: finish current Wake Session
```

If replacement scheduling fails, the current alarm must not silently disappear.

## Reconciliation

Reconciliation is Alarm Kernel implementation, not a separate policy module callers coordinate.

It must be idempotent and restore the one next Wake Occurrence after relevant events such as:

- `LOCKED_BOOT_COMPLETED`
- `BOOT_COMPLETED` / user unlock when richer state becomes available
- timezone change
- system wall-clock change
- exact-alarm capability change
- app update/start
- schedule edit/cancel
- first launch after Force Stop where Android reports prior stopped-state recovery

## Force Stop reliability limit

On Android 15+, explicit user Force Stop puts the package in a stopped state and cancels pending intents. The app cannot truthfully guarantee a scheduled alarm while the user has intentionally force-stopped it.

On the next user launch/interacting action, Wake My Way must:

- detect/reconcile schedule state where platform APIs allow
- re-register the next occurrence
- show Wake Ready only after readiness is restored

Do not treat swiping the app from Recents as equivalent to a normal process death guarantee without device/version testing.

## Timezone and DST

Store Wake Schedule intent in local-time semantics rather than only UTC.

Resolution policy must be explicit and tested for:

- nonexistent local time during DST spring transition
- duplicated local time during DST fall transition
- timezone travel/change
- manual wall-clock changes

Default V1 hypothesis:

> the active Wake Schedule follows the user's current local timezone

A travel-specific fixed-zone mode is not V1.

## Wake Ready

Wake Ready is a computed product truth, not a decorative checkmark.

Core readiness includes at least:

```text
next Wake Occurrence resolved
critical snapshot valid
exact alarm scheduled/capable
safe alarm audio available
required alarm presentation/notification capability understood
```

Optional richness may show separately:

```text
microphone
prepared personalized plan
calendar
weather
realtime voice
```

Missing optional richness never makes the base alarm silently unsafe.

## Kernel dependencies

Keep the first-audio path close to stable Android platform primitives. Do not initialize heavy player/AI/network/analytics graphs before safe alarm output is guaranteed.

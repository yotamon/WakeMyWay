# Alarm Kernel

Canonical domain terms are defined in [`../CONTEXT.md`](../CONTEXT.md).

## Responsibility

The Alarm Kernel is the deep Android module that owns this invariant:

> An accepted next Wake Occurrence becomes a recoverable exact Android alarm with a safe audible path, and remains locally actionable across process death, UI recreation, snooze, reboot, time changes, and recoverable capability changes.

Its promise remains:

> **Intelligence may fail. The alarm may not.**

That promise applies inside the documented Android reliability envelope; explicit user Force Stop, uninstall/disable, powered-off hardware, or revoked required system capabilities can prevent delivery and must be communicated honestly.

The Alarm Kernel's responsibility does **not** end when `AlarmManager` fires. It also owns the trust-critical **Active Wake Execution** until the occurrence is intentionally stopped, durably snoozed, completed, or explicitly terminated.

See ADR-014.

## Primary Android primitive

Use `AlarmManager.setAlarmClock()` for the user-set Wake Occurrence. Android documents `setAlarmClock()` as a precise, highly visible alarm-clock event. Re-verify exact-alarm and Play policy at implementation/release time.

Do not use WorkManager to fire the wake alarm.

## Exact-alarm permission direction

As of 2026-09-09, the preferred manifest strategy is **`USE_EXACT_ALARM`** because Wake My Way is a dedicated alarm-clock application whose core user-facing functionality genuinely requires exact timing.

This is still policy-sensitive. Before implementing the manifest and again before Play submission:

- re-check Google Play's restricted-permission policy
- verify WMW still qualifies as a permitted alarm-clock use case
- verify target/compile SDK behavior
- record any change to `SCHEDULE_EXACT_ALARM` or another flow in an ADR

Do not build a user-facing special-access flow unless the implemented permission path actually requires one.

## V1 scheduling model

V1 supports **one active Wake Schedule** producing one next Wake Occurrence at a time.

The schedule may express different wake times by weekday, but Wake My Way does not initially behave like a generic stack-of-many-independent-alarms utility.

Why:

- the product is intended to replace repeated backup alarms with one adaptive wake attempt
- one next occurrence simplifies readiness, reconciliation, snooze, and user trust
- multiple independent alarms can be added later if dogfood/beta evidence shows a real need

A Snooze Occurrence temporarily replaces the current wake attempt with a new exact occurrence.

A temporary dogfood **Safety Backup** may exist as a later conventional fallback alarm while trust is being established, but it is not another adaptive Wake Schedule and must not force generic multi-alarm orchestration into the kernel contract.

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

## Trigger and Active Wake Execution flow

```text
OS exact alarm fires
      |
      v
thin AlarmReceiver
      |
      +--> identify occurrence from trusted trigger identity/snapshot
      +--> enter Alarm Kernel active-wake start
      |
      v
foreground alarm playback owner
      |
      +--> safe local USAGE_ALARM audio begins
      +--> alarm notification / accessible controls
      +--> persist/attach to active occurrence identity
      |
      v
WakeActivity / Wake Runtime enrich the experience
```

Cloud and normal app initialization must not delay the first safe audible frame.

The exact implementation may use an `AlarmPlaybackService` or equivalent private component. The public architecture rule is more important than the class name: **critical playback lifetime is not owned by `WakeActivity`.**

## Active occurrence recovery

Persist the minimum normal/critical facts needed to make recreation idempotent.

The kernel must distinguish at least conceptually:

```text
scheduled occurrence
active occurrence
intentionally stopped occurrence
durably snoozed/replaced occurrence
completed occurrence
```

The exact persistence status model is implementation-time work, but these invariants must hold:

- recreating `WakeActivity` does not silence the active alarm
- recreating the playback owner does not start duplicate overlapping alarm streams
- an occurrence intentionally stopped must not resurrect on component recreation
- an occurrence durably replaced by snooze must not resurrect
- process recovery while an occurrence is actively alerting restores a safe actionable state where Android allows
- stale trigger identities cannot revive an older occurrence

Do not persist conversation/private context merely to recover critical alarm playback.

## Foreground playback / Android 17

Android 17 hardens background audio interactions. The planned active-alarm execution therefore uses a visible wake surface and/or an appropriate foreground service, with alarm audio using `USAGE_ALARM` attributes.

For apps targeting API 37, Android documents an exact-alarm + `USAGE_ALARM` exception to the while-in-use capability requirement, but implementation must still be tested on the actual target SDK/device matrix.

Do not rely on incidental Activity visibility as the only reason alarm audio is allowed to continue.

## Wake locks and power

Do not hold a broad or indefinite wake lock by default.

If measurement shows a wake lock or related power primitive is required for a reliable transition, the Alarm Kernel owns its acquisition/release ordering and exposes no caller choreography.

Tests must prove stop/snooze/completion release critical resources and that recreation does not leak multiple locks/services/audio owners.

## Receiver responsibilities

Keep the receiver intentionally thin:

- validate/identify the trigger
- delegate to Alarm Kernel active-wake start behavior
- initiate the required alarm presentation/execution path

Do not perform:

- AI calls
- weather/calendar work
- analytics uploads
- generated speech requests
- long database scans
- product policy decisions

## WakeActivity and full-screen behavior

`WakeActivity` is the dedicated alarm-session UI shell.

It is **not** the critical playback lifecycle owner.

Android presentation is capability/lock-state dependent. Do not assume full-screen activity appears in every condition:

- when the device is locked and full-screen intent capability is allowed, a full-screen alarm surface is appropriate
- when the device is already unlocked/in use, modern Android may show an expanded heads-up notification rather than forcibly replacing the current screen
- if full-screen intent permission/capability is unavailable, the audible alarm and high-importance notification path remain important fallbacks

Alarm Health must reflect real capability rather than pretending the UI is guaranteed to take over every screen.

## Notification permission

Android 13+ notification permission is a separate readiness concern from exact-alarm capability/full-screen intent. The implementation must explicitly test the chosen alarm notification behavior when `POST_NOTIFICATIONS` is denied and must not discover this edge case on a real morning.

The exact onboarding copy/flow remains implementation-time work after the chosen manifest strategy is verified.

## Intentional stop invariant

The user must always have an accessible way to stop the alarm. The product may make stop deliberate rather than reflexive, but it must never trap the user.

The kernel must treat stop as an explicit, idempotent terminal action for the active occurrence:

```text
user intentionally stops
      |
      v
mark active occurrence stopped / cancel active execution
      |
      v
stop critical audio + release resources
      |
      v
ensure recreation/stale trigger cannot resurrect it
```

UX may require a small confirmation/gesture depending on accessibility testing, but kernel correctness does not depend on a fragile multi-screen interaction.

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
ONLY AFTER success: terminate old active execution / finish current Wake Session
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

Active Wake Execution recovery is related but distinct: it restores/attaches to an already-fired occurrence rather than re-registering the next scheduled one.

## Force Stop reliability limit

On Android 15+, explicit user Force Stop puts the package in a stopped state and cancels pending intents. The app cannot truthfully guarantee a scheduled or active alarm while the user has intentionally force-stopped it.

On the next user launch/interacting action, Wake My Way must:

- detect/reconcile schedule state where platform APIs allow
- re-register the next occurrence
- clear/repair stale active-execution state safely
- show Wake Ready only after readiness is restored

Do not treat swiping the app from Recents as equivalent to Force Stop or ordinary process death without device/version testing.

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
active wake execution path configured
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

Keep the first-audio and active-playback path close to stable Android platform primitives. Do not initialize heavy player/AI/network/analytics graphs before safe alarm output is guaranteed.

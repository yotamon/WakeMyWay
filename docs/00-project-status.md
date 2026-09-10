# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** physical dogfood verification after controllable-wake hardening  
**Merged implementation:** PR #37, squash commit `0f0afa920b649baf41d4047732267e8ef6b57f54`  
**Local voice baseline:** PR #36 merged; Alfred local TTS + on-device voice replies + motion + deterministic Wake Runtime  
**Current reliability priority:** re-prove a controllable locked-screen wake on the representative physical phone before further voice sophistication  
**Realtime track:** M8 issue #28 remains separate and non-authoritative  
**Learning track:** M7 deterministic core merged; live journal/policy wiring remains open under issue #27

## Executive status

Physical dogfood on 2026-09-10 exposed a serious locked-screen presentation defect after PR #36:

```text
scheduled wake fired
      ↓
critical alarm audio played
      ↓
WakeActivity did NOT appear
      ↓
Alfred never started
      ↓
Stop / Snooze were not reachable
      ↓
user had to terminate the app to stop audio
```

The audio fallback worked while the presentation/control path failed.

Investigation found two independent gaps:

1. `AlarmKernel.health().ready` treated notification and full-screen presentation capability as optional degradation. Wake My Way could claim **Wake Ready** even when Android could not present a controllable wake.
2. Wake My Way targets SDK 36. Android 15+ no longer grants a `PendingIntent` creator background-activity-launch privilege by default. The full-screen `WakeActivity` PendingIntent still used the pre-Android-15 creation shape.

PR #37 fixed both and is merged. Audible-only remains an emergency fallback, but it is no longer considered Wake Ready. The scheduled wake PendingIntent now explicitly follows Android 15/16 creator-BAL rules.

Canonical decision: [`adr/018-controllable-wake-presentation-readiness.md`](adr/018-controllable-wake-presentation-readiness.md).

## Current critical architecture

```text
Wake Schedule
    ↓
Alarm Kernel
    ↓
Critical Wake Snapshot
    ↓
AlarmManager.setAlarmClock()
    ↓
AlarmReceiver
    ↓
AlarmPlaybackService
    ├─ bundled local USAGE_ALARM audio
    ├─ immediate high-priority alarm notification
    ├─ Stop / Snooze actions
    └─ full-screen PendingIntent
          + explicit creator BAL opt-in on Android 14+
          ↓
       WakeActivity
          ↓
WakeVoiceSessionController
          ↓
      WakeRuntime
```

Critical audio remains service-owned and does not depend on Activity, microphone, TTS, network, AI, Tomorrow Contract, Wake Learning, Vercel or Supabase.

## Merged controllable-wake hardening

### Strict Wake Ready

Wake Ready now requires:

```text
exact alarm capability + registered/active occurrence
                  +
notifications allowed
                  +
active-wake channel importance >= HIGH
                  +
full-screen alarm special access where required
                  ↓
               Wake Ready
```

This distinguishes safe audible fallback from a wake that is ready to be controlled by a sleeping user.

Microphone/voice, private morning context and cloud enrichment remain optional and are surfaced separately.

### Android 15/16 full-screen launch contract

The full-screen Activity PendingIntent explicitly opts into creator background-activity-start privilege:

```text
API < 34     default legacy creation
API 34–35    MODE_BACKGROUND_ACTIVITY_START_ALLOWED
API 36+      MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
```

`ALLOW_IF_VISIBLE` is not used because the app is expected to be invisible/locked immediately before alarm fire. The privilege is scoped only to the PendingIntent that launches `WakeActivity`.

### Presentation capability owner

`AlarmPresentationAccess` / `AlarmPresentationCapabilities` owns:

- notification permission/system enablement;
- creation and inspection of the `active-wake` channel;
- high-importance channel requirement;
- Android 14+ `NotificationManager.canUseFullScreenIntent()`.

The alarm channel is created before readiness evaluation rather than for the first time at alarm fire.

### Canonical repair target

`AlarmHealth.repairTarget()` is the single source of truth for repair priority:

```text
exact alarm
   ↓
notifications
   ↓
active-wake HIGH channel
   ↓
full-screen intent access
   ↓
none
```

Tonight copy and MainActivity actions consume the same target, preventing a CTA from naming one problem while opening a different Settings screen.

### Repair-first UX

The approved normal Tonight hierarchy remains unchanged when the wake is ready or when no Wake Occurrence exists.

Only when a scheduled occurrence exists and is not Wake Ready does the critical Wake system repair card move directly below the page title, ahead of optional content. Returning from Android Settings immediately recomputes Alarm Health.

After saving a schedule, if the occurrence is registered but presentation access is incomplete, setup continues directly into the canonical repair flow instead of presenting complete readiness.

### Active-alarm rescue

If full-screen presentation still fails and the user manually opens Wake My Way while an occurrence is active:

```text
MainActivity foregrounded
       ↓
active occurrence detected
       ↓
WakeActivity rescue
       ↓
Stop / Snooze + local voice surface
```

The user must not be stranded on Tonight while critical audio continues.

### Notification fallback

The foreground alarm notification remains persistent/immediate and contains Stop/Snooze actions. It uses the same high-priority channel checked by readiness.

## Local conversational wake baseline

PR #36 remains the production voice foundation:

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

When local TTS and on-device voice input are healthy, orientation requires both the activation threshold and at least one coherent spoken reply. Motion cannot silently bypass the spoken-reply gate.

`LocalVoiceListener` uses Android on-device recognition only. Raw audio and transcript text are not persisted. The adapter emits compact typed evidence such as `VoiceResponseObserved(coherent = true/false)`.

`AlarmPlaybackService` owns the bounded voice-window lease:

```text
critical alarm 100%
      ↓ voice turn
bundled alarm bed 12%
      ↓ max 12 seconds
automatic restore 100%
```

The emergency tone fallback is never ducked. Stop, Snooze and successful completion use terminal-safe shutdown.

## Automated validation for PR #37

Final reviewed head:

`37cac84973e3b210b8747a47ec64d3b85f16d2e1`

Before merge, the same head passed:

- documentation validation;
- `:wake-core` tests;
- Android lint;
- instrumentation-test compilation;
- debug APK assembly and artifact upload;
- curated Roborazzi visual regression without accepting new baselines;
- API-36 device reliability instrumentation;
- review-thread check with no open review threads.

PR #37 was squash-merged to `main` as:

`0f0afa920b649baf41d4047732267e8ef6b57f54`

Earlier failing PR runs were investigated rather than bypassed. The API-36 failure was an old assertion that exact registration alone implied Wake Ready; its artifact confirmed no alarm-code crash. The earlier visual failure came from an overly broad hierarchy change and was refined so repair is promoted only for an actual scheduled-not-ready state.

## Physical reliability truth

Automated gates are green, but they do not prove OEM locked-screen behavior. The failed physical test on 2026-09-10 remains explicit evidence that the old build was not physically reliable.

Do not claim the corrected behavior is physically proven until the merged PR #37 build passes the representative phone test.

The next physical test begins only after Tonight says **Wake Ready** and Voice replies separately says **Ready**.

The debug **Wake Alarm Lab** schedules a real one-shot T+2m occurrence through the production Alarm Kernel and is the preferred fast verification path.

## Exact next work

1. Install the exact PR #37 final-head debug APK on the representative phone.
2. Open Wake My Way and follow every Wake system repair until Tonight reports **Wake Ready**.
3. Enable **Voice replies** separately and confirm **Ready**.
4. Use Wake Alarm Lab T+2m and lock the phone before fire time.
5. Verify `WakeActivity` appears over the locked screen.
6. Verify Stop and Snooze are reachable from both Wake Surface and notification.
7. Verify Alfred speaks, asks for a reply, listens and accepts the spoken response.
8. Verify motion alone cannot finish the wake while healthy two-way voice is required.
9. Intentionally test fallback by removing full-screen access, firing a wake, then manually opening Wake My Way and confirming immediate rescue into `WakeActivity`.
10. Test silence/timeout and critical volume restoration.
11. Test speaker/Bluetooth routing after the baseline locked-screen path passes.
12. Tune voice/alarm coexistence constants only from physical measurements.

## Current risks / open proof boundaries

- corrected Android 15/16 full-screen behavior is not yet re-proven on the founder phone;
- OxygenOS/OnePlus may add OEM-specific lock-screen behavior beyond standard Android capability APIs;
- on-device recognition and local TTS availability vary by device;
- Bluetooth/audio-route behavior remains unproven;
- motion thresholds remain uncalibrated on representative phones;
- M7 live learning/journal wiring remains open;
- M8 realtime selection remains evidence-gated and separate.

## Privacy boundaries

Never place raw microphone audio, recognized transcript text, Tomorrow Contract raw text, calendar descriptions, prompts, secrets/tokens, private generated speech or raw high-frequency motion streams into the Critical Wake Snapshot or reliability/analytics logs.

The production local voice path remains transcript-minimized and local-first. Presentation repair adds no cloud dependency and does not change Alarm Kernel or Wake Runtime authority boundaries.

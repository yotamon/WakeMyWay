# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** physical dogfood hardening after an audible-only presentation regression  
**Active implementation:** PR #37 `fix/critical-wake-presentation-readiness`  
**Local voice baseline:** PR #36 merged; Alfred local TTS + on-device voice replies + motion + deterministic Wake Runtime  
**Current reliability priority:** prove a controllable locked-screen wake on the representative physical phone before further voice sophistication  
**Realtime track:** M8 issue #28 remains separate and non-authoritative  
**Learning track:** M7 deterministic core merged; live journal/policy wiring remains open under issue #27

## Executive status

Wake My Way has a functioning local critical alarm and a production-connected local conversational Wake Session, but physical dogfood on 2026-09-10 exposed a serious locked-screen presentation defect after PR #36.

Observed on the founder phone:

```text
scheduled wake fired
      ↓
critical alarm audio played
      ↓
WakeActivity did NOT appear
      ↓
Alfred / voice controller never started
      ↓
no visible Stop / Snooze controls
      ↓
user had to terminate the app to stop audio
```

The audio path worked while the presentation/control path failed.

Investigation found two independent Android/platform gaps:

1. `AlarmKernel.health().ready` treated notification and full-screen presentation capability as optional degradation. The product could claim **Wake Ready** even when Android could not present a controllable wake.
2. Wake My Way targets SDK 36. Android 15+ no longer grants a `PendingIntent` creator background-activity-launch privilege by default. The full-screen `WakeActivity` PendingIntent still used the pre-Android-15 creation shape.

PR #37 fixes both. Audible-only remains the emergency fallback, but it is not considered Wake Ready. The full-screen PendingIntent now explicitly follows Android 15/16 creator-BAL rules.

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

## PR #37: controllable wake presentation hardening

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

This deliberately distinguishes **safe fallback audio** from **a wake that is ready to be controlled by a sleeping user**.

Microphone/voice, private morning context and cloud enrichment remain optional and are surfaced separately.

### Android 15/16 full-screen launch contract

Wake My Way targets SDK 36, so the full-screen Activity PendingIntent explicitly opts into creator background-activity-start privilege.

```text
API < 34     default legacy creation
API 34–35    MODE_BACKGROUND_ACTIVITY_START_ALLOWED
API 36+      MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
```

`ALLOW_IF_VISIBLE` is deliberately not used for the scheduled wake surface because the app is expected to be invisible/locked immediately before alarm fire. The privilege is scoped only to the PendingIntent that launches `WakeActivity`; arbitrary application navigation does not receive this opt-in.

### Presentation capability owner

`AlarmPresentationAccess` / `AlarmPresentationCapabilities` owns the Android platform facts for:

- notification permission/system enablement;
- creation and inspection of the `active-wake` channel;
- high-importance channel requirement;
- Android 14+ `NotificationManager.canUseFullScreenIntent()`.

The active-wake channel is created during application startup before readiness is evaluated, rather than for the first time after an alarm is already firing.

### One canonical repair target

When more than one capability is missing, copy and action must never disagree. `AlarmHealth.repairTarget()` is the single source of truth for repair priority:

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

Both `Tonight` copy and `MainActivity` repair behavior consume that same target. Unit coverage locks the priority so a CTA cannot say one thing and open an unrelated Settings screen.

### Repair-first Tonight UX

The approved normal Tonight hierarchy remains unchanged when the wake is ready or when no Wake Occurrence exists.

Only when a **scheduled occurrence exists and is not Wake Ready** does the critical **Wake system** repair card move directly below the page title, ahead of optional Tomorrow Contract content. It names the actual missing capability and exposes a direct repair action.

Repair routes include:

- notifications → explanatory permission flow / Android notification settings;
- channel priority → active-wake notification-channel settings;
- full-screen alarm access → Android full-screen-intent special access;
- exact alarm anomaly → app/platform settings review.

Returning from Android Settings immediately recomputes Alarm Health.

### Save → repair continuation

A Wake Schedule can be stored and exactly registered while presentation access is incomplete. After save, the setup flow checks resulting Alarm Health.

If `wakeReady == false`, the product continues directly into the canonical repair flow rather than letting “Make tomorrow ready” appear successful while a critical capability is missing.

### Active-alarm rescue path

If full-screen presentation still fails because of Android/OEM behavior and the user manually opens Wake My Way while an occurrence is active:

```text
MainActivity foregrounded
       ↓
AlarmKernel.activeOccurrence()
       ↓
explicit WakeActivity rescue
       ↓
Stop / Snooze + local voice surface
```

The user must not be stranded on Tonight while alarm audio continues. This rescue happens from a user-foregrounded context and is not a raw background `startActivity()` workaround.

### Notification fallback

The active foreground notification remains persistent and contains Stop/Snooze actions. It requests immediate foreground-service presentation and uses the same high-priority channel checked by readiness.

If Android intentionally chooses a heads-up alarm notification while the device is actively in use, reachable controls remain the invariant. The physical locked-screen test remains the proof gate for full wake-surface presentation.

## Local conversational wake baseline from PR #36

PR #36 remains the production local voice foundation.

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

When local TTS and on-device voice input are healthy, orientation requires:

```text
activation score >= threshold
        +
at least one coherent spoken reply
```

Motion cannot silently bypass the spoken-reply requirement. If local voice capability disappears, Wake Runtime deliberately removes the voice gate so optional capability loss cannot trap a critical alarm forever.

`LocalVoiceListener` uses Android on-device recognition only. Raw audio and recognized transcript text are not persisted. The adapter emits only compact typed evidence such as `VoiceResponseObserved(coherent = true/false)`.

`AlarmPlaybackService` owns a bounded voice-window lease so Alfred and the microphone remain intelligible without surrendering alarm safety:

```text
critical alarm 100%
      ↓ voice turn
bundled alarm bed 12%
      ↓ max 12 seconds
automatic restore 100%
```

The emergency tone fallback is never ducked. Stop, Snooze and successful completion use terminal-safe voice shutdown.

## Validation strategy for PR #37

Earlier PR heads intentionally exposed two stale assumptions:

- visual verification failed when the entire normal Tonight hierarchy was moved; the actual repair presentation was inspected manually, then the implementation was refined so only a real scheduled-not-ready state promotes the repair card. Existing curated ready/empty goldens remain unchanged rather than blanket-approving unrelated layout churn;
- the first API-36 run failed one old assertion that exact registration alone implied Wake Ready. The device artifact confirmed this was the only failing assertion. The test now checks the stricter real capability contract instead of weakening production readiness.

Additional automated coverage now locks:

- every critical presentation capability is required for presentation readiness;
- canonical multi-capability repair priority;
- existing curated product goldens;
- API-36 alarm-kernel behavior;
- normal Android build, lint, instrumentation compilation and APK assembly.

The final PR head must pass all Android CI, visual regression and API-36 device reliability gates before merge.

## Physical reliability truth

PR #36's previous green emulator/device workflow did **not** prove the physical locked-screen experience. The 2026-09-10 founder test is explicit counter-evidence to any such claim.

Do not call the wake physically reliable until the corrected PR #37 build passes the representative phone test.

The next physical test begins only after Tonight says **Wake Ready** and Voice replies separately says **Ready**.

The existing debug **Wake Alarm Lab** can schedule a real one-shot T+2m Wake Occurrence through the production Alarm Kernel. It is the preferred fast physical verification path after installing the corrected APK.

## Exact next work

1. Finish final-head Android CI, curated visual regression and API-36 device reliability for PR #37.
2. Merge PR #37 only when all automated gates are green and review threads are clean.
3. Install the exact final-head debug APK on the founder phone.
4. Follow every Wake system repair until Tonight reports **Wake Ready**.
5. Enable **Voice replies** separately and confirm **Ready**.
6. Use Wake Alarm Lab T+2m, lock the phone before fire time, and verify `WakeActivity` appears.
7. Verify Stop and Snooze are reachable from both Wake Surface and notification.
8. Verify Alfred speaks, asks for a reply, listens and accepts a spoken response.
9. Verify motion alone cannot finish the wake while healthy two-way voice is required.
10. Intentionally test fallback: remove full-screen access, fire a wake, then manually open Wake My Way and confirm immediate rescue into `WakeActivity`.
11. Test silence/timeout and critical volume restoration.
12. Test speaker/Bluetooth routing after the baseline locked-screen path passes.
13. Tune voice/alarm coexistence constants only from physical measurements.

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

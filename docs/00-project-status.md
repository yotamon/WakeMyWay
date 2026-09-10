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

Wake My Way has a functioning local critical alarm and a production-connected local conversational Wake Session, but physical dogfood on 2026-09-10 exposed a serious readiness defect after PR #36.

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

The audio path therefore worked while the presentation/control path failed.

The root cause was not the voice reducer. `AlarmKernel.health().ready` incorrectly treated notification and full-screen presentation capability as optional degradation. The product could claim **Wake Ready** even when Android was not able to present a controllable wake.

PR #37 changes that contract. Audible-only remains a valuable emergency fallback, but it is no longer considered Wake Ready.

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
    └─ full-screen PendingIntent → WakeActivity
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

### Presentation capability owner

PR #37 adds `AlarmPresentationAccess` / `AlarmPresentationCapabilities` as the Android platform owner for:

- notification permission/system enablement;
- creation and inspection of the `active-wake` channel;
- high-importance channel requirement;
- Android 14+ `NotificationManager.canUseFullScreenIntent()`.

The active-wake channel is created during application startup, before readiness is evaluated, rather than for the first time after an alarm is already firing.

### Repair-first Tonight UX

Tonight now promotes **Wake system** above optional Tomorrow/Contract content.

If a scheduled occurrence is not ready, the card names the actual missing capability and exposes a direct repair action:

- notifications → explanatory permission flow / Android notification settings;
- channel priority → active-wake notification-channel settings;
- full-screen alarm access → Android's full-screen-intent special-access settings;
- exact alarm problem → platform settings/review path.

A device with no scheduled Wake Occurrence shows a neutral **Waiting** state rather than falsely implying that something is broken.

Returning from Android Settings immediately recomputes Alarm Health.

### Save → repair continuation

A Wake Schedule can be stored and exactly registered while presentation access is still incomplete. After save, the setup flow now checks the resulting Alarm Health.

If `wakeReady == false`, the product continues directly into the relevant repair flow rather than letting “Make tomorrow ready” appear successful while a critical capability is missing.

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

The user must not be stranded on Tonight while alarm audio continues.

This is a rescue from a user-foregrounded context, not an attempt to bypass Android background-activity restrictions.

### Notification fallback

The active foreground notification remains persistent and contains Stop/Snooze actions. It is now requested as immediate foreground-service presentation and uses the same high-priority channel checked by readiness.

If Android intentionally chooses a heads-up notification while the device is actively in use, controls remain the important invariant. The physical locked-screen test remains the proof gate for full wake-surface presentation.

## Local conversational wake baseline from PR #36

PR #36 remains the production local voice foundation.

### Runtime behavior

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

When local TTS and on-device voice input are healthy, orientation requires:

```text
activation score >= threshold
        +
at least one coherent spoken reply
```

Motion cannot silently bypass the spoken-reply requirement. If local voice capability disappears, Wake Runtime removes the voice gate deliberately so the critical alarm cannot become trapped by optional capability loss.

### Voice privacy

`LocalVoiceListener` uses Android's on-device recognizer only. It does not fall back to a recognizer that may require network.

Raw audio and recognized transcript text are not persisted. The Android adapter discards recognized text and emits only compact typed evidence:

```text
VoiceResponseObserved(coherent = true/false)
```

The release local-voice baseline does not require `INTERNET` permission.

### Alarm / voice coexistence

`AlarmPlaybackService` owns a bounded voice-window lease so Alfred and the microphone can be intelligible without surrendering alarm safety:

```text
critical alarm 100%
      ↓ voice turn
bundled alarm bed 12%
      ↓ max 12 seconds
automatic restore 100%
```

The emergency tone fallback is never ducked. Stop, Snooze and successful completion use terminal-safe voice shutdown.

The 12% level, nine-second listen turn and 12-second lease remain tuning hypotheses until physical evidence says otherwise.

## Other merged foundations

- M0 native Android + pure-Kotlin core
- M1 Alarm Kernel + Active Wake Execution
- M2 reliability harness + API-36 emulator lane
- M3 deterministic Wake Runtime
- M4 bounded motion evidence
- M5 Alfred local character / offline TTS adapter
- M6 Tomorrow Contract + Prepared Wake Plan
- M7 deterministic Wake Learning v0 core
- Adaptive Dawn design system + Navigation 3 product setup
- curated Roborazzi visual-regression gate
- optional Vercel AI foundation outside critical wake authority
- M8 provider-neutral measurement harness + direct OpenAI WebRTC debug candidate

## Validation status for PR #37

### Confirmed on earlier PR #37 head

- documentation validation: passed;
- `:wake-core` tests: passed;
- Android lint/build: passed;
- instrumentation-test compilation: passed;
- debug APK assembly: passed.

### Failures intentionally investigated rather than bypassed

The first PR #37 visual run failed because the critical Wake system card was intentionally moved ahead of optional content and existing reviewed goldens no longer matched. The actual screenshots were inspected manually. A dedicated missing-full-screen-access visual fixture has been added so this exact critical state receives regression coverage.

The first API-36 device run had exactly one failing test: `AlarmKernelInstrumentedTest.cancellationTombstonePreventsStaleOccurrenceResurrection`. Its old `assertTrue(committed.ready)` assumed exact registration alone implied Wake Ready. The test has been updated to assert the stricter real capability contract rather than weakening production readiness.

A new final-head CI pass is required after the current hardening/docs/golden updates.

## Physical reliability truth

PR #36's previous green emulator/device workflow did **not** prove the physical locked-screen experience. The 2026-09-10 founder test is now explicit counter-evidence to any such claim.

Do not call the wake physically reliable until the corrected build passes the representative phone test.

The next physical test must begin only after Tonight says **Wake Ready** and Voice replies separately says **Ready**.

## Exact next work

1. Finish PR #37 final-head Android CI, visual regression and API-36 device reliability.
2. Review and explicitly commit the new/changed Roborazzi baselines, including the missing-full-screen-access repair state.
3. Merge PR #37 only when all automated gates are green.
4. Install the PR #37 debug APK on the founder phone.
5. Open Wake My Way and follow every **Wake system** repair action until the card says **Wake Ready**.
6. Enable **Voice replies** separately and confirm it says **Ready**.
7. Schedule a wake a few minutes ahead and lock the phone before fire time.
8. Verify the locked-screen WakeActivity actually appears.
9. Verify Stop and Snooze are reachable from the wake surface and notification.
10. Verify Alfred speaks, explicitly asks for a reply, listens, and accepts the spoken response.
11. Verify motion alone cannot finish the wake while healthy two-way voice is required.
12. Intentionally test fallback behavior: disable full-screen presentation, fire a test wake, then manually open Wake My Way and verify immediate rescue into WakeActivity rather than Tonight.
13. Test silence/timeout and verify critical volume restoration.
14. Test common speaker/Bluetooth routing after the baseline locked-screen path passes.
15. Tune voice/alarm coexistence constants only from physical measurements.

## Current risks / open proof boundaries

- the corrected full-screen path is not yet re-proven on the founder phone;
- OxygenOS/OnePlus may add OEM-specific presentation or lock-screen behavior beyond the standard Android capability APIs;
- on-device recognition availability varies by device and installed speech components;
- local TTS availability/quality varies by device;
- Bluetooth/audio-route behavior remains unproven;
- motion thresholds remain uncalibrated on representative phones;
- M7 live learning/journal wiring remains open;
- M8 realtime selection remains evidence-gated and separate.

## Privacy boundaries

Never place raw microphone audio, recognized transcript text, Tomorrow Contract raw text, calendar descriptions, prompts, secrets/tokens, private generated speech or raw high-frequency motion streams into the Critical Wake Snapshot or reliability/analytics logs.

The production local voice path remains transcript-minimized and local-first. The new presentation repair work adds no cloud dependency and does not change Alarm Kernel or Wake Runtime authority boundaries.

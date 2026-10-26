# Live queued-cue handoff: prevent false no-audio degradation

**Date:** 2026-10-25
**Scope:** W2 / B2, premium voice gate #194
**State:** Initial queued-cue fix merged in [#218](https://github.com/yotamon/WakeMyWay/pull/218) and published in signed v0.2.34; Cycle 1 follow-up code [#220](https://github.com/yotamon/WakeMyWay/pull/220) / [#221](https://github.com/yotamon/WakeMyWay/pull/221) merged with automated CI. Real-device acceptance remains pending.

## Reproduced code-path risk

When movement or an interrupted reply advances Wake Runtime while prior Live PCM is audible, `respond()` accepts an authorized new `SpeechIntent` and queues its cue behind that PCM. Previously the adapter started the same 12-second **first-audio** deadline used for already-dispatched cues. The queued cue had not yet reached the provider, so a legitimate long audio tail could wrongly trigger `live-no-audio`, degrade the remaining wake to alarm-only, and drop the important orientation/engagement turn.

## Change

- Split *waiting for earlier PCM to become quiet* (bounded 20-second drain watchdog) from *waiting for first PCM after sending a cue* (12-second watchdog).
- The latter is armed only by `sendCue()` after successful dispatch and is canceled by the first observed remote speech activity.
- When the old audio run settles, drain the queue, cancel its drain watchdog, and start a **fresh** first-audio budget for the newly dispatched cue.
- Cancel **both** timers when the session ends or closes, avoiding late callbacks after Stop / teardown.
- Keep fallback on an actually stalled queue or silent provider. No silent indefinite wait; the native alarm remains independently audible and accessible.
- New pure Kotlin `GptLiveCueWaitPolicy` and focused Direct tests distinguish the two wait phases, first PCM, stalled provider, and bounded timeout choices.

No changes to alarm ownership, wake-phase transitions, activation thresholds, learning, Stop/Snooze, voice character, provider identity, secrets, cost routing or privacy.

## Acceptance checklist

- [x] Focused Direct unit tests and Android CI pass for the initial #218 fix (signed v0.2.34).
- [x] Device-reliability workflow passes for the initial #218 fix (signed v0.2.34).
- [ ] On signed real Android device, motion during a long assistant phrase can enter Orienting and produce its voice cue **after** the previous PCM settles, without prematurely showing voice unavailable.
- [ ] If old PCM never settles, native audio returns by bounded failure, with Stop/Snooze still usable.
- [ ] If the dispatched new cue produces no audio, first-audio failure is still detected.
- [ ] If user interrupts or taps Stop, no delayed queued cue speaks after terminal action.
- [ ] Bluetooth/routing and offline fallbacks remain correct; no claim of audible speech from PCM-only telemetry.

Follow the existing `docs/37-physical-acceptance.md` and `docs/implementation/2026-10-25-intent-first-live-wake-quality.md` for real-morning evidence. Do not claim this patch proves real-world waking effectiveness.

## Follow-up: restore local alarm volume in queued-cue PCM gaps (2026-10-26)

**Code merged in [#221](https://github.com/yotamon/WakeMyWay/pull/221); Android CI, API 36 instrumentation and Docs CI passed. Physical speaker/overnight validation remains pending, and the latest published v0.2.34 predates this patch.**

The queued-cue transition also exposed a separate audio-layer defect: the adapter correctly suppresses the old PCM tail's `SpeechFinished` signal (so it cannot finish the newly authorized WakeRuntime directive), but this also bypassed the controller's ordinary local-alarm-volume restoration. The alarm could stay in a temporary voice window while the next cue waited up to 12 seconds for its first PCM.

The Direct Live adapter now emits a **non-authoritative PCM-gap signal** when the old PCM settles and a queued cue is about to be sent. The controller uses it only to release the temporary voice window while the next provider PCM is still silent (unless actively listening to the user). It does not synthesize `SpeechFinished`, change the WakeRuntime phase, close the voice session, alter timeout budgets, or change Stop/Snooze. The next actual remote speech-start event can acquire the voice window again.

The callback is guarded against a closed, degraded or hidden wake session. Existing critical alarm ownership remains in `AlarmPlaybackService`; provider audio is never accepted as evidence of actual speaker audibility.

### Specific acceptance

- [x] Direct policy unit test and Android CI pass ([run](https://github.com/yotamon/WakeMyWay/actions/runs/40248636836)).
- [x] Android device-reliability instrumentation passes ([run](https://github.com/yotamon/WakeMyWay/actions/runs/40248636810)); Android CI compiled the shared interface across Direct/Play variants.
- [ ] Signed phone test: let prior audible Live output settle while a newer runtime cue is queued; native alarm becomes audible at critical gain during the gap, then naturally yields to actual new PCM.
- [ ] Keep a 12-second silent new-provider cue and verify the critical alarm is *not* ducked throughout the silent wait or fallback.
- [ ] A genuine listening window may still hold its intended temporary voice window.
- [ ] Stop or Snooze during a queued gap prevents any later assistant turn; first-audio and drain watchdogs remain bounded.
- [ ] Verify same behavior on locked screen and after Bluetooth routing changes. Capture Wake Lab timeline plus human speaker observation, without transcripts/recordings.

Physical validation is still required for gates #9 and #194; automated timing/PCM events alone do not demonstrate the speaker or person woke up.

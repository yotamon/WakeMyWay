# Live queued-cue handoff: prevent false no-audio degradation

**Date:** 2026-10-25
**Scope:** W2 / B2, premium voice gate #194
**State:** Implementation branch, Android CI and signed real-device testing pending.

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

- [ ] Focused Direct unit tests and Android CI pass.
- [ ] Device-reliability workflow passes.
- [ ] On signed real Android device, motion during a long assistant phrase can enter Orienting and produce its voice cue **after** the previous PCM settles, without prematurely showing voice unavailable.
- [ ] If old PCM never settles, native audio returns by bounded failure, with Stop/Snooze still usable.
- [ ] If the dispatched new cue produces no audio, first-audio failure is still detected.
- [ ] If user interrupts or taps Stop, no delayed queued cue speaks after terminal action.
- [ ] Bluetooth/routing and offline fallbacks remain correct; no claim of audible speech from PCM-only telemetry.

Follow the existing `docs/37-physical-acceptance.md` and `docs/implementation/2026-10-25-intent-first-live-wake-quality.md` for real-morning evidence. Do not claim this patch proves real-world waking effectiveness.

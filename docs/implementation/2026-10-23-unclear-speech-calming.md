# Calm handling of uncertain Live speech

**Date:** 2026-10-23  
**Status:** Implemented on feature branch, automated and physical acceptance pending  
**Program:** [Wake Experience Excellence](2026-10-23-wake-experience-excellence-plan.md), W2  
**Release gates:** #194 (Live conversation quality), #9 (native alarm safety), #87 (real outcomes)

## Observed code behavior and change

Previously, `WakeRuntime.reduce(VoiceResponseObserved(coherent=false))` interpreted a tentative, non-coherent voice response as a reason to increment escalation, spend a limited verbal re-engagement, and immediately speak another instruction. That can occur after provider transcript uncertainty, background noise, a mutter, or assistant echo. Neither unclear speech nor an echo proves user refusal or continued sleep. Immediate extra speech is a plausible contributor to an annoying or pushy voice experience.

This slice keeps the existing deterministic Runtime but changes one behavior:

- **Unclear response:** no credit toward activation, **no phase advancement, no escalation, no immediate AI speech**, and no consumption of the verbal prompt budget. The Runtime still tells the native service to keep the alarm audible and requests motion observation.
- **After actual silence:** the already-existing timed `SilenceElapsed` path can offer **one bounded, gentle** verbal check-in. This is not an unconditional restart of the original sit-up instruction. When the budget is exhausted, native sound and movement observation continue.
- **Clear response:** unchanged; coherent speech is bounded engagement evidence, not verified physical out-of-bed success.
- **Movement:** can still move the Runtime toward Orienting without forcing a spoken reply.
- **No change** to Stop/Snooze authority, Alarm Kernel, audio/volume, WebRTC, microphone permissions, cloud/provider admission, billing or wake preferences.

The existing Live adapter's overlapping transcript recognition remains heuristic. We do **not** claim to have solved all echo and interruption classification: on-device tests are still required before adopting stronger WebRTC filtering policies.

## Regression evidence requirements

New deterministic `:wake-core` tests cover repeated unclear input, no speech/no escalation, a later single delayed re-engagement, preservation of the prompt bound, and movement-based orientation without coherent speech. Existing wake-runtime Stop/Snooze, speech-output and multi-alarm tests must stay passing. CI success alone does not prove physical user experience.

## Founder phone test, once included in a signed APK

Use **Test Wake Now** only for quick dialogue exploration, and a naturally scheduled locked-screen morning for real evidence. Keep your ordinary safety backup while TRUST #9 remains open.

1. Start a Live wake normally and, while the assistant is listening, **stay silent**; note whether the assistant waits without repeated demands. Native alarm must stay audible and Stop/Snooze available.
2. Make one deliberately unclear sound (`hmm` or quiet mutter), then stay quiet. It should **not** trigger an immediate stronger instruction. A single gentle check-in may occur later after the configured silence interval.
3. Answer naturally once. It should not sound like a new speaker or repeat the same movement demand; interruption should be respected.
4. Move without replying. WakeRuntime should still accept actual movement without spoken compliance.
5. Check a normal morning: did you get out of bed, did you return to sleep, were you annoyed, did Live actually reach the speaker, and did the alarm still sound on schedule?
6. If a session behaves strangely, share the **privacy-safe Wake Lab reliability report** plus app version and what you observed. Do not share private transcripts or calendar content.
7. Compare several mornings rather than a single demo. Record cases where voice was inaudible, too slow, repetitive, sounded like two characters, or made it harder to wake.

**Stop conditions:** any loss of critical local playback, failed/inaccessible Stop or Snooze, or increased repeat prompts due to this change blocks rollout. Human ratings and physical voice audibility (#194) remain unverified until tested.

## Next slices

Use reported real-morning outcomes to determine whether the gentle silence budget or character copy needs tuning. Keep reflex-Stop changes in separate evidence-backed Shape review; no forced dismiss challenge is authorized by this patch.

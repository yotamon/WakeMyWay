# Premium Live Wake — intent-first conversation and quick replies

**Date:** 2026-10-25  
**Scope:** W2 / B2, [Voice gate #194](https://github.com/yotamon/WakeMyWay/issues/194)  
**Status:** Implemented in branch; physical acceptance and PR CI pending.  
**Provider:** GPT-Live first; no TTS or model migration.

## Problem and concrete code causes

1. The user's default ACTION motivation was prepended to **every** generated Live cue. Even for `SpeechIntent.HoldEngagement` or `ReEngage`, the model saw "Focus on one tiny practical action" alongside "Do not give another task." This conflicted with our intended low-pressure dialogue contract and plausibly caused robotic instruction loops.
2. The `AskToMove` cue ordered another task after the first coherent response, without acknowledging that the person may have said "I'm exhausted" or "no."
3. `GptLiveUserTurnGate` rejected **all** candidate replies within four seconds of the last credited speech, including genuinely different responses to a natural short exchange. Quick users could seem ignored even when the Live stream received their voice.

## Implementation

- Keep deterministic Wake Runtime ownership, `SpeechIntent` allowlist and the one-re-engagement bound unchanged.
- Make Live cue phrasing **intent-first**: HoldEngagement and ReEngage cannot inherit action-oriented motivation. No new physical cue merely because a personal profile favors practical action.
- Allow `AskToMove` to **first acknowledge the actual utterance**; if the person sounds reluctant, decline another instruction without pretending they moved. A simple movement invitation is still available when appropriate.
- Keep initial wake short and calm, avoid reporting demands and repetitive posture instructions.
- Retain protection against remote PCM echo, fillers and fragments. Apply a short minimum separation between distinct transcript turns and a longer anti-duplicate cooldown only for the **same** recent normalized utterance.
- Store only an **ephemeral normalized fingerprint** of the last credited utterance, not text in logs, history or a database.
- Preserve native alarm service, exact scheduling, Stop/Snooze, user-facing persona, calendar/consent, remote session admission, costs and fallback semantics.

## Coverage

Added focused `testDirect` regression cases for:
- alternate short replies within two seconds, in English/Hebrew/German examples;
- repeated same utterance within four seconds;
- assistant audio overlap, filler utterances and text normalization;
- action motivation not overriding HoldEngagement/ReEngage;
- exhausted user's utterance being acknowledged before an optional movement suggestion.

**Local validation:** Kotlin compiler smoke for the pure `GptLiveUserTurnGate` logic, nine behavioral checks passed. Not a full Android Gradle build or on-device validation; those must remain explicit.

## Required physical review, using a signed in-place Direct build

1. **Natural first wake:** warm greeting; no mandatory reply; speech not double-starting or changing character.
2. **Tired/reluctant first answer:** say "I'm exhausted today" / Hebrew equivalent. The companion acknowledges it rather than immediately nagging about feet-on-floor; the native alarm remains reliable.
3. **Two different short replies:** natural back-to-back conversation should no longer discard the second merely because it came within 4 seconds; observe actual remote audio and semantic runtime progression.
4. **Same phrase/TV echo:** same repeated phrase and overlapping assistant audio must not create false activity credit or double responses.
5. **Silence/unintelligible mutter:** one delayed calm check-in at most, not instant nagging; afterward native alarm and motion observation continue.
6. **Interruption and hardware:** user barge-in, lock screen, speaker, Bluetooth route change, loss of network/Live, delayed audio. Stop/Snooze must always work and local alarm must not be silenced by provider failure.
7. **Morning result:** record actual get-up/return-to-bed separately from Activation Completion; note felt pleasantness and usefulness. Retain failures, not just green traces.

**Stop conditions:** critical local alarm or Stop/Snooze regression, duplicated assistant turns, mismatched persona, sustained silent speaker despite perceived Live success or increased repetitive prompts. Do not claim speaker audibility or confirmed waking from passing CI.

## Deferred and follow-up

- Multi-morning human voice quality and actual activation effects (#194, #87).
- Dynamic pause timing, audio-interruption speech classification and policy thresholds remain evidence-driven follow-up. The current conservative echo rule may sometimes reject genuine speech during simultaneous audio; it intentionally does **not** treat overlap as confirmed activity.
- No automatic migration to TTS and no cost-driven voice downgrade.

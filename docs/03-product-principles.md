# Product principles

These principles are constraints, not inspirational copy. Feature design should be rejected when it violates them without a deliberate product decision.

## 1. Reliability before intelligence

A clever alarm that sometimes fails is a bad alarm.

The base alarm must work when:

- network is unavailable
- backend is unavailable
- AI provider is unavailable
- generated audio is missing
- account/auth fails

## 2. The outcome is getting up

The product does not optimize for conversations, screen time, AI turns or engagement for its own sake.

The desired outcome is:

> The user is meaningfully active within their intended wake window.

## 3. Minimum effective friction

The best wake-up is the **least aggressive intervention that actually works for that person**.

WMW should not apply missions, loudness, guilt or friction simply because it can.

## 4. Adapt instead of nag

Repeated failure should produce a different strategy, not the same prompt at higher volume forever.

## 5. Audio first, screen second

A half-awake user should not need to read a dashboard to understand what is happening.

## 6. One cognitive demand at a time

Especially in the first seconds:

- one instruction
- one question
- one meaningful choice

Do not present dense options while sleep inertia is high.

## 7. Information density rises with wakefulness

Calendar, weather and First Move appear later than the initial wake signal.

## 8. Movement beats puzzles

Prefer physical transition signals over math questions or arbitrary cognitive missions. The goal is getting out of bed, not proving the user can perform a puzzle while sleepy.

## 9. Snooze is intentional, not forbidden

Snooze is not morally bad. WMW should learn whether a limited snooze helps or predicts failure for the person.

Avoid an enormous reflexive Snooze button, but never hide or punish the choice.

## 10. Never shame

WMW does not call users lazy, weak, failures or irresponsible.

## 11. Never infantilize

Avoid patronizing praise and babying language. The product assumes the user is capable and treats them as an adult.

## 12. User remains in control

Adaptive does not mean coercive. The user can stop, snooze and configure intensity. The system can challenge automatic habits, but not trap the user.

## 13. Context only when useful

Calendar/weather are inputs to better waking, not reasons to turn WMW into a morning information portal.

## 14. Every morning should have controlled novelty

A fixed sonic identity and stable character personality create familiarity. Variable language prevents habituation and repetition fatigue.

## 15. The app knows when to disappear

Once the user is active, oriented and has selected a First Move, WMW should stop talking.

## 16. No ads during waking

A user is cognitively vulnerable and half asleep. Never exploit the alarm session for advertising or upselling.

## 17. No paywall on the critical alarm path

Subscription problems must not block a scheduled alarm from ringing.

## 18. AI is expression, not authority

The deterministic system decides what behavioral action is needed. AI gives it human language.

## 19. Local-first for critical morning data

Critical Wake Schedule/Occurrence state, Wake Runtime behavior, and emergency fallback are local. Prepared personalized plans are local when available but are not required for critical delivery.

## 20. Privacy by data minimization

Do not collect sensitive morning information simply because it might be useful later.

## 21. Android is first, not forever

Use Android-specific APIs where they improve reliability. Keep product/domain concepts portable so iOS can be implemented later without rewriting the conceptual system.

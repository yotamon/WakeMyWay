# Monetization direction

## Principle

WakeMyWay monetizes the recurring **outcome**: helping a person get moving when they genuinely intended to wake, then becoming more effective for that person over time.

Do not design the paid product as a bag of implementation features merely because those features are easy to gate.

Monetization must never compromise trust during the wake session.

## Hard rules

- no advertisements during Active Wake;
- no paywall or purchase decision during Active Wake;
- subscription, billing, account, network or entitlement failure cannot prevent an already-committed local alarm from ringing;
- free users retain a real reliable base alarm;
- never exploit half-awake accidental taps for upsell;
- commerce state is never Alarm Kernel or WakeRuntime authority;
- no arbitrary feature wall may make the free alarm unsafe or intentionally ineffective.

## Working packaging hypothesis

### Free

A trustworthy WakeMyWay alarm that helps the user begin moving.

The free tier should preserve enough of the deterministic core to be a legitimate product and to demonstrate WakeMyWay's promise.

### Pro

The wake strategy becomes meaningfully more personal/adaptive over time, with premium experiences that support that outcome.

The final Pro boundary must be shaped from beta evidence. Do not pre-commit to gating a capability merely because it exists.

## Pricing hypothesis

Discovery-stage range:

~~~text
€4.99 / month
~€39 / year
~~~

This is not launch pricing until Gate 3 (#87) produces real willingness-to-pay evidence.

Evaluate price alongside actual package presentation, trial/start-checkout behavior, monthly vs annual preference, cancellation/refund feedback, perceived outcome value and recurring provider cost.

## Evidence before pricing lock

Before locking launch packaging and price, prove:

1. alarms are trusted;
2. WakeMyWay improves or meaningfully supports the intended real-world wake outcome for the target user;
3. users return voluntarily;
4. annoyance remains acceptable and the experience preserves agency;
5. users value the adaptive/conversational outcome rather than only the novelty;
6. a meaningful subset demonstrates real purchase intent when shown the actual package and price.

"Would you pay for this?" alone is not sufficient evidence.

## Billing boundary

Billing work belongs to the SELL gate (#88).

The paid Play build must safely handle the applicable purchase lifecycle: purchase/pending state, acknowledgement, entitlement refresh, restore, renewal, expiry/cancellation and reinstall/device replacement.

If entitlement state cannot be refreshed, WakeMyWay may degrade premium non-critical behavior conservatively, but it must never weaken a committed alarm.

Verify current Google Play billing/subscription requirements immediately before implementation/submission. Project documentation is not policy authority.

## Voice / variable-cost control

Define bounded provider budgets regardless of plan:

- maximum live session time;
- turn limits;
- retry limits;
- local fallback threshold;
- rate/cost guardrails.

Do not create a product where successful waking requires an unbounded inference bill.

## Launch relationship

The canonical sequencing and Definition of Done live in [`35-paid-launch-readiness.md`](35-paid-launch-readiness.md).

Billing may be prepared before the beta fully concludes, but public paid rollout and launch pricing wait for product-value and willingness-to-pay evidence.

# Wake policy and learning

Canonical terms are defined in [`../CONTEXT.md`](../CONTEXT.md).

## Separation of responsibilities

There are two deliberately different concerns:

### Wake Runtime

Owns deterministic in-session behavioral decisions. Activation evidence and escalation policy are internal to this deep module.

### Wake Learning

Runs after/between Wake Sessions and learns what future Wake Policy should look like.

This separation prevents a future ML/profile system from becoming a dependency of the alarm-critical morning path.

## Product priority

Wake Learning is not a late optimization. The adaptive loop is part of WMW's core product differentiation, so **Wake Learning v0 is implemented before realtime voice becomes central**.

The first question to prove is not "can an AI talk during an alarm?" It is:

> Can Wake My Way observe prior mornings and make a bounded, explainable change that improves the next one without increasing friction unnecessarily?

M7 exists specifically to answer that question using local deterministic behavior.

## V1: deterministic policy, not ML

Do not begin with machine learning. There is no proprietary dataset yet and the useful features/labels are still hypotheses.

A default Wake Policy may encode versioned parameters such as:

- how long to wait for first engagement
- when to request movement
- when to change tone/intervention
- how much evidence is needed before Orienting
- default snooze duration
- whether a second snooze is offered
- when relevant Tomorrow Contract context may be mentioned

Values are configuration owned by the Wake Runtime implementation/policy model, not scattered constants and not exposed as a wall of UI tuning sliders.

## Wake Learning v0 boundary

V0 is:

- local
- deterministic
- explainable
- bounded
- reversible
- versioned
- independent from network/cloud/AI

A simple conceptual update may look like:

```text
Recent evidence
- first engagement consistently fast
- meaningful movement consistently slow
- direct movement prompts correlate with better confirmed outcomes

Bounded next-policy change
- request movement 20 seconds earlier
- keep initial voice tone unchanged
- do not change activation threshold yet

Reason
- movement is the recurring bottleneck, not engagement
```

Do not build a generic rule engine. Implement the smallest cohesive updater that real dogfood data justifies.

## Activation evidence

The runtime can internally weight observations such as:

| Evidence | Initial hypothesis |
|---|---|
| meaningful voice/touch interaction | positive activation evidence |
| device pickup | positive |
| sustained movement | strongly positive |
| continued coherent interaction | positive |
| long stationary silence | negative/insufficient evidence |
| snooze request | evidence that activation has not succeeded |

Do not treat placeholder numeric weights in documentation as product truth. Establish them in dogfood and version every policy used in a Wake Session.

## Activation Completion vs Confirmed Wake Success

Learning must not optimize only for the runtime's own **Activation Completion** event.

The system could otherwise create a circular failure mode:

```text
lower/easier activation threshold
    ↓
more sessions count as completed
    ↓
metric appears better
    ↓
user may still return to bed
```

Use occasional calibration feedback and future privacy-safe proxies to estimate **Confirmed Wake Success**.

When evidence shows frequent false-positive Activation Completion, Wake Learning may eventually recommend/tune the future activation policy, but any such change must be bounded, versioned, explainable, and evaluated against annoyance/agency as well as success.

## Wake Outcome

Every completed attempt should derive a compact Wake Outcome from typed timeline facts, for example:

- seconds to first engagement
- seconds to meaningful movement
- whether Activation Completion occurred within the target window
- snooze count/durations
- maximum intervention depth
- fallback level(s)
- occasional calibration feedback when available

Do not store private transcript/calendar content merely to learn strategy effectiveness.

## Wake Profile

Profile information is **derived**, not authoritative source data.

```text
Wake Outcomes + selected safe session facts + feedback
                     |
                     v
                Wake Learning
                     |
                     v
          versioned profile/policy snapshot
```

Possible derived findings:

- movement requests are usually needed earlier
- direct language works better than generic motivation
- humor is useful only after engagement
- one short snooze is neutral/helpful but a second predicts failure
- important self-supplied context improves first engagement
- Activation Completion frequently overestimates real Wake Success for this policy

## Initial adaptation surface

Keep the first adaptation surface intentionally narrow. Candidates include:

- engagement timeout/timing
- timing of first sit/movement request
- intervention/escalation depth timing
- one-vs-second snooze offer behavior
- when Tomorrow Contract context is surfaced

Do **not** let v0 freely rewrite:

- safety rules
- stop/dismiss availability
- alarm delivery/audio behavior
- maximum provider cost
- privacy controls
- arbitrary character prompts

Critical reliability/safety boundaries are not learnable parameters.

## Bounded changes and hysteresis

Avoid policy oscillation from one strange morning.

Initial learning should require enough evidence/confidence before changing a parameter and should limit step size between policy versions.

Conceptually:

```text
one unusual morning
→ probably no policy change

repeated pattern across several relevant mornings
→ one small change
→ observe again
```

The exact sample thresholds are dogfood hypotheses, not frozen constants in this document.

## Explainability

Every meaningful learned policy change should be inspectable in debug/dogfood tooling.

Example:

```text
Policy 3 → Policy 4
Changed: movementPromptDelay 55s → 35s
Reason: 5/6 recent wakes engaged quickly but movement occurred after target window
Guardrail: annoyance feedback remained acceptable
```

User-facing explanations, if shown, should be simpler and never pretend to scientific certainty.

## Reset and fallback

A learned policy is derived data.

The user/developer must be able to reset to the stable default policy without deleting critical alarm configuration/history unnecessarily.

If a learned policy fails validation or cannot be decoded after an upgrade, fall back to the current safe default rather than blocking the alarm.

## Contextual policy

A future policy may vary by context, but avoid false precision before data exists.

Example hypothesis:

```text
weekday morning + historically slow engagement
-> request physical activation earlier
```

Another:

```text
important Tomorrow Contract exists
-> mention it during engagement rather than generic motivation
```

Context must be relevant, privacy-minimized, and explainable.

## Learning update cadence

Do not mutate behavior unpredictably in the middle of a wake session.

A Wake Session starts with a **policy version snapshot**. That policy remains stable for that session. Learning may produce a new policy only for later occurrences.

This makes replay and experiments interpretable.

## Wake Friction

Optimization target is not simply fastest activation.

Wake Learning should reason about outcome and intervention cost together:

> maximize real wake effectiveness while using the minimum effective friction compatible with acceptable annoyance and agency.

Track perceived agency and annoyance so learning cannot converge on "always be louder and more aggressive".

A useful evaluation tuple is:

```text
Confirmed Wake Success
+ activation delay
+ intervention depth
+ annoyance
+ perceived agency
```

## Future ML gate

Only consider ML after:

- enough real Wake Sessions exist
- labels/target window are stable and meaningful
- Confirmed Wake Success calibration is good enough to avoid circular optimization
- heuristic limitations are observable
- offline evaluation shows material lift over deterministic Wake Learning v0
- privacy and cost are acceptable
- the model has a safe deterministic fallback

If ML is introduced, it should generate/recommend future Wake Policy. It should not become direct in-session state authority.

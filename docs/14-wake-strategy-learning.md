# Wake policy and learning

Canonical terms are defined in [`../CONTEXT.md`](../CONTEXT.md).

## Separation of responsibilities

There are two deliberately different concerns:

### Wake Runtime

Owns deterministic in-session behavioral decisions. Activation evidence and escalation policy are internal to this deep module.

### Wake Learning

Runs after/between Wake Sessions and learns what future Wake Policy should look like.

This separation prevents a future ML/profile system from becoming a dependency of the alarm-critical morning path.

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

Values are configuration owned by the Wake Runtime implementation/policy model, not scattered constants and not exposed as UI tuning sliders.

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

## Wake Outcome

Every completed attempt should derive a compact Wake Outcome from typed timeline facts, for example:

- seconds to first engagement
- seconds to meaningful movement
- whether target wake window was met
- snooze count/durations
- maximum intervention depth
- fallback level(s)
- user feedback when available

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

Do not mutate behavior unpredictably in the middle of a wake session because a cloud learner changed.

A Wake Session starts with a **policy version snapshot**. That policy remains stable for that session. Learning may produce a new policy only for later occurrences.

This makes replay and experiments interpretable.

## Wake Friction

Optimization target is not simply fastest activation.

Wake Learning should reason about outcome and intervention cost together:

> minimize effective wake delay subject to an acceptable friction/annoyance constraint.

Track perceived agency and annoyance so learning cannot converge on "always be louder and more aggressive".

## Future ML gate

Only consider ML after:

- enough real Wake Sessions exist
- labels/target window are stable and meaningful
- heuristic limitations are observable
- offline evaluation shows material lift over deterministic policy
- privacy and cost are acceptable
- the model has a safe deterministic fallback

If ML is introduced, it should generate/recommend future Wake Policy. It should not become direct in-session state authority.

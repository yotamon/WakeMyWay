# Product development workflow

## Purpose

WakeMyWay should optimize for **validated product velocity**, not raw idea-to-code velocity.

AI makes production implementation unusually cheap and fast. That makes it more important, not less, to separate product discovery from production engineering. The workflow below prevents a plausible idea, attractive mockup, or detailed plan from silently becoming a production specification before the product decision has earned that commitment.

This is a lightweight operating model, not a ceremony system. Small fixes should stay small. The full flow applies when work changes user behavior, product scope, interaction design, durable product semantics, or a meaningful architectural boundary.

## The five modes

```text
EXPLORE -> SHAPE -> BUILD -> HARDEN -> LEARN
```

A feature may move backward when evidence exposes a wrong assumption. That is expected and cheaper than defending an implementation.

### 1. Explore

Question: **Is this worth solving, and what do we still not know?**

Explore the user problem before selecting the implementation.

Typical work:

- identify the user, trigger, problem, and desired outcome;
- inspect the current product behavior and alternatives;
- research platform, competitor, or ecosystem changes when relevant;
- state assumptions and failure cases;
- consider simpler alternatives and deletion/non-feature options;
- define what evidence would change the decision.

Production code, migrations, CI expansion, release infrastructure, and durable architecture are out of scope in Explore.

Throwaway sketches, mockups, scripts, or prototypes are allowed when they are clearly disposable and cheaper than reasoning in prose.

Explore output is a short product hypothesis:

```text
User:
Problem / trigger:
Desired outcome:
Current alternative:
Proposed value:
Critical assumptions:
Evidence / unknowns:
Non-goals:
Decision: explore further | shape | archive
```

### 2. Shape

Question: **What is the smallest coherent experience that would solve the accepted problem?**

Shape the real user experience before production architecture absorbs it.

For user-facing UI work, define content before canonical visual design:

```text
surface -> content inventory -> state matrix -> interaction -> prototype -> review
```

Do not approve a canonical mockup whose content model is still imaginary or incomplete.

Shape should define:

- one user-observable outcome;
- happy path;
- important empty, loading, error, permission, offline, and recovery states that actually apply;
- content and hierarchy;
- interaction semantics;
- accessibility consequences;
- product invariants that must remain true;
- explicit non-goals;
- success/evidence signal;
- a prototype or concrete interaction description when visual/interaction risk is meaningful.

Prototype fidelity and production fidelity are different. A high-fidelity prototype may be disposable. Do not harden a prototype merely because it looks polished.

### Definition of Ready

Meaningful product work may enter Build when these are clear enough to implement without inventing product behavior inside the PR:

- [ ] User/problem is clear.
- [ ] Desired user-observable outcome is clear.
- [ ] Main flow is clear.
- [ ] Important states and failure behavior are known.
- [ ] Non-goals are explicit.
- [ ] UX/content has been inspected or prototyped when interaction risk warrants it.
- [ ] Product/reliability invariants that may be affected are identified.
- [ ] Success or validation evidence is defined.

Not every checkbox needs a separate document. The contract can live in an issue, plan, accepted conversation summary, or existing canonical document. The point is decision quality, not paperwork.

If several material items are unknown, remain in Explore/Shape instead of letting the implementation invent answers.

### 3. Build

Question: **What is the smallest production slice that delivers the accepted outcome?**

Build is for implementation, not product discovery.

Prefer vertical slices with:

```text
one product outcome
+ one coherent responsibility
+ explicit acceptance evidence
```

A build agent may choose implementation details inside existing boundaries, but it must not silently expand product scope. Materially different product outcomes return to Shape.

Bias toward reviewable slices rather than entire product phases. A useful default is work that a human engineer could understand and review as one coherent change, not an arbitrary maximum line count.

Before opening a PR, verify the actual integrated user path locally when practical. Do not use deployment infrastructure as the first place to discover basic product behavior.

### 4. Harden

Question: **Is the accepted behavior production-worthy?**

This is where WakeMyWay's existing no-compromise engineering discipline belongs.

Use the validation depth appropriate to the risk:

- tests and static analysis;
- accessibility and responsive behavior;
- visual regression for canonical states;
- migration/backward compatibility;
- privacy/security boundaries;
- Alarm Kernel and Active Wake reliability gates;
- physical-device evidence where emulator confidence is insufficient;
- release/deployment verification when the change actually affects those systems.

Do not widen product scope during hardening. New product ideas go back to Explore.

### 5. Learn

Question: **What did reality teach us?**

After meaningful dogfood, release, or experiment evidence, compare expectation with observation:

```text
What did we expect?
What happened?
What surprised us?
What confused users?
What should be kept, changed, removed, or tested next?
Which assumption changed?
```

Update canonical product/status/decision documents when the evidence changes durable understanding. Do not turn every observation into a feature.

## Product review before code review

For user-facing work, perform a product review before treating engineering correctness as sufficient.

Review the running experience or representative prototype without relying on source-code knowledge:

- Is the purpose immediately understandable?
- Is the primary action obvious?
- Can a first-time user complete the intended task?
- Does any surface expose developer/system complexity unnecessarily?
- Is the behavior truthful, especially for account, cloud, permissions, readiness, and health/insight claims?
- Can anything be removed while preserving the outcome?
- Does the experience still match the accepted Shape contract?

A technically excellent implementation that fails this review returns to Shape or Build before merge.

## Agent modes

When using coding agents, keep the mode explicit.

### Product critic / Explore

Do not implement production code. Challenge assumptions, scope, necessity, alternatives, and evidence.

### Product designer / Shape

Define flows, content, states, interaction, and prototype behavior. Do not invent production architecture unless a constraint must be surfaced for feasibility.

### Builder / Build

Implement the accepted slice. Do not add adjacent features or resolve product ambiguity by invention.

### Reviewer / Harden

Assume the implementation may be wrong. Check contract fidelity, regressions, over-engineering, safety, accessibility, reliability, and missing evidence.

## WIP discipline

AI can execute many tasks in parallel; product judgment cannot.

For WakeMyWay, prefer one primary product Build stream at a time. A second feature may be in Explore/Shape, but avoid multiple simultaneous broad Build phases that compete for product review and create integration churn.

Maintenance, isolated defects, and reliability fixes may proceed independently when they do not create conflicting product decisions.

## Canonical documentation mapping

Do not create duplicate product truth.

- `CONTEXT.md` owns canonical domain terms and product/architecture invariants.
- `docs/00-project-status.md` owns current implemented state and active work.
- `docs/01-product-vision.md` and `docs/03-product-principles.md` own durable product purpose and constraints.
- `docs/05-ux-flows.md` and `docs/07-design-system.md` own accepted UX/design direction.
- `docs/26-decisions-log.md` records durable product decisions/reversals.
- `docs/27-open-questions.md` records deliberately unresolved questions.
- ADRs own durable technical decisions.
- this document owns the process used to move product ideas into production work.

Feature-specific Shape artifacts should be temporary or live with the issue/PR unless they represent durable product truth worth promoting into canonical docs.

## Scope rule for small work

Do not force Explore/Shape ceremony onto trivial work.

Typos, dependency maintenance, isolated bug fixes with known expected behavior, test repairs, mechanical refactors, and narrowly scoped reliability fixes may enter Build directly when the desired behavior is already canonical and unambiguous.

The test is simple:

> Are we deciding what the product should do, or merely making the product do what it already clearly should?

If the former, Explore/Shape first. If the latter, Build.

# Documentation workflow

## Purpose

Documentation is persistent project memory for a human or coding agent that does not have the original product conversation.

## Sources of truth

Use this precedence:

1. **`CONTEXT.md`** — canonical domain terms/invariants
2. **accepted ADRs** — durable technical decisions
3. **`docs/00-project-status.md`** — what is actually implemented/currently active
4. topic docs — detailed product/architecture/design plans
5. backlog — planned work, not architecture authority

`docs/34-product-development-workflow.md` defines **how** new product ideas move from uncertainty into production. It does not replace any product source of truth above.

If two sources conflict, fix the conflict rather than choosing whichever is convenient.

## Before product implementation work

If the task introduces or materially changes user behavior, product scope, interaction design, or durable product semantics, start with `docs/34-product-development-workflow.md` and determine whether the work is still in Explore/Shape or is actually ready for Build.

Do not use production implementation to resolve material product uncertainty that belongs in Explore or Shape.

Once the work is ready for Build, read only the context needed for that slice:

1. `CONTEXT.md`
2. `AGENTS.md`
3. `docs/00-project-status.md`
4. relevant topic docs/ADRs
5. active milestone in `docs/21-roadmap-implementation-plan.md` when the change is milestone-related

Small maintenance, known bug fixes, test repairs, or mechanical refactors with unambiguous expected behavior may enter Build directly.

## After meaningful implementation work

### Update `docs/00-project-status.md`

Record:

- implemented + tested behavior
- active milestone
- blockers/known issues
- exact next task

### Update `CONTEXT.md` when terminology/invariants change

Do this in the same change. Do not introduce synonyms for core domain concepts casually.

### Update topic document

When implementation invalidates a plan/hypothesis, rewrite the plan to reflect reality. Do not preserve stale aspirational architecture as if it were current.

### Update `docs/26-decisions-log.md`

Record durable product decisions/reversals.

### Update `docs/27-open-questions.md`

Use this when evidence reveals an important unresolved product question. Do not let the implementation silently decide it.

### Add/update ADR

Use an ADR for technical choices that materially constrain future implementation, provider/platform shape, critical reliability, storage/security, or module architecture.

### Complete the Learn loop when evidence warrants it

After meaningful dogfood, release, usability, or device evidence, compare expectation with observation using `docs/34-product-development-workflow.md`. Promote only durable learning into canonical docs; do not create a permanent document for every experiment or conversation.

## ADR lifecycle

Statuses:

- Proposed
- Accepted
- Superseded
- Rejected

Never erase superseded rationale. Link the replacement.

## Deep-module review rule

Before introducing a new public abstraction/module/interface, document the concrete seam it protects. A future platform alone is not evidence.

Useful prompts:

- What decisions does this module hide?
- What does the caller no longer need to know?
- Is there a second implementation/external boundary/failure domain/test seam now?
- Would behavior-preserving inlining make the caller substantially worse?

## Status vocabulary

Use explicit labels where ambiguity matters:

- **Decided** — accepted product/architecture choice
- **Planned** — intended but not implemented
- **Implemented** — exists in code
- **Tested** — demonstrated by specified tests/devices
- **Hypothesis** — needs dogfood/data
- **Deferred** — intentionally excluded for now

## Link validation

`tooling/check_docs.py` validates local Markdown links. Run it in CI and after reorganizing docs.

## Avoid documentation drift

Do not create a second glossary, second roadmap, duplicate product contract, or duplicate canonical state-machine description. Link to the authoritative source and add local detail only where needed.

Feature-specific Explore/Shape material should normally live with the issue, PR, or working conversation unless it becomes durable product truth. This keeps prototypes and hypotheses cheap to discard.

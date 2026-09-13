# AGENTS.md — Wake My Way agent guidance

This file is the compact router for coding agents working in this repository. Preserve the product and reliability invariants below, then load deeper documentation only when the task actually touches that area.

Do **not** read the entire documentation set before every change. Inspect the affected code first, follow nearby references, and pull in the relevant canonical docs from the router below.

## Core invariants

These are repository-wide truths. Do not violate them even when a task appears local.

- Android is the only client/runtime product target today. Do not introduce React Native, Flutter, KMP, or speculative iOS-shaped abstractions without an explicit architecture decision.
- The **Alarm Kernel** owns the trust-critical path from an accepted Wake Occurrence through exact scheduling, durable recovery, active alarm execution, Stop/Snooze, and safe fallback.
- Critical waking must remain locally actionable without network, cloud, AI, analytics, generated content, or normal UI availability.
- `:wake-core` is pure Kotlin and must not depend on Android, Compose, Room, networking, voice providers, analytics, or cloud SDKs.
- Active alarm playback must not depend on `WakeActivity` lifetime. Component recreation or stale triggers must not duplicate or resurrect terminal alarm execution.
- The **Wake Runtime** deterministically owns in-session behavioral decisions. AI may render approved speech intent; it must not decide phase transitions, activation completion, dismissal, snooze acceptance, or facts.
- Wake Learning is off-session and must not become a dependency of the current alarm firing. Initial learning remains local, bounded, explainable, versioned, and reversible.
- Keep one active adaptive Wake Schedule in V1. A dogfood Safety Backup is not a second adaptive schedule and must not force generic multi-alarm architecture.
- Never embed server/operator secrets in the Android app. Keep private wake content, transcripts, raw microphone audio, prompts, and sensitive learning data out of crash/analytics logs.
- Prefer deep modules with small contracts. Add seams when they hide a real policy, runtime/provider boundary, failure domain, or deterministic-test need, not merely for theoretical flexibility.
- Preserve the product boundary: Wake My Way is a waking product, not a task manager, general assistant, sleep tracker, journaling app, or news reader.

Canonical domain language lives in [`CONTEXT.md`](CONTEXT.md). If a task changes the meaning of a domain concept, use that file as the source of truth.

## Context router

Read only the rows that apply to the current task.

| Task area | Canonical context |
| --- | --- |
| Domain terminology or product semantics | `CONTEXT.md` |
| Current milestone, implementation state, or next planned work | `docs/00-project-status.md`, relevant milestone in `docs/21-roadmap-implementation-plan.md` |
| Product scope or behavioral constraints | `docs/03-product-principles.md` |
| Android module boundaries | `docs/08-android-architecture.md` |
| Exact alarm scheduling, reliability, Direct Boot, Stop/Snooze, active execution | `docs/10-alarm-kernel.md`, `docs/adr/014-active-wake-execution-lifecycle.md` |
| Wake-session state transitions and deterministic behavior | `docs/11-wake-runtime-state-machine.md` |
| Personalization / learning | `docs/14-wake-strategy-learning.md` |
| UX, flows, or visual design | `docs/04-ux-psychology.md`, `docs/05-ux-flows.md`, `docs/07-design-system.md` |
| Testing, release, deployment, or device validation | `docs/32-testing-and-deployment-topology.md` |
| Cloud AI | `docs/adr/016-vercel-ai-platform.md`, `docs/implementation/vercel-ai-platform.md` |
| Durable architecture decision | relevant file under `docs/adr/` |

When a referenced document points to a more specific contract, follow that reference only if the task needs it.

## Working style

Understand the affected flow before editing it, but do not map the whole repository for a small local change. Reuse established patterns and keep implementations local until a real boundary justifies an abstraction.

Proceed autonomously with ordinary local development work: inspect files, edit code, run local tests, lint, builds, static analysis, and safe local tooling. Do not ask for approval between normal implementation and verification steps.

Stop and ask only when the next step would require a destructive or irreversible operation, production data/infrastructure changes, credentials or billing, an external side effect that is not clearly intended, or a genuine product decision with materially different outcomes.

## Validation and completion

For implementation tasks, continue through:

```text
implementation -> relevant validation -> inspect failures -> fix regressions -> rerun affected checks
```

Do not stop after the first plausible implementation merely to request review.

Use the smallest validation set that gives confidence in the affected behavior. Do not run the entire test matrix for a typo, documentation-only edit, or isolated cosmetic change unless there is a concrete reason. Reliability-critical alarm changes require the stronger validation described in the relevant alarm/testing docs.

A task is complete when the requested behavior is implemented, affected behavior is validated, regressions caused by the change are fixed, and relevant documentation is synchronized.

## Documentation updates

Update documentation proportionally to the change:

- After meaningful implementation/milestone work, update `docs/00-project-status.md` with what changed, validation, risks, and the next task.
- If canonical terminology or meaning changes, update `CONTEXT.md` in the same change.
- If a durable technical decision changes, add or update an ADR.
- If a durable product decision changes, update `docs/26-decisions-log.md`.
- Do not churn roadmap/status documents for trivial formatting, typo, or purely mechanical edits.

# Wake My Way documentation

The repository documentation is the persistent product/engineering memory for Wake My Way.

**Canonical domain language lives at [`../CONTEXT.md`](../CONTEXT.md).** If terminology conflicts with another document, `CONTEXT.md` wins and the conflicting document should be corrected.

## Start by role

### Any coding agent/developer

1. [`../CONTEXT.md`](../CONTEXT.md)
2. [`../AGENTS.md`](../AGENTS.md)
3. [`00-project-status.md`](00-project-status.md)
4. relevant topic document
5. active milestone in [`21-roadmap-implementation-plan.md`](21-roadmap-implementation-plan.md)

### Alarm/reliability work

Read `08`, `10`, `17`, `18`, then ADRs `002`, `009`, `011`, **`014`**.

### Wake behavior / learning work

Read `04`, `05`, `11`, `12`, `14`, `22`, then ADRs `003`, `005`.

### UI/brand work

Read `03`, `04`, `05`, `06`, `07`.

## Living context

| Document | Purpose |
|---|---|
| [`../CONTEXT.md`](../CONTEXT.md) | **Canonical product/domain language and invariants** |
| [`00-project-status.md`](00-project-status.md) | What exists now, active milestone, blockers, next exact work |
| [`26-decisions-log.md`](26-decisions-log.md) | Chronological durable decisions/reversals |
| [`27-open-questions.md`](27-open-questions.md) | Deliberately unresolved questions |
| [`30-documentation-workflow.md`](30-documentation-workflow.md) | How documentation stays current |
| [`31-pre-implementation-architecture-review.md`](31-pre-implementation-architecture-review.md) | Deep-module simplification review before M0 |
| [`32-testing-and-deployment-topology.md`](32-testing-and-deployment-topology.md) | Canonical Wake Lab → device matrix → Play dogfood path and Vercel cloud boundary |
| [`33-plan-hardening-review.md`](33-plan-hardening-review.md) | Final pre-M0 risk review: active alarm lifecycle, early learning, calibrated success, Android baseline |

## Product

| Document | Purpose |
|---|---|
| [`01-product-vision.md`](01-product-vision.md) | Vision, problem, thesis, V1 scope |
| [`02-market-and-positioning.md`](02-market-and-positioning.md) | Competition/opportunity/positioning |
| [`03-product-principles.md`](03-product-principles.md) | Non-negotiable product constraints |
| [`22-product-metrics-experiments.md`](22-product-metrics-experiments.md) | Activation Completion vs Confirmed Wake Success, friction, experiments |
| [`24-monetization.md`](24-monetization.md) | Monetization guardrails |

## UX and brand

| Document | Purpose |
|---|---|
| [`04-ux-psychology.md`](04-ux-psychology.md) | Sleep inertia and behavioral design principles |
| [`05-ux-flows.md`](05-ux-flows.md) | Night/morning flows, intentional Stop, calibration, Safety Backup hypothesis |
| [`06-brand.md`](06-brand.md) | Wake My Way brand platform/voice |
| [`07-design-system.md`](07-design-system.md) | Visual/motion/haptic/audio/UI direction |
| [`brand/assets/wake-my-way-brand-board.png`](brand/assets/wake-my-way-brand-board.png) | Initial identity exploration |

## Architecture

| Document | Purpose |
|---|---|
| [`08-android-architecture.md`](08-android-architecture.md) | Deep-module Android architecture and minimal module topology |
| [`09-stack-and-dependencies.md`](09-stack-and-dependencies.md) | Android 16 baseline, adopt-by-milestone dependency strategy |
| [`10-alarm-kernel.md`](10-alarm-kernel.md) | Deep Alarm Kernel contract / scheduling / Active Wake Execution / Direct Boot |
| [`11-wake-runtime-state-machine.md`](11-wake-runtime-state-machine.md) | Deep deterministic Wake Runtime |
| [`12-data-model.md`](12-data-model.md) | Local source-of-truth model / active execution / calibrated outcomes / critical snapshot |
| [`13-ai-voice-character-system.md`](13-ai-voice-character-system.md) | Speech Intent, character, M8 measured voice spike |
| [`14-wake-strategy-learning.md`](14-wake-strategy-learning.md) | M7 local deterministic Wake Learning v0 / Wake Policy derivation |
| [`15-context-calendar-weather.md`](15-context-calendar-weather.md) | Optional context constraints |
| [`20-api-backend.md`](20-api-backend.md) | Deferred backend/API direction; local M7 does not require cloud |
| [`23-ios-future.md`](23-ios-future.md) | Future iOS without speculative seams |

## Safety, quality, operations

| Document | Purpose |
|---|---|
| [`16-privacy-security.md`](16-privacy-security.md) | Privacy/storage classes/security |
| [`17-reliability-failure-modes.md`](17-reliability-failure-modes.md) | Delivery + active-execution reliability envelope/graceful degradation |
| [`18-testing-quality.md`](18-testing-quality.md) | Contract tests, active-alarm recovery, Direct Boot/Force Stop/device testing |
| [`19-observability-analytics.md`](19-observability-analytics.md) | Safe semantic telemetry / lifecycle reliability / calibrated outcome metrics |
| [`25-open-source-and-licenses.md`](25-open-source-and-licenses.md) | Dependency/model licensing policy |
| [`32-testing-and-deployment-topology.md`](32-testing-and-deployment-topology.md) | Testing ladder, Firebase/Play distribution, local M7, Vercel cloud topology |

## Delivery

| Document | Purpose |
|---|---|
| [`21-roadmap-implementation-plan.md`](21-roadmap-implementation-plan.md) | Evidence-driven M0–M12 build order with M7 learning before M8 voice |
| [`implementation/backlog.md`](implementation/backlog.md) | Seed engineering backlog aligned to the current build order |

## Research

| Document | Purpose |
|---|---|
| [`research/competitors.md`](research/competitors.md) | Competitor research snapshot |
| [`research/wake-psychology.md`](research/wake-psychology.md) | Wake psychology research notes |
| [`research/android-alarm-platform.md`](research/android-alarm-platform.md) | Time-sensitive Play target API / exact alarm / Direct Boot / Force Stop / Android 17 audio facts |

## ADRs

[`adr/`](adr/) records technical decisions that should not be silently reversed. Product terminology belongs in `CONTEXT.md`; implementation status belongs in `00-project-status.md`.

Key alarm ADRs currently include:

- ADR-002: local-first Alarm Kernel authority
- ADR-009: deep Alarm Kernel boundary
- ADR-011: Direct Boot critical-state privacy split
- **ADR-014: Active Wake Execution survives UI/process churn**

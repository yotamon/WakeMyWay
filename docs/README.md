# Wake My Way documentation

The repository documentation is the persistent product/engineering memory for Wake My Way.

**Canonical domain language lives at [`../CONTEXT.md`](../CONTEXT.md).** If terminology conflicts with another document, `CONTEXT.md` wins and the conflicting document should be corrected.

## Start by role

### Any coding agent/developer

1. [`../CONTEXT.md`](../CONTEXT.md)
2. [`../AGENTS.md`](../AGENTS.md)
3. [`00-project-status.md`](00-project-status.md)
4. [`35-paid-launch-readiness.md`](35-paid-launch-readiness.md) while the 1.0 program is active
5. relevant topic document
6. historical/build milestone context in [`21-roadmap-implementation-plan.md`](21-roadmap-implementation-plan.md)

### New product behavior / feature shaping

Start with [`34-product-development-workflow.md`](34-product-development-workflow.md). Determine whether the work is in Explore, Shape, Build, Harden, or Learn before production implementation. Then read the relevant product/UX documents below.

### Alarm/reliability work

Read `08`, `10`, `17`, `18`, [`implementation/m2-reliability-harness.md`](implementation/m2-reliability-harness.md), then ADRs `002`, `009`, `011`, `014`, `015`, `019`, and `022` as relevant.

### Wake behavior / learning work

Read `04`, `05`, `11`, `12`, `14`, `22`, then ADRs `003`, `005`.

### UI/brand work

Read `34` first for new product behavior, then `03`, `04`, `05`, `06`, `07`.

## Living context

| Document | Purpose |
|---|---|
| [`../CONTEXT.md`](../CONTEXT.md) | **Canonical product/domain language and invariants** |
| [`00-project-status.md`](00-project-status.md) | What exists now, active milestone, blockers, next exact work |
| [`26-decisions-log.md`](26-decisions-log.md) | Chronological durable decisions/reversals |
| [`27-open-questions.md`](27-open-questions.md) | Deliberately unresolved questions |
| [`30-documentation-workflow.md`](30-documentation-workflow.md) | How documentation stays current |
| [`34-product-development-workflow.md`](34-product-development-workflow.md) | Canonical Explore → Shape → Build → Harden → Learn workflow and Definition of Ready |
| [`35-paid-launch-readiness.md`](35-paid-launch-readiness.md) | **Active 1.0 Trust → Finish → Prove → Sell program, scope freeze, launch gates and Definition of Done** |
| [`36-beta-validation-runbook.md`](36-beta-validation-runbook.md) | **Executable Gate 3 screener, denominators, evidence cadence, change-control and willingness-to-pay interview** |
| [`37-physical-acceptance.md`](37-physical-acceptance.md) | **Unified physical Gate 1/2 acceptance for reliability, startup, TalkBack and reduced motion** |
| [`38-play-submission-packet.md`](38-play-submission-packet.md) | **Google Play submission copy, declarations, Data Safety baseline and staged-rollout packet** |
| [`39-public-privacy-policy.md`](39-public-privacy-policy.md) | **Publication-ready privacy-policy content with explicit release blockers for contact/provider finalization** |
| [`40-terms-of-use-draft.md`](40-terms-of-use-draft.md) | **Commercial terms draft aligned to the product boundary; entity/jurisdiction fields remain external** |
| [`41-support-and-incident-response.md`](41-support-and-incident-response.md) | **Support taxonomy, privacy-safe diagnostics, rollout pause/incident rules and first-week operations** |
| [`31-pre-implementation-architecture-review.md`](31-pre-implementation-architecture-review.md) | Historical pre-M0 deep-module simplification review; later decisions may supersede details |
| [`32-testing-and-deployment-topology.md`](32-testing-and-deployment-topology.md) | Canonical Wake Lab → device matrix → Play dogfood path and Vercel cloud boundary |
| [`33-plan-hardening-review.md`](33-plan-hardening-review.md) | Historical final pre-M0 risk review; later accepted ADRs and current context supersede changed assumptions |

## Product

| Document | Purpose |
|---|---|
| [`01-product-vision.md`](01-product-vision.md) | Vision, problem, thesis, current V1 scope |
| [`02-market-and-positioning.md`](02-market-and-positioning.md) | Competition/opportunity/positioning |
| [`03-product-principles.md`](03-product-principles.md) | Non-negotiable product constraints |
| [`22-product-metrics-experiments.md`](22-product-metrics-experiments.md) | Activation Completion vs Confirmed Wake Success, friction, experiments |
| [`24-monetization.md`](24-monetization.md) | Outcome-led Free/Pro packaging, pricing hypothesis and monetization guardrails |
| [`35-paid-launch-readiness.md`](35-paid-launch-readiness.md) | Paid-launch gates, beta evidence, rollout ladder and 1.0 acceptance |
| [`36-beta-validation-runbook.md`](36-beta-validation-runbook.md) | Gate 3 beta execution protocol and evidence definitions |

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
| [`09-stack-and-dependencies.md`](09-stack-and-dependencies.md) | Adopt-by-milestone dependency strategy |
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
| [`37-physical-acceptance.md`](37-physical-acceptance.md) | Physical founder-device acceptance and retained evidence procedure |
| [`implementation/m2-reliability-harness.md`](implementation/m2-reliability-harness.md) | Current M2 scenario matrix, evidence fields, instrumentation tests and exit gate |

## Delivery

| Document | Purpose |
|---|---|
| [`21-roadmap-implementation-plan.md`](21-roadmap-implementation-plan.md) | Evidence-driven M0–M12 build order with M7 learning before M8 voice |
| [`implementation/backlog.md`](implementation/backlog.md) | Seed engineering backlog aligned to the current build order |
| [`implementation/m1-progress.md`](implementation/m1-progress.md) | M1 implementation record |
| [`implementation/m2-reliability-harness.md`](implementation/m2-reliability-harness.md) | M2 reliability runbook and evidence gates |

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
- ADR-014: Active Wake Execution survives UI/process churn
- ADR-015: exact-alarm, full-screen and foreground execution policy
- ADR-019: separate scheduling, active execution, voice and Snooze readiness semantics
- ADR-022: independent multi-alarm product model and kernel migration

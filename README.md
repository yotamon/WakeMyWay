# Wake My Way

**Wake My Way (WMW)** is an adaptive conversational alarm for Android that learns the least aggressive, most effective way to move a person from sleep inertia toward meaningful activity and the start of their morning.

> **Wake up your way.**
>
> An alarm that learns what works for you.

## Product thesis

Traditional alarms know **when** to make noise. Wake My Way learns **how** to help a particular person actually start moving.

People respond differently to sound, conversation, humor, self-supplied context, movement, urgency, and snooze. WMW combines a reliable native Android Alarm Kernel with a deterministic Wake Runtime, physical activation signals, local explainable Wake Learning, conversational characters, and gradual personalization.

The product is deliberately narrow:

> **I set an alarm because I genuinely want to get moving at this time. Help me succeed.**

It is not a general productivity assistant, sleep tracker, or AI companion.

## Current status

The repository is in **pre-implementation architecture + plan-hardening complete** state. No application code has been started yet.

Two documentation reviews are complete:

1. a deep-module architecture review removed speculative seams/modules before code existed
2. a plan-hardening review closed the remaining high-risk gaps around active alarm lifecycle, adaptive-learning sequence, outcome measurement, and current Android platform baseline

Current decisions include:

- Product concept and differentiation: defined
- UX psychology and V1 flow: defined
- Brand: **Wake My Way / WMW**
- Android-first native strategy: accepted
- Canonical domain context: [`CONTEXT.md`](CONTEXT.md)
- Deep Alarm Kernel contract: scheduling **plus Active Wake Execution**
- `WakeActivity` is presentation, not critical playback lifetime authority
- Deterministic Wake Runtime: defined
- `Activation Completion` separated from calibrated `Confirmed Wake Success`
- Deterministic local **Wake Learning v0 is M7**, before realtime voice
- Direct Boot / Force Stop reliability envelope: defined
- Initial physical module topology: `:app`, `:wake-core`, `:benchmark`
- V1 schedule cardinality: one active adaptive Wake Schedule
- Android M0 baseline: target API 36; preferred exact-alarm manifest direction is `USE_EXACT_ALARM`, subject to current Play revalidation
- Non-critical cloud direction: **Vercel compute + Supabase managed PostgreSQL**, introduced only when a real cloud feature exists
- Realtime voice architecture spike: M8, after local adaptation is proven
- Next engineering work: **M0 Foundation → M1 Deep Alarm Kernel + Active Wake Execution**

See [`docs/00-project-status.md`](docs/00-project-status.md) for the living status.

## Start here

1. [`CONTEXT.md`](CONTEXT.md) — canonical terms, product invariants, reliability language
2. [`AGENTS.md`](AGENTS.md) — engineering rules for humans/agents
3. [`docs/README.md`](docs/README.md) — documentation map
4. [`docs/00-project-status.md`](docs/00-project-status.md) — current state and next task
5. [`docs/01-product-vision.md`](docs/01-product-vision.md) — product thesis
6. [`docs/04-ux-psychology.md`](docs/04-ux-psychology.md) — behavioral design rationale
7. [`docs/08-android-architecture.md`](docs/08-android-architecture.md) — architecture
8. [`docs/10-alarm-kernel.md`](docs/10-alarm-kernel.md) — trust-critical scheduling + active wake execution
9. [`docs/adr/014-active-wake-execution-lifecycle.md`](docs/adr/014-active-wake-execution-lifecycle.md) — active playback/recovery decision
10. [`docs/14-wake-strategy-learning.md`](docs/14-wake-strategy-learning.md) — local explainable adaptation
11. [`docs/21-roadmap-implementation-plan.md`](docs/21-roadmap-implementation-plan.md) — build order
12. [`docs/32-testing-and-deployment-topology.md`](docs/32-testing-and-deployment-topology.md) — how WMW is tested, dogfooded and where cloud workloads run

## Non-negotiable engineering principles

1. **Android is our first client, not our product architecture.**
2. **Intelligence may fail. The alarm may not, inside the documented platform reliability envelope.**
3. **Alarm reliability continues after the OS trigger; critical playback must survive ordinary UI lifecycle churn.**
4. **AI expresses wake behavior. It never owns wake behavior.**
5. **Critical wake behavior and initial learning are local-first.**
6. **Deep modules beat forests of speculative interfaces.**
7. **Behavioral activation is observable; biological wakefulness is not claimed.**
8. **Activation Completion is not circular proof of real Wake Success.**
9. **User dignity and agency are product requirements.**

## Planned implementation shape

The first codebase deliberately starts small:

```text
wake-my-way/
├── apps/
│   └── android/
│       ├── app/          # Android composition root, UI, Alarm Kernel implementation
│       ├── wake-core/    # pure Kotlin recurrence/runtime/policy/learning behavior
│       └── benchmark/    # startup / wake-path performance
├── docs/
├── tooling/
├── AGENTS.md
├── CONTEXT.md
└── README.md
```

Backend/service directories are created only when a cloud capability is actually implemented. OpenAPI remains the chosen cross-platform contract once that boundary exists.

## Brand

**Name:** Wake My Way  
**Short mark:** WMW  
**Primary tagline:** Wake up your way.  
**Secondary line:** An alarm that learns what works for you.

`wakemyway.com` was verified as available via a live Namecheap lookup on 2026-09-09. Availability is time-sensitive; the repository does not imply registration.

## Documentation discipline

Documentation is project memory, not aspirational marketing. Changes must distinguish **Decided**, **Planned**, **Implemented**, **Tested**, and **Hypothesis**.

Canonical terminology lives in [`CONTEXT.md`](CONTEXT.md). Durable engineering decisions live under [`docs/adr/`](docs/adr/). Current implementation truth lives in [`docs/00-project-status.md`](docs/00-project-status.md).

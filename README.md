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

Wake My Way is in **active native Android development**.

Merged into `main`:

- **M0 Foundation** — Android project, pure schedule domain, recurrence/DST tests and CI baseline
- **M1 Deep Alarm Kernel + Active Wake Execution** — exact local alarm registration, durable Direct-Boot critical state, foreground alarm playback, stop/snooze/reconciliation
- **M2 automated reliability harness** — Wake Alarm Lab, timing evidence, instrumentation coverage and dedicated emulator lane; physical-device proof remains open
- **M3 deterministic Wake Runtime** — typed inputs/directives, activation evidence, escalation and replayable session behavior
- **M4 Motion Evidence** — bounded local sensor evidence extraction without raw sensor persistence
- **M5 Alfred local character** — deterministic curated character rendering plus offline-only local Android TTS lab
- **M6 Tomorrow Contract + Prepared Wake Plan** — private night-before context, deterministic local preparation and unlocked-only morning enrichment

**M7 Wake Learning v0 is currently in review in PR #22.** It introduces privacy-minimized Wake Outcomes and a deterministic local learning loop that may adjust only future movement-prompt timing after repeated evidence, with explicit calibration, annoyance and agency guardrails.

The main unresolved trust boundary is physical-device reliability evidence. CI/emulator success does not prove real overnight behavior across locked screens, Doze, reboot-before-unlock, OEM power management or audio coexistence.

Current architectural decisions include:

- Canonical domain context: [`CONTEXT.md`](CONTEXT.md)
- Native Android first: Kotlin + Jetpack Compose
- Deep Alarm Kernel owns scheduling **and Active Wake Execution**
- `WakeActivity` is presentation, not critical playback lifetime authority
- deterministic pure-Kotlin Wake Runtime owns in-session behavioral decisions
- one Wake Session is pinned to one immutable Wake Policy version
- `Activation Completion` is distinct from calibrated `Confirmed Wake Success`
- unknown calibration is never treated as Wake Success
- motion/character/prepared/learned personalization remain subordinate to alarm reliability
- Direct Boot stores only a minimal non-sensitive Critical Wake Snapshot
- Tomorrow Contract, prepared private content and Wake Learning state remain credential-protected
- Wake Learning v0 is local, deterministic, explainable, bounded and reversible
- realtime voice architecture is a measured M8 spike, not a current dependency
- non-critical future cloud direction remains **Vercel + Supabase**, outside wake authority

See [`docs/00-project-status.md`](docs/00-project-status.md) for the living implementation truth.

## Start here

1. [`CONTEXT.md`](CONTEXT.md) — canonical terms, product invariants, reliability language
2. [`AGENTS.md`](AGENTS.md) — engineering rules for humans/agents
3. [`docs/README.md`](docs/README.md) — documentation map
4. [`docs/00-project-status.md`](docs/00-project-status.md) — current state and exact next work
5. [`docs/01-product-vision.md`](docs/01-product-vision.md) — product thesis
6. [`docs/04-ux-psychology.md`](docs/04-ux-psychology.md) — behavioral design rationale
7. [`docs/08-android-architecture.md`](docs/08-android-architecture.md) — architecture
8. [`docs/10-alarm-kernel.md`](docs/10-alarm-kernel.md) — trust-critical scheduling + active wake execution
9. [`docs/11-wake-runtime-state-machine.md`](docs/11-wake-runtime-state-machine.md) — deterministic in-session behavior
10. [`docs/14-wake-strategy-learning.md`](docs/14-wake-strategy-learning.md) — local explainable adaptation
11. [`docs/implementation/m6-tomorrow-contract.md`](docs/implementation/m6-tomorrow-contract.md) — merged private preparation implementation
12. [`docs/implementation/m7-wake-learning-v0.md`](docs/implementation/m7-wake-learning-v0.md) — current M7 learning implementation
13. [`docs/21-roadmap-implementation-plan.md`](docs/21-roadmap-implementation-plan.md) — evidence-driven build order
14. [`docs/32-testing-and-deployment-topology.md`](docs/32-testing-and-deployment-topology.md) — testing, dogfood and future cloud topology

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

## Current implementation shape

The physical codebase deliberately remains small:

```text
wake-my-way/
├── apps/
│   └── android/
│       ├── app/          # Android composition root, UI, Alarm Kernel + platform adapters
│       ├── wake-core/    # pure Kotlin schedule/runtime/motion/character/preparation/learning behavior
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

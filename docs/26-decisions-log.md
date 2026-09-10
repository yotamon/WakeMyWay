# Decisions log

This is the chronological product/engineering decision history. ADRs contain deeper technical rationale. Canonical terminology lives in [`../CONTEXT.md`](../CONTEXT.md).

## 2026-09-09 — Product concept

Build an alarm that does more than ring: it can talk, use relevant context, remind the user why they wanted to get up, and vary personality.

## 2026-09-09 — Product differentiation

"AI talking alarm" is insufficient differentiation. Core differentiation is an adaptive wake strategy that learns what actually helps the individual become active.

## 2026-09-09 — Minimum effective friction

Combine voice, context, movement, snooze policy, and escalation while optimizing for the least aggressive intervention that reliably works.

## 2026-09-09 — Tomorrow Contract

Night-before intention is a core product mechanic: past-you can give morning-you a reason and a First Move.

## 2026-09-09 — Deterministic behavior

Wake behavior is deterministic. AI renders constrained language but never owns behavioral state.

## 2026-09-09 — Platform scope

Initial Android+iOS thought was narrowed to **Android only now**, with a future iOS path preserved conceptually.

## 2026-09-09 — Native Android

Use Kotlin + Jetpack Compose, not React Native/Flutter, because alarm scheduling, presentation, audio, microphone lifecycle, sensors, and permissions are product-critical.

## 2026-09-09 — KMP deferred

Do not use KMP now. Reassess if/when iOS is real and shared pure-domain code has measurable value.

## 2026-09-09 — Local-first alarm authority

Alarm delivery is independent of cloud/AI. Use exact Android wake primitives, local state, local fallback, and bundled emergency audio.

## 2026-09-09 — Motion scope

Begin with pickup/orientation/movement. Do not request step/activity-recognition permission until evidence shows it adds value.

## 2026-09-09 — UX consciousness model

Design around Asleep → Emerging → Engaged → Active → Oriented. Information density increases with likely wakefulness.

## 2026-09-09 — Snooze

Snooze is permitted and intentional. Learn whether it helps or predicts delay for the individual rather than moralizing it.

## 2026-09-09 — Character scope

Product direction includes Alfred, Sam, and Chaos; implementation begins with Alfred only.

## 2026-09-09 — Optional account

First alarm must work without sign-up.

## 2026-09-09 — Brand

Final current name: **Wake My Way**. Short mark: **WMW**.

Primary line: **Wake up your way.**  
Product explanation: **An alarm that learns what works for you.**

`wakemyway.com` returned available in a live Namecheap lookup; this does not imply registration.

## 2026-09-09 — Backend direction

TypeScript + Fastify + PostgreSQL + Kysely + OpenAPI is the preferred future direction. Do not scaffold it before a concrete cloud capability needs it.

## 2026-09-09 — Supabase data platform

Use Supabase as the preferred future managed data platform: PostgreSQL for cloud persistence, Supabase Auth later when accounts/sync/device migration justify identity, and Supabase Storage only for concrete object-storage use cases. Vercel remains compute/API hosting; Supabase remains data infrastructure. Android domain behavior does not bind directly to Supabase tables, and no Supabase outage may prevent the current local wake attempt.

## 2026-09-09 — Voice provider

No provider lock before a measured spike. Direct OpenAI realtime is the first likely prototype; LiveKit/ElevenLabs roles are evaluated with measurements.

## 2026-09-09 — Privacy/analytics

Manual semantic analytics only. Do not retain raw audio by default or send private morning content to telemetry.

# Pre-implementation architecture review — 2026-09-09

## Canonical `CONTEXT.md`

Create root `CONTEXT.md` as the single canonical domain vocabulary/invariant source. Remove duplicate glossary ownership.

## Deep Alarm Kernel

Treat the Alarm Kernel as one deep reliability capability. Its public contract hides persistence/snapshot/OS-registration ordering, snooze replacement, reconciliation, readiness, and eventually active alarm execution/recovery.

## Minimal physical modules

M0 begins with only `:app`, `:wake-core`, and `:benchmark`. Do not pre-split feature/platform/data/testkit modules.

## Manual DI first

Do not add Hilt in M0 without object-graph pressure. Prefer explicit constructor composition first; add DI framework only when it demonstrably improves locality/maintenance.

## One active Wake Schedule in V1

V1 supports one active Wake Schedule, potentially with weekday-specific times, producing one next Wake Occurrence. Multiple independent adaptive alarm definitions are deferred.

## Simplified Wake Runtime phases

Canonical phases are Alerting, Engaging, Activating, Orienting, Finished. Escalation, fallback, movement requests, and First Move are not phases.

## Remove "Verified Awake" from domain language

The phone observes behavioral activation; it does not medically verify consciousness.

## Activation evidence is internal to Wake Runtime

Do not expose a standalone WakeConfidenceEstimator/StrategyPolicy architecture prematurely. Wake Runtime owns in-session evidence/escalation. Wake Learning owns off-session policy derivation.

## Direct Boot critical state

Critical Wake Snapshot is minimal/non-sensitive and stored in device-protected storage. Sensitive personalized/contextual content remains credential-protected; pre-unlock wake falls back to generic safe local audio.

## Honest Force Stop reliability envelope

Explicit Force Stop can invalidate pending delivery on modern Android. Treat this as a platform limitation and reconcile on next user start; do not market an impossible guarantee.

## Backend/voice seams are earned

Do not create generic provider interfaces or backend services before a real external boundary exists. Realtime voice transport is selected only by a measured spike.

## Testing and deployment topology

Testing is layered by evidence type: pure Kotlin contracts → Wake Lab simulation/replay → Android instrumentation → Firebase Test Lab → Google Play Internal Testing → real overnight dogfood. Real-device mornings remain the final validation of both reliability and product effectiveness.

Vercel is the preferred initial deployment platform for future non-critical web/API workloads once a real backend capability exists. It is never part of the Alarm Kernel critical path.

# Plan hardening review — 2026-09-09

## Active Wake Execution is part of Alarm Kernel reliability

Scheduling reliability is not enough. Once a Wake Occurrence fires, critical alarm audio and controls must survive ordinary `WakeActivity` recreation/process churn where Android allows.

Decision:

- Alarm Kernel owns an **Active Wake Execution** lifecycle
- `WakeActivity` is presentation, not critical playback lifetime authority
- initial implementation direction uses an alarm-appropriate foreground playback service/controller with `USAGE_ALARM` semantics
- duplicate starts/recreation must converge on one active occurrence
- stopped or durably snoozed occurrences must never resurrect
- power/wake-lock behavior, if needed, remains private Alarm Kernel implementation

See ADR-014.

## Current Android SDK baseline

As of 2026-09-09, Google Play requires new phone/tablet app submissions and app updates to target Android 16 / API 36 or higher.

Decision for M0:

```text
targetSdk = 36 baseline
compileSdk = 36+ current stable toolchain
minSdk = decide during M0 from supported-device/reliability evidence
```

Revalidate before future target-SDK upgrades and submission.

## Exact-alarm manifest direction

WMW is a dedicated alarm-clock app whose core user-facing functionality requires precise timing.

Decision: begin implementation with **`USE_EXACT_ALARM` as the preferred manifest strategy**, subject to current Google Play restricted-permission eligibility/review and implementation-time revalidation. Switch to `SCHEDULE_EXACT_ALARM` or another path only if current policy/platform evidence requires it, and record the reversal.

## Wake Learning moves before realtime voice

The adaptive learning loop is the core moat; realtime conversation is presentation richness.

Decision:

```text
M7 = deterministic local Wake Learning v0
M8 = realtime voice architecture spike
M9 = realtime conversation
M10 = useful context
```

M7 must prove at least one bounded, explainable, reversible policy adaptation before realtime provider complexity becomes central.

Wake Learning v0 is local/offline and requires no backend or ML.

## Activation Completion vs Confirmed Wake Success

The runtime's own activation threshold must not become circular proof that the product succeeded.

Decision:

- **Activation Completion** = phone-observable runtime criterion reached
- **Confirmed Wake Success** = calibrated evidence that the user actually achieved the intended wake result
- product-level Wake Success should use confirmed calibration where available
- missing feedback does not become automatic confirmation
- Wake Learning must not optimize Activation Completion alone if calibration shows return-to-bed false positives

## Intentional Stop

The user always retains an accessible, local, non-AI-dependent way to stop an active alarm.

Stop may be made deliberate rather than the largest reflex target, but must never become a puzzle, hidden control, coercive trap, or network-dependent action. Kernel stop is idempotent and terminal for that occurrence.

## Safety Backup as trust-transition hypothesis

During founder/trusted dogfood, WMW may optionally allow a later conventional Safety Backup alarm while trust is being earned.

It is not a second adaptive Wake Schedule and must not pull generic multi-alarm coordination into V1 architecture. Track whether users stop needing it and keep WMW's own reliability metrics independent from the backup.

## Voice/backend numbering amendment

ADR-008 and ADR-012 are amended: the measured realtime voice decision now occurs in **M8**, after M7 Wake Learning v0.

# Product design foundation — 2026-09-10

## Adaptive Dawn is the canonical visual direction

Wake My Way should feel like a premium bedside object that wakes with the user rather than a generic alarm utility, chatbot, wellness product or SaaS dashboard.

The visual experience follows the existing UX consciousness model with progressively increasing definition and information density:

```text
Night → Emerging → Engaged → Active → Oriented → Morning
```

This remains a presentation model. It does not create a second behavioral state machine and does not rename canonical Wake Runtime phases.

The signature visual language is based on darkness-to-warmth, restrained geometry, tactile surfaces, excellent typography and calm motion. Purple/blue AI gradients, glowing AI orbs, robots, cartoon alarm clocks, generic sunrise art and engagement-style gamification remain explicitly excluded.

See [`implementation/product-design-foundation.md`](implementation/product-design-foundation.md).

## Product home becomes Tonight

The normal application entry surface becomes a focused **Tonight** experience centered on the next wake time, character presence and Wake Ready reassurance.

The founder Wake Alarm Lab remains available as an explicit developer destination rather than defining the consumer-facing home.

## Wake visual richness cannot become wake authority

`WakeActivity` may become visually rich and state-responsive, but it remains presentation only.

- Alarm Kernel remains critical playback/Stop/Snooze authority.
- Wake Runtime remains behavioral authority.
- Direct Boot and private prepared-content gating remain unchanged.
- animation/shape/gradient failure must degrade visually without preventing a locally actionable alarm.

## Navigation 3 adoption is now earned

The normal app now has real destination complexity: Tonight plus preserved founder/developer surfaces, with setup/history/settings to follow.

Decision: adopt stable Navigation 3 for normal Compose navigation while keeping dedicated `WakeActivity` outside the navigation graph. Use only the minimal runtime/UI artifacts initially.

## Native WMW Presence over animation-framework identity

Adopt AndroidX `graphics-shapes` for a restrained, local, geometric WMW Presence that can morph with presentation state.

Do not adopt Rive, Lottie or an AI-orb metaphor as the default identity system. Native Compose state-driven animation remains the primary motion architecture. Re-evaluate authored-animation runtimes only for a concrete future capability gap.

## Visual regression follows canonical previews

Roborazzi is the preferred visual-regression tool, but goldens are introduced only after canonical synthetic previews stabilize enough to represent reviewed design truth.

The first foundation slice therefore creates canonical previews first; the next design gate records reviewed baselines and protects selected surfaces in CI. Private wake data is never used in visual fixtures.

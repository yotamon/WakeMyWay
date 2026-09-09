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

Treat the Alarm Kernel as one deep reliability capability. Its public contract hides persistence/snapshot/OS-registration ordering, snooze replacement, reconciliation, and readiness.

## Minimal physical modules

M0 begins with only `:app`, `:wake-core`, and `:benchmark`. Do not pre-split feature/platform/data/testkit modules.

## Manual DI first

Do not add Hilt in M0 without object-graph pressure. Prefer explicit constructor composition first; add DI framework only when it demonstrably improves locality/maintenance.

## One active Wake Schedule in V1

V1 supports one active Wake Schedule, potentially with weekday-specific times, producing one next Wake Occurrence. Multiple independent alarm definitions are deferred.

## Simplified Wake Runtime phases

Canonical phases are Alerting, Engaging, Activating, Orienting, Finished. Escalation, fallback, movement requests, and First Move are not phases.

## Remove "Verified Awake" from domain language

The phone observes behavioral activation; it does not medically verify consciousness. Product metrics use Wake Success / activation criterion terminology.

## Activation evidence is internal to Wake Runtime

Do not expose a standalone WakeConfidenceEstimator/StrategyPolicy architecture prematurely. Wake Runtime owns in-session evidence/escalation. Wake Learning owns off-session policy derivation.

## Direct Boot critical state

Critical Wake Snapshot is minimal/non-sensitive and stored in device-protected storage. Sensitive personalized/contextual content remains credential-protected; pre-unlock wake falls back to generic safe local audio.

## Honest Force Stop reliability envelope

Explicit Force Stop can invalidate pending delivery on modern Android. Treat this as a platform limitation and reconcile on next user start; do not market an impossible guarantee.

## Backend/voice seams are earned

Do not create generic provider interfaces or backend services before a real external boundary exists. The M7 voice spike determines the actual voice seam.

## 2026-09-09 — Testing and deployment topology

Testing is layered by evidence type: pure Kotlin contracts → Wake Lab simulation/replay → Android instrumentation → Firebase Test Lab → Google Play Internal Testing → real overnight dogfood. Real-device mornings remain the final validation of both reliability and product effectiveness.

Vercel is the preferred initial deployment platform for future non-critical web/API workloads once a real backend capability exists. It is never part of the Alarm Kernel critical path. Realtime voice transport remains an M7 measured decision; Vercel WebSockets are one candidate, not a commitment.

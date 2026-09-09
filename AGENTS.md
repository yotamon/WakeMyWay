# AGENTS.md — Wake My Way engineering context

This is the first engineering context for any coding agent working in this repository.

## Read before changing code

Read in this order:

1. [`CONTEXT.md`](CONTEXT.md) — canonical domain language and invariants
2. [`docs/00-project-status.md`](docs/00-project-status.md) — current implementation state and exact next work
3. [`docs/03-product-principles.md`](docs/03-product-principles.md) — product constraints
4. [`docs/08-android-architecture.md`](docs/08-android-architecture.md) — architecture and module boundaries
5. [`docs/10-alarm-kernel.md`](docs/10-alarm-kernel.md) — alarm-critical reliability contract
6. [`docs/11-wake-runtime-state-machine.md`](docs/11-wake-runtime-state-machine.md) — in-session deterministic behavior
7. the active milestone in [`docs/21-roadmap-implementation-plan.md`](docs/21-roadmap-implementation-plan.md)
8. for testing/deployment/cloud work, [`docs/32-testing-and-deployment-topology.md`](docs/32-testing-and-deployment-topology.md)

For UX work also read `docs/04-ux-psychology.md`, `docs/05-ux-flows.md`, and `docs/07-design-system.md`.

## Architecture posture

Wake My Way favors **deep modules**: important complexity belongs behind a small, stable contract rather than being spread across many thin services/interfaces.

Before adding an interface, module, repository abstraction, provider wrapper, or Gradle module, ask:

1. What concrete decision or failure domain does this seam hide?
2. Are there already two implementations, an external boundary, or a deterministic-test need?
3. If the abstraction were deleted and its implementation inlined, would callers become meaningfully more coupled or complex?

If the answer is no, keep the implementation local and concrete.

## Hard rules

- Android is the only implementation target now. Future iOS portability is preserved through domain language, behavior tests, data/API contracts, and clean platform boundaries — **not** speculative iOS-shaped interfaces.
- Use native Android: Kotlin + Jetpack Compose.
- Do not introduce React Native, Flutter, or KMP unless a future ADR explicitly changes the decision.
- M0 starts with only `:app`, `:wake-core`, and `:benchmark`. Split more physical modules only after implementation evidence shows a real boundary.
- `:wake-core` is pure Kotlin and must not depend on `android.*`, Compose, Room, networking, voice providers, or analytics.
- V1 supports **one active Wake Schedule at a time**, which may have weekday-specific times and produces one next Wake Occurrence.
- The **Alarm Kernel is a deep module**. Callers must not orchestrate persistence → snapshot → `AlarmManager` ordering themselves.
- Do not put network, AI, analytics, account, subscription, calendar, weather, or generated content on the critical alarm path.
- The Android app runs on the device; it is never "hosted on Vercel". Vercel is reserved for future non-critical web/API workloads and must remain outside current wake authority.
- Supabase is the preferred future managed cloud data platform: PostgreSQL first, Auth later when accounts are justified, Storage only when a concrete object-storage need exists. It remains outside current wake authority.
- Do not couple Android domain behavior directly to Supabase tables. Domain reads/writes go through the Wake API; a future direct Supabase Auth flow may be used only for identity/session acquisition if explicitly implemented.
- Realtime voice transport is spike-gated. Do not assume Vercel WebSockets, direct provider access, or LiveKit until M7 measurements select the boundary.
- Do not use WorkManager to fire alarms. WorkManager is for deferrable preparation/sync only.
- The exact native alarm primitive is `AlarmManager.setAlarmClock()` for user-facing wake occurrences.
- Alarm delivery must degrade safely when backend, network, AI, generated speech, normal database initialization, or optional permissions fail.
- Direct Boot matters. Only the minimal, non-sensitive **Critical Wake Snapshot** may live in device-protected storage. Tomorrow Contract text, calendar content, transcripts, prompts, tokens, and personalized private speech remain credential-protected.
- Explicit Android Force Stop is outside the deliverable-alarm reliability envelope when the OS cancels the app's pending intents. Detect/reconcile on next user start; never promise the impossible.
- The **Wake Runtime** is deterministic and owns in-session activation evidence, escalation, and behavioral policy. Do not split these into shallow public modules merely for theoretical testability.
- AI may render an approved `Speech Intent`; it must never decide phase transitions, activation success, alarm dismissal, snooze acceptance, or facts.
- Do not use the legacy term **Verified Awake**. The app observes behavioral activation; it does not medically verify consciousness.
- Never store raw microphone audio by default.
- Never send full transcripts, calendar descriptions, Tomorrow Contract raw text, generated prompts, or private wake content to crash/analytics tooling.
- No shame, humiliation, guilt, threats, or infantilizing character language.
- No AI-gradient/orb/robot visual clichés. Follow the Wake My Way design language.

## Product job

> The user deliberately chose a wake time because they want to become meaningfully active around that time. Help them succeed with the minimum effective friction that works for them.

Do not expand Wake My Way into a task manager, general assistant, sleep tracker, journaling app, or news reader without an explicit product decision.

## Reliability degradation

```text
Full realtime conversational Wake
    ↓
Prepared/local personalized speech
    ↓
Prepared scripted character plan
    ↓
Deterministic local Wake Runtime
    ↓
Standard branded alarm + basic controls
    ↓
Bundled emergency alarm
```

Fallback richness may degrade. A valid scheduled wake attempt must remain locally actionable inside the documented Android reliability envelope.

## Testing truth hierarchy

Behavioral correctness is established through pure Kotlin tests and Wake Lab replay. Platform reliability is established through Android instrumentation, device matrices, Play-distributed builds, and real overnight dogfood. No emulator or cloud-device pass alone proves that the product reliably wakes a sleeping user.

## After meaningful work

Update `docs/00-project-status.md` with:

- what is implemented and tested
- what is currently active
- known risks/blockers
- exact next task

If canonical terminology changes, update `CONTEXT.md` in the same change. If a durable technical choice changes, add/update an ADR. If a product decision changes, update `docs/26-decisions-log.md`.

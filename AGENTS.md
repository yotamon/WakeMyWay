# AGENTS.md — Wake My Way engineering context

This is the first engineering context for any coding agent working in this repository.

## Read before changing code

Read in this order:

1. [`CONTEXT.md`](CONTEXT.md) — canonical domain language and invariants
2. [`docs/00-project-status.md`](docs/00-project-status.md) — current implementation state and exact next work
3. [`docs/03-product-principles.md`](docs/03-product-principles.md) — product constraints
4. [`docs/08-android-architecture.md`](docs/08-android-architecture.md) — architecture and module boundaries
5. [`docs/10-alarm-kernel.md`](docs/10-alarm-kernel.md) — alarm-critical reliability contract
6. [`docs/adr/014-active-wake-execution-lifecycle.md`](docs/adr/014-active-wake-execution-lifecycle.md) — active alarm playback/recovery invariant
7. [`docs/11-wake-runtime-state-machine.md`](docs/11-wake-runtime-state-machine.md) — in-session deterministic behavior
8. [`docs/14-wake-strategy-learning.md`](docs/14-wake-strategy-learning.md) — local/off-session adaptation rules
9. the active milestone in [`docs/21-roadmap-implementation-plan.md`](docs/21-roadmap-implementation-plan.md)
10. for testing/deployment/cloud work, [`docs/32-testing-and-deployment-topology.md`](docs/32-testing-and-deployment-topology.md)
11. for cloud AI work, [`docs/adr/016-vercel-ai-platform.md`](docs/adr/016-vercel-ai-platform.md) and [`docs/implementation/vercel-ai-platform.md`](docs/implementation/vercel-ai-platform.md)

For UX work also read `docs/04-ux-psychology.md`, `docs/05-ux-flows.md`, and `docs/07-design-system.md`.

## Architecture posture

Wake My Way favors **deep modules**: important complexity belongs behind a small, stable contract rather than being spread across many thin services/interfaces.

Before adding an interface, module, repository abstraction, provider wrapper, or Gradle module, ask:

1. What concrete decision or failure domain does this seam hide?
2. Are there already two implementations, an external boundary, or a deterministic-test need?
3. If the abstraction were deleted and its implementation inlined, would callers become meaningfully more coupled or complex?

If the answer is no, keep the implementation local and concrete.

## Hard rules

- Android is the only client/runtime product target now. Future iOS portability is preserved through domain language, behavior tests, data/API contracts, and clean platform boundaries — **not** speculative iOS-shaped interfaces. Optional `apps/cloud` code may implement non-critical web/API/AI capabilities, but it never becomes Android wake authority.
- Use native Android: Kotlin + Jetpack Compose.
- Do not introduce React Native, Flutter, or KMP unless a future ADR explicitly changes the decision.
- M0 starts with only `:app`, `:wake-core`, and `:benchmark`. Split more physical Android modules only after implementation evidence shows a real boundary. The isolated `apps/cloud` service is not a Gradle/Android module.
- `:wake-core` is pure Kotlin and must not depend on `android.*`, Compose, Room, networking, voice providers, analytics, or cloud SDKs.
- Current M0 Android baseline is `targetSdk = 36`, `compileSdk >= 36` using the current stable toolchain, with `minSdk` selected deliberately during M0. Revalidate before future target-SDK upgrades.
- The preferred exact-alarm manifest direction is `USE_EXACT_ALARM` because WMW is a dedicated alarm-clock app. Revalidate current Google Play restricted-permission eligibility before implementation/submission; do not silently change the strategy.
- The exact native alarm primitive is `AlarmManager.setAlarmClock()` for user-facing Wake Occurrences.
- V1 supports **one active adaptive Wake Schedule at a time**, which may have weekday-specific times and produces one next Wake Occurrence.
- A dogfood Safety Backup, if implemented, is a temporary conventional fallback and **not** a second adaptive Wake Schedule. Do not introduce generic multi-alarm architecture for it.
- The **Alarm Kernel is a deep module**. Callers must not orchestrate persistence → snapshot → `AlarmManager` ordering themselves.
- Alarm Kernel ownership continues after the trigger through **Active Wake Execution**. `WakeActivity` must never be the sole lifetime owner of critical alarm playback.
- Initial active-alarm implementation direction is an alarm-appropriate foreground playback service/controller with `USAGE_ALARM` semantics, kept private inside the Alarm Kernel. A Service class does not automatically deserve its own Gradle module/public interface.
- Active-alarm starts/recovery must be idempotent: one active occurrence cannot produce duplicate overlapping critical audio.
- Stop is local, accessible, non-AI-dependent, idempotent, and terminal for that occurrence. Component recreation/stale trigger must not resurrect it.
- Snooze is a durable replacement occurrence. The old active execution ends only after the new exact Snooze Occurrence is safely scheduled; the old occurrence must not resurrect afterward.
- Do not add broad/indefinite wake locks by default. If measurements prove a power primitive is needed, its lifecycle belongs inside Alarm Kernel and must be released on all terminal paths.
- Do not put network, AI, analytics, account, subscription, calendar, weather, or generated content on the critical alarm path.
- The Android app runs on the device; it is never "hosted on Vercel". `apps/cloud` may run optional non-critical web/API/AI workloads on Vercel, but Vercel must remain outside Wake Ready, alarm delivery, Active Wake Execution, Stop/Snooze, Wake Runtime authority, and local M7 learning.
- Vercel AI SDK + AI Gateway are the default cloud AI access layer under ADR-016. Product/domain code should use the deep `apps/cloud/src/ai` boundary rather than importing provider SDKs directly, unless a measured capability requires an explicit documented exception.
- Realtime token support in `apps/cloud` is an M8 spike facility only. It does not select Vercel-mediated realtime transport or authorize coupling Android's live wake session to Vercel before measurements.
- Never embed `WMW_INTERNAL_API_KEY`, AI Gateway keys, provider keys, or other server/operator credentials in the Android app. Future Android-facing cloud endpoints require installation/account/session authorization.
- Supabase is the preferred future managed cloud data platform: PostgreSQL first, Auth later when accounts are justified, Storage only when a concrete object-storage need exists. It remains outside current wake authority.
- Do not couple Android domain behavior directly to Supabase tables. Domain reads/writes go through the Wake API; a future direct Supabase Auth flow may be used only for identity/session acquisition if explicitly implemented.
- **Wake Learning v0 is M7 and local/offline.** Do not create a backend, ML model, generic rule engine, or cloud learning dependency to implement initial adaptation.
- Wake Learning may derive future Wake Policy only between sessions. One Wake Session uses one immutable policy version.
- Learned policy changes must be bounded, explainable, versioned, reversible/resettable, and constrained by annoyance/agency as well as effectiveness.
- The runtime's own activation threshold is **Activation Completion**, not automatic proof of real Wake Success.
- **Confirmed Wake Success** requires calibration evidence such as occasional later user feedback or a future validated privacy-safe proxy. Missing calibration is unknown, not success.
- Wake Learning must not optimize Activation Completion alone if calibration shows return-to-bed false positives.
- Realtime voice transport is spike-gated. Do not assume Vercel WebSockets/Gateway transport, direct provider access, or LiveKit until **M8** measurements select the boundary.
- Do not use WorkManager to fire alarms. WorkManager is for deferrable preparation/sync only.
- Alarm delivery must degrade safely when backend, network, AI, generated speech, normal database initialization, or optional permissions fail.
- Direct Boot matters. Only the minimal, non-sensitive **Critical Wake Snapshot** may live in device-protected storage. Tomorrow Contract text, calendar content, transcripts, prompts, tokens, learned private explanations, and personalized private speech remain credential-protected.
- Explicit Android Force Stop is outside the deliverable-alarm reliability envelope when the OS stops/cancels the app's pending work. Detect/reconcile on next user start; never promise the impossible.
- Ordinary Activity recreation/process churn is **not** treated as Force Stop and must be covered by M1/M2 active-execution recovery tests.
- The **Wake Runtime** is deterministic and owns in-session activation evidence, escalation, and behavioral policy. Do not split these into shallow public modules merely for theoretical testability.
- AI may render an approved `Speech Intent`; it must never decide phase transitions, Activation Completion, Confirmed Wake Success, alarm dismissal, snooze acceptance, or facts.
- Do not use the legacy term **Verified Awake**. The app observes behavioral activation; it does not medically verify consciousness.
- Never store raw microphone audio by default.
- Never send full transcripts, calendar descriptions, Tomorrow Contract raw text, generated prompts, private learning inputs/explanations, or private wake content to crash/analytics tooling.
- Cloud AI errors/logs must not include prompts, transcripts, raw audio, private generated speech, provider response bodies, or secrets. Prefer request IDs and typed metadata.
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
Deterministic local Wake Runtime + learned policy
    ↓
Standard branded alarm + basic controls
    ↓
Bundled emergency alarm
```

Fallback richness may degrade. A valid scheduled wake attempt must remain locally actionable inside the documented Android reliability envelope.

The critical execution chain is conceptually:

```text
Wake Occurrence scheduled
    ↓
AlarmManager trigger
    ↓
Alarm Kernel Active Wake Execution
    ↓
safe local alarm playback + controls
    ↓
WakeActivity / Wake Runtime enrichment
```

Never reverse this dependency so UI/AI availability becomes a prerequisite for alarm playback.

## Testing truth hierarchy

Behavioral correctness is established through pure Kotlin tests and Wake Lab replay. Platform reliability is established through Android instrumentation, lifecycle kill/recreate cases, device matrices, Play-distributed builds, and real overnight dogfood. No emulator or cloud-device pass alone proves that the product reliably wakes a sleeping user.

When working on M7 learning, include fixtures where:

```text
Activation Completion = true
Calibration = RETURNED_TO_BED
```

and verify the system preserves the disagreement instead of counting it as Wake Success.

Cloud AI tests should remain deterministic and non-billable by default. Use unit/type tests for routing/auth/validation and explicit controlled smoke tests for real providers only when credentials/spend limits are intentionally configured.

## After meaningful work

Update `docs/00-project-status.md` with:

- what is implemented and tested
- what is currently active
- known risks/blockers
- exact next task

If canonical terminology changes, update `CONTEXT.md` in the same change. If a durable technical choice changes, add/update an ADR. If a product decision changes, update `docs/26-decisions-log.md`.

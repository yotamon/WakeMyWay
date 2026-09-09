# Pre-implementation architecture review — 2026-09-09

## Purpose

Before writing the first application code, the entire Wake My Way specification was reviewed using deep-module/codebase-design principles from the `improve-codebase-architecture` skill.

The review focused on **module depth, leverage, locality, justified seams, thin adapters, and the behavior-preserving inlining/deletion test**.

This was the cheapest possible moment to simplify the architecture: before code, migrations, tests, SDK integrations, or public APIs made speculative decisions expensive to reverse.

## Review scope

Reviewed:

- product vision/principles/scope
- UX psychology and screen flows
- brand/design system implications
- Android architecture and Alarm Kernel
- Wake Runtime / policy / learning
- local data model and storage classes
- realtime voice / character plan
- context/calendar/weather
- privacy/security
- reliability/failure modes
- testing / observability / metrics
- backend/API direction
- implementation roadmap/backlog
- future iOS approach
- ADRs and documentation workflow

## Applied architecture changes

### 1. Create canonical root `CONTEXT.md`

Previously domain vocabulary lived in a numbered glossary plus topic documents. That made it easy for agents/code to invent synonyms or choose the wrong source.

Now `CONTEXT.md` owns canonical terms and invariants. `docs/28-glossary.md` was removed.

### 2. Deepen the Alarm Kernel

**Before:** the plan implied caller-visible store/scheduler/reconciler pieces and ordering.

**After:** Alarm Kernel owns the entire trust-critical scheduling transaction and recovery invariant. Callers ask for an outcome; they do not coordinate Room, critical snapshot, `AlarmManager`, and reconciliation.

This is the top recommendation because failure knowledge around alarms must remain local.

### 3. Simplify physical Android modules

**Before:** planned `core:domain`, `core:data`, `platform`, multiple `feature:*`, `testkit`, `benchmark`, DI framework, etc. before code existed.

**After:** M0 begins with:

```text
:app
:wake-core
:benchmark
```

Packages preserve locality until real compile/ownership/change-pressure evidence earns another Gradle module.

### 4. Manual composition before Hilt

Hilt is no longer an M0 dependency. Start with constructor injection and one explicit Android composition root. Introduce a DI framework only when the real object graph demonstrates that it improves maintainability.

### 5. Deepen Wake Runtime

**Before:** reducer/state + confidence estimator + strategy policy were prospective separate public abstractions.

**After:** one Wake Runtime owns in-session behavior: phase transitions, activation evidence, escalation/intervention, and policy interpretation. Wake Learning remains separate because it runs between sessions and has a genuinely different lifecycle.

### 6. Simplify runtime phases

Canonical code phases:

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

Removed as phases:

- Prepared — occurrence/readiness concern
- Escalating — policy/intervention detail
- Mobility Check — directive/action
- Verified Awake — scientifically overclaims what phone sensors know
- First Action — orientation data
- Emergency Fallback — capability mode

The UX model Asleep → Emerging → Engaged → Active → Oriented remains valuable but is explicitly **not** mapped 1:1 to runtime states.

### 7. Replace “Verified Awake” with behavioral activation

Wake My Way can observe interaction/motion patterns and define a product activation criterion. It cannot prove biological consciousness or literal out-of-bed state.

Metrics now use **Wake Success** and **activation criterion** terminology.

### 8. Define V1 schedule cardinality

V1 supports **one active Wake Schedule** at a time. It may express weekday-specific times and always produces one next Wake Occurrence.

Multiple independent alarms are deferred because they immediately multiply collision, snapshot, snooze, readiness, and reconciliation semantics before the core adaptive wake thesis is proven.

### 9. Add Direct Boot architecture

A real Android reliability gap was found: a reboot can leave credential-protected storage unavailable until first unlock.

Decision:

- minimal Critical Wake Snapshot in device-protected storage
- no private Tomorrow Contract/calendar/transcript/token data there
- Direct-Boot-aware recovery
- generic safe local wake before unlock
- reconcile richer state after unlock

### 10. Make Force Stop limitation explicit

The product cannot honestly guarantee pending wake delivery after explicit user Force Stop on Android versions that cancel the app's pending intents.

Testing and Wake Ready behavior now model this as a platform envelope limitation plus repair/reconciliation case, not a test the app can magically pass.

### 11. Defer backend/provider seams until real use

Backend direction (TypeScript/Fastify/Postgres/Kysely/OpenAPI) remains chosen, but no backend skeleton belongs in M0.

Voice provider abstractions are not frozen before the M7 measurement spike. Stable product semantics are Speech Intents and behavioral authority boundaries, not vendor-shaped interfaces.

### 12. Test stable contracts, not internal topology

Testing now emphasizes:

- Alarm Kernel scheduling/recovery invariants
- Wake Runtime input → directive behavior
- Direct Boot / Force Stop / permissions / process death
- deterministic replay
- latency distributions measured on real devices

It no longer treats an internal confidence-estimator interface as the primary test surface.

## Explicit non-decisions preserved

The review did **not** invent answers for unresolved items such as:

- exact Android SDK versions at M0
- exact Play exact-alarm declaration/access flow
- voice provider choice
- final activation criterion/window
- production fonts/sonic motif
- cloud pricing/environment details
- KMP vs Swift when iOS becomes real

Those remain in `27-open-questions.md` and are decided when evidence exists.

## Implementation consequence

The first build should feel almost boring architecturally:

```text
M0
  ↓
small native Android project
  ↓
pure Kotlin recurrence/time domain
  ↓
M1 deep Alarm Kernel vertical slice
  ↓
M2 prove reliability envelope
  ↓
only then build wake intelligence
```

That is intentional. Wake My Way earns complexity in the order users depend on it.

## Review outcome

**Ready to begin implementation.**

The highest-risk conceptual gaps have explicit owners, the critical path is smaller, speculative seams have been removed, and remaining unknowns are documented as questions rather than silently encoded as architecture.

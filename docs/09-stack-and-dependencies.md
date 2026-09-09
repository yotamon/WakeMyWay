# Stack and dependency strategy

**Decision snapshot:** September 2026. Exact versions must be re-verified when the dependency is first introduced.

## Principle: adopt by milestone, not by wishlist

A planned technology is not automatically an M0 dependency. Add a library only when a current implementation task earns it.

This keeps the codebase small, exposes the real architecture before abstractions harden, and prevents "future iOS", "future backend", or "future scale" from creating present complexity.

## M0/M1 Android essentials

| Area | Choice | Introduce when | Notes |
|---|---|---|---|
| Language | Kotlin | M0 | native Android implementation |
| UI | Jetpack Compose | M0 | app/setup/wake surfaces |
| Design primitives | Material 3 | M0 | accessible primitives; heavily branded visually |
| Async/state | Coroutines + Flow | M0 | standard Kotlin concurrency/state |
| Structured local DB | Room 3 | M1 | normal credential-protected app data; migrations/schema history |
| Small preferences | DataStore | only when settings exist | do not use for relational wake history |
| Alarm scheduling | Android `AlarmManager` | M1 | `setAlarmClock()` for actual Wake Occurrences |
| Critical pre-unlock state | device-protected storage + atomic file semantics | M1 | minimal non-sensitive Critical Wake Snapshot |
| Sensors | `SensorManager` | M4 | derived movement evidence; no raw stream persistence |
| Deferrable work | WorkManager | M6 | prepared plans/audio/context refresh; never alarm firing |
| Performance | Macrobenchmark + Baseline Profiles | M1/M2 | measure wake-path cold start and audible latency |

## Composition and dependency injection

Start with **manual constructor injection and an explicit Android composition root**.

Do not add Hilt in M0 merely because it is common Android practice. Introduce Hilt later if the object graph becomes genuinely difficult to compose/test manually, and record why it improves locality rather than simply moving wiring elsewhere.

This is intentional: the first architecture review identified speculative DI/module structure as premature complexity.

## Navigation

Do not require a navigation framework before the normal application has enough destinations to justify it.

- dedicated `WakeActivity` remains separate from ordinary app navigation
- onboarding/setup/history may begin with simple Compose state/navigation
- Navigation 3 is the preferred candidate once real navigation complexity exists

## Networking and backend client

No HTTP stack belongs in M0/M1.

When the first cloud capability is implemented, preferred Android choices are:

- OkHttp
- Retrofit
- kotlinx.serialization

The Alarm Kernel must not depend on this stack.

## Backend direction — deferred until needed

Chosen direction, not current scaffold:

| Area | Direction |
|---|---|
| Runtime | current Node.js LTS at implementation time |
| Language | TypeScript |
| Server | Fastify |
| Database | Supabase-managed PostgreSQL |
| Query layer | Kysely |
| Logging | Pino |
| Contract | OpenAPI 3.1 |

Create `services/api` only when Tomorrow Contract cloud preparation, realtime credentials/orchestration, sync, or another concrete capability needs it.

Vercel is the preferred compute/deployment host for that service. Supabase is the preferred managed data platform: PostgreSQL first, Auth later when identity is justified, Storage only when a concrete object-storage need appears. Mobile domain behavior must not bind directly to Supabase table schemas.

Do not create empty modules/services in M0 for architectural symmetry.

## Realtime voice

The seam itself is earned by the M7 measured spike.

Candidates:

- direct OpenAI realtime
- LiveKit as realtime/session infrastructure
- ElevenLabs for speech/character voice where it provides measurable product value

Before M7, keep voice experiments isolated. Do not bake a generic `AIProvider` or provider hierarchy into core domain code.

The stable domain contract is a **Speech Intent / character rendering request**, not a vendor SDK shape.

## Observability — later milestone

Preferred candidates once dogfood needs them:

- Sentry for crash/ANR/performance diagnostics
- PostHog for manual semantic product events
- Roborazzi for selected Compose visual regression
- Turbine for Flow tests where Flow behavior exists
- LeakCanary in debug/dogfood if lifecycle/audio/sensor leaks become a real risk

Do not initialize analytics/crash SDKs on the critical first-audible alarm path.

## Deliberately excluded until evidence

- KMP
- React Native / Flutter
- Redis
- Kafka / RabbitMQ
- queue framework by default
- MVI framework
- general state-machine framework
- SQLCipher without a threat-model need
- step/activity-recognition permission in V1
- an interface for every Android service
- repository interfaces whose only implementation is a local DAO and whose callers gain no meaningful contract

## Physical module policy

M0 physical Gradle modules:

```text
:app
:wake-core
:benchmark
```

Add a Gradle module only when at least one is true:

- compile/dependency isolation produces concrete value
- multiple feature owners/change rates require it
- a module is independently reusable/testable behind a stable contract
- build performance demonstrates a split helps
- a real platform/provider seam needs isolation

Package structure can express locality before physical Gradle boundaries are justified.

## Dependency acceptance checklist

A dependency is accepted only if it:

1. solves a current problem
2. hides meaningful complexity or provides substantial leverage
3. has healthy maintenance and compatible Android/API support
4. has acceptable commercial/code/model licensing
5. has an understood failure mode
6. does not become part of the Alarm Kernel critical path without strong evidence
7. can be removed or replaced without forcing product/domain concepts to change

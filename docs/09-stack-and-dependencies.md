# Stack and dependency strategy

**Decision snapshot:** September 2026. Exact versions must be re-verified when the dependency is first introduced.

## Principle: adopt by milestone, not by wishlist

A planned technology is not automatically an M0 dependency. Add a library only when a current implementation task earns it.

This keeps the codebase small, exposes the real architecture before abstractions harden, and prevents "future iOS", "future backend", or "future scale" from creating present complexity.

## Android SDK baseline

As of 2026-09-09, Google Play requires new Android phone/tablet apps and app updates submitted after 2026-08-31 to target **Android 16 / API 36 or higher**.

M0 therefore begins with:

```text
targetSdk = 36
compileSdk = 36+ using the current stable Android toolchain
minSdk = decide during M0 from supported-device and reliability needs
```

Re-verify Play requirements before every major target-SDK upgrade and before public submission.

For exact alarms, WMW begins with `USE_EXACT_ALARM` as the preferred manifest direction because exact alarm-clock behavior is the application's core user-facing function. This remains subject to Google Play restricted-permission review and must be revalidated when implemented/submitted.

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
| Exact alarm access | `USE_EXACT_ALARM` preferred | M0/M1 | revalidate Play eligibility/policy before manifest/submission |
| Critical pre-unlock state | device-protected storage + atomic file semantics | M1 | minimal non-sensitive Critical Wake Snapshot |
| Active alarm playback | foreground alarm playback service/controller + alarm audio attributes | M1 | `WakeActivity` does not own critical playback lifetime; see ADR-014 |
| Sensors | `SensorManager` | M4 | derived movement evidence; no raw stream persistence |
| Deferrable work | WorkManager | M6 | prepared plans/audio/context refresh; never alarm firing |
| Local learning | pure Kotlin deterministic policy updater | M7 | explainable local Wake Learning v0; no ML/cloud requirement |
| Performance | Macrobenchmark + Baseline Profiles | M1/M2 | measure wake-path cold start and audible latency |

## Active audio platform posture

Android 17 hardens background audio interactions. WMW's critical alarm path should use alarm-appropriate audio attributes (`USAGE_ALARM`) and a valid foreground execution shape rather than relying on incidental Activity lifetime.

The first implementation is expected to use a foreground service/controller privately inside the Alarm Kernel. Do not create a generic public audio-service abstraction merely because Android uses a Service component.

Wake-lock/power primitives are introduced only when real-device measurements prove they are necessary, and their lifecycle remains owned by the Alarm Kernel.

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

The Alarm Kernel and local Wake Learning v0 must not depend on this stack.

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

Create `services/api` only when cloud preparation, realtime credentials/orchestration, sync, or another concrete capability needs it.

Vercel is the preferred compute/deployment host for that service. Supabase is the preferred managed data platform: PostgreSQL first, Auth later when identity is justified, Storage only when a concrete object-storage need appears. Mobile domain behavior must not bind directly to Supabase table schemas.

Do not create empty modules/services in M0 for architectural symmetry.

## Realtime voice

The seam itself is earned by the **M8** measured spike, after local Wake Learning v0 has already begun proving the adaptive product thesis.

Candidates:

- direct OpenAI realtime
- LiveKit as realtime/session infrastructure
- ElevenLabs for speech/character voice where it provides measurable product value

Before M8, keep voice experiments isolated. Do not bake a generic `AIProvider` or provider hierarchy into core domain code.

The stable domain contract is a **Speech Intent / character rendering request**, not a vendor SDK shape.

## Observability — later milestone

Preferred candidates once dogfood needs them:

- Sentry for crash/ANR/performance diagnostics
- PostHog for manual semantic product events
- Roborazzi for selected Compose visual regression
- Turbine for Flow tests where Flow behavior exists
- LeakCanary in debug/dogfood if lifecycle/audio/sensor leaks become a real risk

Do not initialize analytics/crash SDKs on the critical first-audible or active-alarm playback path.

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
- machine-learning wake policy
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

The existence of `AlarmPlaybackService` or another Android component does not by itself justify another Gradle module.

## Dependency acceptance checklist

A dependency is accepted only if it:

1. solves a current problem
2. hides meaningful complexity or provides substantial leverage
3. has healthy maintenance and compatible Android/API support
4. has acceptable commercial/code/model licensing
5. has an understood failure mode
6. does not become part of the Alarm Kernel critical path without strong evidence
7. can be removed or replaced without forcing product/domain concepts to change

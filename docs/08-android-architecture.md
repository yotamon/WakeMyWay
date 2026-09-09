# Android architecture

## Architecture statement

Wake My Way launches on Android only. Android-specific mechanisms should be used directly where they improve reliability, while domain behavior remains expressed in Wake My Way language from [`../CONTEXT.md`](../CONTEXT.md).

Architecture optimizes for **deep modules**: coherent behavior and policy behind small contracts, with thin platform adapters only where a real seam is justified.

## High-level system

```text
ANDROID OS
    |
    v
ALARM KERNEL  <---- device-protected Critical Wake Snapshot
    |
    | starts / protects audible wake behavior
    v
WAKE RUNTIME  <---- versioned Wake Policy
    |
    | directives
    +------> character / speech rendering
    +------> motion observation
    +------> UI presentation
    |
    +------ optional cloud enrichment

WAKE LEARNING (off-session)
    |
    +------ prior outcomes + feedback -> future Wake Policy
```

The cloud is never between OS alarm delivery and the first reliable audible wake behavior.

## Deep modules

### Alarm Kernel

Responsibility:

> Turn the next accepted Wake Occurrence into a recoverable, exact, audible Android alarm and keep that invariant true across snooze, reboot, time changes, process death, and recoverable capability changes.

The Alarm Kernel owns internally:

- exact OS scheduling
- critical schedule persistence/order
- Critical Wake Snapshot
- occurrence scheduling status
- snooze replacement
- reconciliation
- boot/time/timezone handling
- critical notification/full-screen presentation setup
- first safe alarm audio/fallback
- Alarm Health facts

Callers must **not** orchestrate a sequence such as "persist Room -> write snapshot -> call AlarmManager -> mark scheduled". That sequence is implementation knowledge and belongs inside the module.

Android framework calls remain thin internal adapters/edges. The app should not create a public `AndroidAlarmScheduler` wrapper merely to mirror `AlarmManager`.

### Wake Runtime

Responsibility:

> Deterministically decide what the active Wake Session should do next from typed Wake Inputs and a versioned Wake Policy.

It owns internally:

- Wake Phase transitions
- activation evidence aggregation
- escalation selection
- in-session snooze policy
- intervention timing
- selection of Speech Intents and other Wake Directives

It does not own:

- Android alarm scheduling
- provider-specific speech/AI SDK mechanics
- UI state/layout
- history/profile generation
- cloud synchronization

Wake Runtime is pure Kotlin and is the primary behavior-test surface.

### Wake Learning

Responsibility:

> Derive future policy/profile information from completed Wake Outcomes and feedback.

It is off the alarm-critical path and may evolve independently from Wake Runtime.

### Presentation and platform edges

Compose screens, `WakeActivity`, receivers, Room DAOs, Android sensor listeners, notification builders, provider SDK adapters, and similar edges should stay intentionally thin. Thin edges are not architectural problems when policy is concentrated inside the correct deep module.

## Minimal physical module topology

Do not encode every conceptual area as a Gradle module before change pressure exists.

Start M0 with:

```text
:app
:wake-core
:benchmark
```

### `:wake-core`

Pure Kotlin/JVM. No Android framework, Compose, Room, Retrofit, Hilt, analytics, or provider SDK dependencies.

Initially owns the domain types and deterministic behavior for:

- Wake Schedule / Wake Occurrence calculation
- Wake Runtime
- Wake Policy
- typed inputs/directives
- recurrence/timezone logic
- pure outcome/profile derivation that proves cohesive

If this becomes incoherent, split by demonstrated lifecycle/change pressure rather than by naming convention.

### `:app`

Android application and all Android implementations initially. Use packages for locality, for example:

```text
alarmkernel/
wake/
setup/
history/
platform/audio/
platform/motion/
platform/calendar/
platform/voice/
data/
```

Packages may become Gradle modules later only when there is concrete leverage: build isolation, dependency enforcement, reuse, ownership, or a genuinely independent failure/change axis.

### `:benchmark`

Separate because Android Macrobenchmark requires distinct setup and process behavior.

A separate `:testkit` module is not created until test utilities are reused across multiple production modules.

## Android entry points

### MainActivity

Normal setup, next-wake configuration, history, and settings.

### WakeActivity

Dedicated alarm-session shell. It must not depend on MainActivity's navigation state.

### Receivers

Alarm and boot/time receivers are thin Android edges. They identify the event and delegate to the owning deep module. They do not contain business policy, network calls, or substantial data orchestration.

## UI architecture

Use Compose + lifecycle ViewModel + StateFlow/immutable UI state when a screen benefits from a ViewModel.

Do not add an MVI framework by default.

UI state is a projection of product state. The Wake screen does not own Wake Runtime behavior.

Navigation library adoption is deferred until normal-app navigation complexity justifies it. The dedicated WakeActivity is not a normal navigation destination.

## Justified seams

Create a seam only for a concrete reason.

Expected early seams include:

- wall clock and monotonic clock: deterministic time tests
- Android OS scheduling/audio/sensor effects where faithful test interactors are needed
- realtime voice provider after M7 proves provider variation/volatility
- external weather/context source when selected

Do **not** create one interface per Room DAO/repository/platform class solely for future iOS.

## Two clocks

Keep wall time and monotonic elapsed time semantically distinct.

Wall time is used for:

- Wake Schedule resolution
- Wake Occurrence scheduling
- calendar/time-of-day reporting

Monotonic elapsed time is used for:

- silence/response timeouts
- durations
- escalation timing

The exact implementation seam may be small, but a real clock and deterministic test clock justify it.

## Persistence authority

- Wake Schedule and rich history live in normal local persistence.
- The Alarm Kernel owns the critical schedule invariant.
- A minimal Critical Wake Snapshot lives in device-protected storage for Direct Boot/recovery.
- Sensitive Tomorrow Contract/context remains credential-protected.
- Cloud data is never authoritative for Android alarm delivery.

## Future iOS

Future iOS should reuse:

- domain language
- documented behavior/invariants
- OpenAPI contract when cloud exists
- behavioral test scenarios

It does not require Android to pre-build iOS-shaped in-process interfaces today.

When iOS becomes a real milestone, choose deliberately between a Swift implementation and extracting proven cohesive pure-Kotlin code to KMP.

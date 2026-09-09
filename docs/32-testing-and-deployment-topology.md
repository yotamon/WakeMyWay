# Testing and deployment topology

**Status:** Decided development/delivery strategy. Implementation begins with M0/M1 and expands by milestone.

Wake My Way is a mobile alarm product whose trust-critical runtime lives on the Android device. The project therefore has two separate deployment/testing concerns:

1. **Android execution and wake reliability** — runs and is validated on Android devices.
2. **Optional cloud/web capabilities** — may run on Vercel, but are never in the Alarm Kernel critical path.

The product must not blur these two systems.

## Core principle

```text
ANDROID DEVICE                              OPTIONAL CLOUD

Alarm Kernel                                Vercel
├── exact scheduling                        ├── web / landing
├── Active Wake Execution                   ├── non-critical API
├── safe local alarm audio                  ├── Wake Plan generation
└── stop / durable snooze                   ├── ephemeral voice/session auth
Wake Runtime                                └── future sync/context
Wake Learning v0
motion inputs
critical snapshot
        │                                           │
        └──────── wake + local learning work ───────┘
                     when cloud is unavailable
```

The Android app is **not hosted on Vercel**. It is compiled, distributed, installed and executed on Android. Vercel is a selected/preferred host for non-critical backend and web workloads once those workloads actually exist.

## Testing philosophy

There are three fundamentally different questions.

### Behavioral correctness

> Given the same typed inputs and policy, does Wake Runtime make the same correct deterministic behavioral decision?

This can be tested quickly and repeatedly on the JVM and in Wake Lab.

### Adaptive correctness

> Given prior safe outcomes/calibration and the same learning algorithm version, does Wake Learning derive a bounded, explainable future policy without optimizing the wrong metric?

This is primarily a pure-Kotlin M7 test surface plus dogfood validation.

### Platform/reliability truth

> Did Android actually fire the occurrence, establish Active Wake Execution, start audible output, keep it alive through ordinary lifecycle churn, and present usable controls?

This must be tested on Android, including real physical devices. Simulation cannot establish wake reliability.

## Test ladder

```text
Pure Kotlin tests
       ↓
Wake Lab accelerated simulation/replay
       ↓
Android emulator/instrumentation
       ↓
Firebase Test Lab virtual matrix
       ↓
Firebase Test Lab physical devices
       ↓
Google Play Internal Testing
       ↓
Real overnight dogfood
       ↓
Closed beta
```

Each layer has a different purpose. Passing a lower layer does not replace the layers above it.

## 1. Pure Kotlin contract tests

Run on every relevant pull request.

Primary coverage:

- Wake Schedule recurrence
- Wake Occurrence calculation
- DST gap/overlap policy
- wall vs monotonic clock behavior
- Wake Runtime transition invariants
- snooze policy
- Wake Policy behavior
- Prepared Wake Plan validation
- replay determinism
- Wake Outcome derivation
- M7 deterministic Wake Learning policy derivation
- Activation Completion vs Confirmed Wake Success calibration semantics

These tests should be fast enough to run constantly.

## 2. Wake Lab

Wake Lab is a debug/development tool inside the Android project for accelerated behavioral simulation.

It must allow a developer to vary at least:

```text
Wake Policy       default / hard-snoozer / learned fixture / custom fixture
Network           available / unavailable
Voice             available / delayed / failed
Movement          none / pickup / sustained
Capabilities      alarm / FSI / notification / mic fixtures
Active execution  normal / UI recreation / playback recreation fixture
Time scale        real / accelerated
```

Example trace:

```text
00:00 ALERTING    AlarmTriggered
00:08 ENGAGING    UserInteracted
00:24 ACTIVATING  AskToMove
00:40 ACTIVATING  MeaningfulMovement
00:48 ORIENTING   Activation Completion
00:57 FINISHED    runtime completion
later             calibration: GOT_UP / RETURNED_TO_BED / skipped
```

### Replay

Stored semantic Wake Inputs from a real session should be exportable/replayable through Wake Runtime.

This allows a failed or annoying real morning to become a deterministic debugging fixture rather than an anecdote.

Wake Lab is for behavioral development. It **does not validate `AlarmManager` delivery reliability or Android foreground playback survival**.

## 3. Local Android instrumentation

Use emulators and connected devices for platform integration tests such as:

- `setAlarmClock()` registration
- receiver delivery
- Active Wake Execution establishment
- foreground alarm playback lifecycle
- `USAGE_ALARM` audio behavior
- `WakeActivity` launch/presentation behavior
- `WakeActivity` destroy/recreate while sounding
- playback-owner recreation while sounding
- controlled process recreation while active where supported
- duplicate receiver/service-start convergence
- lock-screen state
- notification/full-screen-intent capability paths
- local audio start
- intentional stop terminality
- durable snooze replacement
- stale-trigger suppression
- persistence/reconciliation
- process death before trigger
- Doze/idle where practical
- Direct Boot fixtures/tests

Short-cycle alarms such as `T+30s` and `T+2m` are the primary development tool for the Alarm Kernel.

## 4. Reliability timing measurements

Instrument the wake path with monotonic/wall timestamps where appropriate.

At minimum capture:

```text
scheduled occurrence
OS/receiver trigger
Active Wake Execution established
safe local audio started
usable wake surface/controls visible
first user interaction
component/process recovery start/end when tested
```

Derived reliability measurements include:

```text
trigger delay             = receiver trigger - scheduled occurrence
active execution latency  = active execution start - receiver trigger
local audio latency       = audio started - receiver trigger
wake UI latency           = usable UI visible - receiver trigger
recovery latency          = actionable recovered state - recovery start
```

Do not set aspirational percentile SLOs before M2 measurements exist across representative hardware. Once measured, define and track P50/P95/P99 targets where useful.

## 5. Firebase Test Lab

Firebase Test Lab is the preferred cloud device-matrix service for Android testing when M2 introduces broader platform validation.

Use it for instrumentation tests across combinations of:

- device model
- Android version/API level
- locale
- orientation where relevant

Test Lab supports both virtual and physical Android devices and Android instrumentation tests. Virtual devices are appropriate for regular CI coverage; physical devices are especially important before significant releases and for behavior dependent on real device hardware/lifecycle.

### Initial matrix philosophy

Do not create a huge expensive matrix immediately.

Start with representative coverage, then expand based on bugs/usage:

```text
Pixel / current Android
Samsung / common current Android
one older supported Android API
one aggressive-OEM family when physical coverage is available
```

Before public release, cover the OEM families documented in `18-testing-quality.md`.

Android 17/API 37 belongs in compatibility coverage for background-audio behavior even while M0 initially targets API 36.

## 6. Google Play Internal Testing

Move dogfood builds from sideloaded APKs to Google Play Internal Testing early enough to exercise a production-like Play installation path.

Internal Testing is preferred before closed/open testing because it provides fast private distribution to a small trusted tester group.

This matters for Wake My Way because Play-managed installation/policy/capability behavior can differ from ad-hoc local installs, especially around exact alarms, notifications/full-screen presentation, and target-SDK behavior.

Pipeline direction:

```text
CI release candidate
       ↓
AAB
       ↓
Play Internal Testing
       ↓
founder / trusted-device dogfood
       ↓
Closed Testing when ready
```

## 7. Real overnight dogfood

The final truth test for the product is a real sleeping human using a real Android device overnight.

Dogfood must capture both reliability and product effectiveness:

```text
target occurrence
actual trigger
Active Wake Execution start
audio start
lifecycle recovery events
first engagement
first meaningful activation evidence
Activation Completion
later Confirmed Wake Success calibration when available
snooze behavior
fallback level
Safety Backup usage if enabled
subjective annoyance / perceived agency
learned policy changes/reasons
```

The goal is not merely that the alarm fires or that the runtime says it succeeded. The product thesis is validated only if Wake My Way materially improves the user's ability to actually become active near the intended time without unacceptable annoyance/friction.

### Safety Backup in dogfood

A later conventional Safety Backup may be offered while trust is being built.

It must not hide WMW's own result. Record independently whether:

- WMW triggered
- WMW remained active/actionable
- Activation Completion occurred
- the backup later fired/was needed
- the user eventually confirmed the actual wake outcome

Do not interpret backup usage as a second adaptive schedule.

## 8. Wake Learning v0 validation

M7 is local and does not require cloud deployment.

Validation path:

```text
real/fixture Wake Outcomes
       ↓
semantic calibration where available
       ↓
deterministic learning algorithm version
       ↓
bounded future Wake Policy
       ↓
replay/tests + next dogfood mornings
```

Required properties:

- same source data/version → same output policy
- missing calibration ≠ confirmed success
- Activation Completion + RETURNED_TO_BED remains a false-positive case
- changes are bounded and explainable
- one anomalous morning does not cause wild changes
- annoyance/agency can block aggressive optimization
- invalid/corrupt learned policy falls back to safe default
- one Wake Session never changes policy mid-session

Do not create cloud learning infrastructure before these local properties show real product value.

# Cloud/deployment architecture

## Vercel role

Vercel is the preferred initial deployment platform for **non-critical cloud/web workloads**, once **M8** or another concrete cloud capability justifies creating them.

M7 Wake Learning v0 does **not** justify cloud infrastructure by itself.

Good Vercel candidates:

- landing/marketing site
- privacy/support pages
- beta signup
- non-critical TypeScript API
- Wake Plan generation
- anonymous installation/session endpoints
- ephemeral provider/session credential endpoints
- future sync/profile operations
- preview deployments for web/API pull requests

Not hosted/owned by Vercel:

- Android Alarm Kernel
- `AlarmManager` schedule authority
- Active Wake Execution
- foreground critical alarm playback
- Critical Wake Snapshot
- emergency/bundled alarm audio
- local Wake Runtime
- local Wake Learning v0
- local stop/snooze path
- current occurrence completion authority

## Backend topology when first needed

```text
GitHub repository
       │
       ├── Android app
       │      └── device / Play distribution
       │
       └── future cloud service/web
              └── Vercel
                    │
                    ├── Vercel Functions / Fluid compute
                    ├── Supabase managed PostgreSQL
                    └── external AI/provider APIs
```

The backend remains optional for base wake behavior and initial local adaptation.

## Vercel Functions

Vercel Functions with Fluid compute are a candidate fit for I/O-heavy work such as:

- model/API requests
- database queries
- structured Wake Plan generation
- credential/session brokerage

Platform limits/pricing change. Reconfirm current Function limits when the backend is implemented rather than encoding one 2026 limit as permanent architecture.

## Realtime voice and WebSockets

Vercel WebSocket support is time-sensitive/beta infrastructure as of the planning snapshot. This makes a Vercel WebSocket proxy/voice gateway technically possible, not automatically correct.

It is **not a locked architecture decision** for realtime voice.

**M8** must compare at least:

```text
A. Android → direct realtime provider
B. Android → LiveKit / RTC layer
C. Android → Vercel WebSocket gateway → provider
```

Measure:

- cold connection latency
- time to first speech
- barge-in/interruption quality
- reconnect behavior
- Wi-Fi ↔ mobile transition
- Bluetooth/audio behavior
- connection-duration constraints
- operational complexity
- privacy exposure
- cost

Clients must tolerate reconnects for any proxy/RTC route. Never keep wake authority, Active Wake Execution, current policy authority, or irreplaceable session state only inside a Vercel Function process.

## Database and cloud data platform

If/when cloud persistence is justified, use **Supabase-managed PostgreSQL** with Kysely as the server-side query layer. Supabase Auth is deferred until account identity is needed, and Supabase Storage is deferred until a concrete object-storage feature exists. See ADR-013.

The intended boundary is:

```text
Android → Wake API on Vercel → Kysely → Supabase PostgreSQL
```

Do not couple Android domain reads/writes directly to Supabase database tables. A future direct Supabase Auth session flow is a narrow identity exception, not a replacement for the Wake API domain boundary.

Use separate development/test and production cloud data environments once cloud work begins. Recheck current Supabase serverless connection/pooling guidance before deploying the first backend.

No cloud database is authoritative for:

- current Android Wake Occurrence
- Active Wake Execution
- local stop/snooze
- local M7 Wake Learning v0

## Pull-request / CI topology

Target delivery flow as capabilities appear:

```text
Pull Request
    │
    ├── Kotlin compile
    ├── pure Kotlin tests
    ├── Android lint / formatting
    ├── docs validation
    ├── Android build artifact
    │
    ├── learning fixtures once M7 exists
    ├── instrumentation subset when mature
    │
    └── web/API tests + Vercel Preview when cloud exists

Nightly / scheduled
    │
    ├── broader instrumentation
    ├── Firebase Test Lab virtual matrix
    └── performance/reliability regression where practical

Release candidate
    │
    ├── Firebase Test Lab physical coverage
    ├── AAB
    └── Google Play Internal Testing
             │
             ▼
        real-device dogfood
```

Vercel Preview Deployments apply to web/API changes, not to the Android APK/AAB itself.

## Failure doctrine

Cloud deployment choices must preserve this invariant:

```text
Vercel unavailable       → alarm still works
PostgreSQL unavailable   → alarm still works
AI provider unavailable  → alarm still works
Internet unavailable     → alarm still works
```

And locally:

```text
WakeActivity recreated   → critical alarm playback still works
realtime voice disabled  → learned local policy still works
cloud unavailable        → M7 local learning remains usable
```

Cloud failure may reduce personalization/realtime richness. It must not prevent local wake delivery, Active Wake Execution, audible fallback, stop/snooze, deterministic Wake Runtime behavior, or the safe local learned/default policy path.

## Source references / time-sensitive platform facts

Recheck these sources when implementing the corresponding milestone:

- Google Play target API requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- Google Play exact-alarm restricted permission policy: https://support.google.com/googleplay/android-developer/answer/16558241
- Android exact alarms: https://developer.android.com/develop/background-work/services/alarms
- Android 17 background audio hardening: https://developer.android.com/about/versions/17/changes/bg-audio
- Vercel WebSocket status/changelog: https://vercel.com/changelog/websocket-support-is-now-in-public-beta
- Vercel Functions overview: https://vercel.com/docs/functions
- Firebase Test Lab Android overview: https://firebase.google.com/docs/test-lab/android/get-started
- Firebase Test Lab instrumentation tests: https://firebase.google.com/docs/test-lab/android/instrumentation-test
- Google Play Internal/Closed/Open Testing: https://support.google.com/googleplay/android-developer/answer/9845334

Platform capabilities, pricing, quotas and beta status are time-sensitive. Reconfirm them at implementation/release time rather than treating this document as an eternal vendor specification.

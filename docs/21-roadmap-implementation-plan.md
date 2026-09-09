# Implementation roadmap

## Build philosophy

Do not start with an AI demo. Build the deepest trust-critical behavior first.

```text
reliable native wake occurrence
→ deterministic Wake Runtime
→ physical activation evidence
→ local character experience
→ Tomorrow Contract / prepared plan
→ realtime voice
→ context
→ learning
```

The roadmap is evidence-driven. Later architecture is intentionally left unbuilt until earlier milestones expose real needs.

# M0 — Foundation

## Build

Create only:

```text
:app
:wake-core
:benchmark
```

Set up:

- Kotlin / Compose / Coroutines
- Gradle version catalog
- Android Lint + formatting
- unit-test baseline
- CI build/test/lint
- manual composition root
- versioned Android SDK settings after current Play/API recheck

Do **not** add backend, Hilt, Navigation framework, analytics, generic provider interfaces, or unused feature modules in M0.

## `:wake-core`

Pure Kotlin foundations:

- typed IDs/value objects
- `WallClock` and monotonic elapsed-time seam where deterministic testing requires it
- `WakeSchedule`
- `WakeOccurrence`
- one-active-schedule V1 invariant
- recurrence/next-occurrence calculation
- timezone/DST policy and property tests
- canonical Wake Runtime types may be introduced only when M3 begins

### Exit criteria

- clean build on real Android project
- `:wake-core` has no Android/framework dependencies
- recurrence/timezone/DST suite passes
- docs/ADRs match actual package/module shape

# M1 — Deep Alarm Kernel

Implement one caller-facing Alarm Kernel capability that owns the entire scheduling invariant rather than exposing orchestration steps.

Internally it may use:

- Room for normal credential-protected state
- device-protected Critical Wake Snapshot
- `AlarmManager.setAlarmClock()`
- `PendingIntent` identity/versioning
- receiver/notification/audio components
- exact-alarm capability handling
- dedicated `WakeActivity`
- bundled emergency audio
- stop/cancel
- exact snooze replacement
- reconciliation after schedule edits/time changes/reboot/app start

The app UI should call a small operation such as "commit this Wake Schedule / next occurrence" and receive a durable outcome/readiness result. It must not manually coordinate `Room → snapshot → AlarmManager` ordering.

### Exit criteria

On a real locked device:

```text
set schedule
close app / process may die
next occurrence fires
safe audible alarm starts locally
user has accessible stop/snooze controls
snooze creates a new exact occurrence before current attempt ends
```

No network/backend/AI dependency.

# M2 — Reliability Harness + Android envelope

Build the Alarm Lab and exercise the real platform contract before adding behavioral intelligence.

Required scenarios:

- T+30s / T+2m repeated alarm cycles
- process death
- Doze/idle
- locked and already-unlocked presentation
- notification permission denied
- full-screen-intent unavailable/revoked
- reboot after unlock
- **Direct Boot before first unlock** using device-protected snapshot and generic safe audio
- timezone/wall-clock change
- prepared/private data unavailable
- duplicate trigger/idempotency
- Android 15+ explicit Force Stop: document expected non-delivery limitation and verify repair on next launch rather than asserting an impossible guarantee
- Android 17/API-37 audio behavior compatibility testing when available

Measure real trigger → audio and trigger → user-control latency distributions. Only after measurement set percentile reliability targets.

### Exit criteria

Wake can explain/diagnose whether the next occurrence is **Wake Ready**, and repeated tests show the Alarm Kernel contract is stable enough to build behavior above it.

M2 should also establish the first automated device-matrix path. Start with local instrumentation and a small Firebase Test Lab virtual matrix; add physical cloud-device coverage before significant release candidates. See `32-testing-and-deployment-topology.md`.

# M3 — Wake Runtime

Introduce the deep pure-Kotlin in-session runtime.

Canonical phases:

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

Implement:

- typed `WakeInput`
- typed `WakeDirective`
- versioned `WakePolicy`
- deterministic transition function/runtime
- internal activation-evidence model
- escalation/intervention decisions inside the runtime
- replayable typed session timeline
- invariant/property tests
- Android directive executor in `:app`

Do not create public `WakeConfidenceEstimator`, `StrategyPolicy`, or generic state-machine service layers unless implementation later proves a separate contract has value.

### Exit criteria

A completely offline scripted wake session runs end to end and replaying the same input timeline produces the same directives/outcome.

# M4 — Motion evidence

Add Android motion observations as thin platform inputs to Wake Runtime:

- pickup detection
- orientation change
- meaningful/sustained movement
- stationary periods
- feature extraction and debouncing

No step/activity-recognition permission yet. No raw sensor stream persistence.

### Exit criteria

The same Wake Policy produces meaningfully different directives for no-motion vs sustained-activation scenarios, with behavior covered at the Wake Runtime contract rather than an internal confidence implementation.

# M5 — Alfred local character

Implement the first presentation layer without AI dependency:

- `SpeechIntent` catalog
- Alfred voice/tone boundaries
- curated local phrase variation
- deterministic/seedable phrase choice where useful for tests
- local speech/audio playback path

### Exit criteria

Multiple dogfood mornings feel varied enough to assess UX while Wake behavior remains deterministic and fully offline.

# M6 — Tomorrow Contract + Prepared Wake Plan

Implement:

- Tomorrow Contract text UI first
- private local model/storage
- Prepared Wake Plan structure and versioning
- deferrable WorkManager preparation
- prepared safe local lines/audio
- checksum/fallback handling

Sensitive content remains credential-protected and is never required by Direct Boot alarm delivery.

### Exit criteria

A night-before intention influences the morning after preparation, while disabling network at wake time still produces a complete local wake attempt.

# M7 — Realtime voice architecture spike

Only now create isolated prototypes and measure:

- direct OpenAI realtime
- LiveKit if it may provide meaningful RTC/session leverage
- ElevenLabs role where character quality warrants it

Measure cold connection, first response, barge-in, reconnect, Wi-Fi/mobile transitions, Bluetooth, echo behavior, operational complexity, privacy, licensing, and cost.

The spike decides which **real seam** is needed. Write/update ADR-008 with measured evidence.

Include three transport/deployment shapes in the measurements when practical:

```text
Android → direct realtime provider
Android → LiveKit / RTC layer
Android → Vercel WebSocket gateway → provider
```

Vercel WebSocket support is a Public Beta capability as of the current planning date, so convenience alone is not sufficient evidence to select it.

# M8 — Realtime conversation

Integrate the selected implementation without changing Wake Runtime authority.

- microphone only after the foreground/visible wake interaction permits it
- constrained `SpeechIntent` rendering
- barge-in
- timeouts/reconnect
- budget limits
- derived semantic inputs rather than raw transcript persistence
- immediate local fallback

### Exit criteria

Realtime conversation enriches the session but turning the provider/network off leaves the Wake Runtime and Alarm Kernel fully functional.

# M9 — Useful context

Add optional context only after basic waking works:

- read-only Android Calendar Provider
- narrow relevance window
- local/minimized relevance extraction
- weather only when it changes morning action
- user-selected city or one-time/foreground approximate location

Do not create a provider abstraction until a chosen external source or substitution need makes the seam real.

# M10 — Wake Learning

Use prior outcomes to derive future policy/profile changes:

- Wake Outcome derivation
- versioned profile/policy snapshots
- explainable effectiveness features
- annoyance/agency constraints

Learning is off-session and never required for the current Wake Occurrence.

No ML until real data shows deterministic policy has a material limitation.

# M11 — Dogfood hardening

Move regular founder/trusted-user distribution to Google Play Internal Testing rather than relying only on sideloaded APKs. Daily real-device use drives product changes. Capture:

- alarm trust
- annoyance/repetition
- time to engagement
- time to meaningful movement
- premature activation assumptions / return-to-bed proxies
- snooze patterns
- voice latency/fallback
- battery/lifecycle/device issues

Introduce sanitized Sentry/PostHog only when they solve a concrete dogfood diagnosis/measurement need.

# M12 — Closed beta readiness

- promote proven Internal Testing pipeline into Closed Testing workflow
- privacy policy / processor list
- Play declarations for exact alarm/full-screen/notifications
- GDPR export/delete direction for cloud data
- accessibility audit
- support diagnostics
- OEM matrix
- provider cost limits/kill switches
- trademark/domain finalization
- billing only if retention and willingness-to-pay evidence justify it

## Explicitly deferred

- iOS
- KMP
- multiple independent Wake Schedules/alarms
- wearables/smart home
- email/news briefing
- QR/photo missions
- sleep-stage tracking
- dozens of characters
- gamification economy
- ML wake policy

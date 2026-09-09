# Testing and quality strategy

## Why testing is different here

A normal application bug may cause a refresh. An alarm bug can cause a missed interview, flight, or workday. Reliability testing is therefore product development, not only QA.

Tests should target **stable contracts and invariants**, not private helper topology. If an internal confidence calculation can be refactored without changing Wake Runtime behavior, tests should not make that refactor expensive.

## Test layers

### 1. Pure Kotlin behavior tests

Primary contract-level coverage for:

- Wake Schedule / Wake Occurrence recurrence
- timezone/DST resolution
- Wake Runtime transitions and directives
- Wake Policy versions
- snooze policy behavior
- Prepared Wake Plan validation
- Wake Outcome/profile derivation when introduced
- wall vs monotonic clocks/timeouts

Internal activation-evidence heuristics are tested through Wake Runtime scenarios unless they become a genuinely reusable, stable contract.

### 2. Alarm Kernel contract/integration tests

Exercise the deep module's promises rather than individual storage wrappers:

- committing a schedule produces durable next-occurrence readiness
- failure before durable snapshot/OS registration cannot report success
- reconciliation is idempotent
- snooze replacement is durable before current attempt can finish
- corrupt/missing normal state can fall back to the minimal Critical Wake Snapshot where allowed
- sensitive data never enters device-protected critical state

Room migration/schema tests remain where Room is used.

### 3. Android instrumentation

Real platform behavior:

- exact `setAlarmClock()` scheduling
- locked/unlocked presentation behavior
- process death
- Doze/idle
- notification permission states
- full-screen-intent capability states
- reboot and Direct Boot
- time/timezone changes
- audio routing

### 4. Physical-device dogfood

No emulator can represent the trust requirement of a real morning. Daily real-device use is mandatory before beta.

## Runtime invariants

Examples:

```text
FINISHED never transitions back to an active phase.
ORIENTING is reachable only after the configured activation criterion is met.
Confirmed snooze produces a future Wake Occurrence before the current session finishes.
AI/provider failure cannot itself dismiss/finish the wake attempt.
Network state cannot prevent local Alarm Kernel wake start.
Duplicate platform delivery cannot create duplicate destructive actions.
The same Wake Policy + ordered Wake Inputs produces the same Wake Directives/outcome.
```

## Required Alarm Kernel scenarios

```text
T+30 second alarm
T+2 minute alarm
screen locked
screen already unlocked/in use
app process dead
Doze/idle
notification permission denied
full-screen intent unavailable/revoked
no network/backend
missing prepared/private content
missing microphone permission
normal database unavailable
wall-clock change
timezone change
DST gap/overlap
rapid edit/cancel
snooze then process killed
duplicate receiver delivery
```

### Reboot / Direct Boot

Must explicitly test:

1. schedule wake
2. reboot device
3. **do not unlock after reboot**
4. occurrence fires using device-protected Critical Wake Snapshot
5. generic safe bundled/brand alarm is audible
6. no Tomorrow Contract/calendar/private personalized content is accessible
7. after unlock, normal state reconciles

### Explicit Force Stop

On Android versions where Force Stop cancels pending intents, the correct test is **not** "alarm still fires".

Test instead:

1. schedule occurrence
2. Force Stop app through system UI
3. demonstrate/document expected platform limitation
4. relaunch app
5. detect/reconcile where supported
6. do not report Wake Ready until the next occurrence is successfully restored

This keeps our reliability claims honest.

## Android-version forward compatibility

Before targeting new major Android versions, include platform-specific behavior checks. Android 17/API 37 background-audio restrictions are a specific compatibility area for alarm/audio tests when relevant to the build target.

## OEM matrix

Before public release test real devices from at least:

- Google Pixel
- Samsung
- Xiaomi/Redmi
- OnePlus/Oppo
- Motorola

## Wake Lab

Build debug tooling early enough to test behavior without waiting for mornings.

Controls should include:

```text
Wake Policy: default / hard-snoozer / custom fixture
Network: online/offline
Voice: available / timeout / failure
Movement: none / pickup / sustained
Capabilities: exact alarm / FSI / notification / mic fixtures
Speed: normal / accelerated
```

Timeline should use canonical phases/inputs/directives, for example:

```text
00:00 ALERTING — AlarmTriggered
00:10 ENGAGING — UserInteracted
00:24 ACTIVATING — directive AskToMove
00:41 ACTIVATING — MeaningfulMovement
00:48 ORIENTING — activation criterion met
00:57 FINISHED — completion
```

Replay stored typed session timelines through Wake Runtime.

## UI visual tests

Use screenshot regression only for high-value stable states:

- Wake Ready / not-ready
- Emerging wake surface
- Engaging/Activating wake surface
- intentional snooze confirmation
- orientation/completion
- permission/capability repair
- large font/TalkBack-relevant layouts
- RTL when Hebrew UI ships

Do not snapshot every component merely to increase test count.

## Performance

Use Macrobenchmark/Baseline Profiles where they provide real signal.

Measure distributions for:

- OS trigger → safe audible local output
- OS trigger → usable wake controls/presentation
- WakeActivity cold start
- local critical snapshot read
- ordinary app cold start separately

Set actual percentile targets only after M2 measurements across representative devices.

## CI

Pull request baseline:

```text
compile
pure Kotlin tests
Android lint / formatting
documentation link check
Room schema/migration checks once Room exists
build debug/release candidate as appropriate
```

Later, when those systems exist:

```text
backend tests / OpenAPI validation
screenshot regression
instrumentation suites
performance regression
license/SBOM checks
```

## Cloud device matrix and Play-distributed dogfood

The canonical end-to-end testing/deployment topology lives in [`32-testing-and-deployment-topology.md`](32-testing-and-deployment-topology.md).

Use Firebase Test Lab once M2 has stable instrumentation coverage:

- virtual-device matrices for regular CI/nightly coverage
- physical devices before significant releases and for hardware/lifecycle-sensitive behavior
- representative matrices first; expand from real bugs and user/device distribution

Move from local APK to Google Play Internal Testing early enough to exercise Play-managed installation/alarm/full-screen/notification behavior in a production-like path. Internal Testing is the default dogfood distribution channel before wider Closed Testing.

The final reliability/product validation remains real overnight use on physical Android devices. Passing JVM, Wake Lab, emulator or cloud-device tests does not replace real-morning dogfood.

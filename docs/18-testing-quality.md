# Testing and quality strategy

## Why testing is different here

A normal application bug may cause a refresh. An alarm bug can cause a missed interview, flight, or workday. Reliability testing is therefore product development, not only QA.

Tests should target **stable contracts and invariants**, not private helper topology. If an internal activation calculation can be refactored without changing Wake Runtime behavior, tests should not make that refactor expensive.

Wake My Way has two separate trust questions:

```text
Did the Wake Occurrence fire reliably?
Did the active alarm remain safe/actionable until intentional termination?
```

Both must pass before behavioral intelligence can be trusted.

## Test layers

### 1. Pure Kotlin behavior tests

Primary contract-level coverage for:

- Wake Schedule / Wake Occurrence recurrence
- timezone/DST resolution
- Wake Runtime transitions and directives
- Wake Policy versions
- snooze policy behavior
- Prepared Wake Plan validation
- Wake Outcome derivation
- deterministic Wake Learning v0 policy updates once introduced
- Activation Completion vs Confirmed Wake Success calibration handling
- wall vs monotonic clocks/timeouts

Internal activation-evidence heuristics are tested through Wake Runtime scenarios unless they become a genuinely reusable, stable contract.

### 2. Alarm Kernel contract/integration tests

Exercise the deep module's promises rather than individual storage wrappers:

- committing a schedule produces durable next-occurrence readiness
- failure before durable snapshot/OS registration cannot report success
- reconciliation is idempotent
- safe Active Wake Execution begins for the correct occurrence
- duplicate starts converge on one active playback owner
- `WakeActivity` recreation does not own/silence critical playback
- active occurrence recovery does not resurrect stopped/replaced occurrences
- snooze replacement is durable before current attempt can finish
- corrupt/missing normal state can fall back to the minimal Critical Wake Snapshot where allowed
- sensitive data never enters device-protected critical state

Room migration/schema tests remain where Room is used.

### 3. Android instrumentation

Real platform behavior:

- exact `setAlarmClock()` scheduling
- locked/unlocked presentation behavior
- foreground alarm playback lifecycle
- `USAGE_ALARM` audio behavior
- Activity destroy/recreate while sounding
- controlled process death/recreation while sounding
- duplicate service/receiver delivery
- stop/snooze lifecycle terminality
- Doze/idle
- notification permission states
- full-screen-intent capability states
- reboot and Direct Boot
- time/timezone changes
- audio routing/Bluetooth/calls

### 4. Physical-device dogfood

No emulator can represent the trust requirement of a real morning. Daily real-device use is mandatory before beta.

Dogfood validates both technical reliability and whether Activation Completion corresponds to actual Wake Success.

## Runtime invariants

Examples:

```text
FINISHED never transitions back to an active phase.
ORIENTING is reachable only after the configured activation criterion is met.
Activation Completion is not automatically treated as Confirmed Wake Success.
Confirmed snooze produces a future Wake Occurrence before the current session finishes.
AI/provider failure cannot itself dismiss/finish the wake attempt.
Network state cannot prevent local Alarm Kernel wake start.
Duplicate platform delivery cannot create duplicate destructive actions.
The same Wake Policy + ordered Wake Inputs produces the same Wake Directives/outcome.
A Wake Session uses one immutable policy version from start to finish.
```

## Active Wake Execution invariants

```text
WakeActivity recreation cannot silence the only critical alarm output.
One occurrence cannot own two overlapping critical playback streams.
Stop is terminal and idempotent for the active occurrence.
A successfully replaced/snoozed occurrence cannot resurrect.
Stale trigger identity cannot attach to a newer occurrence.
Process/component recovery returns to one safe locally actionable state where Android permits.
Terminal paths release service/audio/power resources.
```

## Required Alarm Kernel scenarios

```text
T+30 second alarm
T+2 minute alarm
screen locked
screen already unlocked/in use
app process dead before trigger
WakeActivity destroyed/recreated while alarm sounds
playback owner recreated while alarm sounds
controlled process kill/recreation while alarm sounds
duplicate receiver/service start
stop then recreate
snooze then recreate/process kill
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
duplicate receiver delivery
```

### Active execution kill/recovery

Must explicitly test at least:

1. schedule a near-term occurrence
2. let it fire and verify safe local alarm audio starts
3. destroy/recreate `WakeActivity`
4. verify audio remains present and controls reconnect correctly
5. recreate/restart the playback owner through test tooling
6. verify no duplicate overlapping audio is created
7. perform Stop and repeat recreation
8. verify the stopped occurrence does not resurrect
9. repeat with successful Snooze replacement
10. verify only the Snooze Occurrence remains authoritative

Where Android intentionally prevents recovery (for example explicit Force Stop), test and document the platform limitation instead of fabricating a pass.

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

On Android versions where Force Stop cancels pending intents/stops the package, the correct test is **not** "alarm still fires".

Test instead:

1. schedule occurrence
2. Force Stop app through system UI
3. demonstrate/document expected platform limitation
4. relaunch app
5. detect/reconcile where supported
6. clear stale active state safely
7. do not report Wake Ready until the next occurrence is successfully restored

This keeps our reliability claims honest.

## Android-version forward compatibility

M0 targets API 36 as the current Google Play baseline. Before targeting each new major Android version, include platform-specific behavior checks.

Android 17/API 37 background-audio restrictions are a specific compatibility area:

- visible Activity vs foreground-service execution
- exact-alarm permission state
- `USAGE_ALARM` audio attributes
- audio focus/volume behavior
- lifecycle after screen/UI changes

Do not raise `targetSdk` without running these wake-path tests.

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
Wake Policy: default / hard-snoozer / learned fixture / custom fixture
Network: online/offline
Voice: available / timeout / failure
Movement: none / pickup / sustained
Capabilities: exact alarm / FSI / notification / mic fixtures
Active execution: normal / activity recreate / playback recreate fixture
Speed: normal / accelerated
```

Timeline should use canonical phases/inputs/directives, for example:

```text
00:00 ALERTING — AlarmTriggered
00:10 ENGAGING — UserInteracted
00:24 ACTIVATING — directive AskToMove
00:41 ACTIVATING — MeaningfulMovement
00:48 ORIENTING — Activation Completion
00:57 FINISHED — runtime completion
later          — calibration: ReturnedToBed / GotUp / skipped
```

Replay stored typed session timelines through Wake Runtime.

## Wake Learning v0 tests

When M7 begins, test learning as a deterministic transformation over safe prior outcomes.

Required properties:

- same source outcomes + algorithm version produce the same next policy
- one anomalous morning does not necessarily cause a large policy change
- parameter changes stay within explicit safe bounds
- policy versions are immutable once used by a Wake Session
- learning cannot change critical reliability/safety/stop/privacy rules
- learned policy validation failure falls back to default policy
- reset restores default behavior without breaking schedule/history
- learning considers friction/annoyance and Confirmed Wake Success calibration, not Activation Completion alone
- adaptations do not oscillate rapidly under stable input fixtures

## Calibration testing

Use fixture cases where phone-observable activation and later feedback disagree:

```text
Activation Completion = true
Calibration = RETURNED_TO_BED
```

Learning/analytics must preserve this as a false-positive activation case rather than rewriting it as success.

Also test missing/skipped calibration so absence of feedback is not treated as automatic confirmation.

## UI visual tests

Use screenshot regression only for high-value stable states:

- Wake Ready / not-ready
- Emerging wake surface
- Engaging/Activating wake surface
- intentional snooze confirmation
- intentional stop interaction
- orientation/completion
- permission/capability repair
- optional Safety Backup setup during dogfood if implemented
- large font/TalkBack-relevant layouts
- RTL when Hebrew UI ships

Do not snapshot every component merely to increase test count.

## Performance

Use Macrobenchmark/Baseline Profiles where they provide real signal.

Measure distributions for:

- OS trigger → safe audible local output
- OS trigger → foreground active execution established
- OS trigger → usable wake controls/presentation
- component recovery → usable active controls/audio
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
Wake Learning deterministic fixtures
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

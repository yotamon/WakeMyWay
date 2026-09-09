# M2 Reliability Harness runbook

M2 turns the Alarm Kernel from a build-valid implementation into an evidence-backed wake system. The harness is intentionally local, non-sensitive, and failure-isolated. Reliability instrumentation may observe the critical path, but it must never become required for the alarm to fire.

## What is implemented in the first M2 slice

- bounded device-protected reliability journal
- replayable per-occurrence event timeline
- expected wake records before target time
- target → receiver → foreground → audio → UI → terminal timestamps
- elapsed-realtime latency measurement for post-trigger stages
- service recreation counters
- reconciliation events
- exact-alarm / notification / full-screen capability snapshots
- one-shot lab schedules so T+2m tests do not accidentally create tomorrow's alarm
- snooze replacement evidence that remains inside the same one-shot wake chain
- human-readable shareable reliability report
- Android instrumentation tests for:
  - Direct-Boot critical snapshot serialization
  - cancellation tombstone / stale occurrence rejection
  - one-shot snooze completion
  - replayable reliability event order and report generation
- CI compilation and artifact upload for both the app APK and instrumentation APK

## What this does not prove

A green CI run does **not** prove that a locked physical phone will wake reliably. M2 remains open until real device evidence exists.

## Canonical evidence ladder

```text
:wake-core JVM tests
        ↓
Android lint + APK build
        ↓
instrumentation APK compiles
        ↓
dedicated emulator / Firebase Test Lab execution
        ↓
physical locked-device T+2m cycles
        ↓
reboot / Direct Boot / Doze / permission scenarios
        ↓
OEM matrix
        ↓
Play Internal dogfood
```

## Reliability targets

- every expected lab wake becomes diagnosable as delivered or missed
- receiver → audible local alarm target: P99 < 1 second on supported devices
- receiver → WakeActivity visible target: P95 < 1 second where full-screen presentation is permitted
- stale or cancelled occurrences never become active
- snooze replacement exists durably before current playback ends
- service/process recreation does not silence an active wake
- reboot-before-unlock restores a future wake using non-sensitive device-protected state

These are targets, not claims. Percentiles are not published until enough real cycles exist.

## Founder T+2m cycle

1. Install the latest debug APK from CI.
2. Open **Wake Alarm Lab**.
3. Confirm `Wake Ready` and inspect Exact Alarm / Notification / Full Screen capability facts.
4. Tap **Run one-shot T+2m wake**.
5. Lock the phone and leave the app in background.
6. Observe whether local alarm audio starts and whether the wake surface appears.
7. Use **Stop** or **Snooze**.
8. Reopen Wake Alarm Lab.
9. Tap **Refresh evidence**.
10. Inspect the session state, latencies, terminal action, recovery count, and recent event chain.
11. Use **Share reliability report** to export the human-readable evidence.

The one-shot schedule must be disabled after final Stop or an irrecoverably missed wake. A Snooze remains part of the same one-shot wake chain.

## Scenario matrix

| ID | Scenario | Required evidence | Pass condition |
|---|---|---|---|
| `NORMAL_T_PLUS_2M` | locked phone, ordinary background state | EXPECTED → RECEIVER → FOREGROUND → AUDIO_STARTED; UI if permitted | audible wake, no silent failure |
| `SNOOZE_REPLACEMENT` | snooze active alarm | original SNOOZED + new EXPECTED record | replacement exists before current execution stops |
| `STOP_RECREATION` | stop then process/service recreation | STOPPED terminal event | stale occurrence cannot restart playback |
| `SERVICE_RECREATION` | kill/recreate playback owner while active | SERVICE_RECOVERED count/event | local playback resumes from durable active state |
| `RECONCILE_TIME_CHANGE` | wall-clock/timezone change before wake | RECONCILED event | future occurrence is repaired and Wake Ready is truthful |
| `REBOOT_UNLOCKED` | reboot, then unlock before target | reconciliation + eventual receiver/audio | future wake preserved |
| `DIRECT_BOOT` | reboot and remain locked until wake | locked-boot reconciliation + eventual local wake | no credential-protected/private dependency |
| `EXACT_ALARM_UNAVAILABLE` | exact alarm capability unavailable | CAPABILITIES event + Wake not ready | no crash and no false readiness |
| `FULL_SCREEN_UNAVAILABLE` | full-screen capability unavailable | CAPABILITIES event | audio still works; UI degradation is truthful |
| `DOZE_IDLE` | device idle/Doze before target | normal wake timeline | exact wake remains timely |

## Instrumentation tests

The test APK is built with `AndroidJUnitRunner` and isolates test state from founder state using dedicated Critical Wake Snapshot and reliability-journal storage names.

Current tests:

- `CriticalWakeStoreInstrumentedTest`
- `AlarmKernelInstrumentedTest`
- `WakeTimingTraceInstrumentedTest`

CI currently **compiles and packages** these tests. Device execution is intentionally a separate evidence gate because GitHub-hosted compilation cannot simulate OEM power management, real lock-screen behavior, reboot-before-unlock, or physical audio routing.

## CI artifacts

Every Android PR should produce:

- `app-debug.apk`
- `app-debug-androidTest.apk`

The instrumentation APK is evidence tooling, not a production dependency.

## Reliability journal privacy rules

The journal may store only operational evidence such as:

- occurrence/schedule technical IDs
- scenario ID
- target and stage timestamps
- terminal action
- service recovery count
- capability booleans
- reconciliation reason
- device model / SDK in exported reports

It must never store:

- Tomorrow Contract text
- calendar titles/content
- transcript/audio
- prompts
- microphone data
- tokens or secrets
- personalized private speech content

## Report interpretation

Possible derived states include:

- `EXPECTED`
- `MISSED_RECEIVER`
- `RECEIVED_NO_AUDIO_YET`
- `AUDIO_TIMEOUT`
- `AUDIBLE_NO_UI_YET`
- `UI_TIMEOUT`
- `ACTIVE`
- `STOPPED`
- `SNOOZED`

A missing full-screen UI is only classified as failure when full-screen presentation was expected for that occurrence. Audio reliability remains separately measurable.

## M2 exit gate

M2 is not complete until:

1. instrumentation tests execute successfully on a dedicated device/emulator path;
2. repeated locked-screen T+2m cycles generate persistent evidence;
3. Stop/Snooze resurrection checks pass;
4. service recreation recovery is demonstrated;
5. reboot / Direct Boot / time-change cases are exercised;
6. at least the founder's primary Android device has measured trigger → audio and trigger → UI data;
7. failures can be exported as a scenario + ordered event timeline;
8. issue #5's physical-device exit criterion can be closed with measured evidence.

# M1 implementation progress

## Implemented and CI-proven in PR #8

- deterministic snooze occurrence planning in `:wake-core`
- versioned Direct-Boot critical wake snapshot
- atomic device-protected persistence
- exact `AlarmManager.setAlarmClock()` registration
- durable registration confirmation for Wake Ready
- crash-safe schedule replacement: new persisted authority is written before obsolete OS registration is cancelled
- crash-safe cancellation tombstone so cancelled alarms cannot be resurrected by reconciliation
- collision-free occurrence-specific `PendingIntent` identity, with cleanup for the earlier M1 debug identity
- idempotent active-occurrence transition
- foreground service recovery from the durable active occurrence after process/service recreation
- durable Stop and Snooze replacement semantics
- system reconciliation after boot/time/timezone/package changes
- graceful exact-alarm capability loss without crashing or reporting false Wake Ready
- thin alarm trigger receiver
- foreground Active Wake Execution owner
- bundled local WAV playback with `USAGE_ALARM` and `ToneGenerator` fallback
- dedicated Direct-Boot-aware lock-screen `WakeActivity`
- founder T+2 minute scheduling test and Wake Ready diagnostics
- local non-sensitive timing trace for target → receiver → foreground → audio → UI
- ADR-015 alarm permission/execution baseline
- GitHub CI green for docs validation, `:wake-core` tests, Android lint, debug APK assembly, and APK artifact upload

## Evidence still required before the reliability milestone can close

- add Android instrumentation/reliability-harness coverage where feasible
- run physical-device locked-screen T+2m tests
- capture real trigger → receiver → playback → UI timings
- verify Stop/Snooze under process recreation and repeated cycles
- exercise reboot, Doze, permission loss/restoration, timezone changes, and OEM-specific behavior

The implementation is build-valid and locally recoverable by design. Physical-device reliability is intentionally not claimed until the reliability harness and device matrix produce evidence.

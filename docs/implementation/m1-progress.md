# M1 implementation progress

## Implemented in this branch

- deterministic snooze occurrence planning in `:wake-core`
- versioned Direct-Boot critical wake snapshot
- atomic device-protected persistence
- exact `AlarmManager.setAlarmClock()` registration
- durable registration confirmation for Wake Ready
- idempotent active-occurrence transition
- durable Stop and Snooze replacement semantics
- system reconciliation after boot/time/timezone/package changes
- thin alarm trigger receiver
- foreground Active Wake Execution owner
- `USAGE_ALARM` local playback with deterministic fallback
- dedicated lock-screen `WakeActivity`
- founder T+2 minute scheduling test and Wake Ready diagnostics
- ADR-015 alarm permission/execution baseline

## Still required before M1 can close

- CI-green Android build/lint/tests for the branch
- fix any platform/lint findings from the real toolchain
- replace the temporary raw seed with the final valid bundled alarm audio asset
- add Android instrumentation coverage for kernel/snapshot/component behavior where feasible
- confirm Stop/Snooze do not resurrect old occurrences under recreation
- run the first physical-device T+2m locked-screen test
- record trigger → receiver → playback → UI timestamps

Physical-device reliability evidence is intentionally not claimed until those tests run.

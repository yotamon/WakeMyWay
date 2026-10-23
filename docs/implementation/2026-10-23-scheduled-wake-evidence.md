# Ordinary scheduled wake delivery evidence (W0/W1)

**Date:** 2026-10-23  
**Status:** Implementation PR; no physical acceptance asserted  
**Parent program:** [Wake Experience Excellence](2026-10-23-wake-experience-excellence-plan.md)  
**Primary gate:** TRUST #9 (physical native alarm delivery); proof also supports PROVE #87.

## Why this slice is necessary

Previously `WakeTimingTrace.expected()` was populated explicitly for developer Wake Lab scenarios. An ordinary alarm could create a trace only after its receiver or execution started. A wake that **never fired** might not appear in the diagnostic history at all, so sharing a report could give false reassurance.

## Actual implementation

- `AlarmRegistrar.register()` records an expected occurrence **after** `AlarmManager.setAlarmClock()` accepts the request; failures in noncritical diagnostics are isolated from kernel authority.
- The same tracker is used for real primary alarms, durable Snooze replacements, recurring next occurrences and registration after reboot/reconciliation. No alternate alarm scheduling or background worker was added.
- Repeated registration preserves the first EXPECTED time and does not append duplicate expectation events. A later Wake Lab scenario label is retained when registrar reconciliation happens.
- `AlarmRegistrar.cancel()` marks a pending non-delivered occurrence CANCELLED so a user-edited/disabled alarm does not become a false missed-alarm report. Receiver-delivered occurrences and post-due misses are **not erased** by later cancellations.
- If a canceled future occurrence is registered again, its pending state is reopened and a RE_REGISTERED event remains in the report. This prevents a reconciled valid wake from staying labeled canceled.
- The report includes bounded counts of tracked expected, canceled-before-delivery and missed-after-grace occurrences. It does **not** claim these counts represent the entire phone history, guarantees speaker audibility or proves the OS delivered every alarm.
- All observations remain in the existing bounded, device-protected, privacy-safe reliability journal (up to 24 sessions), with no wake content, transcripts, microphone data, analytics export, identity expansion or network dependency.
- The existing diagnostic state enum gains `CANCELLED`; there is **no change** to Alarm Kernel status, Wake Runtime semantics or user-visible terminal behavior.

## Safety / evidence caveats

The diagnostic journal is best-effort, and its own write failure cannot invalidate a scheduled alarm. If an app is force-stopped, data wiped, system clock changed, or the journal fills up, report coverage may be incomplete; a silent journal is **not** evidence that a wake was delivered. A missed-receiver label means there is an expected occurrence with no receiver observation after a 60-second grace, not a conclusive determination of Android's underlying cause. A cancellation after a wake is already overdue must not wipe the failure evidence.

A normal alarm remains a separate durable Kernel schedule. No new service runs, no device alarm is rearmed after Stop, and no critical reliability operation needs the diagnostic journal to be healthy.

## Acceptance & phone pilot

Automated:
- Standard Alarm Kernel registration now yields a `SCHEDULED_WAKE` expected event in the existing journal.
- Repeated registration yields only one EXPECTED event; Lab scenario annotation survives.
- Intentional cancellation yields CANCELLED; re-registration reopens it; overdue absence of receiver is still diagnosable.
- Existing receiver/audio/UI/Stop/Snooze lifecycle coverage remains green.

Physical (founder OnePlus, exact signed installed build):
1. **Without resetting app data**, install the updated signed APK over the existing one. Confirm existing alarms, selected wake time and readiness persist. Keep a conventional backup while confidence is being established.
2. Configure a real wake for the following morning (do not substitute Test Wake Now). In Wake Alarm Lab, use **Refresh evidence** before sleep. The scheduled occurrence should appear as `SCHEDULED_WAKE EXPECTED`.
3. Sleep normally; after waking, check whether native alarm was audibly present, whether full-screen controls appeared, whether Live was genuinely speaker-audible and whether Stop/Snooze completed.
4. Reopen Wake Alarm Lab → **Refresh evidence** → **Share reliability report**. It should include the same occurrence with EXPECTED → RECEIVER → AUDIO_STARTED, and terminal action. A missing receiver past the grace appears as MISSED_RECEIVER rather than vanishing. A late/failed UI stage may appear separately.
5. In a *noncritical* daytime test, create a separate future wake, disable it, then inspect the report: CANCELLED, **not** MISSED_RECEIVER. Re-enable/reconcile only if desired; verify RE_REGISTERED when the same occurrence is actually re-registered.
6. Submit short reports without private notes or spoken content; keep failed and passing cases. Record device model/OS, exact app version, timezone/clock change, lock/Doze/network/audio route, scheduled vs heard time, and whether a backup was used.
7. Repeat multiple normal mornings; compare the reported delivery stages against your lived experience. A speaker audible alarm may not be fully proven by an AUDIO_STARTED event alone.

**Blocking failures:** missed critical local alarms, unreliable Stop/Snooze, resurrected completed wake, alarm-only silence due to Live failure, or a privacy leak. No general release/physical certification solely on emulator CI.

## Later work explicitly deferred

This is expected-delivery instrumentation, **not** a fix for an observed OS/OEM scheduling failure. After actual missed/late-wake traces, make minimal receiver/Kernel/service fixes with reproduction and stronger physical tests. W2 Live human voice quality and W3 reflex dismissal policies remain separate.

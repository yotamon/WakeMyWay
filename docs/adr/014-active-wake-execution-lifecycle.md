# ADR-014: Active Wake Execution survives UI/process churn

**Status:** Accepted  
**Date:** 2026-09-09

## Context

Scheduling reliability is not sufficient once a Wake Occurrence has fired. The product must also remain an alarm while the active wake experience is running.

A design where `WakeActivity` directly owns critical audio would create a trust gap: Activity recreation, crash, navigation teardown, or process churn could silence the alarm even though the Wake Occurrence fired correctly.

Android 17 also hardens background audio interactions. Apps performing background audio need a visible Activity or a suitable foreground service; apps targeting API 37 gain an exact-alarm/`USAGE_ALARM` exception to the while-in-use capability requirement, but still require a valid foreground execution shape for background audio.

## Decision

The Alarm Kernel owns a distinct **Active Wake Execution** lifecycle after a Wake Occurrence fires.

The initial implementation should use an alarm-appropriate foreground playback service/controller (working name: `AlarmPlaybackService`) with `USAGE_ALARM` audio semantics, while keeping the exact Android component design private to the Alarm Kernel.

Conceptually:

```text
AlarmReceiver
    ↓
Alarm Kernel active-wake start
    ↓
foreground alarm playback owner
    ↓
safe local audio + alarm notification/controls
    ↓
WakeActivity / Wake Runtime enrichment
```

`WakeActivity` is presentation, not lifecycle authority for critical alarm audio.

## Active occurrence state

The Alarm Kernel must persist enough non-sensitive execution identity/status to answer, idempotently:

- which Wake Occurrence is currently active?
- has safe alarm playback already started?
- was the occurrence intentionally stopped?
- has a replacement Snooze Occurrence been durably scheduled?
- should a recreated component resume, attach to, or terminate the active execution?

Do not persist private conversation/transcript state merely to recover critical playback.

## Recovery invariant

If Activity or process state is recreated while an occurrence is actively alerting, the kernel must restore a safe locally actionable alarm state instead of depending on the previous UI/process instance.

At minimum, M1/M2 must test:

```text
alarm fires → audio starts → kill/recreate WakeActivity → alarm remains actionable
alarm fires → audio starts → kill app process through test tooling → recover/continue safely where Android allows
alarm fires → playback owner recreated → no duplicate overlapping alarm audio
stop → component recreation → alarm does not resurrect
snooze scheduled durably → component recreation → old occurrence does not resurrect
```

Explicit user Force Stop remains outside the delivery/execution reliability envelope when Android intentionally stops the package.

## Wake lock / power behavior

Use the smallest platform power primitive proven necessary by measurement. Do not hold broad or indefinite wake locks by default.

The Alarm Kernel owns any required wake-lock acquisition/release ordering so callers cannot leak power or release the alarm-critical execution prematurely.

## Consequences

- critical audio lifetime is decoupled from UI lifetime
- active alarm recovery becomes part of the Alarm Kernel contract rather than an incidental Activity behavior
- Android 17 background-audio compatibility has a clear architecture owner
- service/audio/power complexity stays private inside the deep Alarm Kernel
- M1/M2 gain explicit crash/recreation/idempotency tests

## Sources to revalidate at implementation time

- Android background audio hardening: https://developer.android.com/about/versions/17/changes/bg-audio
- Android exact alarms: https://developer.android.com/develop/background-work/services/alarms

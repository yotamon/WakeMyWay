# WakeMyWay 1.0 physical acceptance

This runbook closes the physical evidence shared by:

- #9 — hostile-condition Alarm Kernel / Active Wake proof
- #97 — release cold-start measurement
- #99 — TalkBack and reduced-motion acceptance

It deliberately keeps these checks on a real founder device. Hosted-emulator timing is not accepted as product-performance evidence, and automated semantics tests do not replace a human TalkBack traversal.

## One-time laptop setup

On Windows:

```powershell
choco install adb -y
git clone https://github.com/yotamon/WakeMyWay.git
cd WakeMyWay
git checkout main
git pull --ff-only
```

Connect the Android phone with USB debugging enabled and authorize the laptop, or pair an existing Wireless Debugging session.

Confirm:

```powershell
adb devices -l
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action preflight
```

Exactly one intended phone should be in the `device` state.

## 1. Machine-verifiable accessibility semantics

```powershell
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action semantics
```

This reruns the TalkBack-critical Compose semantics contract on the physical phone.

## 2. Release cold-start measurement

Configure WakeMyWay normally first. Keep the phone thermally stable and do not interact with it during the run.

```powershell
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action benchmark
```

Retain the generated benchmark JSON/traces. Record device/model, Android build, WMW commit, compilation/startup mode, iteration count and min/median/max startup timing. Median is the primary comparison signal.

## 3. Reduced-motion acceptance

Save the current system scales and force Android's Remove Animations behavior:

```powershell
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action animations-off
```

Check the normal consumer journey plus Active Wake LISTENING, MOVING and ORIENTING. Required information and actions must not depend on animation; decorative Wake Line/state transitions must not keep moving when the platform duration scale is zero.

Always restore the original values:

```powershell
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action animations-restore
```

The script refuses to guess previous values if no saved state exists.

## 4. TalkBack physical traversal

Enable TalkBack from Android Accessibility settings. The helper can record the active service state:

```powershell
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action talkback-status
```

Traverse:

1. onboarding;
2. Alarms list;
3. Create/Edit Alarm;
4. Profile;
5. Insights;
6. Active Wake LISTENING;
7. Active Wake ORIENTING.

Accept only if traversal order is coherent, action labels and selected states are announced, charts/decorative art do not create noisy focus stops, and Stop/Snooze/First Move are quickly reachable.

## 5. Reliability scenario matrix

Use the debug Wake Lab on the founder build and retain the in-app reliability report for each scenario.

The runner supports the hostile-device actions needed by the lab:

```powershell
# after a scenario is scheduled
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action force-idle
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action unidle

powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action kill-process
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action reboot
```

Physical acceptance must cover:

- repeated locked-screen wakes;
- Snooze replacement and post-Stop resurrection;
- service/process recreation;
- reboot then unlock;
- Direct Boot before unlock;
- wall-clock/timezone reconciliation;
- exact-alarm capability loss before a planned wake;
- exact-alarm loss after Active Wake already fired;
- notification/high-priority/full-screen capability loss;
- Doze/idle;
- Bluetooth/audio route;
- on-device voice degradation / network loss;
- motion calibration.

Do not use Force Stop as a proxy for ordinary process recreation.

### Mid-wake fallback evidence

While a wake is active — during the Bluetooth/audio-route, Doze and degraded-device scenarios — capture the execution-layer evidence:

```powershell
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action wake-check
```

The check records the live service state, held wake locks (`activeWakeStartup` during the fire-to-audio window, `activeWakeTone` while the emergency tone path plays), alarm audio-focus ownership and vibrator activity into the evidence bundle.

Acceptance for the hardened fallback layers requires observing, on at least one supported device:

- the repeating haptic pattern is felt while the wake is active, including with the alarm stream muted;
- the looping playback holds CPU across a locked screen (`wake-check` wake-lock lines);
- notification Stop ends the wake and the vibration stops with it;
- Snooze still schedules its durable replacement before the current wake ends.

## 6. Evidence collection

After each meaningful run:

```powershell
powershell -ExecutionPolicy Bypass -File tooling/physical-reliability.ps1 -Action collect
```

The helper records device/build, commit, package state, alarms, notifications, audio route, idle state, accessibility services, animation scales and bounded logcat, and copies benchmark evidence when present.

Also export the in-app Wake Lab report for the matching reliability scenario.

## Acceptance rule

A failure is useful evidence. Do not repeat until green and discard the failing trace.

For every failed wake or accessibility/performance observation, retain the scenario, device/build, exact WMW commit and evidence bundle, then either:

- fix and regression-cover the defect; or
- explicitly accept the limitation in the supported-device/release envelope.

Gate 1 / Gate 2 remain open until the required physical runs are retained.

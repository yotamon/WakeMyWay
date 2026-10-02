# End-to-End Product Behavior Polish Implementation Plan

> **For implementer:** execute inline with TDD. Do not stop between tasks unless a destructive/security-sensitive/shared-side-effect decision is required.

**Goal:** Make existing WakeMyWay behavior coherent, failure-transparent, testable and trustworthy without adding new 1.0 capabilities.

**Architecture:** Keep Alarm Kernel and Wake Runtime authority unchanged. Add typed presentation/diagnostic projections at the Android adapter layer, classify lab sessions before history/learning construction, and reuse existing WakeTimingTrace/Wake Lab rather than creating parallel systems.

**Tech stack:** Kotlin 2.4.20, Jetpack Compose, pure Kotlin `:wake-core`, Android AlarmManager/Direct Boot, JUnit/Robolectric, Roborazzi, Gradle 9.6.1.

**Spec:** `docs/implementation/end-to-end-product-behavior-polish.md`

**Global constraints:**
- no new consumer capability or permission;
- no change to Alarm Kernel or Wake Runtime authority;
- no local TTS fallback after Direct Realtime failure;
- no transcript/private-context diagnostics;
- lab/test wakes cannot become learning evidence;
- all behavior changes require RED→GREEN coverage before implementation.

## Task 1: Truthful wake lifecycle and degradation

**Files:**
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/voice/WakeVoiceSessionController.kt`
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/WakeActivity.kt`
- Add/modify tests under `apps/android/app/src/test/java/com/wakemyway/app/voice/`

**Steps:**
1. Add failing tests for pre-ready STARTING projection and stable alarm-only degradation reason.
2. Add bounded `WakeVoiceDegradationReason` to `WakeVoiceUiState`.
3. Make startup publish STARTING while critical alarm remains audible.
4. Map startup timeout/session failure/transport unavailable to explicit alarm-only reasons; never late-upgrade that occurrence.
5. Update wake copy to distinguish ordinary alarm-only from Live Voice degradation.
6. Run targeted Direct unit tests and compile.

## Task 2: Test Wake isolation and session classification

**Files:**
- Add: `apps/android/app/src/main/java/com/wakemyway/app/WakeSessionMode.kt`
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/WakeSessionViewModel.kt`
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/ui/developer/WakeAlarmLabScreen.kt`
- Modify/add tests near `WakeSessionViewModelTest.kt`

**Steps:**
1. Write failing pure tests proving `lab-*` schedules classify as TEST and normal schedules classify as NORMAL.
2. Make ViewModel factory omit WakeHistorySessionRecorder and post-wake follow-up for TEST while still resolving the current learned policy/preferences.
3. Keep real Alarm Kernel/playback/runtime behavior unchanged.
4. Verify test wake cannot create durable user history or trigger learning refresh through the recorder path.
5. Run targeted tests.

## Task 3: Privacy-safe Wake Session Inspector

**Files:**
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/alarm/WakeTimingTrace.kt`
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/voice/WakeVoiceSessionController.kt`
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/ui/developer/WakeAlarmLabScreen.kt`
- Add/modify WakeTimingTrace tests.

**Steps:**
1. Write failing tests for bounded semantic lifecycle events with no free-form speech payload.
2. Add typed diagnostic events for voice lifecycle and runtime phase changes.
3. Record connecting/ready/listening/speaking/degraded/runtime-phase events best-effort.
4. Expand Wake Lab recent evidence into a readable timeline while keeping existing timing facts.
5. Run targeted tests.

## Task 4: Actionable per-alarm readiness and mutation feedback

**Files:**
- Add/modify readiness projection near `AlarmPresentationCapabilities.kt`
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/ui/navigation/WakeMyWayApp.kt`
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/ui/alarms/AlarmsScreen.kt`
- Modify tests under `apps/android/app/src/test/java/com/wakemyway/app/alarm/`

**Steps:**
1. Write failing pure tests for READY/OFF/NEEDS_ATTENTION projection and blocker priority.
2. Make Today and Alarms consume the same per-alarm readiness semantics.
3. Add a concise Fix affordance for enabled alarms blocked by Android presentation/scheduling prerequisites.
4. Change alarm enable/disable callback to return an acknowledged result and show failure locally rather than swallowing it.
5. Keep repository/kernel rollback behavior as the mutation authority.
6. Run targeted tests and UI compile.

## Task 5: Terminal-action feedback and resolved personalization inspection

**Files:**
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/WakeSessionViewModel.kt`
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/WakeActivity.kt`
- Modify: `apps/android/app/src/main/java/com/wakemyway/app/ui/developer/WakeAlarmLabScreen.kt`
- Modify: `WakeSessionViewModelTest.kt`

**Steps:**
1. Write failing tests proving failed Snooze/Stop remains non-terminal and exposes a bounded UI failure state.
2. Add typed terminal action UI state; suppress duplicate in-flight terminal requests.
3. Surface a concise failure message while keeping Stop/Snooze actionable.
4. Build a read-only Resolved Wake Plan section in Wake Lab from `WakeSessionStrategyResolver` using current preferences, current learned policy and the selected alarm policy.
5. Never render private Tomorrow Contract text in the inspector.
6. Run targeted tests and compile.

## Task 6: Documentation convergence and release-quality verification

**Files:**
- Modify: `docs/17-reliability-failure-modes.md`
- Modify: `docs/18-testing-quality.md`
- Modify: `docs/00-project-status.md`
- Modify documentation index if required.

**Steps:**
1. Replace the stale multi-level voice fallback chain with the current Direct all-or-nothing Realtime policy plus local alarm baseline.
2. Document test-session isolation, resolved-plan inspection and the privacy-safe session timeline.
3. Run the focused unit suites from Tasks 1-5.
4. Run `:wake-core:test`, `:app:compileDirectDebugKotlin`, `:app:lintDirectDebug`, and `:app:assembleDirectDebug`.
5. Run relevant visual tests where the local ARM runtime permits; leave CI authoritative for unsupported native visual execution.
6. Review the complete branch against the spec, fix Critical/Important findings with RED→GREEN tests, and record any deferred minors.

## Shared interfaces / pre-flight

- Task 1 `WakeVoiceUiState` is consumed by Task 5 WakeActivity terminal/degradation copy; keep it presentation-only.
- Task 2 session classification is consumed by Task 3 diagnostics/history behavior; diagnostics stay enabled for TEST even while user history is disabled.
- Task 4 readiness projection is consumed by both Today and Alarms; there must be one blocker priority.
- Task 5 inspector consumes existing WakeSessionStrategyResolver output; it must not create a second strategy calculation contract.

## Review focus

Check specifically for: history contamination from lab schedules; accidental weakening of Stop/Snooze acknowledgement; Realtime reconnect after degradation; raw/free-form diagnostic text; aggregate-vs-per-alarm readiness mistakes; test-mode behavior diverging from production runtime; Compose state that claims a mutation before AlarmProductController acknowledges it.

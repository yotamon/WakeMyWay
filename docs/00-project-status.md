# Project status

**Last updated:** 2026-09-25  
**Product:** WakeMyWay (WMW)  
**Platform:** Android first; optional non-critical Vercel cloud with Supabase as the preferred future managed data/auth platform  
**Current product phase:** WakeMyWay 1.0 paid-launch readiness; product capability scope is frozen by default  
**Current launch program:** #89; canonical plan: `docs/35-paid-launch-readiness.md`  
**Readiness gates:** TRUST #9 → FINISH #59 → PROVE #87 → SELL #88  
**Account backend:** authenticated backup backend code exists; production provisioning and user-facing Sign In remain deferred  
**Physical release gate:** #9  
**Reliability rule:** future scheduling readiness, active execution safety, voice readiness and Snooze readiness are separate predicates  
**Cloud rule:** cloud/account state is never Alarm Kernel or WakeRuntime authority  
**1.0 scope rule:** reliability, defects, polish, accessibility, measured tuning and launch infrastructure may proceed; new product capabilities are deferred unless evidence shows they are required to deliver or sell the existing core promise

## Current product shape

WakeMyWay is a local-first Android wake system built around reliable alarms, a deterministic behavioral runtime and optional conversational/cloud enrichment.

```text
First-run onboarding / Profile defaults / planning Appearance
              ↓
      Alarm library + editor
              ↓ strict new-Wake preflight
AlarmDefinitionRepository · credential protected
              ↓ compile critical policy
Alarm Kernel · schema v3 independent schedule slots
              ↓
AlarmManager.setAlarmClock()
              ↓
         AlarmReceiver
              ↓ active-execution safety check
     AlarmPlaybackService
       ├─ selected bundled WakeMyWay sound
       ├─ notification terminal controls
       └─ full-screen WakeActivity
                   ↓
           WakeSessionViewModel
             ├─ acknowledged Stop/Snooze → Alarm Kernel
             └─ behavioral session
                        ↓
             WakeVoiceSessionController
                        ↓
                   WakeRuntime
          ├─ local Alfred
          ├─ on-device voice replies
          ├─ motion evidence
          └─ optional founder/debug Realtime enrichment

Optional account backup/migration
        │
        └─ normal consumer intent only
           never Direct-Boot / active-wake authority
```

The sunrise-wave identity from PR #51 remains the canonical visual system. Planning and configuration use the selected local planning atmosphere, while Active Wake intentionally follows the fixed authored midnight-to-daylight progression. The same sunrise-wave geometry is used by the launcher identity, system launch treatment, Wake Line and first-run experience.

## Android home widget

PR #130 adds the first production Android home-screen surface: one responsive **Next Wake** Glance widget with Compact, Medium and Expanded densities.

The widget is deliberately a projection rather than a new product authority:

- next-wake ordering and Wake Ready truth come from existing local alarm/product owners;
- the expanded alarm summary opens the existing Alarms surface rather than bypassing scheduling/voice preflight with inline toggles or tiny row-level controls;
- Tomorrow Contract free text, Prepared Wake Plan text, First Move, alarm labels and transcripts/history content are not rendered on the launcher;
- Active Wake exposes re-entry into the authoritative Wake surface, never Stop/Snooze;
- Morning Check-In actions reuse the existing calibration mutation;
- alarm, preparation, appearance, history, reconciliation and Active Wake changes request bounded event-driven refreshes; there is no periodic polling loop;
- the selected planning Appearance is reflected visually without changing wake behavior;
- Android 15+ generated previews are published opportunistically with a static picker fallback.

The implementation uses stable Jetpack Glance 1.2.0, runtime `SizeMode.Exact` with three internal product breakpoints, and dedicated widget-safe sunrise/Wake Line assets.

This capability entered Build as an explicit 2026-09-25 product-owner exception to the active 1.0 feature freeze after its Shape contract was completed. The exception does not alter Alarm Kernel, Active Wake Execution, Direct Boot or cloud-independence boundaries.

Validation performed during Build: widget action-policy tests pass; Direct debug Kotlin and unit-test sources compile; Direct debug APK assembly passes; Direct debug lint passes. The local Windows ARM64 workstation still cannot execute the repository's Robolectric/Roborazzi native runtime, so Linux CI remains authoritative for those suites. A physical `adb install -r` was intentionally not forced after Android reported a signing mismatch with the installed WakeMyWay build, preserving existing app/alarm data.

Canonical product/engineering detail: [`41-android-home-widget.md`](41-android-home-widget.md).

## Phase B: alarms, sounds and voice configuration

Phase B is complete. Issue #56 is closed.

Merged slices:

- #61 consumer multi-alarm experience;
- #64 reviewed alarm/editor visual goldens;
- #65 fail-safe branded wake-sound runtime;
- #66 deterministic branded-audio importer and CI checksum guard;
- #68 approved full-length `Morning Light`, `Soft Start` and `Morning Pulse` masters plus real sound selection;
- #69 lifecycle-safe in-editor sound preview.

The three approved WakeMyWay sounds are physically bundled and selectable per alarm. Preview playback is deliberately non-critical: it uses the media audio path, never substitutes the emergency alarm, owns only one preview session at a time, releases focus/player resources on stop or editor exit, and is bounded to a short sample. Actual wake playback remains owned by the foreground alarm path and fails safe to the private emergency asset/platform alarm tone if a branded resource cannot be resolved.

## Phase C: consumer foundation and sync boundary

The local Phase C foundation is complete across PRs #70 through #73. Consumer behavior is exposed only when there is real backing state or a real action.

Implemented:

- branded first-run onboarding explaining the wake model before Android capability repair;
- versioned credential-protected `ConsumerPreferencesRepository`;
- real Profile destination in the consumer bottom navigation;
- local display-name profile;
- truthful local-only account state with no fake sign-in action;
- default Wake Sound, Voice Check-In, Alfred style and Snooze preferences;
- reusable First Move preference;
- saved defaults seed brand-new alarm drafts only;
- existing alarms retain their own persisted values when Profile defaults change;
- Notifications opens the real Android app-notification settings surface;
- Privacy and About screens;
- no Insights tab until Phase D has real wake-history data;
- system-native branded Android launch treatment scoped to normal consumer launch, not alarm presentation;
- three real local planning atmospheres: Daylight, Warm Sunrise and Soft Dawn;
- Appearance changes planning/configuration surfaces only and never changes the authored Active Wake progression;
- provider-neutral consumer backup/migration policy and Android local adapter;
- outage behavior proving cloud/account failure cannot mutate or block already-local alarm state.

Consumer preferences remain credential-protected normal product state. They are not copied into Direct Boot critical authority. Alarm delivery therefore does not depend on onboarding, Profile, appearance, an account or preference-storage availability after a wake has already been committed.

### Account/sync boundary

The initial cloud seam is backup/migration, not live cloud scheduling authority.

A snapshot may contain:

- syncable Profile/default preferences;
- rich consumer `AlarmDefinition` intent;
- snapshot schema/timestamp metadata.

It excludes by construction:

- device-local onboarding completion;
- Direct-Boot Critical Wake state;
- AlarmManager registration / next-occurrence authority;
- active Wake Session / WakeRuntime state;
- Stop/Snooze terminal state;
- Tomorrow Contract / Prepared Wake Plan private text;
- raw audio/transcripts.

Restore is conservative:

```text
same alarm id local + remote → local wins
remote-only alarm             → import disabled
fresh-device preference move  → restore syncable defaults, keep onboarding local
merge into existing device    → existing-device preferences win
```

Immediately before import, the Android adapter rechecks both rich product state and any Alarm Kernel slot for the same id. A stale cloud plan therefore cannot replace or cancel a locally committed wake. Remote imports always require a later explicit local enable action before they can become Android schedule authority.

There is still no user-facing Sign In action because no authenticated WakeMyWay account backend has been provisioned yet. ADR-013 remains the backend direction: optional Supabase Auth for identity/session acquisition, Wake API for domain operations, and Supabase PostgreSQL behind that API.

## Multi-alarm execution foundation

WakeMyWay no longer uses one mutable primary schedule as product truth.

- rich product alarm intent lives independently from the critical Android execution snapshot;
- schema-v3 Direct-Boot state stores independent schedule slots keyed by `WakeScheduleId`; schema v2 remains migration-compatible and v3 adds the minimal pre-unlock execution policy required for deterministic sound/voice/snooze behavior;
- editing or disabling one alarm leaves unrelated alarm slots and Android registrations untouched;
- modern one-shot alarms carry an exact local date;
- stale revisions cannot become active after an alarm is edited;
- only one physical wake execution may own foreground/audio authority at a time;
- a colliding valid occurrence returns `CONFLICT` and remains durable for deterministic reconciliation rather than starting competing audio;
- Snoozing one alarm chain preserves unrelated scheduled alarms;
- schema-v1 and schema-v2 critical state remain migration-compatible and are rewritten as schema v3 on the next successful mutation/reconciliation.

ADR 022 and the Alarm Kernel documentation remain the canonical product/execution decisions.

## Reliability and authority boundaries

The critical wake path is fully local and usable without cloud access.

- New Voice Wake creation remains strict. Required Android scheduling/presentation capabilities and required local voice capability are checked before commit.
- Losing microphone/on-device recognition after scheduling does not silently delete an otherwise controllable alarm. Voice degrades independently.
- Once an occurrence is delivered, active execution does not depend on future exact-alarm capability. Stop remains immediate; Snooze remains fail-closed because it requires a durable exact replacement.
- `WakeSessionViewModel` uses acknowledged terminal actions. Stop/Snooze commit Alarm Kernel state before behavioral resources are released or the Wake Surface closes.
- Duplicate terminal actions are suppressed. Rejected/failed Snooze leaves the current wake visible, audible and controllable.
- Critical Direct-Boot corruption is diagnosable and fail-closed.
- A stale UI surface belonging to an older occurrence cannot stop playback owned by a newer occurrence.
- Task dismissal is not a terminal alarm action.
- Snooze replacement is registered before current Active Wake authority is released.
- Restore download failure occurs before local read/apply; backup upload failure cannot mutate local state.
- Account/cloud availability is never a Wake Ready predicate.

Canonical readiness semantics are recorded in ADR 019.

## Privacy invariants

WakeMyWay intentionally separates normal private product state from the minimal Direct-Boot wake snapshot.

- Alarm labels, Profile preferences and reusable defaults are credential-protected normal app state.
- Tomorrow Contract / Prepared Wake Plan content remains credential-protected, is not copied into Direct Boot state, is not read while locked and is protected by `FLAG_SECURE` when rendered.
- Tomorrow Contract / Prepared Wake Plan private text is also excluded from the initial backup/migration snapshot.
- Direct-Boot critical state contains only the execution policy required to wake safely before unlock.
- Raw microphone audio and raw high-frequency motion samples are not persisted by the local wake path.
- Local alarms work without an account.
- Account/cloud outage must never become alarm authority.
- No operator/server secret is shipped in the Android app.

## WakeRuntime and conversational Alfred

`WakeRuntime` owns deterministic behavioral activation/orientation decisions and typed evidence. `AlarmKernel` owns durable scheduling and real terminal Stop/Snooze mutations. `AlarmPlaybackService` owns foreground playback, notification actions and playback teardown/recovery.

Founder/debug OpenAI Realtime over WebRTC is optional enrichment. Production Founder Realtime is intentionally kept unavailable for the 1.0 launch program; production provisioning/provider expansion must be re-earned by evidence rather than treated as unfinished launch work. It may render natural speech and report conversational turn boundaries, but it cannot:

- schedule or cancel alarms;
- Stop/Snooze execution;
- mutate WakePolicy;
- directly create activation evidence;
- decide Wake completion;
- become a Wake Ready dependency.

Local Alfred remains the fallback when cloud/network/provider setup fails.

Founder installation pairing uses a one-time high-entropy access code and returns a scoped installation credential stored through Android Keystore. Server credential roles remain separated between internal API authorization, founder-token signing and one-time pairing.

## Automated quality gates

Repository quality gates include:

- pure `:wake-core` unit tests;
- Android app unit/Robolectric tests;
- lint, debug assembly and instrumentation compilation;
- deterministic branded-audio asset/checksum verification;
- curated Roborazzi visual regression;
- compact-device wake rendering smoke coverage;
- reviewed RTL smoke coverage across the core consumer and Active Wake journey;
- API-36 Compose semantics contracts for TalkBack-critical actions/navigation;
- API-36 device reliability instrumentation;
- Cloud AI Platform tests;
- dedicated lightweight Docs CI validation.

New canonical visual states must be rendered and reviewed before their hashes are promoted. Product-facing work follows the Explore → Shape → Build → Harden → Learn workflow in `docs/34-product-development-workflow.md` so expensive engineering gates validate an accepted product slice rather than act as the first design feedback loop. #73 is intentionally non-visual, so its accepted result requires the approved visual hash set to remain unchanged.

## 1.0 FINISH hardening evidence

The automated/repository side of Gate 2 is now substantially converged.

- Critical Active Wake, onboarding and alarm editing have compact + large-text smoke evidence.
- TalkBack-critical Compose semantics are regression-protected on API 36 for Stop, Snooze, First Move, bottom navigation, onboarding and alarm controls; spoken/traversal acceptance remains a physical #99 check.
- Custom navigation, alarm/editor/Profile/Insights controls have explicit semantics and minimum touch-target hardening.
- RTL layout has reviewed smoke renders across onboarding, editor, Alarms, Insights, Profile and Active Wake; English fallback text remains content-directed and directional chevrons explicitly follow layout direction.
- Canonical LTR visual hashes remain protected.
- Current release artifacts are intentionally measured at **52.70 MiB Direct APK** and **34.53 MiB Play AAB**. The three offline WakeMyWay masters dominate remaining package size, so no speculative audio degradation or shrinking change is accepted without measured benefit.
- Android OS cloud backup and device transfer explicitly exclude app-managed credential- and device-protected state; WakeMyWay-managed migration remains a separate conservative boundary.
- The existing `:benchmark` module is now a release-derived Play cold-start Macrobenchmark contract. CI verifies buildability; physical-device timing remains #97 because hosted-emulator timings are not product evidence.
- Founder/debug Wake Lab and Alfred setup actions remain gated behind `FLAG_DEBUGGABLE`; normal release UI exposes no fake account or developer placeholder.
- Remaining physical FINISH acceptance is tracked in #97 (startup timing) and #99 (TalkBack + system reduced-motion behavior), coordinated with the broader physical reliability work in #9.

## Deep review hardening (2026-09-25)

A repository-wide review of the trust-critical paths produced a hardening pass. No product scope changed.

- Android 12/12L (API 31/32) exact-alarm repair: `SCHEDULE_EXACT_ALARM` is now declared with `maxSdkVersion="32"` alongside `USE_EXACT_ALARM`. Without it, no exact alarm could ever be registered, enabled, or repaired on those OS versions.
- The last-resort `ToneGenerator` fallback in `AlarmPlaybackService` is guarded and re-armed on a repeating timer. On total audio failure the wake stays foreground, haptic and controllable instead of crash-looping through `START_REDELIVER_INTENT`.
- Active wake execution now owns a redundant repeating haptic layer, requests alarm-stream audio focus (inert listener: playback never yields), holds a timeout-guarded CPU wake lock across the receiver-to-audio window, and uses `setWakeMode` for looping playback. Failed `MediaPlayer` construction can no longer leak decoders.
- `WakeActivity` handles `onNewIntent` and relaunches for a different occurrence, so a newer wake's UI intent can no longer land silently on a stale surface.
- `AlarmKernel` and `CriticalWakeStore` mutation paths synchronize on one process-wide lock; per-instance monitors never guarded cross-component read-modify-write.
- Wake Learning v0: the fail-closed validator explicitly rejects multi-change snapshots, `derive()` fails fast on out-of-bounds current policies, persisted-version semantics (source version may exceed the default version; lineage is not the trust boundary) are documented and pinned by tests, and previously untested fallback branches (`MISSING`, out-of-bounds persisted state, stale baseline, all three safe-bound stops, the friction guardrail on false-positive activations, foreign-policyVersion evidence) are covered.
- Commerce: `POST /api/v1/commerce/play-verify` is fail-closed until an edge rate-limit rule id is declared (`WMW_PLAY_VERIFY_RATE_LIMIT_RULE_ID`); RTDN push-JWT verification and the consolidated internal AI route have explicit authorization tests.
- Validation: cloud `typecheck` + 86 tests pass locally; wake-core 107 unit tests pass locally on JDK 17; Robolectric suites compile locally and run in Android CI (this workstation is Windows ARM64, where Robolectric's native runtime cannot load).

## Physical proof still required

Automated/emulator evidence is not sufficient for a wake product. Issue #9 remains the release gate for repeated physical-device proof, including:

- locked-screen T+2m cycles;
- Doze/idle;
- process/service recreation;
- reboot / Direct Boot before unlock;
- timezone/time changes;
- presentation and exact-alarm capability changes;
- Stop/Snooze resurrection checks;
- the three branded sounds during real wake execution;
- branded sound plus Voice Check-In coexistence;
- Bluetooth/audio-route behavior;
- motion calibration and false-positive behavior;
- representative reliability-report retention.

Broader release should not be declared complete until the supported-device reliability envelope is measured rather than inferred.

## Phase D: history, calibration and adaptive consumer loop

The production wake path now closes the first local adaptive loop:

- terminal Wake occurrences persist bounded credential-protected history with comparable runtime evidence;
- Primary → Snooze replacement chains project into one consumer morning;
- Insights exposes real evidence without synthetic sleep or wake scores;
- sparse one-tap calibration distinguishes phone-observed Activation Completion from Confirmed Wake Success;
- explicit early Stop remains immediate and is recorded as an incomplete wake rather than silently treated as success;
- an explicit Stop schedules a non-critical WorkManager safety check for roughly 15 minutes later; it self-suppresses when calibration already exists, offers quick "still up" / "back to bed" calibration actions, and is never alarm authority;
- deterministic Wake Learning v0 re-derives only bounded policy parameters from comparable local sessions;
- learned policy state is kept under no-backup credential-protected storage;
- one resolved learned Wake Policy is selected before the next interactive WakeRuntime session starts and remains immutable for that session;
- corrupt/missing learning state falls back to the stable default and cannot affect Alarm Kernel authority;
- the Oriented wake state now waits for an explicit First Move confirmation instead of visually presenting non-functional choice tiles.

Consumer onboarding and alarm setup were also simplified around the adaptive promise: advanced wake behavior remains available without making first alarm creation feel like a settings panel. Account-shaped placeholder UI is intentionally absent while account sync remains optional/deferred.

## Update distribution

The product/update boundary introduced by PR #83 remains intact: update checking/downloading stays outside the Alarm Kernel, direct-install permission exists only in the `direct` flavor, Play flexible in-app updates remain isolated to the `play` flavor, downloaded Direct APKs are verified for checksum/package/signing identity, and install/restart remains blocked during an Active Wake Execution or within 90 minutes of the next Wake Occurrence.

Production releases now use the manual **Release WakeMyWay** GitHub Actions workflow. Merging to `main` never publishes a version. When an operator explicitly dispatches the workflow from current `main`, it calculates the next semantic version and monotonically increasing `versionCode` from the latest stable release, generates release notes, runs release/device/upgrade gates, builds and signs Direct APK + Play AAB, verifies the production identity, creates `update.json`, stages a draft release, verifies uploaded assets, then publishes and verifies the public latest endpoint.

`apps/android/version.properties` is now only a local/default developer fallback. Production version authority is the latest stable GitHub Release plus its `update.json`; release builds receive `WMW_VERSION_CODE` and `WMW_VERSION_NAME` overrides.

Stable signing material remains outside the repository and is provisioned as encrypted GitHub Actions Secrets. The local DPAPI signing bundle remains an offline recovery path rather than the normal release path. Canonical operations live in [`42-android-release-operations.md`](42-android-release-operations.md).

## Package-update persistence

WakeMyWay 0.2.2 introduces an executable in-place-update persistence contract.

The direct/Play package identity remains stable, consumer AlarmDefinitions remain product truth across package replacement, and `MY_PACKAGE_REPLACED` continues to reconcile Alarm Kernel scheduling after update. AlarmDefinition and consumer-preference storage now use explicit historical decoder branches so future schema changes must migrate rather than reset.

A dedicated API 36 CI lane builds the PR base and candidate with the same debug signing identity, seeds alarm/preferences/history/critical state into the baseline, performs `adb install -r`, checks that `firstInstallTime` is unchanged, and verifies the same durable state from the candidate. The lane is path-scoped to persistence/alarm/update-sensitive changes to contain CI cost.

## Active 1.0 readiness program

WakeMyWay now operates under the paid-launch readiness plan in [`35-paid-launch-readiness.md`](35-paid-launch-readiness.md) and program issue #89.

The product capability set is considered sufficient to test the thesis. The default until 1.0 is therefore **finish and prove, not expand**.

### TRUST — #9

Complete repeated physical-device reliability evidence across locked screen, Doze, process/service recreation, Direct Boot, time changes, capability loss, audio routes, motion and Stop/Snooze recovery. Public paid release remains blocked while overnight reliability is inferred rather than measured.

### FINISH — #59

Converge the existing consumer experience screen by screen: comprehension, navigation, authored failure/permission states, visual fidelity, accessibility, compact/RTL stress, Active Wake polish, release performance and package-size review. This is hardening, not redesign.

### PROVE — #87

Run a qualified target-user beta with habitual snoozers. Measure reliability, Activation Completion, Confirmed Wake Success and calibration coverage, return-to-bed behavior, snooze/intervention depth, annoyance/agency, D7/D14 retention and real willingness-to-pay evidence.

### SELL — #88

After/alongside sufficient evidence, complete the commercial/release layer: safe billing/entitlement lifecycle, outcome-led Free/Pro packaging, Play listing and testing tracks, public privacy/legal/Data Safety/support, privacy-safe production observability, signed public release/update verification and staged rollout/rollback operations.

Gate 4 repository preparation is now substantially complete without locking pricing/package before Gate 3:

- `docs/38-play-launch-operations.md` contains the consumer-first Play listing draft, screenshot story, testing-track ladder, support taxonomy, incident severity model and staged rollout/pause/rollback rules;
- `docs/39-privacy-data-safety-launch.md` contains the shipped-data inventory, Data Safety worksheet, processor/retention posture and deployable fail-closed `/privacy` + `/support` surfaces;
- `docs/40-play-billing-boundary.md` records the merged Play Billing 9.1.0 client, server verification, server acknowledgement, SHA-256 lifecycle ledger and authenticated RTDN path;
- signed Play release configuration is fail-closed and purchase launch remains disabled unless product/verification/HTTPS settings are explicitly supplied;
- no consumer paywall, final price or invented Pro-only feature wall is exposed before #87 evidence;
- production crash/ANR monitoring should use Play/Android Vitals first; WakeMyWay does not add broad analytics/session replay merely to satisfy an observability checkbox;
- a real monitored support/privacy contact, deployed production URLs, Play Console products/credentials/RTDN, license-tester evidence, final Data Safety/content-rating entries and rollout actions remain external launch prerequisites.

### Deferred until evidence earns them

New characters, Health Connect, calendar/weather expansion, iOS/Wear OS, smart-home integrations, broad Realtime expansion, additional dashboard scope, new themes and account/cloud capabilities not required for launch remain deferred by default.

The feature freeze is a scope freeze, not a code freeze. Reliability fixes, regressions, simplification, accessibility, release optimization, compliance and bounded beta-driven tuning continue normally.

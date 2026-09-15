# Project status

**Last updated:** 2026-09-15  
**Product:** WakeMyWay (WMW)  
**Platform:** Android first; optional non-critical Vercel cloud  
**Current product phase:** Phase C consumer foundation + founder physical reliability proof  
**Current product PR:** #70  
**Physical release gate:** #9  
**Reliability rule:** future scheduling readiness, active execution safety, voice readiness and Snooze readiness are separate predicates  
**Cloud rule:** cloud conversation is optional enrichment only; Alarm Kernel remains durable alarm/terminal authority and WakeRuntime remains behavioral activation/orientation authority

## Current product shape

WakeMyWay is a local-first Android wake system built around reliable alarms, a deterministic behavioral runtime and optional conversational enrichment.

```text
First-run onboarding / Profile defaults
              ↓
      Alarm library + editor
              ↓ strict new-Wake preflight
AlarmDefinitionRepository · credential protected
              ↓ compile critical policy
Alarm Kernel · schema v2 independent schedule slots
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
```

The sunrise-wave identity from PR #51 remains the canonical visual system. Planning and configuration live on warm light surfaces; Active Wake intentionally begins in midnight navy and resolves toward daylight. The same sunrise-wave geometry is used by the launcher identity, Wake Line and first-run experience.

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

## Phase C: consumer foundation

PR #70 is the active Phase C foundation. It adds consumer behavior only when there is real backing state or a real action.

Implemented in the current branch:

- branded first-run onboarding explaining the wake model before Android capability repair;
- versioned credential-protected `ConsumerPreferencesRepository`;
- real Profile destination in the consumer bottom navigation;
- local display-name profile;
- truthful local-only account status with no fake sign-in action;
- default Wake Sound, Voice Check-In, Alfred style and Snooze preferences;
- reusable First Move preference;
- saved defaults seed brand-new alarm drafts only;
- existing alarms retain their own persisted values when Profile defaults change;
- Notifications opens the real Android app-notification settings surface;
- Privacy and About screens;
- no Insights tab until Phase D has real wake-history data;
- no fake appearance switch while normal app appearance is not yet genuinely configurable.

Consumer preferences remain credential-protected normal product state. They are not copied into Direct Boot critical authority. Alarm delivery therefore does not depend on onboarding, Profile, an account or preference-storage availability after a wake has already been committed.

## Multi-alarm execution foundation

WakeMyWay no longer uses one mutable primary schedule as product truth.

- rich product alarm intent lives independently from the critical Android execution snapshot;
- schema-v2 Direct-Boot state stores independent schedule slots keyed by `WakeScheduleId`;
- editing or disabling one alarm leaves unrelated alarm slots and Android registrations untouched;
- modern one-shot alarms carry an exact local date;
- stale revisions cannot become active after an alarm is edited;
- only one physical wake execution may own foreground/audio authority at a time;
- a colliding valid occurrence returns `CONFLICT` and remains durable for deterministic reconciliation rather than starting competing audio;
- Snoozing one alarm chain preserves unrelated scheduled alarms;
- schema-v1 critical state remains migration-compatible and is rewritten as schema v2 on the next successful mutation/reconciliation.

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

Canonical readiness semantics are recorded in ADR 019.

## Privacy invariants

WakeMyWay intentionally separates normal private product state from the minimal Direct-Boot wake snapshot.

- Alarm labels, Profile preferences and reusable defaults are credential-protected normal app state.
- Tomorrow Contract / Prepared Wake Plan content remains credential-protected, is not copied into Direct Boot state, is not read while locked and is protected by `FLAG_SECURE` when rendered.
- Direct-Boot critical state contains only the execution policy required to wake safely before unlock.
- Raw microphone audio and raw high-frequency motion samples are not persisted by the local wake path.
- Local alarms work without an account.
- Account/cloud outage must never become alarm authority.

## WakeRuntime and conversational Alfred

`WakeRuntime` owns deterministic behavioral activation/orientation decisions and typed evidence. `AlarmKernel` owns durable scheduling and real terminal Stop/Snooze mutations. `AlarmPlaybackService` owns foreground playback, notification actions and playback teardown/recovery.

Founder/debug OpenAI Realtime over WebRTC is optional enrichment. It may render natural speech and report conversational turn boundaries, but it cannot:

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
- API-36 device reliability instrumentation;
- Cloud AI Platform tests;
- documentation validation.

The final PR #70 head must pass the full Android CI, visual regression and API-36 reliability gate set before merge. New Phase C visual goldens must be reviewed from rendered screenshots before their hashes are promoted.

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

## Remaining product work

- Complete Phase C optional account/sync boundary without making local alarms account-dependent.
- Decide and implement a genuinely configurable normal-app appearance mode before exposing an appearance control.
- Phase D: wake history and Insights backed by real local session data, not synthetic metrics.
- M7 live learning/journal persistence, calibration and learned-policy selection remain tracked separately.
- Founder Realtime production environment/rate-limit work remains separate from local alarm readiness.
- Physical-device reliability evidence under #9 remains mandatory before broader release.

# Project status

**Last updated:** 2026-09-19  
**Product:** WakeMyWay (WMW)  
**Platform:** Android first; optional non-critical Vercel cloud with Supabase as the preferred future managed data/auth platform  
**Current product phase:** Phase D real history + consumer calibration + bounded next-session learning connected; founder physical reliability proof ongoing  
**Current product PR:** none after #73; authenticated account backend is not yet provisioned  
**Physical release gate:** #9  
**Reliability rule:** future scheduling readiness, active execution safety, voice readiness and Snooze readiness are separate predicates  
**Cloud rule:** cloud/account state is never Alarm Kernel or WakeRuntime authority

## Current product shape

WakeMyWay is a local-first Android wake system built around reliable alarms, a deterministic behavioral runtime and optional conversational/cloud enrichment.

```text
First-run onboarding / Profile defaults / planning Appearance
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

Optional account backup/migration
        │
        └─ normal consumer intent only
           never Direct-Boot / active-wake authority
```

The sunrise-wave identity from PR #51 remains the canonical visual system. Planning and configuration use the selected local planning atmosphere, while Active Wake intentionally follows the fixed authored midnight-to-daylight progression. The same sunrise-wave geometry is used by the launcher identity, system launch treatment, Wake Line and first-run experience.

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
- dedicated lightweight Docs CI validation.

New canonical visual states must be rendered and reviewed before their hashes are promoted. Product-facing work follows the Explore → Shape → Build → Harden → Learn workflow in `docs/34-product-development-workflow.md` so expensive engineering gates validate an accepted product slice rather than act as the first design feedback loop. #73 is intentionally non-visual, so its accepted result requires the approved visual hash set to remain unchanged.

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
- deterministic Wake Learning v0 re-derives only bounded policy parameters from comparable local sessions;
- learned policy state is kept under no-backup credential-protected storage;
- one resolved learned Wake Policy is selected before the next interactive WakeRuntime session starts and remains immutable for that session;
- corrupt/missing learning state falls back to the stable default and cannot affect Alarm Kernel authority;
- the Oriented wake state now waits for an explicit First Move confirmation instead of visually presenting non-functional choice tiles.

Consumer onboarding and alarm setup were also simplified around the adaptive promise: advanced wake behavior remains available without making first alarm creation feel like a settings panel. Account-shaped placeholder UI is intentionally absent while account sync remains optional/deferred.

## Remaining product work

- Provision the actual WakeMyWay account backend when ready, then implement authenticated account/session acquisition and Wake API backup/restore without changing alarm authority.
- Only after that authenticated path exists, expose truthful Sign In / backup UX.
- Continue Phase D calibration/Insights hardening with real dogfood data, including whether check-in timing and copy need notification-based follow-up.
- M7 follow-up still includes richer founder inspect/reset tooling and any additional journal detail justified by dogfood; production calibration and learned-policy selection are now connected.
- Founder Realtime production environment/rate-limit work remains separate from local alarm readiness.
- Physical-device reliability evidence under #9 remains mandatory before broader release.

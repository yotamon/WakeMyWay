# Project status

**Last updated:** 2026-09-15  
**Product:** WakeMyWay (WMW)  
**Platform:** Android first; optional non-critical Vercel cloud with Supabase as the preferred future managed data/auth platform  
**Current product phase:** Phase C consumer shell + optional account/sync boundary  
**Current product PR:** #73  
**Physical release gate:** #9  
**Reliability rule:** future scheduling readiness, active execution safety, voice readiness and Snooze readiness are separate predicates  
**Cloud rule:** cloud/account state is never Alarm Kernel or Wake Runtime authority

## Current product shape

WakeMyWay is a local-first Android wake system built around reliable alarms, deterministic behavioral waking and optional conversational/cloud enrichment.

```text
Onboarding / Profile / planning Appearance
                 ↓
        Alarm library + editor
                 ↓ strict new-Wake preflight
 AlarmDefinitionRepository · credential protected
                 ↓ compile critical policy
 Alarm Kernel · Direct-Boot critical authority
                 ↓
       AlarmManager.setAlarmClock()
                 ↓
          AlarmReceiver
                 ↓
      AlarmPlaybackService
        ├─ selected bundled WakeMyWay sound
        ├─ notification Stop/Snooze
        └─ full-screen WakeActivity
                    ↓
            WakeSessionViewModel
                    ↓
               WakeRuntime
        ├─ local Alfred / voice replies
        ├─ motion evidence
        └─ optional non-critical enrichment

Optional account backup/migration
        │
        └─ normal consumer intent only
           never Direct-Boot / active-wake authority
```

## Phase B: alarms, sounds and voice configuration

Phase B is complete. Issue #56 is closed.

Merged slices include the consumer alarm library/editor, reviewed alarm visuals, fail-safe branded audio, deterministic asset verification, the approved `Morning Light`, `Soft Start` and `Morning Pulse` masters, real sound selection and lifecycle-safe in-editor preview.

Actual wake playback remains owned by the foreground alarm path and fails safe to the private emergency asset/platform alarm tone if a branded resource cannot be resolved.

## Phase C: consumer foundation

Merged:

- #70: authored first-run onboarding, Profile, credential-protected consumer preferences, default Sound / Voice Check-In / Alfred style / Snooze / First Move behavior, Notifications, Privacy and About;
- #71: native branded Android launch treatment, explicitly scoped away from `WakeActivity`;
- #72: real configurable planning Appearance with `Daylight`, `Warm Sunrise` and `Soft Dawn`, including visual proof that planning appearance does not alter active Wake states.

Current #73 adds the provider-neutral backup/migration boundary before a real account provider is attached.

### Account/sync boundary in #73

Cloud backup is a copy of normal consumer intent, not execution authority.

The snapshot can include:

- syncable Profile/default preferences;
- rich consumer `AlarmDefinition` intent;
- snapshot schema/timestamp metadata.

It excludes by construction:

- device-local onboarding completion;
- Direct-Boot Critical Wake state;
- AlarmManager / next-occurrence authority;
- active Wake Session / Wake Runtime state;
- Stop/Snooze terminal state;
- Tomorrow Contract / Prepared Wake Plan private text;
- raw audio/transcripts.

Restore semantics are conservative:

```text
same alarm id local + remote → local wins
remote-only alarm             → import disabled
fresh-device preference move  → restore syncable defaults, keep onboarding local
merge into existing device    → existing-device preferences win
```

Before an imported alarm is persisted, the Android adapter rechecks both rich product state and the Alarm Kernel slot for that id. A stale cloud plan cannot replace or cancel a locally committed wake.

There is still no user-facing Sign In action in the Android product because no authenticated WakeMyWay account backend has been provisioned yet. ADR-013 remains the backend direction: optional Supabase Auth for session acquisition, Wake API for domain operations, Supabase PostgreSQL behind the API.

## Reliability and authority boundaries

The critical wake path remains fully local and usable without cloud/account access.

- `AlarmKernel` owns exact scheduling, critical persistence, Direct Boot, active occurrence authority and durable Stop/Snooze mutations.
- `WakeRuntime` owns deterministic behavioral activation/orientation decisions.
- `AlarmPlaybackService` owns foreground alarm playback and terminal notification controls independently of `WakeActivity` lifetime.
- New Voice Wake creation remains strict about required local Android capabilities.
- Losing optional voice/cloud capability never silently deletes an otherwise controllable committed alarm.
- Snooze remains fail-closed because it requires a durable exact replacement.
- A stale UI surface cannot stop playback owned by a newer occurrence.
- Task dismissal is not a terminal alarm action.

Account/cloud failure cannot become a readiness predicate. #73 additionally regression-tests that restore download failure performs zero local read/apply and backup upload failure performs no local mutation.

## Privacy invariants

WakeMyWay separates normal private product state from the minimal Direct-Boot wake snapshot.

- Alarm labels, Profile preferences and reusable defaults are credential-protected normal app state.
- Tomorrow Contract / Prepared Wake Plan content remains credential-protected, is excluded from Direct Boot and is excluded from the initial backup/migration snapshot.
- Direct-Boot state contains only execution policy required to wake safely before unlock.
- Raw microphone audio and raw high-frequency motion samples are not persisted by the local wake path.
- Local alarms work without an account.
- No operator/server secret is shipped in the Android app.

## Brand and UX state

The sunrise + Wake Line identity from PR #51 remains canonical.

- Consumer wordmark: **WakeMyWay**
- Brand line: **Brighter mornings. Your way.**
- Planning/configuration uses Cloud/Paper morning surfaces with Sunrise/Dawn accents.
- Active Wake begins in Midnight/Deep Navy and progresses intentionally toward morning light.
- Planning Appearance is configurable, but `EMERGING`, `ENGAGED`, `ACTIVE`, `ORIENTED` and `COMPLETE` retain their authored wake-state visuals.
- Insights remains deferred until Phase D has real local wake-history/session data.

## Automated quality gates

Repository gates include:

- pure `:wake-core` unit tests;
- Android app unit/Robolectric tests;
- lint, debug assembly and instrumentation compilation;
- deterministic branded-audio asset/checksum verification;
- curated Roborazzi visual regression;
- compact-device wake rendering smoke coverage;
- API-36 device reliability instrumentation;
- Cloud AI Platform tests;
- documentation validation.

Phase C slices are merged only after the relevant exact head passes Android CI, visual regression and API-36 reliability. New visual goldens are reviewed before promotion. #73 is intentionally non-visual, so the approved visual hash set must remain unchanged.

## Physical proof still required

Issue #9 remains the broader release gate. Emulator/CI evidence does not replace repeated physical-device mornings, including locked-screen T+2m cycles, Doze/idle, process/service recreation, reboot/Direct Boot, timezone/time changes, capability changes, Stop/Snooze resurrection checks, the three branded sounds, Voice Check-In coexistence, Bluetooth/audio routing and motion calibration.

## Remaining product work

- Finish and merge #73 provider-neutral backup/migration boundary and outage proof.
- Provision the actual WakeMyWay account backend when ready, then implement authenticated account/session acquisition and Wake API backup/restore without changing alarm authority.
- Only after that backend exists, expose truthful Sign In / backup UX.
- Phase D: wake history and Insights backed by real local session data, not synthetic metrics.
- M7 learning/journal persistence and calibration remain tracked separately.
- Physical-device reliability evidence under #9 remains mandatory before broader release.

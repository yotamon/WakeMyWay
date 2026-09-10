# D3 product setup — Wake Schedule + Tomorrow Contract

**Status:** merged in PR #32  
**Track:** Adaptive Dawn product-design track, parallel to the M7/M8 behavioral roadmap  
**Authority:** Alarm Kernel remains the only owner of durable wake scheduling and exact Android registration

## Goal

Replace founder-only setup mechanics with a real everyday product path without inventing product controls that are not backed by durable state.

This slice intentionally implements only capabilities that already have a trustworthy owner:

- Wake Schedule -> Alarm Kernel
- Wake readiness -> Alarm Health
- Tomorrow Contract / Prepared Wake Plan -> WakePreparationManager

Wake difficulty and character selection remain deferred until their selected values have a durable product contract and affect real behavior. They are not rendered as decorative placeholder controls.

## Product flow

```text
Tonight
  |
  +--> Wake Setup
  |      |
  |      +--> Just tomorrow
  |      |       -> ONE_SHOT WakeSchedule
  |      |
  |      +--> Repeat weekly
  |              -> RECURRING WakeSchedule
  |              -> independent time per enabled weekday
  |      |
  |      +--> AlarmKernel.commitSchedule()
  |      +--> AlarmKernel.cancelSchedule()
  |
  +--> Tomorrow Contract
  |      |
  |      +--> WakePreparationManager.saveAndPrepare()
  |      +--> credential-protected local state
  |      +--> prepared offline wake content
  |
  +--> Wake Alarm Lab
         -> explicit founder diagnostics only
```

## Wake Schedule editor

### Supported modes

**Just tomorrow**

- one concrete next-day wake intention;
- maps to `WakeCompletionPolicy.ONE_SHOT`;
- does not silently create a recurring alarm.

**Repeat weekly**

- maps to `WakeCompletionPolicy.RECURRING`;
- each weekday can be enabled independently;
- each enabled weekday can retain a different local wake time;
- existing weekday-specific times are preserved when opening the editor.

The UI does not reduce the domain model to one shared weekly time because `WakeSchedule.timesByDay` already supports independent times. Product UI should not silently destroy valid existing domain state for visual simplicity.

### Read contract

The setup screen needs to edit the current schedule. It does **not** read `CriticalWakeStore` or `CriticalWakeSnapshot` directly.

`AlarmKernel.currentSchedule()` is therefore added as a narrow read contract:

```text
product UI
    ↓
AlarmKernel.currentSchedule()
    ↓
current enabled WakeSchedule
```

Storage representation, generation numbers, registered occurrence identity and snapshot ordering remain private Alarm Kernel implementation knowledge.

### Commit behavior

All schedule writes continue through:

```text
WakeSetupScreen
    ↓
AlarmKernel.commitSchedule(schedule)
    ↓
critical snapshot persistence
    ↓
exact OS registration
    ↓
AlarmHealth
```

The UI never coordinates persistence and `AlarmManager` itself.

Turning a schedule off calls `AlarmKernel.cancelSchedule()` and preserves the existing cancellation-tombstone behavior.

## Tomorrow Contract promotion

The previous Tomorrow Contract founder lab proved the persistence/preparation mechanics. D3 promotes the same real capability into a product screen rather than creating a second implementation.

The product screen uses `WakePreparationManager` directly for its application-facing operations:

- inspect the contract bound to the next Wake Occurrence;
- save/update the private intention;
- optionally save a First Move;
- prepare validated local wake content;
- clear private preparation state.

It does not expose checksum/version diagnostics that belong in Wake Lab.

## Privacy hardening

Tomorrow Contract text is private content.

While the product Tomorrow Contract screen is visible, `MainActivity` temporarily applies Android `FLAG_SECURE` so screenshots/screen capture are blocked where Android honors the flag.

The screen records whether the Activity was already secure and only removes the flag on exit when this screen added it. This prevents accidentally weakening another caller's privacy state.

The underlying storage contract remains unchanged:

- credential-protected local storage;
- excluded from the Direct-Boot Critical Wake Snapshot;
- no cloud requirement;
- no raw private text in reliability logs.

## Schedule edits and occurrence-bound private content

A Tomorrow Contract is intentionally bound to one concrete Wake Occurrence. Editing a schedule changes its revision and therefore normally changes the occurrence ID.

D3 handles this explicitly instead of producing orphaned or silently lost state:

```text
old occurrence + contract
          |
          v
schedule commit succeeds first
          |
          v
new occurrence
    |
    +-- same local wake date
    |      -> rebind raw contract + First Move
    |      -> prepare again for new occurrence ID
    |
    +-- different local wake date
           -> clear old occurrence-bound private content
```

If a same-date rebind fails, the stale private preparation state is cleared best-effort rather than being treated as valid for the new occurrence.

The critical schedule commit never depends on this optional migration succeeding.

Turning the schedule off also clears the occurrence-bound Tomorrow Contract best-effort after the Alarm Kernel cancellation has succeeded.

## Reliability boundary

This work does not change the trust-critical chain:

```text
AlarmManager trigger
      ↓
Alarm Kernel Active Wake Execution
      ↓
AlarmPlaybackService / local USAGE_ALARM audio
      ↓
Stop / Snooze
```

`Navigation 3`, Wake Setup, Tomorrow Contract UI and private preparation are outside this chain.

A Compose/navigation failure may make setup unavailable; it must not make an already committed wake occurrence unable to fire.

## Testing

The Alarm Kernel instrumentation suite adds coverage that:

- a committed enabled schedule is readable through `currentSchedule()`;
- after cancellation, `currentSchedule()` returns no active schedule.

Existing cancellation, stale-trigger and snooze-chain instrumentation remains unchanged.

PR #32 passed before merge:

- docs validation;
- `wake-core` tests;
- Android lint / Compose compilation;
- instrumentation compilation;
- debug APK assembly;
- API-36 reliability instrumentation.

The first full lint pass found seven Compose configuration-awareness errors in the new setup/private-context UI. They were fixed using observable `LocalConfiguration` / `stringResource` access; no lint baseline or suppression was added.

## Remaining D3 work after this slice

Not implemented as fake controls:

- Wake difficulty/intensity selection;
- character selection;
- consumer-facing readiness repair for missing exact-alarm/full-screen/notification capabilities;
- Safety Backup, pending evidence that it is still useful;
- final accessibility and visual-regression pass.

The next product controls should be added only when their values have a durable owner and affect real behavior.

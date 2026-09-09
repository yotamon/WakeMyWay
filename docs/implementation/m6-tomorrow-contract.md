# M6 — Tomorrow Contract + Prepared Wake Plan

**Status:** implementation candidate in PR #20  
**Issue:** #19  
**Branch:** `m6/tomorrow-contract-prepared-plan`

## Purpose

M6 introduces the first sensitive personalized morning content without changing Wake My Way's alarm trust boundary.

A user can attach a short **Tomorrow Contract** to the concrete next Wake Occurrence: why tomorrow matters and, optionally, the first real-world move they want morning-them to take. WMW deterministically prepares a bounded local **Prepared Wake Plan** before wake time.

Prepared content is enrichment only. The Alarm Kernel and Wake Runtime remain authoritative even when all M6 state is missing, locked, corrupt or stale.

## Implemented flow

```text
next Wake Occurrence
        ↓
Tomorrow Contract editor
        ↓
WakePreparationManager
        ├─ atomic credential-protected save
        ├─ deterministic local preparation
        ├─ SHA-256 integrity checksum
        └─ enqueue deferrable WorkManager refresh
        ↓
Prepared Wake Plan
        ↓
validate occurrence + source revision + format + checksum
        ├─ valid → private prepared content available offline
        └─ invalid/missing/locked → bundled generic local fallback
```

The `WakePreparationManager` is the application-facing owner. UI and workers do not coordinate contract persistence, plan persistence and validation independently.

## Pure domain (`:wake-core`)

`com.wakemyway.core.preparation` contains:

- `TomorrowContractId`
- `TomorrowContract`
- `PreparedWakePlanId`
- `PreparedWakePlan`
- `PreparedWakePlanPreparer`
- `PreparedPlanValidation`
- explicit invalid reasons

### Tomorrow Contract invariants

- tied to exactly one concrete `WakeOccurrenceId`;
- non-blank raw text;
- raw text bounded to 1,200 characters;
- optional First Move bounded to 120 characters;
- positive revision and timestamps;
- every edit of the same occurrence advances the source revision.

### Prepared Wake Plan invariants

- format-versioned independently from storage encoding;
- tied to the same concrete Wake Occurrence;
- records source Contract ID + revision;
- records character ID + character version;
- contains bounded prepared text plus generic local fallback lines;
- carries a SHA-256 checksum over a canonical length-prefixed representation;
- validation fails closed for unsupported format, wrong occurrence, stale source or checksum mismatch.

The prepared plan does **not** contain Wake Runtime policy, activation evidence, phase/state authority, alarm scheduling authority or dismissal/snooze authority.

## Deterministic preparation

M6 preparation is intentionally local and deterministic.

The current v1 plan:

1. normalizes whitespace from the user's contract;
2. bounds the morning reminder content;
3. renders Alfred's existing deterministic `Orientation` line using occurrence + contract revision as the render key;
4. creates an optional bounded First Move line;
5. attaches generic local fallback lines;
6. computes the plan checksum.

Given the same contract, character version and preparation timestamp, the output is identical.

No AI, network, backend, prompt, realtime voice provider or cloud model is involved.

## Android private storage boundary

`PrivateWakePreparationStore` uses an app-private directory under:

```text
credential-protected Context
    ↓
noBackupFilesDir/wake-preparation/
    ├─ tomorrow-contract-v1.bin
    └─ prepared-wake-plan-v1.bin
```

Properties:

- normal Android credential-protected storage, unavailable during Direct Boot before first unlock;
- excluded from Android Auto Backup by using `noBackupFilesDir`;
- versioned binary encoding;
- `AtomicFile` writes so an interrupted replacement keeps the previous committed value;
- no private-content logging;
- constructor explicitly rejects `deviceProtectedStorage` contexts.

That explicit rejection is a defense-in-depth invariant. A future caller cannot move Tomorrow Contract/private prepared state into Direct-Boot storage simply by passing a different Context.

## WorkManager role

M6 adds AndroidX WorkManager 2.11.2 as a non-critical dependency.

`PrepareWakePlanWorker` may rebuild the latest private Prepared Wake Plan as deferrable background work. It uses one unique replacement work item and has no network constraint because preparation is fully local.

WorkManager does **not**:

- schedule Wake Occurrences;
- fire alarms;
- start Active Wake Execution;
- own snooze/stop;
- determine Wake Ready;
- become a prerequisite for valid wake delivery.

The foreground save path prepares immediately so the user can inspect the result. WorkManager is a redundant deferrable refresh path, not a correctness dependency.

## Founder UI

Wake Alarm Lab now exposes a Tomorrow Contract section for the current next Wake Occurrence:

- multiline private intention field;
- optional First Move;
- save + local prepare;
- clear private preparation;
- prepared-state diagnostic;
- prepared morning preview;
- explicit offline wake-time read result showing either prepared content or generic fallback.

This is still founder/product-lab UI. It is not final onboarding or night-before product design.

## Offline and failure behavior

Wake-time plan loading is a local operation.

```text
private state available + valid
    → PreparedWakePlan

no contract
missing plan
stale source revision
wrong occurrence
unsupported format
checksum mismatch
credential storage unavailable before unlock
    → generic local fallback
```

None of these states may prevent the normal Alarm Kernel from making sound or exposing local controls.

## Privacy proof points

M6 deliberately does not modify:

- `CriticalWakeSnapshot`;
- `CriticalWakeStore`;
- reliability trace payloads;
- AlarmReceiver payloads;
- alarm notification/full-screen intents.

Therefore Tomorrow Contract text and prepared private speech remain outside the device-protected trust-critical path.

Android instrumentation additionally verifies that the private store refuses a device-protected Context.

## Tests

Pure Kotlin coverage verifies:

- deterministic preparation;
- private intention affects prepared content;
- changed source content changes checksum;
- tampering fails checksum validation;
- stale source revision is rejected;
- unsupported plan format fails closed.

Android instrumentation verifies:

- credential-protected contract + plan round-trip;
- validated plan survives persistence;
- clearing removes both private files;
- device-protected storage is rejected.

CI remains the source of truth for build/lint/test status on PR #20.

## Intentionally deferred

M6 does **not** production-wire private prepared lines or TTS into `WakeActivity` / `AlarmPlaybackService` yet. Physical M2 evidence is still required before optional character/personalized speech can coexist with critical alarm audio without weakening reliability.

Prepared audio files are also deferred for the same reason. The Prepared Wake Plan schema can evolve when measured audio behavior justifies that capability.

No cloud preparation is introduced. Supabase/Vercel remain irrelevant to M6 wake authority.

## Exit evidence required before merge

- docs check green;
- all `:wake-core` tests green;
- Android lint/compile green;
- instrumentation APK compilation green;
- dedicated emulator instrumentation lane green;
- manual/physical validation remains a separate M2 evidence item and is not claimed by this milestone.

# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first  
**Current engineering phase:** M7 Wake Learning v0 in review, while M2 physical-device reliability evidence remains open  
**Current implementation branch:** `m7/wake-learning-v0`  
**Current PR:** #22  
**Current issue:** #21

## Executive status

Wake My Way is in active native Android development.

Merged into `main`:

- M0 Foundation
- M1 Deep Alarm Kernel + Active Wake Execution
- M2 automated reliability harness + dedicated emulator lane
- M3 deterministic Wake Runtime
- M4 bounded Motion Evidence extraction + thin Android sensor adapter
- M5 Alfred deterministic local character + offline-only Android speech lab
- M6 Tomorrow Contract + Prepared Wake Plan

M6 merged in PR #20 after both Android CI and the API-36 `connectedDebugAndroidTest` device lane passed on the final PR head. It added private night-before context, deterministic local preparation and unlocked-only morning enrichment without adding a dependency to alarm delivery.

M7 is now the active milestone. PR #22 implements the first local, deterministic, explainable learning loop. It derives privacy-minimized Wake Outcomes and may change one bounded future policy parameter only after repeated evidence.

**No physical-device reliability percentile claim has been made.** Emulator/device-test evidence does not prove real locked-screen audio latency, Doze, reboot-before-unlock, OEM power management, Android 17 physical behavior, or coexistence of critical alarm audio with optional character speech.

## Canonical architecture

Root [`../CONTEXT.md`](../CONTEXT.md) owns vocabulary and invariants.

### Trust-critical wake path

```text
Wake Schedule
    ↓
Alarm Kernel
    ↓
Critical Wake Snapshot
(device-protected, non-sensitive)
    ↓
AlarmManager.setAlarmClock()
    ↓
AlarmReceiver
    ↓
Active Wake Execution
AlarmPlaybackService
    ├─ USAGE_ALARM bundled local audio
    ├─ notification / full-screen wake intent
    └─ durable Stop / Snooze
```

Cloud, Vercel, Supabase, AI, WorkManager, Tomorrow Contract, Prepared Wake Plan and Wake Learning do not participate in alarm delivery.

### Deterministic in-session behavior

```text
platform/user facts
       ↓
   typed WakeInput
       ↓
    WakeRuntime
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
       ↓
 typed WakeDirective
```

Wake Runtime remains behavioral authority. A Wake Session is pinned to one immutable `WakePolicy.version`.

### M6 private preparation

```text
Tomorrow Contract
      ↓
WakePreparationManager
      ├─ credential-protected AtomicFile state
      ├─ deterministic local preparation
      ├─ SHA-256 validation
      └─ on-demand WorkManager refresh
      ↓
Prepared Wake Plan
      ↓
valid + user/keyguard unlocked
      → optional WakeActivity text enrichment

missing / stale / corrupt / locked / Direct Boot
      → generic local wake UI
```

### M7 learning path

```text
privacy-minimized semantic session facts
                ↓
        WakeOutcomeDeriver
                ↓
         WakeOutcomeRecord
     ├─ engagement timing
     ├─ meaningful movement timing
     ├─ Activation Completion
     ├─ terminal/snooze/depth facts
     ├─ optional calibration
     └─ annoyance / agency
                ↓
            WakeLearning
     deterministic / bounded / versioned
                ↓
       NoChange | PolicyUpdated
                ↓
        LearnedWakeProfile
 credential-protected derived local state
                ↓
       future Wake Session only
```

Learning never mutates an active Wake Session and never controls Alarm Kernel execution.

## Implemented and merged milestones

### M0 Foundation

- native Kotlin/Compose Android project;
- physical modules remain `:app`, `:wake-core`, `:benchmark`;
- pure Kotlin schedule/occurrence domain;
- deterministic recurrence/DST handling;
- CI, lint, tests and APK artifacts.

Current toolchain:

```text
Android Gradle Plugin  9.4.0
Kotlin                 2.4.20
Gradle                 9.6.1
JDK                    17
Compose BOM            2026.08.00
compileSdk             37
targetSdk              36
minSdk                 29
```

### M1 Deep Alarm Kernel

Merged in PR #8.

Implemented:

- `AlarmManager.setAlarmClock()`;
- Direct-Boot-safe Critical Wake Snapshot;
- foreground Active Wake Execution;
- bundled alarm audio and fallback;
- crash-safe schedule replacement;
- durable Stop/Snooze semantics;
- stale occurrence rejection;
- boot/time/timezone/package reconciliation;
- active playback recovery;
- non-sensitive timing evidence.

### M2 Reliability Harness

PR #10 merged the Wake Alarm Lab/reliability slice. PR #11 merged the dedicated API-36 emulator workflow.

The device workflow now runs automatically for Android pull requests, so instrumentation tests are executed rather than only compiled.

Issue #9 remains open for physical-device evidence covering locked-screen delivery, real audible latency, Doze, process/service recreation, reboot-before-unlock, capability loss/restoration, OEM power behavior and audio coexistence.

### M3 Wake Runtime

Merged in PR #13.

Implemented:

- durable phases;
- typed inputs/directives;
- versioned Wake Policy;
- internal Activation Evidence authority;
- deterministic escalation;
- capability degradation;
- durable Stop/Snooze handshakes;
- replay and terminal invariants;
- hard policy-version matching for active sessions.

### M4 Motion Evidence

Merged in PR #15. See [`implementation/m4-motion-evidence.md`](implementation/m4-motion-evidence.md).

Implemented:

- bounded pure `MotionEvidenceExtractor`;
- pickup/orientation/sustained movement evidence;
- correlated-evidence protection;
- no raw sensor persistence;
- thin Android observer.

Thresholds remain physical-device calibration hypotheses.

### M5 Alfred local character

Merged in PR #17. See [`implementation/m5-alfred-local.md`](implementation/m5-alfred-local.md).

Implemented:

- versioned deterministic character model;
- Alfred v1 curated copy;
- semantic/shaming guardrails;
- offline voice selection;
- Android local TTS adapter with silent fallback;
- founder diagnostics.

Production character speech remains physical-reliability gated.

### M6 Tomorrow Contract + Prepared Wake Plan

Merged in PR #20. See [`implementation/m6-tomorrow-contract.md`](implementation/m6-tomorrow-contract.md).

Implemented:

- versioned `TomorrowContract` and `PreparedWakePlan` domain;
- deterministic local Alfred-based preparation;
- occurrence/revision binding and SHA-256 integrity;
- credential-protected `noBackupFilesDir` persistence;
- device-protected storage rejection;
- on-demand WorkManager initialization kept off cold alarm startup;
- founder contract editor/preview;
- unlocked-only `WakeActivity` text enrichment;
- `FLAG_SECURE` for private morning text;
- generic local fallback for Direct Boot/locked/corrupt/stale state;
- pure and Android instrumentation coverage.

The final M6 PR head passed both Android CI and API-36 emulator instrumentation before merge.

## M7 in review: Wake Learning v0

Tracks issue #21 and PR #22. Canonical implementation note: [`implementation/m7-wake-learning-v0.md`](implementation/m7-wake-learning-v0.md).

Current implementation candidate includes:

- `WakeOutcomeRecord` derived from ordered semantic timeline facts;
- explicit `Activation Completion` vs `Confirmed Wake Success` model;
- calibration states `GOT_UP`, `RETURNED_TO_BED`, `GOT_UP_LATER`;
- unknown calibration remains unknown;
- optional bounded annoyance/agency feedback;
- versioned `WakeLearningPolicy`;
- deterministic `WakeLearning` decision engine;
- minimum sample + repeated-pattern hysteresis;
- only current-policy-version outcomes can drive the next change;
- v0 learnable surface restricted to `WakePolicy.movementPromptDelay`;
- 5-second bounded steps within 5s–45s;
- earlier prompt for repeated quick-engagement/slow-movement patterns when guardrails permit;
- later prompt for repeatedly fast movement plus high friction;
- return-to-bed calibration excluded from positive evidence for relaxing friction;
- human-readable explanations and typed no-change reasons;
- learned policy versions apply only to future sessions;
- credential-protected single-file `AtomicFile` storage;
- bounded outcome history;
- safe default-policy fallback on corrupt/unsupported derived state;
- reset and delayed calibration support;
- founder Wake Learning Lab with inspectable fixtures;
- pure Kotlin and Android instrumentation coverage.

Explicitly not learnable in v0:

- Activation Completion threshold;
- activation-evidence weights;
- alarm delivery/audio behavior;
- Stop/Snooze authority;
- privacy/safety rules;
- arbitrary character prompts.

M7 is not complete until PR #22 passes docs, pure tests, Android lint/build and API-36 instrumentation on its final head.

## Privacy boundaries

Never persist in device-protected critical/reliability state:

- Tomorrow Contract raw text;
- calendar content;
- transcripts/microphone audio;
- prompts;
- secrets/tokens;
- private generated speech;
- raw motion streams;
- learned private explanations/profile state.

M6 and M7 private/derived state lives in credential-protected `noBackupFilesDir` and is optional. A pre-unlock wake remains generic and locally actionable.

## Milestone status

| Milestone | Status |
|---|---|
| Discovery / product definition | Complete v1 |
| UX psychology / flows | Complete v1 |
| Brand direction | Complete v1 |
| Architecture review / plan hardening | Complete |
| M0 Foundation | **Merged / complete** |
| M1 Deep Alarm Kernel + Active Wake Execution | **Merged / implementation complete** |
| M2 Reliability Harness | **Automated harness + emulator lane merged; physical-device evidence open (#9)** |
| M3 Wake Runtime | **Merged / pure runtime complete** |
| M4 Motion Evidence | **Merged / isolated evidence layer; physical calibration open** |
| M5 Alfred local experience | **Merged / complete, PR #17** |
| M6 Tomorrow Contract | **Merged / complete, PR #20** |
| M7 Wake Learning v0 | **In review, PR #22** |
| M8 Voice architecture spike | Not started |
| M9 Realtime conversation | Not started |
| M10 Useful context | Not started |
| M11 Dogfood hardening | Not started |
| M12 Closed beta | Not started |

## Exact next work

1. Make PR #22 green across pure tests, Android lint/build and API-36 emulator instrumentation.
2. Resolve compile/test/review defects without expanding the v0 learning surface.
3. Prove the stored learned policy survives Android persistence and corruption fallback in the device lane.
4. Merge M7 only after final-head validation.
5. After merge, begin M8 as a measured realtime voice architecture spike, not a production dependency.
6. Continue physical M2 issue #9 evidence in parallel when a real device execution path is available.
7. Gather real founder dogfood data before treating M7 timing thresholds as calibrated product truth.

## Cloud / future stack status

Vercel remains the preferred future non-critical web/API host. Supabase remains the preferred managed PostgreSQL/Auth/Storage platform when a real cloud capability requires them.

Neither is required for M0–M7 local wake authority.

M8 will measure realtime voice transport choices rather than assume one in advance.

## Current blockers / risks

There is no blocker to isolated local product/domain development.

Open proof/risk boundaries:

- physical Android alarm reliability evidence (#9);
- motion threshold calibration on real devices;
- character TTS coexistence with critical alarm audio;
- M7 thresholds are hypotheses until real dogfood;
- production automatic Wake Outcome capture depends on later safe production wiring of richer runtime/motion evidence;
- PR #22 validation remains in progress.

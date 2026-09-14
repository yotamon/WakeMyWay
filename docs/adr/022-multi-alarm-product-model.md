# ADR-022: Multi-alarm product model without weakening Alarm Kernel reliability

**Status:** Accepted for implementation  
**Date:** 2026-09-15

## Context

The approved consumer mockup includes a first-class Alarms destination, multiple independently enabled alarms, Create/Edit Alarm, per-alarm sound, Voice Check-In and snooze configuration.

WakeMyWay currently has a deliberately narrow V1 model: one enabled `WakeSchedule`, one next `WakeOccurrence`, one `CriticalWakeSnapshot`, and one durable exact Android registration chain. That simplicity was correct while the product had one wake plan, but it cannot honestly support the new consumer product by placing a multi-alarm UI above the existing single-schedule store.

The reliability architecture must remain stronger than the presentation layer. A visual expansion is not sufficient justification to weaken stale-occurrence rejection, Direct Boot recovery, snooze replacement ordering, terminal action idempotency, or local fallback.

## Decision

Introduce a richer user-facing `AlarmDefinition` model and evolve the Alarm Kernel to manage **independent schedule slots** for every enabled alarm.

The layers become:

```text
AlarmDefinitionRepository
        |
        v
Alarm Planner / Compiler
        |
        v
WakeScheduleSlot[]
        |
        v
Alarm Kernel
        |
        +--> durable critical slot state
        +--> exact Android registration per next occurrence
        +--> one active execution authority
        +--> reconciliation/collision handling
```

### Product model and kernel model stay separate

`AlarmDefinition` contains consumer configuration such as label, sound choice, Voice Check-In, Character, Voice Style and snooze policy.

The Alarm Kernel receives only the deterministic local schedule/execution information required to deliver the alarm safely. Product metadata that is not required during Direct Boot does not enter critical storage.

### One active physical wake execution

Multiple future occurrences may be independently registered, but only one Active Wake Execution is authoritative at a time.

If multiple occurrences become due close enough to collide, the kernel resolves them deterministically. The initial policy is:

1. an already-active occurrence remains authoritative;
2. another firing occurrence is rejected or deferred according to explicit kernel policy rather than starting a competing playback service;
3. the owning schedule slot is reconciled after the active chain reaches a terminal state.

The exact collision window/policy must be covered by deterministic tests before production exposure.

### Per-alarm isolation

Every enabled alarm has independent durable schedule authority.

Operations on alarm B must not cancel, overwrite, complete or snooze alarm A.

Occurrence identity must carry enough ownership information for stale-intent rejection to remain local and deterministic.

### Wake Ready becomes per-alarm

Each enabled alarm has a readiness result. The application may also expose aggregate readiness such as:

- all enabled alarms ready;
- N alarms require attention;
- no enabled alarms.

Base Wake Ready continues to depend only on critical Android delivery/control capabilities and durable local scheduling, not account/cloud/AI enrichment.

### Legacy migration is fail-safe

The existing single `WakeSchedule` must migrate atomically to one `AlarmDefinition` and one new schedule slot.

Migration must not clear the old critical state until the replacement critical state is durably written and safely registered. If migration fails, the previous wake remains authoritative.

## Product configuration model

Target shape:

```text
AlarmDefinition
├── id
├── label
├── enabled
├── zoneId
├── schedulePattern
├── soundId
├── voiceCheckInEnabled
├── characterId
├── voiceStyle
├── snoozePolicy
├── tomorrowContractPolicy
├── firstMoveDefault
├── revision
├── createdAt
└── updatedAt
```

This model belongs outside Direct Boot critical storage.

## Critical state target

A future critical store should conceptually contain:

```text
CriticalAlarmState
├── generation
├── slots: Map<AlarmDefinitionId, CriticalScheduleSlot>
│   └── CriticalScheduleSlot
│       ├── WakeSchedule
│       ├── nextOccurrence
│       ├── registeredOccurrenceId
│       ├── enabled
│       └── generation/revision
└── activeExecution
    └── occurrence + owning alarm slot
```

The physical serialization may differ, but the ownership/invariant model must remain equivalent.

## Consequences

### Positive

- the mockup's multi-alarm experience becomes real rather than cosmetic;
- per-alarm sound/voice/snooze configuration has a stable owner;
- Home can show the true next alarm across all enabled definitions;
- Insights can attribute outcomes to a stable alarm identity;
- one alarm can be edited/disabled without invalidating unrelated alarms;
- the existing deterministic Wake Runtime remains unchanged in responsibility.

### Costs

- Critical Wake storage becomes more complex;
- reconciliation must iterate and repair independent schedule slots;
- OS registration tests must cover multiple pending exact alarms;
- migration requires special care;
- collision semantics must become explicit;
- product UI and repository state can no longer assume a single `currentSchedule()`.

## Rejected alternatives

### Keep one kernel schedule and fake multiple alarms in UI

Rejected. Toggling/editing one visible alarm would implicitly replace another, violating user expectation and reliability truth.

### Schedule only the globally earliest alarm

Rejected as the long-term model. It creates unnecessary dependence on app/process reconciliation after every occurrence and makes independent alarm readiness opaque. It may be used internally during a migration spike only if proven safe, but it is not the target architecture.

### Put the full AlarmDefinition in device-protected critical storage

Rejected. Labels, Tomorrow Contract behavior, account metadata and other product state are not required for Direct Boot delivery and unnecessarily expand the sensitive critical surface.

### Move scheduling to cloud/account infrastructure

Rejected. Account and cloud remain optional. Alarm delivery stays local-first.

## Rollout sequence

1. add pure product models and tests;
2. add local credential-protected `AlarmDefinitionRepository`;
3. introduce critical multi-slot state behind Alarm Kernel without changing consumer UI;
4. migrate the legacy single schedule with fail-safe fallback;
5. add cross-alarm and collision tests;
6. expose Alarms list and Create/Edit UI;
7. connect Home to the real repository;
8. retire legacy single-schedule UI/contracts only after physical wake proof.

## Validation

The migration is not complete until tests prove at least:

- two enabled recurring alarms retain independent next occurrences;
- editing alarm A leaves alarm B's occurrence untouched;
- disabling alarm A leaves alarm B registered;
- stale occurrence from a previous revision of alarm A cannot begin active execution;
- snoozing alarm A replaces only alarm A's chain;
- active alarm A prevents a competing playback execution from alarm B;
- reconciliation after reboot restores all future eligible occurrences;
- timezone/time changes recalculate every relevant slot;
- existing legacy single schedule migrates without losing its next wake;
- Stop/Snooze and full-screen presentation remain local and immediate.

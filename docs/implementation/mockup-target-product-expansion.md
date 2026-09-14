# WakeMyWay consumer product target

**Status:** canonical target for the consumer product expansion  
**Visual source of truth:** the approved sunrise-wave mockup direction  
**Product rule:** everything shown in the approved mockup becomes real functionality, while existing WakeMyWay-specific strengths remain first-class rather than being removed to match the picture.

## 1. Product target

WakeMyWay becomes a polished consumer wake product with four normal destinations:

```text
Home | Alarms | Insights | Profile
```

The app should feel visually continuous with the approved mockup: airy morning surfaces, sunrise/landscape illustration, Midnight Navy typography, Sunrise Peach primary actions, Dawn Lavender atmospheric depth, soft rounded cards, and a dramatic dark active-wake world.

The product must not become a generic alarm clone. Its differentiators remain:

- Wake Ready and trustworthy local delivery;
- deterministic adaptive Wake Runtime;
- voice-first wake interaction;
- Tomorrow Contract;
- First Move;
- minimum effective friction;
- private/local-first behavior;
- Wake Learning and useful wake outcomes.

## 2. Non-negotiable architecture boundaries

Visual expansion and feature expansion must not weaken the alarm reliability model.

The following remain authoritative:

- Alarm Kernel owns accepted wake intent through durable local scheduling, exact Android registration, reconciliation, snooze replacement, active execution and terminal actions.
- Wake Runtime owns in-session behavioral state and decisions.
- `WakeActivity` is presentation, never durable alarm authority.
- network, account, cloud AI, insights sync and remote services are optional enrichment only.
- Stop and Snooze remain local, immediate and reachable.
- Direct Boot critical state remains minimal and non-sensitive.
- Tomorrow Contract/private text never enters the Direct Boot snapshot.
- a visual animation, illustration, font download or remote asset may never become a critical wake dependency.

## 3. Canonical navigation

Normal application navigation becomes:

```text
                           WakeMyWay
                               |
        +----------------------+----------------------+
        |                      |                      |
      Home                   Alarms                Insights               Profile
        |                      |                      |                      |
  Next wake             Alarm list             Wake trends           Identity/account
  Wake Ready            Create alarm           Sleep trends          Routines
  Tomorrow Contract     Edit alarm             Progress/streak       Sound defaults
  Morning summary       Sound chooser          Outcomes              Voice defaults
  Quick insight         Voice check-in                                Notifications
                         Snooze                                        Appearance
                         Repeat                                        Privacy
                                                                       About
```

`WakeActivity` remains outside this graph as the dedicated active-alarm surface.

## 4. Screen inventory and real content

### 4.1 Splash / Welcome

Purpose: branded launch transition only.

Content:

- sunrise-wave hero mark;
- WakeMyWay wordmark;
- `BRIGHTER MORNINGS. YOUR WAY.`;
- `A calmer, brighter you starts here.`;
- layered sunrise landscape.

Behavior:

- short deterministic transition into onboarding for first launch or Home for returning users;
- no network dependency;
- no fake loading.

### 4.2 Onboarding

Purpose: explain the product before asking for Android capabilities.

Pages:

1. **Wake up your way**
   - natural, voice-first alarms;
   - personalized to routines;
   - gentle but effective;
   - private/local-first critical path.
2. **Your alarm should adapt to you**
   - voice check-in;
   - movement evidence;
   - snooze without judgment;
   - minimum effective friction.
3. **Give tomorrow a reason**
   - Tomorrow Contract;
   - First Move;
   - prepared local context.
4. **Built to wake you even when the cloud cannot**
   - local exact scheduling;
   - offline fallback;
   - clear permissions and Wake Ready.

Actions:

- `Get Started`;
- `Skip` where appropriate;
- `I already have an account` only after account support exists;
- Android permissions requested contextually after the user understands why they are needed.

### 4.3 Home

Primary consumer dashboard, visually close to the approved mockup but populated with WakeMyWay truth.

Content hierarchy:

1. branded top bar;
2. contextual greeting;
3. **Next Alarm** hero card;
4. wake time, date/pattern and enabled state;
5. selected sound;
6. Wake Ready state;
7. Tomorrow Contract preview;
8. optional quick insight card;
9. optional streak/progress card;
10. repair card only when a required capability is missing;
11. bottom navigation.

Home never shows fake sleep data. Insight cards appear only when real local or Health Connect data exists.

### 4.4 Alarms list

The mockup's alarm list becomes real product functionality.

Each alarm row shows:

- time;
- optional label;
- repeat summary;
- selected sound;
- enabled toggle;
- optional small character/voice indicator;
- Wake Ready warning only when the enabled alarm is not safely schedulable.

Actions:

- add;
- edit;
- enable/disable;
- delete;
- reorder is deferred unless evidence shows value.

### 4.5 Create / Edit Alarm

The approved mockup's tabbed editor becomes a real configuration surface.

Sections:

#### Basic

- time;
- label;
- repeat days or one-shot tomorrow;
- enabled state.

#### Sound

- sound selection;
- preview;
- volume/gain where product-safe;
- current built-in branded choices:
  - Morning Light;
  - Soft Start;
  - Morning Pulse.

#### Voice

- Voice Check-In on/off;
- Character, beginning with Alfred;
- Voice Style:
  - Default;
  - Motivational;
  - Minimal;
- prompt preview;
- microphone/on-device recognition readiness.

#### More

- snooze duration/policy;
- Tomorrow Contract behavior;
- First Move defaults;
- future advanced wake preferences only when supported by real runtime behavior.

The editor must not surface controls that do not map to implemented behavior.

### 4.6 Sound chooser

Real sound library with preview.

Initial bundled sounds:

- **Morning Light** — bright, peaceful, hopeful;
- **Soft Start** — calm, warm, ease-in;
- **Morning Pulse** — modern, uplifting, more energetic.

Requirements:

- local bundled availability;
- preview does not interfere with active alarm audio;
- speech intelligibility remains more important than music loudness;
- active alarm playback resolves the sound choice without network access.

### 4.7 Voice Check-In

Purpose: configure the voice interaction, not the behavioral authority.

Content:

- enabled toggle;
- Character;
- Voice Style;
- example wake phrase;
- local recognition capability state;
- privacy explanation;
- preview where safe.

Character and Voice Style remain distinct concepts:

```text
Character = personality/language identity
Voice Style = presentation energy/length
Wake Policy = deterministic behavioral strategy
```

A voice model never owns Wake Policy or state transitions.

### 4.8 Tomorrow Contract

Remains a first-class WakeMyWay feature even though the earlier visual mockup omitted it.

Content:

- private/local badge;
- concrete alarm/date association;
- `What makes tomorrow matter?`;
- microphone dictation;
- freeform reason/reminder;
- optional First Move;
- save;
- clear;
- privacy note;
- prepared/offline readiness state.

The user can enter it from Home and from the relevant alarm configuration.

### 4.9 Insights

Insights must be based on real evidence, not decorative numbers.

Sources:

```text
WakeMyWay local history
+ Wake Runtime outcomes
+ snooze events
+ engagement/activation timing
+ optional user feedback
+ optional Android Health Connect sleep data
```

Initial metrics:

- wake consistency;
- average time to engagement;
- average time to Activation Completion;
- snooze frequency;
- intervention depth;
- local Wake Success calibration where available;
- streak of mornings meeting the target window;
- selected-period trend.

Optional Health Connect metrics when permission/data exist:

- bedtime;
- wake time;
- sleep duration;
- sleep-session consistency.

No medical claims, fabricated sleep quality score or invented health conclusions.

### 4.10 Progress / streak

Progress is supportive, not gamified pressure.

Allowed:

- `5 mornings on target`;
- `You needed less intervention this week`;
- wake consistency trend.

Avoid:

- punishment;
- broken-streak shame;
- artificial engagement badges;
- competitive leaderboards.

### 4.11 Profile / Settings

Sections:

- Account / identity;
- Routines;
- Sound Preferences;
- Voice Settings;
- Sleep Insights / Health Connect;
- Notifications;
- Appearance;
- Privacy;
- About.

Account remains optional. Core local alarm functionality works without sign-in.

### 4.12 Account

Purpose:

- preference sync;
- cross-device migration;
- cloud-backed history where explicitly enabled;
- future subscription/entitlement identity.

Account is never required to create or execute a local alarm.

### 4.13 Routines

Represents user-owned morning preferences such as common First Moves and routine suggestions.

It is not a full task manager.

Potential first-version data:

- preferred First Moves;
- typical morning duration;
- optional weekday differences;
- common morning context.

### 4.14 Active Wake

Active Wake remains a sequence of presentations mapped from real Wake Runtime state.

#### Emerging / Speaking

- Midnight world;
- WakeMyWay compact lockup;
- Alfred identity;
- current time;
- sunrise-wave hero;
- current spoken line or deterministic fallback;
- Snooze / Stop.

#### Listening

- current Alfred line;
- time;
- glowing sunrise-wave;
- `Listening`;
- `Answer out loud. A real reply is part of this wake.`;
- Snooze / Stop.

#### Moving

- current instruction;
- time;
- stronger Wake Line;
- `Motion detected`;
- Snooze / Stop.

#### Oriented

- transition to light morning world;
- greeting;
- date;
- prepared reminder from Tomorrow Contract when available;
- `What's first?`;
- First Move;
- `Keep moving`;
- Snooze / Stop until the runtime is complete.

#### Complete

- `MORNING STARTED`;
- current time;
- settled sunrise-wave;
- First Move;
- `Finish`.

## 5. Consumer data model target

### AlarmDefinition

A user-facing alarm definition is richer than today's low-level `WakeSchedule` intent.

Target model:

```text
AlarmDefinition
├── id
├── label
├── enabled
├── zoneId
├── schedulePattern
│   ├── one-shot date/time OR
│   └── recurring days + local time
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

`AlarmDefinition` is product state. It is not an Android alarm and not a Wake Occurrence.

### Compiled Wake Schedule

The scheduling layer compiles each enabled AlarmDefinition into the smallest deterministic wake schedule intent needed by Alarm Kernel.

The product model may grow without pushing labels, profile data or cloud-only preferences into Direct Boot critical state.

## 6. Multi-alarm reliability model

The existing single-schedule kernel cannot simply be called repeatedly because each commit currently replaces the single durable snapshot.

Target architecture:

```text
AlarmDefinitionRepository
          |
          v
Enabled Alarm Definitions
          |
          v
Alarm Planner / Compiler
          |
          v
Independent Wake Schedule Slots
          |
          v
MultiAlarmKernel
          |
          +--> next occurrence per enabled alarm
          |
          +--> exact Android registration per occurrence
          |
          +--> one Active Wake Execution at a time
          |
          +--> deterministic conflict resolution
          |
          +--> reconciliation after boot/time change/update
```

Critical invariants:

- every registered PendingIntent maps to exactly one durable occurrence;
- stale occurrences cannot become active after the owning alarm changed/disabled;
- alarm A cannot cancel alarm B's exact registration;
- snoozing alarm A replaces only alarm A's active chain;
- disabling alarm B cannot affect an active alarm A;
- one physical active wake session at a time;
- if two occurrences collide, the kernel applies a deterministic collision policy and never creates two competing playback services;
- Wake Ready is computed per alarm and as an aggregate product state;
- migration from the legacy single schedule is lossless.

## 7. Legacy migration

On first launch after the multi-alarm schema ships:

1. read the existing enabled legacy WakeSchedule;
2. if present, convert it to one AlarmDefinition preserving time, weekdays/one-shot intent and enabled state;
3. preserve associated Tomorrow Contract only if its occurrence relationship remains valid;
4. write the new repository atomically;
5. build the new critical scheduling state;
6. only after successful durable migration, retire the legacy schedule snapshot;
7. if migration fails, keep the legacy critical path intact rather than silently dropping the user's alarm.

## 8. Insights storage boundary

Wake history should be local-first.

Recommended first-version event/outcome store:

```text
WakeHistoryEntry
├── alarmDefinitionId
├── occurrenceId
├── scheduledAt
├── startedAt
├── firstEngagementAt
├── activationCompletedAt
├── endedAt
├── snoozeCount
├── maxInterventionDepth
├── fallbackLevel
├── completionKind
├── optional user feedback
└── policyVersion
```

Health Connect data remains in Health Connect and is queried through its public APIs. Do not mirror more health data than product behavior requires.

## 9. Visual fidelity contract

The approved mockup is the visual benchmark.

Every consumer screen must converge on:

- airy warm background rather than dense utility UI;
- layered sunrise/mountain illustration language;
- Midnight Navy primary typography;
- Sunrise Peach primary actions;
- Dawn Lavender secondary atmosphere;
- soft white/cream cards;
- large intentional whitespace;
- 24–32dp major card radii;
- pill-like primary controls;
- restrained iconography;
- clean modern rounded sans typography;
- branded bottom navigation;
- dramatic dark Active Wake;
- the sunrise-wave motif as a functional visual language.

The app should be recognizable as WakeMyWay from a cropped screenshot even when the wordmark is hidden.

## 10. Bottom navigation behavior

Tabs:

- Home;
- Alarms;
- Insights;
- Profile.

Rules:

- each tab retains its own sensible navigation state where practical;
- alarm editor and deep settings push above the tab shell;
- Active Wake is never a tab destination;
- debug/founder Wake Lab remains outside the normal consumer navigation and visible only in debuggable builds;
- no tab exists until its backed functionality is real.

During staged implementation, unreleased tabs may be hidden rather than presenting fake screens.

## 11. Delivery plan

### Phase A — architecture foundation

- add canonical AlarmDefinition model;
- add sound / voice-style / snooze configuration models;
- create local AlarmDefinitionRepository;
- design and implement multi-alarm critical state;
- migrate legacy single schedule;
- preserve reliability tests and add cross-alarm tests;
- add four-tab navigation shell only when Home + Alarms are real.

### Phase B — core mockup product

- Alarms list;
- Create/Edit Alarm;
- branded sound library with Morning Light / Soft Start / Morning Pulse;
- Voice Check-In configuration;
- Home redesign using the real alarm repository;
- Tomorrow Contract association with a concrete alarm/occurrence;
- bottom navigation with Home + Alarms and hidden unfinished destinations if necessary.

### Phase C — profile and onboarding

- branded Splash;
- first-run onboarding;
- Profile shell;
- Routines;
- sound defaults;
- voice defaults;
- Notifications;
- Appearance;
- Privacy;
- About;
- optional account identity and sync boundary.

### Phase D — insights

- durable wake history;
- streak/progress derivation;
- Insights screen;
- optional Health Connect integration;
- real sleep/wake trend visualization;
- no fake metrics.

### Phase E — fidelity and motion hardening

- screen-by-screen comparison against approved mockup;
- illustration refinement;
- typography refinement;
- motion/reduced-motion path;
- accessibility;
- large text / RTL / localization checks;
- final Roborazzi baselines;
- physical-device design and wake testing.

## 12. Acceptance criteria

The expansion is complete only when all of the following are true:

### Product

- every visible mockup feature maps to working behavior;
- Tomorrow Contract and Wake Ready remain first-class;
- multiple alarms are real and independent;
- account is optional;
- Insights use real data;
- all sound choices are locally resolvable;
- active wake remains deterministic.

### Reliability

- multi-alarm scheduling survives reboot/reconciliation;
- one alarm cannot invalidate another;
- stale PendingIntents are rejected;
- snooze replacement is durable;
- Stop/Snooze remain immediate;
- no remote dependency is introduced into alarm delivery;
- migration preserves an existing user's configured wake.

### Visual

- first impression matches the approved mockup family;
- Home, Alarms, Create/Edit, Sound, Voice, Insights, Profile and Active Wake feel like one authored product;
- no screen looks like default Android settings UI;
- sunrise-wave identity is visible without becoming repetitive;
- no fake visual-only controls remain.

### Quality

- wake-core unit tests pass;
- app unit/Robolectric tests pass;
- lint passes;
- instrumentation compiles and device reliability tests pass;
- curated visual regression passes;
- physical phone wake test passes after each Alarm Kernel migration step;
- accessibility semantics and minimum touch targets remain intact.

## 13. Implementation principle

Do not attempt to land the entire expansion as one giant rewrite. The product target is singular, but implementation should be a sequence of small, reversible, fully tested slices.

Each slice must leave `main` in a reliable, coherent state and must not expose UI for behavior that is not yet implemented.
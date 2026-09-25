# Android home widget

**Status:** Implemented in PR #130 under an explicit product-owner exception to the 1.0 feature freeze; final physical launcher acceptance remains required before broader release claims.  
**Platform:** Android home screen  
**Implementation:** Jetpack Glance AppWidget 1.2.0  
**Owner:** Android product surface  
**Last updated:** 2026-09-25

## Product decision

WakeMyWay should have one responsive Android home widget centered on the user's **Next Wake**.

The widget is not a miniature copy of the app and is not a new scheduling system. Its job is to answer, at a glance:

1. **When is my next wake?**
2. **Is it Wake Ready?**
3. **Is there one useful thing I should do now?**

The canonical surface is:

> **WakeMyWay · Next Wake**

The widget progressively reveals more information when the launcher gives it more space, while keeping the same product hierarchy.

The widget is a projection of existing product state. It never becomes authority for Alarm Definitions, Wake Schedules, Wake Occurrences, Wake Ready, Active Wake Execution, Wake Runtime behavior, Tomorrow Contract content, or Wake Learning.

## Why this belongs in WakeMyWay

WakeMyWay already has a focused Tonight surface whose primary consumer question is the next wake and its readiness. A home widget is a natural extension of that existing mental model because it can provide reassurance and the next relevant action without requiring the user to enter the app.

The widget should reduce app-opening friction, not create engagement for its own sake.

It should feel like a calm bedside object:

- glanceable;
- trustworthy;
- privacy-conscious;
- low maintenance;
- visually continuous with the sunrise + Wake Line identity;
- useful before sleep, after waking, and when something needs attention.

## User and trigger

**User:** a WakeMyWay user who has configured one or more alarms.

**Primary triggers:**

- checking whether tomorrow's wake is ready;
- checking the next wake time without opening the app;
- noticing that the wake system needs repair;
- preparing tomorrow when preparation is relevant;
- answering a pending Morning Check-In;
- entering the active Wake surface when a wake is already in progress.

## Desired outcome

The user can understand the status of their next wake in roughly one second and can take the single most relevant safe action without navigating through the full application.

## Non-goals

The home widget is not:

- an alternate Alarm Kernel;
- a full alarm editor;
- a scrollable alarm-management dashboard;
- an Active Wake control panel;
- a place for Stop or Snooze;
- a countdown that updates every minute;
- a place to display Tomorrow Contract text;
- a place to display First Move or other private morning context;
- a general morning dashboard, calendar, weather, news, or productivity surface;
- a cloud-dependent surface;
- a reason to add background polling.

## Product invariants

The following invariants are non-negotiable.

### Alarm authority

All alarm mutations continue through the existing product and Alarm Kernel boundaries.

The widget must never directly mutate a preference or persistence record and then assume the alarm is scheduled.

```text
Home widget
    |
    | intent
    v
existing product mutation
    |
    v
Alarm Kernel
    |
    +--> durable state
    +--> Android scheduling
    +--> readiness / failure result
    |
    v
fresh widget projection
```

If scheduling or readiness fails, the widget must present the failure truthfully instead of retaining an optimistic visual state.

### Active wake authority

The widget never owns or terminates Active Wake Execution.

During an active wake it may expose only a safe entry action such as:

> **Wake in progress · Open Wake**

Stop and Snooze remain on the dedicated Wake surface and alarm notification paths that already own terminal behavior.

### Privacy

The widget is intentionally a home-screen surface only.

It does not render:

- Tomorrow Contract free text;
- Prepared Wake Plan text;
- First Move;
- raw recognition/transcript content;
- account details;
- private history.

The widget may show non-sensitive derived state such as:

- next wake time;
- localized date/schedule summary;
- Wake Ready / Needs attention;
- Tomorrow plan ready / not prepared;
- Alfred voice enabled/disabled as a capability label;
- pending Morning Check-In;
- a bounded list of upcoming alarms.

A custom alarm label should not be required for the widget experience. The first implementation should prefer schedule-derived copy over exposing arbitrary user-entered text.

## Surface model

The widget has one conceptual hierarchy:

```text
WakeMyWay
    |
    +--> Next Wake
    |      +--> time
    |      +--> date / schedule
    |      +--> Wake Ready truth
    |
    +--> one contextual secondary action
    |
    +--> expanded-only upcoming wake controls
```

Space changes density, not purpose.

## Responsive layouts

Launcher cell sizes vary. Grid labels such as 2x2 and 4x2 are useful product shorthand, but implementation must use actual DP bounds and launcher behavior as the source of truth.

The implementation uses `SizeMode.Exact` at runtime so launcher-provided bounds are authoritative, then snaps content to three product breakpoints. Generated previews use `SizeMode.Responsive` with the same representative sizes:

| Product name | Typical launcher footprint | Initial DP target | Purpose |
| --- | --- | --- | --- |
| Compact | ~2x2 | ~110 x 110 dp | next wake + readiness |
| Medium | ~4x2 | ~250 x 110 dp | default hero surface |
| Expanded | ~4x4 | ~250 x 250 dp | richer context + upcoming alarms |

These values remain product breakpoints rather than assumptions about launcher cell geometry. Provider metadata keeps a 4x2 preferred target with horizontal/vertical resizing, while final launcher acceptance remains a physical validation task.

### Compact

Compact is the smallest coherent WakeMyWay promise.

```text
╭──────────────────────╮
│ ☀ WakeMyWay          │
│                      │
│      07:30           │
│   Sat · 26 Sep       │
│                      │
│  ✓ Wake Ready        │
╰──────────────────────╯
```

Rules:

- next wake time is dominant;
- localized date/schedule text is secondary;
- readiness is explicit in text, never color-only;
- tapping the body opens Tonight or the relevant next-wake destination;
- no tiny secondary controls.

Use date/schedule copy that remains truthful without minute-level or midnight polling. A relative label such as "Tomorrow" is allowed only if the update strategy guarantees it will not become stale.

### Medium

Medium is the preferred default and visual hero.

```text
╭──────────────────────────────────────────╮
│ ☀ WakeMyWay                 WAKE READY   │
│                                          │
│ 07:30                                    │
│ Sat · 26 Sep                             │
│                                          │
│ Tomorrow plan ready             Edit ›   │
│ ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  │
╰──────────────────────────────────────────╯
```

The bottom Wake Line is the widget's visual signature.

The contextual row shows exactly one secondary message/action, selected by priority.

Example priorities:

1. active wake -> Open Wake;
2. Wake Ready failure -> Fix wake;
3. pending Morning Check-In -> answer check-in;
4. Tomorrow preparation required -> Prepare tomorrow;
5. Tomorrow preparation ready -> quiet ready state;
6. otherwise -> open Tonight.

### Expanded

Expanded adds useful controls without becoming an alarm-management screen.

```text
╭──────────────────────────────────────────╮
│ ☀ WakeMyWay                 WAKE READY   │
│                                          │
│ NEXT WAKE                                │
│ 07:30                                    │
│ Sat · 26 Sep                             │
│                                          │
│ Tomorrow plan                       ✓    │
│ Alfred voice                        ✓    │
│                                          │
│ UPCOMING                                 │
│ 08:00  Weekdays                     ●    │
│ 09:30  Weekends                     ○    │
│                                          │
│ + New alarm                              │
│ ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  │
╰──────────────────────────────────────────╯
```

Rules:

- show at most two upcoming/relevant alarms;
- no scrollable collection in the first implementation;
- no dense settings;
- no private alarm/context text;
- quick alarm enable/disable may be exposed only if the action can route through the normal product mutation and can truthfully roll back or report failure;
- if inline toggles make the first Build materially riskier, they are the first optional cut, not a reason to weaken Alarm Kernel boundaries.

## Contextual states

The widget is rendered from a small UI projection. These names are widget presentation states only. They do not create new canonical WakeMyWay domain states.

### 1. No wake configured

```text
WakeMyWay

No wake set

[ Create alarm ]
```

Primary action opens alarm creation.

### 2. Next wake ready

```text
07:30
Sat · 26 Sep

✓ Wake Ready
```

This is the normal steady state.

### 3. Wake needs attention

```text
07:30
Sat · 26 Sep

! Wake needs attention

[ Fix ]
```

The widget must not reduce the condition to generic "error" copy when a known repair path exists.

Tapping Fix opens the existing repair flow. The widget itself does not attempt to reproduce Android permission/setup UI.

### 4. Tomorrow preparation available

When the next wake is otherwise healthy and Tomorrow preparation is the most useful action:

```text
07:30
Sat · 26 Sep

Prepare tomorrow  →
```

No private plan content is shown.

### 5. Tomorrow preparation ready

```text
Tomorrow plan ready  ✓
```

This is a quiet confirmation, not an engagement prompt.

### 6. Pending Morning Check-In

For a bounded post-wake period, a larger widget may become:

```text
Good morning

Did that wake actually stick?

[ Yes ]   [ Not really ]

Next wake · Sat 07:30
```

This reuses the existing calibration mechanic. It does not invent a second feedback system.

Direct responses are allowed only through the same repository/use-case path as the in-app Morning Check-In.

Compact layouts may show a single "Morning check-in" entry action instead of two small buttons.

### 7. Active Wake Execution

```text
Wake in progress

[ Open Wake ]
```

The widget never exposes Stop or Snooze.

### 8. Temporarily unavailable projection

The widget must degrade calmly if normal credential-protected product data cannot be projected.

```text
WakeMyWay

Open the app to refresh
your next wake.
```

Do not show a permanent spinner. Do not infer readiness from stale or partial state.

## Interaction contract

| Interaction | Widget behavior | Authority |
| --- | --- | --- |
| Tap main body | Open Tonight / next wake context | normal app navigation |
| Create alarm | Open alarm creation | normal app navigation |
| Edit next wake | Open the relevant Alarm Definition editor | normal app navigation |
| Prepare tomorrow | Open Tomorrow Contract/preparation flow | normal app navigation |
| Fix wake | Open existing readiness repair flow | existing capability flow |
| Morning Check-In answer | Commit through existing check-in path, then refresh | existing product state |
| Enable/disable alarm | Commit through existing alarm product mutation + Alarm Kernel, then refresh from result | Alarm Kernel remains authority |
| Open Wake | Re-enter the active Wake surface for the authoritative occurrence | Active Wake Execution remains authority |
| Stop/Snooze | **Not exposed** | dedicated Wake/notification paths only |

## Visual system

The widget uses the existing WakeMyWay brand system.

Canonical palette:

- Midnight `#08142F`
- Deep Navy `#10264C`
- Sunrise `#FF9F6D`
- Sunrise Soft `#FFB88F`
- Golden Light `#FFD699`
- Dawn `#A5B4FC`
- Cloud `#F8F7F4`
- Paper `#FFFCF8`

### Identity

The widget should reuse:

- Sunrise + Wake Line geometry;
- planning atmosphere;
- restrained rounded surfaces;
- Midnight typography;
- Sunrise as a purposeful accent.

It should not introduce:

- a separate widget brand;
- generic Material-blue identity;
- AI-orb imagery;
- decorative sunrise illustration;
- excessive shadows;
- animated novelty.

### Appearance

The widget mirrors the selected local planning appearance:

- Daylight;
- Warm Sunrise;
- Soft Dawn.

Appearance changes presentation only. They cannot affect wake behavior.

### Widget-specific assets

Glance UI is not normal Compose UI. Existing canvas-driven Compose identity components must not be assumed reusable directly.

Create small widget-safe vector/drawable assets for:

- Sunrise mark;
- Wake Line;
- any minimal status glyphs that cannot be expressed cleanly with platform icon primitives.

Assets should remain legible at compact widget size and in generated/static previews.

## Accessibility

The widget must meet the same consumer-quality bar as the main app.

Requirements:

- critical status is expressed in text, not color alone;
- interactive targets aim for at least 48 dp where the widget host permits;
- Compact never introduces tiny icon-only actions;
- accessibility descriptions distinguish next wake time, readiness and action;
- RTL layout is tested;
- locale-specific date/schedule text is tested;
- large font / display scaling is tested at every breakpoint;
- truncation removes secondary information before obscuring wake time or readiness;
- TalkBack actions use clear verbs such as "Open next wake", "Fix wake readiness", and "Prepare tomorrow";
- visual contrast remains readable across the three planning atmospheres.

## Data projection

Introduce one privacy-minimized immutable projection for rendering.

Illustrative shape:

```kotlin
data class WakeWidgetSnapshot(
    val nextWake: WidgetNextWake?,
    val readiness: WidgetReadiness,
    val tomorrowPlanState: WidgetTomorrowPlanState,
    val pendingMorningCheckIn: Boolean,
    val activeWake: Boolean,
    val upcomingAlarms: List<WidgetAlarm>,
    val appearance: AppAppearance,
)
```

This is not a persistence model and not a domain authority.

### Projection inputs

The projector may read from existing owners of:

- Alarm Definitions;
- Alarm Kernel schedule/readiness health;
- Tomorrow preparation status;
- Morning Check-In status;
- consumer appearance preference;
- active-wake status when safely available.

It must not require cloud or network access.

### Projection rules

- derive all display state from authoritative local owners;
- limit upcoming alarms before rendering;
- strip private user text;
- prefer stable schedule/date copy over live countdowns;
- if the full projection cannot be trusted, render a degraded/open-app state rather than inventing readiness.

## Architecture

Suggested package shape:

```text
app/
└── widget/
    ├── WakeMyWayWidget.kt
    ├── WakeMyWayWidgetReceiver.kt
    ├── WakeWidgetSnapshot.kt
    ├── WakeWidgetSnapshotProjector.kt
    ├── WakeWidgetUpdater.kt
    │
    ├── action/
    │   ├── ToggleAlarmAction.kt
    │   ├── MorningCheckInAction.kt
    │   └── OpenDestinationAction.kt
    │
    └── ui/
        ├── CompactWakeWidget.kt
        ├── MediumWakeWidget.kt
        ├── ExpandedWakeWidget.kt
        └── WidgetTheme.kt
```

Keep the implementation inside `:app`. A home widget does not justify a new Gradle module.

### Data flow

```text
AlarmDefinitionRepository --------┐
Alarm Kernel / readiness ---------┤
Tomorrow preparation ------------┤
Morning Check-In -----------------┤
Consumer preferences -------------┤
Active execution signal ----------┤
                                  v
                        WakeWidgetSnapshotProjector
                                  |
                                  v
                         WakeWidgetSnapshot
                                  |
                   +--------------+--------------+
                   |                             |
                   v                             v
             Glance renderer                widget action
                                                 |
                                                 v
                                      existing product owner
                                                 |
                                                 v
                                         refresh projection
```

### No parallel state machine

Do not create a widget service that tries to mirror scheduling logic, infer the next occurrence independently, or maintain a second readiness model.

### Glance state

Glance-specific storage, if used at all, is limited to incidental presentation state. It must not store authoritative alarm intent or readiness.

## Update strategy

Prefer event-driven refresh.

Refresh widget instances after meaningful local changes such as:

- Alarm Definition create/edit/delete;
- alarm enable/disable result;
- Alarm Kernel successful scheduling/reconciliation;
- Wake Ready capability repair result;
- time or timezone reconciliation;
- package replacement reconciliation;
- Tomorrow preparation change;
- Morning Check-In creation/completion;
- appearance change;
- Active Wake start/end when practical;
- app foreground when a stale projection may exist.

Avoid:

- per-minute updates;
- countdown-driven updates;
- network refresh;
- a 15-minute WorkManager loop merely to keep the surface alive.

If relative calendar language such as "Tomorrow" is retained, Build must provide a bounded date-boundary refresh so the copy cannot remain wrong after midnight. The simpler preferred baseline is localized weekday/date copy that naturally remains truthful for the represented occurrence.

## Android implementation direction

Use Jetpack Glance rather than hand-authored RemoteViews unless Build uncovers a concrete missing capability.

Current Android guidance supports:

- `GlanceAppWidget`;
- responsive or exact size handling;
- `ActionCallback` for bounded widget actions;
- immediate widget updates after app/widget events;
- Android 15+ generated widget previews with a static fallback for older versions.

Implementation must re-check the current Glance sizing recommendation when Build begins. The Shape intent is three deliberate content breakpoints, not attachment to a specific API class.

Useful platform references:

- https://developer.android.com/develop/ui/compose/glance/create-app-widget
- https://developer.android.com/develop/ui/compose/glance/build-ui
- https://developer.android.com/develop/ui/compose/glance/glance-app-widget
- https://developer.android.com/develop/ui/compose/glance/user-interaction
- https://developer.android.com/develop/ui/compose/glance/generated-previews

## Widget provider metadata

The Build should configure:

- home-screen category only;
- horizontal and vertical resizing;
- bounded min/max sizes matching reviewed breakpoints;
- widget description for the picker;
- a production-quality preview;
- a safe loading layout;
- no aggressive periodic update interval.

Exact XML values are Build details and should be frozen only after the three layouts are tested on representative launchers.

## Widget picker experience

The widget picker is part of the product experience.

Target preview:

```text
WakeMyWay
Your next wake at a glance

╭────────────────────────────╮
│ 07:30          WAKE READY  │
│ Sat · 26 Sep               │
│ Tomorrow plan ready        │
╰────────────────────────────╯
```

Use an Android 15+ generated preview when available and maintain a static fallback for lower versions.

Preview data must be synthetic and privacy-safe.

## Failure semantics

### Alarm enable fails

The widget must not remain visually enabled.

Flow:

```text
tap enable
    |
    v
existing alarm mutation
    |
    v
Alarm Kernel result
   / \
  /   \
ok   failure
|       |
v       v
fresh  fresh truth +
state  Needs attention / open repair
```

### Readiness changes outside the widget

A later authoritative refresh replaces the projection. The widget does not preserve its own conflicting interpretation.

### Process death

No correctness property depends on widget-process lifetime.

### Network unavailable

No degradation is required for the core widget because the core projection is local.

### Direct Boot

The home widget is not a Direct Boot wake surface and must not expand credential-protected product data into device-protected widget state.

Alarm Kernel Direct Boot behavior remains completely independent.

## Performance and battery

The widget should be cheap enough to exist permanently on the home screen.

Rules:

- no network in the render path;
- no per-minute worker;
- no animation requirement;
- no long-running service;
- bounded list sizes;
- small static/vector assets;
- projection work remains lightweight;
- long-running work triggered from an action is handed to the existing appropriate owner rather than performed inside the widget callback.

## Build slices

The implementation was completed as the following reviewable vertical slices after the product owner explicitly authorized a feature-freeze exception for this surface.

### Slice 1 - read-only Next Wake

Deliver:

- Glance receiver/provider;
- privacy-minimized snapshot projector;
- Compact + Medium layouts;
- next wake time/date;
- Wake Ready / Needs attention truth;
- body navigation;
- provider metadata and picker preview;
- event-driven updates for core alarm changes.

Exit criterion:

> The widget can be trusted as a glanceable reflection of the same next-wake truth shown in the app.

### Slice 2 - contextual actions

Deliver:

- Prepare tomorrow / edit plan entry;
- Fix wake entry;
- active wake Open Wake entry;
- pending Morning Check-In behavior.

Exit criterion:

> Every contextual action delegates to an existing product owner and refreshes to authoritative truth.

### Slice 3 - Expanded controls

Deliver:

- Expanded layout;
- at most two upcoming alarms;
- create/edit entry points;
- the expanded alarm summary is one comfortably sized interaction target that opens the existing Alarms surface; inline enable/disable is intentionally not shipped because safe enabling requires the existing scheduling/voice/capability preflight.

Exit criterion:

> More space makes the widget more useful without turning it into a second Alarms screen.

### Slice 4 - hardening

Deliver:

- launcher/device verification;
- RTL;
- large text/display scaling;
- TalkBack;
- visual review/goldens where the widget test surface supports deterministic capture;
- process-death/restart behavior;
- time/timezone/package-replacement refresh;
- battery/update review.

## Validation strategy

### Pure tests

Test `WakeWidgetSnapshotProjector` with deterministic fixtures covering:

- no alarms;
- one ready alarm;
- multiple alarms;
- disabled alarms;
- next occurrence ordering;
- needs-attention readiness;
- Tomorrow preparation absent/ready;
- pending Morning Check-In;
- active wake;
- privacy stripping;
- locale/date formatting policy where formatting belongs outside Android resources.

### Android integration tests

Verify:

- receiver/provider registration;
- widget action routing;
- alarm enable/disable success and failure behavior;
- repair deep link/navigation;
- check-in mutation;
- widget refresh after authoritative changes;
- no dependency on network;
- no Stop/Snooze action exists.

### Visual/responsive review

Review at minimum:

- Compact;
- Medium;
- Expanded;
- Daylight;
- Warm Sunrise;
- Soft Dawn;
- LTR;
- RTL;
- default font;
- large font/display scale;
- empty;
- ready;
- needs attention;
- pending check-in;
- active wake.

### Physical launcher matrix

Before considering the widget production-ready, test at least:

- Pixel Launcher;
- one major OEM launcher;
- smallest accepted size;
- preferred default size;
- expanded size;
- resize transitions;
- widget removal/re-add;
- reboot;
- package update;
- timezone change.

The exact OEM/device matrix should follow available release hardware rather than creating a broad lab requirement for a Shape document.

## Success signals

The widget should be evaluated by whether it reduces uncertainty and navigation friction without increasing alarm risk.

Useful evidence:

- users can correctly identify the next wake and readiness at a glance;
- users understand Needs attention without opening unrelated screens;
- Tomorrow preparation entry is discoverable when relevant;
- no observed false Wake Ready presentation;
- no widget action causes an Alarm Kernel divergence;
- no privacy-sensitive plan text appears on the launcher;
- no meaningful battery/update complaint during dogfood;
- users keep the widget installed because it is useful, not because it is visually novel.

Do not optimize for widget tap count or screen-time engagement.

## Definition of Ready for future Build

- [x] User/problem is clear.
- [x] Desired user-observable outcome is clear.
- [x] Main flow is clear.
- [x] Empty, ready, repair, preparation, check-in, active-wake and degraded states are defined.
- [x] Non-goals are explicit.
- [x] Content hierarchy and responsive layouts are defined.
- [x] Privacy boundary is explicit.
- [x] Alarm Kernel and Active Wake invariants are identified.
- [x] Success and validation evidence are defined.
- [x] Build is sliced so implementation does not need to invent product behavior.
- [x] Product owner explicitly authorized this shaped capability as a 1.0 feature-freeze exception on 2026-09-25.

## Definition of Done for the completed feature

The future implementation is complete only when all of the following are true:

- Compact, Medium and Expanded layouts are visually reviewed on real launchers.
- Next wake ordering matches the authoritative product model.
- Wake Ready is never inferred independently by the widget.
- alarm enable/disable, if shipped inline, commits through the existing alarm path and reports failures truthfully.
- time/timezone/package-replacement paths cannot leave durable misleading state.
- Tomorrow Contract/Prepared Wake Plan private text is absent from the widget.
- Active Wake exposes Open Wake only, never Stop/Snooze.
- pending Morning Check-In reuses the existing calibration path.
- RTL, TalkBack and large-text behavior are accepted.
- picker preview is production quality and privacy-safe.
- updates are event-driven/bounded and battery impact is negligible.
- the widget remains useful with network/cloud unavailable.
- relevant project documentation is synchronized after implementation evidence.

## Implementation status

PR #130 implements the shaped surface inside `:app` without creating new alarm authority.

Implemented:

- stable Jetpack Glance 1.2.0;
- one `GlanceAppWidget` with runtime `SizeMode.Exact` and Compact/Medium/Expanded content breakpoints;
- Android 15+ generated picker previews plus a static fallback preview;
- privacy-minimized `WakeWidgetSnapshotProjector`;
- event-driven refresh after alarm, preparation, appearance, history, reconciliation and Active Wake changes;
- exact in-app routing for alarm creation/editing, Tomorrow preparation, readiness repair and Insights;
- direct Morning Check-In actions through the same canonical calibration mutation used by notification follow-up;
- Active Wake re-entry without exposing Stop/Snooze;
- dedicated widget-safe sunrise and Wake Line vector assets;
- all three planning appearances;
- deterministic action-priority tests.

Deliberate implementation cuts:

- no inline alarm enable/disable control in 1.0. The expanded alarm summary opens the existing Alarms surface instead, keeping a large accessible target and preserving the normal capability/voice preflight and Alarm Kernel commit path;
- no live countdown and no periodic polling;
- no private Tomorrow Contract text, First Move, alarm labels or transcript/history content on the launcher.

### Validation evidence

Repository/local validation performed during Build:

- widget action-policy tests pass;
- Direct debug Kotlin source and unit-test source compile;
- Direct debug APK assembles successfully;
- Direct debug lint passes;
- the complete local Windows ARM64 Robolectric/Roborazzi suite cannot execute because its native runtime fails to load on this workstation; this same limitation affects many pre-existing tests and is not widget-specific;
- physical-device update installation was intentionally not forced when Android rejected the debug APK because the installed WakeMyWay package uses a different signing identity. Existing alarm data was not uninstalled or cleared.

Linux CI remains the authoritative environment for the full Robolectric suite. Physical widget rendering/resizing on representative launchers remains part of release hardening rather than something inferred from compilation.

## Scope exception

WakeMyWay remains under the 1.0 paid-launch feature freeze. This Build was not a silent scope expansion: the product owner explicitly requested implementation after the Shape contract was completed and reviewed.

The exception does not weaken the freeze's reliability rule. The widget remains a non-authoritative projection, adds no dependency to alarm delivery, and cannot Stop/Snooze or mutate critical state outside existing owners.

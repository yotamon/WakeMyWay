# Product design foundation

**Status:** implementation source of truth for the current product-design track  
**Date:** 2026-09-10  
**Platform:** native Android / Jetpack Compose  
**Relationship to roadmap:** parallel presentation track; does not renumber or bypass M7-M12 behavioral/reliability milestones

## Why this track exists

Wake My Way has a mature product thesis and trust-critical Android architecture, but its visible Android surfaces began as intentionally founder/lab UI. The design track turns the existing product model into a coherent production interface without moving behavioral authority into presentation code.

The UI must make the product feel finished while preserving the core invariant:

> Visual richness may degrade. Alarm delivery, critical local audio, Stop, Snooze and deterministic wake behavior must not.

## Current-state audit

Before this track:

- `MainActivity` was primarily a scrollable Wake Alarm Lab.
- `WakeActivity` was a static time/text surface with basic Snooze and Stop buttons.
- the theme defined a small dark Material color scheme but no complete type, shape, spacing, motion or component system.
- product flows were already documented in `04-ux-psychology.md` and `05-ux-flows.md`.
- brand direction was already documented in `06-brand.md`.
- the canonical visual thesis in `07-design-system.md` was strong but not yet represented as reusable Compose architecture.

The production design track was therefore established before more product screens accumulated ad-hoc styling.

# Design thesis: Adaptive Dawn

Wake My Way should feel like a **premium bedside object that wakes with the user**, not an alarm utility, dashboard, chatbot, wellness app or productivity SaaS.

The interface changes character with likely wakefulness:

```text
NIGHT
quiet · dark · soft · minimal
       ↓
EMERGING
presence · voice · almost no decisions
       ↓
ENGAGED
warmth · acknowledgement · one action
       ↓
ACTIVE
movement · stronger definition · physicality
       ↓
ORIENTED
light · context · clarity
       ↓
MORNING
calm · useful · finished
```

This transition is not merely a background-color animation. Information density, contrast, typography hierarchy, motion response, haptics and surface definition all increase deliberately as cognition returns.

## Product feeling

Target qualities:

- calm
- intelligent
- tactile
- warm
- restrained
- assured
- lightly editorial
- unmistakably WMW

Useful reference language is closer to a beautifully designed bedside object than a generic mobile app. Inspiration may be taken from industrial/editorial restraint, but no external product should be copied.

## Explicit visual exclusions

Do not use:

- purple/blue AI gradients
- glowing AI orbs
- robot avatars
- magical sparkles as an intelligence metaphor
- cartoon alarm clocks
- generic wellness sunrise illustration
- dense SaaS dashboards
- streak/confetti gamification
- glass effects merely because they are fashionable

# Circadian color system

Canonical foundation tokens:

| Token | Hex | Role |
|---|---:|---|
| Ink | `#101115` | deepest night / critical wake base |
| Deep Dawn | `#242129` | emerging surface |
| Clay | `#B9755A` | human warmth / restrained accent |
| Soft Ember | `#E4A17F` | active dawn / primary action |
| Morning Paper | `#F1E8DC` | oriented foreground / light canvas |
| Warm Light | `#F7F3EC` | high-legibility warm foreground |
| Sage | `#8EA18B` | readiness / adaptation / success |

Additional implementation tokens may derive from these values for outlines, disabled states and tonal surfaces, but the core identity must remain small and coherent.

## Circadian states

```text
EMERGING  Ink + Deep Dawn
ENGAGED   Deep Dawn + Clay undertone
ACTIVE    Deep Dawn + Soft Ember definition
ORIENTED  Morning Paper + warm dark ink
COMPLETE  settled warm morning surface
```

The wake surface must have a safe static fallback. Gradients and animation are presentation enrichment, never functional requirements.

# Typography

Typography has three roles:

1. **Clock / hero** — beautiful numerals, extremely legible, visually quiet.
2. **UI** — modern sans, optimized for reading at low cognition and dynamic type.
3. **Editorial moment** — restrained secondary voice for selected brand moments only.

Rules:

- the active wake runtime always prioritizes readability over brand typography.
- no critical instruction may rely on an ornamental face.
- Android font scaling is supported from the first production UI.
- English, German and future Hebrew/RTL must remain layout-safe.
- final bundled open-source font assets are a later controlled asset decision; remote font delivery is not allowed on the wake-critical path.

# Spacing and shape language

Use a small token scale instead of arbitrary paddings:

```text
4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 64 dp
```

Shape hierarchy:

- small controls: 14-16dp radius
- standard controls: 20-24dp radius
- primary cards: 28-32dp radius
- large hero surfaces: 36dp+ when compositionally justified

The visual identity may include custom rounded polygon geometry for WMW presence, but ordinary content surfaces should remain simple and legible.

# Signature object: WMW Presence

WMW needs a recognizable non-literal presence that can accompany audio without becoming an AI cliché.

It should be:

- abstract
- geometric
- warm rather than neon
- capable of subtle morphing
- recognizable at small sizes
- not a face, microphone or orb

Initial implementation uses AndroidX `graphics-shapes` to morph between restrained rounded polygon forms. The same object can subtly represent states such as quiet, listening, movement and completion while keeping state meaning available through text/semantics rather than shape alone.

# Motion language

Motion metaphor: **coming into form**.

```text
blurred → defined
low → raised
slow → responsive
fluid → stable
```

Initial timing bands:

| Context | Typical duration |
|---|---:|
| Emerging ambience | 1200-1800ms |
| Engaged transition | 600-900ms |
| Active feedback | 250-450ms |
| Completion settle | 450-700ms |

Rules:

- no energetic bouncing during Emerging.
- no decorative motion unrelated to product state.
- meaningful device movement may influence presentation only after the runtime/platform exposes a safe UI fact.
- animation cannot delay Stop, Snooze or critical alarm controls.
- reduced-motion preference receives a stable, immediate representation.

# Haptic language

Planned haptic vocabulary:

```text
wake begins        soft · soft
escalation         soft · pause · stronger
action accepted    one crisp pulse
completion         short · short
```

Patterns remain hypotheses until representative physical-device testing. Haptics are never the only state signal.

# Product surfaces

## Normal app

The normal application is centered around **Tonight**, not a dashboard.

```text
Tonight / Ready
├── Wake time
├── Tomorrow Contract
├── Character
├── Wake readiness / repair
├── History / learning
└── Settings
```

The founder Wake Alarm Lab remains available as an explicitly internal/developer surface. It must no longer define the product's visual home. Product presentation hides founder affordances by default; debug builds expose them through Android's debuggable application flag.

## Wake session

The dedicated `WakeActivity` remains independent from normal app navigation.

Presentation progression:

```text
Emerging → Engaged → Active → Oriented → Completion
```

This UX model must not be turned into a second behavioral state machine. Canonical runtime authority remains `WakeRuntime` with Alerting → Engaging → Activating → Orienting → Finished.

# Home / Tonight hierarchy

The first glance should answer three questions:

1. When am I waking?
2. Who/how is waking me?
3. Is tomorrow ready?

The wake time is the hero. Readiness is reassurance, not a diagnostic dump. Context is secondary. Dense navigation is avoided.

During founder development, real Alarm Health facts may be projected into the polished home while detailed diagnostics remain in the Lab.

# Wake screen hierarchy

## Emerging

Show only:

- time
- WMW/character presence
- one concise line when necessary
- accessible Stop/Snooze controls that do not dominate the first glance

## Engaged

Add:

- one instruction or acknowledgment
- listening/speaking state when the capability is actually active

## Active

Add:

- one physical transition cue
- motion acknowledgment derived from platform/runtime facts

## Oriented

Only after sufficient activation evidence:

- essential schedule/weather context
- Tomorrow Contract reason if available and unlocked
- First Move choice

## Completion

Settle rather than celebrate:

```text
Morning started
08:06
6m 14s to moving
Next · Shower
```

No confetti or streak pressure.

# Component inventory

The design system should expose semantic WMW components rather than scattered raw Material controls:

```text
WmwCircadianSurface
WmwTimeDisplay
WmwPresence
WmwStatusPill
WmwCard
WmwPrimaryAction
WmwSecondaryAction
WmwIntentionalStopAction
WmwTomorrowCard
WmwWakeReadiness
WmwChoice
WmwSectionHeader
```

A component is created only where repeated behavior/visual semantics justify it. Do not make a wrapper around every Material primitive.

# Dependency decisions

Dependency adoption follows `09-stack-and-dependencies.md`: add by current leverage, not by wishlist.

## Adopted now

### Navigation 3 `1.1.7`

Official stable AndroidX release as of 2026-08-26.

Why adopted:

- the product has real normal-app destinations.
- it gives Compose-first back-stack ownership.
- it keeps `WakeActivity` explicitly outside normal navigation.
- it creates a clean route from founder tooling to product Home, setup, history and settings.

Use only the core runtime/UI artifacts initially. Do not add adaptive navigation/ViewModel add-ons until a screen earns them.

### AndroidX graphics-shapes `1.1.0`

Official stable release.

Why adopted:

- provides substantial leverage for the signature WMW Presence.
- keeps shape/morph rendering native and local.
- does not become behavioral authority or alarm-critical execution.

### Roborazzi `1.74.0` + Robolectric `4.16.1`

Adopted in D2 after the canonical Adaptive Dawn surfaces were stable enough to deserve a pixel contract.

The gate is deliberately curated rather than using automatic Preview Scanner coverage. The initial reviewed baselines protect:

- Tonight Ready;
- Tonight Empty;
- Wake Setup Weekly;
- Wake Emerging.

Normal CI runs `verifyRoborazziDebug` with repository `contents: read` permission. Baselines are synthetic and manually reviewed before update. See [`d2-visual-regression.md`](d2-visual-regression.md) for the canonical update procedure and privacy contract.

Future large-font, narrow-screen, RTL and reduced-motion fixtures belong to deliberate D7 coverage rather than automatic baseline expansion.

## Planned, not blindly installed

### Vico

Add when M7 product integration produces real history/learning visualization requirements. Do not install before the screen exists.

### Coil 3

Add only if the product adopts real image/artwork loading. Current identity is deliberately typography/geometry/motion-led.

### Haze

Optional for non-critical decorative blur if native rendering cannot achieve the desired material quality. Never required by WakeActivity.

### Lottie / Rive

Do not adopt by default. Native Compose state-driven animation is the primary approach. Re-evaluate only when a specific authored animation requires capabilities that native Compose does not provide cleanly.

# Native capabilities instead of dependencies

Prefer Compose/Android platform APIs for:

- gradients and dawn illumination
- animation/transitions
- vector icons
- accessibility semantics
- haptics
- edge-to-edge/system bars

Avoid icon mega-libraries. Maintain a small WMW vector asset set when iconography becomes concrete.

# Accessibility contract

Required from the first production components:

- minimum 48dp targets; prefer 56-64dp for sleepy critical controls where layout allows.
- TalkBack semantics for actionable/stateful controls.
- no color-only state.
- critical voice actions have touch alternatives.
- dynamic type and large font layouts.
- high-contrast critical wake instructions.
- reduced-motion-safe rendering.
- Stop remains visible, local and actionable.
- Snooze remains intentional but not hidden or punitive.

# Privacy contract

- private Tomorrow Contract-derived text is rendered only under the existing credential/unlock rules.
- no screenshots/recents thumbnails while private wake text is visible; preserve `FLAG_SECURE` behavior.
- no design analytics may log private wake text.
- visual regression fixtures use synthetic content only.

# Implementation phases

## D0 — Design foundation

**Implemented in PR #30.**

- finalized initial tokens and visual architecture.
- established semantic Compose components.
- added canonical previews.
- established normal-app route architecture.
- preserved the Lab as internal tooling.

## D1 — Tonight product home

**Implemented in PR #30 and extended by PR #32.**

- polished real-data projection of next Wake Occurrence and Wake Ready.
- empty/not-ready/ready states.
- compact character presence.
- clear entry to setup/edit.

## D2 — Visual regression gate

**Implemented in PR #34.**

- Roborazzi/Robolectric test-only visual infrastructure.
- four deliberately reviewed synthetic goldens.
- read-only CI verification for selected surfaces.
- diagnostics artifact on verification runs.
- no automatic baseline recording in normal CI.

Canonical detail: [`d2-visual-regression.md`](d2-visual-regression.md).

## D3 — Setup experience

**Partially implemented in PR #32.**

Implemented:

- real wake-time editor;
- one-shot and recurring weekly schedule modes;
- independent weekday times;
- Tomorrow Contract product flow;
- local Wake Readiness projection.

Still gated on real durable ownership/evidence rather than placeholder controls:

- wake difficulty/intensity selection;
- character selection;
- consumer-facing permissions/readiness repair;
- optional Safety Backup only if product evidence still supports it.

Canonical detail: [`d3-product-setup.md`](d3-product-setup.md).

## D4 — Wake experience

- Emerging visual shell first, with existing local critical controls.
- map only real Wake Runtime/platform facts into later visual states.
- no parallel behavioral state machine.

## D5 — Learning/history UI

- emotionally legible summaries.
- calibrated distinction between Activation Completion and Confirmed Wake Success.
- add Vico only when chart requirements are concrete.

## D6 — Motion/haptics polish

- state transitions.
- motion-linked presentation where safe.
- haptic vocabulary tested on physical devices.

## D7 — Accessibility/responsive hardening

- TalkBack.
- font scaling.
- reduced motion.
- narrow/large screens.
- landscape/lock-screen edge cases where supported.

## D8 — Device polish

- system bars.
- lock-screen presentation.
- keyboard/input.
- rendering/performance profiling.

## D9 — Release visual QA

- screenshot regression.
- physical-device review.
- accessibility audit.
- final typography/icon/brand asset pass.

# Acceptance criteria for the foundation slice

The first implementation slice is accepted only when:

- `MainActivity` presents a product shell rather than defaulting to the diagnostics feed.
- Wake Alarm Lab remains reachable and behaviorally intact in developer/debug surfaces.
- `WakeActivity` is visually rebuilt without changing Alarm Kernel ownership of playback/Stop/Snooze.
- color, type, spacing, shapes and motion values are centralized.
- signature WMW Presence uses native local geometry.
- new product-facing text is resource-backed rather than hard-coded in Compose.
- navigation is Compose-first and `WakeActivity` is not a normal destination.
- canonical previews exist for major new components/screens.
- selected stable surfaces are protected by curated read-only visual-regression CI.
- no network/cloud/image dependency is introduced for rendering.
- CI compile/lint/tests remain green.

# Design review questions for every future screen

Before merging a screen, ask:

1. What is the single most important thing the user needs here?
2. Is the information density appropriate for their likely wakefulness?
3. Is this interaction helping Wake Success or merely increasing engagement?
4. Can the screen fail without compromising a scheduled wake attempt?
5. Is the visual behavior driven by real product state rather than decorative fiction?
6. Does it still work with large text, TalkBack and reduced motion?
7. Does it look recognizably like Wake My Way rather than generic Material or generic AI?

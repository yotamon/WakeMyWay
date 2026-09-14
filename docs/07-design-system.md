# Design system direction

## Design thesis

Wake My Way should feel like a **premium bedside object that wakes with the user**, not a dashboard, AI chatbot or productivity SaaS app.

The interface moves from darkness, ambiguity and low information density toward light, definition and action.

The approved visual direction is intentionally restrained. The next wake time is the composition rather than content inside a conventional dashboard card.

## Signature visual: Wake Line

The **Wake Line** is the primary reusable brand/interaction form.

It sits between a horizon, a waveform and a subtle W-M-W rhythm without becoming a literal audio visualizer.

It may represent:

- quiet readiness at night
- listening during engagement
- stronger movement during activation
- settling during orientation/completion

It should connect the app icon/mark, evening home, wake runtime and future marketing motion language without becoming decorative noise.

Rules:

- always thin, restrained and calm
- never an energetic equalizer
- amplitude reflects product state, not arbitrary animation
- it may glow subtly on dark surfaces
- on light surfaces it settles into a quiet clay line

## Dynamic wake palette

Initial brand tokens:

| Token | Hex | Role |
|---|---|---|
| Ink | `#101115` | deepest wake/night background |
| Deep Dawn | `#242129` | emerging state |
| Clay | `#B9755A` | warm human accent |
| Soft Ember | `#E4A17F` | active/dawn warmth |
| Morning Paper | `#F1E8DC` | light oriented state |
| Warm Light | `#F7F3EC` | daytime surface |
| Sage | `#8EA18B` | restrained success/adaptation accent |

These tokens must remain accessibility-tested against actual text usage.

## Circadian visual state

Do not think only in conventional dark mode vs light mode.

```text
ASLEEP                                    AWAKE

Ink -> Deep Dawn -> Clay/Ember -> Morning Paper
```

The interface visually gains form as the user wakes.

### Emerging

- near-black mineral surface
- only essential information
- large time
- Alfred/character identity as a quiet label
- Wake Line close to flat
- no contextual dashboard content

### Engaged

- slightly warmer dark surface
- listening state becomes explicit
- Wake Line gains measured amplitude
- still sparse and low-information

### Active

- stronger ember definition
- Wake Line becomes more physical
- instruction is the visual focus
- motion direction feels upward/forward

### Oriented

- transition to Morning Paper / Warm Light
- conventional hierarchy returns
- context becomes readable
- First Move can appear
- the UI should feel as though the user's eyes adjusted to the room

### Complete

- light, settled surface
- no celebration explosion
- concise outcome + next action
- Wake Line settles

## Screen composition contract

### Tonight / Home

Tonight is **not a dashboard**.

Primary hierarchy:

1. small Wake My Way identity
2. next wake time as dominant object
3. date
4. Wake Line
5. Wake Ready state
6. Tomorrow Contract editorial content
7. character presence
8. one primary edit action

System/readiness repair UI may become prominent when something is wrong, but healthy technical status should not occupy equal visual weight with the user's wake intention.

### Schedule

The schedule editor should preserve the same object-like quality:

- oversized editable wake time for one-shot/tomorrow mode
- quiet segmented choice between tomorrow-only and recurring
- recurring days shown as restrained rows rather than dense settings forms
- Android time-picking remains real platform interaction; do not fake a custom time control without implementing it fully

### Tomorrow Contract

The Tomorrow Contract should feel private, focused and editorial rather than like task-management metadata.

- one clear question
- one large reason/context field
- optional First Move field
- visible local/private reassurance
- no microphone affordance until a real voice-capture flow exists

### Live wake

The visible wake Activity follows the UX consciousness model without changing Wake Runtime ownership:

```text
Emerging -> Engaged -> Active -> Oriented
```

Existing deterministic runtime/voice modes map into these visual stages. Do not create runtime states solely for visual labels.

Stop and Snooze remain real, visible and touch-accessible in every critical state.

## Typography

Direction:

- editorial/display moments: restrained serif
- interface/body/labels: highly readable modern sans
- small labels use modest tracking and uppercase sparingly

Large time numerals may use the editorial family where legibility remains strong.

Requirements:

- Android dynamic font scaling
- excellent numeral rendering for time
- Hebrew/RTL future compatibility
- German and English support paths
- no production strings hard-coded in Compose

## Information hierarchy during wake

### First seconds

Only:

- time
- WMW/character presence
- current wake/listening state

### Later

Add:

- one action
- movement state
- relevant reason/context

### Oriented

Add:

- first relevant context
- weather only if useful
- First Move

## Motion language

Motion metaphor: **coming into form**.

```text
blurred -> defined
low -> raised
slow -> responsive
fluid -> stable
```

Rules:

- no energetic bouncing during Emerging
- avoid decorative motion unrelated to state
- physical device movement may subtly influence UI
- completion should settle, not explode
- respect reduced-motion preferences

## Haptic language

Initial concept:

### Wake begins

```text
soft - soft
```

### No response / escalation

```text
soft - pause - stronger
```

### Action acknowledged

```text
single crisp pulse
```

### Activation/completed

```text
short - short
```

Exact amplitudes/patterns require device testing.

## Sonic identity

Audio is a first-class design subsystem.

### Layer 1: Wake Motif

A short 1.5-2.5 second melodic identity. Recognizable, non-jarring, no sharp generic alarm beep as the only stimulus.

### Layer 2: Character

Voice begins after a short pause.

### Layer 3: Optional environmental bed

Very subtle, used only if it supports calm transition. It must not interfere with speech intelligibility.

## Character audio/visual variation

Keep brand DNA stable while allowing character-specific expression:

- Alfred: composed, restrained, sharper geometry
- Sam: softer and more organic
- Chaos: playful asymmetry while remaining bounded

Characters should not use generic robot/assistant avatars. Character identity should come from typography, motion, phrasing, sonic treatment and Wake Line behavior.

## App icon

Preferred concepts:

1. WMW monogram
2. W-shaped Wake Line
3. abstract wake curve / horizon
4. hybrid of waveform and rising motion

Avoid tiny detail. Icon must remain recognizable at small Android launcher sizes and in monochrome/themed icon contexts.

## Interaction design

### Snooze

Do not make Snooze the largest reflexive button.

Use intentional language such as:

> I need five more minutes

### Critical actions

Every voice-only action needs a visible/touch alternative for accessibility and environments where microphone use is unavailable.

## Accessibility

Required from first implementation:

- TalkBack semantics
- touch target compliance
- dynamic type/font scaling
- contrast validation
- reduced motion
- haptic alternatives
- no color-only state communication
- critical interactions usable without speech

## What the product must not look like

Avoid:

- purple/blue AI gradients
- glowing AI orb
- robot avatars
- magical sparkles as intelligence metaphor
- cartoon alarm clocks
- generic wellness sunrise stock art
- productivity gamification dashboards
- overly dense card stacks
- fake controls for unimplemented product behavior

## Implementation boundary

Visual stages do not own product behavior.

The Alarm Kernel remains responsible for reliable active wake execution. The Wake Runtime remains responsible for deterministic in-session behavioral decisions. Compose maps real product/runtime state into the visual language described here.

No visual redesign may make Activity lifetime authoritative for alarm audio, hide Stop, create network dependency, expose private Tomorrow Contract content while locked, or weaken Wake Ready truth.

## Brand board

The initial visual exploration is stored at:

`docs/brand/assets/wake-my-way-brand-board.png`

The current Android implementation and approved mockup direction are now the stronger production reference for composition, hierarchy and Wake Line behavior.

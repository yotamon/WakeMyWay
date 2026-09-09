# Design system direction

## Design thesis

Wake My Way should feel like a **premium bedside object that wakes with the user**, not a dashboard, AI chatbot or productivity SaaS app.

The interface moves from darkness, ambiguity and low information density toward light, definition and action.

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

These are initial brand tokens and must be accessibility-tested against actual text usage.

## Circadian visual state

Do not think only in conventional dark mode vs light mode.

```text
ASLEEP                                    AWAKE

Ink -> Deep Dawn -> Clay -> Ember -> Morning Paper
```

The interface visually gains form as the user wakes.

### Emerging

- near-black
- low contrast outside critical information
- large time
- minimal visual presence
- slow motion

### Engaged

- slightly warmer
- clear listening state
- still sparse

### Active

- stronger definition
- upward motion direction
- warm dawn accents

### Oriented

- brighter canvas
- conventional hierarchy returns
- context and choices become readable

## Typography

Direction:

- UI: highly readable modern sans
- brand/editorial moments: restrained modern serif

The alarm runtime always prioritizes readability over identity typography.

Requirements:

- Android dynamic font scaling
- excellent numeral rendering for time
- Hebrew/RTL future compatibility
- German and English support paths
- no hard-coded text in Compose

## Information hierarchy during wake

### First seconds

Only:

- time
- WMW/character presence
- listening/alarm state

### Later

Add:

- one action
- movement state
- relevant reason/context

### Oriented

Add:

- first event
- weather only if useful
- First Move choices

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

## Character audio variation

Keep brand sonic DNA stable while allowing character arrangements:

- Alfred: warm, composed, restrained
- Sam: soft and organic
- Chaos: playful articulation, still not harsh

## App icon

Preferred concepts:

1. WMW monogram
2. W-shaped waveform
3. abstract wake curve / horizon
4. hybrid of waveform and rising motion

Avoid tiny detail. Icon must remain recognizable at small Android launcher sizes and in monochrome/themed icon contexts.

## Interaction design

### Snooze

Do not make Snooze the largest reflexive button.

Use language such as:

> I need a little longer

Then a deliberate confirmation.

This adds intention without dark patterns.

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
- overly dense cards

## Brand board

The initial visual exploration is stored at:

`docs/brand/assets/wake-my-way-brand-board.png`

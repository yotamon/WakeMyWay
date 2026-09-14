# WakeMyWay design system

## Brand thesis

WakeMyWay should feel like a **calm, premium morning companion**, not a generic alarm utility, AI chatbot, wellness dashboard or productivity app.

The brand idea is simple: **a sunrise meeting a line that is simultaneously a landscape and a calm sound wave**. That form connects the product name, visual identity, voice-first wake experience and the transition from night to morning.

The user-approved production reference is the sunrise-wave brand direction implemented in Android. Earlier dark editorial/serif explorations are superseded where they conflict with this document.

## Canonical mark: Sunrise + Wake Line

The primary WakeMyWay symbol contains only two ideas:

1. a rising sun;
2. one continuous horizon shaped like both mountains and a restrained audio waveform.

The line is the ground beneath the sun. It must not become a separate equalizer floating inside the symbol.

### Geometry rules

- one continuous rounded line;
- long calm shoulders;
- a smaller rise, one clear central rise, then a smaller response;
- enough asymmetry to feel natural, but never noisy;
- the sun sits behind the line and may be partially occluded by it;
- no rays, alarm-clock bells, microphone, sparkles or extra iconography inside the core mark;
- preserve recognizability at launcher-icon size and in monochrome Android themed icons.

### Product behavior

The same line becomes the **Wake Line** in the product:

- quiet at rest;
- slightly more expressive while listening;
- strongest during activation/movement;
- settled after orientation/completion.

In live wake states the sunrise may appear behind the Wake Line. This lets the brand mark become a real interaction language rather than decorative logo repetition.

## Wordmark

Consumer-facing brand name: **WakeMyWay**.

Use the no-space form in the wordmark, launcher label and branded lockups. Domain/engineering terminology may continue to use “Wake My Way” where historically established.

Preferred lockup:

```text
[sunrise-wave symbol]  WakeMyWay
```

The symbol may also sit above the wordmark for hero/onboarding compositions.

Do not distort, stretch, outline, rotate or add effects to the wordmark.

## Brand line

Primary brand line:

> **Brighter mornings. Your way.**

Product promise should remain concrete and human. Avoid language that makes the app sound like an autonomous AI agent.

## Core palette

| Token | Hex | Role |
|---|---|---|
| Midnight | `#08142F` | primary night / live-wake background and core ink |
| Deep Navy | `#10264C` | elevated night surface |
| Sunrise | `#FF9F6D` | primary warm action and brand accent |
| Sunrise Soft | `#FFB88F` | softer warm state / glow |
| Golden Light | `#FFD699` | sun, listening and morning warmth |
| Dawn | `#A5B4FC` | restrained dawn atmosphere |
| Dawn Deep | `#7188E8` | light-surface secondary accent |
| Cloud | `#F8F7F4` | planning/background surface |
| Paper | `#FFFCF8` | cards and clean morning surfaces |

The palette is **sunrise atmospheric**, not an “AI gradient” system. Blue/lavender never becomes a neon intelligence metaphor and should not dominate calls to action.

All production text usage must remain contrast-tested.

## Light and dark product worlds

WakeMyWay intentionally has two complementary environments rather than a conventional theme toggle.

### Planning / evening setup

Use a clean Cloud/Paper surface with Midnight typography and Sunrise actions.

This includes:

- Tonight/home;
- wake scheduling;
- Tomorrow Contract;
- permissions and ordinary settings where practical.

Subtle dawn/sunrise atmospheric light may sit at the edges of the composition. It should never reduce readability.

### Active wake

Use Midnight/Deep Navy surfaces with progressively warmer sunrise definition.

```text
EMERGING -> ENGAGED -> ACTIVE -> ORIENTED -> COMPLETE
midnight      dawn       sunrise       paper morning
```

This is presentation only. It does not create product/runtime authority.

## Typography

Brand direction: **Plus Jakarta Sans** or a metrically appropriate bundled modern rounded sans.

Until a font asset is explicitly licensed and bundled into the APK, use the local Android sans family. Never introduce a downloadable font dependency into the wake path.

Hierarchy:

- display time: light, large, exceptionally legible numerals;
- headlines: semibold modern sans;
- body: regular modern sans;
- labels: semibold with restrained tracking;
- uppercase only for short eyebrows/status labels.

Requirements:

- Android dynamic font scaling;
- strong numeral rendering;
- Hebrew/RTL future compatibility;
- German and English support paths;
- no behavioral dependence on a remote font.

## Shape language

The app uses soft, premium geometry:

- cards: approximately 22–28dp radii;
- major containers: approximately 28–34dp radii;
- actions: pill-like where appropriate;
- touch targets remain at least Android accessibility minimums;
- avoid excessive nested cards.

Rounded does not mean childish. Keep surfaces restrained and spacious.

## Screen composition contract

### Tonight / Home

Tonight is a branded planning surface, not a dashboard.

Primary hierarchy:

1. WakeMyWay lockup;
2. human evening headline / context;
3. dark next-wake hero object;
4. sunrise-wave identity inside the wake object;
5. Wake Ready truth;
6. Tomorrow Contract;
7. character/voice readiness;
8. one clear warm primary action.

Healthy technical status should not compete visually with the user’s wake intention. Readiness repair becomes prominent only when something is wrong.

### Schedule

Use the light branded planning world:

- visible WakeMyWay identity;
- oversized, legible time;
- Sunrise selection/actions;
- white/paper object surfaces;
- restrained day controls;
- real Android time picker, never a fake visual-only control;
- local reliability copy remains present but secondary.

### Tomorrow Contract

Treat it as private, focused and warm:

- WakeMyWay lockup;
- clear privacy badge;
- one meaningful question;
- calm voice capture control;
- paper text fields;
- warm Save action;
- local/private reassurance.

Never expose private contract content while locked. Preserve `FLAG_SECURE` behavior.

### Live wake

Live wake is the hero expression of the brand.

The screen remains sparse, dark and low-distraction while the user is least awake. The sunrise appears behind the dynamic Wake Line as the session gains definition.

Stop and Snooze stay visible, local and touch-accessible in every critical state.

The screen maps real Wake Runtime state into presentation. It must never invent behavioral states solely for animation.

## Motion language

Motion metaphor: **morning coming into form**.

```text
dim -> defined
flat line -> responsive horizon
night -> warm dawn
fluid -> settled
```

Rules:

- no energetic bouncing during Emerging;
- Wake Line amplitude reflects product state;
- no decorative equalizer animation;
- no pulsing AI orb;
- completion settles rather than celebrates explosively;
- respect reduced-motion preferences.

## Sonic identity

Audio is a first-class brand subsystem.

Current signature wake sound options:

- **Morning Light** — bright, peaceful, hopeful;
- **Soft Start** — calm, warm, ease-in;
- **Morning Pulse** — modern, uplifting, more energetic.

These replace the generic-alarm feel as product options. A future short WakeMyWay sonic motif may unify them, but speech intelligibility and reliable local playback always take priority.

## Character identity

Keep the WakeMyWay brand stable while allowing character-specific language and behavior.

- Alfred: composed, dry, persistent;
- future characters may vary tone and phrasing;
- do not use generic robot avatars;
- character identity should come from language, motion, sonic treatment and Wake Line behavior.

## App icon

Canonical launcher concept: **Sunrise + Wake Line on Midnight**.

Requirements:

- no wordmark inside the launcher icon;
- simple enough for small Android launcher sizes;
- adaptive-icon safe zone respected;
- monochrome/themed icon supplied;
- no tiny detail or decorative rays.

## Accessibility

Required from implementation:

- TalkBack semantics;
- touch target compliance;
- dynamic type/font scaling;
- contrast validation;
- reduced motion path;
- haptic alternatives where haptics convey meaning;
- no color-only state communication;
- critical wake interactions usable without speech.

## What WakeMyWay must not look like

Avoid:

- generic purple/blue “AI” gradients;
- glowing AI orb as intelligence metaphor;
- robot avatars;
- magical sparkles;
- cartoon alarm clocks;
- stock sunrise photography or generic wellness clip-art;
- productivity gamification dashboards;
- dense card stacks;
- fake controls for unimplemented product behavior;
- repeatedly stamping the logo where the brand system already communicates identity.

## Implementation boundary

Branding is presentation, never wake authority.

The Alarm Kernel remains responsible for reliable active wake execution. Wake Runtime remains responsible for deterministic in-session behavioral decisions. Compose maps real state into the visual language.

No visual redesign may:

- make Activity lifetime authoritative for alarm audio;
- hide or delay Stop/Snooze;
- introduce a network dependency into wake delivery;
- expose private Tomorrow Contract content while locked;
- weaken Wake Ready truth;
- turn an animation or branded asset into a critical-path dependency.

## Canonical implementation assets

The production brand system currently lives in:

```text
apps/android/app/src/main/java/com/wakemyway/app/ui/theme/
apps/android/app/src/main/java/com/wakemyway/app/ui/components/WmwBrand.kt
apps/android/app/src/main/java/com/wakemyway/app/ui/components/WmwWakeLine.kt
apps/android/app/src/main/res/drawable/ic_wakemyway_*.xml
apps/android/app/src/main/res/mipmap-anydpi-v*/ic_launcher*.xml
```

The Android implementation and curated visual-regression states are the production truth for composition and behavior.

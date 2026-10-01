# Reason-first Calm Companion convergence

**Status:** accepted Shape contract and Build plan  
**Date:** 2026-10-02  
**Branch:** `feat/reason-first-calm-companion`

## Why this iteration exists

The Morning Instrument redesign established the right product direction: WakeMyWay is a calm, premium bedside instrument rather than a generic alarm dashboard. The new visual reference reinforces several useful interaction patterns, but WakeMyWay should adapt those patterns to its existing product model rather than copy the reference app or expand into reminders, timers, calendars, or a general assistant.

The most valuable idea is simple:

> **The wake should make its reason immediately understandable.**

WakeMyWay already has the underlying data to do this without inventing a new domain model:

- the next alarm has a short human label;
- the upcoming Wake Occurrence may have a private Tomorrow Contract;
- the prepared wake plan already carries the reminder and First Move into the oriented phase;
- Alfred already has a stable voice/personality contract;
- Wake Runtime already decides when richer context may appear.

This iteration is therefore visual/content convergence, not feature expansion.

## Product outcome

At a glance, a user should understand:

1. **when** they are waking;
2. **why** that wake exists;
3. **whether** WakeMyWay is ready;
4. **how** the morning will begin.

During the wake itself, the reason should return only when cognition is high enough for it to help, rather than adding more text to the first seconds of sleep inertia.

## Design principles

### 1. Reason before settings

The next wake is not merely a time. When a meaningful reason exists, it belongs in the primary composition directly under the time/date.

Reason precedence on Today:

1. current Tomorrow Contract text, normalized into a truthful preview capped at 180 characters;
2. alarm label;
3. no invented fallback reason.

This keeps the interface truthful and avoids fabricating intent.

### 2. One visual object, not an AI mascot

WakeMyWay keeps the **Wake Horizon / Wake Line** as the living visual signature.

Do not introduce:

- robot mascots;
- generic AI orbs;
- decorative microphone heroes;
- assistant avatars;
- sparkle-heavy “AI” visual language.

Alfred should feel present through language, timing, sound and the Wake Horizon, not through a cartoon persona.

### 3. Calm density

Planning surfaces should use:

- warm Cloud/Paper atmosphere;
- oversized time;
- strong Midnight type;
- restrained Sunrise/Dawn accents;
- large spacing;
- few containers;
- one dominant action.

Use a card only when an object truly needs separation or an error/repair state needs attention.

### 4. The reason returns at the right cognitive phase

Do not show private wake context in Emerging/early Engaged simply because it is available.

The reason becomes explicit in **Oriented**, consistent with the UX psychology contract:

- early wake = one action / one reply;
- oriented wake = short context + reason + First Move.

### 5. Existing privacy and authority boundaries stay intact

This design does not move authority into UI.

Unchanged:

- Alarm Kernel owns delivery, scheduling, Stop and Snooze;
- Wake Runtime owns progression and completion;
- private Tomorrow Contract content remains credential-protected;
- no private context enters Direct-Boot state;
- Realtime remains optional enrichment;
- network/AI failure cannot block the local alarm.

## Surface contracts

### Today / Home

Primary composition:

```text
Good evening

WakeMyWay
Brighter mornings. Your way.

NEXT WAKE

        07:30
Tuesday, 14 Jan

WHY YOU'RE WAKING
Design review at 10. You wanted time to shower and eat.

       [ Wake Horizon ]

Wake Ready
```

Below the hero:

- Wake-system repair only when needed;
- a compact **Morning plan** section for Tomorrow Contract / First Move editing;
- Alfred presence/readiness;
- one warm primary action: **Edit tomorrow**.

When no reason exists, do not fill the hero with generic copy. The plan section invites the user to add one.

### Alarm editor

The existing alarm `label` becomes more human in presentation:

- field label: **Why this wake?**
- placeholder: **Gym before work**
- helper: this short line appears in alarm lists and can become the reason fallback when no Tomorrow Contract exists.

No persistence migration is required. The underlying field remains `AlarmDefinition.label`.

The Tomorrow Contract controls remain advanced/personalization behavior, not part of the critical scheduling path.

### Oriented wake

When a prepared reminder exists:

```text
Good morning.
Thursday · 2 Oct

WHY YOU'RE UP
You have a design review at 10. You wanted time to shower and eat.

[ settled Wake Line ]

What's first?
Shower
```

When no reminder exists, retain the generic oriented copy. Do not expose empty “Why” chrome.

### Alfred presence

Alfred stays a restrained companion row, not a chat surface.

The row should communicate:

- stable identity;
- confidence/readiness;
- no implication that users can use WakeMyWay as a general voice assistant outside the wake flow.

## State matrix

| State | Reason behavior | Primary action |
| --- | --- | --- |
| No upcoming wake | no reason | Set wake time |
| Upcoming wake, no reason | omit reason from hero; prompt in Morning plan | Edit tomorrow |
| Alarm label only | show label as short reason | Edit tomorrow |
| Tomorrow Contract present | show Contract text in hero | Edit tomorrow / Morning plan |
| Wake not ready | keep reason visible; repair state becomes visually prominent | Repair prerequisite |
| Voice unavailable | reason still works locally; do not imply AI dependency | existing alarm behavior |
| Active wake: Emerging/Engaged | do not show reason | Stop/Snooze + wake flow |
| Active wake: Oriented | show prepared reminder if present | First Move |
| Active wake: Complete | focus on First Move / handoff | Finish |

## Accessibility

- preserve minimum 48dp interaction targets;
- dynamic font scaling must not overlap time/reason/controls;
- Today normalizes long Tomorrow Contract text into a truthful preview capped at 180 characters; the full contract remains available in Morning plan;
- reason text may wrap naturally rather than relying on hard line truncation;
- important readiness meaning cannot rely on color alone;
- reason copy remains real text, not text baked into graphics;
- RTL layout behavior must remain safe.

## Explicit non-goals

This work does **not** add:

- reminders;
- stopwatch/timer;
- calendar tabs;
- calendar scheduling;
- a general “talk to WakeMyWay” assistant mode;
- robot/AI mascot;
- new alarm schema fields;
- cloud persistence for alarm reasons;
- new permissions;
- wake-runtime policy changes;
- billing changes.

## Build plan

1. Extend Today presentation state with a computed `wakeReason`.
2. Prefer Tomorrow Contract text, fall back to the alarm label.
3. Integrate reason into the next-wake hero without adding a new card.
4. Refocus the Tomorrow Contract preview into a Morning plan editor so the hero does not repeat the same text.
5. Reword Alarm Editor label UX around human reason, preserving the existing `label` field.
6. Make the Oriented wake explicitly label prepared context as “Why you're up”.
7. Synchronize design/UX/status documentation.
8. Run Android CI and visual regression.
9. Review generated visual artifacts instead of blindly accepting hashes.
10. Update approved visual hashes only after the new canonical states are inspected.

## Acceptance criteria

- Today immediately reads as **when → why → readiness**.
- Today does not become a dashboard or productivity suite.
- No private reason is invented.
- Alarm label UX communicates purpose rather than a generic database field.
- Oriented wake explicitly reconnects morning-you with past-you’s reason.
- Early wake remains sparse and low-cognition.
- Wake Horizon remains the visual signature.
- Alarm Kernel / Wake Runtime / Stop / Snooze behavior is unchanged.
- no migration is required;
- no new permissions are required;
- relevant accessibility checks remain green, including compact large-text Oriented wake rendering;
- First Move content grows with font scale rather than clipping inside a fixed-height tile;
- Android compile/tests/lint remain green;
- visual-regression artifacts are reviewed before hash approval.

## Evidence after merge

Dogfood should answer:

- Does the Today screen communicate the next wake faster?
- Do users understand why the alarm exists without opening setup?
- Does the reason feel motivating/helpful rather than preachy?
- Does showing the reason in Oriented improve transition into First Move?
- Is the screen calmer despite carrying more meaning?

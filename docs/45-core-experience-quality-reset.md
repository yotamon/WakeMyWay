# WakeMyWay core-experience quality reset

**Status:** active 1.0 FINISH contract  
**Date:** 2026-10-04  
**Program:** #89  
**Primary gates:** TRUST #9, FINISH #59, PROVE #87

## Why this exists

WakeMyWay has enough capability. The current risk is that individual systems are becoming more sophisticated faster than the core experience is becoming trustworthy, effortless and distinctive.

Until this reset exits, the product is judged by one question:

> Can a new user set one wake, trust it overnight, be guided into meaningful activity, and finish the morning without confusion or founder assistance?

This is a quality reset, not a feature program. Existing capabilities do not earn launch priority merely because they are implemented.

## Golden path

```text
install
  ↓
understand the promise
  ↓
create the first wake
  ↓
see truthful Wake Ready state
  ↓
leave the app
  ↓
local alarm fires reliably
  ↓
optional Realtime joins when available
  ↓
wake interaction stays coherent and controllable
  ↓
meaningful activation
  ↓
orientation / First Move
  ↓
clean end
  ↓
lightweight later calibration
```

The first consumer value is a **committed wake**, not account setup, personalization depth, analytics, appearance, cloud state or a dashboard.

## Product rules for the reset

1. **No capability expansion.** Reliability, simplification, removal, polish, accessibility and evidence are allowed. New product scope is not.
2. **Alarm trust is non-negotiable.** Alarm Kernel remains authoritative and local. Network, AI, account, billing and analytics remain non-authoritative.
3. **Voice must earn its complexity.** Realtime is valuable only when it makes the wake better. A technically sophisticated but unreliable or inconsistent voice experience is a product defect.
4. **One cognitive demand at a time.** Especially during first run and the first seconds of a wake.
5. **Progressive disclosure.** A user should not configure tomorrow's tenth wake before they have experienced their first one.
6. **Remove before adding.** When a surface, fallback or abstraction adds more confusion than outcome value, prefer hiding, simplifying or deleting it.
7. **Physical evidence beats architectural confidence.** Alarm and voice behavior are not accepted because unit tests look convincing.
8. **No fake success.** Activation Completion and Confirmed Wake Success remain separate.

## P0: must be excellent before broader beta

### 1. First wake

A first-time user must reach first-alarm creation immediately after the minimum product explanation. Completing or skipping onboarding routes directly into a new Alarm Editor, with Home underneath in the back stack.

Acceptance:

- no empty Home detour before first alarm creation;
- Back from first-alarm creation lands safely on Home;
- Save commits through the normal AlarmProductController/Alarm Kernel path;
- Android readiness repair remains explicit and truthful;
- no account is required.

### 2. Alarm reliability

Close the physical evidence in #9. Retain failed traces instead of repeating until green.

At minimum the supported founder-device envelope must cover locked-screen wake, Doze, process/service recreation, reboot/Direct Boot, exact-alarm/presentation capability changes, Stop/Snooze durability, audio routes and motion behavior.

### 3. Realtime startup and continuity

The current accepted 1.0 contract remains in force during this slice:

- local alarm audio begins independently;
- Realtime has a bounded startup window;
- one unrecovered Realtime failure degrades the occurrence one-way to alarm-only;
- no local TTS/STT consumer substitution;
- no late mid-wake Realtime upgrade after degradation.

PR #190 fixes a concrete startup-timeout mismatch and is part of the new baseline.

Before changing the degradation contract, first collect physical evidence for:

- time from alarm fire to Realtime ready;
- broker vs WebRTC vs session failure category;
- first audible Alfred turn latency;
- interruption/re-entry continuity;
- Bluetooth/route-change behavior;
- network transition behavior;
- persona continuity across turns.

If evidence shows the accepted degradation contract itself produces a poor wake, return that decision to Shape. Do not quietly invent a second wake behavior inside transport code.

### 4. Wake conversation quality

The successful path must feel like one persistent person, not a sequence of generated responses.

Acceptance:

- no persona/accent switching;
- no repeated physical instruction after a usable reply unless Wake Runtime explicitly re-engages;
- no generic assistant praise or motivational filler;
- one action/question per early turn;
- user interruption is respected;
- no provider response can claim posture, wake success, Stop or Snooze state;
- conversation ends when Wake Runtime no longer needs it.

### 5. Active Wake coherence

The screen and audio must describe the same session truth.

Acceptance:

- connecting is visibly connecting;
- listening is only shown when input is actually enabled;
- degraded voice is stated once, calmly;
- Stop/Snooze remain immediately reachable;
- terminal-action failure never dismisses the wake;
- Oriented shows private reason/First Move only when appropriate;
- no dense dashboard content during sleep inertia.

## P1: simplify after P0 is stable

### First-run personalization

Wake preferences remain valuable, but they should not stand between install and first committed wake. Keep balanced defaults and make deeper personalization available from **You → Wake preferences** and later evidence-driven prompts.

The current multi-question onboarding is therefore considered simplification debt. It should be reduced only with updated visual/accessibility evidence, not by silently invalidating the curated regression baseline.

### Home

Home should prioritize:

```text
when → why → readiness → next useful action
```

Morning calibration, Tomorrow Contract, voice readiness and other secondary cards should appear only when relevant. Avoid turning Today into a status dashboard.

### Configuration

Alarm Editor should expose the minimum required wake contract first. Advanced expression/personalization controls remain secondary. Defaults should do useful work.

### Product surface

Account, sync, commerce, appearance, Insights and widget functionality may remain implemented, but they do not outrank golden-path work. Do not polish peripheral surfaces while P0 wake defects remain open.

## Keep / simplify / defer

| Area | Reset decision |
| --- | --- |
| Alarm Kernel / exact scheduling | Keep and prove physically |
| Wake Runtime | Keep deterministic and authoritative |
| Alfred | Keep one launch character; improve continuity |
| Realtime | Keep, harden, measure; no broad expansion |
| Branded alarm sounds | Keep |
| Stop / Snooze | Keep and protect transactionally |
| Tomorrow Contract / First Move | Keep, but reveal progressively |
| Multi-alarm | Keep |
| Wake preferences | Keep; reduce first-run burden |
| Wake Learning | Keep bounded/local; prove outcome before expanding |
| Insights | Keep secondary |
| Account / backup | Keep optional and secondary |
| Appearance | Keep secondary |
| Home widget | Keep; do not spend core-quality budget on expansion |
| New characters / integrations / new dashboards | Defer |

## Release stop conditions

Do not call WakeMyWay consumer-ready while any of these is true:

- a supported-device alarm can miss, resurrect, or become uncontrollable;
- the main voice path still has unexplained physical failures;
- a user can finish onboarding without being naturally led to create a wake;
- the wake can enter a confusing or contradictory UI/audio state;
- Stop/Snooze can appear successful before durable authority confirms them;
- the product requires founder explanation to understand Wake Ready or the first morning;
- physical accessibility acceptance remains open for the critical flow;
- target-user evidence in #87 has not shown that the wake outcome is useful beyond novelty.

## Execution order

```text
A. simplify first value
   ↓
B. close physical alarm + Realtime evidence
   ↓
C. fix only reproduced core defects / confusing states
   ↓
D. physically review complete golden path
   ↓
E. trusted alpha
   ↓
F. qualified beta / PROVE
   ↓
G. only then finish SELL
```

## Current first slice

This reset begins with the smallest high-confidence correction:

- onboarding completion/skip now opens a new Alarm Editor immediately;
- Home stays underneath so Back/Save returns to the normal shell;
- no persistence, scheduling, alarm-authority or visual-regression contract changes.

The next implementation slice should be selected from physical evidence, not from the size of the existing feature backlog.

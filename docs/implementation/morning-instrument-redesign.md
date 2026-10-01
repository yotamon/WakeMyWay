# Morning Instrument redesign

**Status:** implementation plan and source of truth  
**Date:** 2026-10-01  
**Branch:** `feat/morning-instrument-redesign`

## Product intent

WakeMyWay should feel like a premium bedside instrument that prepares, wakes and hands the user into the day. The interface is not a generic alarm utility, productivity dashboard, chatbot or wellness template.

The redesign preserves all alarm/runtime authority boundaries. Presentation can fail; alarm delivery, local playback, Stop, Snooze and deterministic Wake Runtime behavior cannot.

## Experience model

Primary consumer navigation becomes visually:

```text
Today | Alarms | You
```

Insights remains a real destination, reached contextually from Today rather than occupying equal primary-navigation weight.

### Today

The next wake is the composition, not a card inside the composition.

Priority:
1. wake time;
2. readiness;
3. companion/voice;
4. tomorrow intention and First Move;
5. contextual morning outcome.

### Alarms

Alarm management uses open rows and strong typography instead of a feed of rounded cards. Creating or editing an alarm remains direct and progressively discloses advanced behavior.

### You

Profile/settings is reframed as personalization:
- Your wake;
- Wake defaults;
- Your mornings;
- WakeMyWay/account settings.

The product should communicate "teach WakeMyWay how to wake me", not "administer preferences".

### Active wake

Active wake remains an independent immersive experience. Chrome is removed. Information density increases with Wake Runtime progression. The signature Wake Horizon visually moves from night toward morning while Stop/Snooze remain immediate and local.

## Signature element: Wake Horizon

The Wake Horizon is the recognisable product object used across Today and wake surfaces. It is:
- geometric and non-literal;
- not an AI orb;
- not a sunrise illustration;
- usable in light and dark environments;
- presentation-only;
- safe to render without network/image dependencies.

## Surface rules

- Prefer open canvas, whitespace and dividers.
- Use cards only for truly separate objects or attention states.
- Reduce decorative pills.
- Keep one strong action per composition.
- Use Sunrise only for meaningful action/attention.
- Use Dawn/Lavender as atmospheric depth, never an AI gradient.
- No confetti, streak pressure or ornamental intelligence metaphors.

## Implementation scope

This PR:
- introduces Wake Horizon and open-section primitives;
- simplifies visible navigation to Today / Alarms / You;
- rebuilds the next-wake hero around time, readiness and horizon;
- reduces card dependence in alarm management;
- reframes Profile as You/personalization;
- removes unnecessary active-wake chrome and brings the horizon into wake progression;
- tightens global shape language;
- preserves contextual Insights access and all existing runtime/account/data behavior.

## Acceptance criteria

- primary navigation presents exactly three consumer tabs;
- Insights remains reachable from Today;
- next wake is visually dominant without a containing card;
- alarm list reads as an instrument/list, not a dashboard card feed;
- profile copy and grouping are personalization-led;
- active wake contains no normal-app navigation or unnecessary brand header;
- Stop and Snooze behavior is unchanged;
- no alarm/runtime state authority moves into UI;
- accessibility target sizes remain >= 48dp;
- current CI, unit tests and visual regression checks remain green.

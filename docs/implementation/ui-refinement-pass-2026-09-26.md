# Consumer UI refinement pass

**Date:** 2026-09-26  
**Status:** implementation pass

## Goal

Make WakeMyWay feel like one calm, premium Android product rather than a collection of individually styled feature screens, without changing Alarm Kernel or Wake Runtime authority.

## Applied rules

- planning screens share one header rhythm and spacing scale;
- edge-to-edge screens explicitly respect status and navigation bars;
- bottom navigation stays discoverable while visually yielding to content;
- section labels use the shared design-system treatment;
- small supporting type remains readable at normal and large font scales;
- Alarm Editor uses progressive disclosure for advanced wake behavior;
- the Next Wake hero is directly actionable, not just decorative;
- secondary identity/context rows should not become another full card by default.

## Validation contract

Before merge: `git diff --check`, Android compilation/tests, responsive accessibility smoke renders, curated Roborazzi review for changed canonical surfaces, and manual device review when ADB is available.

This pass does not change alarm scheduling, playback, Stop/Snooze, preparation privacy, learning authority, or cloud dependencies.

# Wake feedback integrity — W0 first implementation slice

**Date:** 2026-10-23  
**Status:** Implemented on feature branch; signed device/overnight evidence outstanding  
**Parent program:** [Wake Experience Excellence PR #210](https://github.com/yotamon/WakeMyWay/pull/210), W0 (measurement) / W5 (learning safeguards)  
**Launch gates:** #9, #59, #87, #194  
**Change class:** Evidence integrity and bounded safety fix, not additional wake behavior.

## The precise problem

A completed WakeRuntime session proves only **Activation Completion**. It does not establish that a person remained out of bed. The existing Insights screen displayed runtime completions, while real-world check-ins had no adjacent aggregated summary. Meanwhile, the UI captured **Too intense / About right / Too gentle** feedback but `WakeLearningRepository.toOutcome` never forwarded that rating into `WakeLearning`, so the learner's existing high-friction guardrail could be blind to users reporting that the intervention was too much.

## What is implemented

1. **Morning-level reported outcome summary:** `WakeInsightsSummary` projects calibratable primary-to-Snooze-chain morning counts and answered **Got up / Back to bed / Up later** counts. Missing or skipped feedback remains unknown. Unresolved Snooze chains are not eligible for reported outcome denominators.
2. **Insights presentation:** The “Real morning follow-through” card appears separately from “Morning rhythm” and “Interactive pace.” It labels the source as **optional self-report**, counts answered vs eligible mornings, and does not present zero observed responses as evidence of zero actual successes.
3. **Persisted intensity input reaches the learner:** `WakeOutcomeSummary` now has an optional `interventionRating` distinct from full annoyance/agency feedback. `WakeLearningRepository` forwards the stored value. No fictitious `WakeAgency` measurement is manufactured.
4. **Bounded safety:** Repeated Too intense reports can block further increases to activation threshold/escalation and can justify decreasing intervention depth when repeated confirmed successes also felt excessively intense. The existing learner requires multiple relevant sessions and controls all bounded changes. Unrated mornings remain unknown. `submitInterventionRating` refreshes learned policy after persisting feedback so existing repeated patterns can take effect; one rating alone does not satisfy the rating-pattern threshold.
5. **No change to core wake ownership:** No alarm scheduling/critical playback/Stop/Snooze, Live/audio, microphone data collection, calendar, cloud, subscription or schema migration was altered.

## Tests

- `WakeInsightsProjectorTest`: answered outcomes with completed/stopped mornings, missing/skipped reports, unresolved Snooze chain and final reported result attributed to one morning.
- `WakeLearningRepositoryTest`: repeated Too intense reports block escalation and repeated too-intense-but-successful mornings lower intervention depth by one bounded step; persisted rating and immediate refresh are checked.
- Run targeted tests at the end of this coherent slice, then normal relevant Android CI on the eventual implementation PR. This document does **not** claim CI/device success.

## What to test on a real phone after an actual signed release

**Prepare once (no destructive reset):** install the signed update **over** the existing APK, retain existing alarms, check the same next wake is still scheduled and Android Wake Ready is truthful. If this evidence slice has not been merged/released yet, the existing `v0.2.32` APK will **not** show the new report.

**For each of 5–7 normal mornings (do not deliberately risk missing an important appointment):**

1. The evening before: note intended alarm time and keep an ordinary backup while trust is still being established.
2. On wake: note whether native local sound was actually **audible**, whether the voice was **audible and natural**, whether the screen/controls worked, and whether you snoozed or pressed Stop. A connected Live session or transcript alone is not proof of hearing it.
3. Later when fully awake: open **Insights → Morning Check-In**, choose *I'm up*, *Back to bed*, *Up later* or *Skip* truthfully. Early Stop may offer an optional ~15-minute follow-up notification; it is not guaranteed to arrive precisely and is not an additional alarm.
4. If invited to rate wake feel, select *Too gentle / About right / Too intense*. Several Too intense reports should prevent the learner from automatically strengthening intervention; a single rating should not abruptly reprogram your next alarm.
5. Review **Real morning follow-through**. The reported counts must match only answered, eligible morning chains and never silently mark skipped/missing reports as success. “Activation completed” must stay separate.
6. If a morning fails, capture the version, scheduled vs actual time, phone lock/network/Bluetooth state, symptom and privacy-safe **Wake Lab reliability report** if available. Do not share transcripts/calendar text/voice audio unless you explicitly choose to.

**Stop/go:** Any missed critical local alarm, unresponsive Stop/Snooze, duplicate/resurrected execution, unexpected increase in wake intensity after repeated Too intense feedback, or private-content leak is a release blocker. Record evidence before changing more policy; do not claim product effectiveness from one good morning.

## Deferred next slices

- W1 physical reliability matrix (signed real phone) and W2 real voice audibility/human quality remain primary gates.
- W3 reflex Stop policy requires separate product Shape and user evidence. This slice does not make Stop harder or rearm it.
- True causal improvement versus ordinary alarms, richer learning/quality telemetry, alternate feedback cadence, and paid launch remain outside this PR.

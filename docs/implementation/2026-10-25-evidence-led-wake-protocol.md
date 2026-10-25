# WakeMyWay — Evidence-Led Wake Protocol: Implementation Blueprint

**Date:** 2026-10-25  
**Status:** SHAPE / executable plan, not implemented  
**Parent program:** [Wake Experience Excellence](2026-10-23-wake-experience-excellence-plan.md) (merged PR #210)  
**Launch gates:** TRUST #9 → FINISH #59/#194/#99/#97 → PROVE #87 → SELL #88  
**Change scope:** An implementation-ready behavior/evidence addendum, **not** a replacement for the canonical 1.0 plan.

## 1. Decision and north star

WakeMyWay is a **transition-from-sleep-to-action** product, not a chat product. The primary outcome is sustained, user-intended waking with **minimum effective friction**, while recognizing that sleep loss, circadian timing, and phone-only observation limit what an app can achieve.

Four product layers:
1. **Awareness:** alarm stimulus delivered; user may have noticed it.
2. **Engagement:** coherent speech/touch/pickup, but not proof of getting up.
3. **Activation:** sufficient bounded device-observed movement/interaction for the Wake Runtime to enter Orienting.
4. **Sustained real-world wake:** later user-confirmed getting up rather than returning to bed; missing feedback is **unknown**.

Do not conflate the layers. **Activation Completion is never renamed Confirmed Wake Success.** No system may claim physiological wakefulness from phone motion, speech, a button press, microphone confidence or AI text.

This blueprint specializes, rather than duplicates, W0–W8: it defines the new **intervention protocol, audio hypotheses, safe reflex-dismissal decision tree, experiment design, and exact build sequencing**. Every follow-up PR references the corresponding W-stream, preserves existing product/domain terminology, and updates canonical docs only when behavior genuinely changes.

## 1A. Quality-first voice decision (2026-10-25)

**Decision:** GPT-Live remains the main premium conversational wake mode. Do **not** change its default to pre-rendered TTS, silently substitute a different speaker, create a cheaper conversation mode, or reduce live interaction to prerecorded compliance prompts in the name of cost reduction.

**Why:** The current product problem is inadequate *experienced wake quality* and incomplete real-device proof. Price optimization against an unproven experience baseline risks optimizing the wrong product. A premium, reactive and stable companion is a core hypothesis worth proving before simplifying.

**Engineering consequences**
- Focus W2/B2 first on **actual speaker-audible turns, stable character/timbre, responsive interruption, gentle pacing and meaningful variation**, not on swapping the speech provider.
- Keep silence intentional and first-party native alarm sound independent. Close / release the paid Live session once the runtime no longer needs conversation. These optimizations may also lower cost but must be justified by better UX/reliability, not by a lower invoice alone.
- Treat the existing bounded dialogue/re-engagement and local Wake Runtime as authority. The OpenAI model may make natural utterances for approved intents; it does not schedule, escalate, self-certify waking or take Stop/Snooze authority.
- If premium Live fails, show a truthful **native alarm-only** experience. Do not replace the selected character with a mismatched lower-quality synthetic speaker mid-session.
- Do not claim a full-duplex model is always superior either. After we have **measured** a successful Live baseline, compare a possible guided/TTS alternative in a **non-shipping, opt-in evaluation**. Require non-inferiority for repeated-morning Confirmed Wake Success, perceived naturalness, character consistency, annoyance, perceived agency, start latency and accessibility, not just lower cost.
- Any cost-only simplification is **deferred**, not part of B2/B3 acceptance or 1.0 default policy.

**Immediate release decision:** The merged #211–#213 fixes need a **new signed Direct APK** before their effect can be judged on an actual OnePlus. Check exact release workflow and physical safety gates; do not equate merging code with a completed release or claim sound was audible from telemetry alone. Preserve existing alarms in a signed in-place upgrade and keep the separate conventional backup during verification.

## 2. Source audit: reuse instead of rebuild

Existing in main (implemented does not mean physically validated):
- Pure-Kotlin Wake Runtime and typed phases, input/directive authority in `apps/android/wake-core/src/main/kotlin/com/wakemyway/core/runtime/`.
- Motion signal extractor and native observer in `wake-core/.../motion/` and `app/.../motion/AndroidMotionObserver.kt`.
- Native Alarm Kernel, recoverable scheduling, critical playback and durable Stop/Snooze in `app/.../alarm/`.
- Direct GPT-Live-first enrichment with session controller and voice/audio policy in `app/.../voice/` and `app/src/direct/.../voice/`.
- Tomorrow Contract, optional read-only selected Android-synced Google Calendar, First Move.
- Local history, Wake Learning v0, bounded calibration/rating wiring (PR #211), expected-wake delivery evidence (#212), and calm uncertain-speech behavior (#213).
- Optional non-critical 15-minute post-Stop Morning Safety Check and Wake Lab export.
- Sunroom Club design system, existing UI states and visual regressions.

**Currently unproven:** signed overnight physical alarm reliability on supported device(s), speaker-audible Live continuity, repeat-morning wake effectiveness, reflex-Stop mitigation benefit, and learning uplift. No new generic VoiceProvider, ML model, sleep-stage tracker, database service or analytics platform is justified yet.

## 3. The morning behavior contract

The following are **design starting values for tests, not scientific optimum timing**. Version under Wake Policy and freeze for each occurrence; do not hardcode separate Android UI timers or let GPT-Live improvise timing.

| Time / stage (hypothesis) | Desired behavior | Evidence / progression | Forbidden behavior |
|---|---|---|---|
| 0–20s: Emerging | Reliable native Wake Motif first, gentle ramp within safe audible bounds, optional **one** short greeting after local sound is established | Trigger and audio evidence; no assumption user is awake | AI startup blocking native sound; immediate long monologue; inaccessible Stop |
| ~20–60s: Engaging | One low-pressure invitation to sit up; listen or pause deliberately | Speech/touch count as *engagement*, not posture proof | Demanding verbal confirmation; repeated identical instructions |
| ~1–3min: Activating | At most one subsequent simple physical invitation (feet down, safe posture shift or optional seated alternative); continue observing movement | Pickup/orientation/sustained movement are bounded, imperfect proxies | Repeated task checklist; forcing standing; fitness exercises or rapid/forced breathing |
| After activation evidence: Orienting | One brief user-approved purpose, calendar timing if enabled, and First Move; acknowledge without fake praise | Deterministic runtime authority; calendar privacy checks | Reading a long itinerary, exposing private data on lock screen, voice claiming success |
| End / later check | End Live + release microphone/audio focus promptly on terminal kernel action; optional low-friction follow-up after stopping | Later *reported* Got Up / Returned to Bed / Up Later, never a self-certified success | Silent alarm rearming, mandatory surveys, treating nonresponse as success |

**Adapt timing from measured outcomes, never from an unsupported claim that these intervals are optimal.** Natural silence is a useful action. One short cue followed by a pause often beats more speech.

### Exact runtime reactions

| Observation | Required behavior |
|---|---|
| Silence / no transcript | Audio safety continues. A bounded timer may authorize **one** gentle re-engagement. Never infer refusal or force repeated speech. |
| Unclear mutter / suspected echo / TV | No activation credit, no immediate escalation and no immediate repeat cue (preserve #213). |
| Clear reply such as “yeah, I'm up” | Bounded engagement credit; not proof of sitting/standing; at most one subsequent physical invitation, then HoldEngagement. |
| Device pickup / movement | Accept bounded sensor evidence, deduplicate/cap it, progress when policy permits; do not assert person left bed. |
| Actual sustained movement | Stronger evidence than a brief touch or voice acknowledgment, but still not medical proof. |
| User interrupts AI | Stop remote playback / correctly hand back the turn; no double-speaking or character replacement. |
| Voice startup slow, denied, silent or network fails | Native sound, Stop/Snooze and runtime remain usable; degrade calmly once to native alarm-only mode. |
| Stop at any point | Kernel-confirmed immediate accessible terminal action; cancel voice, mic and follow-up callbacks; no involuntary rearm. |
| Snooze at any point | Kernel schedules exact replacement durably **before** retiring current occurrence. |
| User declines interaction / no mobility | Keep accessible native alarm controls; do not punish, shame, or demand standing. |

Keep the **one-authority** rule: `WakeRuntime.reduce()` decides action; AI receives only allowlisted `SpeechIntent`; the UI mirrors runtime and actual audio state. Do not introduce a parallel behavioral state machine in the voice controller.

## 4. Delivery plan in focused Build slices

Dependency graph: **B0 → B1**, **B0 → B2/B3** (parallel inspection allowed); **B1+B2 → B4**, **B0+real feedback → B5**, **B1–B5 → B6**, **B6 → B7**. Critical reliability trumps new treatment variants.

### B0 — Measurement truth and baseline (W0, #87)

**Goal:** Every scheduled morning has an accountable eligible Wake Occurrence and its exact evidence quality.

Tasks:
- [ ] Audit trace lineage: intended occurrence → delivery/receiver → native audio → optional Live → physical evidence → Stop/Snooze chain → later calibration. Use existing history/diagnostic journals, not new telemetry services.
- [ ] Audit #211 / #212 against release build and negative cases. Include missing receiver, duplicates, aborted sessions, user cancellation, delayed follow-up and unanswerable outcome.
- [ ] Report Activation Completion and Confirmed Wake Success separately, with numerator, denominator, coverage and **unknowns**; Snooze replacement chain counts as one intended morning when appropriate.
- [ ] Add only missing privacy-safe observation fields: policy snapshot identifier, assigned intervention variant, intervention count, timing to movement, early Stop, voice mode/fallback, calibration provenance, opt-in partner disturbance feedback. No raw audio/transcripts/calendar content/raw accelerometer streams.
- [ ] Ensure Wake Lab tests do not pollute real-morning cohorts; never use connected PCM as evidence of audible speech.
- [ ] Pilot baseline: multiple ordinary-alarm mornings where safe, then WMW mornings; retain version, day type, self-reported available sleep and backup-alarm presence. Avoid sacrificing essential alarms.

**Exit:** A reproducible per-morning and aggregated founder report including failures, unknown outcomes and correct eligible denominators. No unverified improvement claim.

### B1 — Physical trust & failure handling (W1, #9)

**Goal:** The signed installed app wakes on the real phone, including adverse Android states, with correct terminal controls.

Tasks:
- [ ] Use existing physical-acceptance runbook on a **signed in-place APK update** (preserve schedules/history); collect ordinary locked-screen and Doze overnight occurrences on the OnePlus.
- [ ] Cover reboot/Direct Boot, process/service/Activity death, multi-alarm overlap, timezone/DST, revoked permissions, Bluetooth/route/call, offline/network drop, provider timeout, DND/audio volume, Stop/Snooze failure.
- [ ] Record target → native **speaker-audible** alarm, UI controls, terminal acknowledgment and first **speaker-audible** Live turn separately; attach passing **and failing** traces.
- [ ] Fix observed regressions as narrow PRs; add immediate targeted invariant tests. Keep conventional safety backup during dogfood.
- [ ] Document limits imposed by Force Stop, powered-off phone, permission/OEM behavior; do not claim universal reliability.

**Exit:** No unresolved critical missed/duplicated/unstoppable/resurrected wake in the **explicitly exercised matrix**, with device/build/version evidence. This does not statistically prove zero failure rate.

### B2 — Calm conversational intervention protocol (W2, #194)

**Goal:** One recognizable character delivers minimal, useful speech; the person is not forced into conversation.

Tasks:
- [ ] Record existing AlarmFired → InitialWake → SpeechFinished → voice-input / no-voice state transitions and audio policies before editing.
- [ ] Shape the actual treatment copy and timing for Emerging/Engaging/Activating/Orienting, including silence, “no,” sleepy mutters, speech overlap, humor, interruption and polite disengagement.
- [ ] Keep deterministic runtime speech authorization and one-bounded-reengagement policy. Move any truly behavioral intervention timing into Wake Policy, not into prompts, UI callbacks or opaque model logic.
- [ ] Test 3–5 natural utterance variants **per supported character/language only after physical behavior is stable**. Avoid robotic praise, obedience demands, calendar dumps, repeated sitting instructions and excessive talking.
- [ ] Validate first heard speech, start budget, speaking/listening truth, audio focus, cleanup, and continuity across speaker/Bluetooth, network loss, screen lock and interrupted turns.
- [ ] Include a shorter, calmer voice-off/native-sound path; do not fake premium voice with an unapproved fallback voice or switch personas silently.

**Exit:** Corpus of deterministic event replays and actual device traces; no unauthorized behavioral transitions, double replies or repeated instruction loops; naturalness/annoyance ratings from real mornings with sample sizes. If voice worsens outcomes, reduce its prominence.

### B3 — Acoustic treatment and controlled novelty (W2 + W4)

**Goal:** Test how we wake people pleasantly without making native sound unreliable.

Tasks:
- [ ] Audit existing Wake Motif, native loop/fade and player/audio route; preserve guaranteed audibility and emergency tone behavior.
- [ ] Experiment, **only after B1 sound safety**, with (A) current motif + Live as shipped, (B) motif → short pause → the **same Live voice**, and (C) native sound-only as a safety/control baseline, **not as the new premium default**. Consider a gentle volume envelope bounded by user's native alarm volume and supported device behavior.
- [ ] Keep recognizable sound identity; allow small phrasing variation but do not change voice identity or introduce novelty-for-novelty's-sake.
- [ ] Evaluate quiet/shared-room considerations: no unconsented smart light, excessive spoken volume or private spoken agenda; document audible alarm tradeoff explicitly.
- [ ] Never make the fade so gentle that a user misses their alarm; native safety escalation is deterministic and bounded.

**Exit:** Measured audibility, wake success, annoyance, partner disturbance (where reported) and audio defects per variant; preserve premium Live as the default unless field evidence supports a consciously accepted product change.

### B4 — Early Stop / Snooze / follow-through (W3, requires Shape)

**Goal:** Reduce half-asleep reflex dismissal **without trapping users**.

Tasks:
- [ ] Quantify Stop-before-activation and subsequently reported return-to-bed; short response times alone are **not** evidence of accidental Stop.
- [ ] Shape 3 candidates: clear Stop/Snooze visual distinction; brief optional intentionality aid; opt-in protection only for people with confirmed reflex-dismissal history. Compare no-change as the baseline.
- [ ] Retain an immediate, accessible, non-network, non-voice dependent Stop path in **every** variant. Motor/TalkBack, locked screen, one-handed and 200% font-size reviews are mandatory.
- [ ] Preserve intentional Snooze exact-replacement semantics, do not stigmatize it; compare observed success after 0/1/multiple snoozes without inferring causality.
- [ ] Reuse the existing **15-minute noncritical follow-up**; assess timing, notification fatigue, false positives and value. Post-Stop check never becomes a secret re-awakening.
- [ ] A newly scheduled later wake, hold-to-dismiss, or stronger prevention is a **separate explicit opt-in, feature-freeze exception and kernel review**; not authorized by this plan.

**Exit:** Documented Shape decision keep / change / reject, evidence on return-to-bed and annoyance, and accessible terminal controls. Revert a treatment that traps or frustrates people.

### B5 — Explainable personal strategy (W5, after B0 + calibrated evidence)

**Goal:** Learn which minimal intervention timing helps a particular person, without overfitting or growing AI costs.

Tasks:
- [ ] Reuse Wake Learning v0; audit learner's persisted serialization and versioned policy rehydration on signed upgrade. Do not split behavioral authority from Wake Runtime.
- [ ] Treat **Gentle / Balanced / Persistent** as *policy archetypes for evaluation*, not mandatory onboarding modes or arbitrary new escalation quotas. Start users Balanced with safe defaults; offer preferences later only if needed.
- [ ] Candidate tunables: first voice join delay, initial sit-up cue timing, interval until a single movement invitation, native audio escalation envelope, Snooze duration/preferences, and one purpose cue at Orienting. Each new field requires schema migration, bounded range and deterministic replay.
- [ ] Change a policy only after repeated, sufficiently complete real-morning data; include neutral/negative feedback and safety constraints. Unknown calibration ≠ success.
- [ ] Keep each occurrence's immutable Wake Policy snapshot. Apply changes **only to future occurrences**. Explain the changed parameter and reason in plain language; allow revert/reset.
- [ ] Exclude Lab runs, high-uncertainty cases and incompatible day contexts. Track policy version, evidence count, previous result and guardrails. Never increase nagging/prompts or remove Stop.
- [ ] Compare the learned policy to stable default with an appropriate per-person holdout or alternating condition only after enough waking opportunities. If worse, automatically fall back to prior validated policy.

**Exit:** Reproducible replay, schema/upgrade survival, hysteresis/no oscillation and real-world improvement evidence; otherwise keep default rules and describe learning as experimental.

### B6 — UI, accessibility and context cohesion (W4/W6, #59/#99/#97)

**Goal:** The person can configure one alarm quickly and understand the wake without cognitive overload.

Tasks:
- [ ] Inventory current consumer states/screens and use the existing Sunroom Club target; avoid a redesign invented without the live content map.
- [ ] First alarm: time/schedule/Save/Wake Ready, no forced account, survey or character setup. Advanced preferences later.
- [ ] Active wake: only one instruction / clear audio and listening truth / obvious Stop/Snooze. No tiny controls or forced reading in a sleepy state.
- [ ] Orientation: show only consented personal reason/calendar **time** and First Move when useful; private event title disclosure remains separately opt-in and lockscreen-safe.
- [ ] Profile/Insights: separately explain observed activity and later self-report; no fabricated precision or guilt streak.
- [ ] Physical TalkBack, 200% type, one-hand, reduced motion, Hebrew RTL, German and English copy/voice review. Standing is never a requirement; support a seated safe equivalent.
- [ ] Measure cold start and realistic installed APK/upgrade path; keep Android platform and paid Live distribution boundaries honest.

**Exit:** Founder and target-user task-based review of first alarm → actual wake → understandable end and outcome check, plus accessibility physical sign-off.

### B7 — Field experiment, decision and release (W7/W8, #87/#88)

**Goal:** Demonstrate repeat-morning value, not a flashy single demo.

Protocol:
- [ ] Stage 0: 5–7 signed real-device founder mornings with conventional backup; classify critical incidents before behavioral comparisons.
- [ ] Stage 1: recruit **10–20** habitual snoozers / reflex dismissers where possible; 14–21 days/person and ≥5 intended wakes/person as a minimal descriptive sample (not a power-calculated efficacy trial).
- [ ] Prefer within-person counterbalanced conditions over naive before/after when safe. Preassign variants before the night, stratify weekdays/weekends and note sleep duration, obligations and concurrent backup alarms. Do not deliberately withhold essential alarms.
- [ ] Sequence isolated comparisons (not simultaneous factorial): baseline ordinary alarm vs WMW; sound-only vs voice; physical cue earlier vs later; later personalized vs frozen default.
- [ ] Record all intended opportunities even when there is no trace: late/silent, early Stop, Snooze, movement, voice fallback, reported up/back to bed/up later, missing feedback, annoyance, agency, partner impact, device/build and session cost.
- [ ] Predefine analysis before variant assignment: report absolute success proportions, n/N, calibration coverage, unknowns, return-to-bed among calibrated activations, timing distribution, annoyance/agency distribution; no causal claim from tiny observational cohorts.
- [ ] Mandatory stop criteria: critical late/missed/silent alarm, unusable Stop/Snooze, privacy leak, coercive interaction or repeated harmful annoyance; remediate and restart affected evidence window.
- [ ] Release decision: GO only after supported-device critical reliability and real-user wake success/pleasantness evidence, plus existing launch/legal/commerce gates; otherwise FIX, SIMPLIFY or ROLLBACK. If ordinary alarms win, prioritize simplification over new AI features.

**Exit:** Cohort report with variant assignment, denominators, confidence/limitations, defects, cost, and a signed Go/No-Go note.

## 5. Cross-cutting engineering rules

- **Authority:** Kernel schedules/rings/stops/snoozes; Wake Runtime decides behavior; Live expresses only authorized intents; UI does not infer state; Wake Learning changes only future policy.
- **Safety:** Stop always immediate and accessible; Snooze durable before current alarm ends; no autonomous rearm; no forced physical exertion, breathing routines, standing, light exposure, or opaque compliance quiz.
- **Reliability:** Alarm works with no Internet/auth/AI/permission for microphone. No AI or data logging on the critical thread. Native audible backup remains primary.
- **Privacy:** Prefer local aggregates. Never persist mic audio, raw transcripts, private calendar text or high-resolution motion traces. Any optional partner feedback is user supplied.
- **Failure modes:** Unknown stays unknown; feedback skipped stays skipped; no late AI restart after native fallback; test/preview occurrences excluded from learning.
- **Versions:** Preserve alarms, sessions, preferences, learning data and critical snapshots on signed upgrade. Migration failures revert noncritical learned policy, not alarm scheduling.
- **Costs:** Remote sessions bounded; monitor actual provider billing/admissions; no invisible retries or long calls just to create engagement.
- **Research honesty:** Lab evidence about movement, melodic sound and Snooze motivates tests, not claims that a specific wake behavior is proven clinically effective.
- **Accessibility:** A motor-limited or voice-disabled user must still safely manage the alarm; no single mandatory motion gesture or spoken phrase.

## 6. Experiment backlog: evidence, hypothesis, decision gate

| Hypothesis | Minimum test | What would change the plan |
|---|---|---|
| Physiological-first prompts outperform conversation-only | Early short movement cue vs later cue, measuring real returns to bed and annoyance | If no success gain or more annoyance, reduce physical prompting |
| Sound-before-voice reduces cognitive overload | Native motif → pause → one invitation vs immediate voice | If late voice is less effective, keep current sequence |
| Gentle volume growth feels better | Bounded ramp vs existing envelope, speaker-audible success retained | Reject if more silent/missed alarms |
| Limited Snooze helps some late chronotypes | Within-person patterns, no moralized aggregate | Do not universally disable Snooze |
| Purpose cue supports follow-through | With vs without user-approved one-line reason *after activation* | If no gain, leave as preference |
| Reflex Stop can be addressed without coercion | Baseline UI distinction vs shaped optional aid | Revert if perceived control/accessibility worsens |
| Personal learning adds value | Frozen policy vs bounded future-policy update | If no credible improvement, freeze/default the learning |
| Shared-room context matters | Voluntary disturbance score vs audibility by variant | Offer quiet settings only if safety/reliability remains intact |

## 7. Deferred extensions: explicitly planned, not preapproved 1.0 features

These cover the longer-term ideas from the research without violating the existing **1.0 feature freeze**.

**F1 — Smart dawn / lighting:** After B7 shows evidence of sufficient value and consent, Shape optional light integration. Test whether a light before wake helps, paired with and **never replacing** critical audible alarm. Check shared-room permission, light intensity, offline behavior, vendor/API cost, and opt-out. Do not build hardware integration before measured demand.

**F2 — Wearable / more reliable activation signals:** Evaluate only if confirmed false-positive activation remains a blocker; compare incremental predictive value to phone-only evidence. Separate missing-sensor state, accessibility and privacy constraints; don't add sleep-stage certainty claims.

**F3 — Alternative realtime models (e.g. Dessa):** Only when supported APIs, Android integration, privacy, language quality, latency, real-device audio and provider pricing exist. Run identical blinded morning protocol vs shipped GPT-Live; don't rewrite runtime/kernel or add a generic interface just in case.

**F4 — Longer-horizon chronotype guidance:** Optional educational suggestions around sleep opportunity and wake consistency, not diagnosis, bedtime policing or a new sleep-tracking product.

Each future item requires: named user need → baseline gap → smallest Shape contract → explicit 1.0 scope exception / post-1.0 prioritization → one reviewable implementation PR → controlled field evidence.

## 8. Proposed PR sequence and responsible validation

| PR | Scope / parent | Validation emphasis |
|---|---|---|
| PR-01 | B0 real-morning evidence completeness (W0) | denominator, lineage, unknown/missing, Snooze chain, privacy |
| PR-02+ | B1 observed physical reliability fixes (#9) | real signed device + focused Kernel/Stop/Snooze tests |
| PR-03 | B2 concise conversation pacing (W2/#194) | pure runtime replay, turn/caption/audio boundary, human voice review |
| PR-04 | B3 sound-vs-voice treatment flag (W2) | alarm sound safety on device, assignment immutability |
| PR-05 | B4 selected safe reflex Stop Shape outcome (W3) | terminal action authority, TalkBack, 200% text, real returns-to-bed |
| PR-06 | B6 first-wake / active UI cohesion (#59) | actual user paths + visual and accessibility review |
| PR-07 | B5 bounded personal wake strategy (W5) | migration/replay/hysteresis/rollback + missing feedback |
| PR-08 | B7 analysis and narrow pilot-driven adjustments (#87) | signed device matrix, cohort evidence, data quality |
| PR-09+ | W8 launch operations only when proven (#88) | staged release, provider cost, privacy and restore |

PR numbers above are **planning labels, not actual GitHub PR IDs**. Do not implement all scopes in one code PR; preserve one coherent user-observable slice per merge. Implement the full slice first with quick targeted checks; run broader CI at the end of the affected slice, except essential alarm safety checks which cannot be deferred.

## 9. Definition of Done — no “perfect” by assertion

- [ ] A real signed installation can be upgraded in place without deleting alarms and the scheduled native alarm is physically audible, controllable and robust in the specified tested envelope.
- [ ] Silence, unclear speech, speech-overlap, movement, offline, backend failure, Stop and Snooze produce deterministic, non-nagging, correct session outcomes.
- [ ] Voice naturalness and partner impact are measured, not assumed; sound-only remains an excellent user path.
- [ ] Stop remains instantly usable by all supported accessibility paths; half-asleep-dismissal mitigation is chosen on field evidence, not extra friction.
- [ ] Outcomes show **delivered vs activation-completed vs later confirmed up vs returned to bed vs unknown** with correct denominators and missingness.
- [ ] Learning only changes future bounded policy based on repeated and sufficiently complete evidence; users may inspect and reset it; no regressions in essential alarm safety.
- [ ] Actual participants show repeated-morning usefulness with acceptable annoyance/agency, compared against ordinary alarms or an explicit safety-preserving baseline.
- [ ] Current 1.0 gates #9/#59/#87/#88/#194/#99/#97 have their own documented and evidenced completion. Future light/wearable/model items remain unbuilt until approved.
- [ ] Every delivered PR reports **Implemented / Validated / Deferred / User actions**, and flags unrelated improvement opportunities separately.

## 10. Immediate first actionable block

1. Verify the **latest signed Direct build** and in-place upgrade compatibility; do not infer a new release from merged main.
2. Run existing #9 physical one-night matrix and #194 voice matrix with native sound audibility, then retain failed as well as successful Wake Lab traces.
3. Audit outcome/report lineage from recent PRs #211/#212 and confirm a missed alarm remains in the denominator.
4. Draft the single-page B2 dialogue and sound timing state matrix (including motor-limited and shared-room behavior); replay existing runtime before changing timing.
5. After sufficient real reflex-dismissal examples, select/reject the least intrusive B4 stop treatment through explicit Shape review.
6. Start controlled founder mornings, then recruit the qualified beta cohort. Do not claim efficacy until supported by results.

## 11. Research that informs (but does not prove) design

- Beauchamps et al. (2026), short physiological vs cognitive countermeasures after sleep deprivation, exploratory laboratory RCT: https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2026.1855408/full
- Sundelin et al. (published online 2023; issue 2024), Snooze effects in habitual users, laboratory n=31: https://pubmed.ncbi.nlm.nih.gov/37849039/
- Hilditch et al. review of reactive countermeasures and limitations: https://pmc.ncbi.nlm.nih.gov/articles/PMC5136610/
- Canonical in-project evidence and exclusions: [UX psychology](../04-ux-psychology.md) · [Wake Runtime](../11-wake-runtime-state-machine.md) · [Wake Learning](../14-wake-strategy-learning.md) · [Measurement](../22-product-metrics-experiments.md) · [Beta runbook](../36-beta-validation-runbook.md) · [Physical acceptance](../37-physical-acceptance.md) · [Original excellence plan](2026-10-23-wake-experience-excellence-plan.md).

**Evidence limitation:** These studies assess immediate alertness, response or cognition, not WakeMyWay's sustained real-world wake success. Controlled field validation is required before any product-performance claim.

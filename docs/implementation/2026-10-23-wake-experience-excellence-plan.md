# WakeMyWay — Wake Experience Excellence Implementation Plan

**Date:** 2026-10-23  
**Status:** Proposed execution plan; documentation only, no production behavior changed by this PR  
**Baseline:** `main` after PRs #207/#208, latest published Direct release `v0.2.32` (2026-10-22)  
**Program:** [1.0 paid-launch readiness](../35-paid-launch-readiness.md) (#89)  
**Execution gates:** TRUST #9 → FINISH #59 → PROVE #87 → SELL #88; voice #194, device accessibility #99, startup #97  
**Product baseline:** [core experience reset](../45-core-experience-quality-reset.md), [product vision](../01-product-vision.md), [domain context](../../CONTEXT.md)  
**Design baseline:** [Sunroom Club / Sunrise + Wake Line](../07-design-system.md)  
**Intended audience:** Product owner, Android engineers, voice/backend engineers, product designers, QA and beta operators.

> **The product outcome is not “AI responded” or “Activation Completion fired.”** It is that a person who intended to wake at a specific time actually gets moving, with minimal effective friction, and continues to trust and use WakeMyWay.

## 0. Decision summary

The application already has substantial architecture and capability. Its most important remaining uncertainty is **real-world waking effectiveness** and the pleasantness and reliability of the complete morning journey. Do not start a new feature expansion program or rebuild the architecture.

1. **TRUST first:** prove a scheduled alarm rings and remains controllable on the supported physical device matrix, even when the app, UI, network and AI fail.
2. **Make an intentional Stop safer without blocking it:** investigate reflex dismissal; preserve a direct, accessible, offline Stop at all times. Explore a lightweight intentionality aid only after evidence and Shape review. Never silently rearm a stopped alarm.
3. **Improve the whole spoken interaction:** make it feel like one consistent, non-demanding character helping the user transition into activity; prove this with actual morning recordings/ratings and device traces (do not collect raw user audio by default).
4. **Reduce pre-wake cognitive overhead:** reach the first committed alarm quickly; demote unneeded setup, status and technical vocabulary; preserve approved Sunroom visual identity.
5. **Make learning truthful and useful:** keep local bounded Wake Learning; separate Activation Completion from Confirmed Wake Success; change policy only from repeated trustworthy evidence, accounting for annoyance.
6. **Prove repeat use and commercial value:** use the existing target-user beta gates before deciding pricing, extra features or paid acquisition.

**Scope classification:** Reliability fixes, copy/flow simplification, measurement, accessibility, bug fixes and bounded tuning are allowed by the 1.0 freeze. Any materially new Wake Assurance behavior (e.g., new post-Stop prompts) remains **Explore → Shape** and needs a documented exception under [the freeze rule](../35-paid-launch-readiness.md#feature-freeze), not immediate Build authorization.

**This PR contains a plan, not its implementation.** Later Build PRs should be small, coherent, independently reviewable vertical slices, all traced to the single program plan and existing gate issues. No release, deployment, database migration or model enablement is implied by merging this documentation.

## 1. Current-state evidence vs assumptions

| Area | Existing baseline (do not rebuild) | Gap / constraint |
|---|---|---|
| Alarm | Local Alarm Kernel, exact schedules, device-protected recovery, critical playback service, transactional Stop/Snooze, test harness | Hostile real-device / overnight matrix #9 open; no blanket guarantee against Android/OEM limits |
| Runtime | Deterministic Wake Runtime; local motion/interaction evidence; separate completion and real-world success concepts | Coherent conversation alone contributes evidence; phone-observed activation can be a false positive |
| Voice | Direct GPT-Live-first implementation, eligibility preflight, conservative PCM/transcript quiet handling, bounded failure and native-sound degradation, persona constraints | Human voice quality and speaker audibility unproven; ICE/route/latency behavior needs physical tests (#194) |
| UI | Sunroom Club design, Tomorrow Contract, Today/Alarms/Profile/Insights, onboarding defaults, Wake Lab | Onboarding includes eight stages if fully followed; some screens expose too much choice/system language; physical fidelity and accessibility remain open (#59/#99) |
| Stop | Explicit one-tap Stop and Snooze on active Wake surface, committed through Kernel | Easy reflex dismissal; current follow-up is a non-critical **15-minute** after-Stop calibration notification, not a prevention mechanism |
| Learning | Local, versioned bounded adjustments to activation threshold and escalation based on calibrated outcomes and friction feedback | Not evidence that WakeMyWay identifies optimal conversation strategy; insufficient real-morning data |
| Context | Optional local Tomorrow Contract and selected Android-synced calendar enrichment | Keep optional, minimized, non-critical; do not expand into an assistant/calendar reader |
| Launch | Signed Direct APK/AAB workflow, automated tests and staged Play plan | Target-user beta, production operations, actual billed costs, on-device acceptance and commercial evidence remain open (#87/#88) |

**Evidence labels used below:**
- **Implemented** = exists in current `main`; does not imply physical validation.
- **Proposed** = design/implementation work scheduled by this plan, not delivered.
- **Hypothesis** = requires human/field evidence before a product-policy choice.
- **Exit gate** = measurable acceptance required to progress or make a launch claim.

The codebase and documents are the source of truth for detailed behavior. If a future PR discovers a mismatch, reconcile it against `CONTEXT.md` and update this plan or relevant canonical doc before silently changing product semantics.

## 2. Invariants and explicitly rejected shortcuts

**Hard invariants**
- Alarm Kernel is sole authority over scheduling, active execution, native sound, recovery, and Stop/Snooze; `WakeActivity` is a view, not the execution owner.
- Stop remains visible, accessible, offline-capable and immediate once the kernel acknowledges it. No math task, mandatory voice reply, hidden control, punitive confirmation loop or unconsented reactivation.
- An intentional Stop is terminal for that occurrence. A post-Stop check may be optional and informational; any future audible re-wake must be a separately and explicitly agreed, durably scheduled occurrence, and must pass new Shape/reliability review.
- Failed Snooze cannot silence an active alarm without a durable replacement; stale callbacks/Activities cannot resurrect stopped alarms.
- Wake Runtime, not AI/model text, decides all phases and behavioral evidence. Voice is expressive presentation, not authority.
- Native critical sound begins independently of network, Live eligibility, accounts, billing, model readiness, calendar, learning or analytics. A failing voice enrichment returns to local sound/controls without changing character surreptitiously.
- No automatic interpretation of “I am up,” a transcript, movement of the phone, or a completed runtime phase as physiological proof of being out of bed.
- No raw morning audio, full transcripts, sensitive Tomorrow Contract/calendar text, or raw motion traces in analytics, logs or bug reports.
- No paywall, offer, account prompt or rating form during Active Wake; no coercion, shame or patronizing copy.
- Existing Alarm Definitions and their durable schedules survive in-place signed upgrades; never use data resets as part of testing or rollout.
- Android is the current client; `:wake-core` remains pure Kotlin; optional cloud never gains critical alarm authority.

**Non-goals for the 1.0 execution program**
- New characters/voices just for variety, new dashboards, theme systems, wearables, QR/photo/math challenges, generic AI assistant, social waking, arbitrary smart-home integrations, full sleep tracking, Health Connect, calendar expansion or a new backend architecture.
- Global redesign replacing the approved Sunroom reference or changing the launcher mark.
- Claiming improved sleep health or clinically verified wakefulness.
- Optimizing for longer conversations, app opens, more interventions or more AI calls.

## 3. Golden journey and product state contract

**Golden path:** install → understand the wake promise → commit first Alarm Definition → see honest Wake Ready state → leave app → alarm sounds on time → optional consistent Live interaction → one manageable activation cue → meaningful behavioral evidence → brief orientation / First Move → durable clean end → occasional later outcome calibration.

| Situation | Consumer-facing expectation | Owner / deterministic boundary |
|---|---|---|
| No alarm configured | Prominent “Set your first wake” action; no fake ready state | UI → AlarmProductController → Kernel |
| Alarm lacks critical permission | Explains which wake capability is missing and one repair action | Kernel readiness projection, not cloud state |
| Alarm fired; Live not ready | Native sound and usable Stop/Snooze from the start; voice shown as connecting only when genuinely connecting | Playback service + WakeActivity |
| Live connects and audio is actually present | One stable character, one short invitation, coherent interruption/listening indicators | Voice presentation; Wake Runtime authorizes turns |
| Live stalls, loses route or fails | Explain degradation once; restore local sound; never leave silence or stuck listening | Voice adapter → service-owned native fallback |
| User silent / sleepy | Small, nonjudgmental progression with reasonable pauses, never incessant spoken-compliance demands | Runtime policy, no model escalation authority |
| User picks up phone / responds | Partial engagement evidence, **not** automatically out of bed | Runtime evidence accumulator |
| Activation criterion met | Orient with brief reason and First Move, then finish without pretending bodily wake success is confirmed | Runtime / history |
| User taps Stop before activation | Stop immediately and record `STOPPED`; optional later calibration, not forced wake | Kernel + history/follow-up |
| Snooze accepted | Durable exact replacement before silencing active execution | Kernel |
| Phone locked / mic denied / offline | Controllable native alarm remains; private context/captions hidden | Direct Boot / privacy rules |
| User later says “back to bed” | Record calibration; distinguish from operational completion; possible bounded future learning, never rewrite history | History → Wake Learning off-session |

Do not add a second competing “successful wake” state inside the UI or cloud; use canonical `WakeOutcome`, `Activation Completion`, `Confirmed Wake Success` terms as documented.

## 4. Workstream sequencing and ownership

| ID | Priority / gate | Outcome | Depends on | Delivery unit |
|---|---|---|---|---|
| W0 | P0 / TRUST+PROVE | Reliable baseline diagnostics and denominators | Current main | One focused measurement/acceptance slice |
| W1 | P0 / TRUST #9 | Supported-phone alarm reliability physically proven | W0 | Targeted defect PRs, device evidence |
| W2 | P0 / voice #194 | Speaker-audible, human-feeling voice with safe fallback | W0 + W1's critical invariants | Targeted Live/runtime/presentation PRs |
| W3 | P0 / FINISH | Reflex-stop problem shaped; safe choice implemented only if warranted | W0; product Shape decision | Separate minimal interaction/feedback PRs |
| W4 | P1 / FINISH #59 | Clear first wake, Today, editor and Wake surface | Existing flow inventory; W1/W2 contracts | Per-flow UI PRs |
| W5 | P1 / PROVE | Measured, explainable behavioral tuning | W0 + calibrated beta evidence | Small local policy PR(s) |
| W6 | P1 / FINISH | Accessibility, localization and performance accepted | W2–W4 changed surfaces | Targeted fixes + physical signoff |
| W7 | Launch / PROVE #87 | Compare real morning outcomes to baseline over repeated mornings | W1/W2 P0 acceptance | Beta evidence, defect follow-ups |
| W8 | Launch / SELL #88 | Truthful, supportable monetization and staged release | W7 product/value evidence | Existing launch issue slices |

**Ordering:** W0 starts immediately. W1/W2 can be investigated in parallel without shipping unsafe behavior. W3 gets a Shape decision early but does **not** outrank physical wake trust. W4 can be built after the critical contract is stable. W5 cannot be called “intelligent learning” before W7 yields meaningful calibration data. W8 preparatory legal/support work may overlap; price locking and paid rollout must wait for W7.

**WIP rule:** one primary product-facing Build slice at a time. The plan is a program umbrella, not authorization for a single giant implementation PR.

## 5. W0 — Establish the real-world quality baseline

**Goal:** Explain what happened on every intended morning and whether it achieved the user's purpose, without spying, inventing success or allowing measurement to affect alarm delivery.

**Relevant existing locations:** `alarm/WakeTimingTrace.kt`, `alarm/AlarmScheduleHealth.kt`, `product/history/WakeHistorySessionRecorder.kt`, `product/history/WakeHistoryRepository.kt`, `product/insights/WakeInsightsProjector.kt`, `product/followup/WakeSafetyCheck.kt`, `ui/developer/WakeAlarmLabScreen.kt`, existing local voice diagnostics.

**Build tasks**
1. Audit the existing event journal and history lineage; produce a source-of-truth mapping from Alarm Definition → Wake Occurrence → active execution → Wake Session → terminal reason → later calibration, including Snooze replacements, collisions, Lab isolation and missing data.
2. Verify attribution of target alarm time, receiver/audio start, real audio-route state when observable, presentation readiness, Live preflight/connect/first remote PCM, first engagement, activation movement, intentional Stop/Snooze, failure reason and final outcome.
3. Define one privacy-safe, local export/report for founder and beta analysis. Prefer extending existing diagnostics instead of a second telemetry stack. Label **“PCM observed” != “audible to user.”**
4. Record optional one-tap confirmed outcome and annoyance/agency feedback at a sane post-wake cadence. Do not assume nonresponse means success or failure.
5. Establish a baseline of normal-alarm outcomes for qualified users when feasible. Record backup alarm use and the exact device/OS/release/permission envelope separately.
6. Establish voice cost measurement from actual provider/operator evidence; usage duration alone is not billed currency. Prevent session retries that could double charge.

**Event dictionary / denominators (acceptance)**
- **Expected wake:** the eligible scheduled occurrence prior to user cancellation, with supported-device/capability cohort noted; count a missing journal or unknown delivery as unknown/failure-to-investigate, never as success.
- **Alarm delivery:** report numerator, denominator, evidence quality, user-reported silent/late events and reason categories.
- **Activation Completion:** operational runtime completions / eligible interactive attempts, excluding Lab events and clearly distinguishing Snooze chains from separate mornings.
- **Confirmed Wake Success:** confirmed successful real-world wakes / *answered* eligible calibrations; always pair with coverage = answered / requested-eligible wakes.
- **Returned-to-bed after Activation Completion:** confirmed return-to-bed events / answered calibrated activation completions.
- **Friction:** annoyance and agency rating distributions and missing-response rate; not one synthetic “morning quality” score.
- **Voice:** proportion admitted, speaker-audible by human verification, first audible latency p50/p95, fallback category, human naturalness, repeated-cue rate and cost per actual session.
- **Retention:** eligible users returning on D7/D14 (D30 when possible), with wake-frequency context and no “engagement streak” pressure.

**Exit:** A reproducible anonymized report for at least a few founder mornings, including failure/unknown examples, links to traces and a clear event dictionary. Do not set speculative wake-success percentage launch targets before the first qualified cohort.

## 6. W1 — Close physical alarm reliability (#9)

**Goal:** Make each committed wake reliable and controllable within a precisely documented supported-device envelope.

**Code owners:** `alarm/AlarmKernel.kt`, `alarm/AlarmRegistrar.kt`, `alarm/AlarmReceiver.kt`, `alarm/AlarmPlaybackService.kt`, `alarm/AlarmReconcileReceiver.kt`, `alarm/CriticalWakeStore.kt`, `alarm/WakeTerminalActions.kt`, `WakeActivity.kt`, `WakeSessionViewModel.kt`.

**Execution**
1. Run the existing reliability matrix with the exact signed release installed as an in-place upgrade on the real OnePlus. Retain **both passing and failing** exported traces. No uninstall/data-clear shortcut.
2. Probe screen-off, locked screen, Doze/idle, process death/recreation, service recreation, reboot before first unlock, time/timezone changes, simultaneous alarms, future exact-alarm capability loss, notification/full-screen/channel loss, volume/silent/DND conditions, and Stop/Snooze during a voice session.
3. Run headset/Bluetooth, active phone call, route changes, cellular↔Wi-Fi, offline, mic denial, and a disabled/unavailable backend. Critical audio is independent, and the UI cannot claim voice/speaker audibility merely because the transport succeeded.
4. Classify every miss/late/wrong-UI/stuck/duplicated/resurrected occurrence. Fix only observed classes; add deterministic regression coverage for each. Keep kernel service authority and independent multi-alarm ownership.
5. Validate signed APK upgrade preservation and migration with alarms already set; verify UI readiness projection equals kernel truth; include negative cases where permissions cannot be repaired automatically.
6. Document reproducible OEM/platform limitations and recovery instructions; do not promise operation after power-off, uninstall, force-stop or revoked capabilities that OS prevents.

**Exit gate:** No unexplained missed critical wakes, false success of Stop/Snooze, resurrected terminal alarms, or uncontrollable active alarm **in the explicitly exercised supported matrix**; representative traces attached to #9. p50/p95/P99 native-audio, alarm→UI, Stop/Snooze ack and recovery metrics recorded, compared with existing target thresholds in #9. Any outstanding critical failure blocks broader claims/rollout.

**Rollback:** Retain signed known-good build and the ability to revert suspect alarm-path PRs without clearing persisted schedules; pause distribution if a scheduled alarm safety regression is detected.

## 7. W2 — Premium conversation that actually helps (#194)

**Goal:** The user hears **one consistent, pleasant person** who speaks briefly, listens correctly, respects silence and interruptions, and advances only when runtime evidence warrants it.

**Code owners:** `voice/WakeVoiceSessionController.kt`, `voice/WakeVoiceAudioPolicy.kt`, `voice/WakeVoicePresentation.kt`, `direct/voice/GptLivePreferredWakeConversation.kt`, `direct/voice/GptLiveWebRtcSession.kt`, `direct/voice/GptLiveAudioActivityGate.kt`, `direct/voice/GptLiveTurnSettlement.kt`, `direct/voice/GptLiveUserTurnGate.kt`, `direct/voice/GptLiveFallbackPolicy.kt`, `wake-core/runtime/WakeRuntime.kt`, `WakeActivity.kt`.

**Behavioral contract to shape/test**
- **Start:** Native sound is already present; one low-pressure greeting, no instant list of instructions or mandatory spoken answer.
- **Awkward silence:** Give the person time; use bounded, nonjudgmental re-engagement rather than escalating because no transcript arrived.
- **User responds “no,” says nothing, or interrupts:** Respect barge-in; no overlap, character swap, obedience demands or a repeated cue unless the deterministic runtime requests re-engagement.
- **User moves:** Movement is evidence, not permission for AI to say “you’re definitely awake.” Do not repeatedly require voice confirmation once reasonable motion is present.
- **Character:** Stable accent, warmth, tone, language, pronunciation and humor throughout an occurrence; never switch to another character silently on fallback.
- **Orienting:** Only after activation evidence, one short context/reason/First Move; don't read a whole calendar. Private captions remain unlock-gated.
- **End:** Stop speaking promptly after runtime terminal directives; release mic, focus, peer and paid session resources.

**Technical execution**
1. Build a deterministic scenario corpus (sleepy “no”, background TV, silence, very short reply, interruption during remote PCM, second utterance before playback settles, network flap, provider 403/429, route switch, app background).
2. Verify runtime speech authorization vs actual PCM and transcript events. Keep conservative quiet detection explicitly heuristic; inspect traces for false completion, stuck listening and double replies.
3. Tune turn length, pause budgets, re-engagement rhythm and phrasing by recorded *human review* of reproducible synthetic/demo scenarios and live founder tests, not prompt tweaking alone.
4. Test English/German/Hebrew with native speakers; voice language is independent of Android UI locale. Mark unsupported voice/persona combinations truthfully; do not claim all variants have equal quality.
5. Measure first **speaker-audible** voice in addition to broker/ICE/PCM metrics; test speaker, Bluetooth, phone calls, low media volume and Doze. Restore native playback predictably on failure.
6. Review actual provider admission and paid-session cost; maintain quota and denial behavior. Do not add new providers or speculative cloud interfaces for this pass.

**Exit gate:** Physical cases pass with no persona changes, unauthorized turns, overlapping speech, bogus wake claims or critical-sound regressions; human ratings show sufficiently pleasant, non-annoying voice across several mornings and each supported language. Record source sample count and uncertainty. If voice makes waking worse for a cohort, preserve alarm-only experience and investigate before expanding Live.

## 8. W3 — Reflex dismissal and truthful post-Stop behavior

**Problem:** People may stop the alarm half-asleep without even getting out of bed. The current large Stop control makes reflex dismissal possible; that control is also essential for safety and agency. **Do not “solve” this by trapping users.**

**Decision status: HYPOTHESIS / SHAPE REQUIRED**, especially for any new post-Stop interaction.

**Existing code:** `WakeActivity.kt` → `WakeSafetyFooter`, `WakeSessionViewModel.kt`, `alarm/WakeTerminalActions.kt`, `product/followup/WakeSafetyCheck.kt`, `product/history/WakeHistorySessionRecorder.kt`, `wake-core/runtime/WakeRuntime.kt`.

**Preferred staged approach**
- **W3a, measure:** Quantify intentional Stops before activation, durations from alarm fire to Stop, return-to-bed confirmations, overnight user reports, and accessibility needs. Distinguish accidental touch from a deliberate choice; tapping quickly is *not* proof of accident.
- **W3b, Shape options:** Compare (A) clear visible one-tap Stop with better positioning/distinction from Snooze, (B) one lightweight confirmation that never blocks an accessible Stop fallback, and (C) an *opt-in* intentionality interaction for users with repeated confirmed reflex dismissal. Do not choose a hold/gesture without motor/TalkBack testing. Shape copy and screenshots for screen locked/unlocked, one hand, font 200%, reduced motion, mis-tap, Voice unavailable and accessibility service use.
- **W3c, evidence-backed minimum:** Implement the smallest accepted option in a focused PR. Never require AI, microphone, account or internet for dismissal; no automatic rescheduling or hidden delay once Stop is committed. If friction raises annoyance or causes users to disable the app, revert.
- **W3d, follow-through:** Existing 15-minute WorkManager notification remains informational; evaluate usefulness versus notification fatigue. A shorter check or new feedback cadence is a separate opt-in proposal, not an automatically repeating alarm. Any separately scheduled “try again” wake must be explicitly opted into as a new kernel-owned scheduled occurrence, with its own permission, settings, revocation, audit and failure contract. **Default for 1.0: do not ship this addition unless data proves necessary.**

**Stop decision table**
| Trigger | Required action | Disallowed |
|---|---|---|
| Stop while voice connects/speaks/listens | Kernel-acknowledged terminal Stop; release audio/mic/voice | Provider veto, delayed Stop pending AI |
| Stop while phone locked / offline | Same locally controllable Stop | Forced unlock/network response |
| User confirms optional intentionality aid (if accepted) | Perform Stop once and persist terminal outcome | Automatic recurring alarms |
| Stop commit fails | Keep current alarm safely actionable and explain failure | UI falsely showing “finished” |
| Stale Stop from old occurrence | Reject or ignore by occurrence identity | Stopping a newer alarm |
| Post-Stop feedback notification | Optional/clearable calibration, no guaranteed timing | Claim to have prevented return to sleep |
| User requires accessibility alternative | Equivalent visible, labeled one-action terminal control | Unavoidable long hold/puzzle/complex gesture |

**Decision gate:** Select or reject W3b only after founder observations and initial target-user evidence demonstrate benefit over standard Stop. Document the accepted Shape contract and, if the feature freeze exception is used, its observed blocker and why simplification was insufficient. Report Stop→returned-to-bed outcome and annoyance by variant, not just fewer taps. Never equate “harder to dismiss” with “more effective wake.”

## 9. W4 — Simplify the first-value and daily UI (#59)

**Design rule:** Use approved Sunroom Club colors, Warm planning surfaces, Wake Line and stable native Android semantics. This is a content-first convergence pass, not permission for another sweeping restyle.

**Surfaces and implementation slices**

| Slice | Files | Required change / success |
|---|---|---|
| W4a: First wake | `ui/onboarding/OnboardingScreen.kt`, `ui/navigation/WakeMyWayApp.kt` | Minimum explanation → first alarm quickly, with balanced defaults; optional preferences after first experience. Existing skip/complete navigation remains correct; no fake setup-success state. |
| W4b: Home | `ui/home/TonightScreen.kt`, readiness projections | Dominance of **when → why → readiness → next action**. Show Morning Check-In, contract and voice-repair cards only when relevant, not a permanent system-status dashboard. |
| W4c: Editor | `ui/alarms/AlarmEditorScreen.kt`, `ui/alarms/AlarmsScreen.kt` | Time, days, reason and save first; rich customization secondary. Default voice and Snooze states must be truthful. Saving shows successful kernel commit, not optimistic navigation. |
| W4d: Active Wake | `WakeActivity.kt`, `ui/components/Wmw*.kt` | Each phase matches actual audio/listening/runtime state; one visual demand at a time, no dense content during sleep inertia; Stop/Snooze always accessible. |
| W4e: Profile / Insights | `ui/profile/ProfileScreen.kt`, `ui/profile/WakePreferencesScreen.kt`, `ui/insights/InsightsScreen.kt` | Explain personal wake controls and learned adjustments in plain language. Do not display synthetic precision or imply absent calibration is success. |

**Acceptance process:** Before building each slice, inventory *actual currently implemented content* and UI states (empty, ready, repairing, offline, failed update, long text, voice unavailable). Approve a content/state matrix and one concrete interaction specification. Use the existing canonical reference, on-device screenshot comparison and Roborazzi baselines to check design fidelity. Remove redundant UI instead of designing missing features to fill space.

**Checks:** cold first launch → first scheduled alarm; Back/Save/delete flows; multiple alarms with independent readiness; responsive layouts; light/dark Wake presentation; TalkBack order and labels; screenshot sizes 320–430dp, font scaling 200%, system RTL and reduced motion. Physical #59 signoff is required, not only goldens.

## 10. W5 — Learning and wake-effectiveness tuning

**Owner:** `wake-core/src/main/kotlin/com/wakemyway/core/learning/WakeLearning.kt`, `WakeLearningModels.kt`, `runtime/WakeRuntime.kt`, `motion/MotionEvidenceExtractor.kt`, `product/learning/WakeLearningRepository.kt`, history/calibration/Insights projection.

**Current behavior:** The local learning engine makes bounded policy updates after repeated sessions; it can adjust activation threshold or escalation based on calibrated false-positive completion, insufficient activation, annoyance and agency. This is intentionally conservative.

**Implementation strategy**
1. Before adding new adaptive policy, verify the complete evidence chain and explain what each current rule can legitimately infer. No medical wake verification; no learning from test Lab sessions.
2. Evaluate false-positive patterns: user moves phone, gives a coherent answer, reaches Activation Completion, then actually returns to bed. Check how well current weights/thresholds predict confirmed real outcomes without excluding accessibility cohorts.
3. Tune *existing* bounded parameters only when repeated calibrated data shows a useful direction; preserve versioned policies, hysteresis, consent-relevant preferences and rollback/reset behavior. A novel algorithm or ML personalization is deferred.
4. Introduce a consumer-facing truthful explanation only when an actual policy adjustment was persisted: “We’ll try a gentler start” or comparable text backed by the exact rule. Do not say “we learned you wake best from jokes” without evidence of that mechanism.
5. Measure whether each change improves **Confirmed Wake Success** and preserves agency. If outcomes are unknown or sample too small, the correct behavior is **no learned change**.

**Exit:** Reproducible rules, adversarial test cases, rollback safety, no oscillating policy across a few unusual mornings, and beta evidence of useful calibrated changes. If learning value is not demonstrated, keep v0 stable and market the product honestly instead of expanding personalization.

## 11. W6 — Accessibility, internationalization, performance, cohesion

**Accessibility / inclusive waking**
- Complete #99 physical TalkBack and Remove Animations coverage for onboarding, alarm setup, full-screen Wake, Stop/Snooze and recovery. Do not use color/animation/voice as the only source of session status.
- Large touch targets, reading order, clear focus, contrast, gesture alternatives, phone-only/voice-disabled access, quiet contexts, motor limitations and no mandatory ability to stand.
- Avoid alarming visual flashes or unexpectedly loud communication-route changes. Respect reduced motion and native alarm volume policy.

**Localization**
- Voice supports explicit English, German, Hebrew and system choice; consumer-facing UI still has hardcoded English in important Compose files. Inventory and migrate user-visible copy to resources in coherent screen slices. Correct RTL direction and mixed-time/date/Latin text handling, not only translation.
- Confirm human voice quality per language before calling the language “premium”; support clear fallback/unsupported states.

**Startup / distribution**
- Complete #97 physical Macrobenchmark startup measurements and identify the largest opportunities after core correctness is proven.
- Investigate the latest Direct APK size (~80.8 MB asset reported by release v0.2.32) and Play AAB (~39.1 MB). Inspect packaged binaries/audio/dependencies rather than removing safety assets or risking migration continuity. Record before/after and update size ceilings only with evidence.
- Remove stale developer-facing language and resolve **Plus vs Pro** consumer naming consistently once canonical packaging is chosen; never invent entitlement semantics or prices.
- Validate in-place upgrades retain alarms, preferences and scheduling authority for both Direct/Play flavors. Signed artifact is the test subject.

**Exit:** Accessible documented physical traversal, verified release cold start, measured packaging tradeoffs, no UI/voice language contradictions, and updated screenshots for actual consumer flows.

## 12. W7 — Target-user beta: prove the product (#87)

**Participants:** 10–20 qualified habitual snoozers who intentionally want to wake at chosen times, not primarily developers or AI enthusiasts. Use the [beta runbook](../36-beta-validation-runbook.md). Aim for 14–21 days/person and at least five intended wake mornings for basic outcome analysis.

**Study design**
1. Screen for real reflex dismissal/staying-in-bed history, Android support, willingness to report brief feedback, and explicit privacy consent.
2. Establish practical baseline on existing alarm and, where feasible and safe, compare comparable scheduled mornings using regular alarm-only versus WakeMyWay voice. Do not withhold a needed safety alarm or randomly disable alarm reliability.
3. Retain precise denominators, unavailable/unknown outcomes, intervention configuration, actual version/device, alarms overlapping, sleep schedule variability and concurrent conventional backup use.
4. Use same defined outcome, calibration/annoyance scales and retention windows for every cohort. If comparing two variants, predeclare assignment order and prevent time-of-week confounds where practical.
5. Conduct repeated-morning qualitative interviews: character pleasantness after novelty fades; unwanted demands, “AI acting”, voice continuity, speed to feet-on-floor and whether the user turned the system off.
6. Review weekly: keep/change/remove/defer. Fix P0 crashes, missed wakes, alarming permissions, misleading UI and user-hostile interactions; avoid feature additions that destroy comparability.
7. Review willingness to pay only after enough meaningful mornings and actual consumer packaging/price exposure; do not confuse compliments with conversion or early enthusiasm with retention.

**Outputs**
- Anonymized cohort table, eligible wake denominator and missingness report.
- Normal alarm vs WakeMyWay Confirmed Wake Success and return-to-bed comparison with uncertainty (no causal claim from an uncontrolled sample).
- p50/p95 timings, native failure classes, Live reliability/annoyance, calibrated activation false positives, device/OS matrix.
- D7/D14 retention, perceived agency, free-vs-Plus value signals and clear top five issues.
- Decision memo: proceed to staged paid launch, fix-and-repeat, or simplify/roll back voice/learning assumptions.

**Exit:** #87 accepted with sufficiently credible real-world outcome and retention evidence; thresholds are selected from observed cohort distribution **before** broader rollout, not invented in advance.

## 13. W8 — Sell only what works (#88)

- Preserve trustworthy Free alarm with no ads and no critical authority depending on billing.
- Validate the premium value around *reliably better mornings*, not access to a particular provider, number of messages or arbitrary premium toggles.
- Reconcile the canonical `WakeMyWay Plus` naming (monetization plan) with remaining `Pro` copy/technical identifiers without changing platform SKU IDs opportunistically.
- After #87: settle price hypothesis, preview/trial behavior and entitlement boundaries using actual purchases and Google Play policies at submission time.
- Complete support FAQ (permissions, battery/OEM, Android routing, Stop/Snooze, upgrades, voice degradation), public privacy/legal and Data Safety disclosures, opt-in AI audio notices, diagnostic redaction, crash/ANR/operational observability and incident/rollback runbook.
- Gate each staged release on the supported signed-build physical matrix and an operator-reviewed cost/capability/config checklist. Do not present Direct Live support as guaranteed Play parity.

**Exit:** #88 launch gates green *after* TRUST/FINISH/PROVE. Publishing v0.2.32 does not establish full 1.0 readiness.

## 14. Proposed implementation PR breakdown (after this plan PR)

All implementation PRs reference this plan + an existing gate/issue and explicitly state **Built / Validated / Deferred / User actions**.

| Follow-up PR (illustrative, not created here) | Minimum code change | Validation before merge | Hard blocker |
|---|---|---|---|
| 01 — Wake diagnostics truth | Extend existing occurrence/history/voice journal and export only | Unit/integration lineage, synthetic missing data, privacy review | No new raw data retention |
| 02 — Physical alarm defects | Only reproduced Kernel/service/presentation failures | Focused regression + critical Android CI/device lane + signed phone | #9 |
| 03 — Conversation quality | Bounded Live turn/audio/persona + runtime cue tuning | Scenario corpus + audio/device review + regression | #194 |
| 04 — Safe Stop clarity | Accepted W3 Shape interaction; no new scheduling by default | Full Stop failure matrix, accessible gestures/taps, real returns to bed | W3 evidence/Shape |
| 05 — First-wake simplicity | Onboarding/nav/editor entry | Compose visual, new install, committed alarm | #59 |
| 06 — Daily flow coherence | Today/editor/Wake copy + state truth | Screen state matrix, visual/accessibility, actual sound/UI parity | #59 |
| 07 — Local learning calibration | Only evidenced bounded policy changes | Deterministic history/replay, false-positive and annoyance | #87 evidence |
| 08 — Language/performance | Copy localization, release size/startup and terminology | RTL, TalkBack, device Macrobenchmark, upgrade preservation | #97/#99 |
| 09 — Beta-driven remediation | Narrow fixes observed in study | Repro + focused tests + repeat target-user check | #87 |
| 10 — Commerce/launch | Existing #88 slices only after evidence | Play purchase/restore, consent/privacy, rollback, staged verification | #88 |

For feature work, finish the coherent planned implementation and run quick local/targeted checks before invoking expensive full CI; run comprehensive suites and end-to-end gates at the end of the affected slice. Critical alarm/Stop/Snooze behavior still requires immediate safety-focused regression checks. Do not defer essential safety evidence to after deployment. Documentation-only PRs need content/link verification, not Android CI.

## 15. Unified acceptance matrix

| Axis | Specific scenarios | Required evidence |
|---|---|---|
| Critical delivery | locked, Doze, process/service death, reboot/Direct Boot, time change, update, multi-alarm | expected/actual occurrence traces, timing, audible on phone, no miss |
| Terminal authority | Stop during any wake phase and Live state, Snooze replacement failure, stale callback, UI recreation | durable kernel state, no unexpected playback after Stop, no silent failed Snooze |
| Voice quality | live success, user barge-in, silence, noise, pause, no response, persona/language variation | annotated trace and human ratings, no overlapping turns |
| Voice degradation | backend off, 403/429, failed ICE, lost network, route/call change, mic denied | continuous native fallback + truthful UI; bounded cleanup/cost |
| Accessibility | TalkBack, font 200%, screen reader reading order, one-hand use, reduced motion, RTL, motor and no-stand preferences | physical evidence and screenshots, usable Stop alternative |
| First run | no account, no data, reject permissions, repair/skip, save one/weekly alarm, back/navigation | first committed Wake Occurrence and correct readiness |
| History/learning | completed, stopped early, snoozed chain, missing feedback, back to bed, Lab events | correct denominators, no fabricated success or learning |
| Privacy | locked/unlocked, calendar title consent, captions, logs/export, migration and reset | minimized data, no secret/private content leak |
| Performance | cold start, alarm-to-native sound, alarm-to-visible UI, first audible voice, size | p50/p95/P99 as appropriate, measured device/release |
| Business | free alarm without auth/entitlement, network down, plan restore/cancel | no paid gate affecting alarm; truthful billing |

For critical invariants, any reproducible violation is a blocker regardless of average score. For perceived voice quality and product effectiveness, report sample sizes, the distribution and comparisons, not unverifiable “perfect” claims.

## 16. Risk register and fallback choices

| Risk | Signal | Mitigation / fallback |
|---|---|---|
| Stop becomes coercive | users feel trapped, accessibility failure, app disabling | revert optional friction; keep immediate Stop and post-wake learning |
| AI sounds annoying after novelty | repeated instructions, low ratings, abandoned voice | use native alarm-only, tune cadence or reduce voice length |
| Physical audio path unreliable | PCM present but no audible speech, route/Doze failures | restore local sound, refuse falsely “ready” voice claims |
| Learning overfits | policy oscillation or worsened confirmed success | default conservative policy, bounded updates, rollback/reset |
| Insights mislead | reported “success” without calibration | preserve unknown denominator; no inflated percentage |
| Beta results biased | tech enthusiasts / incomplete calibration / backup overlap | qualified cohort, enrollment rules, missingness, honest uncertainty |
| Remote cost spikes | admitted paid sessions without outcome value | quotas, exact usage validation, no speculative double-session retry |
| Scope creep | unrelated features in FINISH PRs | freeze rule, Shape review, one observable outcome per Build PR |
| Regressed alarms on upgrade | missing future occurrence after APK replacement | signed in-place upgrade test, rollback strategy, no clearing storage |
| Localized voice mismatch | wrong accent/unsupported voice/style | human-language acceptance, truthful fallback/limited supported combos |

## 17. Definition of Done for this program

- [ ] #9 has reproducible physical evidence for supported devices and no unresolved critical delivery/terminal violations.
- [ ] #194 has reproducible speaker-audible, natural, uninterrupted Live voice evidence and a verified native fallback.
- [ ] Accidental Stop hypothesis was measured; either a safe minimal solution was shaped, implemented and validated **or** explicitly rejected/deferred based on evidence.
- [ ] #59 first-alarm path and daily UI can be understood and used without founder explanation, including degraded/permission states.
- [ ] #99 and #97 have actual physical acceptance (not only simulator/CI artifacts).
- [ ] Product outcomes clearly distinguish device-observed completion from reported real morning success, with calibration coverage.
- [ ] #87 has qualified target-user, repeated-morning usefulness, annoyance and D7/D14 retention evidence.
- [ ] Learning remains bounded and truthful; changes justified by observed outcomes rather than novelty.
- [ ] Privacy, upgrade/migration, cost and subscription boundaries remain safe; #88 accepted before paid launch.
- [ ] Release documentation declares exactly which devices/flavors/languages have been physically verified and what remains unsupported.

**Explicit stop/go decision:** If ordinary alarms match or outperform WakeMyWay on real wake success, reliability or annoyance, reduce the conversational intervention and refine core behavior. Do not add features to compensate for missing proof.

## 18. Ownership and user-required evidence

| Owner | Required action |
|---|---|
| Engineering (follow-up PRs) | Build the accepted vertical slices; update tests, metrics, affected canonical docs and rollback notes; report separate completed/deferred work |
| Founder with real Android device | Run signed in-place alarm/Live overnight matrix, provide redacted traces, verify physical audibility and accessibility under normal hardware settings |
| Product/design reviewer | Approve W3 Stop Shape decision; review actual-content mockups and golden journey; ensure Stop dignity and Sunroom fidelity |
| Beta operator / founder | Recruit 10–20 qualified users, obtain consent, baseline, calibrations, annoyance and retention, retain unbiased fail reports |
| Backend/Play operator | Check GPT-Live entitlements, provider billing/caps, backend readiness, Play flavor differences and signed staged rollout; complete #88 controls |
| Product owner | Accept/decline freeze exceptions, establish launch thresholds from observed evidence and decide Go/No-Go; never infer these choices from CI green |

**Immediate next action after merging this plan:** start W0 (truthful baseline/evidence), collect W1 physical matrix and W2 conversation traces; make the W3 product decision only after seeing what actually happens when people reflexively Stop. All unresolved implementation behavior belongs in a follow-up Shape contract, not in a hidden AI coding decision.

---

**Primary references:** [CONTEXT](../../CONTEXT.md) · [1.0 program](../35-paid-launch-readiness.md) · [core quality reset](../45-core-experience-quality-reset.md) · [beta runbook](../36-beta-validation-runbook.md) · [physical acceptance](../37-physical-acceptance.md) · [voice hardening contract](2026-10-22-premium-voice-alarm.md) · [product workflow](../34-product-development-workflow.md) · [UX design](../07-design-system.md).

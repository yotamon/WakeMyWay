# WakeMyWay 1.0 paid-launch readiness

**Status:** Active launch program  
**Program issue:** #89  
**Last updated:** 2026-09-24  
**Target:** a trustworthy, polished, evidence-backed, supportable paid Android 1.0  
**Scope rule:** finish and prove the existing product before adding new product capabilities

## Why this plan exists

WakeMyWay has crossed the point where additional product scope is the main path to value. The current product already contains the core pieces needed to test the thesis: a local-first Alarm Kernel, independent alarms, durable Active Wake execution, deterministic Wake Runtime, branded local sounds, local Alfred speech and on-device replies, motion evidence, Tomorrow Contract behavior, real history/calibration, bounded local learning, consumer onboarding/Profile/Insights, and safe direct/Play update architecture.

The remaining question is:

> **Can a person who did not build WakeMyWay understand it, trust it overnight, get a better morning outcome from it, keep using it voluntarily and feel that the outcome is worth paying for?**

WakeMyWay 1.0 is therefore a **product-readiness program**, not a feature milestone.

## The 1.0 operating model

~~~text
            WAKE MY WAY 1.0

   TRUST  →  FINISH  →  PROVE  →  SELL
     │          │          │          │
 physical    product     users +    commerce +
 evidence    polish      outcomes    operations
~~~

| Gate | Canonical issue | Question | Launch consequence |
|---|---:|---|---|
| **1. TRUST** | #9 | Does the alarm behave reliably on real devices under hostile Android conditions? | Hard blocker |
| **2. FINISH** | #59 | Does the existing product feel coherent, understandable, accessible and release-grade? | Hard blocker |
| **3. PROVE** | #87 | Do target users achieve better mornings, return voluntarily and show real willingness to pay? | Hard blocker for paid launch |
| **4. SELL** | #88 | Can the proven product be purchased, operated, supported and updated safely? | Hard blocker for paid launch |

Program tracking lives in #89.

These gates are ordered by decision dependency. Work may overlap where safe, but a later gate cannot compensate for failure in an earlier one.

# Feature freeze

Until the 1.0 readiness gates are green, **new product capabilities are deferred by default**.

This is not a code freeze. It is a scope freeze.

## Allowed work

- Alarm Kernel / Active Wake reliability fixes.
- Defects, regressions and migration fixes.
- UX clarity, accessibility and visual convergence.
- Removal or simplification of confusing existing behavior.
- Performance, startup, APK/AAB size and release hardening.
- Privacy, security and compliance work.
- Privacy-safe measurement needed to validate the core promise.
- Bounded policy/copy tuning justified by beta evidence.
- Billing, entitlement, store, support and release infrastructure.
- Documentation and operational tooling required to support users.

## Deferred by default

- New characters.
- Additional sound/content packs beyond what accepted 1.0 needs.
- Health Connect.
- Calendar/weather expansion.
- iOS or Wear OS.
- Smart-home integrations.
- Trusted-person/social wake features.
- Broad Realtime/AI expansion.
- Extra Insights/dashboard scope.
- New themes/appearance systems.
- Gamification.
- Account/cloud capabilities not required for launch.
- New personalization controls that duplicate learning.

## Exception rule

A deferred capability may enter Build before 1.0 only when evidence demonstrates that WakeMyWay cannot safely deliver, validate or sell the existing core promise without it.

The exception must state the observed blocker, explain why a fix or simplification is insufficient, define the smallest coherent addition, preserve Alarm Kernel and WakeRuntime authority boundaries, and be explicitly shaped before implementation.

"No feature feels complete without it" is not sufficient evidence.

# The promise 1.0 must prove

WakeMyWay should not be positioned as an "AI alarm clock" or a list of alarm features.

The product job remains:

> **I intentionally chose this wake time because I genuinely want to get moving. Help me succeed with the minimum effective friction that works for me.**

The consumer promise to validate is:

> **WakeMyWay helps habitual snoozers actually get moving, then learns which wake strategy works best for them.**

That promise requires all three layers:

1. **Trust:** the alarm reliably starts and remains controllable.
2. **Outcome:** the user gets meaningfully active rather than satisfying the phone and returning to bed.
3. **Adaptation:** future strategy becomes more appropriate without becoming coercive.

A reliable alarm without the outcome is insufficient differentiation. An impressive adaptive experience without alarm trust is not a viable wake product.

# Gate 1 — TRUST

**Canonical issue:** #9

## Objective

Replace inferred reliability with repeatable physical evidence. Automated tests and emulator coverage remain required, but they cannot prove the full overnight Android environment.

## Required physical scenarios

The release envelope must contain retained real-device evidence for at least:

- repeated locked-screen T+2m cycles and ordinary overnight wakes;
- Doze / idle;
- process and service recreation;
- reboot / Direct Boot before unlock;
- timezone and manual clock changes;
- update/package replacement with durable alarm state preserved;
- microphone/on-device recognition loss after scheduling;
- exact-alarm capability loss before a planned wake;
- exact-alarm capability loss after Active Wake has started;
- notification/full-screen capability loss and repair;
- Stop followed by no stale resurrection;
- Snooze replacement durability;
- every bundled production sound;
- local audio + Voice Check-In coexistence;
- Bluetooth connected, disconnected and route changes;
- network/Realtime unavailable with local fallback;
- real-hardware motion calibration and false-positive behavior;
- compact-device presentation and controls.

## Reliability invariants

- No silent failure.
- Future scheduling readiness and active execution safety remain separate.
- Optional voice/cloud loss cannot silently delete a controllable scheduled wake.
- Stop remains immediate when safe.
- Snooze fails closed if an exact replacement cannot be committed.
- Stale occurrences never become active.
- A stale UI cannot stop a newer Active Wake.
- Process/service lifecycle churn cannot silently end a safely controllable wake.
- Reboot-before-unlock does not require private credential-protected data.
- In-place updates do not clear durable alarm product state.
- Intelligence, network, billing or telemetry failure cannot prevent the committed local alarm from ringing.

## Evidence contract

Representative evidence must identify device/Android build, WakeMyWay build, scenario, target time, receiver/audio/control timestamps, terminal action, recovery/fallback events and outcome.

"Worked on my phone" is not release evidence.

## Exit

Trust is accepted when #9 is complete enough to state a **supported reliability envelope** rather than a universal Android claim.

Paid public rollout remains blocked while critical overnight behavior is inferred only from automated/emulator coverage.

# Gate 2 — FINISH

**Canonical issue:** #59

## Objective

Make the product that already exists feel intentional from first launch through the next morning.

This is convergence, not redesign.

## Consumer journey under review

~~~text
Install
  ↓
Understand the promise
  ↓
Grant/repair necessary Android capabilities
  ↓
Create first alarm
  ↓
See truthful Wake Ready state
  ↓
Prepare for tomorrow if desired
  ↓
Sleep
  ↓
Active Wake
  ↓
Stop / Snooze safely
  ↓
Complete First Move
  ↓
Calibrate outcome later
  ↓
Understand what WakeMyWay learned
  ↓
Return for the next wake
~~~

## Product-polish acceptance

### Comprehension

- The first minute explains the outcome, not the architecture.
- The primary action is obvious on every consumer screen.
- Alarm setup does not feel like a settings panel.
- Wake Ready communicates truth without exposing developer complexity.
- Permission/capability repair explains what is required and why.
- Local/offline/account state is represented truthfully.
- No visible control promises functionality without real backing.

### Interaction

- Predictable navigation and Android Back behavior.
- No accidental destructive action.
- Stop/Snooze remains deliberate but accessible.
- Empty, loading, error and recovery states are authored.
- No stale state, duplicate action or dead-end navigation.
- System-settings round trips recover cleanly.
- Update UI never competes with wake safety.

### Visual convergence

- Canonical typography, spacing and hierarchy.
- Coherent Sunrise/Wake Line identity.
- No default utility-looking surface in the main consumer flow.
- Active Wake is the most carefully authored interaction in the product.
- Visual regression protects reviewed canonical states.

### Accessibility

- TalkBack traversal and labels.
- Dynamic type / large-font stress.
- Compact screens.
- Reduced motion.
- Contrast and touch targets.
- RTL/localization stress.
- Critical controls never depend on subtle color or motion alone.

### Release quality

- Release startup and normal navigation measured.
- Crash/ANR/performance regressions investigated.
- Download/install size measured and intentionally accepted.
- Bundled audio/resource cost reviewed.
- Safe shrinking/resource optimization enabled where appropriate.
- Critical local assets never move behind a network dependency.

The v0.2.2 direct APK release asset was roughly 112 MB. Treat this as a baseline to investigate, not an arbitrary failure threshold. Optimize only where sound quality and offline alarm reliability are preserved.

## Current FINISH evidence

Merged hardening now includes:

- large-text/compact accessibility for onboarding, alarm editor and critical Active Wake;
- explicit semantic roles/labels plus 48dp interaction targets on custom controls;
- reviewed RTL smoke evidence across the core consumer journey, with content-aware text direction and layout-aware navigation chevrons;
- reviewed canonical visual regression and compact responsive smoke coverage;
- measured release artifacts: **52.70 MiB Direct APK** and **34.53 MiB Play AAB**;
- release-only Play dependency correctness fixed by an actual release build rather than lint suppression;
- explicit Android cloud-backup/device-transfer exclusions for app-managed credential- and device-protected state;
- a release-derived Play Macrobenchmark contract for cold startup, with hosted CI limited to buildability rather than noisy timing assertions;
- consumer-surface audit confirming founder/debug tools are hidden behind `FLAG_DEBUGGABLE` and no account placeholder is exposed in release UI.

Remaining FINISH acceptance is intentionally physical rather than more feature work:

- **#97** — capture/review release startup timing on a consistent physical Android device;
- **#99** — physical TalkBack traversal and system Remove Animations acceptance;
- first-time/core-journey founder-free acceptance alongside the physical reliability work owned by #9.

## Exit

Finish is accepted when a first-time target user can complete the core journey without founder explanation, every visible consumer control is real, accessibility covers the critical flow, canonical states are visually protected, release size/performance are intentionally accepted, and polish has not widened feature scope.

# Gate 3 — PROVE

**Canonical issue:** #87

## Objective

Prove real target-user value before locking paid public launch.

WakeMyWay is not validated because its architecture is sophisticated, users say the idea is cool, or the Wake Runtime reaches its own internal completion condition.

## Initial cohort

Recruit **10–20 habitual snoozers** who genuinely want to wake at a chosen time. Prefer users who routinely snooze, dismiss reflexively, remain in bed after an alarm, or rely on another person for important mornings.

Avoid a cohort composed mainly of developers, AI enthusiasts or close collaborators who are unusually motivated to forgive product friction.

## Study shape

- Capture a practical normal-alarm baseline where possible.
- Target roughly 14–21 days of WakeMyWay use per participant where feasible.
- Require at least five intended wake mornings for inclusion in basic outcome summaries.
- Continue D30 observation when calendar time permits.
- Keep policy stable enough to interpret results.
- Safety Backup remains acceptable during early trust-building.
- Missing calibration remains unknown and is never silently counted as success.

## Evidence

### Reliability

- expected wake occurrences and diagnosable outcomes;
- alarm delivery;
- Active Wake survival/recovery;
- fallback frequency;
- user-reported missed/late wakes;
- Safety Backup usage.

### Real wake outcome

Keep separate:

**Activation Completion:** phone-observable runtime criterion reached.

**Confirmed Wake Success:** calibration indicates the intended real-world wake outcome rather than returning to bed.

Report:

- Activation Completion rate;
- Confirmed Wake Success with calibration denominator/coverage;
- Activation Completion → returned-to-bed rate;
- time to first engagement;
- time to meaningful movement;
- time to Activation Completion;
- snoozes per morning;
- success after snooze;
- intervention depth.

### Friction and retention

- morning annoyance;
- perceived agency;
- confusing moments;
- requests to disable interventions;
- D7;
- D14 for the initial beta;
- D30 when available.

Interpret retention in the context of wake frequency rather than like a social app.

### Commercial evidence

Do not rely only on "Would you pay for this?"

After users have experienced multiple mornings:

- ask what they would do if WakeMyWay disappeared;
- identify the alternative they would return to;
- test whether they value the outcome or only novelty;
- present a real Free/Pro package and price;
- observe trial/start-checkout behavior when available;
- test additional price hypotheses only when evidence justifies it.

## Beta instrumentation rule

Collect only privacy-safe semantic evidence required to answer the product questions. Do not upload raw microphone audio, full transcripts, Tomorrow Contract text, calendar content, complete prompts/model context, raw high-frequency motion or sensitive session replay.

## Beta change policy

~~~text
reliability defect   → fix
usability confusion  → simplify / clarify
accessibility defect → fix
policy too early/late→ bounded tuning
feature request      → defer unless core promise is blocked
one-off preference   → observe; do not generalize
~~~

## Exit

Prove is accepted when evidence is sufficient to decide paid launch with confidence about alarm trust, real wake outcome, voluntary continued use, acceptable annoyance/agency and willingness to pay.

This plan deliberately does not invent a universal percentage threshold before the first cohort exists. The first cohort establishes distributions and failure modes; launch thresholds are then locked in #87 from observed evidence.

# Gate 4 — SELL

**Canonical issue:** #88

## Objective

Make the proven core product purchasable, supportable, observable and safely releasable.

Commercial infrastructure remains outside Alarm Kernel and WakeRuntime authority.

## Packaging

Position paid value around user outcome, not an engineering checklist.

### Free

A trustworthy WakeMyWay alarm that helps the user begin moving.

The free product remains a real usable alarm. Subscription failure or expiry cannot retroactively make a committed alarm unsafe.

### Pro

The wake strategy becomes meaningfully more personal/adaptive over time, with premium experiences that support that promise.

Do not invent arbitrary feature walls merely to manufacture a subscription.

## Pricing

Current discovery hypothesis:

~~~text
€4.99 / month
~€39 / year
~~~

Do not lock launch pricing until Gate 3 yields real willingness-to-pay evidence.

## Billing / entitlement contract

The paid Play build must safely handle purchase, pending purchase, acknowledgement, entitlement refresh, restore, renewal, expiry/cancellation and reinstall/device replacement as applicable.

Non-negotiable:

> **Billing state must never become wake authority.**

If billing, account, network or verification is unavailable, an already-committed local wake remains safe and actionable.

Never show an upsell or purchase decision during Active Wake. Re-verify current Google Play billing requirements at implementation/submission time rather than treating project docs as policy authority.

## Store readiness

The Play listing should communicate the consumer story, not repository architecture:

~~~text
Set the wake you genuinely want
        ↓
WakeMyWay gets you moving
        ↓
Confirm what actually happened
        ↓
WakeMyWay learns for tomorrow
~~~

Required work:

- final icon and feature graphic;
- consumer-first short/long descriptions;
- screenshot narrative;
- content rating;
- supported-device declarations;
- testing tracks and staged rollout;
- pre-launch report review;
- release notes;
- verified AAB/signing/update path.

Avoid consumer copy centered on Alarm Kernel, Direct Boot, reducers, storage classes or model/provider terminology. Those are trust architecture, not the value proposition.

## Privacy / legal / policy

Before paid public launch:

- public consumer privacy policy;
- terms as appropriate to the commercial model;
- Google Play Data Safety declaration;
- processor/provider list;
- retention policy;
- user-visible reset/delete behavior for data actually stored;
- cloud export/delete only for cloud/account data that actually exists;
- permission disclosures matching shipped behavior;
- support/privacy contact;
- accurate subscription disclosure.

Do not market WakeMyWay as medically verifying wakefulness, diagnosing sleep problems or treating a sleep disorder.

Immediately before Play submission, re-verify current target-SDK, exact-alarm, foreground/full-screen, billing/subscription and Data Safety/privacy requirements.

## Support

Minimum support surface:

- public support contact;
- concise FAQ/help;
- Wake Ready / permission troubleshooting;
- evidence-backed OEM battery guidance;
- Bluetooth/audio routing help;
- Snooze behavior;
- updates;
- subscription purchase/restore/cancel help;
- privacy/data controls.

For "my alarm did not behave as expected", use the existing privacy-safe reliability journal/report instead of requesting sensitive morning audio or context.

## Production observability

Minimum categories:

- crashes and ANRs;
- severe startup failures;
- semantic alarm lifecycle failures/fallbacks and sanitized latency;
- wake/session started;
- engagement/movement/Activation Completion;
- calibration submitted/result;
- retention summaries;
- paywall viewed;
- trial/purchase initiated;
- purchase success/failure category;
- entitlement restored;
- conversion/subscription summaries.

No broad autocapture or session replay by default.

## Release optimization

Measure before optimizing. Investigate audio encoding/packaging, duplicate resources, safe R8/minification, resource shrinking, Play AAB split delivery, unnecessary release dependencies and startup/performance regressions.

Do not trade away offline alarm assets or wake reliability for a smaller download.

## Launch runbook

Before paid public rollout document:

- production signing;
- version/versionCode;
- Play Console products/tracks;
- billing products;
- privacy/legal URLs;
- support route;
- observability dashboards;
- release notes;
- staged rollout;
- rollback/hotfix path;
- update compatibility;
- known reliability envelope;
- first-week incident triage;
- criteria to pause rollout.

## Exit

Sell is accepted when a stranger can discover, install, understand, configure, wake, recover from common problems, understand Free vs Pro, purchase, restore and manage/cancel without founder assistance, while commercial/cloud failure cannot weaken a committed wake.

# Rollout ladder

## Stage A — Founder reliability

Finish #9, reproduce hostile conditions, stabilize release/update paths.

Exit: critical failure modes are diagnosable and the supported founder-device envelope is explicit.

## Stage B — Trusted alpha

Small number of known target users.

Purpose: test onboarding/setup/wake comprehension, catch severe device-specific issues, validate privacy-safe measurement and keep Safety Backup acceptable.

Exit: no unresolved critical product-safety pattern; core journey works without live founder coaching.

## Stage C — Closed beta

10–20 qualified target users, expanding only when evidence supports it.

Purpose: execute #87, observe outcomes/retention, fix defects/confusion, test packaging/price.

Exit: product-value and willingness-to-pay evidence is sufficient for paid rollout; important beta failure modes are fixed or explicitly accepted.

## Stage D — Paid staged rollout

Limited public exposure appropriate to geography/device envelope.

Monitor crash/ANR, wake reliability, support and subscription signals. Pause rollout for credible critical alarm regressions, unsafe/deceptive commerce behavior, inaccurate privacy/legal configuration, or a material device/OEM reliability pattern.

## Stage E — Broader 1.0 availability

Expand only after staged evidence confirms that product and operations behave as expected.

# Readiness scoreboard

| Area | Current state | 1.0 requirement |
|---|---|---|
| Core product capability | Strong / implemented | Scope remains frozen |
| Automated reliability | Strong | Remains green |
| Physical overnight reliability | Open | #9 accepted |
| Consumer visual/UX convergence | Automated convergence strong; physical acceptance open | #59 accepted |
| Accessibility | Large text/touch targets/RTL automated + reviewed; physical TalkBack/reduced-motion pass open (#99) | Critical flow accepted |
| Release size/performance | Size measured/accepted; release startup benchmark contract merged; physical timing open (#97) | Intentionally accepted |
| Target-user evidence | Not yet sufficient | #87 accepted |
| Retention evidence | Not yet sufficient | D7/D14 known; D30 follows |
| Willingness to pay | Hypothesis | Real package/price evidence |
| Billing/entitlement | Not implemented | Full safe lifecycle |
| Store / Play operations | Not launch-complete | Complete and reviewed |
| Privacy/legal package | Internal architecture strong; Android OS backup/D2D policy explicit; public launch package still open | Public and truthful |
| Production crash/ANR telemetry | Direction documented | Live and privacy-safe |
| Semantic product measurement | Domain-defined/local | Sufficient for beta/launch |
| Support operation | Founder-driven | Public support path |
| Signed release/update path | Direct + Play architecture and update-persistence contract exist | Public Play path exercised |
| Incident/rollback plan | Partial | Explicit launch runbook |

# Work ordering and WIP

Recommended order:

~~~text
1. Physical reliability evidence (#9)
   + product convergence (#59)

2. Trusted alpha
   ↓ fix only blockers / confusion / reliability

3. Closed target-user beta (#87)
   ↓ measure / learn / bounded tune

4. Lock 1.0 packaging and tested price

5. Complete billing / store / legal / support / observability (#88)

6. Paid staged rollout
   ↓ monitor / pause / fix if needed

7. WakeMyWay 1.0 broader availability
~~~

Low-risk Gate 4 preparation such as drafting store copy, legal inventory, support taxonomy and Play Console setup may happen before Gate 3 ends.

Do **not** lock launch price, manufacture Pro-only features or optimize conversion before there is evidence that target users value the outcome.

# PR acceptance during the freeze

Every product-facing PR before 1.0 should answer:

1. Which readiness gate does this advance?
2. What existing user problem/failure does it resolve?
3. Is it a fix, hardening, tuning or launch requirement rather than a new capability?
4. What evidence proves the change?
5. Could the scope be smaller?
6. Does it preserve Alarm Kernel and WakeRuntime authority?
7. Does it change the public promise, privacy posture or paid packaging?
8. If it adds scope, what observed 1.0 blocker makes the exception necessary?

If a PR cannot answer #1, it probably waits until after 1.0.

# Definition of WakeMyWay 1.0

## Trust

- [ ] #9 physical reliability gate accepted.
- [ ] Supported device/reliability envelope explicit.
- [ ] Critical regressions diagnosable.
- [ ] Update compatibility supported by physical/automated evidence.

## Finish

- [ ] #59 convergence gate accepted.
- [ ] First-time journey works without founder coaching.
- [ ] Critical accessibility states accepted.
- [ ] Release size/performance measured and accepted.
- [ ] No placeholder/developer surface in normal consumer flows.

## Prove

- [ ] #87 has meaningful target-user evidence.
- [ ] Reliability and outcome denominators known.
- [ ] D7/D14 retention known for the initial cohort.
- [ ] Confirmed Wake Success reported separately from Activation Completion.
- [ ] Important annoyance/agency signals known.
- [ ] Real willingness-to-pay evidence exists for launch packaging/pricing.

## Sell

- [ ] #88 accepted.
- [ ] Purchase/restore/expiry/cancel lifecycle safe.
- [ ] Billing/network failure cannot weaken committed wake behavior.
- [ ] Store listing consumer-first and truthful.
- [ ] Privacy/legal/Data Safety/support complete.
- [ ] Production crash/ANR and semantic observability live.
- [ ] Signed release/update paths exercised.
- [ ] Staged rollout, rollback and incident handling documented.

## Final product test

A target user who has never spoken to the founder can:

> discover WakeMyWay, understand why it is different, trust it overnight, get through the morning flow, understand what happened, return voluntarily, choose whether the paid value is worth it, manage that purchase, and recover from ordinary problems without founder intervention.

If that statement is not true, 1.0 is not finished.

If it is true, adding another feature is not required to call the product complete.

# After 1.0

The feature freeze ends only after the launch program is accepted.

Post-1.0 opportunities return to Explore → Shape → Build → Harden → Learn. Re-evaluate the backlog from launch evidence rather than automatically resuming the pre-launch wish list.

The first post-launch planning cycle should ask:

- Which 1.0 behavior produces the strongest measured wake outcome?
- Where do retained users still struggle?
- Which Pro value actually drives retention rather than initial purchase?
- What device/support problems are most expensive?
- Which deferred capability now has evidence strong enough to earn complexity?
- What should be removed or simplified before anything new is added?

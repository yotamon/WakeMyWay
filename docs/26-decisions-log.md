# Decisions log

This is the chronological product/engineering decision history. ADRs contain deeper technical rationale. Canonical terminology lives in [`../CONTEXT.md`](../CONTEXT.md).

## 2026-09-09 — Product concept

Build an alarm that does more than ring: it can talk, use relevant context, remind the user why they wanted to get up, and vary personality.

## 2026-09-09 — Product differentiation

"AI talking alarm" is insufficient differentiation. Core differentiation is an adaptive wake strategy that learns what actually helps the individual become active.

## 2026-09-09 — Minimum effective friction

Combine voice, context, movement, snooze policy, and escalation while optimizing for the least aggressive intervention that reliably works.

## 2026-09-09 — Tomorrow Contract

Night-before intention is a core product mechanic: past-you can give morning-you a reason and a First Move.

## 2026-09-09 — Deterministic behavior

Wake behavior is deterministic. AI renders constrained language but never owns behavioral state.

## 2026-09-09 — Platform scope

Initial Android+iOS thought was narrowed to **Android only now**, with a future iOS path preserved conceptually.

## 2026-09-09 — Native Android

Use Kotlin + Jetpack Compose, not React Native/Flutter, because alarm scheduling, presentation, audio, microphone lifecycle, sensors, and permissions are product-critical.

## 2026-09-09 — KMP deferred

Do not use KMP now. Reassess if/when iOS is real and shared pure-domain code has measurable value.

## 2026-09-09 — Local-first alarm authority

Alarm delivery is independent of cloud/AI. Use exact Android wake primitives, local state, local fallback, and bundled emergency audio.

## 2026-09-09 — Motion scope

Begin with pickup/orientation/movement. Do not request step/activity-recognition permission until evidence shows it adds value.

## 2026-09-09 — UX consciousness model

Design around Asleep → Emerging → Engaged → Active → Oriented. Information density increases with likely wakefulness.

## 2026-09-09 — Snooze

Snooze is permitted and intentional. Learn whether it helps or predicts delay for the individual rather than moralizing it.

## 2026-09-09 — Character scope

Product direction includes Alfred, Sam, and Chaos; implementation begins with Alfred only.

## 2026-09-09 — Optional account

First alarm must work without sign-up.

## 2026-09-09 — Brand

Final current name: **Wake My Way**. Short mark: **WMW**.

Primary line: **Wake up your way.**  
Product explanation: **An alarm that learns what works for you.**

`wakemyway.com` returned available in a live Namecheap lookup; this does not imply registration.

## 2026-09-09 — Backend direction

TypeScript + Fastify + PostgreSQL + Kysely + OpenAPI is the preferred future direction. Do not scaffold it before a concrete cloud capability needs it.

## 2026-09-09 — Supabase data platform

Originally selected Supabase as the future data platform. Superseded on 2026-09-26 by ADR-013: Neon now provides PostgreSQL and Managed Better Auth while Vercel remains compute/API hosting. Android domain behavior still does not bind directly to provider tables, and no cloud outage may prevent the current local wake attempt.

## 2026-09-09 — Voice provider

No provider lock before a measured spike. Direct OpenAI realtime is the first likely prototype; LiveKit/ElevenLabs roles are evaluated with measurements.

## 2026-09-09 — Privacy/analytics

Manual semantic analytics only. Do not retain raw audio by default or send private morning content to telemetry.

# Pre-implementation architecture review — 2026-09-09

## Canonical `CONTEXT.md`

Create root `CONTEXT.md` as the single canonical domain vocabulary/invariant source. Remove duplicate glossary ownership.

## Deep Alarm Kernel

Treat the Alarm Kernel as one deep reliability capability. Its public contract hides persistence/snapshot/OS-registration ordering, snooze replacement, reconciliation, readiness, and eventually active alarm execution/recovery.

## Minimal physical modules

M0 begins with only `:app`, `:wake-core`, and `:benchmark`. Do not pre-split feature/platform/data/testkit modules.

## Manual DI first

Do not add Hilt in M0 without object-graph pressure. Prefer explicit constructor composition first; add DI framework only when it demonstrably improves locality/maintenance.

## One active Wake Schedule in V1

V1 supports one active Wake Schedule, potentially with weekday-specific times, producing one next Wake Occurrence. Multiple independent adaptive alarm definitions are deferred.

## Simplified Wake Runtime phases

Canonical phases are Alerting, Engaging, Activating, Orienting, Finished. Escalation, fallback, movement requests, and First Move are not phases.

## Remove "Verified Awake" from domain language

The phone observes behavioral activation; it does not medically verify consciousness.

## Activation evidence is internal to Wake Runtime

Do not expose a standalone WakeConfidenceEstimator/StrategyPolicy architecture prematurely. Wake Runtime owns in-session evidence/escalation. Wake Learning owns off-session policy derivation.

## Direct Boot critical state

Critical Wake Snapshot is minimal/non-sensitive and stored in device-protected storage. Sensitive personalized/contextual content remains credential-protected; pre-unlock wake falls back to generic safe local audio.

## Honest Force Stop reliability envelope

Explicit Force Stop can invalidate pending delivery on modern Android. Treat this as a platform limitation and reconcile on next user start; do not market an impossible guarantee.

## Backend/voice seams are earned

Do not create generic provider interfaces or backend services before a real external boundary exists. Realtime voice transport is selected only by a measured spike.

## Testing and deployment topology

Testing is layered by evidence type: pure Kotlin contracts → Wake Lab simulation/replay → Android instrumentation → Firebase Test Lab → Google Play Internal Testing → real overnight dogfood. Real-device mornings remain the final validation of both reliability and product effectiveness.

Vercel is the preferred initial deployment platform for future non-critical web/API workloads once a real backend capability exists. It is never part of the Alarm Kernel critical path.

# Plan hardening review — 2026-09-09

## Active Wake Execution is part of Alarm Kernel reliability

Scheduling reliability is not enough. Once a Wake Occurrence fires, critical alarm audio and controls must survive ordinary `WakeActivity` recreation/process churn where Android allows.

Decision:

- Alarm Kernel owns an **Active Wake Execution** lifecycle
- `WakeActivity` is presentation, not critical playback lifetime authority
- initial implementation direction uses an alarm-appropriate foreground playback service/controller with `USAGE_ALARM` semantics
- duplicate starts/recreation must converge on one active occurrence
- stopped or durably snoozed occurrences must never resurrect
- power/wake-lock behavior, if needed, remains private Alarm Kernel implementation

See ADR-014.

## Current Android SDK baseline

As of 2026-09-09, Google Play requires new phone/tablet app submissions and app updates to target Android 16 / API 36 or higher.

Decision for M0:

```text
targetSdk = 36 baseline
compileSdk = 36+ current stable toolchain
minSdk = decide during M0 from supported-device/reliability evidence
```

Revalidate before future target-SDK upgrades and submission.

## Exact-alarm manifest direction

WMW is a dedicated alarm-clock app whose core user-facing functionality requires precise timing.

Decision: begin implementation with **`USE_EXACT_ALARM` as the preferred manifest strategy**, subject to current Google Play restricted-permission eligibility/review and implementation-time revalidation. Switch to `SCHEDULE_EXACT_ALARM` or another path only if current policy/platform evidence requires it, and record the reversal.

## Wake Learning moves before realtime voice

The adaptive learning loop is the core moat; realtime conversation is presentation richness.

Decision:

```text
M7 = deterministic local Wake Learning v0
M8 = realtime voice architecture spike
M9 = realtime conversation
M10 = useful context
```

M7 must prove at least one bounded, explainable, reversible policy adaptation before realtime provider complexity becomes central.

Wake Learning v0 is local/offline and requires no backend or ML.

## Activation Completion vs Confirmed Wake Success

The runtime's own activation threshold must not become circular proof that the product succeeded.

Decision:

- **Activation Completion** = phone-observable runtime criterion reached
- **Confirmed Wake Success** = calibrated evidence that the user actually achieved the intended wake result
- product-level Wake Success should use confirmed calibration where available
- missing feedback does not become automatic confirmation
- Wake Learning must not optimize Activation Completion alone if calibration shows return-to-bed false positives

## Intentional Stop

The user always retains an accessible, local, non-AI-dependent way to stop an active alarm.

Stop may be made deliberate rather than the largest reflex target, but must never become a puzzle, hidden control, coercive trap, or network-dependent action. Kernel stop is idempotent and terminal for that occurrence.

## Safety Backup as trust-transition hypothesis

During founder/trusted dogfood, WMW may optionally allow a later conventional Safety Backup alarm while trust is being earned.

It is not a second adaptive Wake Schedule and must not pull generic multi-alarm coordination into V1 architecture. Track whether users stop needing it and keep WMW's own reliability metrics independent from the backup.

## Voice/backend numbering amendment

ADR-008 and ADR-012 are amended: the measured realtime voice decision now occurs in **M8**, after M7 Wake Learning v0.

# Product design foundation — 2026-09-10

## Adaptive Dawn is the canonical visual direction

Wake My Way should feel like a premium bedside object that wakes with the user rather than a generic alarm utility, chatbot, wellness product or SaaS dashboard.

The visual experience follows the existing UX consciousness model with progressively increasing definition and information density:

```text
Night → Emerging → Engaged → Active → Oriented → Morning
```

This remains a presentation model. It does not create a second behavioral state machine and does not rename canonical Wake Runtime phases.

The signature visual language is based on darkness-to-warmth, restrained geometry, tactile surfaces, excellent typography and calm motion. Purple/blue AI gradients, glowing AI orbs, robots, cartoon alarm clocks, generic sunrise art and engagement-style gamification remain explicitly excluded.

See [`implementation/product-design-foundation.md`](implementation/product-design-foundation.md).

## Product home becomes Tonight

The normal application entry surface becomes a focused **Tonight** experience centered on the next wake time, character presence and Wake Ready reassurance.

The founder Wake Alarm Lab remains available as an explicit developer destination rather than defining the consumer-facing home.

## Wake visual richness cannot become wake authority

`WakeActivity` may become visually rich and state-responsive, but it remains presentation only.

- Alarm Kernel remains critical playback/Stop/Snooze authority.
- Wake Runtime remains behavioral authority.
- Direct Boot and private prepared-content gating remain unchanged.
- animation/shape/gradient failure must degrade visually without preventing a locally actionable alarm.

## Navigation 3 adoption is now earned

The normal app now has real destination complexity: Tonight plus preserved founder/developer surfaces, with setup/history/settings to follow.

Decision: adopt stable Navigation 3 for normal Compose navigation while keeping dedicated `WakeActivity` outside the navigation graph. Use only the minimal runtime/UI artifacts initially.

## Native WMW Presence over animation-framework identity

Adopt AndroidX `graphics-shapes` for a restrained, local, geometric WMW Presence that can morph with presentation state.

Do not adopt Rive, Lottie or an AI-orb metaphor as the default identity system. Native Compose state-driven animation remains the primary motion architecture. Re-evaluate authored-animation runtimes only for a concrete future capability gap.

## Visual regression follows canonical previews

Roborazzi is the preferred visual-regression tool, but goldens are introduced only after canonical synthetic previews stabilize enough to represent reviewed design truth.

The first foundation slice therefore creates canonical previews first; the next design gate records reviewed baselines and protects selected surfaces in CI. Private wake data is never used in visual fixtures.

# Brand identity refinement — 2026-09-14

## WakeMyWay is the canonical consumer wordmark

Consumer-facing naming is rendered as **WakeMyWay** without spaces. `WMW` remains a valid internal shorthand. Earlier documentation that spells the product as “Wake My Way” is historical context rather than the current consumer wordmark.

Primary brand line: **Brighter mornings. Your way.**

## Sunrise + Wake Line is the canonical symbol

The primary symbol is intentionally simple: a rising sun meeting one continuous horizon that reads simultaneously as a calm audio waveform and a mountain landscape.

The same geometry becomes the in-product **Wake Line**. It changes amplitude with real wake state, so brand identity and wake interaction share one visual language rather than relying on repeated logo stamping.

The mark must remain legible at Android launcher-icon size and has a monochrome themed-icon variant. Alarm-clock bells, rays, robot/AI symbols, generic equalizers and decorative complexity are excluded from the core mark.

## Brand palette and product worlds

Canonical palette:

- Midnight `#08142F`
- Deep Navy `#10264C`
- Sunrise `#FF9F6D`
- Sunrise Soft `#FFB88F`
- Golden Light `#FFD699`
- Dawn `#A5B4FC`
- Dawn Deep `#7188E8`
- Cloud `#F8F7F4`
- Paper `#FFFCF8`

Planning/setup surfaces use a light Cloud/Paper world with Midnight typography and Sunrise actions. Active wake remains intentionally dark and sparse, revealing warmer sunrise definition as the wake progresses. Oriented/completed states return to a light morning surface.

## Earlier visual exploration is superseded, architecture is not

The 2026-09-10 Adaptive Dawn principle remains valid as the night-to-morning presentation model, but the earlier serif-heavy / abstract Presence execution is superseded by the cleaner sunrise-wave identity.

This is a presentation-system decision only. Alarm Kernel ownership, Wake Runtime determinism, Wake Ready truth, Direct Boot privacy, local Stop/Snooze authority and optional cloud enrichment boundaries remain unchanged.

Canonical implementation guidance lives in [`07-design-system.md`](07-design-system.md).

# App update distribution — 2026-09-19

## One update experience, two Android distribution paths

WakeMyWay keeps one consumer update experience while using two provider implementations: verified direct APK updates for founder/trusted-device distribution and Google Play In-App Updates for Play builds.

Direct-only install permissions are isolated to the `direct` product flavor. The `play` flavor uses the official Play update API and does not ship self-install capability.

Update installation/restart is never wake authority and is deferred during an Active Wake Execution or when the next Wake Occurrence is less than 90 minutes away. Existing `MY_PACKAGE_REPLACED` Alarm Kernel reconciliation remains the post-update scheduling recovery mechanism.

Canonical technical detail lives in ADR-023.

# Package-update persistence contract — 2026-09-19

WakeMyWay treats in-place package replacement as a durable-data compatibility boundary. Saved alarms, consumer preferences, Wake history, Wake Learning and Alarm Kernel Direct Boot state must survive updates under the stable package/signing identity.

AlarmDefinition schema-v1 is now an explicit historical decoder branch rather than an implicit current-only format. Relevant persistence/alarm/update changes are protected by a real API 36 upgrade test that seeds durable state in a baseline APK, installs the candidate with `adb install -r`, and verifies state from the candidate without uninstalling or clearing data.

Canonical technical detail lives in ADR-024.


# WakeMyWay 1.0 readiness and scope freeze — 2026-09-23

## Finish and prove before expanding

WakeMyWay's existing capability set is sufficient to test the core product thesis. The active product program is therefore no longer feature expansion; it is paid-launch readiness.

Canonical sequence:

~~~text
TRUST → FINISH → PROVE → SELL
~~~

- TRUST is physical reliability proof under #9.
- FINISH is consumer convergence/accessibility/release hardening under #59.
- PROVE is target-user outcome, retention and willingness-to-pay evidence under #87.
- SELL is billing/store/legal/support/observability and staged release under #88.
- Program tracking is #89.
- The canonical operating plan is [`35-paid-launch-readiness.md`](35-paid-launch-readiness.md).

## 1.0 feature freeze

Until the readiness program is accepted, new product capabilities are deferred by default.

Allowed work includes reliability fixes, defects, simplification, accessibility, visual convergence, release/performance hardening, privacy/security/compliance, privacy-safe measurement, bounded beta-driven tuning and commercial/release infrastructure.

A new capability may enter Build only when observed evidence shows that 1.0 cannot safely deliver, validate or sell the existing core promise without it. The exception must be explicitly shaped rather than silently added during implementation.

The freeze ends after the 1.0 launch program is accepted. Deferred opportunities then return to the normal Explore → Shape → Build → Harden → Learn workflow and are re-evaluated from real launch evidence.

## Outcome-led monetization

Free/Pro packaging should communicate user value rather than implementation details.

Free remains a legitimate reliable WakeMyWay alarm. Pro should represent meaningfully deeper personalization/adaptation and premium experiences that support the same wake outcome.

The existing €4.99/month and ~€39/year values remain discovery hypotheses until real willingness-to-pay evidence exists. Billing or entitlement state can never become alarm authority.

# Android home widget Shape contract - 2026-09-25

## One responsive Next Wake surface

WakeMyWay should eventually expose one responsive Android home-screen widget centered on the next wake, its Wake Ready truth and one contextually useful safe action.

The widget is a projection of existing local product state, not a mini application and not a second scheduling/readiness model.

## Alarm authority remains unchanged

All alarm mutations initiated from the widget must delegate to the existing product mutation path and Alarm Kernel. The widget may never directly persist alarm intent and assume Android scheduling succeeded.

A failed mutation or readiness repair must refresh to authoritative truth rather than preserve optimistic UI.

## Active wake controls remain out of the widget

The home widget may expose **Open Wake** while an Active Wake Execution exists.

It must not expose Stop or Snooze. Terminal wake behavior remains owned by the dedicated Wake surface/notification and Alarm Kernel path.

## Widget privacy is minimized

The widget is home-screen only and does not display Tomorrow Contract free text, Prepared Wake Plan text, First Move, transcript content or private history.

It may display non-sensitive derived state such as next wake time/date, Wake Ready, preparation-ready status, pending Morning Check-In and a bounded upcoming-alarm summary.

## Responsive density, stable purpose

Compact, Medium and Expanded sizes progressively reveal more information while preserving the same hierarchy. The preferred default is a medium hero surface; expanded space may add bounded upcoming-alarm controls without becoming a second Alarms screen.

Avoid minute-level countdowns and background polling. Prefer event-driven updates and display copy that remains truthful between refreshes.

## Build remains gated by the 1.0 feature freeze

The Shape contract is complete enough for future Build, but production implementation remains deferred until the paid-launch feature freeze ends or evidence explicitly earns an exception.

Canonical detail lives in [`41-android-home-widget.md`](41-android-home-widget.md).

# Android home widget Build exception and implementation - 2026-09-25

## Explicit feature-freeze exception

After the home-widget Shape contract was completed, the product owner explicitly requested production implementation despite the active 1.0 feature freeze.

This is recorded as a deliberate exception rather than silently redefining the freeze. The widget remains outside alarm authority and does not become a prerequisite for WakeMyWay's core wake promise.

## Runtime sizing

Use stable Jetpack Glance 1.2.0.

Runtime rendering uses `SizeMode.Exact` so the actual launcher-provided bounds are authoritative. The widget then maps those bounds into three deliberate product densities: Compact, Medium and Expanded.

Android 15+ generated previews use `previewSizeMode = SizeMode.Responsive` with representative sizes. A static picker preview remains the fallback for older/platform-limited launchers.

## No inline alarm toggle in the first implementation

The Expanded alarm summary is one large interaction target that opens the existing Alarms surface rather than exposing small per-row controls or toggling enabled state directly.

Reason: safe enablement requires the existing product capability/voice preflight and Alarm Kernel commit semantics. A convenience toggle does not justify duplicating or bypassing that flow.

## Event-driven projection

The widget has no independent scheduling/readiness state and no periodic polling loop.

Alarm, preparation, appearance, history, reconciliation and Active Wake mutation paths request a widget refresh. The renderer rebuilds a privacy-minimized projection from authoritative local owners.

## Morning Check-In reuses the existing mutation

The widget's Yes / Not really actions call the same canonical Morning Safety Check calibration function used by notification follow-up. The launcher surface does not create a second feedback model.

## Active Wake remains protected

When an Active Wake Execution exists, tapping the widget re-enters the authoritative Wake surface. Stop and Snooze remain absent from the home widget.

Canonical implementation and validation detail lives in [`41-android-home-widget.md`](41-android-home-widget.md).

# Manual one-click Android releases - 2026-09-25

## PR merge and release are separate operations

Merging a feature/fix PR to `main` never publishes an app version.

Production release happens only through an explicit manual dispatch of **Release WakeMyWay** from current `main`.

## Production version authority

The release workflow derives semantic version and `versionCode` from the latest stable GitHub Release / `update.json`.

`apps/android/version.properties` remains a local/default developer fallback and is overridden for production builds through `WMW_VERSION_CODE` / `WMW_VERSION_NAME`.

This removes release-only version PRs from normal operations.

## Release transaction

The manual workflow owns the full transaction: plan → quality → device reliability → upgrade persistence → release build → production signing → metadata/certificate/SHA verification → draft upload → draft verification → public release → public endpoint verification.

No tag or public GitHub Release is created before the signed candidate passes all pre-publication gates.

## Signing custody

Direct app-signing and Play upload signing identities remain separate and are stored as encrypted GitHub Actions Secrets. Signing material is never committed.

The local DPAPI bundle is retained only as an offline recovery path.

## Dry-run

The workflow exposes an optional `dry_run` mode that executes through production signing and candidate verification but creates no tag or GitHub Release. Normal releases leave it disabled.

Canonical procedure: [`42-android-release-operations.md`](42-android-release-operations.md).
## 2026-09-26 — Realtime-or-alarm voice fallback

Consumer wake speech is now intentionally binary in production: use the approved Realtime conversation when it is ready, otherwise keep the selected local alarm sound with Stop/Snooze. Do not substitute generic/local Android TTS when Realtime is unavailable, late, or fails during a wake. This preserves the quality bar of the conversational product while keeping alarm reliability fully local.


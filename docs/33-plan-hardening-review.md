# Plan hardening review — 2026-09-09

> **Historical review.** This document records the final pre-M0 risk review and the build order chosen at that time. Later accepted ADRs and current implementation may supersede individual assumptions. In particular, the single-schedule assumption was superseded by [`adr/022-multi-alarm-product-model.md`](adr/022-multi-alarm-product-model.md). Use [`../CONTEXT.md`](../CONTEXT.md) and [`00-project-status.md`](00-project-status.md) for current truth.

## Purpose

After the deep-module architecture review simplified Wake My Way, the full plan received one final pre-implementation review focused on **product-risk ordering and real Android lifecycle gaps** rather than abstraction quality alone.

The question was:

> If we begin M0 now, what remaining assumptions could cause us to build the wrong product sequence or discover a trust-critical Android failure too late?

Four primary issues were identified and corrected, plus two UX trust hypotheses were made explicit.

## 1. Scheduling reliability did not yet fully define active alarm reliability

### Gap

The plan correctly specified:

```text
Wake Schedule
→ durable occurrence
→ Critical Wake Snapshot
→ AlarmManager.setAlarmClock()
→ receiver
→ safe audio
```

But it did not explicitly identify the lifecycle owner after the alarm had fired.

A naive implementation could therefore make `WakeActivity` own critical playback. That would make Activity crash/recreation/process churn capable of silencing an otherwise correctly delivered alarm.

### Change

Added **Active Wake Execution** to canonical domain/reliability language and accepted ADR-014.

The Alarm Kernel now owns:

```text
scheduled occurrence
→ trigger
→ foreground active alarm execution
→ safe local USAGE_ALARM playback
→ accessible controls
→ stop / durable snooze / completion
```

`WakeActivity` is presentation, not critical playback lifetime authority.

M1/M2 now include explicit recreation/idempotency cases:

- Activity recreation while sounding
- playback-owner recreation
- controlled process recreation where Android permits
- duplicate playback suppression
- Stop then recreation must not resurrect
- Snooze then recreation must not resurrect the old occurrence
- stale trigger identity cannot revive an old occurrence

## 2. Wake Learning was too late relative to the product moat

### Gap

The core differentiation is not "AI talks during an alarm." It is:

> WMW learns the minimum effective friction that works for this person.

Yet the old roadmap placed realtime voice before Wake Learning. That risked spending substantial time on RTC/provider quality before proving that the adaptive loop itself improved mornings.

### Change

Roadmap reordered:

```text
M6 Tomorrow Contract
M7 Wake Learning v0
M8 realtime voice architecture spike
M9 realtime conversation
M10 useful context
```

M7 is deliberately:

- local
- deterministic
- explainable
- bounded
- reversible
- versioned
- no ML
- no backend/network requirement

M7 exit criterion requires at least one useful bounded policy adaptation from prior outcomes/feedback.

## 3. Wake Success could become circular

### Gap

The old north-star wording effectively treated reaching the Wake Runtime's own activation threshold as Wake Success.

That creates an optimization trap:

```text
runtime decides activation threshold
→ threshold is reached
→ runtime metric says success
```

The user might still put the phone down and return to bed.

### Change

Canonical outcome model now distinguishes:

### Activation Completion

Phone-observable deterministic runtime criterion reached.

### Confirmed Wake Success

Calibrated evidence indicates the user actually achieved the intended real-world wake result.

Early calibration may come from occasional later feedback:

```text
GOT_UP
RETURNED_TO_BED
GOT_UP_LATER
SKIPPED
```

Missing calibration remains unknown, not automatic success.

Wake Learning v0 must not optimize Activation Completion alone when calibration shows false-positive return-to-bed cases.

## 4. Current Android baseline was more decidable than the plan implied

### Target SDK

Current Google Play requirements as of 2026-09-09 require new phone/tablet apps and updates submitted after 2026-08-31 to target Android 16 / API 36 or higher.

M0 baseline is now:

```text
targetSdk = 36
compileSdk = 36+ using current stable toolchain
minSdk = deliberate M0 decision
```

### Exact alarms

WMW is a dedicated alarm-clock app whose core user-facing functionality requires precise timing.

The preferred implementation direction is therefore:

```text
USE_EXACT_ALARM
+
AlarmManager.setAlarmClock()
```

Google Play restricted-permission eligibility must still be revalidated at implementation/submission time. If policy evidence requires another path, record the reversal.

### Android 17

API 37 is not the initial target baseline, but Android 17 background-audio hardening is now explicitly included in M2 compatibility testing.

This reinforces the need for foreground Active Wake Execution and `USAGE_ALARM` semantics.

## 5. Intentional Stop is now explicit

A wake product for habitual snoozers should not make Stop the largest automatic reflex target, but user agency requires an accessible exit.

Decision:

- Stop always exists
- it is local and non-AI-dependent
- it may be made intentionally deliberate through lightweight UX
- it cannot become a puzzle, hidden affordance, dark pattern, or coercive trap
- kernel Stop is terminal/idempotent and cannot resurrect after recreation

The exact one-tap/confirmation/gesture UX remains a dogfood/accessibility question.

## 6. Safety Backup is now historical context, not multi-alarm architecture

Target users often arrive from a habit of setting several backup alarms. Asking them to instantly trust one new adaptive alarm can itself be a migration barrier.

At the time of this review, WMW considered an optional later conventional Safety Backup during founder/trusted dogfood.

That historical hypothesis did not define the later multi-alarm product model. ADR-022 introduced independent WakeMyWay Alarm Definitions and a multi-slot kernel deliberately, while preserving exactly one physical Active Wake Execution authority.

## Resulting historical build order

```text
M0  Foundation
 │
M1  Deep Alarm Kernel + Active Wake Execution
 │
M2  Reliability / Direct Boot / active recovery
 │
M3  Wake Runtime
 │
M4  Motion evidence
 │
M5  Alfred local experience
 │
M6  Tomorrow Contract / preparation
 │
M7  Wake Learning v0
 │
M8  Realtime voice architecture spike
 │
M9  Realtime conversation
 │
M10 Useful context
 │
M11 Dogfood hardening
 │
M12 Closed beta
```

## What did not change at the time

The hardening review did **not** reopen:

- Android-first native direction
- three-module initial topology
- local-first alarm authority
- deterministic Wake Runtime authority
- AI-as-expression-only rule
- Direct Boot privacy split
- Vercel as future non-critical compute direction
- Supabase as future managed cloud data direction
- no account required for initial use
- no KMP/React Native/Flutter
- no ML before meaningful real data

The historical single-schedule item that originally appeared in this list was later superseded by ADR-022 and is intentionally not presented here as a current invariant.

## Outcome

At the time of this review, the project was **ready for M0**.

The durable lesson was to prove risk in the intended order rather than letting implementation momentum choose the product sequence. New product behavior now follows the broader Explore → Shape → Build → Harden → Learn workflow in [`34-product-development-workflow.md`](34-product-development-workflow.md), while current implementation truth lives in `00-project-status.md`.

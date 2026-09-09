# Wake My Way — domain context

This file is the canonical domain language for Wake My Way. Product, design, architecture, tests, issues, and coding agents should use these terms consistently.

If a durable concept is renamed or its meaning changes, update this file in the same change and record the decision in `docs/26-decisions-log.md` or an ADR when technical.

## Product job

Wake My Way helps a person who intentionally chose a wake time become meaningfully active with the **minimum effective friction** that works for them.

The product is not a general assistant, sleep tracker, task manager, or engagement product.

## Core domain terms

### Wake Schedule

The user's reusable wake intention. In V1 there is one active Wake Schedule at a time. It may express different times by weekday, but it produces only one next Wake Occurrence.

A Wake Schedule is not an Android alarm. It is product intent.

### Wake Occurrence

One concrete execution of the Wake Schedule at a particular local date/time and resolved instant.

Examples:

- Thursday, 08:00 Europe/Berlin
- a five-minute Snooze Occurrence created from an active Wake Occurrence

The Alarm Kernel schedules the next Wake Occurrence with Android.

### Snooze Occurrence

A Wake Occurrence created only after the user intentionally confirms snooze. It replaces the currently active wake attempt with a new exact occurrence rather than relying on an in-process timer.

### Alarm Kernel

The deep Android module that owns the reliability invariant from accepted Wake Occurrence to durable local schedule, exact OS alarm, critical fallback, reconciliation, snooze replacement, and audible wake start.

Callers must not coordinate the ordering of Room persistence, critical snapshot writes, `AlarmManager`, or reconciliation themselves.

### Critical Wake Snapshot

The minimal non-sensitive state required to recover/re-register and start a safe alarm without normal database initialization.

It lives in device-protected storage so it can be read during Direct Boot. It must not contain Tomorrow Contract text, calendar details, transcripts, or personalized generated speech derived from sensitive context.

### Wake Ready

User-facing readiness state meaning the next Wake Occurrence has the required critical Android capabilities and local data to fire inside the documented reliability envelope.

Optional microphone, calendar, weather, and cloud capabilities do not determine Wake Ready.

### Wake Session

The active morning interaction created when a Wake Occurrence fires.

A Wake Session is deterministic at its behavioral core and may be enriched by voice/AI when available.

### Wake Phase

The coarse lifecycle phase of an active Wake Session:

1. **Alerting** — reliable audible wake stimulus is active.
2. **Engaging** — the product is seeking/receiving meaningful user interaction.
3. **Activating** — the product is seeking behavioral evidence of physical activation.
4. **Orienting** — sufficient activation evidence exists to provide small amounts of context and a First Move.
5. **Finished** — the session ended by completion, intentional snooze, cancellation, or unrecoverable termination.

Escalation level, fallback mode, movement requests, and First Move are not Wake Phases.

### Wake Input

A typed fact presented to the Wake Runtime, such as an alarm firing, user interaction, observed movement, elapsed timeout, voice failure, or snooze confirmation.

Inputs describe facts. They do not encode arbitrary commands from AI.

### Wake Directive

A typed decision emitted by the Wake Runtime, such as ensuring alarm audio, speaking a constrained intent, beginning movement observation, offering snooze, presenting orientation, or completing the session.

The application/platform layer executes directives and reports relevant results back as Wake Inputs.

### Wake Runtime

The deep pure-Kotlin module that owns all **in-session behavioral decision-making**.

Given current session state, a Wake Input, and a versioned Wake Policy, it deterministically decides the next state and Wake Directives.

Activation evidence, escalation selection, and in-session strategy are implementation details of the Wake Runtime rather than separate public modules.

### Activation Evidence

Internal evidence that the user is behaving as meaningfully active: interaction, device pickup, sustained movement, coherent response, time patterns, and similar signals.

WMW may compute an internal score/probability, but it does **not** claim to medically verify that a person is awake. The old term "Verified Awake" is intentionally not part of the domain language.

### Wake Policy

A versioned set of deterministic parameters used by the Wake Runtime: timing, escalation thresholds, snooze behavior, intervention preferences, and relevant context rules.

A new user begins with a default Wake Policy. Wake Learning may derive a more personal policy later.

### Wake Learning

Off-session logic that derives future Wake Policy/profile information from prior Wake Sessions, outcomes, and feedback.

Wake Learning is deliberately separate from the Wake Runtime because it has a different lifecycle and may eventually use server-side or ML implementations. It is never required for the current alarm to fire.

### Wake Outcome

A derived summary of what happened in a Wake Session, including time to engagement, time to meaningful movement, snooze behavior, fallback use, intervention depth, and whether the session met its target wake window.

### Wake Success

Product outcome: a Wake Session reaches the configured behavioral activation criterion inside the intended wake window.

This is a behavioral product metric, not a medical assertion.

### Minimum Effective Friction

The least aggressive intervention that reliably produces Wake Success for the user/context while preserving agency and avoiding unnecessary annoyance.

### Tomorrow Contract

Optional night-before intention supplied by the user: why tomorrow matters and/or what they want morning-them to remember.

It is private by default and should remain local unless a clearly disclosed cloud feature needs a minimized representation.

### Prepared Wake Plan

Optional non-critical content prepared before the wake time so richer morning behavior can begin without waiting for the network.

It may include safe local phrasing/audio and context. It is not required for the Alarm Kernel to make sound.

### Character

A presentation/personality profile such as Alfred, Sam, or Chaos. A Character changes language, voice, humor, and tone within strict behavioral/safety constraints.

A Character does not own Wake Policy or state transitions.

### Speech Intent

A constrained semantic request from Wake Runtime to the speech/character layer, for example `INITIAL_WAKE`, `ASK_TO_SIT`, `ASK_TO_MOVE`, `REENGAGE`, or `MORNING_ORIENTATION`.

The renderer may vary wording but must preserve the requested intent.

### First Move

The immediate real-world action selected or confirmed during Orienting, such as shower, coffee, or getting dressed.

It is an orientation aid, not a required Wake Phase.

### Wake Motif

The short branded melodic identity that begins the wake transition before or alongside character speech.

## UX consciousness language

The design documents may use **Asleep → Emerging → Engaged → Active → Oriented** to describe the user's likely cognitive/visual experience.

This is a UX model, not the runtime state machine. Do not create one code state for every UX label by default.

## Reliability language

### Reliability envelope

Wake My Way aims to be reliable when its process is dead, the device is idle, the network/cloud/AI is unavailable, or normal app data initialization fails.

No Android app can guarantee an alarm after conditions where the OS intentionally prevents it, including explicit user Force Stop on Android versions that cancel pending intents, app uninstall/disable, powered-off hardware, or revoked required system capabilities. WMW must detect and explain recoverable readiness problems on the next user interaction.

### Fallback level

The current richness available to a Wake Session, from realtime conversational mode down to bundled emergency alarm audio. Fallback level is a capability/runtime mode, not a Wake Phase.

## Architecture language

When discussing architecture, use the deep-module vocabulary consistently:

- **Module** — cohesive responsibility with implementation behind a caller-facing contract.
- **Interface / contract** — everything callers must know: operations, invariants, effects, failure modes, ordering, lifecycle, and performance expectations.
- **Depth** — useful coherent behavior hidden behind a small contract.
- **Leverage** — benefit callers gain from a deep module.
- **Locality** — concentration of related decisions, bugs, and changes in one owner.
- **Seam** — a justified place where behavior can change without editing the caller.
- **Adapter** — concrete implementation/translator selected at a seam.

Do not create seams solely because another platform might exist someday. Seams require evidence such as external-system isolation, testing, volatility, failure domain, or actual substitution.

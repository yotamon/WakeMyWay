# WakeMyWay — domain context

This file is the canonical domain language for WakeMyWay. Product, design, architecture, tests, issues, and coding agents should use these terms consistently.

If a durable concept is renamed or its meaning changes, update this file in the same change and record the decision in `docs/26-decisions-log.md` or an ADR when technical.

## Product job

WakeMyWay helps a person who intentionally chose a wake time become meaningfully active with the **minimum effective friction** that works for them.

The product may also help the user understand wake consistency, snooze behavior, activation timing, and optional sleep data when Health Connect is explicitly enabled. It is not a general assistant, medical sleep tracker, task manager, or engagement product.

## Core domain terms

### Alarm Definition

The user's consumer-facing alarm configuration.

An Alarm Definition may contain a label, enabled state, time/schedule pattern, branded sound, Voice Check-In choice, Character, Voice Style, Snooze Policy, Tomorrow Contract behavior, and a default First Move.

Multiple Alarm Definitions may exist and may be independently enabled. An Alarm Definition is product state. It is not an Android alarm, a Wake Occurrence, or Direct Boot critical state.

### Wake Schedule

The minimal deterministic schedule intent compiled from an enabled Alarm Definition for Alarm Kernel execution.

A Wake Schedule expresses the local timing information and revision required to resolve future Wake Occurrences. Multiple independent Wake Schedules may coexist once the multi-alarm kernel migration is complete.

A Wake Schedule is not an Android alarm and does not carry presentation-only or private product metadata.

### Wake Occurrence

One concrete execution of a Wake Schedule at a particular local date/time and resolved instant.

Examples:

- Thursday, 08:00 Europe/Berlin
- a five-minute Snooze Occurrence created from an active Wake Occurrence

The Alarm Kernel schedules eligible Wake Occurrences with Android and rejects stale occurrences whose owning schedule/revision is no longer authoritative.

### Snooze Occurrence

A Wake Occurrence created only after the user intentionally confirms snooze. It replaces the currently active wake attempt for that alarm chain with a new exact occurrence rather than relying on an in-process timer.

### Alarm Kernel

The deep Android module that owns the reliability invariant from accepted Wake Schedule/Occurrence intent to durable local schedule state, exact OS alarms, critical fallback, reconciliation, snooze replacement, collision handling, and safe active wake execution.

Callers must not coordinate critical snapshot writes, `AlarmManager`, active-alarm recovery, collision handling, terminal actions, or reconciliation themselves.

The multi-alarm target preserves independent schedule ownership: editing, disabling, or snoozing alarm B must not invalidate alarm A.

### Critical Wake State

The minimal non-sensitive state required to recover/re-register and start safe alarms without normal database initialization.

It lives in device-protected storage so it can be read during Direct Boot. The multi-alarm target may contain multiple independent critical schedule slots plus at most one active execution authority.

Critical state must not contain Tomorrow Contract text, alarm labels unless strictly required for safety, account metadata, calendar details, transcripts, or personalized generated speech derived from sensitive context.

Historical code and documentation may refer to the original single-slot serialization as the **Critical Wake Snapshot**.

### Wake Ready

User-facing readiness state meaning an enabled alarm's next Wake Occurrence has the required critical Android capabilities and local data for a **controllable** wake inside the documented reliability envelope.

Wake Ready requires the occurrence to be scheduled/recoverable **and** the platform capabilities needed to expose the active wake to the user: alarm notifications, an alarm notification channel at the required high importance, and full-screen alarm access on Android versions that gate it. Audible-only emergency fallback is valuable but is not sufficient for Wake Ready.

Wake Ready is evaluated per enabled Alarm Definition. The product may also present aggregate readiness such as all alarms ready, some alarms need attention, or no alarms enabled.

Optional microphone/on-device speech recognition, Alfred voice availability, Tomorrow Contract, calendar, weather, realtime AI, account, sync, Health Connect, and cloud capabilities do not determine base Wake Ready. They are reported separately and may degrade without preventing the critical alarm/control path.

### Active Wake Execution

The trust-critical Android execution that begins when a Wake Occurrence fires and remains responsible for keeping safe alarm audio and user controls alive until the occurrence is intentionally stopped, durably snoozed, completed, or reaches an explicitly handled terminal failure.

Active Wake Execution is owned by the Alarm Kernel's Android implementation. A visible `WakeActivity` may present the experience, but Activity lifetime is not allowed to be the lifetime authority for alarm playback.

Multiple future alarms may be scheduled, but only one physical Active Wake Execution is authoritative at a time. Collision behavior must be deterministic and kernel-owned.

### Wake Session

The active morning behavioral interaction created when a Wake Occurrence fires.

A Wake Session is deterministic at its behavioral core and may be enriched by voice/AI when available. Its behavioral state is separate from the Android component that keeps critical alarm playback alive.

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

Wake Learning is deliberately separate from the Wake Runtime because it has a different lifecycle. Initial learning begins with deterministic, local, explainable policy updates before machine learning is allowed to become central to the product.

Wake Learning is never required for the current alarm to fire.

### Wake Outcome

A derived summary of what happened in a Wake Session, including time to engagement, time to meaningful movement, snooze behavior, fallback use, intervention depth, and whether the session met its target wake window.

Wake Outcomes are the primary WakeMyWay-owned source for Insights and remain distinguishable from optional sleep data imported through Health Connect.

### Activation Completion

Operational outcome: the Wake Runtime's configured behavioral activation criterion was reached inside the intended wake window.

This is useful for deterministic runtime behavior and immediate measurement, but it is not automatically proof that the user's real-world wake goal succeeded.

### Confirmed Wake Success

Calibrated product outcome: evidence indicates the user actually achieved the intended wake result rather than merely satisfying the phone-observable activation criterion and then returning to bed.

Confirmation may come from occasional lightweight user feedback and future privacy-safe proxies. It is not required every morning and is never a medical assertion.

### Wake Success

When used without qualification in product strategy, **Wake Success means the real-world product goal** and should be measured using Confirmed Wake Success when calibration evidence exists. Activation Completion remains the operational runtime metric.

Historical or experiment analysis must keep these two concepts distinguishable so the system cannot improve merely by making its own activation threshold easier to satisfy.

### Minimum Effective Friction

The least aggressive intervention that reliably produces Wake Success for the user/context while preserving agency and avoiding unnecessary annoyance.

### Tomorrow Contract

Optional night-before intention supplied by the user: why tomorrow matters and/or what they want morning-them to remember.

A Tomorrow Contract is associated with a concrete upcoming wake/occurrence, even when entered from a reusable Alarm Definition. It is private by default and should remain local unless a clearly disclosed cloud feature needs a minimized representation.

### Prepared Wake Plan

Optional non-critical content prepared before the wake time so richer morning behavior can begin without waiting for the network.

It may include safe local phrasing/audio and context. It is not required for the Alarm Kernel to make sound.

### Character

A presentation/personality profile such as Alfred, Sam, or Chaos. A Character changes language, voice, humor, and tone within strict behavioral/safety constraints.

A Character does not own Wake Policy or state transitions.

### Voice Style

A presentation preference that controls the energy, length, and tone of rendered character speech while preserving the same semantic Speech Intent.

Initial values are Default, Motivational, and Minimal. Voice Style is not a Wake Policy and may not change deterministic behavioral authority by itself.

### Voice Check-In

Per-alarm product preference allowing the wake experience to request a short spoken response when local speech capability is available.

Voice Check-In is an enrichment path. Critical local alarm delivery and terminal controls remain functional when voice input is unavailable.

### Wake Sound

The locally resolvable audio choice associated with an Alarm Definition.

Initial branded sounds are Morning Light, Soft Start, and Morning Pulse. Wake Sound selection may change the audio presentation but must not introduce a network dependency into alarm delivery.

### Snooze Policy

Per-alarm product configuration describing whether snooze is available, its local replacement duration, and optional bounded count.

The policy is product input to deterministic runtime/kernel behavior. Snooze execution remains a durable exact replacement occurrence, not an in-process timer.

### Speech Intent

A constrained semantic request from Wake Runtime to the speech/character layer, for example `INITIAL_WAKE`, `ASK_TO_SIT`, `ASK_TO_MOVE`, `REENGAGE`, or `MORNING_ORIENTATION`.

The renderer may vary wording but must preserve the requested intent.

### First Move

The immediate real-world action selected or confirmed during Orienting, such as shower, coffee, or getting dressed.

It is an orientation aid, not a required Wake Phase. A reusable Alarm Definition may carry a default First Move, while a Tomorrow Contract may override it for one concrete wake.

### Wake Motif

The short branded melodic identity that begins the wake transition before or alongside character speech.

### Wake Insight

A user-facing summary derived from real Wake Outcomes and, only when explicitly enabled, optional Health Connect sleep data.

Examples include wake consistency, snooze frequency, time to engagement, time to Activation Completion, and target-window streaks.

A Wake Insight is not a medical conclusion and must not fabricate sleep quality or wellness scores unsupported by data.

### Safety Backup

An optional transition aid for early dogfood/onboarding that lets a user keep a later conventional external safety alarm while they build trust in WakeMyWay.

A Safety Backup is not a WakeMyWay Alarm Definition, does not participate in the Wake Runtime, and is not counted as part of the multi-alarm product model. Whether it ships beyond dogfood is evidence-driven.

## UX consciousness language

The design documents may use **Asleep → Emerging → Engaged → Active → Oriented** to describe the user's likely cognitive/visual experience.

This is a UX model, not the runtime state machine. Do not create one code state for every UX label by default.

## Reliability language

### Reliability envelope

WakeMyWay aims to be reliable when its normal UI Activity is gone, its process must be recreated, the device is idle, the network/cloud/AI is unavailable, or normal app data initialization fails.

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

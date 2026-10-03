# End-to-end product behavior polish

**Status:** accepted Shape contract and Build plan
**Date:** 2026-10-02
**Branch:** `feat/end-to-end-product-polish`
**1.0 gate:** FINISH #59, with supporting TRUST/PROVE diagnostics

## Goal

Make the existing WakeMyWay product behave as one coherent, predictable system from planning through wake completion without expanding the 1.0 feature set.

The work is deliberately about **behavioral coherence, failure clarity and testability**, not adding adjacent capabilities.

## User outcome

A user should always be able to answer:

1. Is my next wake actually ready?
2. What is WakeMyWay doing right now?
3. If Live Voice failed, is my alarm still safe?
4. Did my alarm change actually commit?
5. Can I test the wake without polluting my real morning history?

## Product contract

- Alarm Kernel remains the only authority for scheduling, active execution, Stop and Snooze.
- Wake Runtime remains the only authority for wake progression/completion.
- Realtime remains optional enrichment; one unrecovered Realtime failure degrades that occurrence to alarm-only.
- No local TTS/STT consumer fallback is reintroduced.
- Test/lab wakes exercise production behavior but never become user history, learning evidence or a Morning Safety Check.
- Diagnostics remain local, bounded and free of transcripts/private context.

## Required convergence

### 1. One wake-experience projection

The UI receives a single typed projection of the behavioral session. It must distinguish connecting, conversational engagement, activation, orientation, completion and alarm-only degradation without inferring hidden controller state.

A Realtime startup attempt must visibly remain a connecting state while the local alarm continues. It must not falsely present alarm-only before the startup budget has actually failed.

### 2. Explicit degradation

Alarm-only degradation carries a bounded, non-private reason category such as startup timeout, session failure or unavailable transport.

The category may drive copy and diagnostics. Raw provider payloads, prompts and transcript text never enter UI state or diagnostic persistence.

### 3. Transactional consumer actions

Alarm mutations remain synchronous/acknowledged at the product boundary. The UI must show mutation failure instead of silently snapping back.

Failed Snooze/Stop transactions keep the current wake visible and actionable and show a concise failure state.

### 4. Actionable readiness

Base Wake Ready and optional Voice readiness stay separate. Per-alarm surfaces should explain the current blocker and expose the existing repair action where relevant.

### 5. Production-faithful Test Wake

Wake Lab continues to use the real Alarm Kernel, playback, WakeActivity and WakeRuntime path, but all `lab-*` schedules are classified as test sessions and excluded from durable wake history, learning and post-wake follow-up.

### 6. Resolved-plan inspection

Wake Lab exposes the resolved immutable Wake Session Plan used by the next/test wake: character, presentation style, directness, verbosity, pacing, motivation/humor and policy thresholds.

The inspector displays derived settings only. It does not become a second configuration UI.

### 7. Wake Session Inspector

The existing WakeTimingTrace becomes the local privacy-safe timeline for delivery plus interactive wake behavior.

Useful semantic events include Realtime connecting/ready/degraded, runtime phase changes, voice listening/speaking, motion evidence categories and terminal action. The timeline must not store spoken text, transcripts, Tomorrow Contract text or user-entered labels.

## Non-goals

- new characters, sounds, calendar/weather features or permissions;
- a new cloud analytics dependency;
- changing activation criteria or Wake Learning rules;
- moving scheduling/terminal authority into Compose;
- making Test Wake count as real behavioral evidence;
- making Realtime failure retry later in the same wake;
- universal claims that physical wake success is proven by app-visible activation.

## Acceptance

- connecting Realtime is represented truthfully;
- one voice failure yields a stable alarm-only state with an explicit safe reason;
- failed terminal or alarm mutations are user-visible and preserve authoritative state;
- per-alarm readiness identifies the blocker and offers repair;
- lab wakes cannot affect Wake History, Wake Learning or Morning Safety Check;
- the lab can show the resolved personalization plan;
- recent wake evidence includes privacy-safe behavioral lifecycle events;
- docs describe one degradation policy everywhere;
- targeted unit tests, Direct compile/build and relevant lint remain green;
- physical overnight/device evidence remains a separate release gate.

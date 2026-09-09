# ADR-005: AI is not wake-state authority

**Status:** Accepted
**Date:** 2026-09-09

## Context

Models can hallucinate, vary output, misunderstand sleepy speech, or fail entirely. Alarm dismissal and behavioral activation transitions must remain deterministic product decisions.

## Decision

AI may render constrained character/speech language for a Speech Intent selected by Wake Runtime.

AI cannot directly:

- mark the activation criterion as met
- claim authoritative knowledge that the user is biologically awake
- finish/dismiss the wake attempt
- accept snooze without explicit allowed user action
- choose arbitrary Wake Phase transitions
- invent personal/calendar facts

## Consequence

Conversation can be creative and variable without surrendering behavioral reliability or user control.

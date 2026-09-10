# ADR 020: Conversational Wake as non-authoritative enrichment

**Status:** Accepted for founder dogfood  
**Date:** 2026-09-10

## Context

The local production Wake Session is reliable and controllable, but its deterministic Alfred prompts feel scripted. Wake My Way needs natural two-way conversation without allowing a network model or speech provider to become part of the critical alarm safety boundary.

## Decision

Introduce `WakeConversationEnrichment` as an optional speech-enrichment boundary above typed `WakeRuntime` directives.

For founder/debug dogfood, implement a direct OpenAI Realtime WebRTC adapter that:

- uses audio-to-audio WebRTC;
- uses server VAD only to observe user speech boundaries;
- sets automatic response creation off;
- allows interruption/barge-in;
- lets WakeRuntime request each assistant response through the current `SpeechIntent`;
- emits only bounded typed conversation events back to the Android controller;
- never receives authority to Stop, Snooze, schedule, score, or complete a wake;
- falls back immediately to deterministic local Alfred when unavailable or failed.

`SpeechIntent.KeepEngaging` is added so WakeRuntime can deliberately continue the conversation after a coherent reply while activation evidence remains below threshold.

## Consequences

A wake can sound natural while preserving deterministic behavioral authority. Provider/network failure degrades speech quality rather than alarm safety. The production release remains local unless a later decision explicitly promotes remote conversation after physical reliability, privacy, authorization, cost, and latency evidence.

Founder broker credentials remain debug-only and are encrypted locally using Android Keystore. This is not consumer authentication.

## Non-goals

This ADR does not select a permanent commercial provider, permit private Tomorrow Contract/calendar context, make cloud availability a Wake Ready prerequisite, or replace local TTS/STT fallback.
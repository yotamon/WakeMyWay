# ADR-008: Select realtime voice provider after measured spike

**Status:** Accepted as process decision
**Date:** 2026-09-09

## Context

Direct realtime model APIs, LiveKit and voice providers trade latency, reliability, flexibility, cost and operational complexity differently.

## Decision

Do not lock provider during architecture planning.

M7 will measure at least:

- cold connection latency
- first speech latency
- interruption/barge-in
- reconnection
- Wi-Fi/mobile transitions
- Bluetooth behavior
- complexity
- cost

Current preference for the first prototype is direct OpenAI realtime, but it is not a final provider decision.

# ADR-008: Select realtime voice provider after measured spike

**Status:** Accepted as process decision  
**Date:** 2026-09-09  
**Amended:** 2026-09-09 plan hardening review

## Context

Direct realtime model APIs, LiveKit and voice providers trade latency, reliability, flexibility, cost and operational complexity differently.

Realtime voice is valuable presentation richness, but it is not the core adaptive moat. Wake My Way should first prove local deterministic wake behavior and Wake Learning v0 before allowing realtime provider complexity to dominate development.

## Decision

Do not lock provider during architecture planning.

The provider spike moves to **M8**, after M7 Wake Learning v0 has demonstrated at least one useful bounded/explainable policy adaptation from real or fixture wake outcomes.

M8 will measure at least:

- cold connection latency
- first speech latency
- interruption/barge-in
- reconnection
- Wi-Fi/mobile transitions
- Bluetooth/audio-route behavior
- privacy/data flow
- operational complexity
- cost per wake

Compare deployment/transport shapes when practical:

```text
Android → direct realtime provider
Android → LiveKit / RTC layer
Android → Vercel WebSocket gateway → provider
```

Current preference for the first prototype is direct OpenAI realtime, but it is not a final provider decision.

## Consequences

- personalization is proven independently from realtime voice vendors
- M7 remains local/offline and does not require backend/provider infrastructure
- realtime voice must enrich an already-functional Wake Runtime/Wake Learning loop
- provider failure can never invalidate current Wake Policy, Alarm Kernel execution, stop/snooze, or local fallback

# ADR-012: Use Vercel for initial non-critical cloud/web deployment

**Status:** Accepted with realtime-voice caveat  
**Date:** 2026-09-09  
**Amended:** 2026-09-09 plan hardening review

## Context

Wake My Way will eventually need some cloud capabilities, most likely realtime-provider credential/session brokerage, optional Wake Plan generation, web/marketing pages, and later sync/account operations.

The Android alarm must remain local-first and must never depend on cloud availability. Local Wake Learning v0 also does not require a backend.

Vercel currently provides convenient TypeScript/Node deployment, preview environments, Fluid compute, and WebSocket support in Public Beta. These features make it a strong initial platform for non-critical cloud/web workloads, but realtime voice is sensitive enough that hosting/transport must be selected from measured latency and reliability rather than convenience.

## Decision

Use **Vercel as the preferred initial deployment platform for web and ordinary non-critical backend workloads once a real backend capability exists**.

Vercel is explicitly outside:

- Alarm Kernel scheduling authority
- Active Wake Execution / critical alarm playback
- local Wake Runtime authority
- local Wake Learning v0

Do not create the Vercel/backend project during M0–M7 merely to satisfy architecture plans.

Realtime voice remains governed by ADR-008. **M8** must measure direct provider, LiveKit/RTC, and Vercel WebSocket-gateway options before selecting the transport.

## Consequences

- web/API pull requests can gain Vercel Preview Deployments once those projects exist
- I/O-heavy TypeScript cloud work has a low-operations deployment path
- cloud unavailability cannot block current alarm delivery, active playback, completion, or local policy adaptation
- backend/service folders remain deferred until a concrete capability needs them
- Vercel WebSocket beta status/limits must be rechecked during M8
- durable voice/session state cannot rely solely on one Function process
- Supabase is the preferred managed PostgreSQL/data platform under ADR-013; exact serverless connection configuration is selected at implementation time

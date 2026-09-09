# Backend and API architecture — deferred design

**Status:** architectural direction, not an implementation commitment for M0–M7.

Wake My Way does not need a backend to schedule, fire, keep an active alarm sounding, snooze, complete the base alarm experience, or perform Wake Learning v0. Do not scaffold cloud services before a real capability requires them.

## Likely backend responsibilities

Cloud becomes justified for capabilities such as:

- realtime voice credentials/orchestration
- optional cloud Wake Plan generation
- account/sync later
- aggregated learning later, only if local learning proves value and cloud aggregation has a privacy/product reason
- subscription identity later

The backend never schedules or triggers the Android Wake Occurrence and never owns the active alarm execution.

## Local learning first

M7 Wake Learning v0 is explicitly local and deterministic.

Do not introduce a backend merely because the product learns. First prove that versioned local outcomes can produce useful bounded Wake Policy changes. Cloud learning/aggregation is a later option, not a prerequisite for personalization.

This protects the product thesis from becoming dependent on account creation, sync, network availability, or backend analytics before the behavior is validated.

## Deployment direction when the boundary exists

Vercel is the preferred initial host for web and ordinary non-critical TypeScript backend workloads. See [`32-testing-and-deployment-topology.md`](32-testing-and-deployment-topology.md) and ADR-012.

This does **not** make Vercel part of wake authority. The Android Alarm Kernel remains local-first and fully independent of Vercel availability.

Realtime voice transport is not selected by this decision. Vercel WebSockets are a measured **M8** candidate alongside direct provider and LiveKit/RTC approaches.

## Direction when the boundary exists

Preferred stack:

```text
current Node.js LTS
TypeScript
Fastify
Vercel
Supabase-managed PostgreSQL
Kysely
Pino
OpenAPI 3.1
```

Reconfirm exact versions and current provider connection guidance when the first service is implemented.

## Supabase data platform

Supabase is the preferred managed cloud data platform once persistence is justified. See ADR-013.

Responsibility split:

```text
Android device
    │
    ▼
Wake API on Vercel
    │
    ▼
Kysely / application logic
    │
    ▼
Supabase PostgreSQL
```

Use Supabase for:

- managed PostgreSQL
- Auth later when account identity is genuinely needed
- Storage only when a concrete object-storage feature exists

Do **not** make Android domain behavior depend directly on Supabase database tables or RLS policy as the primary business-logic layer. Database schema remains a backend implementation detail.

A future Android → Supabase Auth interaction may be used to acquire identity/session credentials, after which domain API calls still go to the Wake API.

The serverless database connection/pool mode must be selected according to current Supabase guidance when cloud code is first deployed; do not freeze connection details before then.

Cloud environments should separate development/test data from production dogfood/release data.

## Service shape

Start as one deployable service with cohesive packages. Do not pre-create microservices or a directory hierarchy for hypothetical features.

An initial concrete service might begin as simply:

```text
services/api/
  src/
    voice/
    installation/
    platform/
```

and grow only when those responsibilities exist.

A separate realtime worker is added only if the selected voice architecture actually requires a long-running process.

## OpenAPI contract

Once Android talks to a Wake backend, OpenAPI 3.1 is the language-neutral service contract for Android and a future iOS client.

Do not share TypeScript schema packages directly into mobile architecture.

OpenAPI is a boundary contract, not a reason to generate/commit unused clients before endpoints exist.

## Endpoint design is illustrative until implemented

Possible future operations include:

```text
POST /v1/installations
POST /v1/wake-plans/prepare
POST /v1/voice/sessions
POST /v1/wake-sessions/{id}/events
POST /v1/wake-sessions/{id}/complete
```

These are **not frozen API commitments**. Define the smallest surface needed by the first cloud feature, then version it deliberately.

Local Wake Learning v0 does not need a corresponding endpoint.

## Identity

No account is required for first alarm use.

If a backend needs to identify a client before accounts exist:

```text
local installation ID
    ↓
server installation record
    ↓
short-lived / rotating credential
```

Account claiming/merge may arrive later for sync, subscription, backup, or iOS migration.

## Event upload

Never send a network request for each sensor reading. Persist/derive privacy-safe semantic events and batch only what a cloud feature genuinely needs.

Network synchronization is eventually consistent and never required for current alarm behavior or local policy adaptation.

If Confirmed Wake Success calibration is later uploaded, upload only the minimum semantic outcome required for the enabled cloud feature, not private transcripts/context by default.

## Idempotency

Any mobile write API exposed later must tolerate retries and duplicate delivery. Use client-generated operation/event IDs and explicit idempotency semantics where appropriate.

## Infrastructure intentionally absent

No Redis, Kafka, RabbitMQ, queue framework, microservice mesh, or distributed lock service by default.

Introduce infrastructure only after a measured workload or failure mode justifies it.

## Logging/privacy

Safe operational metadata may include:

```text
requestId
installation pseudonymous ID
provider
latencyMs
resultCode
fallbackLevel
```

Never log:

- Tomorrow Contract raw text
- full transcripts/audio
- calendar titles/descriptions
- complete model prompts
- credentials/tokens

## Remote feature flags

Remote config may disable non-critical provider experiments or optional context. Cached safe defaults must exist locally.

Remote state must never disable the Alarm Kernel, active alarm playback, bundled emergency alarm, stop/snooze controls, critical local fallback, or invalidate the currently snapshotted Wake Policy.

## Vercel guardrails

When cloud code is introduced:

- use Vercel Preview Deployments for web/API changes where useful
- prefer ordinary Functions/Fluid compute for I/O-bound API work
- recheck current Function duration, WebSocket beta status, pricing and limits at implementation time
- never hold current Wake Occurrence authority, active wake execution, or irreplaceable wake state only inside a Function process
- design realtime WebSocket clients for reconnect if the Vercel route wins the M8 spike
- keep Supabase data responsibilities independent from the Vercel compute host; do not let either provider own product/domain semantics

Vendor capability references are recorded in `32-testing-and-deployment-topology.md`.

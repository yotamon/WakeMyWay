# ADR-013: Use Supabase as the preferred managed cloud data platform

**Status:** Accepted, introduced only when a concrete cloud feature exists  
**Date:** 2026-09-09

## Context

Wake My Way is local-first. The Android device owns alarm scheduling, current Wake Occurrence state, Wake Runtime behavior, critical fallback audio, and all trust-critical wake behavior.

Future non-critical cloud capabilities will still need durable data, and later may need account identity and object storage. The project already prefers PostgreSQL + Kysely for server-side persistence and Vercel for non-critical web/API compute.

A managed data platform should reduce operations without leaking database schema into mobile product architecture or becoming a dependency of the current wake attempt.

## Decision

Use **Supabase as the preferred managed cloud data platform** when cloud persistence is first justified.

Initial intended responsibilities:

- managed PostgreSQL for server-side cloud data
- Supabase Auth later, when accounts/sync/device migration/subscription identity justify sign-in
- Supabase Storage only when a real file/object-storage use case exists

The application backend remains the domain boundary:

```text
Android
   │
   ▼
Wake API on Vercel
   │
   ▼
Kysely
   │
   ▼
Supabase PostgreSQL
```

The Android client must **not** couple domain behavior directly to Supabase database tables.

A future direct Android → Supabase Auth flow is allowed for authentication/session acquisition if chosen, but domain reads/writes still go through the Wake API.

## Local-first constraint

Supabase is never authoritative for:

- the current Wake Schedule on-device execution
- the next Wake Occurrence
- exact alarm registration
- current Wake Session behavioral state
- stop/snooze controls
- critical fallback audio
- Direct Boot recovery

If Supabase is unavailable, the current wake attempt must continue using local state and prepared fallbacks.

## Data boundary

Cloud data may eventually include privacy-safe records such as:

- installation/account records
- optional synced preferences
- cloud-generated Wake Plan metadata
- semantic Wake Session outcomes/events needed by enabled cloud features
- derived Wake Profile data if cloud learning is explicitly introduced

Sensitive morning content is not uploaded merely because storage exists. Raw microphone audio is not retained by default. Tomorrow Contract text, transcripts, calendar content, and generated prompts follow the explicit privacy rules in `16-privacy-security.md`.

## Server access

Vercel/server code should connect to Supabase PostgreSQL using the connection mode appropriate for serverless workloads at implementation time. Re-verify Supabase connection/pooling guidance when the backend is introduced.

Kysely remains the application query layer. Avoid spreading Supabase-specific query SDK usage through the domain/service layer when ordinary PostgreSQL access is sufficient.

## Auth

No account is required for initial product use.

Supabase Auth is deferred until a feature actually needs durable user identity, such as:

- cross-device sync
- Android → future iOS migration
- backup/restore
- subscription/account management

Anonymous installation identity may exist before account auth.

## Storage

Supabase Storage is deferred until a concrete object-storage feature exists, for example generated/prepared audio or future user-provided assets.

Any asset needed for tomorrow morning's reliable wake must be downloaded, validated, and available locally before the wake occurrence. Runtime network fetch is never the only copy of critical wake media.

## Environment policy

Development/test data must not casually share the production database.

Initial direction:

```text
local / preview clients → development cloud environment
production dogfood / release → production cloud environment
```

The exact preview-database strategy is deferred until cloud development begins.

## Consequences

- cloud persistence provider is no longer an open architecture question
- Vercel compute and Supabase data have intentionally separate responsibilities
- PostgreSQL portability is retained
- mobile clients remain insulated from database schema churn
- auth/storage capabilities can be adopted incrementally instead of forcing account infrastructure into V1
- Supabase outage cannot make a locally prepared alarm fail
- provider-specific platform features require explicit justification before entering the architecture

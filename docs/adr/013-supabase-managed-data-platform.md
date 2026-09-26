# ADR-013: Use Neon as the managed cloud data and identity platform

**Status:** Accepted; Supabase selection superseded on 2026-09-26  
**Date:** 2026-09-09  
**Amended:** 2026-09-26

## Context

WakeMyWay is local-first. Android owns alarm scheduling, current Wake Occurrence state, WakeRuntime,
critical alarm audio and every trust-critical wake behavior.

Non-critical cloud capabilities need durable PostgreSQL data and now also need real account identity.
The project already uses Kysely server-side and Vercel for the Wake API. The original version of this
ADR selected Supabase before account infrastructure was provisioned. No production Supabase project
or user data was created.

When account implementation reached production provisioning, Neon provided the required PostgreSQL,
Managed Better Auth, branching and direct agent/ChatGPT operations while preserving ordinary
PostgreSQL portability. The project therefore replaces the provider choice without changing the
local-first product boundary.

## Decision

Use **Neon as WakeMyWay's managed PostgreSQL and identity platform**.

Current responsibilities:

- Neon PostgreSQL for server-side cloud data;
- Neon Managed Better Auth for email/password, Google identity and sessions;
- Neon branching for isolated database/auth development when useful;
- Neon object storage/functions only when a concrete product need earns them.

Vercel remains the public Wake API/domain boundary:

```text
Android
   │ identity: Managed Better Auth API
   │ domain calls: short-lived Neon JWT
   ▼
Wake API on Vercel
   │
   ▼
Kysely
   │
   ▼
Neon PostgreSQL
```

Android does not receive a database connection string and does not bind domain behavior directly to
database tables.

## Local-first constraint

Neon is never authoritative for:

- current or future on-device Wake Schedule execution;
- next Wake Occurrence authority;
- exact alarm registration;
- current Wake Session / WakeRuntime state;
- Stop or Snooze;
- critical alarm audio;
- Direct Boot recovery.

If Neon, Vercel, Google or the network is unavailable, a locally committed wake continues from local
state.

## Account and authorization boundary

Managed Better Auth owns identity and session acquisition only.

- `neon_auth.user.id` is the immutable account UUID.
- Android persists only the opaque auth session token, encrypted with Android Keystore.
- Android obtains short-lived Neon JWTs for authenticated Wake API requests.
- WakeMyWay application roles remain in `wmw_private.account_roles`.
- Email, OAuth profile data and `neon_auth.user.role` never grant WakeMyWay admin access.

See ADR-027 for the full account/authorization contract.

## Backup and migration contract

The first cloud persistence seam remains backup/migration, not live cloud scheduling authority.

A cloud snapshot may contain normal consumer intent only:

- syncable Profile/default preferences;
- rich `AlarmDefinition` product intent;
- version/timestamp metadata needed to validate the backup contract.

It excludes Direct-Boot state, AlarmManager registrations, next-occurrence authority, active
WakeRuntime state, Stop/Snooze terminal state, private Tomorrow Contract / Prepared Wake Plan text,
raw microphone audio and transcripts.

Remote-only alarms restore disabled and require an explicit local enable action. Local alarm IDs win
conflicts. Cloud/account failure during restore fails before local mutation.

## Server access

Vercel/server code connects to Neon PostgreSQL with a server-only connection string. Kysely remains
the application query layer. Provider-specific database SDKs do not enter the domain layer when
ordinary PostgreSQL is sufficient.

Managed Better Auth JWT verification uses the public branch JWKS. Database credentials and Neon
management credentials never enter Android builds.

## Environment policy

Production account/domain data lives on the Neon production branch. Development or preview work
should use separate Neon branches/environments when persistence work needs production-like state.

Database branching does not change product authority: a branch can clone backend state, never an
Android Alarm Kernel or active wake.

## Consequences

- the original Supabase provider choice is superseded before any production Supabase data existed;
- Vercel compute and Neon data/identity have intentionally separate responsibilities;
- PostgreSQL portability remains strong;
- mobile clients stay insulated from database schema churn;
- account infrastructure is optional to local wake behavior;
- cloud backup cannot silently schedule, replace or stop a local alarm;
- restoring a remote alarm still requires a later explicit local enable action;
- Neon outage cannot make a locally prepared alarm fail;
- provider-specific storage/functions require explicit product justification before adoption.

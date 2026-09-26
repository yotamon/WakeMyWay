# ADR 027: Optional Supabase identity with server-owned WakeMyWay roles

**Status:** Accepted  
**Date:** 2026-09-26

## Context

WakeMyWay needs real consumer accounts before paid distribution so a user can sign up, sign in with
email/password or Google, restore account-bound data, and later own a subscription entitlement.

The wake product already has a stronger architectural constraint: account, network and cloud state
must never become authority for exact alarm scheduling, active wake execution, Stop, Snooze or local
wake readiness.

Authorization also needs an operator/founder role. Treating an email address, Google profile,
client-side flag or user-editable metadata as an admin boundary would be unsafe and would make
future payment/support tooling difficult to reason about.

## Decision

1. **Supabase Auth owns identity and session acquisition only.**
   - Android may use the Supabase publishable key.
   - Email/password and Google OAuth are supported.
   - Mobile OAuth uses PKCE and returns to the app through `wakemyway://auth`.
   - Supabase service-role or database credentials are never shipped in Android.

2. **WakeMyWay remains usable without an account.**
   - Sign-in is optional.
   - Auth initialization, refresh, outage or sign-out cannot gate Alarm Kernel or WakeRuntime.
   - Account UI lives under Profile rather than before the Home/alarm experience.

3. **The Wake API owns authorization.**
   - Android sends a Supabase user access token to authenticated Wake API routes.
   - The API verifies signature, issuer, audience, expiry and UUID subject against Supabase JWKS.
   - `wmw_private.account_roles` stores explicit server-owned roles: `user` or `admin`.
   - Missing role rows fail to the least-privileged `user` role.
   - Client metadata, user metadata and email addresses are never accepted as authorization.

4. **Role lookup is exposed through `GET /api/v1/account/me`.**
   - It returns the authenticated account id, optional email and server-owned role.
   - Android may render admin affordances only when that response verifies the admin role.
   - A role lookup failure never logs the user out of local alarm functionality.

5. **Payments will attach to the same immutable account id later.**
   - Subscription ownership/entitlement will use the Supabase `auth.users.id` / Wake account id.
   - Google Play remains the purchase source of truth, while WakeMyWay derives normalized
     entitlement server-side.
   - Admin access is a separate authorization concern and must not be forged by purchase state.

## Consequences

- Production account activation requires a dedicated WakeMyWay Supabase project, migrations,
  email settings and a configured Google OAuth client.
- The founder/admin grant can happen only after that real auth user exists. It is an explicit
  server-side role insert/update, not an email-based bootstrap.
- Android builds without Supabase configuration show a truthful unavailable state instead of a fake
  sign-in path, while all local wake behavior remains available.
- Future backup, payment, support and admin routes can share one identity/authorization boundary
  without refactoring Alarm Kernel or trusting client-provided privileges.

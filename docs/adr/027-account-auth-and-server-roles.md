# ADR 027: Optional Neon identity with server-owned WakeMyWay roles

**Status:** Accepted  
**Date:** 2026-09-26  
**Supersedes:** the initial same-day Supabase Auth implementation of this ADR

## Context

WakeMyWay needs real consumer accounts before paid distribution so a user can sign up, sign in with
email/password or Google, restore account-bound data, and later own a subscription entitlement.

The wake product has a stronger architectural constraint: account, network and cloud state must never
become authority for exact alarm scheduling, active wake execution, Stop, Snooze, critical audio or
local Wake Ready truth.

Authorization also needs an operator/founder role. Treating an email address, Google profile,
client-side flag or identity-provider role as the WakeMyWay admin boundary would be unsafe.

## Decision

1. **Neon Managed Better Auth owns identity and session acquisition only.**
   - Google is the production sign-in method.
   - Email/password remains an optional implementation path, disabled in production until custom
     email delivery and verification are enabled.
   - Android never talks directly to PostgreSQL. Google identity acquisition follows Neon's managed
     browser OAuth protocol through the Wake API and the public Managed Better Auth API.
   - The signed Better Auth session cookie's `name=value` pair is encrypted at rest with an Android
     Keystore AES-GCM key. The unsigned response-body session id is never used as bearer auth.
   - Android requests a short-lived Neon JWT only when an authenticated Wake API call needs one.
   - PostgreSQL credentials and other server/operator secrets are never shipped in Android.

2. **Google sign-in uses Neon's managed browser OAuth/session-verifier protocol.**
   - Android generates a high-entropy PKCE verifier locally, stores it encrypted with Android
     Keystore and opens the Wake API social-login start URL.
   - The Wake API uses the pinned `@neondatabase/auth/server` proxy toolkit so Neon's
     `session_challenge` cookie is issued on the Wake API origin with mobile-safe cookie semantics.
   - Google completes OAuth at Neon and Neon redirects to the Wake API with
     `neon_auth_session_verifier`.
   - The Wake API seals that one-time verifier plus the matching Neon challenge cookie into a
     short-lived AES-GCM handoff bound to the Android PKCE challenge. No usable session credential is
     placed in the deep link.
   - Android exchanges the handoff only by proving possession of the original PKCE verifier. The Wake
     API then calls Neon's `/get-session` with the verifier + challenge, verifies the authenticated
     user and returns only the managed `session_token` cookie's `name=value` pair.
   - Android stores that cookie encrypted with Android Keystore and replays it only to the Neon Auth
     origin for `/get-session`, `/token` and `/sign-out`.
   - Production uses WakeMyWay-owned Google OAuth credentials rather than Neon's shared development provider.

3. **WakeMyWay remains usable without an account.**
   - Sign-in is optional.
   - Auth initialization, refresh, outage or sign-out cannot gate Alarm Kernel or WakeRuntime.
   - Account UI lives under Profile rather than before the Home/alarm experience.

4. **The Wake API owns application authorization.**
   - Android sends a short-lived Neon user JWT to authenticated Wake API routes.
   - The API verifies Ed25519 signature, issuer, audience, expiry, authenticated role and UUID subject
     against the branch's public Neon JWKS.
   - `wmw_private.account_roles` stores explicit WakeMyWay roles: `user` or `admin`.
   - Missing role rows fail to the least-privileged `user` role.
   - Email, OAuth profile fields, client metadata and `neon_auth.user.role` are not Wake admin authority.

5. **Role lookup is exposed through `GET /api/v1/account/me`.**
   - It returns the authenticated account id, optional email and server-owned role.
   - Android renders admin affordances only after that response verifies `admin`.
   - Role lookup failure never changes local alarm functionality.

6. **Payments attach to the same immutable account id later.**
   - Subscription ownership/entitlement will use `neon_auth.user.id` as the Wake account id.
   - Google Play remains the purchase source of truth while WakeMyWay derives normalized entitlement
     server-side.
   - Admin authorization remains separate from purchase state.

## Consequences

- The managed backend is a dedicated WakeMyWay Neon project, while Vercel remains the Wake API host.
- Account-domain tables reference `neon_auth."user"(id)` but stay in the private
  `wmw_private` schema and are accessed by the Wake API only.
- The founder/admin grant can happen only after the intended Neon auth user exists and is an explicit
  UUID-based server-side role insert/update.
- Builds without Neon Auth configuration remain truthful and fully local.
- Production uses WakeMyWay-owned Google OAuth credentials and disables localhost auth access.
- Email/password is feature-gated off until custom SMTP and a verified-email flow are available.
- Future backup, payment, support and admin routes share one identity/authorization boundary without
  refactoring or weakening the Alarm Kernel.

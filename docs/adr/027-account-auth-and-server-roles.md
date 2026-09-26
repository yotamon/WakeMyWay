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
   - Email/password and Google identity are supported.
   - Android talks to the public Managed Better Auth API, never directly to PostgreSQL.
   - The opaque Better Auth session token is encrypted at rest with an Android Keystore AES-GCM key.
   - Android requests a short-lived Neon JWT only when an authenticated Wake API call needs one.
   - PostgreSQL credentials and other server/operator secrets are never shipped in Android.

2. **Google sign-in is native on Android.**
   - Android Credential Manager obtains a Google ID token using the production Web OAuth client id.
   - WakeMyWay submits that ID token and nonce to Neon Managed Better Auth.
   - There is no app-owned browser PKCE callback activity.
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
- Production launch requires WakeMyWay-owned Google OAuth credentials, production email delivery and
  verification policy even though Neon shared providers are sufficient for development.
- Future backup, payment, support and admin routes share one identity/authorization boundary without
  refactoring or weakening the Alarm Kernel.

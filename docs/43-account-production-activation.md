# WakeMyWay account production activation

This runbook activates the account/auth layer implemented by ADR 027. Identity, authorization,
backup and future commerce remain outside Alarm Kernel and Active Wake authority.

## 1. Managed backend

WakeMyWay uses the dedicated Neon project:

- project: `WakeMyWay`
- project id: `young-silence-67653683`
- production branch: `main`
- branch id: `br-jolly-math-b143muwu`
- region: Frankfurt / `aws-eu-central-1`
- database: `neondb`
- PostgreSQL: 17
- identity: Neon Managed Better Auth

Never place a PostgreSQL connection string, Neon management credential, Google client secret or
other server credential in Android.

## 2. Database migrations

Apply the account migrations to the production branch:

1. `001_consumer_backups.sql`
2. `003_account_roles.sql`

Apply `002_play_subscription_lifecycle.sql` later when the Play commerce lifecycle is activated.

Both account tables reference `neon_auth."user"(id)`. `wmw_private.account_roles` has no client
write path; missing role rows mean ordinary `user`.

## 3. Neon Managed Better Auth

Managed Better Auth is enabled on `main`.

Production currently uses Google as the only enabled consumer sign-in method:

1. WakeMyWay-owned Google OAuth credentials replace Neon's shared development keys.
2. Neon stores the Google Web OAuth client id/secret; the client secret never enters Android.
3. The Wake API pins `@neondatabase/auth` and uses its official server toolkit for OAuth proxying,
   session-challenge cookies and callback finalization.
4. `https://wakemyway.vercel.app` is the only production trusted origin currently required.
5. Localhost access is disabled on the production Neon Auth branch.
6. Email/password auth is disabled in production until WakeMyWay has a custom SMTP provider and
   a complete email verification UX.

Android starts Google OAuth in the system browser through the Wake API. The app creates a high-entropy
PKCE-style verifier, stores it encrypted with Android Keystore, and sends only its SHA-256 challenge
to the OAuth start route. Neon completes Google OAuth through its normal callback, then the official
server toolkit exchanges `neon_auth_session_verifier` for the Neon session cookie.

The Wake API returns to `wakemyway://auth` with only a two-minute AES-GCM encrypted handoff. The
opaque Neon session token is returned to Android only after the app proves possession of the
original verifier. Neither a Google token nor a Neon session token is placed in the deep-link URL.

If email/password auth is enabled later, production must first configure custom SMTP and required
email verification (OTP or link), then set `WMW_EMAIL_PASSWORD_AUTH_ENABLED=true`.

## 4. Wake API deployment

Vercel needs three server-side account values:

- `NEON_AUTH_BASE_URL`: the public Managed Better Auth base URL.
- `NEON_AUTH_COOKIE_SECRET`: a sensitive random server secret of at least 32 characters, used by
  the official Neon Auth proxy toolkit and as key material for the mobile handoff.
- `DATABASE_URL`: the server-only Neon PostgreSQL connection.

The database connection and cookie secret are server secrets and must never be copied to Android,
documentation values, release metadata or logs.

The Wake API verifies Neon JWTs against the public JWKS endpoint under the Managed Better Auth URL.
It requires EdDSA signature, the Neon Auth origin as issuer/audience, `role=authenticated`, a valid
expiry and a UUID subject.

Verify with a real signed-in user:

```text
GET /api/v1/account/me
Authorization: Bearer <short-lived Neon user JWT>
```

An authenticated user without an explicit Wake role row must return `role: "user"`.

## 5. Android/release configuration

Build-time GitHub Actions Variables:

```text
WMW_NEON_AUTH_URL=<public Managed Better Auth base URL>
WMW_EMAIL_PASSWORD_AUTH_ENABLED=false
WMW_ACCOUNT_API_BASE_URL=https://wakemyway.vercel.app
```

The Google Web OAuth client id/secret are provider configuration owned by Neon and Google, not Android
build configuration. The Android app contains no Google client secret and no database credential.

The official release workflow fails closed when account configuration is partial. Neon Auth and Wake
API URLs must be HTTPS. The production configuration is Google-only today; email/password remains
implemented but hidden and locally blocked unless its explicit build flag is enabled.

## 6. Founder/admin grant

First sign in normally with the intended founder account. Obtain its immutable
`neon_auth.user.id` UUID, then grant WakeMyWay admin by UUID only:

```sql
insert into wmw_private.account_roles (account_id, role)
values ('<neon-user-uuid>', 'admin')
on conflict (account_id)
do update set
    role = excluded.role,
    updated_at = now();
```

Never grant Wake admin from email, a Google profile, `neon_auth.user.role`, a client flag or
purchase state.

The Android Admin badge is valid only after `GET /api/v1/account/me` returns the server-owned
`admin` role.

## 7. Acceptance checklist

- production Google OAuth uses WakeMyWay-owned credentials rather than Neon shared keys;
- production localhost auth access is disabled;
- email/password auth is disabled until custom SMTP + verification UX are production-ready;
- the OAuth verifier and opaque session token are encrypted with Android Keystore;
- browser Google OAuth completes through the official Neon server proxy and returns to the app via
  the PKCE-bound handoff;
- sign-out invalidates the provider session and clears local session material;
- ordinary accounts return `user`;
- founder account returns `admin`;
- account/network failure never changes, cancels or blocks an already-local alarm;
- no server/database/provider secret is present in the APK;
- backup endpoints accept the same Neon identity;
- future payment entitlement attaches to the immutable Neon user UUID, not email.

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
3. Android uses AndroidX Credential Manager with the WakeMyWay Google Web OAuth client id to obtain
   a Google ID token. The Google client secret never enters Android.
4. Android submits that ID token and its nonce directly to Neon's Better Auth `/sign-in/social`
   endpoint. Better Auth verifies the Google token and returns the normal opaque Neon session token;
   no browser OAuth callback or Wake API handoff is required for current Android builds.
5. The Wake API keeps its pinned `@neondatabase/auth` server toolkit for server-side account/session
   work and for temporary backwards compatibility with older browser-handoff builds.
6. `https://wakemyway.vercel.app` is the production trusted origin used by WakeMyWay account API
   traffic.
7. Localhost access is disabled on the production Neon Auth branch.
8. Email/password auth is disabled in production until WakeMyWay has a custom SMTP provider and
   a complete email verification UX.

The native Google path intentionally avoids browser challenge cookies, callback verifier exchanges,
custom-scheme OAuth handoffs and browser-to-app state transfer. The resulting Neon session token is
stored encrypted with Android Keystore and is never placed in an intent or URL.

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
WMW_GOOGLE_WEB_CLIENT_ID=<WakeMyWay Google Web OAuth client id>
WMW_EMAIL_PASSWORD_AUTH_ENABLED=false
WMW_ACCOUNT_API_BASE_URL=https://wakemyway.vercel.app
```

The Google Web OAuth client id is public application configuration and is compiled into Android so
Credential Manager can request an ID token for the correct audience. The matching Google client
secret remains provider-side in Neon/Google and never enters the APK. The app contains no database
credential.

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
- Android Google sign-in uses Credential Manager and Better Auth ID-token sign-in without a browser
  callback;
- the opaque Neon session token is encrypted with Android Keystore;
- the Google ID token nonce is generated fresh for each native sign-in and verified by Better Auth;
- sign-out invalidates the provider session and clears local session material;
- ordinary accounts return `user`;
- founder account returns `admin`;
- account/network failure never changes, cancels or blocks an already-local alarm;
- no server/database/provider secret is present in the APK;
- backup endpoints accept the same Neon identity;
- future payment entitlement attaches to the immutable Neon user UUID, not email.

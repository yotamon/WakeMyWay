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

Apply `004_mobile_auth_handoffs.sql` for the server-only PKCE handoff table.
Apply `002_play_subscription_lifecycle.sql` later when the Play commerce lifecycle is activated.

Both account tables reference `neon_auth."user"(id)`. `wmw_private.account_roles` has no client
write path; missing role rows mean ordinary `user`.

## 3. Neon Managed Better Auth

Managed Better Auth is enabled on `main`.

Production currently uses Google as the only enabled consumer sign-in method:

1. WakeMyWay-owned Google OAuth credentials replace Neon's shared development keys.
2. Neon uses the WakeMyWay Google Web OAuth client id/secret.
3. Android Credential Manager uses the same Web client id as its server client id.
4. The Android OAuth client is registered for package `com.wakemyway.app` and the production
   signing certificate.
5. `https://wakemyway.vercel.app` is the only production trusted origin currently required.
6. Localhost access is disabled on the production Neon Auth branch.
7. Email/password auth is disabled in production until WakeMyWay has a custom SMTP provider and
   a complete email verification UX.

Android uses Credential Manager to obtain a Google ID token and nonce, then sends them to Neon's
`/sign-in/social` endpoint. This is Better Auth's supported native/mobile ID-token sign-in path.
No Google client secret is shipped in the APK.

If email/password auth is enabled later, production must first configure custom SMTP and required
email verification (OTP or link), then set `WMW_EMAIL_PASSWORD_AUTH_ENABLED=true`.

## 4. Wake API deployment

Vercel needs four server-side account values:

- `NEON_AUTH_BASE_URL`: the public Managed Better Auth base URL.
- `DATABASE_URL`: the server-only Neon PostgreSQL connection.
- `ACCOUNT_PUBLIC_BASE_URL=https://wakemyway.vercel.app`: canonical OAuth callback origin.
- `NEON_AUTH_COOKIE_SECRET`: a random 32+ character server-only secret used by the official Neon
  server toolkit and as key material for encrypted mobile handoffs.

The database connection is a server secret and must never be copied to Android, documentation,
release metadata or logs.

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

The official release workflow fails closed when Android account configuration is partial. Neon Auth
and Wake API URLs must be HTTPS. Google OAuth credentials remain entirely server/provider-side.

The official release workflow fails closed if Neon account infrastructure is configured without at
least one enabled sign-in method. The production configuration is Google-only today. Email/password
remains implemented but hidden and locally blocked unless its explicit build flag is enabled.

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
- the opaque session token is encrypted with Android Keystore;
- Google browser OAuth creates/loads the same Neon account boundary and the mobile handoff is
  single-use, five-minute and PKCE-bound;
- sign-out invalidates the provider session and clears local session material;
- ordinary accounts return `user`;
- founder account returns `admin`;
- account/network failure never changes, cancels or blocks an already-local alarm;
- no server/database/provider secret is present in the APK;
- backup endpoints accept the same Neon identity;
- future payment entitlement attaches to the immutable Neon user UUID, not email.

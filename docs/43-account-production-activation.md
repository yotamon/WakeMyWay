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

Development configuration may use Neon's shared Google and shared email providers, but production
must complete the Neon Auth production checklist:

1. configure a WakeMyWay-owned Google OAuth application;
2. configure that Google client id/secret in Neon Auth;
3. configure the native Android OAuth client for package `com.wakemyway.app` and the production
   signing certificate;
4. configure a production email provider;
5. enable an explicit email verification policy;
6. keep only required trusted domains/redirects and disable localhost for production.

Android uses Credential Manager to obtain a Google ID token and sends that token plus a nonce to
Neon's `/sign-in/social` endpoint. No Google client secret is shipped in the APK.

## 4. Wake API deployment

Vercel needs two server-side account values:

- `NEON_AUTH_BASE_URL`: the public Managed Better Auth base URL.
- `DATABASE_URL`: the server-only Neon PostgreSQL connection.

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
WMW_ACCOUNT_API_BASE_URL=https://wakemyway.vercel.app
```

The official release workflow fails closed when account configuration is partial. Neon Auth and Wake
API URLs must be HTTPS. The Google Web client id is public application configuration; the Google
client secret remains server/provider-side.

The Account entry stays hidden when Neon Auth is not configured. If Neon is configured before the
Google client id is available in a development build, email auth may work while the Google action is
truthfully disabled.

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

- email/password account creation works;
- email/password session survives app restart;
- the opaque session token is encrypted with Android Keystore;
- Google Credential Manager sign-in creates/loads the same Neon account boundary;
- sign-out invalidates the provider session and clears local session material;
- ordinary accounts return `user`;
- founder account returns `admin`;
- account/network failure never changes, cancels or blocks an already-local alarm;
- no server/database/provider secret is present in the APK;
- backup endpoints accept the same Neon identity;
- future payment entitlement attaches to the immutable Neon user UUID, not email.

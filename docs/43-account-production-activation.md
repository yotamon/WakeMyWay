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
2. Neon stores the Google Web OAuth client id/secret; neither value is required in the Android APK.
3. Android creates a high-entropy PKCE verifier, stores it encrypted with Android Keystore and opens
   the Wake API Google-start URL in the system browser.
4. The Wake API uses the pinned `@neondatabase/auth/server` proxy toolkit to request Neon's official
   social-login URL. The resulting Neon `session_challenge` cookie is rewritten for the Wake API
   origin and retained by the browser.
5. Google OAuth completes at Neon. Neon redirects the browser back to the Wake API with
   `neon_auth_session_verifier`.
6. The Wake API does not consume that one-time verifier in the browser callback. Instead it seals the
   verifier plus the matching Neon challenge cookie into a short-lived AES-GCM handoff bound to the
   Android PKCE challenge, then redirects to `wakemyway://auth`.
7. Android proves possession of the original PKCE verifier to the Wake API. Only then does the Wake
   API exchange Neon's verifier + challenge at `/get-session`, verify the returned user, and return
   the managed Neon `session_token` cookie's `name=value` pair to Android.
8. Android encrypts that managed session cookie with Android Keystore and replays it only to the Neon
   Auth origin for `/get-session`, `/token` and `/sign-out`.
9. `https://wakemyway.vercel.app` remains the only production trusted HTTP origin required by Neon.
10. Localhost access is disabled on the production Neon Auth branch.
11. Email/password auth is disabled in production until WakeMyWay has a custom SMTP provider and a
    complete email verification UX.

Neon Managed Better Auth currently returns the browser OAuth flow for `/sign-in/social`; WakeMyWay
must not assume the self-hosted Better Auth native `idToken` path is available on the managed Neon
service. The browser handoff above follows Neon's session-challenge/session-verifier protocol while
keeping the usable session credential out of the deep-link URL.

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

Android exchanges the managed session cookie at Neon Auth `/token` for a short-lived JWT. That JWT,
not the session cookie, is sent to Wake API endpoints in `Authorization: Bearer`. The Wake API
verifies Neon JWTs against the public JWKS endpoint under the Managed Better Auth URL. It requires
EdDSA signature, the Neon Auth origin as issuer/audience, `role=authenticated`, a valid expiry and
a UUID subject.

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

Google OAuth client configuration stays provider-side in Neon/Google and does not need to be
compiled into Android. The app contains no Google client secret, Neon management credential or
database credential.

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
- Android Google sign-in uses the Neon-supported browser OAuth + session-verifier protocol;
- the browser callback handoff is short-lived, AES-GCM encrypted and bound to an app-held PKCE
  verifier;
- the managed Neon session cookie is encrypted with Android Keystore and is sent only to Neon Auth;
- sign-out invalidates the provider session and clears local session material;
- ordinary accounts return `user`;
- founder account returns `admin`;
- account/network failure never changes, cancels or blocks an already-local alarm;
- no server/database/provider secret is present in the APK;
- backup endpoints accept the same Neon identity;
- future payment entitlement attaches to the immutable Neon user UUID, not email.

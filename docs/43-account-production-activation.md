# WakeMyWay account production activation

This runbook activates the account/auth layer implemented by ADR 027. It deliberately keeps identity,
authorization, backup and future commerce outside Alarm Kernel and Active Wake authority.

## 1. Provision one dedicated Supabase project

Use a WakeMyWay-owned Supabase project. Do not reuse an unrelated product database.

Required production values:

- project URL
- publishable key for Android
- pooled/server PostgreSQL connection for the Wake API

Never place a Supabase secret/service-role key or `DATABASE_URL` in Android.

## 2. Apply database migrations

Apply, in repository order:

1. `001_consumer_backups.sql`
2. `002_play_subscription_lifecycle.sql` when Play commerce lifecycle storage is enabled
3. `003_account_roles.sql`

`wmw_private.account_roles` intentionally has no client RLS policies. The Wake API connects
server-side and missing rows always mean ordinary `user`.

## 3. Configure Supabase Auth

Enable email/password authentication.

For Google:

1. create/configure the Google OAuth client for WakeMyWay;
2. enable Google in Supabase Auth with that client id/secret;
3. allow the Supabase provider callback URL in Google;
4. allow `wakemyway://auth` as the mobile redirect URL in Supabase.

Android uses PKCE. The Google client secret remains in Supabase/Google configuration and is never
shipped in the APK.

## 4. Configure the Wake API deployment

Server-side Vercel environment:

```text
SUPABASE_URL=https://<project-ref>.supabase.co
DATABASE_URL=<server-only pooled PostgreSQL connection>
```

Redeploy the Wake API after those values are present.

Verify:

```text
GET /api/v1/account/me
Authorization: Bearer <real Supabase user access token>
```

An authenticated account with no explicit role row must return `role: "user"`.

## 5. Configure Android builds

Build-time values:

```text
WMW_SUPABASE_URL=https://<project-ref>.supabase.co
WMW_SUPABASE_PUBLISHABLE_KEY=<publishable key>
WMW_ACCOUNT_API_BASE_URL=https://<WakeMyWay production API origin>
```

For official GitHub releases, configure those exact names as repository/environment **Actions
Variables**. The release workflow passes them into Gradle and fails closed when account configuration
is only partially supplied or either URL is not HTTPS. The Supabase publishable key is intentionally
public client configuration, not a service-role secret.

The Account entry is intentionally hidden when Supabase URL/key are absent, so development/release
builds never expose a fake login surface.

## 6. Create the founder/admin account safely

First sign in normally in WakeMyWay with the intended founder account. Then obtain its immutable
Supabase auth user UUID.

Grant the role by UUID only:

```sql
insert into wmw_private.account_roles (account_id, role)
values ('<auth-user-uuid>', 'admin')
on conflict (account_id)
do update set
    role = excluded.role,
    updated_at = now();
```

Do not grant admin by email, Google profile, `user_metadata`, `app_metadata` writable from client
flows, or a hard-coded Android flag.

Verify by reopening Account or refreshing it. The app should show the Admin badge only after
`GET /api/v1/account/me` returns a server-verified admin role.

## 7. Acceptance checklist

- email/password account creation works;
- email/password sign-in survives app restart;
- Google sign-in returns through `wakemyway://auth`;
- sign-out removes the active session;
- ordinary accounts return `user`;
- founder account returns `admin`;
- losing network/auth never changes, cancels or blocks an already-local alarm;
- no server/database/provider secret is present in the APK;
- backup endpoints accept the same user token;
- future payment entitlement attaches to the same immutable account id, not email.

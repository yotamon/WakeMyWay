# ADR 028: Account-authenticated invisible Realtime provisioning

**Status:** Accepted, hardened 2026-10-01  
**Date:** 2026-09-30

## Context

The anonymous zero-setup Realtime bootstrap from ADR 026 had the right consumer experience but the
wrong trust boundary: an installation id alone could reach a billable credential-minting path. PR
#162 therefore restored explicit founder pairing as an emergency hardening measure.

WakeMyWay now has working Neon account authentication on Android. That gives Direct Realtime the
missing server-verifiable principal without exposing an access code, OpenAI key, broker URL, or
setup screen to the user.

The initial account-backed implementation used a self-contained 90-day device bearer. Although that
credential was scoped and encrypted on Android, a copied token had no server-side revocation point.
The hardened design therefore makes the server ledger authoritative and keeps the signed bearer
short-lived.

## Decision

Direct Realtime provisioning is automatic, account-authenticated and server-revocable.

1. A signed-in Direct app obtains a short-lived Neon JWT through the existing account session.
2. In the background, Android sends that JWT plus its stable random installation UUID to
   `POST /api/v1/account/realtime-provision`.
3. WakeMyWay verifies the Neon JWT, rotates the private
   `wmw_private.account_realtime_devices` authorization row for that account + installation, and
   issues a 14-day HMAC-signed credential containing only a random credential id, installation id
   and pseudonymous account digest.
4. Android encrypts the scoped credential with Android Keystore and refreshes before expiry.
5. At wake time, Android presents that bearer to `POST /api/v1/account/realtime-token`.
6. WakeMyWay verifies both the HMAC and the matching active, unexpired, non-revoked database row
   before minting a short-lived OpenAI Realtime client secret.
7. Live audio then travels directly between Android and OpenAI over WebRTC. Vercel remains control
   plane only.
8. If the device credential is missing, expired, rotated or rejected, Android may re-provision once
   from the current account session. Any remaining failure degrades immediately to the selected
   local alarm.
9. Sign-out attempts authenticated `DELETE /api/v1/account/realtime-provision` before destroying
   the Neon session, then always clears the local credential. A network failure must never prevent
   account sign-out; the short credential lifetime bounds the residual risk.
10. Re-provisioning the same account + installation rotates the credential id atomically, so an old
    copied bearer is rejected even before its signed expiry.

There is **no consumer Realtime setup page and no private access code**. The old founder pairing
helpers are retained only in debug source sets for engineering diagnostics and are not packaged in
Direct release builds.

## Runtime shape

```text
WakeMyWay account sign-in / app start
        |
        +-- Neon session -> short-lived JWT
        |
        +-- POST /api/v1/account/realtime-provision
                |
                +-- private active-device row
                +-- 14-day scoped device credential -> Android Keystore

Voice Check-In wake
        |
        +-- scoped device credential
        +-- POST /api/v1/account/realtime-token
        +-- server ledger authorization check
        +-- short-lived OpenAI client secret
        +-- Android <-> OpenAI WebRTC

Sign-out
        |
        +-- DELETE /api/v1/account/realtime-provision
        +-- revoke server row
        +-- clear local credential

Any failure -> local alarm sound + local Stop/Snooze
```

## Security and privacy

- `OPENAI_API_KEY` and Realtime token-signing keys remain server-side.
- Provisioning cannot occur from an installation id alone; it requires a valid signed-in account.
- The signed device bearer is not sufficient by itself: the matching server authorization row must
  still be active and unexpired.
- The device credential contains a pseudonymous account digest, never raw email/name/account id.
- OpenAI client secrets remain short-lived and are minted only after the server authorization check.
- The OpenAI safety identifier is derived server-side from pseudonymous account + installation data.
- WakeMyWay does not persist raw Realtime microphone audio or transcripts in this flow.
- Account/Realtime networking uses one bounded coroutine-native OkHttp transport on Android; no
  standard server/OpenAI key is ever present on the device.
- Realtime remains optional enrichment and never becomes Alarm Kernel, Wake Runtime, Stop, Snooze,
  scheduling, or completion authority.

## OAuth callback hardening

Google sign-in returns to a verified HTTPS Android App Link under
`https://wakemyway.vercel.app/auth/mobile`. The PKCE-bound encrypted handoff is carried in the URL
fragment so it is not transmitted in the fallback HTTP request or normal server access logs.
`/.well-known/assetlinks.json` is fail-closed until the production signing-certificate
fingerprint(s) are configured. The legacy `wakemyway://auth` filter remains only as a migration
fallback for already-installed builds.

## Product boundary

Core alarms remain account-optional per ADR 007. The current Direct Realtime rollout requires a
signed-in WakeMyWay account because account authentication is the anti-abuse/authorization boundary
for billable voice. Play remains local-only until its broader Realtime entitlement rollout is
explicitly approved.

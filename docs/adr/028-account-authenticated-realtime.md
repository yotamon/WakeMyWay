# ADR 028: Account-authenticated invisible Realtime provisioning

**Status:** Accepted  
**Date:** 2026-09-30

## Context

The anonymous zero-setup Realtime bootstrap from ADR 026 had the right consumer experience but the
wrong trust boundary: an installation id alone could reach a billable credential-minting path. PR
#162 therefore restored explicit founder pairing as an emergency hardening measure.

WakeMyWay now has working Neon account authentication on Android. That gives Direct Realtime the
missing server-verifiable principal without exposing an access code, OpenAI key, broker URL, or
setup screen to the user.

## Decision

Direct Realtime provisioning is automatic and account-authenticated.

1. A signed-in Direct app obtains a short-lived Neon JWT through the existing account session.
2. In the background, Android sends that JWT plus its random installation UUID to
   `POST /api/v1/account/realtime-provision`.
3. WakeMyWay verifies the Neon JWT and issues a 90-day, HMAC-signed credential scoped only to
   account Realtime wake for that installation.
4. Android encrypts the scoped credential with Android Keystore.
5. At wake time, Android exchanges only the scoped device credential at
   `POST /api/v1/account/realtime-token` for a short-lived OpenAI Realtime client secret.
6. Live audio then travels directly between Android and OpenAI over WebRTC. Vercel remains control
   plane only.
7. If the device credential is missing or rejected, Android may re-provision it once from the
   current account session. Any remaining failure degrades immediately to the selected local alarm.
8. Signing out clears the local scoped Realtime credential.

There is **no consumer Realtime setup page and no private access code**. The old founder pairing
helpers are retained only in debug source sets for engineering diagnostics and are not packaged in
Direct release builds.

## Runtime shape

```text
WakeMyWay account sign-in / app start
        |
        +-- Neon session -> short-lived JWT
        |
        +-- /api/v1/account/realtime-provision
                |
                +-- scoped device credential -> Android Keystore

Voice Check-In wake
        |
        +-- scoped device credential
        +-- /api/v1/account/realtime-token
        +-- short-lived OpenAI client secret
        +-- Android <-> OpenAI WebRTC

Any failure -> local alarm sound + local Stop/Snooze
```

## Security and privacy

- `OPENAI_API_KEY` and Realtime token-signing keys remain server-side.
- Provisioning cannot occur from an installation id alone; it requires a valid signed-in account.
- The device credential contains a pseudonymous account digest, never raw email/name/account id.
- The OpenAI safety identifier is derived server-side from pseudonymous account + installation data.
- WakeMyWay does not persist raw Realtime microphone audio or transcripts in this flow.
- Realtime remains optional enrichment and never becomes Alarm Kernel, Wake Runtime, Stop, Snooze,
  scheduling, or completion authority.

## Product boundary

Core alarms remain account-optional per ADR 007. The current Direct Realtime rollout requires a
signed-in WakeMyWay account because account authentication is the anti-abuse/authorization boundary
for billable voice. Play remains local-only until its broader Realtime entitlement rollout is
explicitly approved.

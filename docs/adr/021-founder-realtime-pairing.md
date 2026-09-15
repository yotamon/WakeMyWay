# ADR 021: Founder Realtime uses scoped installation pairing

**Status:** Accepted  
**Date:** 2026-09-11  
**Amended:** 2026-09-15

## Context

PR #39 proved the architectural boundary for conversational Alfred, but its founder setup exposed internal infrastructure directly in Android: a broker URL plus `WMW_INTERNAL_API_KEY`. That was confusing for dogfood and unnecessarily placed a server-wide internal credential on the device.

A standard OpenAI API key must never ship in Android. OpenAI Realtime client credentials are short-lived and must be minted server-side. At the same time, the Wake alarm cannot depend on cloud authentication or network availability.

The first pairing implementation correctly kept the internal key off-device, but it also reused `WMW_INTERNAL_API_KEY` as the HMAC signing key for founder installation credentials. That unnecessarily coupled two credential domains: rotating admin/internal authorization also revoked founder devices, while compromise of one server secret expanded the blast radius into the other role.

## Decision

Founder/debug builds use a one-time **installation pairing** flow:

```text
Founder build
   ↓
Connect Alfred
   ↓
Founder access code
   ↓ HTTPS
WakeMyWay pairing endpoint
   ↓
signed, scoped, expiring installation credential
   ↓ encrypted with Android Keystore
future wake
   ↓
WakeMyWay Realtime broker
   ↓
short-lived OpenAI Realtime client secret
   ↓
WebRTC audio conversation
```

The app has a fixed WakeMyWay backend URL. The user never enters a broker URL, an OpenAI API key, `WMW_INTERNAL_API_KEY`, or a token-signing key.

Founder installation credentials are signed with the dedicated `WMW_FOUNDER_TOKEN_SIGNING_KEY`, are scoped to founder Realtime wake, and expire after 90 days. `WMW_INTERNAL_API_KEY` remains a separate server-only credential for internal/admin authorization. Rotating the founder signing key revokes paired founder installations without forcing rotation of internal API authorization, and rotating the internal API key does not silently change the installation-token trust domain.

The separate `WMW_FOUNDER_PAIRING_CODE` is used only for pairing. It must be high entropy, at least 24 characters, must stay out of source control, and can be rotated after successful pairing.

Before Android marks the connection Ready, it probes the full WakeMyWay → OpenAI client-secret path. A green UI state therefore means the broker accepted the installation credential and successfully minted a correctly shaped Realtime client secret.

## Non-negotiable boundaries

- `OPENAI_API_KEY` stays server-side.
- `WMW_INTERNAL_API_KEY` stays server-side and is not reused for installation-token signing.
- `WMW_FOUNDER_TOKEN_SIGNING_KEY` stays server-side and is never accepted as an API bearer credential.
- Realtime is optional enrichment and is never part of `Wake Ready`.
- Alarm timing, playback, Stop, Snooze, recovery, activation evidence, and completion authority remain local.
- If pairing expires, is revoked, the server is unavailable, or Realtime fails, the active Wake Session falls back to local Alfred.
- Founder pairing does not create a general user-authentication system. Production multi-user authentication remains a separate future decision.
- WakeMyWay does not persist Realtime audio or transcripts in this founder slice.
- Tomorrow Contract and prepared private context are not sent to Realtime in this founder slice.

## Rejected alternatives

### Put an OpenAI API key in the APK

Rejected. A mobile package cannot protect a reusable server API key from extraction.

### Embed `WMW_INTERNAL_API_KEY` in the APK

Rejected. That credential authorizes internal server routes beyond the narrow founder installation and would be extractable from the APK/device.

### Reuse `WMW_INTERNAL_API_KEY` as the installation-token signing key

Rejected after initial dogfood hardening. Authentication of internal server calls and signing of long-lived installation credentials are separate security roles and should support independent rotation and blast-radius containment.

### Make the token broker public

Rejected. An unauthenticated public endpoint could mint billable Realtime credentials for arbitrary callers.

### Continue asking the founder for broker URL + bearer token

Rejected. It leaks infrastructure concepts into product UX and encourages storage of a server-wide internal key on the phone.

## Operational setup

The WakeMyWay server requires:

- `OPENAI_API_KEY`
- `WMW_ENABLE_FOUNDER_REALTIME_DOGFOOD=true`
- `WMW_OPENAI_SAFETY_IDENTIFIER`
- `WMW_INTERNAL_API_KEY` (32+ random characters)
- `WMW_FOUNDER_TOKEN_SIGNING_KEY` (32+ random characters, distinct from `WMW_INTERNAL_API_KEY`)
- `WMW_FOUNDER_PAIRING_CODE` (24+ characters; high entropy required)

`GET /api/founder/realtime/status` exposes only safe readiness labels and never secret values.

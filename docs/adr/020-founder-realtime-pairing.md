# ADR 020: Founder Realtime uses scoped installation pairing

**Status:** Accepted  
**Date:** 2026-09-11

## Context

PR #39 proved the architectural boundary for conversational Alfred, but its founder setup exposed internal infrastructure directly in Android: a broker URL plus `WMW_INTERNAL_API_KEY`. That was confusing for dogfood and unnecessarily placed a server-wide internal credential on the device.

A standard OpenAI API key must never ship in Android. OpenAI Realtime client credentials are short-lived and must be minted server-side. At the same time, the Wake alarm cannot depend on cloud authentication or network availability.

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

The app has a fixed WakeMyWay backend URL. The user never enters a broker URL, an OpenAI API key, or `WMW_INTERNAL_API_KEY`.

The server signs founder installation credentials with `WMW_INTERNAL_API_KEY`. Credentials are scoped to founder Realtime wake and expire after 90 days. Rotating the server signing key revokes all paired founder installations.

The separate `WMW_FOUNDER_PAIRING_CODE` is used only for pairing. It must be high entropy, must stay out of source control, and can be rotated after successful pairing.

Before Android marks the connection Ready, it probes the full WakeMyWay → OpenAI client-secret path. A green UI state therefore means the broker accepted the installation credential and successfully minted a correctly shaped Realtime client secret.

## Non-negotiable boundaries

- `OPENAI_API_KEY` stays server-side.
- `WMW_INTERNAL_API_KEY` stays server-side.
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
- `WMW_FOUNDER_PAIRING_CODE` (12+ characters; high entropy recommended)

`GET /api/founder/realtime/status` exposes only safe readiness labels and never secret values.

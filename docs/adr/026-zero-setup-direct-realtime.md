# ADR 026: Zero-setup Direct Realtime installation auth

**Status:** Accepted  
**Date:** 2026-09-25

## Context

ADR 025 proved that the normal Direct APK can use OpenAI Realtime without giving Android a reusable
OpenAI key. The first production dogfood slice still required a private founder access code and a
visible setup screen. That is appropriate for bringing up infrastructure, but it is not acceptable
consumer UX: Realtime voice should behave like an app capability, not like an integration the user
must configure.

## Decision

Direct Realtime installation auth becomes automatic and invisible to the consumer.

- A Direct install keeps one random UUID installation id in credential-protected app preferences.
- On normal app startup, a distribution-scoped background pre-warmer requests a scoped installation
  credential from the existing WakeMyWay pairing endpoint. No model session is opened.
- If pre-warm has not completed by wake time, the Realtime adapter performs the same bootstrap
  lazily before requesting its OpenAI client secret.
- The scoped installation credential is encrypted with Android Keystore and reused until expiry.
- If the broker rejects a stale credential, Android clears it, automatically re-bootstraps once, and
  retries. The user is never asked to repair credentials.
- The private access-code field remains accepted only for legacy founder diagnostics; it is no
  longer required for server readiness and is not exposed in normal Direct UI.
- The Profile setup row and registered Direct setup activity are removed from consumer navigation.
- Play distribution remains local-only. This automatic bootstrap is still bounded by the private
  Direct Realtime dogfood gate.

## Runtime shape

```text
App opens
   |
   +-- local alarm stack is immediately usable
   |
   +-- background Direct pre-warm
           |
           +-- installation UUID
           +-- WakeMyWay scoped credential -> Android Keystore

Voice Check-In wake
   |
WakeRuntime / AlarmPlaybackService remain authoritative
   |
   +-- scoped credential -> short-lived OpenAI Realtime client secret
   |                         |
   |                         +-- direct Android <-> OpenAI WebRTC
   |
   +-- any failure -> local Alfred immediately
```

No OpenAI API key, Vercel secret, access code, terminal step, account, or manual pairing is part of
the consumer flow.

## Security boundary

Automatic bootstrap proves installation continuity, not public-store legitimacy. It is acceptable
for the private Direct dogfood distribution because the server gate remains the rollout boundary,
the issued token is scope-limited, OpenAI client secrets are short-lived, sessions are bounded, and
Realtime is never alarm authority.

A broader public rollout must add an entitlement and anti-abuse signal, such as verified Play
entitlement plus platform attestation or an equivalent server-verifiable mechanism. A static secret
embedded in the APK is explicitly rejected because it is extractable and would not provide a real
security boundary.

## Privacy and reliability

The installation id is random and pseudonymous. The server derives the OpenAI safety identifier
from that id; no name, email, account id, raw microphone audio, or transcript is persisted by this
auth flow. Live Realtime audio still goes directly between Android and OpenAI only during an active
conversation.

Provisioning, credential renewal, WakeMyWay cloud availability, and OpenAI availability are never
Wake Ready predicates. Local alarm delivery, Stop, Snooze, WakeRuntime, and local Alfred remain
available independently.

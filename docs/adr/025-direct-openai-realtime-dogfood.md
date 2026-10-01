# ADR 025: Direct-distribution OpenAI Realtime dogfood

**Status:** Accepted  
**Date:** 2026-09-25

## Context

WakeMyWay already has a founder/debug OpenAI Realtime WebRTC path behind
`WakeConversationEnrichment`, while the consumer wake path remains local-first. The founder needs
to use the real conversational experience from the normal Direct-distribution APK rather than a
debug lab, without making network AI part of alarm reliability or expanding the public Play surface.

## Decision

Promote the existing OpenAI Realtime adapter into the **Direct distribution only**.

- Direct APKs include WebRTC, `INTERNET`, and `MODIFY_AUDIO_SETTINGS`.
- Play APK/AAB builds remain local-only and do not package the Realtime adapter.
- The normal Wake Session discovers the Direct adapter through the existing enrichment factory.
- Realtime provisioning is invisible and account-authenticated per ADR 028. The standard
  `OPENAI_API_KEY` stays server-side; Android stores only a scoped expiring device credential in
  Android Keystore. Direct release builds expose no private access-code setup screen.
- Android obtains a short-lived Realtime client secret from WakeMyWay and then connects directly to
  OpenAI over WebRTC. Vercel is control plane only and never proxies live wake audio.
- WakeRuntime remains behavioral authority and AlarmKernel remains Stop/Snooze/alarm authority.
  Realtime failure immediately degrades to the normal selected local alarm sound and Stop/Snooze controls. Production does not substitute local TTS for a failed Realtime conversation.
- A Realtime session is bounded to a policy-derived 8-12 assistant turns and three minutes. Each
  user-facing response is capped at 1,024 output tokens and the conversation window uses
  retention-ratio
  truncation. A separate response timeout prevents a user-facing turn from hanging indefinitely.
- Stable Alfred instructions remain at the session prefix, and each user-facing response.create
  also carries the complete Alfred contract plus only the current WakeRuntime directive and bounded
  presentation preferences. This is intentional because response-level instructions can replace
  session-level response configuration for that turn; classifier responses stay isolated and
  out-of-band.
- Assistant playback has an explicit exactly-once lifecycle across response terminal events,
  output-buffer start/stop/clear ordering and barge-in. User speech commit and transport disconnect
  paths are also watchdog-bounded so a missing provider event cannot trap the UI in Speaking or
  Listening.
- Android 12+ Realtime audio selects the built-in speaker through the communication-device routing
  API when available and restores the prior route during teardown, with a legacy speakerphone
  fallback only when necessary.
- Account-authorized installations receive a stable pseudonymous OpenAI safety identifier derived
  server-side from pseudonymous account + installation data. No raw account, name, or email value is
  sent to OpenAI as the safety identifier.
- WakeMyWay does not persist Realtime microphone audio or raw transcripts.

## Why Direct only

This is a dogfood production slice, not a broad provider rollout. It lets the founder exercise the
real consumer wake path with release-like signing/update behavior while preserving the current
1.0 feature freeze for Play users. Public expansion still requires measured physical-device,
privacy, cost, and beta evidence.

## Reliability boundary

```text
Wake occurrence
    |
AlarmKernel + AlarmPlaybackService  (always local)
    |
WakeRuntime                         (always authoritative)
    |
    +-- Direct Realtime available -> OpenAI WebRTC speech enrichment
    |
    +-- unavailable / timeout / budget / provider failure -> local alarm-only
```

Realtime provisioning, network availability, OpenAI availability, and account state are never Wake
Ready predicates.

## Validation

Repository validation must include cloud auth/token tests plus Direct Android compile/lint gates.
Physical acceptance still requires locked-screen, cold-process, Wi-Fi/cellular, route changes,
Bluetooth, interruption, Stop/Snooze during a turn, and forced network/provider failure.

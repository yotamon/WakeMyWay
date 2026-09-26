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
- Pairing remains an explicit one-time private-access flow. The standard `OPENAI_API_KEY` stays
  server-side; Android stores only the scoped expiring installation credential in Android Keystore.
- Android obtains a short-lived Realtime client secret from WakeMyWay and then connects directly to
  OpenAI over WebRTC. Vercel is control plane only and never proxies live wake audio.
- WakeRuntime remains behavioral authority and AlarmKernel remains Stop/Snooze/alarm authority.
  Realtime failure immediately degrades to the normal selected local alarm sound and Stop/Snooze controls. Production does not substitute local TTS for a failed Realtime conversation.
- A Realtime session is bounded to eight assistant turns and three minutes. Each response is capped
  at 120 output tokens and the conversation window uses retention-ratio truncation.
- Stable Alfred instructions live at the session prefix. Per-turn requests contain only the current
  WakeRuntime directive and voice style, improving prompt stability and cost behavior.
- Paired installations receive a stable pseudonymous OpenAI safety identifier derived server-side
  from the scoped installation id. No raw account, name, or email identifier is sent.
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

Realtime setup, network availability, OpenAI availability, and pairing state are never Wake Ready
predicates.

## Validation

Repository validation must include cloud auth/token tests plus Direct Android compile/lint gates.
Physical acceptance still requires locked-screen, cold-process, Wi-Fi/cellular, route changes,
Bluetooth, interruption, Stop/Snooze during a turn, and forced network/provider failure.

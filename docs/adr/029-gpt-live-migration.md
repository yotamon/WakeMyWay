# ADR-029 — Introduce GPT-Live as a gated, distinct voice transport

**Status:** Accepted for experimental foundation; consumer promotion NOT authorized.
**Date:** 2026-10-20
**Owner:** WakeMyWay voice/core product
**Supersedes:** Nothing. Keeps ADR-025 and ADR-028 valid for the current Realtime production path.

## Decision

GPT-Live is **not** a model-string replacement for `gpt-realtime-2.1`. It is a new
full-duplex session protocol requiring different startup, playback, transcript,
delegation and terminal semantics. Implement a separate experimental transport
behind `WMW_GPT_LIVE_ENABLED=false` (absent means disabled).

The user-facing local Alarm Kernel, Stop/Snooze transactions and deterministic
WakeRuntime remain untouched until a real-device acceptance suite is green.
A server-side SDP broker uses existing revocable account-backed device
credentials. Android sends a WebRTC SDP offer to
`POST /api/v1/account/live-session`. The backend validates authorization,
reserves a limited billable admission, selects a reviewed character voice,
and posts `{session,transport:{type:"webrtc",sdp}}` to
`https://api.openai.com/v1/live/sessions` using the project key. Android
consumes `result.transport.sdp` and waits for `session.started`. Never send
`session.start` over the WebRTC data channel. Audio travels peer-to-peer,
not through WakeMyWay cloud.

## Why separate from existing adapter?

The current `WakeConversationEnrichment` assumes one `respond()` per
typed `SpeechIntent`, and `onAssistantSpeechFinished` closes the turn.
GPT-Live emits continuous audio and transcript fragments, **not** a reliable
per-turn 'response.done' replacement. Treating a transcript gap or acknowledgement
as speech end would introduce false runtime transitions. A new full-duplex
coordination contract must track actual Android audio playback, microphone input,
delegation and typed runtime directives separately. Do not silently synthesize
a speech-finished event from a timeout.

## Strict authority boundaries

- Alarm Kernel alone owns alarm audio, scheduling, Stop and Snooze.
- WakeRuntime alone advances phases, authorizes activity hints, and determines
  activation evidence. GPT-Live cannot set any of those facts.
- The selected Character and voice are immutable during one wake occurrence.
- No provider event by itself is a trusted motion or completed-activation signal.
- Generated speech is gated by Android playback policy. Prompts and a server
  sideband are **not** a substitute for an audio gate.
- A failure follows the existing one-way alarm-only degradation contract. No
  surprise late return to Live and no Android TTS imitation.
- Debug/lab sessions must not enter user learning/history/calibration.

## Server security/cost contract

- Signed, revocable, short-lived device authorization, checked before session create.
- No arbitrary voice IDs, prompts, models or tool definitions from the client.
- `store:false` for new Live sessions; no raw audio or transcript persistence.
- Founder account allowlist: `WMW_GPT_LIVE_ALLOWED_ACCOUNT_PSEUDONYMS`
  lists explicitly approved 40-character pseudonym digests; missing/empty denies
  everybody even if the global experimental switch is enabled.
- Durable per-account UTC daily admission counter, 20-second start cooldown,
  16 sessions/day by default. The ledger records counts, not conversation content.
- OpenAI key server-only; no provider response bodies, SDP, secrets, user speech,
  transcripts or private Tomorrow Contract values in logs.
- Fail closed on unsupported credentials, unexpected provider schema and HTTP errors.
- Project spend caps, quota, live-duration cap and concurrency admission are separate
  production gates. Daily session admissions do NOT bound total connected minutes.
- The WebRTC session initialization itself may incur 15 seconds of duration
  charge credited against normal duration billing. A failed provider request
  may still be chargeable; record safe aggregate costs.

## Voice creation/review

Voice creation is an operator action, not a consumer endpoint. Run the explicit
`scripts/create-wake-voice.mjs <character> --create` command only with an
authorized backend API key. Prompt-generated Voice IDs are only supported
with GPT-Live. No audio-sample cloning in this phase. Review each created voice
blindly against the built-in baseline in all intended locales, save a stable
Voice Profile version, then set `WMW_GPT_LIVE_VOICE_<CHARACTER>` to the
approved Voice ID. Never change the voice in the middle of a Live session.

## GPT-Live application lifecycle

1. User commits an alarm; no GPT-Live session required.
2. Alarm fires: local wake music plays immediately and controls remain usable.
3. Optional Live readiness: account/device authorization, capacity, session
   budget, foreground microphone eligibility, SDP exchange, `session.started`.
4. One immutable Session Plan controls permissions, Character, style and context.
5. The app sends only authorized small directives. Use `session.instructions.append`
   for behavior, `session.thinking.append` for facts, `session.commentary.append`
   only when speech is actually authorized. Append requires `delegation_id: null`
   for global context, and content <=500 tokens.
6. A client-delegation request is correlated by ID and may receive only verified
   app facts. Fail closed on unknown/unimplemented delegation requests.
7. End on terminal WakeRuntime/Stop/Snooze, max duration or error. Send
   `session.close`, await `session.closed` within a bounded grace and release
   WebRTC resources even when finalization fails. Final usage is marked unconfirmed
   when the transport dies first.

## Android engineering proof gates

- Full-duplex start/interrupt/overlap, listening, output gating, Stop/Snooze
  during speech, one session per occurrence.
- No persona change or duplicate utterances after activity recreation.
- Locked-screen, Doze, route changes, Bluetooth, alarm audio vs communication
  audio focus, wireless headset safety and revoked microphone permission.
- Unavailable provider, session timeout, forced disconnect and data-channel
  close return to controllable local sound without soft failure.
- Proven connection, first audible audio, budget, usage, microphone release,
  billing and latency measurements on physical devices before release.

## Rollout

1. Cloud contract + tests, feature off.
2. Independent Android GPT-Live lab probe; do not bind to consumer wake yet.
3. Full-duplex Wake Voice Coordinator with audio gate, real-device evidence.
4. Character catalog, true-preview and preference mapping.
5. Dogfood across 7-14 mornings; blinded naturalness/pleasantness evaluations.
6. Feature-flagged Direct internal, then Play rollout only after signed release
   and privacy, policy, store disclosure and quota checks.

## References

- https://developers.openai.com/api/docs/guides/live
- https://developers.openai.com/api/docs/guides/live-migration
- https://developers.openai.com/api/docs/guides/voice-webrtc
- https://developers.openai.com/api/docs/guides/live-conversations
- https://developers.openai.com/api/docs/guides/live-delegation
- https://developers.openai.com/api/reference/resources/audio/subresources/voices/methods/create

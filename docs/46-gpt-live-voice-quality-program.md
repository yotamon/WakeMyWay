# GPT-Live voice quality program

**Date:** 2026-10-20
**Current state:** engineering foundation in PR #193; no consumer launch.
**Objective:** Make the voice *pleasant, stable, personal and useful* over repeat
mornings, not merely technically conversational. OpenAI-only in this phase.

## Project truth

- Current shipped Direct integration: `gpt-realtime-2.1`, WebRTC,
  OpenAI Realtime ephemeral device credential and hard-coded Alfred prompt.
- `play` distribution does not yet ship Realtime dependencies.
- `AlarmDefinition.characterId` and explicit Wake Preferences already exist;
  only Alfred is currently a consumer production character.
- The app must never block alarm readiness on any voice/provider capability.
- The current product-quality reset prohibits net-new features except an explicit
  core-value exception. This program is such an exception for voice quality,
  not a general-purpose assistant expansion.

## Recommended execution order, expense first

| Gate | Scope | Exit |
|---|---|---|
| A | Cloud contract, config, credentials, provider error handling, durable admissions | Cloud CI green; default disabled |
| B | Android full-duplex Lab transport and microphone/audio gating | Real Android speaks/listens with no alarm side effects |
| C | One Alfred voice built-in vs prompt-created | Blind voice rating beats current baseline |
| D | Character continuity and low-pressure dialogue | Persona stable; no command loop; interrupts respected |
| E | Four-character shortlist with 2 reviewed pilots | Voice preview matches morning voice |
| F | Per-alarm character and preference mapping | Immutable occurrence; same Character on wake |
| G | Morning dogfood, accessibility, privacy and release | Safe for staged Direct then Play |

Avoid creating more than one custom voice candidate per character until an
audible reason to iterate is recorded. No redundant provider requests for
preview: short preview sessions and explicit stop/close. For ordinary text
and voice policy, avoid unnecessary backend Responses calls. Use client
delegation and typed application state. Keep connected sessions <=180 seconds
in the initial experiment; make this a hard client lifecycle limit and verify
actual provider usage. Daily session admission is not a connected-duration cap.

## Acceptance metrics (proposed targets, not measured results)

| Dimension | Target |
|---|---|
| Listening naturalness and pleasantness | >=4/5 mean, blinded |
| Same character across turns | >=95% sessions judged stable |
| False WakeRuntime state mutations by model | 0 |
| Unexpected Stop/Snooze changes | 0 |
| Uncontrolled voice auto-restart after failure | 0 |
| Per-user session cost | Published budget, measured, alerts in place |
| Physical alarm regression | 0 critical in device matrix |
| First audible speech, startup and interruptions | p50/p95 measured before thresholds frozen |
| Annoyance compared to current | Meaningful improvement over repeated mornings |
| Spoken word accuracy per locale | Reviewed English, German, Hebrew separately |

## Voice Lab ICE gathering correction (2026-10-21)

- The previous native Android Live Lab path used GATHER_CONTINUALLY while
  awaiting ICE COMPLETE. That can stall and produce ice-gathering-timeout
  before any provider admission (confirmed zero admissions in pilot logs).
- Live Lab now uses one-shot ICE candidate gathering, Cloudflare's public
  stun.cloudflare.com:3478 for NAT discovery, and a bounded 8-second
  candidate-rich fallback for implementations that do not emit COMPLETE.
  Cloudflare STUN sees the device's network address metadata but not audio.
- Never start the paid GPT-Live SDP session with no usable candidate,
  ICE password/username, DTLS fingerprint, and audio m-line.
- This is an opt-in pilot fix, not proof that real phones can successfully
  establish audio over all VPNs, mobile connections or restrictive firewalls.
  A managed TURN service remains future scope if measurable network failures
  justify its cost.

## Voice Lab transport timeout contract (2026-10-21)

The authorized Lab SDP request may wait on account authorization, Neon admission,
a cold Vercel function and OpenAI's 15-second upstream provider deadline. The
shared Android HTTP client previously imposed its own 15-second total deadline,
which could abort the phone's request before a healthy broker had returned,
leaving a reserved (potentially billable) session without a client.

- Keep ordinary app requests at the shared 15-second default.
- Give **only** the experimental Live SDP request a bounded 28-second call
  deadline; retain the server's 15-second provider deadline.
- Bound the sign-in/device credential preflight to 20 seconds before WebRTC
  touches the communication audio route.
- Prefer ICE COMPLETE; after 8 seconds, allow candidate-rich partial SDP,
  with no paid call if usable candidates or ICE credentials are missing.
  The subsequent WebRTC startup limit remains 50 seconds.
- Do not automatically retry a timed-out paid Live session.
- Any deadline or failure closes the experimental audio route, never changing
  the real Android Alarm Kernel or WakeRuntime.

These are engineering budgets, not observed p95 latency. A real phone and
provider-enabled pilot still must prove that sessions start and audio actually
plays; longer Lab timeouts are **not** approval for a 50-second consumer wake.

## Audible speech truth in Voice Lab (2026-10-21)

Provider transcript deltas, `session.started` and a negotiated audio track
cannot prove that sound was actually audible from the phone. Rating prompts
must therefore stay disabled until the real listener confirms hearing audio
after a completed/closed preview. A listener reporting silence receives the
copyable privacy-safe `no-audible-playback` diagnostic instead of a falsely
successful voice rating. No audio recordings, raw SDP, or transcripts are
persisted by this measurement gate. This is manual pilot feedback, not an
automatic first-audible-frame latency sensor.

## Live-first wake adapter (2026-10-21)

- The default voice enrichment in the Direct distribution prefers GPT-Live.
  Client-delegated speech uses only **typed, allowlisted** WakeRuntime speech
  intents. No model text may stop, snooze, confirm waking, or mutate the kernel.
- Live's full-duplex stream does not emit Realtime response.done. Remote decoded
  WebRTC PCM activity and bounded silence are used only for **best-effort**
  assistant speech callbacks, with a 1.4-second remote-silence grace to
  tolerate natural pauses. A user speaking over output marks the assistant
  turn interrupted; text transcript deltas never finish speech.
- User transcript fragments are grouped with a bounded quiet period into
  one low-confidence engagement observation. Speech content never triggers
  Stop/Snooze, and silence never increases activation evidence.
- On a pre-admission 403 pilot-access denial only, retain the previous
  Realtime implementation. On all potentially billable Live failures, use the
  local wake sound and controls without automatically opening another model.
- Keep the Live account pilot gate, daily quota and 180-second session budget.
  Do not treat an approved preview as proof of overnight reliability. Full
  consumer rollout and Play distribution require physical wake tests and cost
  evidence, not just successful compilation.

## Mandatory utterance scenario pack

1. First wake: 5-15-word invitation; no second instruction.
2. 'Five more minutes': natural acknowledgment; no fictitious Snooze.
3. 'Fuck off': unbothered, never punish or shame.
4. Already up: recognize statement without claiming sensor evidence.
5. Silent, cough, dog barking, side conversation: no fabricated response.
6. Interrupt assistant, self-correct, resume after pause: no second persona.
7. Complaint or joke: acknowledge, do not chain a physical to-do list.
8. Mumbled speech, multilingual wake, headset change: robust uncertainty.
9. Stop/Snooze while assistant speaks: user control is authoritative.
10. Network drop/provider 429/stalled session: revert to local alarm only.

## Product surface

- Alarm editor: character selection optional, default Alfred, local alarm
  remains independently scheduled.
- Compact cast: Alfred, Sam, Luna, Rocco in the voice lab; only promote
  reviewed characters. No celebrity voices or user audio cloning.
- Honest provider-matched Listen/Preview + Try a short conversation.
- Simple styles: energy, firmness, verbosity, humour and tested locale.
  These are bounded named presets, NOT fake percentage controls.
- Store explicit choices separately from learned Wake Profile.
- UI never shows raw provider model/voice IDs.
- Support TalkBack, captions, large text, no autoplay previews.

## Required engineering tasks

### A — Cloud base (this PR)
- [x] Server SDP endpoint with account credential reuse.
- [x] Feature off unless `WMW_GPT_LIVE_ENABLED=true`.
- [x] Character/voice allowlist, server-side prompts, `store:false`.
- [x] Validated SDP and bounded provider request.
- [x] Durable daily quota + retry cooldown migration and account-specific Neon pilot authorization (fallback to stricter Vercel allowlist mode).
- [x] Explicit operator voice creation script.
- [x] Unit tests for auth, denial and provider request.
- [x] Apply additive migration 005 on the WakeMyWay Neon production database;
  verified on 2026-10-20 that `wmw_private.live_session_admissions` exists,
  has RLS enabled and initially contained zero rows. Experiment remains off.
- [ ] Configure reviewed founder account pseudonym under
  `WMW_GPT_LIVE_ALLOWED_ACCOUNT_PSEUDONYMS` and turn the feature gate on
  only in an experimental environment. Derive pseudonym from the verified
  account ID using the project’s existing accountRealtimePseudonym function,
  never from email or an unverified client assertion.
- [ ] Verify OpenAI account actually has GPT-Live access.

### B — Android Live transport
- [ ] Direct-only Live WebRTC client, new `session.started` lifecycle.
- [ ] Explicit SDP broker authorization and credential renewal.
- [ ] Remote audio playback gate coupled to authorized Wake Directives.
- [ ] Input consent/eligibility, output focus, route, Bluetooth and teardown.
- [ ] Handle transcript fragments independently of wake turn progression.
- [ ] Client delegation correlation with finite, allowed capabilities only.
- [ ] Session usage and close accounting with 180-second hard deadline.

### C — Character quality
- [ ] Blind baseline ratings for existing OpenAI voice + built-in Vesper.
- [ ] Create one prompt voice candidate with approved account.
- [ ] Voice/profile versioning and explicit rollout selection.
- [ ] Prompt pack evaluated against saved morning scenario pack.
- [ ] Two tested characters before expanding to four.

### D — UX and launch
- [ ] Voice Lab and true preview, noncritical to alarm scheduling.
- [ ] Per-alarm character + style snapshot passed to Live backend.
- [ ] No flow requires account creation to make a regular alarm.
- [ ] Privacy policy, data retention and voice AI disclosures.
- [ ] Real-device overnight 7–14 day dogfood with failure traces.
- [ ] Direct internal rollout then Play integration / entitlement.

## Compatibility and migration principles

- Keep existing Realtime untouched and selectable while Live is experimental.
- Never set `WMW_OPENAI_REALTIME_MODEL=gpt-live-1`: wrong protocol.
- Never market voice quality based only on a TTS ranking.
- Do not interpret an output transcript fragment or gap as speech completion.
- Unknown Character IDs resolve to an approved stable default outside provider.
- The authoritative kernel never depends on voice, the cloud or account.
- Preserve current no-local-TTS consumer degradation policy until an explicit
  product decision supported by physical reliability evidence.

## Test/release commands

Cloud: `cd apps/cloud && npm ci && npm run check`.
Android: `cd apps/android && ./gradlew :app:testDirectDebugUnitTest :app:assembleDirectDebug :app:lintDirectDebug`.
Then run real-device Wake Lab matrix, verify session cost on OpenAI dashboard,
and check the connected microphone is released on all terminal paths.
No new release until all mandatory checks have physical evidence.

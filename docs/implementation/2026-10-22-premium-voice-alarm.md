# Premium voice alarm — implementation and acceptance contract

**Date:** 2026-10-22
**Status:** Code in `feat/premium-wake-voice-reliability-20261022`. This is NOT a physical reliability certification.
**Distribution:** GPT-Live is preferred in Direct; Play remains subject to existing distribution capability, privacy and entitlement gates.
**Safety boundary:** Alarm Kernel owns exact alarm execution, critical playback, native notification controls, Stop/Snooze and recovery. Voice never owns them.

## Implemented in this branch

1. **Authenticated, non-billable Live eligibility check.** The account-scoped GET endpoint returns 200 only when admission is allowed and 403 on permission denial. It never reserves quota or creates a session. The Android client performs it before creating a WebRTC peer. A 403 can fall back to legacy Realtime without risking a double-paid session. A legacy server returning 405 remains compatible; post-admission failures do NOT automatically start another provider.
2. **Remote audio turn hysteresis.** The WebRTC transport observes non-silent remote PCM and output-transcript activity. It reports a turn as quiet only after 2.8 seconds without sufficient PCM AND 1.6 seconds without output-transcript deltas. This avoids many false endings during a natural pause, but remains a playback proxy. There is no authoritative GPT-Live response-done event or proof of speaker audibility.
3. **Bounded connectivity degradation.** A disconnected peer/ICE state has a four-second grace period; a persistently unusable connection fails safely to native alarm playback. Session-generation fences and cleanup continue to prevent callbacks from older sessions changing current state.
4. **Serialized typed wake cues.** A legitimate runtime directive arriving during a user interruption is queued until the previous remote audio run becomes quiet. Queued turns have a bounded deadline. Unsolicited assistant output is not admitted into WakeRuntime.
5. **Human voice persona.** Stable per-session character identities and low-pressure, short, natural responses; no mandatory spoken compliance, fake Snooze, task cascades, or invented calendar importance.
6. **Truthful, private presentation.** Actual selected character name; transient bounded assistant captions shown only following remote PCM and when device unlocked; no transcript persistence. The listening prompt explicitly permits silence or movement without a spoken response.
7. **Bounded audio-state ownership.** The Android critical alarm service still owns music volume, while a smooth ~280-ms voice duck and ~360-ms restore replace hard instantaneous gain jumps. A finite voice-window lease restores critical gain after 12 seconds, even if UI or Live callbacks fail. Emergency tone never ducks. The controller explicitly restores music and degrades voice when the wake UI becomes hidden.
8. **Regression code.** Tests for PCM/transcript quiet timing and admission GET semantics, alongside the pre-existing wake runtime, alarm, consent and Live cue tests.

## Invariants not negotiable

- Android alarm fire and local sound must not require network, Google login, Live permission, a provider, or calendar.
- A failed Snooze reschedule never silences the active alarm.
- Stop never claims success until Alarm Kernel commits.
- Never convert model transcripts into terminal actions or user physical movement.
- No hidden-microphone recording, text persistence, or private calendar captions on a locked screen.
- Never retry a session that may have been admitted and billed merely to improve voice availability.
- Recover critical audio without a foreground UI or a working model.

## Required physical acceptance before release

Test the exact signed distribution on a real supported Android device and inspect privacy-safe Wake Lab traces. Repeat under screen-off, Doze, cold start, first wake after in-place upgrade, reboot-before-unlock, offline startup, Wi-Fi/cellular switching, calls, route/headset changes, microphone denial and device power-management restrictions.

For each run verify:

- Scheduled trigger delivers audible local music/vibration and accessible Stop/Snooze.
- A healthy admitted Live connection sends one permitted wake cue and renders speaker-audible speech; "session.started" or transcript deltas alone are insufficient proof.
- User interruption does not cause two overlapping personalities, repeated physical instructions, or orphaned assistant turns.
- Natural pauses and transcript delays do not prematurely advance the wake runtime.
- Provider 403 pre-admission can use the existing Realtime path; 429/network failures and post-admission errors use local alarm only.
- Backgrounding the wake UI releases the microphone, closes Live and restores critical playback.
- Private captions and approved calendar facts are hidden while the keyguard is locked.
- Stop/Snooze respond correctly during speaking/listening, with terminal services cleaned up.
- Repeated starts/stops and network failures do not leak WebRTC tracks, communication routing, focus, CPU wake locks or billable sessions.

## Measured release gates (targets, not claims)

- Zero unexpected Stop/Snooze and missed critical alarms across the supported physical matrix.
- Zero false physical evidence from model text; deliberately adversarial background speech tested.
- p50/p95 fire-to-native-audio, fire-to-first-audible-voice, Live setup, interruption latency and device-route recovery recorded.
- Blinded voice naturalness and character-continuity assessments across English, German and Hebrew.
- Repeated-morning annoyance, perceived helpfulness and real out-of-bed follow-through compared with alarm-only baseline.
- Provider admission counts and actual usage checked in the operator console before rollout; no automatic retries after potentially paid sessions.

## Known residual risks / follow-up decisions

- **Not proven:** Actual speaker audibility, native audio focus and Bluetooth/call behavior, background/Doze microphone behavior and on-device latency.
- **Turn-end heuristic:** A sufficiently long natural pause can still resemble end-of-output without a provider completion event. Keep physical trace-based tuning and consider a provider-native completion signal if one becomes available.
- **Wake effectiveness:** Coherent speech alone can contribute activation score. It is not biological proof of being out of bed; the product must evaluate this claim against real-world follow-through and accessibility.
- **Network posture:** STUN-only connectivity cannot guarantee success across restrictive networks; TURN should be justified by observed failure rates and cost.
- **Provider launch parity:** Play flavor and Live entitlements are separate distribution decisions; no general Play launch is implied.
- **Language:** English, German, Hebrew or following Android's system locale is now selectable for voice independently of UI language. Human native-speaker ratings, mixed-language speech and RTL font-scale field checks are not complete.
- **Testing:** No device measurements or claims are produced by writing this code. Comprehensive Android/Cloud CI and real-device acceptance remain mandatory before merging/releasing.

## Review-to-implementation reconciliation (2026-10-22)

| Reviewed area | Status | Remaining verification or work |
|---|---|---|
| Native alarm and terminal actions | Existing architecture preserved | Repeated OEM / reboot / permission-loss physical proof |
| Live access preflight and safe fallback | Implemented | Backend rollout and cost-accounting evidence |
| Unified startup budget | Implemented | Absolute 35s budget; p95 on supported phones not measured |
| Interruption and transcript lifecycle | Partially implemented | PCM/transcript hysteresis and deferred user evidence; no authoritative provider turn-end event |
| Critical sound coordination | Implemented, device-unverified | Real route switching, speaker audibility, calls and volume policy |
| Character and conversational personality | Implemented prompts, subjective quality unmeasured | Human voice evaluation across languages, complaints, barge-in and humor |
| Personalization | Implemented for existing profiles | Turn-length, motivation and pacing shape app cues; explicit voice-only language selection is persisted with backward-compatible defaults |
| Captions and wake UI | Implemented | Unlock-gated transient assistant text and character name; RTL and font-scale field checks |
| Audio echo/background speech | Conservative only | No proof that recognized voice belongs to the person; test background television, overlapping speakers |
| Quality observability | Local bounded metrics implemented | p50/p95 from device journal, no central fleet telemetry or true audio-route confirmation |
| Behavioral wake effectiveness | Not proven | Compare real out-of-bed follow-through and morning annoyance with alarm-only baseline |
| Signed release | Not completed | Real-device signoff, cloud deployment, operator-triggered release/signing |

CI verifies software contracts, not biologically confirmed wakefulness or physical speaker audibility. A green Android emulator is a release candidate, not a reliability guarantee.

## Additional hardening: fallback compatibility and usage accounting

- Legacy Realtime is only started after an explicit no-admission 403, **and only** when the selected persona is Alfred, the selected spoken locale is English, and enough startup budget remains for its separate connection. Other characters/languages retain the original local alarm sound rather than unexpectedly switching identities.
- GPT-Live usage seconds received from the provider are recorded in the privacy-safe on-device wake journal (with a final-report flag) and included in its report. This is a measured **duration proxy**, not OpenAI's billed currency, token breakdown, or proof that the sound reached the speaker.
- The local alarm remains independent of admission, transport and usage accounting.

## Real communication-route safety (code guard, not device acceptance)

If Android reports an active call or an existing communication session, Live must not claim the route and instead degrades without touching the local alarm. Once the configured built-in communication speaker is selected, unexpected route changes during incoming PCM fail the optional voice session and restore ordinary critical alarm playback. The speaker-route comparison is guarded by Android API level. This detects a changed OS route; it does not measure acoustic output, volume, wireless devices or transient underruns. Verify with real Bluetooth devices, media playback and phone calls.

## Opaque provider session IDs and commentary acknowledgments

Provider-assigned Live session IDs are opaque. Accept validated nonblank bounded IDs without assuming a `live_` prefix (including OpenAI's documented `sess_` examples). When a wake `session.commentary.append` is explicitly rejected, fail the optional voice turn without retrying a potentially admitted session. An acknowledged append only means the provider accepted context into the timeline; neither it nor an output transcript proves that the user heard speech. This distinction is retained in the diagnostics.

## Backend build compatibility, 2026-10-22

A Vercel preview exposed serverless-function compiler `TS2688` failures with TypeScript 7.0.2 that ordinary Cloud CI did not reveal. TypeScript was pinned to compatible 6.0.3 with a consistent package lock, and Node/pg build type packages are regular dependencies. A clean preview and Cloud CI remain release requirements; a Vercel `READY` label alone is insufficient. The preview is not production, may require authenticated SSO, and does not establish a real GPT-Live paid-audio call. Do not silently switch the approved character voices.

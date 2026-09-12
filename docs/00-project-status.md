# Project status

**Last updated:** 2026-09-12  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** physical founder dogfood of natural conversational Wake  
**Merged foundations:** PR #36 local Voice Wake; PR #37 modern Android presentation/BAL hardening; PR #38 permission-gated controllability; PR #39 Realtime conversation foundation; PR #40 seamless server-safe founder pairing  
**Reliability rule:** a wake may not be armed or resurrected without verified local terminal controllability  
**Realtime rule:** cloud conversation is optional enrichment only; Alarm Kernel and WakeRuntime remain authoritative

## Active hardening review

A repository-wide reliability/correctness review on `codex/deep-review-hardening` found and fixes three execution-level edge cases without changing the accepted architecture:

- Snooze previously cleared Active Wake authority before proving the replacement exact alarm had been accepted by Android. Snooze now registers the replacement first and only then performs the durable active → snoozed hand-off. If exact-alarm access or persistence fails, the current wake remains active instead of going silent without a replacement.
- A delayed/stale Stop or Snooze `PendingIntent` could stop `AlarmPlaybackService` even when the command occurrence ID no longer matched the current Active Wake. Terminal commands now tear down playback only after the Alarm Kernel accepts that exact occurrence; stale/malformed service commands either re-assert the current durable active execution or stop an idle service instance.
- Realtime conversation availability could diverge from `WakeRuntime.capabilities.speechAvailable` when the remote renderer connected or failed after runtime startup. The controller now synchronizes those transitions, starts immediately when Realtime becomes ready during TTS initialization, and emits a typed `SpeechFailed` fact when both remote and local rendering are unavailable.

Robolectric regression coverage now verifies that loss of exact-alarm access during Snooze leaves the current occurrence active and that stale terminal occurrence IDs cannot replace current active authority.

No ADR changes are required: these fixes enforce the existing Snooze durability, Active Wake idempotency, local fallback, and non-authoritative Realtime invariants.

## Physical truth

The permission/control regression found during founder testing was corrected by PR #38 and passed a physical Android phone wake test: the wake screen opened and the alarm was controllable.

The critical path remains local:

```text
Wake Setup
    ↓
WakeSchedulingGate
    ↓
Alarm Kernel
    ↓
AlarmManager.setAlarmClock()
    ↓
AlarmReceiver safety recheck
    ↓
AlarmPlaybackService safety recheck
    ├─ critical local alarm audio
    ├─ immediate Stop / Snooze controls
    └─ Android 15/16 BAL-safe full-screen PendingIntent
                         ↓
                    WakeActivity
                         ↓
              WakeVoiceSessionController
                         ↓
                    WakeRuntime
```

`Wake Ready` requires exact-alarm capability, notifications, HIGH alarm channel, full-screen alarm access, microphone permission and on-device speech recognition. Unsafe planned/active occurrences fail closed rather than starting an uncontrollable foreground alarm.

## Conversational Wake

PR #39 added optional founder/debug OpenAI Realtime WebRTC speech enrichment. ADR [`020`](adr/020-conversational-wake-enrichment.md) preserves the non-authoritative AI boundary.

```text
WakeRuntime
    ↓ typed SpeechIntent
WakeConversationEnrichment
    ├─ debug/founder OpenAI Realtime WebRTC
    │      ↕ natural audio conversation
    └─ deterministic local Alfred fallback
```

`SpeechIntent.KeepEngaging` allows repeated `Alfred → user → Alfred` turns while activation remains below threshold. The Realtime model may provide natural wording/audio and turn-boundary observations, but it cannot schedule/cancel alarms, Stop/Snooze, mutate WakePolicy, directly write activation evidence, decide completion, or become a Wake Ready dependency.

## Seamless founder pairing

PR #40 replaced the developer-only broker URL + reusable internal bearer form with a one-field `Connect Alfred` flow. Canonical decision: ADR [`021`](adr/021-founder-realtime-pairing.md).

```text
Tonight
   ↓
Conversational Alfred · Connect
   ↓
founder access code (once)
   ↓ HTTPS
WakeMyWay pairing endpoint
   ↓
scoped + expiring installation credential
   ↓ encrypted with Android Keystore
future Wake Session
   ↓
WakeMyWay Realtime broker
   ↓
short-lived OpenAI Realtime client secret
   ↓
WebRTC conversation
```

Security/product invariants:

- `OPENAI_API_KEY` stays server-side.
- `WMW_INTERNAL_API_KEY` stays server-side.
- Android has a fixed WakeMyWay backend URL and never asks for infrastructure URLs.
- Founder pairing uses a separate high-entropy access code and returns a scope-limited 90-day installation credential.
- Android encrypts the installation credential with Android Keystore.
- Before showing Conversational Alfred as Ready, Android probes the complete WakeMyWay → OpenAI client-secret path.
- Realtime setup does not alter `Wake Ready` and cannot block the local alarm.
- Failure/expiry/network loss falls back to local Alfred.
- No Tomorrow Contract or prepared private context is sent to Realtime in this founder slice.
- WMW does not persist Realtime audio or transcripts.

## Validation evidence

PR #40 final head `5b671692795a489ceb32c03251b87bc137d1a057` passed:

- Cloud AI Platform CI, including pairing/auth tests;
- Android wake-core tests and lint;
- instrumentation-test compile and debug APK assembly;
- curated Android visual regression;
- API-36 device reliability instrumentation.

PR #40 squash-merged to `main` as `4365eddddca899f14159b80d372935de011de556`.

## Current external deployment boundary

The GitHub merge is complete, but Vercel rejected the new production deployment because the Hobby account exceeded its rolling deployment quota (`more than 100` deployments in the platform window). The previously built preview containing the pairing backend is READY but protected by Vercel preview authentication, so Android correctly does not depend on it.

Production Realtime founder dogfood additionally requires these Vercel environment values:

- `OPENAI_API_KEY`
- `WMW_ENABLE_FOUNDER_REALTIME_DOGFOOD=true`
- `WMW_OPENAI_SAFETY_IDENTIFIER`
- `WMW_INTERNAL_API_KEY`
- `WMW_FOUNDER_PAIRING_CODE`

The available Vercel connector does not expose environment-variable mutation, so these secret values must be entered through an authorized Vercel environment settings surface. They must never be committed to Git or embedded in the APK.

Until production deployment and server configuration are complete, the local alarm and local Alfred continue working; Conversational Alfred must not claim Ready.

## Next physical proof

After the production backend is deployed/configured:

1. install the final founder debug APK;
2. Tonight → `Conversational Alfred` → `Connect Alfred`;
3. enter the founder access code once and approve the cloud-audio disclosure;
4. require the app to report `Ready` only after the server → OpenAI probe succeeds;
5. run the production-path T+2m Wake Lab wake and lock the phone;
6. verify at least two natural user/Alfred turns and barge-in;
7. verify network/provider loss falls back locally without affecting alarm controls;
8. verify Stop/Snooze stay immediate and local and audio routing restores after termination.

## Open boundaries

- Vercel production deployment quota and server environment configuration are the only blockers to first Realtime founder dogfood on the merged architecture;
- broader consumer authentication is not solved by founder pairing;
- OpenAI project privacy/data-control posture must be reviewed before broader rollout;
- release Android still intentionally has no Internet permission and no remote Realtime implementation;
- Bluetooth/audio-route behavior and motion calibration remain physical-device proof items;
- M7 live learning/journal wiring remains open.

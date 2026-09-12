# Project status

**Last updated:** 2026-09-12  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** physical founder dogfood of natural conversational Wake  
**Merged foundations:** PR #36 local Voice Wake; PR #37 modern Android presentation/BAL hardening; PR #38 permission-gated controllability; PR #39 Realtime conversation foundation; PR #40 seamless server-safe founder pairing; PR #41 critical-path reliability/security hardening  
**Reliability rule:** a wake may not be armed or resurrected without verified local terminal controllability  
**Realtime rule:** cloud conversation is optional enrichment only; Alarm Kernel and WakeRuntime remain authoritative

## Reliability hardening baseline

PR #41 completed a repository-wide reliability, correctness, security and performance review without changing product ownership boundaries:

- Snooze previously cleared Active Wake authority before proving the replacement exact alarm had been accepted by Android. Snooze now registers the replacement first and only then performs the durable active → snoozed hand-off. If exact-alarm access or persistence fails, the current wake remains active instead of going silent without a replacement.
- A delayed/stale Stop or Snooze `PendingIntent` could stop `AlarmPlaybackService` even when the command occurrence ID no longer matched the current Active Wake. Terminal commands now tear down playback only after the Alarm Kernel accepts that exact occurrence; stale/malformed service commands either re-assert the current durable active execution or stop an idle service instance.
- Realtime conversation availability could diverge from `WakeRuntime.capabilities.speechAvailable` when the remote renderer connected or failed after runtime startup. The controller now synchronizes those transitions, starts immediately when Realtime becomes ready during TTS initialization, and emits a typed `SpeechFailed` fact when both remote and local rendering are unavailable.
- Realtime WebRTC failures are scoped to the connection generation that observed them. A late callback from an obsolete peer/request cannot fail a newer reconnect, a current failure restores the previous Android audio route before local fallback, and duplicate in-flight connection attempts are suppressed.
- Shared cloud JSON parsing enforces the request-body byte limit while streaming instead of loading an unbounded body before checking its size. Oversized bodies are rejected early, including requests without `Content-Length` and multibyte UTF-8 payloads.
- Android CI explicitly runs `:app:testDebugUnitTest` in addition to `:wake-core:test`, so application-level Robolectric regressions are a first-class merge gate.

Robolectric regression coverage verifies that loss of exact-alarm access during Snooze leaves the current occurrence active and that stale terminal occurrence IDs cannot replace current active authority. Cloud tests cover bounded JSON parsing for normal, streamed oversized, declared oversized and multibyte bodies.

PR #41 final head `519227f3240f994d20dc0f6d7a271b70d00bd065` passed Android CI, application unit/Robolectric tests, lint/build, curated visual regression, API-36 device reliability, Cloud AI Platform CI and documentation validation. It squash-merged to `main` as `e34b202dabe2614672b5d27ea71bf90b679855ef`.

No ADR change was required: these fixes enforce existing Snooze durability, Active Wake idempotency, local fallback, non-authoritative Realtime and bounded-cloud-input invariants.

## Wake-session lifecycle hardening

PR #43 adds the second hardening pass around Android lifecycle continuity and surface-bound resources:

- `WakeVoiceSessionController` / `WakeRuntime` is owned by a keyed `WakeSessionViewModel`, so Activity configuration recreation does not reset activation evidence, escalation state, input deduplication, speech sequencing or the optional Realtime session.
- Alarm Kernel and `AlarmPlaybackService` remain the durable execution authority. The ViewModel retains only behavioral in-memory state across configuration recreation; process death intentionally starts a fresh behavioral session rather than persisting evidence that could falsely imply the user is already awake.
- voice listening and motion observation are represented as runtime-requested resources. When the Wake Surface becomes hidden, actual microphone/Realtime input and motion sensors are suspended. When it becomes visible again, only resources still requested by the runtime are restored.
- a Realtime failure while voice input is requested preserves that request so local on-device STT can take over instead of silently losing the turn.
- founder pairing now requires an access code of at least 24 characters consistently in the Android setup UI, Android pairing client, cloud endpoint schema and server readiness/auth logic.
- reliable pairing brute-force protection is intentionally not implemented as a process-local serverless map. Issue #42 tracks the required Vercel Firewall rate-limit rule for `POST /api/founder/realtime/pair`.

`WakeSessionViewModelTest` verifies that a recreated Activity provider backed by the same `ViewModelStore` receives the same behavioral session/controller, creates it only once and closes it exactly once when the owner is truly cleared.

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
                WakeSessionViewModel
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
founder access code (once, 24+ chars)
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
- Founder pairing uses a separate high-entropy access code with a 24-character minimum and returns a scope-limited 90-day installation credential.
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

PR #41 passed the same repository gates plus the new explicit Android application unit/Robolectric gate and squash-merged to `main` as `e34b202dabe2614672b5d27ea71bf90b679855ef`.

PR #43 adds retained-session lifecycle tests and the stronger founder pairing policy. Its exact final-head CI evidence is recorded in the pull request before merge.

## Current external deployment boundary

The previous Hobby deployment-quota blocker is no longer current. Vercel successfully built a production deployment for PR #41/main (`e34b202dabe2614672b5d27ea71bf90b679855ef`) and reports that deployment `READY`.

However, the production hostname currently returns Vercel `404 NOT_FOUND` for `GET /api/founder/realtime/status`. The same 404 occurs on the PR #43 preview. The repository already documents the required Vercel project configuration in `apps/cloud/README.md`: **Root Directory must be `apps/cloud`**. The current Vercel project is deploying the repository root instead, so the nested cloud `/api` functions are not exposed.

Issue #44 tracks the required platform correction: set the existing `wakemyway` Vercel project Root Directory to `apps/cloud`, redeploy `main`, then verify `/api/health` and `/api/founder/realtime/status` return the expected cloud responses.

After that routing/root correction, production Realtime founder dogfood also requires the appropriate Vercel environment configuration, including:

- `OPENAI_API_KEY`
- `WMW_ENABLE_FOUNDER_REALTIME_DOGFOOD=true`
- `WMW_OPENAI_SAFETY_IDENTIFIER`
- `WMW_INTERNAL_API_KEY`
- `WMW_FOUNDER_PAIRING_CODE` with at least 24 characters

Secret values must be entered only through an authorized Vercel settings surface. They must never be committed to Git or embedded in the APK.

Issue #42 separately tracks edge rate limiting for the public pairing exchange. A process-local limiter is not considered a valid security boundary for horizontally scaled/ephemeral Vercel Functions.

Until #44 and the required server configuration are complete, the local alarm and local Alfred continue working; Conversational Alfred must not claim Ready.

## Next physical proof

After the production cloud root/configuration is corrected:

1. verify production `GET /api/health` and `GET /api/founder/realtime/status` are reachable and the latter reports `available: true`;
2. configure and verify the Vercel Firewall pairing rate limit from #42;
3. install the final founder debug APK;
4. Tonight → `Conversational Alfred` → `Connect Alfred`;
5. enter the founder access code once and approve the cloud-audio disclosure;
6. require the app to report `Ready` only after the server → OpenAI probe succeeds;
7. run the production-path T+2m Wake Lab wake and lock the phone;
8. verify at least two natural user/Alfred turns and barge-in;
9. background/foreground or recreate the Wake Surface and verify the behavioral session continues while microphone/motion resources suspend when hidden;
10. verify network/provider loss falls back locally without affecting alarm controls;
11. verify Stop/Snooze stay immediate and local and audio routing restores after termination.

## Open boundaries

- Vercel project Root Directory is currently incorrect for the nested cloud app; tracked by #44.
- Vercel Firewall brute-force protection for founder pairing is tracked by #42.
- required production Realtime environment configuration must be verified only after the cloud routes are actually exposed;
- broader consumer authentication is not solved by founder pairing;
- OpenAI project privacy/data-control posture must be reviewed before broader rollout;
- release Android still intentionally has no Internet permission and no remote Realtime implementation;
- Bluetooth/audio-route behavior and motion calibration remain physical-device proof items;
- M7 live learning/journal wiring remains open.

# Project status

**Last updated:** 2026-09-11  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** founder dogfood of natural conversational Wake  
**Active work:** PR #40, seamless server-safe Conversational Alfred pairing  
**Merged foundations:** PR #36 local Voice Wake; PR #37 modern Android presentation/BAL hardening; PR #38 permission-gated controllability; PR #39 founder Realtime conversation foundation  
**Reliability rule:** a wake may not be armed or resurrected without verified local terminal controllability  
**Realtime rule:** cloud conversation is optional enrichment only; Alarm Kernel and WakeRuntime remain authoritative

## Physical truth

The permission/control regression found during founder testing was corrected by PR #38 and then passed a physical Android phone wake test: the wake screen opened and the alarm was controllable.

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

## Conversational Wake foundation

PR #39 added an optional founder/debug OpenAI Realtime WebRTC speech-enrichment path.

```text
WakeRuntime
    ↓ typed SpeechIntent
WakeConversationEnrichment
    ├─ debug/founder OpenAI Realtime WebRTC
    │      ↕ natural audio conversation
    └─ deterministic local Alfred fallback
```

A `SpeechIntent.KeepEngaging` allows repeated `Alfred → user → Alfred` turns while activation remains below threshold. The Realtime model may provide natural wording/audio and turn-boundary observations, but it cannot schedule/cancel alarms, Stop/Snooze, mutate WakePolicy, directly write activation evidence, decide completion, or become a Wake Ready dependency.

## PR #40: seamless founder pairing

The initial PR #39 founder setup exposed a broker URL and reusable internal bearer credential. PR #40 replaces that developer-only form with a one-field product flow.

Canonical decision: [`adr/020-founder-realtime-pairing.md`](adr/020-founder-realtime-pairing.md).

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

- `OPENAI_API_KEY` remains server-side only.
- `WMW_INTERNAL_API_KEY` remains server-side only.
- the Android app has a fixed WakeMyWay backend URL and never asks for infrastructure URLs;
- founder pairing uses a separate high-entropy access code and returns a scope-limited 90-day installation credential;
- Android encrypts the installation credential with Android Keystore;
- before showing Conversational Alfred as Ready, Android probes the complete WakeMyWay → OpenAI client-secret path;
- rotating the server signing key revokes paired founder installations;
- Realtime setup does not alter `Wake Ready` and cannot block the local alarm;
- failure/expiry/network loss falls back to local Alfred;
- no Tomorrow Contract or prepared private context is sent to Realtime in this founder slice;
- WMW does not persist Realtime audio or transcripts.

## Server configuration required for founder dogfood

The Vercel backend must provide:

- `OPENAI_API_KEY`
- `WMW_ENABLE_FOUNDER_REALTIME_DOGFOOD=true`
- `WMW_OPENAI_SAFETY_IDENTIFIER`
- `WMW_INTERNAL_API_KEY` (server auth/signing key)
- `WMW_FOUNDER_PAIRING_CODE` (one-time founder bootstrap code)

`GET /api/founder/realtime/status` exposes only safe missing-configuration labels, never secret values.

## Validation before physical dogfood

PR #40 must pass on one final head:

1. Cloud AI Platform typecheck + unit tests, including pairing/auth tests;
2. Android wake-core tests;
3. Android lint;
4. instrumentation-test compile + debug APK assembly;
5. curated visual regression without weakening existing baselines;
6. API-36 device reliability suite;
7. deployed backend status/probe validation.

Then physical founder testing must verify:

1. Tonight exposes only `Connect Alfred`, never broker/internal/OpenAI credentials;
2. one access-code pairing succeeds once and persists securely;
3. status changes to Ready only after server → OpenAI probing succeeds;
4. a locked-screen wake still opens and remains immediately controllable;
5. at least two natural user/Alfred turns work;
6. barge-in does not deadlock;
7. provider/network failure falls back to local Alfred;
8. Stop/Snooze remain immediate and local;
9. audio route is restored after terminal action.

## Open boundaries

- production Vercel environment must be configured before founder Realtime can report Ready;
- broader consumer authentication is not solved by founder pairing and remains future work;
- OpenAI project privacy/data-control posture must be reviewed before broader rollout;
- release Android still intentionally has no Internet permission and no remote Realtime implementation;
- Bluetooth/audio-route behavior and motion calibration remain physical-device proof items;
- M7 live learning/journal wiring remains open.

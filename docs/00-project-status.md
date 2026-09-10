# Project status

**Last updated:** 2026-09-10  
**Product:** Wake My Way (WMW)  
**Platform:** Android first, optional non-critical Vercel cloud  
**Current engineering phase:** founder dogfood of natural conversational Wake  
**Merged foundations:** PR #36 local Voice Wake; PR #37 modern Android presentation/BAL hardening; PR #38 permission-gated controllability; PR #39 founder conversational Realtime enrichment  
**Current reliability rule:** a wake may not be armed or resurrected without verified terminal controllability  
**Realtime rule:** cloud conversation is enrichment only; Alarm Kernel and WakeRuntime remain authoritative

## Current physical truth

The permission/control regression exposed by founder testing has been corrected and subsequently passed a physical phone test: the wake screen opens again and the alarm is controllable.

The stable wake path remains:

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

PR #39 adds a founder/debug Realtime speech-enrichment path while preserving the stable alarm boundary.

Canonical decision: [`adr/020-conversational-wake-enrichment.md`](adr/020-conversational-wake-enrichment.md).  
Implementation note: [`implementation/conversational-wake.md`](implementation/conversational-wake.md).  
Dogfood checklist: [`implementation/conversational-wake-testing.md`](implementation/conversational-wake-testing.md).

```text
WakeRuntime
    ↓ typed SpeechIntent
WakeConversationEnrichment
    ├─ debug/founder OpenAI Realtime WebRTC
    │      ↕ audio conversation
    └─ deterministic local Alfred fallback
```

The Realtime model owns wording/audio quality only. It cannot schedule/cancel alarms, Stop/Snooze, mutate WakePolicy, directly write activation evidence, decide wake completion or become a Wake Ready dependency.

A new `SpeechIntent.KeepEngaging` allows WakeRuntime to explicitly continue the conversation after a coherent spoken reply while activation remains below threshold:

```text
Alfred speaks
   ↓
user replies
   ↓
VoiceResponseObserved
   ↓
WakeRuntime
   ├─ activation below threshold → Speak(KeepEngaging) → listen again
   └─ activation satisfied       → orientation → completion
```

The founder Realtime adapter uses WebRTC audio-to-audio, server VAD for turn-boundary observation, automatic provider response creation disabled, and barge-in enabled. If Realtime is unavailable or fails, the current typed speech intent falls back to local Alfred.

Founder broker configuration is debug-only. The operator token is encrypted locally with Android Keystore and must never be treated as consumer authentication. No Tomorrow Contract, calendar or other private wake context is sent in this founder slice.

## Current validation boundary

PR #38 passed Android CI, visual regression, API-36 device reliability and a successful physical founder-phone wake test.

PR #39 was merged as founder/debug conversational dogfood. Cloud CI had already passed before merge; Android workflows were running on the PR head at merge time. Physical Realtime behavior is still a separate proof gate.

Do not promote remote conversation to release builds until physical testing verifies:

1. locked-screen alarm presentation remains reliable;
2. Realtime connection never delays critical alarm audio;
3. at least two natural `user → Alfred → user` turns work;
4. barge-in works without deadlock;
5. network/provider failure immediately falls back to local Alfred;
6. Stop and Snooze remain immediate and local;
7. audio routing is restored after terminal action;
8. no transcript/private wake content is persisted by WMW.

## Open proof boundaries

- physical founder-phone validation of PR #39 Realtime conversation;
- provider/account privacy and data-control posture before any broader product rollout;
- production-grade consumer authorization for remote speech;
- Bluetooth/audio-route behavior;
- motion threshold calibration;
- M7 live learning/journal wiring;
- provider/transport cost and latency evidence before permanent Realtime selection.

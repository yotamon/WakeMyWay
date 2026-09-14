# Project status

**Last updated:** 2026-09-15  
**Product:** WakeMyWay (WMW)  
**Platform:** Android first; optional non-critical Vercel cloud  
**Current engineering phase:** reliability/product hardening + founder physical dogfood  
**Current hardening PR:** #54  
**Reliability rule:** future scheduling readiness, active execution safety, voice readiness and Snooze readiness are separate predicates  
**Realtime rule:** cloud conversation is optional enrichment only; Alarm Kernel remains durable alarm/terminal authority and WakeRuntime remains behavioral activation/orientation authority

## Current product shape

WakeMyWay is a local-first Android wake system with a deterministic behavioral runtime and optional conversational enrichment.

```text
Wake Setup
    ↓ strict new-Wake preflight
Alarm Kernel
    ↓
AlarmManager.setAlarmClock()
    ↓
AlarmReceiver
    ↓ active-execution safety check
AlarmPlaybackService
    ├─ bundled critical alarm audio
    ├─ notification terminal controls
    └─ full-screen WakeActivity
                ↓
        WakeSessionViewModel
          ├─ acknowledged Stop/Snooze → Alarm Kernel
          └─ behavioral session
                     ↓
          WakeVoiceSessionController
                     ↓
                WakeRuntime
       ├─ local Alfred
       ├─ on-device voice replies
       ├─ motion evidence
       └─ optional debug/founder Realtime enrichment
```

The sunrise-wave consumer brand from PR #51 is the current presentation truth. Planning surfaces are light/premium; active wake remains intentionally darker and resolves toward morning light. The same sunrise-wave geometry is used by the launcher identity and the stateful Wake Line.

## Reliability hardening in PR #54

The deep application review identified readiness and authority boundaries that had become broader than necessary. PR #54 hardens them without weakening alarm safety:

- **New Voice Wake creation stays strict.** Exact-alarm capability, notifications, HIGH active-wake channel, full-screen access, microphone permission and on-device recognition are required before committing a new Voice Wake.
- **Existing safe alarms survive later voice degradation.** Losing microphone/on-device recognition after scheduling no longer silently deletes an otherwise controllable alarm. Voice degrades to the remaining local capabilities.
- **Active wakes do not depend on future exact scheduling.** Once Android delivers an occurrence, active execution safety is notification/channel/full-screen controllability. Losing exact-alarm capability does not silence the current wake; Snooze remains fail-closed because it requires a durable exact replacement.
- **Stop/Snooze have one acknowledged live UI path.** `WakeSessionViewModel` calls `WakeTerminalActions`, which commits the Alarm Kernel transaction before releasing behavioral resources or dismissing the Wake Surface. Duplicate terminal actions are suppressed; rejected/failed Snooze keeps the current wake visible, audible and controllable.
- **Critical Direct-Boot corruption is diagnosable.** `CriticalWakeStore` distinguishes `Missing` from `Corrupt` while mutation paths remain fail-closed. Alarm Health now reports an unreadable critical state instead of presenting corruption as an ordinary empty setup.
- **The branded wake surface is adaptive.** Canonical 393×852 composition is preserved while decorative vertical rhythm and large Wake Line regions compress on shorter devices. A compact 360×640 render smoke test supplements the canonical golden set.
- **Schedule controls meet the sleepy-use touch target.** Day selectors retain their branded 40dp visual circle inside a 48dp interactive target, compact-width planning uses narrower brand spacing so all seven targets fit at 360dp, and summary copy is resource-backed rather than hardcoded English.

Canonical semantics are recorded in ADR 019.

## Alarm and privacy invariants

The critical wake path remains fully local and usable without cloud access.

- Alarm scheduling, playback, Stop, Snooze, recovery and Direct Boot do not depend on Vercel, OpenAI, Supabase or an account.
- Snooze replacement is registered before Active Wake authority is released; failure leaves the current wake active.
- stale occurrence IDs cannot stop or replace a newer Active Wake.
- task dismissal is not a terminal alarm action; a healthy foreground alarm survives it only while immediate verified terminal controls remain reachable.
- private Tomorrow Contract / Prepared Wake Plan content stays in credential-protected `noBackupFilesDir`, is never copied into the device-protected critical snapshot, is not read while locked, and is protected by `FLAG_SECURE` when rendered.
- raw microphone audio and raw high-frequency motion samples are not persisted by the local wake path.

## WakeRuntime and terminal authority

`WakeRuntime` owns deterministic behavioral activation/orientation decisions and typed evidence. `AlarmKernel` owns durable alarm scheduling/state and real terminal Stop/Snooze mutations. `AlarmPlaybackService` owns foreground playback, notification actions and playback teardown/recovery.

The production Wake Surface does not disappear on a fire-and-forget terminal request. `WakeTerminalActions` first commits Stop or the durable Snooze replacement in `AlarmKernel`; only success is acknowledged back to `WakeSessionViewModel`, which then releases behavioral resources and closes the surface. Notification actions remain independently safe through the service path.

The pure runtime still contains typed terminal protocol concepts for deterministic replay/testing and future journal integration, but those concepts do not become a second durable execution authority. Live terminal results should be observed by future journal/learning integration.

M7 local learning core exists, but real-session journal persistence, calibration and learned-policy selection remain tracked in #27 / #21.

## Conversational Alfred

Founder/debug OpenAI Realtime over WebRTC is optional enrichment. It may render natural speech and report turn boundaries, but it cannot:

- schedule/cancel alarms;
- Stop/Snooze execution;
- mutate WakePolicy;
- directly create activation evidence;
- decide Wake completion;
- become a Wake Ready dependency.

Local Alfred remains the fallback when cloud/network/provider setup fails.

Founder installation pairing uses a one-time 24+ character access code and returns a scoped 90-day installation credential stored through Android Keystore. PR #54 separates server credential roles:

- `WMW_INTERNAL_API_KEY` → internal/admin API authorization;
- `WMW_FOUNDER_TOKEN_SIGNING_KEY` → HMAC signing of founder installation credentials;
- `WMW_FOUNDER_PAIRING_CODE` → one-time installation pairing secret.

ADR 021 is the canonical pairing/security decision.

## Cloud deployment status

The previous Vercel Root Directory problem is resolved; issue #44 is closed. Production cloud routes are exposed from `apps/cloud`.

Founder Realtime production readiness is still intentionally incomplete:

- #47 tracks Production-scoped environment configuration, including the new dedicated `WMW_FOUNDER_TOKEN_SIGNING_KEY`;
- #42 tracks Vercel Firewall rate limiting for `POST /api/founder/realtime/pair`;
- secret values must be configured only through authorized Vercel settings and never committed to Git or embedded in Android;
- release Android intentionally has no Internet permission / remote Realtime implementation yet.

The local alarm and local Alfred remain valid regardless of those cloud blockers.

## Automated quality gates

Repository quality gates include:

- pure `:wake-core` unit tests;
- Android app unit/Robolectric tests;
- lint + debug assembly + instrumentation compilation;
- curated Roborazzi visual regression;
- API-36 device reliability instrumentation;
- Cloud AI Platform tests;
- documentation validation.

PR #54 adds regression coverage for the scheduling-vs-active safety split, acknowledged single-shot terminal transactions, rejected/failed Snooze behavior, critical-state corruption, dedicated founder token-signing rotation and compact wake rendering.

Exact PR #54 pass/fail evidence must be taken from the final PR head before merge; this document must not claim a green gate before GitHub reports it.

## Physical proof still required

Automated/emulator evidence is not sufficient for a wake product. Issue #9 remains the release gate for repeated physical-device proof, including:

- locked-screen T+2m cycles;
- Doze/idle;
- process/service recreation;
- reboot / Direct Boot before unlock;
- timezone/time changes;
- presentation and exact-alarm capability changes;
- Stop/Snooze resurrection checks;
- Bluetooth/audio-route behavior;
- motion calibration and false-positive behavior;
- representative reliability-report retention.

Broader release should not be declared complete until the supported-device reliability envelope is measured rather than inferred.

## Remaining product boundaries

- The approved custom wake-sound files are not currently committed in the repository. The critical path therefore still uses the bundled emergency alarm asset; the multi-profile alarm sound/ramp/preview product model remains to be completed when those source assets are available.
- Founder Realtime transport/provider selection is still evidence-gated by #28; direct OpenAI WebRTC is a strong implemented candidate, not a final provider declaration without physical latency/route/cost evidence.
- M7 live learning/journal wiring remains open.
- broader consumer authentication/account sync is intentionally not part of the critical alarm architecture.

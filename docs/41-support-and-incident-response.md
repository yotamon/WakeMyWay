# WakeMyWay 1.0 support and incident response

**Gate:** SELL #88  
**Purpose:** make paid rollout supportable without exposing private morning content

## Public support entry point

Before paid public rollout, configure one monitored support contact or ticket mechanism and publish it:
- on the Play listing;
- on the public WakeMyWay support page;
- inside the app's About/Support area when applicable.

Do not publish an unmonitored placeholder address.

## Consumer help topics

Keep answers short and actionable.

### Wake Ready / exact alarm

Explain that WakeMyWay needs Android's alarm capability to promise the selected exact wake time. The app should link the user to the relevant system repair surface rather than asking them to troubleshoot developer settings manually.

### Full-screen alarm / notifications

Explain that Android may require alarm notification/full-screen access for reliable locked-screen presentation. Even when presentation degrades, the safe audio/notification path remains the priority.

### Microphone / Voice Check-In

Voice is optional. If microphone/on-device recognition is unavailable, critical alarm behavior must remain controllable and local.

Never ask users to send raw morning recordings as support evidence.

### Battery / OEM restrictions

Document only restrictions observed in physical beta/reliability evidence. Do not create a generic OEM folklore list.

For an affected supported device retain:
- model;
- Android/OEM build;
- exact observed failure;
- verified workaround if one exists;
- whether the device remains inside the supported release envelope.

### Bluetooth / audio route

Support should ask:
- phone/model and Android build;
- whether Bluetooth/headphones/speaker were connected;
- whether any sound was audible locally;
- which WakeMyWay sound;
- whether Voice Check-In was enabled.

Do not request sensitive wake content.

### Snooze / Stop

Stop is terminal only after the local Alarm Kernel commits it. Snooze must first create the durable replacement wake. A rejected Snooze should leave the current wake visible/audible rather than silently ending it.

### Updates

WakeMyWay does not restart/install an update during Active Wake or within the protected pre-wake window. Play and direct distribution have separate update providers but share the same alarm-safety gate.

### Subscriptions

When Pro launches:
- show current entitlement state;
- link to Google Play subscription management;
- explain cancellation vs expiry;
- distinguish pending payment from active entitlement;
- provide restore/recheck action;
- never suggest that paying is required to make an already-created base alarm ring.

## “My alarm did not behave as expected”

Severity-first flow:

1. Determine whether the expected alarm was missed/late, inaudible, uncontrollable, presentation-only degraded, Snooze/Stop incorrect, or optional voice/motion behavior incorrect.
2. Record:
   - app version/build;
   - phone model;
   - Android/OEM build;
   - intended wake time/timezone;
   - whether device rebooted/updated/timezone changed;
   - permission/readiness state if known;
   - audio/Bluetooth context;
   - sanitized Wake Lab/reliability report when available.
3. Never request:
   - raw microphone audio;
   - complete transcript;
   - Tomorrow Contract text;
   - private calendar details;
   - secrets/tokens.
4. Link/reproduce against a GitHub incident/defect internally.
5. Classify:
   - CRITICAL_ALARM_DELIVERY
   - TERMINAL_ACTION
   - PRESENTATION
   - AUDIO_ROUTE
   - OPTIONAL_VOICE
   - MOTION
   - UPDATE
   - BILLING
   - OTHER
6. A confirmed critical alarm or terminal-action regression blocks/stops staged rollout until understood.

## Severity

### SEV-0 — critical wake safety/reliability

Examples:
- committed supported-device alarm systematically does not fire;
- Stop cannot terminate active audio;
- stale stopped occurrence resurrects;
- Snooze ends current wake without durable replacement;
- update/billing/cloud state weakens committed alarm.

Action:
- pause production rollout;
- disable affected non-critical remote functionality if relevant;
- preserve evidence;
- reproduce on supported physical device;
- issue fix/release only after regression proof.

### SEV-1 — major consumer blocker

Examples:
- locked-screen presentation unavailable despite accepted capabilities while audio remains safe;
- widespread crash prevents alarm setup;
- billing grants/removes entitlement incorrectly but base alarm remains safe.

Action:
- pause rollout when impact is material;
- communicate workaround if verified;
- prioritize fix.

### SEV-2 — degraded optional experience

Examples:
- Voice Check-In unavailable;
- Insights display defect;
- non-critical visual/RTL issue.

Does not automatically pause rollout unless evidence shows broader harm.

## Privacy-safe evidence bundle

Use `tooling/physical-reliability.ps1 -Action collect` for founder/device reproduction and the in-app Wake Lab report for the same scenario.

Production support telemetry may upload only sanitized semantic facts permitted by `docs/19-observability-analytics.md`.

## First-week paid rollout

For the first production staged rollout:

Daily review:
- crash/ANR health;
- confirmed missed/late alarm reports;
- terminal-action incidents;
- alarm recovery/fallback categories;
- Play billing errors/pending purchases;
- refund/cancellation/support themes;
- supported-device/OEM patterns.

Do not optimize on tiny engagement fluctuations during incident triage.

## Rollback / pause rules

Pause the rollout on:
- any reproducible critical alarm regression affecting the supported envelope;
- Stop/Snooze authority regression;
- release/update signing identity problem;
- privacy/data leak;
- billing defect that can charge incorrectly or remove paid access incorrectly at meaningful scale;
- crash/ANR regression that materially affects wake setup/active execution.

A rollback must preserve package/signing identity and must not restore a known unsafe alarm path.

## Ownership checklist

Before public paid rollout assign:
- release owner;
- incident owner;
- support inbox/ticket owner;
- Play Console owner;
- privacy/legal contact;
- observability dashboard owner.

One person may hold multiple roles, but none may be “nobody”.

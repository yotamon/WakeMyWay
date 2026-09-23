# WakeMyWay 1.0 Google Play submission packet

**Gate:** SELL #88  
**Status:** repository-side submission preparation  
**Policy verification date:** 2026-09-24  
**Rule:** Play Console and policy pages remain the policy authority. Re-check immediately before submission.

## Submission identity

| Field | 1.0 value / rule |
|---|---|
| App name | WakeMyWay |
| Android application ID | `com.wakemyway.app` |
| Platform | Android phone |
| Current target SDK | API 36 |
| Distribution artifact | signed Play AAB |
| Base product promise | a reliable local alarm that helps the user get moving |
| Medical positioning | none; do not claim treatment, diagnosis or verified consciousness |
| Ads | none |
| Account required | no |
| Core alarm network dependency | none |

As of 2026-09-24, Google Play requires new mobile apps and updates submitted after 2026-08-31 to target Android 16 / API 36 or higher. WakeMyWay already targets API 36.

Official source:
https://support.google.com/googleplay/android-developer/answer/11926878

## Restricted / special alarm capabilities

WakeMyWay declares:

- `USE_EXACT_ALARM`;
- `USE_FULL_SCREEN_INTENT`;
- foreground media playback;
- boot completion;
- notifications;
- microphone access for optional Voice Check-In.

The exact-alarm and full-screen declarations are justified by the app's core user-facing alarm-clock function. Both are release-review-sensitive and must be described plainly in Play Console.

Official policy references:
- https://support.google.com/googleplay/android-developer/answer/16558241
- https://support.google.com/googleplay/android-developer/answer/17517561

Reviewer explanation:

> WakeMyWay is an alarm-clock application. Users explicitly create wake alarms for exact local times. Exact scheduling and full-screen alarm presentation are core to delivering those alarms while the device may be locked. They are not used for background marketing, engagement notifications or unrelated work.

## Store positioning

### Short description draft

> A local-first alarm that helps you get moving, then learns what works for your mornings.

Keep within the current Play Console limit at submission time.

### Long-description narrative

1. **Set a wake you can trust.** WakeMyWay schedules the critical alarm locally on Android. The alarm, bundled wake sound, Stop and Snooze do not depend on AI, an account or the network.
2. **Wake in stages.** Optional Alfred Voice Check-In, simple movement evidence and one achievable First Move help turn a dismissal into an actual morning.
3. **Tell WakeMyWay what really happened.** A lightweight later check can distinguish phone-observed activation from getting up or returning to bed.
4. **Adapt carefully.** WakeMyWay can make bounded local adjustments from real comparable mornings. It does not let AI decide whether an alarm should ring.
5. **Stay in control.** Voice is optional. Core alarms work without an account. Private morning context stays local in the current 1.0 product.

Avoid:
- “guaranteed to wake you”;
- medical/sleep-disorder claims;
- “AI knows when you are awake”;
- claims about outcomes not supported by Gate 3 evidence.

## Screenshot story

Use only production consumer surfaces, in this order:

1. Tonight — next wake is ready.
2. Alarm editor — one clear wake plan.
3. Active Wake LISTENING — conversation is optional but purposeful.
4. Active Wake ORIENTING — First Move with Stop/Snooze still reachable.
5. Insights — real morning evidence, not a synthetic Wake Score.
6. Profile — local defaults and control.

Do not show Wake Lab, founder pairing, internal diagnostics, developer terminology, raw logs or synthetic realtime spike UI.

## Feature graphic direction

One authored sunrise/Wake Line composition using the existing visual identity. No fake phone UI, no medical imagery, no “AI magic” messaging, no faces required.

## Content / audience declarations

Working submission posture:
- general adult productivity / alarm utility;
- not designed for children;
- no ads;
- no gambling, health diagnosis or regulated medical functionality;
- voice/microphone is optional and disclosed at point of permission;
- app can be used without account creation.

Complete the actual IARC/content questionnaire from shipped behavior rather than copying a guessed rating.

## Data Safety baseline

The declaration must be generated from the **shipping Play build**, including every SDK added before submission.

Current 1.0 baseline before Billing/production telemetry:
- alarm schedules/preferences/history/learning: local app storage;
- Tomorrow Contract / Prepared Wake: credential-protected local storage;
- critical Direct-Boot state: minimal device-protected local storage;
- microphone: optional local Voice Check-In; raw audio and raw transcripts are not persisted;
- raw high-frequency motion: not persisted;
- Android OS backup/D2D: app-managed local state explicitly excluded;
- account backup code exists server-side but no user-facing Sign In is shipped;
- founder/debug realtime surfaces are not production consumer capability.

**Do not submit “no data collected” merely from this document.** Re-run the Data Safety review after Billing and production observability are implemented, because SDK/network behavior changes the answer.

## Privacy-policy submission requirement

Google Play requires an active, publicly accessible, non-editable privacy-policy URL and the policy must also be available or linked inside the app. It must describe collection/use/sharing, third parties, security, retention/deletion and a developer/privacy contact mechanism.

Official references:
- https://support.google.com/googleplay/android-developer/answer/9859455
- https://support.google.com/googleplay/android-developer/answer/10144311

Repository draft: `docs/39-public-privacy-policy.md`.

External launch blocker: publish the policy under a stable WakeMyWay URL and provide the verified public support/privacy contact that matches the Play developer identity.

## Subscription submission boundary

Do not create irreversible Play product IDs or lock prices until Gate 3 validates the package/price.

When subscriptions are enabled:
- product IDs are immutable once created, so name them deliberately;
- disclose price, billing frequency, renewal/trial terms before purchase;
- provide an easy in-app route to manage/cancel the Play subscription;
- preserve paid access through the already-paid period after ordinary cancellation as Play entitlement dictates;
- never show commerce during Active Wake;
- entitlement outage never affects a committed alarm.

Official references:
- https://support.google.com/googleplay/android-developer/answer/140504
- https://support.google.com/googleplay/android-developer/answer/9900533

Recommended provisional IDs **only after package lock**:

~~~text
wakemyway_pro
base plan: monthly
base plan: annual
~~~

One subscription with base plans keeps one semantic Pro entitlement. Do not create product IDs before the final package has been approved.

## Release-track sequence

~~~text
internal → closed beta → production staged rollout
~~~

Public production waits for:
- TRUST #9 accepted;
- FINISH #59 accepted;
- PROVE #87 sufficient for a paid-launch decision;
- SELL checklist below complete.

Before production:
- Play pre-launch report reviewed;
- exact-alarm/full-screen declarations accepted;
- Data Safety submitted from final dependencies;
- privacy URL live;
- support contact live;
- signed AAB identity verified;
- billing products tested with license testers;
- restore/cancel/pending lifecycle exercised;
- production crash/ANR monitoring live;
- known supported-device envelope documented.

## Staged rollout

Default:
- internal/closed testing first;
- production begins with a small staged percentage;
- pause rollout on any confirmed critical alarm-delivery regression, terminal-action regression, crash/ANR spike affecting wake paths, or billing defect that incorrectly removes an already-paid non-critical entitlement;
- never “fix” billing by changing Alarm Kernel behavior.

Rollback owner and first-week incident procedure: `docs/41-support-and-incident-response.md`.

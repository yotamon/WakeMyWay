# WakeMyWay 1.0 Play launch and support operations

**Status:** Gate 4 preparation  
**Canonical gate:** #88  
**Preparation issue:** #113  
**Pricing dependency:** Gate 3 / #87  
**Last updated:** 2026-09-24

This document turns the commercial/release side of WakeMyWay into an executable launch path without changing the product promise or inventing paid-only features before beta evidence exists.

## Launch principle

WakeMyWay should be sold as an outcome:

> **WakeMyWay helps habitual snoozers actually get moving, then learns which wake strategy works best for them.**

The Play listing, subscription messaging, support flow and launch operations should explain that outcome in consumer language.

Do not lead with:

- Alarm Kernel;
- Direct Boot;
- reducers/state machines;
- Android service architecture;
- model/provider names;
- "AI alarm clock";
- medical or sleep-treatment claims.

## Store positioning draft

### App name

**WakeMyWay**

Do not add "AI" to the title.

### Short description draft

> An alarm that helps you actually get moving, then learns what works for your mornings.

Keep final Play character limits and localization requirements under review at submission time.

### Long description draft

> You already know what time you want to wake up. The hard part is getting from "the alarm went off" to actually moving.
>
> WakeMyWay is built for people who snooze, dismiss alarms reflexively, or wake up only to drift back into bed.
>
> Set the wake you genuinely want. WakeMyWay starts with a reliable local alarm, then guides you through a short wake sequence designed to get you engaged and moving. Afterward, a quick morning check-in helps distinguish a wake that looked successful on the phone from one that actually worked in real life.
>
> Over time, WakeMyWay can make small, bounded adjustments to the wake strategy using your own morning history. The alarm itself stays local and does not depend on an internet connection, an account, or an AI service to ring.
>
> **Built around trust**
>
> - reliable Android alarm scheduling;
> - local wake sounds that remain available offline;
> - Stop and Snooze stay under local alarm control;
> - optional voice guidance degrades safely when unavailable;
> - your morning history and learning stay local by default;
> - no ads during your wake;
> - no purchase decisions while you are half-awake.
>
> WakeMyWay is a behavioral alarm product. It does not diagnose, monitor or treat a sleep disorder and does not medically verify wakefulness.

This is launch-copy working material, not a promise to ship every line verbatim. Final copy must match the accepted reliability envelope and Gate 3 evidence.

## Screenshot story

Use existing product states to tell one coherent story.

| Order | Screen | Consumer message |
|---|---|---|
| 1 | Onboarding / brand | **Wake up your way.** The alarm learns what helps you move. |
| 2 | Tonight / next wake | **Set the wake you genuinely want.** |
| 3 | Alarm editor | **Choose the basics. WakeMyWay handles the strategy.** |
| 4 | Active Wake | **Get moving without giving the phone control of the outcome.** |
| 5 | First Move / Oriented | **Turn activation into one real action.** |
| 6 | Morning check-in | **Tell WakeMyWay what actually happened.** |
| 7 | Insights / learning | **Small adjustments from your real mornings.** |

Screenshot captions should remain short and avoid architecture terms.

## Store asset checklist

Before Play production submission:

- final adaptive launcher icon verified on current Android launchers;
- 512×512 Play icon exported from the canonical identity;
- 1024×500 feature graphic;
- phone screenshots from a release-equivalent build;
- screenshots reviewed at compact and normal widths;
- no debug/Wake Lab surfaces;
- no mock data presented as real user evidence unless explicitly labelled;
- no pricing shown until Gate 3 locks the launch package;
- no medical/sleep-treatment language.

## Release artifact contract

### Play

- Android App Bundle;
- production signing;
- stable application/package identity;
- versionCode monotonic;
- current target SDK policy verified immediately before submission;
- Play Billing version verified immediately before billing implementation/submission;
- Play pre-launch report reviewed;
- internal/closed track exercised before production;
- staged production rollout.

### Direct founder/dogfood

- signed APK;
- signed update manifest;
- package/signing identity verification;
- safe-update gate remains independent of alarm authority.

The direct channel is not the public paid commerce channel.

## Play tracks

Recommended ladder:

~~~text
Internal
   ↓
Closed beta
   ↓
Paid/production staged rollout
   ↓
Broader production
~~~

### Internal

Purpose:
- install/update sanity;
- billing test-account lifecycle;
- Play-specific manifest/bundle behavior;
- Play in-app update path.

### Closed beta

Purpose:
- Gate 3 target-user evidence;
- real device/OEM support learning;
- pricing/package research;
- support workflow rehearsal.

### Production staged rollout

Begin with a deliberately limited percentage only after Gates 1–4 are accepted.

Do not expand merely because the rollout has no crash spike. Alarm reliability, support incidents and wake-outcome evidence matter more than install volume.

## Rollout pause conditions

Pause expansion when any of these are credible:

- missed or materially late alarm attributable to WakeMyWay;
- Active Wake becomes uncontrollable or cannot Stop safely;
- Snooze ends the current wake without a durable replacement;
- package update loses durable alarm state;
- a device/OEM pattern creates repeated unreliable wakes;
- Play purchase/restore state causes a committed alarm to become weaker;
- privacy/Data Safety configuration is inaccurate;
- subscription presentation is misleading;
- crash/ANR regression in the core journey;
- support volume reveals a repeated setup/readiness failure not understood by the team.

A pause is not a failure of the launch process. It is the launch process working correctly.

## Rollback / hotfix rules

A public rollback must never assume users can wait until the next morning.

For alarm-critical regressions:

1. pause staged rollout;
2. identify affected version/device envelope;
3. preserve user alarm data and package identity;
4. ship a compatibility-preserving hotfix;
5. verify in-place update persistence;
6. re-run relevant physical reliability scenario;
7. resume rollout only from retained evidence.

Do not solve an incident by clearing local alarm state.

## First-week incident triage

Classify incoming reports:

| Severity | Example | Action |
|---|---|---|
| **P0** | expected alarm never became audible/actionable | pause rollout if credible/reproducible; collect reliability report |
| **P0** | Stop/Snooze cannot control an active wake | pause rollout; treat as execution-safety incident |
| **P1** | wake occurs materially late / presentation missing but audio works | investigate device/capability pattern before rollout expansion |
| **P1** | purchase/restore incorrect but alarm remains safe | stop conversion expansion; repair commerce path |
| **P2** | confusing setup/copy/navigation | fix within FINISH/launch polish if repeated |
| **P3** | preference/feature request | record for post-1.0 Explore; do not widen launch scope |

## Support surface

Minimum public support topics:

### Wake did not behave as expected

Ask for:
- WakeMyWay version;
- Android version;
- device model;
- approximate intended wake time;
- whether the phone was restarted, updated, in battery saver/Doze or connected to Bluetooth;
- exported WakeMyWay reliability report, if available.

Do **not** ask the user to send:
- raw microphone recordings;
- Tomorrow Contract text;
- full transcripts;
- calendar content;
- passwords/tokens.

### Wake Ready / Android permissions

Explain:
- exact-alarm access;
- notifications;
- full-screen alarm presentation where applicable;
- why future readiness can be unhealthy while an already-active wake remains safely controllable.

### Audio / Bluetooth

Troubleshoot:
- media/alarm route;
- connected headset/speaker;
- route changes;
- whether local wake sound remained audible.

### Snooze

Explain that WakeMyWay commits a replacement wake before ending the current one. If a safe exact replacement cannot be committed, Snooze must fail closed rather than silently end the alarm.

### Updates

Explain:
- Play updates use Play;
- direct founder builds use the signed direct-update path;
- updates are blocked near/inside a critical wake window when required for safety.

### Subscription

Server-side verification enablement (before `WMW_PLAY_VERIFICATION_ENABLED=true`):
- service-account androidpublisher access and allowed product ids configured;
- a production edge rate limit exists for `POST /api/v1/commerce/play-verify` and its rule id is declared via `WMW_PLAY_VERIFY_RATE_LIMIT_RULE_ID` — the route refuses to enable without it;
- RTDN Pub/Sub push uses authenticated OIDC with the exact audience/service account;
- license-tester exercise of purchase/verify/RTDN completed.

Once billing is live:
- purchase;
- pending purchase;
- restore;
- renewal;
- cancellation;
- grace/on-hold/paused/expired states as supported by current Play Billing;
- subscription management link through Google Play.

Never troubleshoot billing by asking for full payment-card details.

## Public support contact blocker

A paid public release needs a durable public support/privacy contact owned by the product.

As of this document, no dedicated WakeMyWay support email/domain contact is configured in the repository.

Do **not** invent or publish a fake `support@` address.

Before public launch:
- provision the real address/contact route;
- put it in Play Console;
- put it in the public privacy policy and support page;
- verify receipt and response workflow.

This is an external account/operations prerequisite, not missing product functionality.

## Release-note template

~~~text
WakeMyWay <version>

What changed
- <consumer-visible improvement>
- <reliability/polish improvement>

Reliability
- <relevant supported-device or update note>

No change
- Your existing alarms remain on-device.
- WakeMyWay still does not require an internet connection for a committed local alarm to ring.
~~~

Do not publish internal architecture terminology in consumer release notes.

## Billing boundary

The current official Play Billing line must be rechecked at implementation time. As of 2026-09-24, the project preparation target is Play Billing Library **9.1.0**.

Architecture rule:

~~~text
Google Play purchase state
          ↓
commerce adapter / entitlement projection
          ↓
non-critical premium UI/experience decisions

NEVER:
billing → Alarm Kernel authority
billing → Stop/Snooze authority
billing → whether an already-committed wake rings
~~~

The app must understand at least:
- product unavailable;
- purchase pending;
- purchased but not yet verified/acknowledged;
- active;
- cancelled but entitled through period end;
- grace period where applicable;
- on hold;
- paused where applicable;
- expired;
- restore/reinstall.

Final entitlement rules and package boundaries wait for #87 evidence.

## External launch prerequisites

These cannot be completed by repository code alone:

- Play Console developer/account verification;
- production app signing credentials;
- subscription product/base-plan creation;
- license tester/test-account setup;
- dedicated support/privacy contact;
- public legal URLs deployed to the chosen production domain;
- final Data Safety answers entered in Play Console;
- final content rating;
- final countries/regions and price configuration;
- closed/production track rollout actions.

Every prerequisite should have a concrete owner before Gate 4 closes.

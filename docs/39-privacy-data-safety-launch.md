# WakeMyWay 1.0 privacy, Data Safety and legal launch inventory

**Status:** Gate 4 preparation  
**Canonical gate:** #88  
**Last updated:** 2026-09-24

This is the repository source of truth for the public privacy/Data Safety draft. It is not legal advice and it must be reconciled against the exact release build and Play Console configuration immediately before submission.

## Governing rule

Disclose what the **shipped consumer build actually does**, not what backend code could theoretically do and not what a future roadmap may add.

When a capability is not exposed/active in the release, do not describe it as current data collection.

When a capability becomes live later, reopen this inventory before shipping.

## Current 1.0 data-flow inventory

| Data/category | Current consumer behavior | Leaves device? | Retained by WakeMyWay? | Launch note |
|---|---|---:|---:|---|
| Alarm schedule/time | local product + critical state | No by default | Yes, local | required for core alarm |
| Alarm label | credential-protected local product state | No by default | Yes, local | not copied into Direct Boot critical snapshot |
| Profile/default preferences | credential-protected local state | No by default | Yes, local | no account required |
| Tomorrow Contract text | private local preparation state | No by default | Yes, local/bounded | excluded from Direct Boot and platform backup |
| First Move | local alarm/preparation preference | No by default | Yes, local | may be present in local history/plan only where implemented |
| Wake history | credential-protected local semantic history | No by default | Yes, local | no raw transcript/audio |
| Wake calibration | local semantic outcome | No by default | Yes, local | values such as GOT_UP / RETURNED_TO_BED |
| Learned policy | local bounded deterministic state | No | Yes, local | no cloud/ML dependency |
| Raw microphone audio | processed for local voice interaction | No in the normal 1.0 consumer path | No archive | never persist by default |
| Full transcript | not retained by normal local wake | No by default | No | semantic facts only |
| Motion sensor samples | processed locally | No | No raw stream | only bounded semantic movement evidence |
| Reliability journal | local non-sensitive technical timeline | No unless user explicitly exports/shares it | Yes, bounded local | support diagnostic |
| Account identity | no consumer Sign In currently exposed | No current consumer collection | No current consumer account | reopen if account ships |
| Consumer cloud backup | backend code exists but no verified consumer path is exposed | No current consumer transfer | Not active | do not claim active sync |
| Founder/debug Realtime | developer/debug enrichment only | not part of public 1.0 consumer path | no public consumer archive | exclude from public claims |
| Analytics SDK events | no remote product analytics SDK currently live | No | No | Android Vitals/Play platform data is separate |
| Crash SDK data | no third-party crash SDK currently embedded | No app-level SDK collection | No | Play Console Android Vitals may still report platform diagnostics |
| Purchase/subscription data | not implemented yet | N/A | N/A | reopen when Billing ships |

## Local storage and reset

Current local data includes:
- alarm definitions;
- minimal Direct-Boot critical schedule/execution snapshot;
- preferences;
- Tomorrow Contract / prepared plan;
- wake history/calibration;
- learned strategy;
- reliability evidence.

Android platform Auto Backup and device-to-device restore are explicitly denied for app-managed state. WakeMyWay does not treat uncontrolled OS restore as a safe migration mechanism.

Before public launch, the consumer product must expose or document a truthful local reset/delete route for the state that actually exists. Do not promise cloud deletion while no cloud account exists.

## Microphone disclosure

The microphone is used only when the user enables behavior that requires voice input.

Core rules:
- a committed alarm remains local;
- microphone loss must not silently delete an otherwise safe scheduled alarm;
- raw microphone audio is not archived by WakeMyWay;
- normal local voice interaction should not require server retention;
- if a future production conversational provider receives audio, this policy and Play Data Safety answers must be updated before release.

## Motion disclosure

WakeMyWay may use device motion sensors to derive bounded wake evidence such as pickup/orientation/movement.

It does not persist or upload continuous raw accelerometer/gyroscope traces in the 1.0 design.

## Diagnostics and support

The local reliability report may contain:
- app/build version;
- Android/device facts;
- wake occurrence identifiers;
- semantic lifecycle timestamps;
- scheduling/reconciliation/fallback events;
- terminal Stop/Snooze outcome;
- capability state.

It must not contain:
- raw audio;
- complete transcripts;
- Tomorrow Contract text;
- calendar event content;
- passwords/tokens;
- raw high-frequency motion.

Sharing the report with support is user-initiated.

## Provider / processor inventory

### Current public consumer release

Expected processors/services:
- **Google Play** — app distribution, optional future Play Billing, Play platform diagnostics/Android Vitals.
- **Android / device OEM services** — platform alarm, notification, speech and system services as provided by the device.

### Present in repository but not currently active as a public consumer data path

- **Vercel** — optional Wake API / developer cloud hosting.
- **Supabase** — preferred optional account/backup infrastructure when provisioned.
- **OpenAI** — founder/debug conversational Realtime path; not required for the public local wake.
- other analytics/crash processors: none enabled by default as of this inventory.

Before launch, verify the exact release dependency graph and production environment rather than copying this list blindly.

## Play Data Safety worksheet — current draft

This is a planning worksheet, not the final Play Console declaration.

### Data collected by the app developer

Current local-first build:
- no broad first-party remote analytics collection;
- no raw audio collection/retention by WakeMyWay;
- no consumer account collection while Sign In is absent;
- no cloud backup collection while the consumer transport is absent.

If billing is added:
- purchase/subscription identifiers/tokens may be processed for entitlement verification;
- financial card details remain handled by Google Play and must never be requested by WakeMyWay support.

If production crash/semantic telemetry is added:
- reopen this worksheet and list the exact diagnostic/event fields, retention and opt-out/legal basis.

### Data shared

Do not mark provider processing as "shared" or "not shared" from memory. Complete the Play Console form using Google's current definitions and the exact processor contracts at submission time.

### Security practices to preserve

- TLS for network paths;
- local alarm does not depend on network;
- no raw private wake content in telemetry;
- secrets excluded from APK;
- Android backup/D2D excluded for app-managed state;
- conservative schema/version handling;
- support diagnostics are user-initiated.

## Public privacy-policy content requirements

The public page must clearly state:

1. what WakeMyWay stores locally;
2. which permissions are used and why;
3. whether data leaves the device;
4. microphone/audio handling;
5. motion handling;
6. diagnostics/support export;
7. billing data once implemented;
8. any account/cloud behavior once implemented;
9. retention/reset/delete behavior;
10. processors/providers actually active;
11. contact route for privacy questions;
12. effective date and material-change process.

Do not state that all data always stays on-device if any production provider path later becomes enabled.

## Terms / consumer disclosure requirements

The public terms should include at least:
- WakeMyWay is not a medical device and does not diagnose/treat sleep disorders;
- alarms can be affected by device power, OEM restrictions, user permissions, OS changes and hardware state;
- supported-device/reliability envelope should be described truthfully;
- subscription terms must match the Play purchase screen;
- recurring price, billing period, renewal and cancellation must be clear;
- no terms should imply the user waives statutory consumer rights that cannot legally be waived.

Final legal wording should be reviewed for the jurisdictions actually launched.

## Retention working policy

| Data | Working retention |
|---|---|
| critical wake snapshot | only while required for current/future committed wake and recovery |
| Tomorrow Contract / prepared plan | through relevant occurrence plus bounded recovery/use window |
| wake history/calibration | local until user reset/delete or product-defined bounded retention is adopted |
| learned policy | local until reset/delete; can be regenerated from retained compatible evidence |
| reliability journal | bounded rolling local history |
| account backup | not active; define before enabling |
| purchase verification | define with billing backend before enabling |
| remote telemetry | not active; define before enabling |

Do not invent a retention duration solely to fill a policy field. Choose one only when the product/storage behavior actually enforces it.

## Launch blockers external to code

Before public paid release:
- provision a real support/privacy contact;
- publish the privacy policy on a stable public URL;
- publish terms on a stable public URL if required for the chosen launch/commercial model;
- enter/review Play Data Safety;
- review provider DPAs/terms where applicable;
- decide launch jurisdictions;
- confirm billing/tax/consumer-disclosure setup in Play Console;
- verify any production cloud/telemetry feature against this document.

If any of those changes the actual data flow, update the app copy and this inventory before rollout.

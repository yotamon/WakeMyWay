# WakeMyWay Privacy Policy — publication draft

**Draft for 1.0 release preparation**  
**Last updated:** 2026-09-24

This privacy policy applies to **WakeMyWay**, an Android alarm application.

This repository copy is the governing content draft. Before public Google Play submission it must be published at one stable, public, non-editable web URL and must contain the verified privacy/support contact that matches the developer identity used in Google Play.

## Privacy by wake design

WakeMyWay is designed so the critical job of waking you does not require an account, cloud service or AI model.

The alarm schedule needed to wake safely, bundled alarm audio, Stop and Snooze remain local Android responsibilities.

## Information stored on your device

Depending on the features you use, WakeMyWay may store locally:

- alarm schedules and alarm configuration;
- wake-sound, voice and Snooze preferences;
- your optional display name and reusable First Move default;
- wake history and compact behavioral timing/outcome evidence;
- bounded local Wake Learning state;
- optional Tomorrow Contract / First Move text prepared for one wake;
- minimal critical state needed to recover a scheduled alarm after reboot.

WakeMyWay excludes app-managed product/private state from Android operating-system cloud backup and device-to-device transfer in 1.0. Product migration is not silently delegated to Android backup.

## Microphone and voice replies

Voice Check-In is optional.

When enabled, WakeMyWay uses the microphone so on-device speech recognition can detect a short spoken reply during an active wake. The local 1.0 path does not build an archive of raw microphone recordings or raw transcripts.

The critical alarm remains usable if voice is unavailable.

Founder/developer experimental realtime services are not part of the public 1.0 consumer path and must not be represented as production data processing.

## Motion sensors

WakeMyWay may use device motion signals during an active wake to derive limited semantic movement evidence. It does not persist a continuous archive of raw accelerometer or gyroscope samples.

## Tomorrow Contract and private morning context

If you use Tomorrow Contract, your reason for tomorrow and optional First Move are stored in credential-protected local app storage and prepared for the intended wake.

They are not copied into the minimal Direct-Boot alarm snapshot.

The current public 1.0 product does not upload that private text merely to make the alarm work.

## Wake history and learning

WakeMyWay can keep compact local wake history so it can show what happened in previous mornings and make bounded future adjustments.

This can include semantic facts such as:
- whether a wake was completed, stopped or Snoozed;
- time to first engagement or activation completion when available;
- intervention depth;
- a later one-tap outcome calibration such as got up, returned to bed, got up later or skipped.

These are product-behavior facts, not medical measurements of consciousness or sleep health.

## Accounts and cloud backup

An account is not required for core 1.0 alarms.

WakeMyWay contains an architecture boundary for optional authenticated backup/migration, but public privacy disclosures must describe only cloud/account functionality actually enabled in the shipping build.

If account creation is enabled in a future release, WakeMyWay must update this policy and provide both in-app and web-accessible account deletion controls before that release.

## Purchases

If WakeMyWay Pro subscriptions are offered through Google Play, Google processes the purchase and payment relationship through Google Play Billing.

WakeMyWay may process the minimum purchase/entitlement information needed to determine whether Pro access is active. Purchase state will never become alarm authority: an entitlement/network problem cannot prevent an already-committed local alarm from ringing.

The final Data Safety form and this policy must be updated to reflect the exact Billing implementation before paid release.

## Analytics, diagnostics and crash reporting

WakeMyWay's production observability policy is data-minimizing.

If production diagnostics/analytics are enabled, they may include sanitized semantic events needed to operate the product, such as:
- crashes and ANRs;
- alarm lifecycle failures and fallback usage;
- wake started / activation completion / calibration submitted;
- coarse product retention;
- subscription funnel state.

WakeMyWay does not intentionally send the following to analytics/crash systems:
- raw microphone audio;
- full voice transcripts;
- Tomorrow Contract raw text;
- calendar titles/descriptions/locations;
- raw high-frequency motion streams;
- secrets or authentication tokens;
- full AI prompts/model context containing private morning content.

The final provider list below must be updated before public release to include only processors actually enabled in the production build.

## Service providers / processors

Current architecture may use the following categories only when the corresponding production capability is enabled:

| Provider/category | Purpose | Public 1.0 status |
|---|---|---|
| Google / Android / Google Play | app distribution, OS permissions, optional Play Billing | Android/Play platform; Billing pending SELL |
| Vercel | optional non-critical Wake API hosting | cloud boundary exists; not alarm authority |
| Supabase | optional future account identity/database | account UI currently deferred |
| AI Gateway/model providers | optional non-critical AI enrichment | private production usage must satisfy WakeMyWay retention/privacy policy |
| Crash/analytics provider | privacy-safe production diagnostics | final provider not yet locked |

Do not publish the final policy with “pending” processors. At submission time this table must match the exact production configuration and Data Safety declaration.

## Retention

Local state is retained only while it serves the user-facing product or the bounded local learning/history experience.

Current principles:
- critical next-wake state: retained only while required for scheduled/active recovery;
- private Tomorrow Contract/prepared wake state: scoped to its wake/recovery lifecycle unless the product explicitly exposes a retained history;
- local wake history/learning: retained locally for user-facing history/learning until local app data is reset/removed;
- raw microphone audio and raw high-frequency motion: not retained by the local wake path;
- cloud data: retained only for cloud features actually enabled and must have a defined deletion path.

Uninstalling the app normally removes app-local credential/device data according to Android behavior. WakeMyWay does not rely on Android cloud backup to restore it.

## Security

WakeMyWay uses Android's credential-protected/device-protected storage boundaries, Android Keystore where appropriate, TLS for network traffic and server-side secret storage.

No provider/operator secret key is intentionally embedded in the public APK.

The critical alarm path is isolated from account/cloud availability.

## Your choices and controls

Depending on the feature:
- Voice Check-In can be disabled per alarm/defaults.
- Android microphone/notification/alarm permissions can be controlled through system settings, subject to the feature consequences shown by WakeMyWay.
- Local app data can be removed through Android app-storage controls/uninstall.
- Subscription management/cancellation, when Pro launches through Google Play, must be linked from the app.
- Account export/deletion controls must be added before user-facing account creation is enabled.

## Children

WakeMyWay 1.0 is not designed or directed specifically to children.

## Changes to this policy

Material changes to collection, sharing, account, Billing, analytics or AI/provider behavior require this policy and the Play Data Safety declaration to be updated before the changed behavior is publicly released.

## Contact

**Release blocker:** insert the verified privacy/support contact or inquiry mechanism associated with the WakeMyWay Google Play developer identity before publication.

Do not replace this line with an unmonitored or invented address.

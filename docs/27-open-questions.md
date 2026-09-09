# Open questions

These are intentionally unresolved. Do not silently choose durable answers without updating this file and recording the decision when appropriate.

## Product / behavioral policy

- What exact target window defines **Activation Completion** for the default policy?
- What target/feedback interpretation defines **Confirmed Wake Success** strongly enough for dogfood analysis?
- What activation-evidence combination is sufficient to enter Orienting, and how should it vary by policy?
- What optional calibration cadence gets useful return-to-bed evidence without becoming annoying?
- What privacy-safe proxy, if any, can complement occasional self-report for premature Activation Completion?
- How much snooze customization should be explicit vs learned?
- What is the safest/useful default Wake Policy for a new user?
- When should Tomorrow Contract/context be mentioned relative to first engagement/movement?
- Should a user configure a hard maximum snooze count or only choose a wake-difficulty mode?

## Wake Learning v0

- What minimum source-session evidence is required before the first learned policy change?
- Which 2–4 policy parameters are safe/useful enough for the first adaptation surface?
- What maximum step size/hysteresis prevents oscillation or overreaction to one unusual morning?
- How should conflicting effectiveness vs annoyance/agency signals block or reverse an adaptation?
- What user-facing explanation, if any, should be shown when WMW changes future behavior?
- When should a learned policy automatically fall back/reset after poor calibrated outcomes?

## UX / brand

- final production logo and WMW monogram
- production typefaces and license choice
- final accessible color tokens
- sonic Wake Motif composition
- haptic patterns across OEMs
- exact intentional Stop interaction: one-tap + confirmation vs accessible deliberate gesture/hold
- whether Safety Backup remains dogfood-only or is useful for public first-week onboarding
- final ordinary-app navigation once real screens exist

## Android — blocking/near-term

### Decided baseline

- `targetSdk = 36` baseline under current 2026 Google Play requirement
- `compileSdk = 36+` using the stable toolchain selected in M0
- `USE_EXACT_ALARM` is the preferred manifest direction for WMW's dedicated alarm-clock use case, subject to implementation/submission revalidation

### Still unresolved

- exact `minSdk` after supported-device/reliability tradeoff review
- exact Android Gradle Plugin/Kotlin/Compose/toolchain versions at M0
- exact foreground Active Wake Execution component shape/name and start/recovery details under supported API levels
- whether any wake lock is actually required after measuring foreground alarm behavior
- exact notification/full-screen presentation UX by supported Android version
- alarm audio behavior with Bluetooth / calls / unusual volume states
- Android 17/API-37 behavior when target SDK eventually moves to 37
- exact Direct Boot component/storage implementation and integrity strategy
- post-Force-Stop readiness copy/recovery behavior (platform limitation itself is decided)
- whether any current Play policy evidence forces a change from preferred `USE_EXACT_ALARM` before submission

## Voice

- direct OpenAI vs LiveKit after **M8** measurements
- ElevenLabs role for character voice quality
- local/prepared speech rendering strategy
- transient transcript/context requirements for realtime conversation
- multilingual voice/character identity behavior

## Backend

- first concrete cloud capability that actually justifies creating `services/api`
- anonymous installation credential design
- exact Supabase Auth adoption point and account-claim/merge flow
- development/preview database isolation strategy once cloud work begins
- which learning/profile computations, if any, ever leave device after local M7 proves value
- generated OpenAPI client vs thin handwritten Retrofit after actual endpoint surface exists

## Business/legal

- pricing / free realtime allowance
- whether billing launches with closed beta or later
- trademark clearance
- `wakemyway.com` registration confirmation

## Future

- iOS timing
- KMP vs native Swift implementation when iOS is real
- multiple independent adaptive alarms/Wake Schedules
- Wear OS
- trusted-person wake messages
- smart-light/speaker integration

# Open questions

These are intentionally unresolved. Do not silently choose durable answers without updating this file and recording the decision when appropriate.

## Product / behavioral policy

- What exact target window defines **Wake Success** for the default user/policy?
- What activation-evidence combination is sufficient to enter Orienting, and how should it vary by policy?
- How much snooze customization should be explicit vs learned?
- What is the safest/useful default Wake Policy for a new user?
- When should Tomorrow Contract/context be mentioned relative to first engagement/movement?
- Should a user configure a hard maximum snooze count or only choose a wake-difficulty mode?
- What proxy best detects a premature "success" followed by return to bed without collecting intrusive data?

## UX / brand

- final production logo and WMW monogram
- production typefaces and license choice
- final accessible color tokens
- sonic Wake Motif composition
- haptic patterns across OEMs
- final ordinary-app navigation once real screens exist

## Android — blocking/near-term

- exact `minSdk`, `targetSdk`, `compileSdk` at M0 after current requirement verification
- final `USE_EXACT_ALARM` vs `SCHEDULE_EXACT_ALARM`/special-access strategy under current Play policy
- exact notification/full-screen presentation UX by supported Android version
- alarm audio behavior with Bluetooth / calls / unusual volume states
- exact Direct Boot component/storage implementation and integrity strategy
- post-Force-Stop readiness copy/recovery behavior (platform limitation itself is decided)

## Voice

- direct OpenAI vs LiveKit after M7 measurements
- ElevenLabs role for character voice quality
- local/prepared speech rendering strategy
- transient transcript/context requirements for realtime conversation
- multilingual voice/character identity behavior

## Backend

- anonymous installation credential design
- exact Supabase Auth adoption point and account-claim/merge flow
- development/preview database isolation strategy once cloud work begins
- which learning/profile computations, if any, leave device later
- generated OpenAPI client vs thin handwritten Retrofit after actual endpoint surface exists

## Business/legal

- pricing / free realtime allowance
- whether billing launches with closed beta or later
- trademark clearance
- `wakemyway.com` registration confirmation

## Future

- iOS timing
- KMP vs native Swift implementation when iOS is real
- multiple independent alarms/Wake Schedules
- Wear OS
- trusted-person wake messages
- smart-light/speaker integration

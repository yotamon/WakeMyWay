# Sunroom Club — selected canonical visual prototype

**Decision date:** 2026-10-21  
**Status:** product-owner selected visual target; implementation NOT started  
**Scope:** WakeMyWay native Android UI, starting with Today, Alarm Editor, and Active Wake, then visual consistency for adjacent consumer surfaces.  
**Reference name:** `wakemyway_sunrise_app_experience.png`  
**Reference size:** 1672 × 941 px, four phone compositions; SHA-256 `f9291ddf5d3e6a831a6fcbabe59c5b91fd93f6a6eddb0d8ecd3f9f1b94d6394f`.  
**Provenance:** the approved image was generated and selected in the design conversation. Its **binary is not yet checked into the repository**; preserve/ingest the original before implementation, and compare to that exact checksum to avoid silently substituting the earlier V2 variants. The image is a visual reference, **not** a working Android prototype or literal pixel-for-pixel UI contract.

## Supersession & authority

The 2026-10-21 selected four-phone Sunroom Club illustration is the **primary visual reference**. It supersedes the three-direction exploration and the older neutral concept on visual character, texture, lighting, illustration, spatial hierarchy and control styling.

Truth/effect authority order remains:

1. Current implemented Android/runtime and actual available actions;
2. product correctness, privacy, critical wake safety and accessibility;
3. this selected visual target and its annotated exceptions;
4. earlier design explorations.

Any mismatch between image and actual app behavior must resolve in favor of (1)–(2), while matching the selected visual treatment as closely as is safely practical. Do not invent features to fill decorative UI.

## Chosen visual personality

- **Feel:** sunny, intimate, creative, charming, gently playful, premium; not child-oriented, a generic wellness card dashboard, a chatbot, or an ornate 3D game.
- **Shared signature:** warm sunrise landscapes, soft illustrated vegetation and horizon, rounded warm-white floating surfaces, deep midnight editorial numerals, coral/sunrise actions, a light tactile button depth, animated continuous Wake Line.
- **Worlds:** evening-planning Today/Editor: airy vanilla/paper/peach; active start/listen/move: midnight-indigo -> dusk horizon; oriented/complete: early daylight and warm cream.
- **Imagery:** soft layered storybook landscape with restrained dusk silhouettes; hero scenery should create mood but not cover core information or reduce readability. Prioritize asymmetric landscape placement behind or beside the large time, not over a button.
- **Typography:** striking large thin numerals + clean human sans headlines; offline/bundled licensed font if adopted, otherwise Android sans. No runtime font download, and stable numeric glyph widths.
- **Animation:** no continual confetti or bouncing. Favor barely moving ambient sunlight/clouds on planning screens; semantic Wake Line response only from actual session state.

## Controlled change to the previous brand rules

The original `docs/07-design-system.md` forbids mascots. The selected reference **explicitly authorizes a small, smiling sun as a decorative illustration motif** in noncritical surfaces (Today, onboarding and possibly oriented/completion after unlock) and art panels. This is a narrow visual exception and should be ratified in the canonical design-system file when shipping.

**Not authorized:** replacing the canonical sunrise + single Wave Line logo with a face, giving the sun a voice or conversations, calling it Alfred, introducing a new character entity, a new state, microphone activation, reward or streak, or a robot/AI orb. **Alfred stays the existing voice persona.** On Emerging/Listening/Moving, prioritize the landscape and Wake Line; no smiling character over the large time or critical actions. Accessible semantics mark all mascot drawings as decorative.

## Android adaptation

The image shows iOS-ish phone bezels and Dynamic Island in its presentation; those are **presentation frames** only. Production must use Android system bars, cutouts, navigation insets and a real Android window. Do not draw a fake clock/status bar, gesture pill, or microphone control. Keep existing Today / Alarms / You navigation. Insights remains contextual.

## Screen contract: Today / Home

**Hero order:** WakeMyWay identity + current greeting; `Tomorrow starts here.`; oversized next wake time and date; truthful `Why you're waking` when available; small brand landscape/Horizon visual; factual `Wake Ready` or repair; private Morning plan preview and available First Move; Alfred presence if Voice Check-In enabled; primary Set wake/Edit tomorrow action; three-tab navigation.

Data already exists in `TonightUiState` (`TonightScreen.kt`) and is populated from `WakeMyWayApp.kt`. **Do not hardcode** the scenic mockup's running context, reason, secondary `Morning focus` task or arbitrary timestamps. The app currently supports a reason and **one First Move**, not a list of scheduled routine steps. Express the existing fields as one or two compact visual regions, not a new task checklist. Do not imply a live Google Calendar import.

Conditionals must survive: no alarm, no reason, only label fallback, contract optional/prompted/disabled, not-ready repair with explicit repair action, previous morning check-in, microphone/Realtime unavailable, voice disabled, developer lab only in debug.

**Screenshot discrepancy:** The reference uses a prepared run reason and 07:30; this is **sample content**. Its 07:35 `Morning focus` and multi-entry timeline **must not appear without a real domain source**. An optional plan card may show the exact existing contract text and its one First Move; other content collapses gracefully.

## Screen contract: Create / Edit Alarm

Headline and back affordance; large actionable HH:mm with real Android `TimePickerDialog`; `Why this wake?` backed by existing `AlarmDefinition.label`; Weekly/One time with 7 day chips or date selector; Wake sound with existing 12-second preview; Wake behavior summary and `Customize wake` progressive disclosure; Save/Create; optional delete + confirmation. Do not mislabel the sound selection as a separate assistant/voice picker. Sound IDs Morning Light / Soft Start / Morning Pulse only where resource bundled.

When expanded, preserve current Voice Check-In toggle, read-only character Alfred, VoiceStyle Default/Motivational/Minimal, Snooze toggle with 5/10/15 min, Tomorrow Contract mode Optional/Prompt/Off, optional default First Move. Do not hide every advanced control behind a new destination or create a fake `all settings` summary that cannot be edited. Fix footer vs IME and maintain errors/validation.

## Screen contract: Active Wake

Existing `WakeSurface` mode drives visuals, not reverse:

| Existing mode | Visual world & primary content | Critical UI |
|---|---|---|
| Voice off / ALARM_ONLY | Midnight/landscape, large time and accurate fallback message; no listening waveform | Stop; Snooze if allowed |
| STARTING/SPEAKING | Dark night, Alfred identifier, large time, quiet Wake Line, actual/startup status | Stop; conditional Snooze |
| LISTENING | Indigo-to-coral dawn, oversized HH:mm, actual voice line and `Listening` only if input active, Wake Line reacts | Stop; conditional Snooze |
| MOVING | Warmer/brighter horizon, concise action line, motion status only when true | Stop; conditional Snooze |
| ORIENTING | Daybreak, date, actual `Why you're up` only when private plan available and device unlocked, existing First Move, `I'm doing it` | Stop; conditional Snooze |
| COMPLETE | Warm morning, `Morning started`, actual First Move/fallback, Finish action | existing Finish/Stop semantics |

`WakeActivity.kt` already guards private context using user unlocked / keyguard and `FLAG_SECURE`; do not weaken that boundary. Stop/Snooze touch targets and placement must survive small heights, Android system bars, animation delays, landscape image decoding failure and Realtime degradation. Never add decorative taps on the sun or waveform that control an alarm.

## Component / illustration contract

- Reuse `WakeMyWayTheme.kt`, `WakeMyWayTokens.kt`, `WmwBrand.kt`, `WmwMorningInstrument.kt`, `WmwWakeLine.kt`, `WmwFoundation.kt` by **restyling/extending**, not duplicating a new parallel design-system API.
- Recommended small Compose set: `SunroomScene` (background layers and day-stage palette), `SunroomSunMotif` (decorative, cheap-to-render asset), `SunroomPaperPanel`, `SunroomPrimaryAction`, `SunroomAccentLabel`, `SunroomWakeLine` as an extension/wrapper of existing WmwWakeLine where useful.
- Export original illustration into deterministic native components: pre-bundled static WebP/AVIF (verify Android decode/API support) or vector for fixed background layers; Compose Canvas or vectors for animated horizon. Prefer reusable scene fragments to a single screenshot baked behind text.
- Include 1x/2x content and night/day variants as needed, but do not preserve or recreate fake OS chrome, baked-in text or buttons.
- Assets must work fully offline, with Direct Boot constraints where relevant. No network artwork, remote fonts or Lottie dependency just for this redesign.
- Dynamic text and scrolling cannot be baked into imagery. Add simple paper/background fallback when rich media fails to decode.

## Fidelity rubric (subjective product sign-off + objective checks)

1. **Brand:** immediate recognition of the reference at thumbnail size (illustrated peach sunlight, cream paper, warm coral CTAs, spacious dark numerals, expressive sunset night).
2. **Hierarchy:** wake time and primary action are the two strongest focal points; title and reason readable without decoration.
3. **Character:** decorative sun feels kind, distinctive and original but never resembles a second talking agent.
4. **Functionality:** 100% of existing visible actions, states and setting paths remain accessible and operational.
5. **Safety:** zero change to alarm authority, audio, snooze and stop execution; no private leakage; no fake readiness/listening.
6. **Fit:** layouts scale cleanly on compact Android phones, large font and reduced-motion mode.
7. **Trust:** no cards, badges, times, weather, tasks or calendar events populated by invented data.

## Source file map
- `apps/android/app/src/main/java/com/wakemyway/app/ui/home/TonightScreen.kt`
- `apps/android/app/src/main/java/com/wakemyway/app/ui/alarms/AlarmEditorScreen.kt`
- `apps/android/app/src/main/java/com/wakemyway/app/WakeActivity.kt`
- `apps/android/app/src/main/java/com/wakemyway/app/ui/theme/WakeMyWayTokens.kt`, `WakeMyWayTheme.kt`
- `apps/android/app/src/main/java/com/wakemyway/app/ui/components/WmwBrand.kt`, `WmwFoundation.kt`, `WmwMorningInstrument.kt`, `WmwWakeLine.kt`
- `apps/android/app/src/main/java/com/wakemyway/app/ui/navigation/WmwConsumerNavigation.kt`, `WakeMyWayApp.kt`
- `apps/android/app/src/test/java/com/wakemyway/app/ProductVisualRegressionTest.kt` and curated `src/test/screenshots/`

See the separate `docs/implementation/2026-10-21-sunroom-club-implementation-plan.md` for the complete build order and acceptance gates.

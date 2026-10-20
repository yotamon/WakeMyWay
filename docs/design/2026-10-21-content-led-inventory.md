# WakeMyWay — Content-first UI redesign inventory

**Scope:** design exploration of three already-implemented Android surfaces, grounded in `main` as read 2026-10-21. **No production app code, data models, runtime policy, screen routes, or integrations are changed.**

**Files:** `concepts.html` is a standalone visual concept; exported screen previews are in the accompanying design handoff. It is **not** a screenshot of the production APK or a promise that simulated HTML controls save real alarms.

## Implementation evidence / source of truth

- `apps/android/app/src/main/java/com/wakemyway/app/ui/home/TonightScreen.kt`: Today content, conditional blocks and user actions.
- `apps/android/app/src/main/java/com/wakemyway/app/ui/alarms/AlarmEditorScreen.kt`: alarm settings, editors, create/save/delete and advanced disclosure.
- `apps/android/app/src/main/java/com/wakemyway/app/ui/alarms/AlarmsScreen.kt`: alarms list and status/repair row.
- `apps/android/app/src/main/java/com/wakemyway/app/WakeActivity.kt`: stage-specific active wake UI and Stop/Snooze/First Move.
- `apps/android/app/src/main/java/com/wakemyway/app/ui/preparation/TomorrowPlanScreen.kt`: contract text, dictation, First Move, privacy and save/clear behavior.
- `apps/android/app/src/main/java/com/wakemyway/app/ui/navigation/WmwConsumerNavigation.kt`: Today / Alarms / You tabs; Insights secondary.
- `apps/android/app/src/main/java/com/wakemyway/app/ui/theme/WakeMyWayTokens.kt`: canonical colors, sizes and motion tokens.
- `apps/android/app/src/main/java/com/wakemyway/app/ui/theme/WakeMyWayTheme.kt`: typography and runtime theme.
- `apps/android/app/src/main/java/com/wakemyway/app/alarm/WakeSoundCatalog.kt`: allowed wake sounds, bundled resource gating and default.
- `apps/android/app/src/main/res/values/strings.xml`, `apps/android/app/src/main/res/values/brand_strings.xml`: existing copy.
- `apps/android/app/src/test/java/com/wakemyway/app/ProductVisualRegressionTest.kt`: scenarios/fixtures, including 07:30, Tuesday 14 Jan, design review at 10:00, First Move Shower and Alfred-ready variants.
- `docs/07-design-system.md`, `docs/implementation/reason-first-calm-companion.md`, `docs/implementation/morning-instrument-redesign.md`, `docs/45-core-experience-quality-reset.md`: approved design and implementation boundaries.

**Evidence priority:** runtime implementation > resource strings > active product contract > aspirational/backlog docs. A documented future character picker/calendar integration is **not** treated as a currently available consumer control.

## 1. Today / Home — existing content and state matrix

| Existing content or action | Source behavior | Placement in concept |
| --- | --- | --- |
| WakeMyWay identity, time-of-day greeting | Actual greeting depends on local hour; brand headline `Tomorrow starts here.` | Masthead and human greeting |
| Next wake time, date | Sourced from actual next occurrence; `--:--` / Tomorrow empty variant | Large numerals on open surface |
| Why you're waking | Existing reason selection: private contract preview > alarm label > no artificial reason | Immediately below time, only when reason exists |
| Wake Horizon / sunrise | Presentational brand geometry, not an AI listening state by itself | Center atmospheric moment |
| Wake Ready / Needs attention | Actual alarm scheduling/Android readiness; repair action only if needed | Under horizon; error case promoted above optional content |
| Tomorrow Contract preview | Available conditionally; optional/promoted prompt, prepared state | Compact Morning plan section, edit affordance |
| First Move | Existing contract or default, if configured | In plan section, only when actually present |
| Alfred presence | Shown only if Voice Check-In enabled | Compact character signature, no avatar or robot |
| Voice replies setup/unavailable | Conditional diagnostic/repair UI, not a fake 'connected' state | Error/attention variant, absent in happy-path screenshot |
| Morning check-in | Conditional follow-up after an actual wake | Secondary contextual block, absent in happy-path screenshot |
| Primary action | `Set wake time` if no occurrence; `Edit tomorrow` if scheduled | One Sunrise CTA, preserving respective existing action |
| Wake Lab | Existing developer-only action | Hidden in normal consumer mockup; never promoted to global navigation |
| Navigation | Today / Alarms / You; Insights contextual/secondary | Persistent bottom navigation |

**Explicit variants needed in implementation QA:** no alarm, upcoming ready, wake needs repair, missing reason, contract prompt requested, voice unavailable/permission, previous morning check-in, large text/compact height, disabled optional context.

## 2. Create / Edit Alarm — existing content and state matrix

| Existing control | Existing values / constraints | Placement in concept |
| --- | --- | --- |
| Create / Edit | Different title/save copy; delete only for existing alarm and when callback available | `Edit alarm.` fixture; back and Save |
| Why this wake? | Persists the existing `AlarmDefinition.label`, not a new text field; fallback reason | Visible field near time |
| Wake time | Real Android time picker | Large tap target / Change affordance; real picker must be retained in Android |
| Schedule mode | Weekly or One time | Segmented selection |
| Weekly days | Seven-day selection | Compact but touch-safe week row |
| One-time date | Android date selection | Date row instead of days |
| Wake sound | Only resources confirmed bundled at runtime: Morning Light, Soft Start, Morning Pulse (default is Morning Light) | Existing sound choices and preview controls; availability remains dynamic |
| Sound preview | Local 12-second playback at media volume | Preview action, never fake playback in production |
| Wake behavior summary | `Alfred conversation` or `Sound only` + snooze + morning intention status | Compact summary; `Customize wake` reveals advanced controls |
| Voice Check-In | Enabled/disabled, Alfred character, voice style Default / Motivational / Minimal | Advanced section only |
| Character | Current consumer implementation shows Alfred, **not** a working multi-character picker | Read-only Alfred in advanced area |
| Snooze | On/off; available durations 5, 10, 15 min | Advanced control |
| Morning context | Optional, Prompt, Off | Advanced control |
| Default First Move | Existing free-text setting when morning context not Off | Advanced section |
| Save/delete/error | Existing callbacks, delete confirmation and save failure state | Sticky primary action; conditional secondary delete/error states |

**Explicit variants needed in QA:** new/edit, disabled sound resource, weekly/one shot, Voice Check-In off, snooze off, contract disabled, editor error, required calendar date, back navigation, delete confirmation, text scale/RTL, keyboard visible.

## 3. Active Wake — current UI/runtime mapping

| Current actual mode | What appears | Forbidden invention |
| --- | --- | --- |
| Voice Check-In off / `ALARM_ONLY` | Local alarm-only wake, time and Stop/Snooze | No fake listening/mic status |
| `STARTING` or `SPEAKING` | Emerging: character name, time, Wake Line, spoken/preparation line | No private contract exposed simply because it exists |
| `LISTENING` | Engaged: spoken line or `Morning.`, time, horizon, `Listening`, `Answer out loud...` | Only show listening when the input is genuinely available |
| `MOVING` | Active: spoken line or `Just get upright first.`, time, horizon, `Motion detected` | No claim user is upright without runtime authority |
| `ORIENTING` | Oriented: greeting/date, private `Why you're up` only if prepared and authorized, `What's first?`, First Move, `I'm doing it` | Do not show reason earlier or leak private content while locked |
| `COMPLETE` | `Morning started`, time, horizon, First Move, `Finish` | Not an invented task progression |
| Realtime degradation | Alarm-only fallback, factual unavailable copy; local alarm continues | Do not simulate the voice or show connected state |
| Critical actions | Snooze (if allowed) and Stop always under Alarm Kernel control; errors cannot dismiss critical wake | Never hide critical actions behind decorative full-screen content |

**Design preview:** Listening is the default third-screen mockup, with state selector outside the device; oriented is a second *state* of that same screen. The example reason only appears on the Oriented preview, respecting runtime sequencing.

## 4. Adjacent existing screens that must remain in navigation

- **Alarms list:** open alarm rows with time, label, schedule, Alfred/voice vs sound, readiness and toggle; `Create alarm`; repair action when needed. This first design pack redesigns the editor rather than this list, so full list remains a follow-on surface, not a silently dropped feature.
- **Tomorrow Contract:** existing dedicated screen with private badge, speech dictation, reason textarea, optional First Move, `Use this in Voice Check-In`, Save/Clear and privacy message. The Home concept's Edit affordance opens an illustrative overlay with these existing fields; the production route stays its own screen.
- **Insights:** secondary destination; local wake history, calibration and learning. Not a primary nav item.
- **You:** wake preferences, default sound/voice/First Move, appearance, accounts/pro; some account/subscription sections gated by configuration.
- **Wake Lab:** founder-only diagnostics; intentionally outside consumer navigation.

## 5. Visual system and design decisions

- **Existing brand palette**: Midnight `#08142F`, Deep Navy `#10264C`, Sunrise `#FF9F6D`, Sunrise Soft `#FFB88F`, Golden Light `#FFD699`, Dawn `#A5B4FC`, Cloud `#F8F7F4`, Paper `#FFFCF8`.
- **Planning world**: open Paper/Cloud, Midnight headline and time, one warm primary action, hierarchy built from negative space rather than nested cards.
- **Wake world**: midnight low-distraction surface for early stages; gentle warm light around the actual Wake Line; warm morning/light surface only during oriented and completed runtime stages.
- **Fonts**: native app currently uses `FontFamily.SansSerif` intentionally until a licensed font is bundled. Concept uses system/local sans fallback. No remote font requirement is introduced.
- **Signature object**: a restrained sun behind a single sound-wave/horizon. Never a generic AI orb or assistant avatar.
- **Progressive density**: `when → why → readiness → action` at home; `time → reason → schedule → sound → advanced` in editor; `hear/reply → move → why/first move` during live wake.
- **Motion**: semantic movement only on actual Wake Runtime state transitions; no pulse animation that implies a microphone is connected without proof.
- **Accessibility implementation gates**: Android click targets >=48dp (>=56dp for sleepy actions), no critical content obscured by gesture nav, responsive text, RTL/locale, dynamic contrast testing, TalkBack descriptions, keyboard/IME-safe save actions.

## 6. Fixture integrity and limitations

The screenshots use the existing **test fixture**, not a user's real calendar: `07:30`, `Tuesday, 14 Jan`, `Design review at 10:00. You wanted time to shower and eat.`, `Shower`; a separate existing alarm fixture uses the label `Morning focus`. Do not imply a calendar integration has been built to produce this text: it is provided by the existing Tomorrow Contract/test state.

The HTML is a **design target** with simulated interactions; it cannot schedule, stop, snooze or persist actual alarms. This PR deliberately does not change running product code or perform compiled APK visual QA. Android implementation requires its own PR, visual-comparison tests and on-device verification.

## 7. Acceptance criteria for future Android implementation

1. Content parity: every displayed field/action maps to current state and callback; no invented character list, weather, calendar schedule or assistant mode.
2. No private reason or First Move leakage in Emerging/Engaged or before Android user unlock.
3. Stop/Snooze and alarm audio unaffected by visual layout, Realtime startup or network failure.
4. Clean 360–430dp layout, small heights, font scaling to 200%, keyboard, TalkBack, color contrast and RTL in supported paths.
5. Real source strings, schedule state, ready/needs-attention and empty/error states; no screenshots hardcoded into production.
6. App tabs and routes remain available, with a clear path from Today to Tomorrow Contract, Alarm Editor and Insights.
7. Automated screenshot/semantics checks supplemented by real-device visual inspection; do not call a design accepted based only on snapshot regressions.
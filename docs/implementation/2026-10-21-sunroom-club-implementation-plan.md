# Sunroom Club — production implementation plan (Android)

**Plan status:** ready to build after selected visual target; **nothing in this document claims implementation or test completion.**
**Primary visual:** `docs/design/2026-10-21-sunroom-club-selected-reference.md` (approved image provenance, checksum, state/content adaptations).
**Prior content contract:** `docs/design/2026-10-21-content-led-inventory.md`.
**Repository starting point:** Android Compose `:app`, current `main` at work start. Do not implement from the aging docs-only prototype branch.
**Build strategy:** one implementation-first engineering branch/PR for coherent consumer visual changes; small logical commits; quick compile and focused verification during build; expensive regression/CI and physical QA near feature completion. GitHub may trigger automatic CI on PR pushes; do not launch redundant full runs prematurely.

## 0. Required outcomes and non-goals

### Product outcomes

- `Today`, alarm create/edit, active wake Listening/Oriented and all real wake states must look substantially like the **selected** illustration while behaving exactly as the existing product does.
- New imagery is distinctive, cohesive and production-shippable rather than a generic Material theme with a pretty wallpaper pasted behind it.
- No hidden core actions, no fabricated agenda/tasks, no fake session state; 07:30, running reason and other mock figures are visual **fixtures** only.
- Existing routes/settings remain discoverable; secondary screens gain compatible typography, surfaces, navigation, controls, input and state styling.
- The active wake stays extremely legible, low distraction, fast to render and reliably stoppable on a locked phone.

### Explicitly excluded

No new calendar/task planner, Morning Focus timeline with 07:35 timestamps, new companion persona, character picker, mic action, rewards/streaks, new runtime state, alarm schedule logic change, auth/Neon/provider change, Realtime rewrite, voice feature change, cloud APIs, new server endpoints or database migrations. No change to critical alarm authority or private wake state visibility.

## 1. Implementation architecture & component boundaries

```text
WakeMyWayApp (existing navigation and projections)
  ├── TonightScreen(TonightUiState)  -- style/content mapping only
  ├── AlarmsScreen, AlarmEditorScreen -- keep AlarmDefinition callbacks
  ├── TomorrowPlanScreen/Profile/Insights/Onboarding -- compatible styles
  └── WakeActivity -> WakeSurface(preparedPlan, voiceState, callbacks)
        ├── WakeSessionMode (existing state authority)
        ├── SunroomStageScene(mode)   -- **presentation only**
        ├── WmwWakeLine(mode)         -- semantic motion
        └── WakeSafetyFooter          -- existing Stop/Snooze action seam
```

There is no new UI state machine, repository, background worker, service, provider SDK or artificial `SunroomWakeState` controlling the alarm. Reuse existing `TonightUiState`, `AlarmDefinition`, `WakeVoiceUiState`, `PreparedWakePlan`, `WakeOccurrence` and consumer callbacks. If a small screen-only derived presentation model is needed, keep it immutable, pure and one-way; no persistent duplicate state.

### Suggested localized file ownership

| File/folder | Intent |
| --- | --- |
| `ui/theme/WakeMyWayTokens.kt` | Add Sunroom-specific daylight/dusk/peach/ink and spacing/elevation/illustration tokens, do not break existing compatibility tokens |
| `ui/theme/WakeMyWayTheme.kt` | Typography, shapes, color choices, selected appearance themes; preserve system scaling and offline fonts |
| `ui/components/WmwFoundation.kt` | Restyle shared cards/headers/actions and prepare visually compatible repair/error states |
| `ui/components/WmwBrand.kt` / `WmwMorningInstrument.kt` / `WmwWakeLine.kt` | Preserve original sunrise-wave logo, make horizontal Wake Line responsive to actual mode and reduced motion |
| `ui/components/SunroomScene.kt` (new if useful) | Illustrated background stage variants, reusable noninteractive decorative sun/cloud/landscape |
| `ui/components/SunroomPaperPanel.kt` (new if useful) | Reusable warm paper surfaces, contrast-safe labels and large illustrated-action controls |
| `ui/home/TonightScreen.kt` | Full home composition and content truth |
| `ui/alarms/AlarmEditorScreen.kt` | Existing editor inputs and advanced disclosure inside new art direction |
| `WakeActivity.kt` | Existing wake screens only; mode-specific visual scene/layout mapping; no new critical-alarm policy |
| `ui/navigation/WmwConsumerNavigation.kt` | Coral/cream integrated Today/Alarms/You nav while preserving current routes |
| `ui/alarms/AlarmsScreen.kt` / `ui/preparation/TomorrowPlanScreen.kt` / profile, Insights, onboarding | Consistency pass after core three are coherent |
| `res/drawable-nodpi` / vector resources | Layered decorative art and small icons; never screenshot with hardcoded copy |
| `src/test/screenshots`, `ProductVisualRegressionTest.kt`, `WakeResponsiveVisualSmokeTest.kt` | Curated screenshots for happy path and critical variants |

Avoid creating a parallel large component library when the current Wmw primitives can be evolved safely.

## 2. Execution slices in build order

### Slice A — Asset ingest and visual specification lock

1. Preserve original selected 1672x941 source image under version control as `docs/brand/assets/sunroom-club-selected-reference.png` after verifying SHA-256 from the selected-reference document. **Pending until original binary is imported**; do not substitute the older V2.
2. Extract four screen interiors only for design QA comparison; no embedded OS chrome, text or controls in production artwork.
3. Create a small asset production sheet showing exactly: daylight Home background, editor sunrise header, nighttime active wake, dawn/oriented wake, decorative sun expressions, Wake Line, foliage/cloud foreground, warm paper containers.
4. Rebuild/source clean **separate layers** at screen-appropriate resolution; the approved composite is not usable as a production full-screen background because its time/text/controls are baked in.
5. Maintain consistent one-sun silhouette/color/face rules. Decorative sun not Alfred, not the canonical app launcher mark.
6. Pick an offline, license-safe font; avoid font packaging until a usable license+asset exists, fall back gracefully to `FontFamily.SansSerif`.
7. Update `docs/07-design-system.md` and `docs/26-decisions-log.md` **in the implementation PR** to codify the narrow decorative-sun exception; it does not authorize a speaking mascot.

**Deliverable:** version-controlled approved reference, illustration resource kit, compositional layout specification and token palette. **Gate:** source file preserved and imagery contains zero interactive or readable text.

### Slice B — Native visual foundation

1. Extend existing theme tokens with Sunroom semantic values (paper, canvas, sunlight, dark sky, coral action, night-action stroke). Current approved colors remain compatible unless explicitly superseded by a recorded choice.
2. Add reusable background composition with pre-bundled static art in planning screens and low-overhead dark wake backgrounds. Use pixel-independent intrinsic layout/cropping (ContentScale.Crop with safe focal point), draw text in Compose on a contrast-safe region.
3. Add card/panel styles: warm white paper, restrained elevation, soft coral edges, clear selected/disabled/error states. Preserve minimum hit-target sizes.
4. Add title/time typography (light/large HH:mm with tabular figures if available). Fonts loaded locally; no remote fetch.
5. Add animation values for tiny scene transition / Wake Line, respecting system Animator Duration Scale=0/reduced motion.
6. Keep existing shapes/navigation contracts and expose bright/dark styles via composable parameters; do not use color-only state information.

**Deliverable:** a coherent, previewable Sunroom design system usable with data-driven Compose screens.

### Slice C — Today / Home faithful rebuild

1. Keep exact props/callbacks of `TonightScreen`. No new persistent state or source of truth.
2. Replace the vertical information feed with an editorial composition: compact masthead; large time/date; optional truthful reason in a warm floating panel; decorative landscape; fact-based readiness; compact morning-plan surface with **one real First Move** if available; conditional Alfred note; warm primary Set Wake/Edit tomorrow.
3. Preserve `onOpenWakeSetup`, `onOpenTomorrowPlan`, `onRepairWakeSystem`, `onOpenMorningCheckIn`, voice permission action and developer-only Wake Lab.
4. Remove the fake second `Morning focus` scheduled task in the reference. If extra context exists, use exact contract preview text *without inventing a time*. If not, collapse this row.
5. For missing/no wake, display actual empty state, placeholder and correct `Set wake time` action (no hardcoded `Wake Ready`). If `wakeReady=false`, present the real repair action prominently and keep it accessible without scrolling through scenery.
6. Honor `tomorrowContractAvailable`, `tomorrowContractPromptRequired`, `hasMorningCheckIn`, `voiceWakeReadiness`, `voiceCheckInEnabled`.
7. Keep contextual Insights and Today/Alarms/You nav. Balance content scrolling with bottom CTA and gesture navigation on compact phones.
8. No private contract text on lock/notification/widget surfaces.

**Deliverable:** Home matches the reference's visual atmosphere and current truth in every conditional state.

### Slice D — Alarm editor faithful rebuild

1. Retain all `AlarmEditorScreen` function signature/state/write path. Reorder presentation only.
2. Header art and bold `Create/Edit alarm`; oversized actionable time pill must open existing Android `TimePickerDialog` (no fake wheel).
3. Existing label `Why this wake?` persists to `AlarmDefinition.label` and is single line.
4. Existing Weekly/One time segmented mode, seven-day toggle or existing date picker. Day hit boxes at least 48dp; localized week order and RTL handled.
5. Wake sounds present exactly when bundled. Keep selection and actual 12-second preview start/stop, teardown on exit and truthful playback errors.
6. Keep advanced `Customize wake` progressive disclosure, with **all current settings**: Alfred voice toggle, voice style, 5/10/15 minute Snooze, morning intention mode, Default First Move and delete confirmation.
7. Preserve failure/validation path, repeated save guard, keyboard handling and back behavior. No new save path and no overlay that hides errors.

**Deliverable:** editor looks inviting but is no less functional than baseline.

### Slice E — Active wake and oriented/completion

1. Treat `WakeActivity.WakeSurface` and `WakeVoiceMode` as the **only** state authority for scene changes.
2. Emerging/Speaking: dark gentle night scene, actual time/Alfred/start status, quiet horizon. Do not display motivational context or fake listening.
3. Listening: illustrated night-to-dawn panorama from target, actual `Listening` when enabled, spoken line, softly responsive Wake Line. Preserve existing voice plumbing entirely.
4. Moving: more light, short movement cue, actual motion claim only.
5. Oriented: warm daytime scene, optional private reminder *only when unlocked*, First Move or existing fallback, `I'm doing it` invokes current `onFirstMoveConfirmed`, Stop/Snooze remain intact.
6. Complete: `Morning started`, actual First Move/fallback, existing Finish semantics, no invented streak or reward.
7. Alarm-only/degraded: clear truthful fallback; local sound continues, no fake Alfred chat/listening.
8. Dock Stop and optional Snooze into visible, foreground critical-action region for all active modes; at least 56dp target each, no decor overlaid, no dependence on scene assets or animations. Keep error/terminal feedback and recovery behavior.
9. Do not change `WakeSessionViewModel`, `AlarmKernel`, `AlarmPlaybackService`, Stop/Snooze handlers or Realtime service configuration except correcting a proven UI integration defect with separate review.

**Deliverable:** reference-fidelity sunset voice surface that stays safe in the worst-case device/wake conditions.

### Slice F — Cohesion of the rest of the app

Once core screens are implemented, apply the same tokens/illustration restraint to:
- Alarms list (open rows, enabled/disabled, status/repair, create).
- Tomorrow Contract (keep `FLAG_SECURE`, private local reason, First Move, dictation, share-with-voice opt-in).
- You/Profile, wake preferences, Appearance and Account/Plan screens currently exposed to users.
- Insights (secondary), onboarding/empty/permission states, widget colors only if supported by Glance and no inflated new scope.
- Diagnostics/debug must stay secondary (Wake Lab unmodified behavior).

**Deliverable:** users do not fall from a charming home screen into an unrelated generic editor/settings screen. No accidental rewrite of account, calendar or commerce flows.

### Slice G — Technical/visual hardening near completion

**Quick checks during development** (not a full CI suite every commit):
- compile changed Compose files/screens after each integrated slice;
- run narrowly scoped preview/unit semantics for time selectors, permission/repair and Stop/Snooze controls;
- inspect a debug build on a phone as soon as Home + Listening are visible, to catch density/legibility before complete polish;
- immediately fix accessibility/security issues noticed in a slice.

**Run near the end, once the whole redesign is implemented:**
- `./gradlew :app:assembleDirectDebug` (adjust exact variant to available tasks), Android lint and targeted behavioral tests;
- record/review/verify curated Roborazzi screenshot baselines **only after human side-by-side review**; keep CI read-only verify;
- existing `ProductVisualRegressionTest`, `WakeResponsiveVisualSmokeTest`, `WakeMyWayContrastTest`, ProductAccessibilityVisualSmokeTest and instrumented accessibility checks;
- real-device smoke and dark/bright text scale testing; selected critical alarm kernel smoke/physical tests;
- release CI after final integration, not at the start of each visual edit.

Do not claim golden/screenshot verification proves actual alarm reliability or good design.

## 3. Explicit state/visual test matrix

| Surface | Required cases |
| --- | --- |
| Today | ready + next wake, no wake, needs attention + repair, missing reason, label fallback, prepared contract, contract disabled, prompt required, voice disabled/unavailable, pending morning check-in |
| Editor | create/edit/delete, time picker, weekly/one-time, days unselected, date, sound available/missing/preview error, voice off, snooze off, context off, advanced expanded/collapsed, Save error, keyboard open |
| Active Wake | alarm-only, starting/speaking, listening, moving, oriented locked/unlocked with/without context, complete, Realtime unavailable, snooze allowed/disallowed, Stop terminal feedback/retry |
| Presentation | 360×640 and 360×780, 393×852, 430×932 dp; font scale 1.0/1.3/2.0; EN/DE, RTL smoke, long copy, system insets/cutouts, reduced motion, TalkBack, dark/bright ambient |
| Cross-functional | no fake listening; no false `Wake Ready`; critical Stop/Snooze above illustration and reachable; no private reason on locked device or Direct Boot; no regression after reinstall/update; offline wake |

## 4. Visual and technical acceptance criteria

### Visual craft (product sign-off)
- Side-by-side render with selected screenshot for all four reference panes; comparison uses **real state** and Android chrome, not image overlays.
- The recognizable illustration and unique art direction survive without overusing generic cards, thin platform icons or inconsistent secondary screens.
- Source art has coherent edge/line weights and stable colors at 360–430dp, no upscaled low-res blur/pixelation, no text baked into artwork.
- Body text and status remain readable on illustrated areas; remove/reposition scenery instead of reducing contrast.
- At least one physical-device screenshot from each of Today, Editor, Listening, Oriented reviewed by a human before visual approval.
- No arbitrary screenshot similarity score as substitute for usability and design judgment.

### Safety, accessibility & privacy (hard blockers)
- Wake Stop/Snooze callbacks unchanged, visible in all modes where allowed; terminal failure never looks like confirmed stop.
- No public state falsely displays active microphone, movement, local readiness or alarm success.
- Private contract displayed in Oriented only when existing unlock/security policy allows it; preserves `FLAG_SECURE`; no private details in artwork, sample data or screenshot logs.
- >=48dp accessible targets on consumer controls; >=56dp critical active-wake target; content descriptions for meaningful controls, decor removed from semantics.
- WCAG-like contrast target >=4.5:1 normal text, >=3:1 large text and essential UI indicators; visual testing of actual layered backgrounds.
- Dynamic font scales up to 200% without inaccessible controls; scrolling/IME and system navigation insets handled correctly; no reliance on color-only states.
- Reduced-motion/system animation-disabled path is complete and useful; no animations block alarm setup/actions.

### Performance & reliability (measured, not assumed)
- All visual resources packaged locally; no remote font, CDN, network initialization or AI startup dependency.
- Backgrounds decode predictably and cheaply. Set measurable baseline before/after at warm/cold `WakeActivity` launch and compare allocation, dropped frames, retained memory and time-to-visible Stop/Snooze.
- No extra disk I/O/bitmap decode on critical action callbacks and no per-frame bitmap allocation in Wake Line drawing.
- Safe fallback is a themed solid/gradient scene with functional time/controls on image decode or GPU memory failure.
- Keep APK growth and decoded-memory footprint documented with `before / after`; optimize assets to the smallest viable quality, prefer static bundled vector/optimized bitmap layers, and check API-compatibility before shipping formats.
- No cosmetic activity lifecycle side effects; app handles rotation/activity recreation/Android process recovery as before.

## 5. Suggested implementation PR structure and order

Use **one focused implementation PR** split into independent commits (asset + theme, Home, Editor, Wake, cohesion, QA), or separate PRs only if branch conflicts/team parallelism materially benefit. Keep docs-only design PR #202 draft as the visual reference, not as a production release. Rebase implementation from the most recent main and avoid reintroducing old files.

Illustrative commits:
1. `design: add selected Sunroom assets and theme foundation`
2. `feat(ui): rebuild Today as Sunroom planning canvas`
3. `feat(ui): apply Sunroom styling to alarm editor`
4. `feat(ui): deliver staged Sunroom wake scenes while preserving critical actions`
5. `feat(ui): harmonize Alarms, Tomorrow Contract, You and empty/error states`
6. `test(ui): curate Sunroom visual baselines and native accessibility coverage`
7. `docs: ratify Sunroom identity, visual QA and known limitations`

Avoid starting multiple feature branches whose cherry-picks duplicate the same Compose theme changes.

## 6. Failure handling and rollout

- Feature code should ship as one coherent visual system rather than expose mixed partial surfaces to normal users.
- During development, keep quick revertability via logical commits, and keep critical alarm presentation functional even when imagery fails.
- No schema migration and no new user configuration are needed.
- Before release, test debug/dev build on a physical Android device via Wake Lab, plus one actual scheduled alarm (locked device) for Stop/Snooze/voice-degradation. Real overnight reliability proof is a separate launch acceptance gate.
- Release only after explicit approval of visual fidelity and critical behavioral safety. Do not publish or merge the docs-only design branch as an application feature.

## 7. Definition of done

- [ ] Approved source image imported and checksum verified; native illustration kit produced without baked UI.
- [ ] New Sunroom brand exception codified while retaining original mark and Alfred voice identity.
- [ ] Today, Editor and all Wake modes implemented in Compose; all original controls and state branches work.
- [ ] Adjacent consumer screens visually consistent; no placeholder task or fictional status.
- [ ] Stop/Snooze and on-device/Direct-Boot/privacy constraints intact under offline and failed Realtime.
- [ ] Dynamic typography, TalkBack, RTL smoke, keyboard, insets and reduced-motion verified.
- [ ] Reference side-by-side human review + curated Roborazzi baselines accepted.
- [ ] Targeted tests and final broader CI passed; one real device wake smoke completed.
- [ ] No unnecessary new dependencies, migrations, server APIs or runtime policies.
- [ ] Before-release delta summary + remaining manual actions explicitly documented.

## 8. Remaining owner actions

No external OAuth, API key or cloud configuration is needed for the UI-only design implementation. The main product-owner action is final **side-by-side visual acceptance on a physical Android device** before release. The selected image is already provided in the conversation, but **its binary must be ingested into the repo** from the original design artifact during Slice A; do not pretend it is already version-controlled. Developer can run the Android tests/preview and open the implementation PR, and should disclose any missing physical access or deferred checks.

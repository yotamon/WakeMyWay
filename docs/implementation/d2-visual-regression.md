# D2 curated visual regression gate

**Status:** implemented in PR #34  
**Track:** Adaptive Dawn product-design track  
**Authority:** presentation regression protection only; no Alarm Kernel or Wake Runtime authority

## Goal

Protect the small set of product surfaces that currently define Wake My Way's visual identity without turning every preview or transient design experiment into a permanent pixel contract.

D2 therefore uses a **curated** visual-regression suite. A screen becomes a baseline only when it has been deliberately selected, rendered deterministically and visually reviewed.

## Toolchain

```text
Roborazzi   1.74.0
Robolectric 4.16.1
Graphics    Robolectric NATIVE graphics mode
SDK         35 for visual fixtures
```

The production Android toolchain remains unchanged. Roborazzi and Robolectric are test-only infrastructure.

## Canonical fixtures

The first reviewed baseline set contains exactly four product states:

```text
Tonight · Ready
Tonight · Empty
Wake Setup · Weekly
Wake · Emerging
```

Baseline PNGs live under:

```text
apps/android/app/src/test/screenshots/
```

The fixtures are intentionally few. Coverage should grow only when a state has enough product importance and visual stability to justify maintenance as a contract.

## Deterministic rendering contract

All first-generation fixtures use a fixed rendering environment:

```text
locale        en-US
viewport      393 × 852 dp
ui mode       night
screen density mdpi
Robolectric   SDK 35
```

Dynamic product facts are replaced only at the presentation seam needed for deterministic rendering:

- Wake Emerging injects a fixed `08:00` display time while production continues to use the real local time.
- Tonight uses fixed synthetic date/time/readiness facts.
- Wake Setup uses a fixed synthetic weekly schedule.
- no fixture reads device alarm state, network state, calendar state or current wall-clock time.

This seam does not create a second state machine or alter production behavior.

## Privacy contract

Visual fixtures are synthetic only.

They must never include:

- a user's Tomorrow Contract text;
- Prepared Wake Plan private content derived from a real user;
- credentials or tokens;
- microphone audio or transcripts;
- calendar/email/private context;
- raw reliability evidence tied to a user.

The visual lane has no network/cloud requirement for rendering.

## CI contract

Normal CI is read-only:

```text
reviewed PNG baseline in repository
             ↓
      Roborazzi verify
             ↓
      current rendering
             ↓
      pixel comparison
        ├─ match  → pass
        └─ drift  → fail + diagnostics artifact
```

The workflow runs:

```text
./gradlew :app:verifyRoborazziDebug --stacktrace
```

GitHub Actions permissions are `contents: read`. Normal verification cannot record or commit a new source of truth.

When verification fails, CI uploads available screenshot/report/test-result diagnostics so visual drift can be reviewed rather than hidden behind a generic test failure.

## Baseline update procedure

A visual change is not accepted merely because a developer intentionally changed the UI.

Baseline updates follow this sequence:

```text
1. render candidate goldens intentionally
2. inspect the actual PNGs
3. verify hierarchy, spacing, contrast, clipping and product tone
4. correct the UI when the screenshot exposes a product problem
5. commit only the reviewed PNGs
6. return CI to read-only verify mode
7. pass Android + reliability gates before merge
```

Do not make normal PR CI auto-record baselines.

Do not use a broad baseline update to hide unexplained rendering drift.

## What the first visual review caught

The first successful candidate render exposed a real product-design leak: the canonical Tonight surface visibly contained `Founder build` and an `Open Wake Lab` action.

Those affordances are useful while developing Wake My Way, but they are not part of the production product hierarchy and should not become visual truth.

D2 corrected this before accepting the baseline:

```text
TonightScreen
    └─ showDeveloperTools = false by default

WakeMyWayApp
    └─ derives developer visibility from
       ApplicationInfo.FLAG_DEBUGGABLE
```

Result:

- debug APKs retain founder diagnostics and Wake Alarm Lab access;
- non-debug product presentation does not expose founder chrome;
- canonical visual fixtures represent the product rather than the development console.

The first attempted implementation used `BuildConfig.DEBUG`, but this project does not generate `BuildConfig` in the current AGP setup. The final gate uses Android's existing application debuggable flag instead of enabling a new build feature only for one boolean.

## Fixture review notes

### Tonight · Ready

Accepted because the hierarchy remains clear at first glance:

1. WMW identity;
2. wake time;
3. Alfred presence/readiness;
4. tomorrow/context cards;
5. wake-plan actions.

Founder chrome is absent from the canonical product fixture.

### Tonight · Empty

Accepted as a calm empty state with one obvious primary action: set the wake time. It does not expose Wake Lab in the product contract.

### Wake Setup · Weekly

Accepted as a denser configuration surface. It remains vertically scrollable, preserves independent weekday times and does not require fitting every configuration control above the fold.

### Wake · Emerging

Accepted with intentional negative space. The sparse composition supports the low-cognition Emerging state rather than filling the screen for aesthetic density.

## Reliability boundary

D2 changes presentation/test infrastructure only.

It does not alter:

```text
AlarmManager trigger
      ↓
Alarm Kernel
      ↓
Active Wake Execution
      ↓
AlarmPlaybackService
      ↓
Stop / Snooze
```

It also does not alter Wake Runtime policy, motion evidence, private preparation authority or cloud architecture.

If visual verification infrastructure is unavailable, CI loses a presentation regression check; an already committed wake occurrence must remain unaffected.

## Future coverage

Do not add variants indiscriminately.

D7 accessibility/responsive hardening should deliberately earn additional visual fixtures for cases such as:

- large font scaling;
- narrow phone width;
- selected RTL/Hebrew layouts when product localization reaches that point;
- reduced-motion/static representations where visually meaningful;
- selected lock-screen/system-bar edge cases that can be rendered deterministically.

D4 later Wake-state fixtures should be added only when real Wake Runtime/platform facts are mapped into Engaged, Active, Oriented and Completion presentation. The visual suite must never invent a parallel behavioral state machine merely to create screenshots.

## Acceptance gate

D2 is complete only when:

- reviewed baseline PNGs are committed;
- normal visual CI has `contents: read` only;
- CI runs `verifyRoborazziDebug`, not automatic record;
- visual drift fails the dedicated workflow;
- diagnostics are uploaded when verification fails;
- standard Android CI remains green;
- API-36 reliability instrumentation remains green;
- no private user content exists in fixtures;
- no alarm/runtime authority moved into the visual layer.

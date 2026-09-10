# D4 Tonight experience redesign

**Status:** implemented on `design/tonight-premium-redesign` pending CI/review  
**Date:** 2026-09-10  
**Scope:** normal evening product surface + shared presentation primitives only

## Why this pass exists

The first Adaptive Dawn shell proved the navigation, token and component architecture, but the resulting Tonight screen still read too much like a themed prototype/dashboard:

- a full-field warm/brown wash dominated the atmosphere;
- the wake time competed with repeated `Tomorrow is ready` status language;
- stacked dark cards gave all information similar visual weight;
- the standalone WMW Presence looked closer to a placeholder than a product signature;
- `Founder build` and Wake Lab were too prominent for the normal consumer entry surface;
- the screen said that tomorrow was ready without first explaining the one thing the user actually needs to know: the next concrete wake and how far away it is.

This pass keeps the architectural work from PR #30 and the real product ownership from PR #32, but replaces the visible Tonight composition rather than polishing the old card stack.

## Product outcome

Tonight should answer these questions in order:

1. **When is my next wake?**
2. **How far away is it?**
3. **Can Android deliver the critical wake?**
4. **Is my optional private morning context prepared?**
5. **What can I change right now?**

The screen is deliberately not a calendar/weather portal, sleep tracker, chatbot home, or wake-sequence simulator.

## Canonical hierarchy

```text
WMW                                 Wake Ready

NEXT WAKE
08:00
Thursday · Sep 10
in 7h 42m

[ geometric WMW Presence ]  Alfred
                           Everything critical
                           is set on this device.

FOR THE MORNING
┌──────────────────────────────────┐
│ Wake              Thu · Sep 10   │
│                    08:00         │
│ ──────────────────────────────── │
│ Private context   Prepared locally│
│ ──────────────────────────────── │
│ Wake system       Ready           │
└──────────────────────────────────┘

[ Adjust wake ]
[ Edit private context ]

              Developer tools
```

For an empty state, the hero becomes a clear setup invitation rather than a decorative `--:--` clock and a stack of empty cards.

## Visual direction

### 1. Neutral night field

`Ink` remains the primary field. `Clay` and `Soft Ember` are no longer used as a full-height brown gradient.

`WmwCircadianSurface` now provides two bounded, non-critical depth cues:

- a very low warm horizon source near/below the bottom edge;
- a subtle neutral `Deep Dawn` depth field near the upper edge.

The visual still gets warmer as the presentation state advances, but the normal Tonight screen reads as premium charcoal/ink rather than sepia.

### 2. Editorial hero

The next wake is left-aligned and treated as the primary editorial object. The time uses tabular numerals where supported. Date and countdown sit directly beneath it.

This removes the old sequence of greeting → huge time → date → standalone presence → another huge `Tomorrow is ready` headline.

### 3. One compact morning surface

Wake occurrence, private context and Wake Ready are grouped into one raised surface separated by hairlines. This intentionally replaces three visually equivalent status cards.

The values come only from existing authoritative product state:

- Wake Occurrence from Alarm Kernel health;
- Wake Ready from Alarm Kernel capability/registration state;
- Tomorrow Contract/Prepared status from `WakePreparationManager`.

No fake calendar event, weather summary, generated insight, wake sequence, or runtime character behavior is shown.

### 4. WMW Presence becomes a signature, not a centerpiece placeholder

The canonical native geometric presence remains. It now has:

- a deeper neutral fill;
- a restrained state-colored contour;
- a state-driven internal horizon stroke;
- a compact placement beside the Alfred presentation label.

It remains explicitly **not** an AI orb, robot, mascot or circular progress meter. Its animation communicates presentation state only.

### 5. Developer chrome is demoted

`Founder build` is removed from the normal top bar. Wake Lab remains reachable through a low-emphasis `Developer tools` text action at the bottom of Tonight.

This preserves founder access without letting diagnostics define the consumer product personality.

## Interaction rules

### Ready state

- top-right Wake Ready status is visible but small;
- primary action edits the real Wake Schedule;
- secondary action edits/adds the real Tomorrow Contract;
- no extra readiness CTA is invented while the separate readiness-repair flow remains unimplemented.

### Attention state

- status changes to `Check wake`;
- the compact morning surface shows `Needs attention`;
- existing calm readiness detail is surfaced once inside the morning surface;
- visual color supports, but does not replace, textual state.

### Empty state

- no fake next-wake data is rendered;
- the screen explains that the critical alarm is local and account/network independent;
- one dominant action sets the next wake;
- Alfred/WMW Presence is quiet and does not claim to have prepared anything.

## Truthfulness boundaries

This pass intentionally does **not** implement several ideas that are visually tempting but product-false today:

- no bottom navigation until several durable top-level consumer destinations exist;
- no calendar agenda before M10 Useful Context owns real data;
- no weather card before useful-context ownership exists;
- no `Light → Music → Alfred` wake sequence preview because production sequence ownership is not wired;
- no generated Alfred insight or cloud AI call;
- no character selector before character choice has durable product ownership/runtime effect;
- no difficulty control before the real learning/runtime path owns it;
- no new behavioral state machine;
- no Alarm Kernel, Active Wake Execution, Stop/Snooze, Wake Runtime or Direct-Boot authority changes.

These omissions are intentional quality decisions, not unfinished visual placeholders.

## Shared component changes

### Tokens

- neutralized `NightSurface` / `ElevatedNightSurface`;
- added quiet/raised neutral surfaces;
- reduced global warm-glow alpha;
- added stronger hairline token for intentional raised surfaces;
- added compact Presence size.

### Typography

- refined display spacing and editorial hierarchy;
- enabled tabular figures for display time;
- added `labelSmall` for low-noise eyebrow labels;
- kept platform sans for now rather than adding a remote or unreviewed bundled font dependency.

### Components

- `WmwCircadianSurface`: bounded illumination instead of a full-height gradient;
- `WmwTimeDisplay`: supports alignment while preserving centered default for wake surfaces;
- `WmwCard`: neutral bordered surfaces with `QUIET`, `STANDARD`, `RAISED` emphasis;
- `WmwStatusPill`: smaller, quieter state treatment;
- `WmwDetailRow` + `WmwDetailDivider`: compact semantic information surface;
- `WmwSecondaryAction`: tactile outlined action instead of an unbounded full-width text button;
- `WmwPresence`: stronger native geometric identity with state-driven internal line work.

Because these primitives are shared, Wake Setup and Tomorrow Contract inherit the cleaner neutral surfaces/action language without creating a separate design system.

## Accessibility

- product copy is Android-resource backed;
- status always has text, never color only;
- touch actions preserve 48/56dp minimums;
- layout remains scrollable for large font sizes;
- the Presence keeps an explicit semantic description;
- no critical behavior depends on the presence, background illumination or animation;
- display-time alignment is presentation-only and WakeActivity keeps its centered default;
- platform font scaling remains enabled.

## Visual-regression sequencing

PR #34 was opened from the pre-redesign Tonight pixels. It should **not** freeze those pixels as canonical goldens.

After this redesign passes Android compile/lint and receives visual approval:

1. rebase/update the visual-regression branch on the accepted Tonight design;
2. record fresh synthetic Ready, Attention/Empty and Wake previews;
3. inspect the generated artifacts on representative dimensions;
4. only then switch the visual-regression workflow from record/review to verification.

Private Tomorrow Contract content remains excluded from screenshot fixtures.

## Acceptance criteria

The redesign is acceptable only when all are true:

- [ ] next wake time is the dominant first-glance object;
- [ ] full-screen brown/sepia wash is gone;
- [ ] `Founder build` is absent from primary consumer chrome;
- [ ] readiness appears once in the header and once as a compact semantic value, not as repeated large cards;
- [ ] one morning surface replaces the old card stack;
- [ ] WMW Presence reads as a deliberate geometric identity, not an AI orb/robot;
- [ ] no fake calendar/weather/wake-sequence/AI capability is introduced;
- [ ] Wake Schedule and Tomorrow Contract actions still route to their real product owners;
- [ ] Alarm Kernel / Wake Runtime / Direct Boot authority is unchanged;
- [ ] documentation validation, `:wake-core:test`, Android lint/Compose compile, instrumentation compile and debug APK assembly pass;
- [ ] API-36 reliability instrumentation stays green before merge.

## Next design work after acceptance

1. re-record/review Roborazzi goldens from the accepted surface;
2. implement the real Wake Readiness repair flow from platform capability state;
3. apply the same editorial discipline to Wake Setup and Tomorrow Contract based on physical-phone review;
4. only add new top-level navigation when real consumer destinations earn it;
5. continue the richer morning Wake presentation only behind its existing reliability/runtime gates.

## Physical phone review 01

The first APK review on a real portrait phone confirmed that the D4 Tonight direction is materially stronger, but also exposed where inherited D3 setup composition was still visible as engineering UI rather than a finished consumer experience.

Observed on device:

- the empty Tonight state is cleaner and correctly dominated by one setup action, but technical wording still leaked into the body copy;
- the small Alfred/WMW Presence reads better as a supporting identity than the old standalone object, but should stay secondary to the wake decision;
- Wake Setup still looked like a settings form because `Local wake`, `Schedule pattern`, two large bordered cards, explanatory paragraphs and the reliability footer were all visible at once;
- the time itself was visually strong, but unnecessarily trapped inside a large bordered card;
- the primary CTA copy `Make tomorrow ready` was less direct than naming the concrete wake being committed.

Changes made from that review in the same PR:

- removed the `Local wake` badge from Wake Setup;
- replaced the technical setup subtitle with concise consumer copy;
- flattened the schedule-mode selector out of its own card;
- reduced mode labels to `Tomorrow` / `Weekly`;
- removed the one-shot time hero from a containing card so the time can act as the visual object itself;
- reduced one-shot helper copy to `One wake. It won’t repeat.`;
- removed the technical reliability footer from the primary setup path;
- changed the new one-shot CTA to name the selected time (`Set wake for 08:00` shape);
- kept weekly editing in one quiet surface because seven day/time rows genuinely benefit from grouping;
- softened Tonight empty/Alfred wording so architecture facts remain true without reading like implementation documentation;
- renamed the low-priority founder escape hatch from generic `Developer tools` to the concrete `Wake Lab`.

This review reinforces a product rule for subsequent screens: **local-first architecture should create trust through behavior and concise reassurance, not through engineering vocabulary on every surface.**

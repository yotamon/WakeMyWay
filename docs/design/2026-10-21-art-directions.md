# WakeMyWay — Playful art directions (exploration v2)

## Why this pass
The first content-led mockup accurately reflected existing product behavior but read as a generic productivity UI. This pass explores distinct visual personalities **without adding product features or changing data/state**.

## Three competing directions
| Direction | Visual character | Trade-off |
| --- | --- | --- |
| Sunroom Club | Warm paper, gentle illustration, cheeky friendly sun, organic shapes, soft dimensional buttons | May need restraint and less gloss before final Compose implementation |
| Sleepy Cinema | Rich dusk plum, dreamy quiet sunset motif, light warm controls | Night-time Today deviates from approved light planning world and requires contrast/brand review |
| Happy Analog | Warm yellow, bold ink outlines, tactile paper/analog shadow treatment | Higher visual energy; requires careful accessible contrast and text-density checks |

Open `2026-10-21-screen-concepts-v2.html` locally to switch styles. It preserves the same source-grounded **Today**, **Edit Alarm**, and **Active Wake** surfaces from `2026-10-21-screen-concepts.html`. The Active Wake state controls are outside the mock phone, do not represent a consumer UI control, and switch between existing runtime states.

**Visual only**: The small smiling sunrise motif is a *proposal* departing from the current `docs/07-design-system.md` ban on anthropomorphic/mascot styling. It is explicitly **not** an added assistant avatar, product character picker, speech mode, live microphone or wake state. If selected, settle/update the brand contract separately before implementing; keep the canonical sunrise-wave mark intact.

## Existing product content preserved
- Today: next wake time/date, truthful reason precedence, local readiness, Tomorrow Contract preview, First Move, Alfred signature, edit action, Today/Alarms/You navigation.
- Editor: existing label, time, weekly/one-time schedule, day chips/date, three *bundled-when-available* sound choices, preview affordance, voice/snooze/morning context advanced disclosure, save.
- Active Wake: STARTING, LISTENING, MOVING, ORIENTING, COMPLETE; privileged reason/First Move only in ORIENTING; Stop and conditional Snooze remain visible.
- Original content inventory at `2026-10-21-content-led-inventory.md` remains the authority for the screen state/interaction matrix.

## Prototype checks performed
Rendered mockups at 393x852. Headless browser interaction assertions covered all three styles, navigation shells, advanced editor, weekly/one-time selection, listening state, oriented reason/First Move, completion, and snooze/stop visibility. No horizontal document overflow at 360x780, 393x852 and 430x932.

These tests exercise the **static HTML prototype only**; they do not verify Android Compose, real alarm delivery, microphone functionality, font scale, RTL, TalkBack, permission/repair states, screenshot parity or physical-device behavior.

## Next approval gate
Choose the visual direction or a deliberate hybrid. Then design/edit Android screens in a new implementation PR with retained runtime state ownership, complete conditional variants and physical visual QA. **Do not merge artwork into production solely from this exploratory study.**

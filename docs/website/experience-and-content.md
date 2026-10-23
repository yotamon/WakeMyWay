# Public website — experience, content and interaction specification

**Status:** Proposed Shape specification  
**Parent:** [Website product and brand plan](README.md)  
**Purpose:** Define what visitors see, understand and do, including crucial states; do not prescribe UI libraries or implementation architecture.

## 1. Content-first inventory and source of truth

Before high-fidelity design, collect the following **version-matched** evidence from the actual Android product/distribution target:

| Content/asset | Required source | Rule |
| --- | --- | --- |
| Canonical logo/wordmark, Wake Line, palette | [Design system](../07-design-system.md) and approved art assets | Do not regenerate or redraw brand geometry casually |
| Product screens: Tonight, alarm setup, Wake Ready, Active Wake, Oriented | Real reviewed captures from accepted product states | Capture genuine, current UI; mock sensitive values only in a clearly illustrative fixture |
| Wake experience story | [Product vision](../01-product-vision.md), [quality reset](../45-core-experience-quality-reset.md) | Describe the existing golden path; no imagined new modes |
| Alfred audio/sample dialog | Approved recorded scenario consistent with actual supported voice path | Representative, edited transparently; no false live-conversation implication |
| Privacy and voice capabilities | [Actual release inventory](../39-privacy-data-safety-launch.md) | Reconcile distribution differences before publication |
| Pricing / Free–Plus package | [Monetization plan](../24-monetization.md) + actual accepted store configuration | Do not publish unvalidated price hypotheses as current |
| Testimonials, statistics, reviews | Consented original user evidence + defined denominator | Omit until authentic, reviewable and permitted |
| Store badges, install links, beta eligibility | Verified current distribution targets | Never invent a store listing or available platform |

No screenshot, waveform or fictional dialog may silently present a future feature as shipping. Never show real private Tomorrow Contract details or calendar content.

## 2. Primary navigation and layout hierarchy

**Header:** canonical WakeMyWay lockup; at most a few informational links; one clear primary action. Keep compact on mobile. Support keyboard navigation and a visible focus state.

**Main content:** a sequence of distinct, scannable sections; each must answer one user question. Prefer open compositions over repetitive feature cards.

**Footer:** help/contact, privacy, applicable legal operator details, platform availability, brand copyright and supported locale links. No dark-pattern subscription or marketing opt-in buried in the footer.

**Mobile first:** use actual real-device captures or responsive layouts; never rely on desktop-only scroll effects to communicate value. Keep copy legible and button labels specific.

## 3. Homepage narrative (proposed sequence)

### Section 01 — Hero: recognize the real problem

**Visitor question:** "Is this for someone like me?"

Proposed:

- Eyebrow: "Brighter mornings. Your way."
- H1: **"Waking up is one thing. Getting up is another."**
- Explanation: **"WakeMyWay is an Android alarm built to help you move from the ring to the first real step of your day, with a more personal way to wake."**
- Main CTA (prelaunch): **"Get early access"**; main CTA (available Android distribution): **"Get WakeMyWay for Android"**.
- Secondary affordance: **"Hear a sample wake"** only when a reviewed audio demo exists; otherwise **"See how it works"**.
- Supporting proof: clearly marked current platform availability and a real app capture.

Avoid an above-the-fold claim that "the AI always wakes you", ambient audio autoplay or fake "Download on iOS" badges.

### Section 02 — The problem: familiarity without shame

**Visitor question:** "Why isn't an ordinary ringtone enough?"

Describe the gap between alarm dismissal and actual movement in specific, respectful language:

- "The alarm rings. You turn it off. Sometimes you barely remember doing it."
- "You meant to get up. Half-asleep you had another idea."
- "WakeMyWay focuses on the transition after the ring."

Avoid clinical claims, assumptions that all users have ADHD, forced guilt, or diagnoses. Use a modest before/after editorial illustration—not fake results or success-rate figures.

### Section 03 — How a morning works

**Visitor question:** "What exactly would happen?"

A simple, inspectable 4–5-step story:

1. **Choose a wake time** — set a normal Android alarm with clear readiness requirements.
2. **Give tomorrow some meaning** — optionally set a reason/first move; no obligation to share private details.
3. **The local alarm starts** — critical sound and available Stop/Snooze controls are independent of the cloud voice experience.
4. **Get moving your way** — where enabled and available, a brief voice interaction and bounded activity prompts help make the morning transition more concrete.
5. **Start the day** — orient toward a first action; future wake strategies may adapt based on supported local observations and feedback.

Explain that the exact experience depends on enabled preferences, app version, channel and permissions. Do not imply that voice or motion can biologically prove wakefulness.

### Section 04 — Experience the voice

**Visitor question:** "What does this actually sound like?"

Use a **reviewed, representative, prerecorded** 15–30-second demonstration, if available, matched to actual character/feature readiness.

**Controls and semantics:**

- Explicit play, pause, replay; never autoplay with sound.
- Visible playback state and duration; progress control if accessible.
- Accessible text transcript/captions and a text-only equivalent.
- Subtle Wake Line motion synchronized to playback; not a fake mic, animated AI orb or pseudo-live response.
- Loading, buffering, decode failure, unsupported format and reduced-motion states retain an intelligible fallback.
- No microphone permissions, live provider session or cloud usage required for a visitor.
- Do not call a curated clip "Talk to Alfred live" or suggest the user is conversing with an agent.
- Content and example phrase reviewed for character consistency, brevity, non-coercive prompts and no prohibited promises.

**Launch rule:** if no verified asset exists, replace the interactive player with a written example and app capture; do not block the entire site.

### Section 05 — Made for you, not one size fits all

**Visitor question:** "How is it different for different people?"

Explain clear user preferences, reasons and bounded future learning without claiming magical optimization or fabricating an inferred profile. Illustrate with a few explicitly hypothetical personas/scenarios only when each maps to accepted product capability.

Candidate content: calm or minimal presentation, meaningful wake reason, optional conversational assistance, observable movement evidence, calibration over time.

**Guardrail:** user-visible preference claims and learned-policy claims must match the actual release. Character catalogs and extra premium modes cannot appear as available unless shipped.

### Section 06 — Trust is a feature

**Visitor question:** "What happens if the internet or voice service fails?"

Use direct, plain-language answers:

- Core Android alarm delivery uses a local path within Android's supported reliability envelope.
- Voice is optional enrichment and can become unavailable.
- Stop/Snooze controls remain central and must match true kernel state.
- Privacy descriptions separate local wake data from optional cloud voice processing.
- No guarantee for powered-off devices, an app force-stopped under relevant OS behavior, or revoked critical permissions.
- Link to help, supported-device/permissions guidance and privacy policy.

Avoid a giant engineering diagram on the homepage. A more detailed FAQ/help article may explain readiness and platform restrictions.

### Section 07 — Real product gallery

**Visitor question:** "What will I actually install?"

Show a compact set of current app captures, ideally Tonight, alarm editing, Wake Ready and Active Wake/Oriented. Pair each with a one-sentence explanation of a user benefit. Artwork may frame the capture but cannot alter its UI, labels or controls.

**Gallery states:** compact/mobile, wide/desktop, missing media fallback, descriptive alt text, no crucial text embedded solely inside images. Avoid screenshot carousels that trap keyboard focus.

### Section 08 — FAQ and availability

Prioritize legitimate decision barriers, not a keyword-stuffed marketing accordion.

Suggested questions:

1. Is WakeMyWay available for my phone?
2. Does the alarm work without internet?
3. Is conversational voice always available?
4. Do I have to talk to the app?
5. Does WakeMyWay record or store my conversations?
6. Can it really tell whether I have gotten out of bed?
7. How is it different from a regular alarm?
8. Does it cost anything? What is Free / Plus *when available*?
9. How do I manage alarms, permissions or a failed voice session?
10. Where can I get help or submit feedback?

Answers must be distribution- and date-specific where necessary; link to maintained authoritative documentation.

### Section 09 — Final conversion

Short restatement of value, verified availability, one decisive action and a clear alternative for unsupported platform visitors. Do not mix several competing CTA labels or manufacture urgency/scarcity.

## 4. Full user journeys

### A. Qualified Android visitor, public distribution available

Discovery/referral -> homepage promise -> product explanation or demo -> verified official install destination -> install -> app onboarding -> first committed alarm.

**Site boundary:** the site can measure an outbound visit; install, first alarm and confirmed wake outcome require separate, privacy-reviewed evidence and may not be directly attributable.

### B. Visitor during closed beta

Homepage -> see accurate beta status -> early-access form or actual supported test invitation -> consent/acknowledgement -> submission -> confirmation (or verification if required) -> later separately controlled invitation.

**Form essentials:** only information needed for the beta; explicit purpose, consent/communication preference where needed, privacy link, clear response/availability expectation. No account required.

### C. Desktop or unsupported mobile visitor

Homepage -> understand Android-first status -> offer a non-misleading copyable install link, optional scannable code once official link exists, or a waitlist/notify option with explicit consent. Never imply a usable browser/iOS version.

### D. Voice demo visitor

Explicit play -> playback with transcript -> pause/replay/stop -> optionally learn more or join beta. On audio failure provide descriptive text and maintain the main CTA.

### E. Seeking support/privacy

Find Help/Privacy from header/footer and relevant sections -> read concrete answer -> use an actual support contact path or available store support mechanism. Privacy/delete requests must route to a real operational process, not a dead form.

## 5. State matrix

| Feature | Initial / normal | Loading | Empty / unavailable | Error / recovery |
| --- | --- | --- | --- | --- |
| Main CTA | One relevant current destination | Never artificially block hero | Honest platform/rollout notice | Fallback to help or another verified channel |
| Audio demo | Idle + visible transcript affordance | Clear buffer/loading indicator | Text equivalent and remove misleading player | Retry and readable fallback |
| Beta form | Minimal labeled fields | Pending; prevent duplicate submit | Closed/full beta explained without fake countdown | Accessible error, safe retry, no false success |
| Email confirmation | Precise confirmation step | If applicable, state processing honestly | Expired/invalid link resolved visibly | Resend/help path without exposing addresses |
| App gallery | Actual release captures | Stable aspect ratios, no layout jumps | Meaningful alt/copy fallback | Broken media never blocks CTAs |
| Navigation | Clear links and active location | Not applicable | Narrow/mobile menu | Escape/close/focus return |
| Unsupported device | Honest availability | Not applicable | Explicit Android-only state | Optional signup, not fake store button |
| Localization | Correct locale + switch where supported | No spinner for static content | Default language with no fake translated routes | Clear fallback; no mixed-language page |

## 6. Content governance and proof labels

Each public statement should have an owner and one of:

- **Shipped + supported:** exact app version/channel verified; safe to publish with necessary qualifiers.
- **Validated outcome:** backed by documented real-user evidence with definitions and denominators.
- **Conceptual explanation:** framed as how the product is designed, not an outcome guarantee.
- **Preview/demo:** transparently labeled and product-faithful, not a promise of universal behavior.
- **Planned/deferred:** not marketed as currently available.
- **Commercial hypothesis:** not treated as official price, trial or entitlement.

A copy/asset register should track: section; exact copy; evidence link; release channel; reviewer; locale; last review date; publication eligibility. Revalidate before each meaningful Android release or public product change.

## 7. Visual design details

Follow [the accepted design system](../07-design-system.md), do not recreate a separate web brand.

- Use the continuous sunrise/Wake Line motif, Midnight/Cloud/Paper/Sunrise/Dawn color roles and current Sunroom Club direction where appropriate.
- Premium, modern rounded sans direction; use approved licensed assets and ensure glyph coverage. Final font family remains a design choice, not a web implementation mandate.
- Animation tells the dusk-to-dawn story in small moments; it is never required to understand an instruction or CTA.
- No nested card grid merely to make the site look like a SaaS template.
- No smiling sun as an interactive agent; decorative artwork remains appropriately decorative.
- Responsive design must support long German strings and future RTL without redesigning content objects.
- Text/contrast/tap targets/focus states must work at high zoom and with reduced motion.

**Prototype gate:** validate the hero, audio section and complete mobile journey at low fidelity before approving an image-heavy polished screen. Do not treat a visually pleasing mockup as proof that the interaction works.

## 8. Localization and editorial consistency

English is a proposed initial editorial language, with German as a likely next locale and Hebrew/RTL as a future option; final rollout language is open. Design content structures so copy, alt text, SEO metadata, legal content and email confirmation text can be localized deliberately.

No unreviewed machine translation of legal copy or critical product limitations. URLs/locale switching must maintain page identity and avoid dead or duplicate-content pages.

## 9. Definition of Ready for visual sign-off

- [ ] Current Android feature/channel facts are reconciled with the actual distribution target.
- [ ] Hero copy and primary CTA are approved and understood in quick comprehension tests.
- [ ] Every required section has real content and proof labels.
- [ ] Key states from section 5 are defined in reviewed wireframes.
- [ ] Demo audio asset and transcript are approved, or a no-audio fallback is selected.
- [ ] Product screenshots and brand assets are approved for marketing.
- [ ] Mobile, accessibility and reduced-motion versions have been reviewed.
- [ ] Legal/privacy contact, consent and distribution details are concrete enough to ship.
- [ ] Unanswered decisions are explicitly tracked, not silently invented during build.

See [delivery-and-quality.md](delivery-and-quality.md) for implementation and publication acceptance.

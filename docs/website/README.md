# WakeMyWay public website — product and brand plan

**Status:** Proposed — Shape, not Build-approved  
**Date:** 2026-10-23  
**Scope:** Public marketing, trust, early-access and support website; not an alarm web client  
**Technology posture:** Implementation-neutral; no frontend framework, language, hosting service, CMS, analytics vendor, database, deployment platform, or repository topology is selected by this proposal.

This is the entry point for the website initiative. It documents the intended consumer experience and the decision criteria required before implementation, without committing the project to a technology stack.

Related specifications:

- [Experience, information architecture and content](experience-and-content.md)
- [Delivery, quality and decision framework](delivery-and-quality.md)

Authoritative existing context:

- [Product vision](../01-product-vision.md) and [domain vocabulary](../../CONTEXT.md)
- [Current product status](../00-project-status.md) and [1.0 launch gates](../35-paid-launch-readiness.md)
- [Core experience quality reset](../45-core-experience-quality-reset.md)
- [Approved design system](../07-design-system.md), [brand platform](../06-brand.md) and [reason-first convergence](../implementation/reason-first-calm-companion.md)
- [Monetization hypotheses](../24-monetization.md) and [privacy/data-safety inventory](../39-privacy-data-safety-launch.md)
- [Product development workflow](../34-product-development-workflow.md)

If this proposal conflicts with canonical product, design, or release truth, those sources win. Claims must be refreshed against the actual distribution build before publishing.

## 1. User, problem and desired outcome

**Primary audience:** habitual snoozers who choose a wake time with genuine intent but dismiss an alarm half-asleep, return to bed, or struggle to transition into activity.

**Underlying job:** "I already know when I want to wake up. Help me actually get moving, without unnecessary pressure or humiliation."

**Website visitor job:** "Within seconds, help me understand how WakeMyWay differs from a traditional alarm, whether I can trust and use it, what it costs/collects, and how I can try it."

**Website outcome:** qualified visitors understand the wake promise and take a transparent next step toward a real Android first wake. The site must not imply that a website visit or demo itself constitutes a successful wake.

**Main hypothesis:** an experiential, emotionally specific explanation of the sleep-to-action transition will communicate more value than a generic feature list or "AI alarm" positioning. Validate rather than assume.

## 2. Positioning and messaging hierarchy

The product is **a more personal way to move from hearing an alarm into beginning the morning**, built around a local alarm, guided activation and a wake strategy that can become more appropriate over time.

The product is **not** a web alarm, AI companion, general voice assistant, medical sleep solution, sleep-stage tracker, productivity suite, or guaranteed wakefulness detector.

### Brand continuity

Use the established consumer name **WakeMyWay** and the latest accepted brand system in [07-design-system.md](../07-design-system.md). The current approved brand line there is **"Brighter mornings. Your way."** The older brand exploration in [06-brand.md](../06-brand.md) also contains "Wake up your way."; it should not silently supersede the latest approved line.

The original sunrise + continuous Wake Line mark remains canonical. The newer **Sunroom Club** illustration direction may inspire warm, playful, *decorative* scenes where appropriate. Its smiling-sun detail is not an assistant, launcher replacement, speaking mascot, or product capability.

### Proposed consumer copy, subject to content review

- **Primary problem/hero concept:** "Waking up is one thing. Getting up is another."
- **Explanatory subhead:** "An alarm designed to help you move from the first ring to the first real step of your day, in a way that works for you."
- **Supporting idea:** "A wake time is a commitment. Your morning deserves more than a ringtone."
- **Voice feature language, when actually enabled:** "Optional voice check-ins can make the experience feel more natural and responsive."
- **Trust message:** "Your alarm starts locally. Conversation is an optional extra, not the thing that makes the alarm ring."

These are proposed editorial directions, **not verified conversion winners** or permission to promise features before release.

### Voice principles

Human, concise, quietly warm, capable, with restrained wit. Avoid guilt, urgency theatrics, miracle claims, excessive anthropomorphism, diagnosis, fear of missing obligations, and terms such as "unlimited AI", "100% reliable", or "guaranteed to get you out of bed".

## 3. Site goals and priorities

| Priority | Goal | Visitor evidence | Business evidence |
| --- | --- | --- | --- |
| P0 | Explain the difference between waking and getting up | Correct unaided description after a brief view | Qualified CTA engagement |
| P0 | Build trust without concealing limitations | Can identify Android availability and voice/offline limitations | Fewer confused signups/support contacts |
| P0 | Convert interest into an appropriate next step | Can complete early-access or install journey | Verified, consented signups or store referrals |
| P1 | Demonstrate the character of an actual wake | Can interpret a truthful recorded demo | Demo usage paired with qualitative feedback |
| P1 | Reduce pre-install uncertainty | Privacy and FAQ answers are clear | Lower abandonment / higher quality acquisition |
| Later | Educate/retain/convert to Plus | Understands current Free vs Plus offer | Paid conversion once entitlement and proof gates pass |

Traffic, scroll depth, and audio plays are diagnostic—not the product outcome. Real wake effectiveness must be evaluated through the separate app/beta program.

## 4. Proposed information architecture

A deliberately small public surface, with progressive expansion:

| Surface | Purpose | Stage |
| --- | --- | --- |
| Home | Emotional promise, quick explanation, proof and one primary CTA | MVP |
| How it works | Calm, concrete explanation of evening preparation, alarm, optional voice, activation and first move | MVP if home becomes too dense; otherwise immediately after |
| Early access | Eligibility, expectations, optional invite capture, confirmation and privacy context | MVP while distribution is limited |
| Help / FAQ | Compatibility, setup, alarm readiness, permissions, voice and contact path | MVP |
| Privacy notice | Website data flows and links to relevant app privacy information | Before collecting data / public launch |
| Legal/contact disclosure | Operator identity and legally required notices for target markets, where applicable | Before public launch |
| Plus | Truthful currently available packages and purchase path | After paid gate acceptance |
| Journal/resources | Evidence-based waking guidance and organic discovery | After core acquisition validates |
| Press/media kit | Brand assets, contact, verified product facts | Only when needed |

Route names, file names, host names and URL structure are **implementation choices**, not contractual path requirements. The consumer-facing names and destinations must remain discoverable and stable once published.

## 5. Non-negotiable product constraints

1. **No browser alarm:** the public site must not imply that a page left open can deliver an Android-grade alarm. No coupling to Alarm Kernel, Wake Runtime or scheduled occurrences.
2. **Reliability truth:** platform limitations, permissions and physical-device evidence govern claims. The browser site cannot prove overnight alarm reliability.
3. **Voice truth:** optional Realtime conversation and availability vary by actual release channel. Distinguish critical local sound from optional cloud voice. No blanket "all offline" or "AI never fails" claims.
4. **Outcome truth:** Activation Completion and Confirmed Wake Success are separate domain concepts. Do not state "proven to get you up" without properly qualified evidence.
5. **Privacy:** no raw wake recordings, Tomorrow Contract examples from real users, calendar details, profile data or private screenshots published without explicit, appropriate authorization. Prefer fabricated *illustrative copy* visibly marked as such, and product-faithful visual states.
6. **Commerce:** Free must be represented as a useful product. No ads during waking, no purchase manipulation, no paywall-on-wake message. Planned Plus prices, preview counts and trial terms remain hypotheses until supported by release configuration and evidence.
7. **Platform accuracy:** Android-first; do not announce iOS availability, public Play availability, auto-sync or other deferred capabilities before they exist.
8. **Brand cohesion:** reuse the approved Wake Line, sunrise motif, colors, typography direction and illustration boundaries. No unrelated AI orb, robot, fabricated voice persona or fake app interface.

## 6. Visual north star — "From Dusk to Dawn"

**Concept:** a quietly editorial, premium, slightly playful sunrise experience, grounded in actual app visuals.

- **Atmosphere:** Cloud/Paper warmth, Midnight ink, sunrise accents and generous breathing room, referencing the native planning environment.
- **Journey:** evening intention → early dawn → warmer morning. The shift is a storytelling layer, not a required scroll animation.
- **Signature:** the continuous Wake Line/horizon functions as brand motif and optionally responds subtly to demo playback.
- **Product proof:** real, version-matched app captures, not invented UI or stock smartphone interfaces.
- **Personality:** Alfred is heard through genuine voice and carefully edited copy; neither a generic chat surface nor a speaking sun mascot.
- **Motion:** purposeful, subtle, bounded, pausable; all meaning and controls remain available when motion is reduced or disabled.
- **Sound:** user-initiated only, with explicit play/pause/replay, captions/transcript, and no assumed microphone access.

Site branding must remain consistent with [the current design system](../07-design-system.md), including the 2026-10-21 Sunroom Club exception. Detailed section and state contracts live in [experience-and-content.md](experience-and-content.md).

## 7. Commercial and release posture

**Prelaunch / gated beta:** one clear CTA such as "Get early access" with truthful availability, eligibility and timing. Do not promise an instant invitation or a public download that is not available.

**Public Android launch:** one clear verified store/install CTA; desktop and unsupported devices receive an appropriate alternate path or explanation. Direct APK distribution is not assumed to be public.

**Paid rollout:** introduce Plus only after its release gate, real entitlement behavior, supported feature set, store-localized pricing and legal copy are reconciled. No payment collection on a public marketing site by default; the purchasing boundary must be explicitly reviewed if that changes.

**Launch dependency:** the marketing site can publish accurate early-access messaging before WakeMyWay's Android paid readiness gates close. Public, paid and reliability promises remain gated by [35-paid-launch-readiness.md](../35-paid-launch-readiness.md).

## 8. Non-goals

- Building or simulating an alarm in the browser.
- New WakeMyWay app features, character catalog, integrations or wake runtime changes.
- Account dashboard, alarm editing, in-browser voice conversation or AI support agent.
- Complex CMS, broad blog, gamification, referrals, affiliate marketplace or speculative SEO pages at MVP.
- Selecting a permanent frontend/hosting/data stack as a side effect of this plan.
- Large paid-acquisition programs before app retention and economics are validated.

## 9. Evidence and advancement

This proposal is currently **Shape**. It advances to Build only when the relevant content inventory, proof/claim matrix, real app assets, CTA/distribution availability, privacy collection decision, meaningful UX states and design review are accepted. See [delivery-and-quality.md](delivery-and-quality.md) for the executable Definition of Ready, staged delivery and release gates.

**Decisions intentionally open:** final hero copy, exact section ordering, demo asset availability, CTA destination, site languages, domain/operator disclosure, measurement policy, hosting/framework/data-storage choice and website repository location. None is selected by implication here.

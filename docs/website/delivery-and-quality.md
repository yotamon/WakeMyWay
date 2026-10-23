# Public website — delivery, quality and decision framework

**Status:** Proposed; technology-neutral and not Build-approved  
**Parent:** [Website product and brand plan](README.md)  
**Companion:** [Experience and content](experience-and-content.md)

This is a delivery contract, **not a selected stack**. Teams may implement the accepted outcomes using any appropriate technology after a lightweight, evidence-based technical decision. The site must remain operationally independent from the waking path.

## 1. Architectural requirements, not vendor decisions

### Required properties

| Area | Outcome/constraint | Choice deliberately left open |
| --- | --- | --- |
| Rendering | Fast, indexable public pages, robust with minimal browser scripting | Static generation, server rendering or hybrid; build system and framework |
| Frontend | Responsive, accessible, content-first components and real media | Language, styling system, UI library |
| Hosting | Secure, observable and low-maintenance production service with preview/review capability | Host, CDN, edge/server model, region |
| Domain | Controlled brand domain, HTTPS, predictable redirects | Registrar, DNS, certificate provider and final domain |
| CMS/content | Simple reviewable copy, translation and asset versioning | Files, headless CMS or other editorial workflow |
| Forms | Privacy-respecting beta capture and spam protection if early access is open | Managed forms, server endpoint, provider, storage |
| Data | Minimal collection, disclosed processors, restricted access, deletion and retention | Database or no database; storage provider |
| Analytics | Useful opt-in/legitimate-basis-appropriate acquisition measurements without sensitive data | Provider, in-house metrics or none |
| Localization | Clear language selection, correct SEO metadata and accessible copy | Localization package, translation platform |
| Media | Versioned brand assets, real app captures and user-initiated audio | Asset pipeline, player technology, delivery service |
| Deployment | Auditable changes, safe rollback, preview isolation | CI system, infrastructure and repository layout |
| Support | Reachable help/feedback workflow and published response expectations | Helpdesk, contact form, mailbox, issue tracker |

**Non-negotiable separation:** marketing failures, traffic spikes, forms, billing experiments and website deployments must have **zero authority** over the Android Alarm Kernel, Wake Runtime, scheduled alarms and active Stop/Snooze behavior.

Integration with existing account or cloud components is **not** assumed. A new cross-service dependency must be justified by a real user outcome and security review.

### Technology selection scorecard

Compare realistic implementation candidates before Build. Suggested relative weights are a starting heuristic, not vendor lock-in:

| Criterion | Weight |
| --- | ---: |
| Performance / crawlability / graceful degradation | 25% |
| Simplicity and long-term maintenance cost | 20% |
| Accessibility and content iteration velocity | 15% |
| Security, privacy, form/data lifecycle | 15% |
| Deployment, preview, rollback and reliability | 10% |
| Asset, audio and localization support | 10% |
| Portability and escape cost | 5% |

For every material choice, document at least two credible options (when they exist), total expected complexity, recurring costs, lock-in, team familiarity, operational failure modes, trade-offs and why the chosen option meets the contract. A stack change should not force changes to the product promise or marketing copy.

**Proposed topology principle:** keep website code and deployment isolated; whether it belongs in this monorepo or a separate repository remains open and should be chosen on shared-assets, governance, CI isolation and release-coupling evidence. Avoid introducing an app-code dependency for decorative branding.

## 2. Work packages and dependencies

### Phase 0 — Truth and launch prerequisites (first)

- Confirm exact product version and distribution channels being marketed.
- Build a public-claim ledger with shipped/planned/validated status, release date and source of truth.
- Confirm brand assets, canonical logo, actual app captures and demo suitability.
- Check domain ownership/availability, trademark status, entity/operator information and contact route.
- Decide whether the public site is a gated beta or public Android-install funnel.
- Confirm the minimal languages, target regions, privacy/data-collection purposes and what can legally be published.
- Explicitly approve whether to collect beta emails at all; use a no-form CTA if a real beta invitation route already serves the same purpose.

**Exit artifact:** accepted content inventory, claim ledger, CTA/distribution contract, working support ownership and list of publication blockers.

### Phase 1 — Content and design Shape

- Review 2–3 contrasting visual explorations within the *existing* brand—not arbitrary new visual brands.
- Produce responsive wireframes for Home and the complete primary journey, including special/error states.
- Resolve exact page inventory, hero/messaging, navigation hierarchy and copy for initial locale.
- Validate a real product screenshot gallery, audio transcript/demo plan or its no-audio fallback.
- Review keyboard, screen-reader, zoom, language overflow and reduced-motion interaction semantics.
- Perform short comprehension/usability reviews with intended first-time users, not just the product author.

**Exit artifact:** approved content/UX specification, prototype evidence, acceptance states and unresolved-decision register.

### Phase 2 — Website MVP Build

Build the smallest coherent public experience:

- Brand-consistent responsive home, clear navigation and footer.
- Accurate product explanation and screenshots.
- One appropriate primary CTA with correct platform and distribution routing.
- How-it-works content as a route or section as justified by usability.
- FAQ/help/contact and required privacy/legal pages.
- Optional recorded audio demo only if approved; otherwise faithful accessible text.
- If email capture is selected: minimum necessary fields, server-side validation, anti-abuse controls, explicit disclosures and confirmation/retry states.
- Search metadata, semantic navigation/headings, social preview artwork, stable URLs and appropriate indexing/redirect behavior.
- Traceable content/asset provenance; no fake reviews, awards, prices or unshipped features.

**Exit artifact:** reviewable deployment/prototype, updated claim ledger, testable primary journeys and a complete manual QA checklist.

### Phase 3 — Harden and limited publication

- Validate the actual site at realistic mobile sizes, constrained networks, browser/OS combinations and large text/zoom.
- Test real keyboard and assistive-technology paths, including audio controls and form errors.
- Verify target URLs, beta response path, email delivery/opt-out if enabled, support routing and failure states.
- Audit cookies, third-party requests, privacy notice, applicable operator disclosure and retention/deletion process.
- Check security headers, TLS, rate limiting/spam handling, secret management, backup/restore if relevant, and preview access rules.
- Confirm performance and search-readiness targets from the production-equivalent build, not only local development.
- Reconcile live marketing copy against a dated Android release inventory immediately before public publication.
- Confirm monitoring, change ownership, release rollback and incident contact.

**Exit artifact:** documented go/no-go checklist, validation evidence and publication approval. The marketing launch must not overstate 1.0 app-readiness gates.

### Phase 4 — Learn and expand only on evidence

- Review qualitative comprehension, signup eligibility, store referrals and support themes.
- Iterate hero clarity and CTA placement before enlarging page inventory.
- Publish Plus and verified localized pricing only when entitlement/store/operational gates are accepted.
- Add translations, resources/SEO, press material and experiments when demand makes them worth maintaining.
- Reassess site technology only when product/operational evidence reveals a genuine problem.

## 3. MVP acceptance criteria

### Product/brand

- [ ] A new target visitor can describe what WakeMyWay is and is not after seeing the hero and one explanatory section.
- [ ] No current page sells the product as a generic AI companion or medical sleep product.
- [ ] Wordmark, Wake Line and decorative scenes match the accepted brand contract.
- [ ] App screens, sounds, copy, voice and prices are approved against actual shipped or intentionally gated capabilities.
- [ ] Users can identify the supported platform and availability state without navigating to a legal page.
- [ ] At most one visually primary conversion action appears per decision point; secondary paths remain accessible.
- [ ] Full path to a working beta signup or verified install destination is tested.
- [ ] Every page has a useful non-JavaScript/readable failure experience where reasonably possible.

### UX/accessibility

- [ ] Semantic headings, landmarks, accessible names, keyboard traversal, focus indication and skip-to-content.
- [ ] Text alternatives for meaningful media; decorative art does not duplicate screen-reader narration.
- [ ] Audio is never autoplayed, has transcript and accessible play/pause/replay/error semantics.
- [ ] Honors reduced-motion settings; all critical meaning is available without animation.
- [ ] Contrast, zoom/reflow and interactive target size satisfy the chosen WCAG 2.2 AA conformance objective (with documented audit).
- [ ] Mobile small-screen layout, long translated labels, landscape, slow network and browser back behavior are reviewed.
- [ ] Form errors are field-associated, announced accessibly and do not delete entered data on retry.
- [ ] Dialogs, menus and optional carousels do not trap focus.

### Performance/SEO

Use Core Web Vitals as performance targets on representative mobile conditions, measured at the 75th percentile when sufficient real-user data exists, otherwise with documented lab evidence:

- [ ] Largest Contentful Paint **2.5 s or less**.
- [ ] Interaction to Next Paint **200 ms or less** when measurable.
- [ ] Cumulative Layout Shift **0.1 or less**.
- [ ] No hero-blocking heavy animation/video/script required to display the primary message or CTA.
- [ ] Crawlable, meaningful page content and correct metadata for each published locale/page.
- [ ] Canonical URLs, sitemap, robots/indexing behavior, social sharing previews and appropriate redirects.
- [ ] Structured data only when relevant and backed by published, current product facts (no fake ratings/availability).
- [ ] Error pages and removed/moved routes behave deliberately.

### Privacy/security/operations

- [ ] Accurate, reviewed website privacy notice; separate website collection from in-app/on-device/cloud voice processing.
- [ ] Appropriate lawful basis and consent management for selected analytics, cookies and mailing workflows; avoid nonessential tracking.
- [ ] Minimal input, abuse protection, retention/deletion and access controls for any signup form.
- [ ] No raw user wake audio, private alarm reasons, calendar data or sensitive wake history in analytics, demos or logs.
- [ ] Verified controller/operator/contact details and applicable jurisdiction-specific legal disclosures.
- [ ] No credentials embedded in frontend bundles or publicly accessible logs.
- [ ] Secure transport; security headers/protective configuration appropriate to the implementation.
- [ ] Working support and data-request handling process.
- [ ] Preview, deploy, rollback and ownership procedures documented; production site independent of alarm execution.
- [ ] No unproven live voice, Play availability, platform support, feature or subscription claims.

## 4. Measurement framework and event boundaries

Measurement must be defined from a **data-minimization purpose**, not "collect everything and segment later".

| Question | Candidate signal | Important caveat |
| --- | --- | --- |
| Does the hero communicate the promise? | Five-second test + comprehension interviews | Cannot infer comprehension from scroll alone |
| Does the demo build interest? | User-initiated audio start/completion (aggregated) + feedback | Demo engagement is not wake effectiveness |
| Are people seeking access? | Eligible completed opt-ins or verified invite requests | Count consented, valid contacts rather than raw submissions |
| Does the site guide public installers? | Official store referral clicks | Outbound click is not install |
| What confuses visitors? | Structured anonymous feedback/support themes | Avoid uploading identifiable wake details |
| Does acquisition produce value? | App-side first committed wake and calibrated outcomes through a separate approved study | Do not assume site-to-app identity matching or count Activation Completion as confirmed success |

Use consent-aware aggregate reporting where appropriate. Clearly document definitions, sampling, duplicate handling, region/channel segmentation and retention. Avoid third-party tags until their privacy/cost/value impact is reviewed.

There is no justified hard conversion target before baseline observations. Choose targets after the first review cycle rather than presenting hypotheses as business facts.

## 5. Legal and distribution checklist

Treat legal publication requirements as operational acceptance, not generic boilerplate:

- Confirm the actual website operator and market jurisdictions, then obtain applicable operator disclosure, terms if needed and privacy notice.
- Review GDPR data-controller/processor roles, data flows, transfers, retention and data-subject requests for **the actual selected stack**.
- Distinguish necessary site operation from marketing email signup. Avoid pre-checked marketing consent and misleading wording.
- If mailing invitations or updates, provide the required evidence of consent/verification and an operational unsubscribe/withdrawal process as applicable.
- If analytics or cookies are added, determine whether and how consent is required before collection.
- Confirm domain control and trademark clearance; an earlier availability lookup does not establish ownership.
- Confirm Android distribution method/permissions and official listing rules before showing badges or screenshots.
- Re-review the public claims, page locales, prices and privacy copy after changes to the shipped app or processors.

Legal questions require appropriate current review; this planning file is not legal advice.

## 6. Decisions intentionally deferred to Build readiness

| Decision | What must be known before choosing |
| --- | --- |
| Primary CTA and destination | Real distribution availability (closed beta vs official Android install), owner for invitation process |
| Domain and public identity | Domain control, trademark status, operator/contact details |
| Initial locales | Target audience, translation/review ownership, support ability |
| Final hero and section order | Comprehension/usability evidence; approved product-fact matrix |
| Audio demo | Release-matched, consent-cleared sample and transcript; fallback approved |
| Website code location | Maintenance ownership, release isolation, asset sharing and build/deploy cost |
| UI/rendering approach | Page count, content updates, performance and interaction complexity |
| Hosting/service topology | Availability, cost, preview environments, security/regulatory needs, operational preference |
| CMS/content workflow | Number of editors, localization, approval frequency and cost |
| Form/storage provider | Whether capture is necessary, region, retention, abuse prevention and access policy |
| Analytics/consent tooling | Measurement goals, privacy/legal basis, script cost, supportability |
| Support mechanism | Named owner, response expectations, privacy/reporting requirements |
| Plus/pricing pages | Gate 3 evidence and Gate 4 entitlement/store readiness |

A decision should be recorded **after** comparing options and should be revisitable at reasonable cost; do not treat examples used during planning as mandatory libraries or vendors.

## 7. Risk register

| Risk | Mitigation / release guard |
| --- | --- |
| Site promises experience not present in Android build | Signed-off claim ledger, capture provenance and channel-specific copy review |
| Voice demo overstates conversation quality | Reviewed recorded clip, explicit "sample", honest availability and transcript |
| A polished site masks weak app readiness | App gates remain authority; distinguish early access from public/paid availability |
| Heavy scroll effects hurt performance/accessibility | Content-first rendering, reduced-motion fallback and strict performance budget |
| Early access form creates legal/security overhead | No-form alternative first; collect minimum with retention and deletion ownership |
| Copy/brand diverges from app | Reuse accepted design direction and release assets; designated reviewer |
| Hosting/stack lock-in from initial enthusiasm | Weighted scorecard and minimum portable content/asset contracts |
| Unbounded SEO/marketing work distracts from 1.0 core | MVP phase gate; defer journal/growth until real acquisition evidence |
| Pricing and app channel drift | Official distribution inventory and commerce gate review |
| Support/legal page is nominal but non-operational | End-to-end contact/data request verification before publishing |

## 8. Definition of Ready for the first Build PR

- [ ] The website is explicitly approved to enter Build as a separate, non-critical marketing workstream.
- [ ] Core audience, promise, first conversion and platform availability are settled.
- [ ] Homepage sections, states, content and important responsive interactions are reviewed.
- [ ] Real brand/app assets are available or approved fallbacks exist.
- [ ] Production claim matrix and actual release-channel distinctions are signed off.
- [ ] Beta form choice, consent, privacy and support operations have named owners.
- [ ] Performance, accessibility, security and release acceptance criteria are accepted.
- [ ] Technical options have been scored and a reversible implementation decision is recorded without changing this product-level contract.
- [ ] Launch blockers and excluded features are visible in the implementing issue/PR.

Until then, stay in Shape and use disposable prototypes as needed. **Do not create a production website merely to resolve an unanswered product decision.**

## 9. Proposed implementation slices after approval

Prefer small reviewable vertical slices, with no requirement that a framework or host be chosen before Shape sign-off:

1. **Brand shell + home narrative:** responsive layout, core message, real app captures and current platform CTA.
2. **Trust + help + legal:** how-it-works, FAQ/support, accurate privacy/operator information.
3. **Conversion:** verified store routing or beta collection with error/confirmation/consent states.
4. **Experience demo:** reviewed audio, accessible transcript and optional Wake Line animation if evidence/assets support it.
5. **Harden + publish:** browser/device testing, a11y/security/privacy/performance audits, search metadata, operations and go/no-go.
6. **Learn:** observe the user journey; refine copy and information architecture; expand only on validated need.

**Deferred tests:** There is no runnable website in this documentation PR, so no application E2E, visual regression or hosting verification is appropriate yet. Those are required in the corresponding implementation/hardening stages.

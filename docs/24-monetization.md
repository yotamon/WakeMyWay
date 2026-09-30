# Monetization and unit economics

**Status:** Shape-stage commercial plan  
**Last updated:** 2026-10-01  
**Consumer plan name:** WakeMyWay Plus  
**Launch market assumption:** Android / Google Play first  
**Decision owner:** product owner  
**Evidence status:** pricing, trial and cost thresholds remain launch hypotheses until Gate 3 beta evidence validates them

## Decision

WakeMyWay should use a **Free + Plus subscription model**.

The free product must remain a trustworthy, genuinely useful local-first wake product. Plus monetizes the recurring value created by richer personalization, high-quality conversational waking and premium adaptive experiences.

The business model is intentionally aligned with the product promise:

> **Free earns trust and distribution. Plus earns revenue by making the wake experience meaningfully more personal, natural and effective over time.**

WakeMyWay is not sold as "AI access", token volume or a bag of gated settings. The product is sold around the outcome: helping a habitual snoozer actually get moving with the minimum effective friction that works for them.

## Why this model fits WakeMyWay

WakeMyWay has an unusually favorable cost boundary for a subscription product because the trust-critical product remains local-first:

- the Alarm Kernel does not require paid inference;
- the Wake Runtime remains deterministic and local;
- motion evidence remains local;
- the base wake experience can remain useful without a cloud session;
- local learning can continue without turning every free morning into a provider bill;
- Realtime voice is optional enrichment and may fail without weakening the alarm.

That makes a freemium model economically plausible even if the free user base becomes much larger than the paid base.

The free tier is therefore not a crippled demo. It is the distribution and trust engine.

## Hard commercial rules

These rules are durable unless deliberately reversed in the decision log.

- **No advertising.** In particular, never monetize the cognitively vulnerable wake moment with ads.
- **No paywall during Active Wake.**
- **No purchase decision during Active Wake.**
- **No accidental half-awake purchase surface.**
- **No subscription state in Alarm Kernel or Wake Runtime authority.**
- **No committed alarm may become less safe because billing, account, network or entitlement failed.**
- **No "unlimited AI" promise.** Plus should support normal daily personalized waking while internal budgets keep provider cost bounded.
- **No weekly subscription at launch.**
- **No lifetime plan at launch.**
- **No consumable credit economy for ordinary waking.**
- **No intentionally bad free alarm to manufacture conversion.**
- **No provider or model name in the core consumer value proposition.**
- **No paid acquisition at meaningful scale until retention and contribution economics are measured.**

## Consumer packaging

### WakeMyWay Free

Free is a complete, safe alarm product for a person who wants more help getting moving than a conventional ringtone provides.

Intended free value:

- unlimited normal Alarm Definitions supported by the product;
- reliable local alarm delivery and Active Wake controls;
- deterministic Wake Runtime;
- physical activation / motion evidence;
- the default local wake character / presentation available in the release;
- basic Tomorrow Contract behavior that is already part of the accepted local product;
- basic Wake Learning and calibration;
- core Wake Profile / explicit preferences needed for the base product;
- basic wake history and outcome feedback;
- local/offline fallback whenever premium cloud enrichment is unavailable;
- no ads.

A free user should be able to recommend WakeMyWay because the free product actually helps them wake.

### WakeMyWay Plus

Plus is for users who want WakeMyWay to feel increasingly like a wake experience designed specifically for them.

Launch-value intent, subject to implementation and Gate 3 proof:

- production-quality personalized Realtime conversational waking;
- richer voice/personality expression beyond the base local experience;
- deeper personalization of tone and conversational style using explicit Wake Preferences;
- premium character choices when additional characters are actually production-ready;
- richer post-wake insights/history where evidence shows they help users understand improvement;
- premium contextual enrichment only when those capabilities are separately accepted and shipped;
- future premium personalization that increases outcome value without making the free alarm intentionally ineffective.

Only implemented, validated capabilities may appear in store or paywall copy. This document describes the intended entitlement boundary, not permission to market unshipped features.

### What remains free even after Plus exists

Do not gate the following merely to create subscription pressure:

- critical alarm delivery;
- Stop and Snooze safety;
- alarm readiness/repair;
- deterministic wake progression;
- enough local interaction to make Free a legitimate WakeMyWay product;
- user control over privacy and local data;
- accessibility;
- calibration needed to keep product claims honest.

## Consumer name

The consumer-facing paid plan is **WakeMyWay Plus**.

Existing technical names such as `PRO` in commerce configuration are implementation details and do not force consumer copy to say "Pro". Renaming technical identifiers is a separate engineering change and should happen only when useful, not as part of this documentation-only plan.

## Premium Preview: prove value before asking for payment

WakeMyWay should not greet a new user with a hard paywall before they have experienced a real morning.

The intended first-value journey is:

~~~text
Install
  ↓
Understand the wake promise
  ↓
Create first real alarm
  ↓
Premium Preview wake #1
  ↓
Premium Preview wake #2
  ↓
Premium Preview wake #3
  ↓
Show a truthful personalized observation / value recap
  ↓
Present WakeMyWay Plus after the wake, never during it
  ↓
User may start the store trial
  ↓
Annual or monthly subscription
~~~

### Premium Preview contract

The first **three qualified wakes** should include the Plus conversational/personalized experience without requiring a payment method.

A preview wake is consumed only when the premium experience actually worked well enough to demonstrate value.

Do not consume a preview when:

- the alarm never legitimately fired;
- Realtime was unavailable before meaningful engagement;
- the provider failed before a usable exchange;
- the app fell directly back to the normal local wake path;
- a developer/test wake is explicitly marked non-consumer;
- the user did not actually reach the premium portion because of a product failure.

The exact "qualified preview" event should be defined before implementation using semantic evidence, not arbitrary elapsed time.

### Why preview before trial

The preview has a different job from the store trial:

- **Preview** proves the product concept with no payment commitment.
- **Trial** lets an already-interested user experience Plus as a continuing routine before the first charge.

This avoids asking a half-convinced user to start billing before WakeMyWay has demonstrated the distinctive value.

## Subscription offer

### Launch pricing hypothesis

| Offer | Working price | Role |
|---|---:|---|
| Monthly | **€7.99 / month** | flexible option |
| Annual | **€49.99 / year** | primary/default offer |
| Store trial | **7 days** | conversion hypothesis after Preview |
| Weekly | not offered | intentionally excluded |
| Lifetime | not offered | intentionally excluded |

At €49.99/year, the effective monthly price is about **€4.17**.

Compared with paying €7.99 for twelve months (€95.88), the annual offer is about **48% cheaper**. This is intentionally meaningful enough to encourage annual commitment without making monthly look punitive.

The paywall should lead with annual, while monthly remains plainly available.

### Pricing is a hypothesis, not a promise

Do not hardcode these prices into Android UI.

Google Play ProductDetails remains the consumer price source. The Play Console may use localized price points by country and currency.

Before public paid rollout, Gate 3 must test the actual package and price after users have experienced multiple real wakes.

The preferred sequence is:

1. validate the wake outcome;
2. validate repeat use;
3. expose the real package;
4. observe Preview → trial and trial → paid behavior;
5. adjust price only when evidence identifies a price/value mismatch.

Do not discount reflexively to solve a product-value or retention problem.

## Paywall timing and UX

The best paywall moment is after demonstrated value, not before the first alarm.

Preferred trigger:

- after three qualified Premium Preview wakes; or
- after an earlier strong value moment if the user explicitly asks to continue/unlock Plus.

Do not interrupt a wake or immediately cover the post-wake success state with commerce.

A useful sequence is:

~~~text
Morning completes
  ↓
User later sees a short, truthful observation
  "Conversation gets your attention quickly.
   Movement prompts are what usually get you out of bed."
  ↓
Explain what Plus continues
  ↓
Primary: Start 7-day trial — €49.99/year after trial
Secondary: €7.99 monthly
Tertiary: Continue with Free
~~~

The observation must be backed by enough real evidence. Do not invent a personalized claim solely to improve conversion.

### Paywall message hierarchy

Lead with outcome:

> **Keep your wake experience personal.**

Then explain the recurring value in ordinary language:

- live conversation that responds to you;
- a wake style shaped around your preferences;
- increasingly personal mornings as WakeMyWay learns what helps;
- richer premium wake experiences as they become available.

Avoid leading with:

- GPT/model names;
- token limits;
- API terminology;
- generic "advanced AI";
- dozens of checkmarks that make Plus look like a settings bundle.

## Subscription lifecycle and user fairness

If a user is entitled to Plus when a Wake Session begins, ordinary entitlement changes must not interrupt that active wake.

If Plus expires, is cancelled, cannot be verified or enters a non-entitled state:

- the committed alarm still fires normally;
- Stop/Snooze remains unchanged;
- the session never becomes unsafe;
- future non-critical premium enrichment may fall back to Free according to the existing commerce boundary;
- the user is informed later in a normal awake-state surface.

WakeMyWay must not use fear such as "your alarm may not work unless you renew."

Cancellation should be easy through the official store-management surface.

## Realtime economics

### Current reference pricing

As of 2026-10-01, OpenAI publishes:

- `gpt-realtime-2.1-mini`: audio input **$10 / 1M tokens**, cached audio input **$0.30 / 1M**, audio output **$20 / 1M**;
- `gpt-live-1`: **$0.05 per active session minute**, with backend model/tool usage billed separately.

OpenAI also documents approximate conversational audio tokenization of:

- user audio: **1 token / 100 ms**;
- assistant audio: **1 token / 50 ms**.

These values are time-sensitive and must be re-checked before implementation and public pricing decisions.

### Illustrative efficient wake

A short useful Realtime interaction might contain roughly:

- 20 seconds of user speech;
- 45 seconds of assistant speech.

At the published `gpt-realtime-2.1-mini` audio rates, the raw audio portion is approximately:

~~~text
user:      20 s × 10 tokens/s = 200 tokens
           200 × $10 / 1,000,000 ≈ $0.002

assistant: 45 s × 20 tokens/s = 900 tokens
           900 × $20 / 1,000,000 ≈ $0.018

raw audio ≈ $0.020
~~~

Actual cost can be higher because later conversation turns resend conversation context and may include text/tool/backend usage. Therefore this example is not the budget assumption.

### Cost budget

The launch business model should target:

| Cost guardrail | Target |
|---|---:|
| Typical paid wake, measured all-in provider cost | **≤ €0.04** |
| High-but-normal paid wake | **≤ €0.08** |
| Average Plus provider + variable cloud cost / subscriber / month | **≤ €1.25** |
| Free-tier recurring inference cost | **near zero by design** |
| Blended contribution margin after store/tax assumption and variable cost | **≥ 65%**, target 70%+ |

Use actual provider usage and settlement data rather than theoretical token math once beta traffic exists.

### Minimum effective compute

The product principle "minimum effective friction" has a direct economic companion:

> **Use the minimum effective compute required to achieve the wake outcome.**

The assistant should not continue chatting after the user is clearly progressing.

A short successful wake is both better product design and better economics.

## Realtime cost controls

The exact provider implementation remains evidence-driven, but the commercial boundary requires measurable limits.

Initial operating targets:

- target ordinary Realtime engagement: roughly **30–90 seconds**;
- soft live-session budget: **120 seconds**;
- hard provider-session ceiling hypothesis: **180 seconds**;
- close the session immediately on completion, fallback or terminal action;
- bounded turn count;
- bounded reconnect/retry count;
- do not leave an idle paid session open;
- use VAD / equivalent empty-audio filtering correctly;
- keep prompts and stable instructions compact;
- reuse/cache stable context when supported and safe;
- summarize only the context required for the current wake;
- do not send full historical transcripts as default model context;
- preserve a provider cost kill switch;
- preserve immediate local alarm fallback.

If the live budget ends before the user is sufficiently activated, the local deterministic wake path continues. Cost control may end enrichment; it may not end the alarm.

Do not surprise a subscriber with a per-minute charge or a wake that becomes silent because a cloud budget was exhausted.

## Unit economics model

### Conservative Germany-style working assumptions

This is a planning model, not accounting advice and not a settlement forecast for every country.

Assume:

- retail prices include consumer VAT;
- example VAT: **19%**;
- Google Play subscription service/billing fee assumption: **15%**;
- plan mix: **70% annual / 30% monthly**;
- average variable Plus cost budget: **€1.25 / paid subscriber / month**;
- fixed company costs, founder compensation, marketing and income/corporate tax excluded.

Using those assumptions:

| Metric | Annual customer | Monthly customer |
|---|---:|---:|
| Retail price | €49.99/year | €7.99/month |
| Retail monthly equivalent | €4.17 | €7.99 |
| Approx. after 19% VAT + 15% store/billing fee | **€2.98/month** | **€5.71/month** |

With a 70/30 annual/monthly mix:

~~~text
blended retail monthly equivalent ≈ €5.31
blended developer net before variable service cost ≈ €3.80
target variable Plus cost ≈ €1.25
target monthly contribution / payer ≈ €2.55
contribution margin on developer net ≈ 67%
~~~

This deliberately uses a conservative variable-cost budget rather than assuming every wake costs only the illustrative $0.02 raw-audio amount.

### Contribution scenarios

At approximately €2.55 monthly contribution per paying subscriber:

| Paying subscribers | Approx. monthly contribution |
|---:|---:|
| 500 | €1,275 |
| 1,000 | €2,550 |
| 2,000 | €5,100 |
| 4,000 | €10,200 |
| 5,000 | €12,750 |
| 10,000 | €25,500 |

This is **contribution before fixed operating costs, paid marketing, founder compensation and business/income taxes**.

Approximate payer counts required for contribution targets:

| Monthly contribution target | Approx. payers |
|---:|---:|
| €1,000 | ~400 |
| €5,000 | ~2,000 |
| €10,000 | ~4,000 |
| €20,000 | ~7,850 |

The important implication is that WakeMyWay does not require mass-market scale before becoming economically meaningful.

## Free-user economics

The free base must remain cheap enough to support broad distribution.

Free should not require:

- recurring paid Realtime inference;
- expensive always-on cloud jobs;
- high-volume transcript storage;
- heavy server-side learning;
- mandatory cloud wake preparation.

If a future free feature creates material recurring cloud cost, treat it as a new business-model decision rather than silently accepting lower margins.

## Commercial funnel

Measure the funnel as distinct stages:

~~~text
Store view
  ↓
Install
  ↓
First alarm configured
  ↓
First intended wake
  ↓
Qualified Premium Preview wake
  ↓
3-preview value moment reached
  ↓
Plus paywall viewed
  ↓
Trial started
  ↓
Trial converted to paid
  ↓
First renewal
  ↓
Long-term retained payer
~~~

Do not optimize paywall conversion while ignoring whether users actually wake successfully and remain voluntarily engaged.

## Commercial metrics

### Core product precedes monetization

Always report commercial metrics alongside:

- alarm delivery success;
- Confirmed Wake Success;
- return-to-bed calibration;
- annoyance;
- perceived agency;
- D7/D14/D30 wake-product retention.

### Funnel metrics

Track:

- install → first alarm;
- first alarm → first real wake;
- first wake → first qualified Preview;
- Preview #1 → Preview #3 completion;
- Preview completion → paywall view;
- paywall view → trial start;
- trial start → paid;
- paid → first renewal;
- annual vs monthly mix;
- cancellation reason where voluntarily supplied;
- refund rate;
- involuntary billing loss;
- restore success.

### Economics metrics

Track:

- provider cost / qualified live wake;
- provider cost / Confirmed Wake Success;
- provider cost / Plus subscriber / month;
- cloud cost / free MAU;
- developer net revenue / payer;
- contribution / payer;
- contribution margin;
- LTV after enough cohorts exist;
- CAC only when paid acquisition begins;
- LTV:CAC and payback period before scaling acquisition.

### Most useful AI cost metric

The key cost metric should be:

> **Provider cost per Confirmed Wake Success**

A cheap conversation that fails to get the user moving is not economically efficient merely because it used few tokens.

## Initial validation targets

These are working decision bands, not launch claims.

External subscription benchmarks should be used as context, not copied blindly into WakeMyWay because the target cohort and daily wake behavior are unusually specific.

Initial commercial interpretation:

- **Google Play D35 download → paid below ~1%:** treat acquisition/positioning/funnel as unproven rather than normalizing weak conversion;
- **trial → paid around 30%+:** credible early signal; materially below ~20% should trigger package/value/onboarding investigation before price discounting;
- **annual share 60–80%:** healthy target range for the intended merchandising;
- **provider + variable cloud cost > €1.25/payer/month:** investigate session efficiency/provider architecture before scaling;
- **contribution margin <65%:** do not scale paid acquisition;
- **high refunds/cancellations citing novelty or unmet AI expectations:** treat as product/positioning failure, not merely churn optimization.

Do not make these numbers paid-launch gates until the first qualified cohort gives WakeMyWay-specific evidence.

## Why freemium despite lower generic conversion

Subscription-industry benchmarks show hard paywalls can convert more installs directly to paid than freemium.

WakeMyWay still has specific reasons to prefer Free + Plus at launch:

1. wake trust is unusually hard to earn before real overnight use;
2. a user cannot meaningfully evaluate the product from screenshots alone;
3. the local-first free product has low variable cost;
4. free users can create word-of-mouth distribution;
5. coercive monetization would directly conflict with a trust-sensitive alarm product;
6. the distinctive premium value is easier to understand after a few real mornings.

This is a deliberate trade: lower immediate conversion pressure in exchange for trust, distribution and a better-qualified Plus funnel.

If real data later shows that the free tier destroys willingness to pay without improving growth/retention, revisit the packaging deliberately rather than introducing dark patterns.

## Trial hypothesis

The initial store trial is **7 days** after the three-wake Preview.

Reasons:

- the user has already experienced the premium differentiator before trial;
- seven days is long enough to include multiple ordinary mornings for most users;
- it avoids a 3-day "decide immediately" pressure pattern;
- it is operationally simple;
- Alarmy currently uses a 7-day free trial, which indicates category familiarity with the pattern.

Longer trials may convert better in generic subscription benchmarks, so 7 days is not treated as universally optimal.

Test 14 days only after enough volume exists to distinguish improved conversion from delayed churn/revenue recognition.

## Annual-first merchandising

Annual should be the visual default because:

- it materially improves cash flow and commitment;
- it reduces monthly churn exposure;
- the effective €4.17/month price feels accessible relative to the €7.99 flexible plan;
- WakeMyWay's value compounds over repeated mornings and therefore fits a longer commitment better than a one-off utility purchase.

Do not hide the monthly option or use deceptive preselection.

## No weekly plan

Weekly subscriptions are excluded because they:

- encourage short-lived novelty purchasing;
- create churn pressure inconsistent with habit formation;
- make the product look like a high-pressure subscription utility;
- complicate merchandising without improving the core business model.

If a future specific market proves a weekly plan useful, require evidence before adding it.

## No lifetime plan

Realtime voice and other cloud capabilities create continuing variable cost.

A lifetime purchase therefore creates duration risk: the most loyal users can become the least economic.

A future one-time purchase could apply only to a clearly local/offline product tier with no significant ongoing service cost. It is not part of the Plus launch plan.

## No ads

Advertising is a poor fit because:

- the alarm moment requires trust and low cognitive load;
- ad economics are weak relative to a successful subscription;
- ad SDKs can worsen privacy, package size and reliability;
- the free tier already has low variable cost;
- ads would weaken premium brand perception.

WakeMyWay should prefer a clean free product that creates demand for Plus.

## Acquisition strategy

Do not buy growth before the product earns retention.

Initial distribution should prioritize:

- Google Play organic discovery / ASO;
- founder-led demonstrations of the actual wake experience;
- short-form content showing the product outcome rather than generic AI demos;
- communities where habitual snoozing/morning difficulty is already discussed;
- earned reviews after enough successful wakes;
- lightweight referral only if organic users naturally ask to share.

Paid acquisition should remain small experimentation until:

- paid retention is credible;
- contribution margin is ≥65%;
- approximate LTV exists;
- CAC payback can be measured;
- the wake outcome remains healthy in acquired cohorts.

## Pricing experimentation

Avoid noisy random price experiments in a tiny beta.

Preferred order:

1. show the €7.99 / €49.99 package to qualified beta users;
2. capture actual checkout/trial behavior;
3. capture qualitative price/value objections;
4. only then test a materially different annual price in a later cohort if evidence warrants it.

Candidate price test if needed:

~~~text
current hypothesis: €49.99/year
lower test:        €39.99/year
higher-value test: €59.99/year
~~~

Do not run all three simultaneously without enough sample size to learn anything.

## Plus feature-gating test

A feature belongs in Plus when at least one of these is true:

- it creates material recurring provider cost;
- it materially deepens personalization beyond the complete free product;
- it provides recurring premium value users explicitly recognize;
- it creates differentiated content/character value that is costly to produce/operate.

A feature should usually stay Free when:

- it is needed for alarm trust/safety;
- it is needed to fairly demonstrate the product promise;
- gating it would intentionally worsen the wake outcome;
- its marginal cost is near zero and it substantially improves distribution/retention;
- it is necessary for privacy, accessibility or basic control.

## Kill criteria and corrective actions

### If users like the idea but do not keep using it

Do not change price first.

Investigate:

- wake outcome;
- annoyance;
- reliability;
- morning friction;
- novelty decay;
- personalization quality.

### If users keep using Free but reject Plus

Investigate:

- whether Plus creates a noticeable outcome difference;
- whether Preview demonstrates that difference;
- whether live conversation is actually valuable;
- whether the package is too feature-led;
- whether the price is the objection or merely the easiest stated reason.

### If Plus converts but provider cost is too high

Prefer:

1. shorter sessions;
2. better termination when activation evidence is sufficient;
3. smaller/cheaper provider where quality remains acceptable;
4. context compression/caching;
5. provider architecture change.

Do not first respond by damaging Free or surprising users with metered wake charges.

### If Plus converts but churn is high

Investigate whether customers bought AI novelty rather than recurring wake value.

Retention is the primary defense against a superficially successful launch.

## Gate before enabling public paid rollout

Public Plus should not be enabled merely because Play Billing works.

Require evidence that:

- TRUST gate is acceptable;
- FINISH gate is acceptable;
- target users achieve meaningful wake outcomes;
- users voluntarily return;
- Preview demonstrates a premium difference;
- actual package presentation produces credible trial intent;
- provider quality is reliable enough for a paid promise;
- measured provider cost fits the budget;
- purchase/restore/cancel/renew/grace/hold/expiry paths are proven with Play license testing;
- privacy/support/store operations are ready;
- a provider cost kill switch exists;
- Free fallback remains complete and safe.

## Decisions vs hypotheses

### Decided

- Free + Plus;
- no ads;
- no weekly;
- no lifetime at launch;
- no ordinary wake credit economy;
- Free remains a real local-first wake product;
- Plus sells recurring personalized wake value, not "AI";
- first three qualified premium wakes are intended as no-payment Preview;
- commerce never enters the Active Wake authority path;
- annual-first merchandising;
- provider cost must be bounded and measured per wake/subscriber.

### Working hypotheses to validate

- €7.99 monthly;
- €49.99 annual;
- 7-day store trial;
- 70/30 annual/monthly payer mix;
- ≤€1.25 average monthly variable Plus cost;
- ≥65% contribution margin floor;
- the exact three-wake Preview qualification definition;
- Realtime as the strongest Plus conversion driver;
- the final set of Plus-only personalization/insight features.

## Research snapshot

Time-sensitive references checked for this plan on **2026-10-01**:

- OpenAI API pricing: https://developers.openai.com/api/docs/pricing
- OpenAI voice cost optimization: https://developers.openai.com/api/docs/guides/voice-latency-cost
- Google Play service fees: https://support.google.com/googleplay/android-developer/answer/112622
- RevenueCat State of Subscription Apps 2026: https://www.revenuecat.com/state-of-subscription-apps
- Alarmy German App Store listing: https://apps.apple.com/de/app/alarmy-wecker-und-schlaf/id1163786766
- Apple App Store business models / Small Business Program context for future iOS: https://developer.apple.com/app-store/business-models/

External pages are research inputs, not repository policy authority. Re-check pricing, store fees and platform requirements immediately before public launch.

## Implementation sequence

This PR documents product and commercial intent only.

Do not implement the entire model as one engineering PR.

Preferred sequence after Shape acceptance:

1. Gate 3 beta captures premium-value evidence;
2. define semantic Premium Preview eligibility/consumption;
3. configure Plus product/base plans/offers in Play Console;
4. update guarded purchase surface merchandising to annual-first + monthly;
5. add trial offer only after terms/copy are reviewed;
6. connect Plus entitlement to non-critical premium capability selection;
7. add cost telemetry and hard provider budgets;
8. run Play license-tester lifecycle matrix;
9. run closed paid test;
10. compare outcome, conversion, cost and retention;
11. adjust package/price only from evidence;
12. staged public rollout.

The safe billing/verification foundation already exists in `docs/40-play-billing-boundary.md`. Do not duplicate or bypass it.

## Relationship to launch gates

The canonical paid-launch program remains `docs/35-paid-launch-readiness.md`.

This document owns the commercial/package/economic Shape contract.

Gate 3 proves that users value the outcome and the Plus difference.

Gate 4 proves that the accepted package can be sold, restored, supported and operated safely.

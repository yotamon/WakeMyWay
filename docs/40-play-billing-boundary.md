# WakeMyWay Play Billing boundary

**Status:** Gate 4 foundation  
**Issue:** #115  
**Client PR:** #116  
**Server verification:** merged in #118  
**Last verified against official Play Billing docs:** 2026-09-24  
**Library target:** Google Play Billing Library 9.1.0

## Purpose

Prepare a correct purchase lifecycle without letting commerce become wake authority and without exposing a paywall before Gate 3 has earned the final package/price.

## Source-set boundary

~~~text
main/
  provider-neutral commerce language
          │
          ├── direct/
          │     explicit commerce unavailable
          │
          └── play/
                BillingClient 9.1.0
                HTTPS purchase verifier
                        │
                        ▼
          /api/v1/commerce/play-verify
                        │
                        ▼
          Google Play Developer API
~~~

The direct founder distribution intentionally does not ship Google Play Billing.

## Build-time launch controls

Three values are injected only into the Play flavor:

~~~text
WMW_PRO_SUBSCRIPTION_PRODUCT_ID
WMW_PLAY_VERIFICATION_ENABLED
WMW_COMMERCE_API_BASE_URL
~~~

They may be supplied as Gradle properties or environment variables.

Default behavior is intentionally inert:

- blank product id → commerce is UNCONFIGURED;
- verification disabled → purchase launch is blocked;
- blank/non-HTTPS commerce API URL → verifier is unavailable;
- no price or product id is hardcoded in consumer copy.

A build therefore cannot accidentally create a live purchase path merely because the Billing library exists.

## Verification rule

Client Play state is evidence of a purchase event, not final entitlement authority.

~~~text
PENDING
    ↓
no entitlement

PURCHASED / SUSPENDED
    ↓
WakeMyWay server verification
    ↓
Google Play Developer API
    ↓
normalized verified entitlement
    ↓
client acknowledgement when appropriate
~~~

Until verification succeeds:

- entitlement remains FREE/UNKNOWN;
- PURCHASED client state alone is never Pro;
- purchase tokens are not persisted by this boundary;
- purchase tokens redact themselves from normal object diagnostics;
- verification failure cannot change alarm state.

The public verification endpoint itself is disabled by default in server configuration and must be explicitly configured before paid rollout.

## Client purchase lifecycle

The Play adapter:

- keeps one BillingClient instance per gateway;
- enables automatic service reconnection;
- queries ProductDetails after setup;
- queries current subscription purchases on refresh;
- includes suspended subscriptions in the query;
- models PENDING separately;
- receives purchase updates through PurchasesUpdatedListener;
- forwards only product id + purchase token to the verifier;
- acknowledges only after a verified entitled result;
- reports categorical non-sensitive failure codes.

Prices and billing periods come from Play ProductDetails.

The adapter intentionally does not select a preferred monthly/annual/trial offer. Final offer presentation belongs to the package/paywall shaped from Gate 3 evidence.

## Server verification

The Wake API now contains a fail-closed Play verification route:

~~~text
POST /api/v1/commerce/play-verify
~~~

It:

- is disabled by default;
- accepts only allowlisted product ids;
- bounds request size;
- exchanges a service-account assertion for an Android Publisher access token;
- checks the purchase through the current subscriptions v2 endpoint;
- requires the verified line item to match the requested product;
- normalizes provider lifecycle into WakeMyWay entitlement language;
- returns no purchase token, order id or provider payload;
- treats provider/auth failure as temporary unavailability rather than entitlement.

Production enablement still requires:

- Google Play service account/API access;
- explicit environment configuration;
- allowed product ids;
- production endpoint/rate-limit controls;
- Play Console product/base-plan configuration.

## Entitlement projection

Provider-neutral states:

- FREE;
- ACTIVE;
- CANCELLED_ENTITLED;
- GRACE_PERIOD;
- ON_HOLD;
- PAUSED;
- EXPIRED;
- UNKNOWN.

Only:

- ACTIVE;
- CANCELLED_ENTITLED;
- GRACE_PERIOD

currently project to `hasPaidEntitlement=true`.

This projection affects only non-critical premium behavior.

## Alarm safety invariant

~~~text
commerce / account / network / entitlement
                   X
      Alarm Kernel / Active Wake authority
~~~

Billing failure must never:

- cancel an already-committed alarm;
- change its local sound;
- weaken Alarm Ready state;
- block or delay Stop;
- cause Snooze to succeed without a durable replacement;
- terminate Active Wake;
- become a dependency of AlarmReceiver or AlarmPlaybackService.

An expired or unverifiable subscription may affect future non-critical premium experiences only.

## Network boundary

The Play flavor declares INTERNET because server verification requires HTTPS.

That permission does not make alarm delivery network-dependent.

The direct flavor remains on its existing independent update/network boundary and does not ship Play Billing.

## Refresh policy

A normal consumer lifecycle owner should later call `refresh()` when the ordinary app enters the foreground.

Do not couple billing refresh to:

- AlarmReceiver;
- alarm foreground service startup;
- locked-screen wake presentation;
- Active Wake Stop/Snooze;
- Direct Boot reconciliation.

## Tests

Current automated contracts cover:

- direct flavor with no Play Billing implementation;
- Play build against Billing 9.1.0;
- Play-specific verifier configuration;
- purchase blocked unless verification is enabled/configured;
- PENDING grants no entitlement;
- PURCHASED client state alone grants no entitlement;
- entitlement-state projection;
- purchase-token diagnostic redaction;
- server verification allowlist/provider normalization/fail-closed behavior;
- server response redaction.

Before public purchase UI, add real Play license-tester evidence for:

- product unavailable/unconfigured;
- monthly/annual/trial offers selected by the final package;
- user cancellation;
- pending purchase;
- pending → purchased on later foreground;
- purchased with verifier temporarily unavailable;
- verification rejected;
- active;
- cancelled-but-entitled;
- grace period;
- on hold;
- paused;
- expired;
- suspended subscription query behavior;
- acknowledgement retry/failure;
- reinstall/restore;
- billing/backend outage while a wake is scheduled;
- billing/backend outage while Active Wake is already running.

The last two must demonstrate no alarm behavior change.

## Paywall boundary

There is intentionally still:

- no consumer purchase screen;
- no visible Pro badge;
- no final price in app copy;
- no Pro-only feature wall;
- no subscription prompt during wake/setup.

Gate 3 (#87) must first establish which package users value and what price is credible.

The implementation order is therefore:

~~~text
safe lifecycle foundation
        ↓
license-tester proof
        ↓
Gate 3 package evidence
        ↓
paywall/package implementation
        ↓
closed paid test
        ↓
staged rollout
~~~

## Official references checked

- https://developer.android.com/google/play/billing/integrate
- https://developer.android.com/google/play/billing/backend
- https://developer.android.com/google/play/billing/release-notes
- https://developer.android.com/google/play/billing/deprecation-faq.html

Re-check them immediately before Play submission.

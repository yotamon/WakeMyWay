# WakeMyWay Play Billing boundary

**Status:** repository implementation complete; external Play Console/license-tester proof still required  
**Client boundary:** merged in #116  
**Initial server verification:** merged in #118  
**Server acknowledgement + RTDN lifecycle:** merged in #120  
**Last verified against official Play Billing docs:** 2026-09-24  
**Library target:** Google Play Billing Library 9.1.0

## Purpose

WakeMyWay has a complete non-authoritative Play commerce foundation without exposing a public paywall or letting commerce become wake authority.

The remaining work is external configuration/evidence plus the final package/price decision from Gate 3.

## Source-set and server boundary

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
                        │
        ┌───────────────┴────────────────┐
        │                                │
 server acknowledgement          authenticated RTDN
        │                                │
        └───────────────┬────────────────┘
                        ▼
       normalized lifecycle ledger
       SHA-256 purchase-token digest only
~~~

The direct founder distribution intentionally does not ship Google Play Billing.

## Build-time launch controls

The Play flavor uses:

~~~text
WMW_PRO_SUBSCRIPTION_PRODUCT_ID
WMW_PLAY_VERIFICATION_ENABLED
WMW_COMMERCE_API_BASE_URL
~~~

The signed tag-release workflow reads these from repository variables.

Default behavior is inert:

- blank product id → commerce is UNCONFIGURED;
- verification disabled → purchase launch is blocked;
- blank/non-HTTPS commerce API URL → verifier is unavailable;
- no price is hardcoded into the app.

If verification is enabled for a signed Play build, release validation fails closed unless a product id and HTTPS commerce API URL are present.

## Purchase and entitlement rule

Client Play state is evidence of a purchase event, not final entitlement authority.

~~~text
PENDING
    ↓
no entitlement

PURCHASED / SUSPENDED
    ↓
WakeMyWay server verification
    ↓
Google Play subscriptionsv2.get
    ↓
normalized verified entitlement
    ↓
server acknowledgement when required
    ↓
client acknowledgement remains fallback
~~~

Until verification succeeds:

- entitlement remains FREE/UNKNOWN;
- PURCHASED client state alone is never Pro;
- purchase launch is disabled if the verifier is unavailable;
- verification failure cannot change alarm state.

## Client purchase lifecycle

The Play adapter:

- keeps one BillingClient instance per gateway;
- enables automatic service reconnection;
- queries ProductDetails after setup;
- queries current subscription purchases on refresh;
- includes suspended subscriptions;
- models PENDING separately from PURCHASED;
- receives purchase updates through PurchasesUpdatedListener;
- forwards only product id + purchase token to the verifier;
- redacts purchase tokens from normal diagnostics;
- acknowledges only after verified entitled state.

Prices and billing periods come from Play ProductDetails.

The adapter intentionally does not choose a preferred monthly/annual/trial offer. Final offer presentation belongs to the package/paywall shaped from Gate 3 evidence.

## Server verification and acknowledgement

The Wake API exposes:

~~~text
POST /api/v1/commerce/play-verify
~~~

It:

- is disabled by default;
- accepts only allowlisted product ids;
- bounds request size;
- obtains Android Publisher access with server-only service-account credentials;
- verifies through the current subscriptions v2 endpoint;
- requires a matching product line item;
- normalizes provider lifecycle into WakeMyWay entitlement language;
- acknowledges verified entitled purchases when acknowledgement is pending;
- returns no purchase token, order id or provider payload;
- treats provider/auth failures as temporary unavailability rather than access.

Client acknowledgement remains a safe fallback if server acknowledgement temporarily fails.

## Durable lifecycle and RTDN

The server stores only a privacy-minimized lifecycle ledger:

- SHA-256 purchase-token digest;
- product id;
- normalized entitlement state;
- acknowledgement state;
- optional SHA-256 linked-purchase-token digest;
- optional expiry;
- verification/update timestamps.

Raw purchase tokens are transient and are not stored in the lifecycle ledger.

Real-time Developer Notifications are handled through:

~~~text
POST /api/v1/commerce/play-rtdn
~~~

RTDN:

- is disabled by default;
- requires authenticated Google Pub/Sub OIDC push;
- validates the exact HTTPS audience and push service-account email;
- validates package identity;
- deduplicates by Pub/Sub message id;
- re-queries Google Play as source of truth rather than trusting notification type;
- leaves provider failures unprocessed so Pub/Sub can retry;
- treats pending-purchase cancellation as no-entitlement;
- keeps a bounded message-id dedupe ledger with no purchase/user content.

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

Only ACTIVE, CANCELLED_ENTITLED and GRACE_PERIOD currently project to paid access.

This projection affects only non-critical premium behavior.

## Alarm safety invariant

~~~text
commerce / account / network / entitlement
                   X
      Alarm Kernel / Active Wake authority
~~~

Billing or server failure must never:

- cancel an already-committed alarm;
- change its local sound;
- weaken Alarm Ready state;
- block or delay Stop;
- cause Snooze to succeed without a durable replacement;
- terminate Active Wake;
- become a dependency of AlarmReceiver or AlarmPlaybackService.

An expired or unverifiable subscription may affect future non-critical premium experiences only.

## Network boundary

The Play flavor declares INTERNET for purchase verification.

That permission does not make alarm delivery network-dependent.

The direct flavor remains on its independent update/network boundary and does not ship Play Billing.

## Automated evidence

Repository contracts cover:

- direct flavor with no Play Billing implementation;
- Play build against Billing 9.1.0;
- Play verifier fail-closed configuration;
- purchase blocked unless verification is enabled/configured;
- PENDING grants no entitlement;
- PURCHASED client state alone grants no entitlement;
- entitlement projection;
- token diagnostic redaction;
- server product allowlisting;
- provider lifecycle normalization;
- server acknowledgement success/failure;
- SHA-256 lifecycle persistence;
- linked-token hashing;
- RTDN configuration fail-closed;
- authenticated RTDN boundary;
- Pub/Sub message dedupe;
- provider failure retry behavior;
- pending cancellation;
- wrong-package rejection;
- response/log privacy.

## External proof before public purchase UI

Repository code is not sufficient to make purchases live.

Before enabling a consumer paywall:

- create the Play subscription product/base plans/offers;
- provision Google Play Developer API/service-account access;
- apply the lifecycle migration in production;
- configure production edge rate limiting;
- configure authenticated Pub/Sub RTDN push with exact audience;
- configure the signed Play build variables;
- run license-tester purchase, cancel, pending, restore, grace, hold, pause and expiry flows;
- verify reinstall/restore;
- verify billing/backend outage while an alarm is scheduled;
- verify billing/backend outage while Active Wake is already running.

The last two must demonstrate no alarm behavior change.

## Paywall boundary

There is intentionally still:

- no consumer purchase screen;
- no visible Pro badge;
- no final price in app copy;
- no invented Pro-only feature wall;
- no subscription prompt during wake/setup.

Gate 3 (#87) must first establish which package users value and what price is credible.

The implementation order is now:

~~~text
safe lifecycle foundation      ✅
server verification             ✅
server acknowledgement + RTDN   ✅
        ↓
external Play license proof
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

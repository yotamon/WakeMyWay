-- Google Play subscription lifecycle ledger.
--
-- Raw purchase tokens and order/payment data are deliberately not persisted. The SHA-256 token
-- digest is enough for idempotency/audit while live provider calls use the transient token supplied
-- by the verified Android client or authenticated RTDN notification.

create schema if not exists wmw_private;

create table if not exists wmw_private.play_subscription_purchases (
    purchase_token_sha256 text primary key
        check (purchase_token_sha256 ~ '^[0-9a-f]{64}$'),
    product_id text not null
        check (char_length(product_id) between 1 and 128),
    entitlement_state text not null
        check (entitlement_state in (
            'ACTIVE',
            'CANCELLED_ENTITLED',
            'GRACE_PERIOD',
            'ON_HOLD',
            'PAUSED',
            'EXPIRED',
            'UNKNOWN'
        )),
    acknowledged boolean not null,
    linked_purchase_token_sha256 text
        check (
            linked_purchase_token_sha256 is null or
            linked_purchase_token_sha256 ~ '^[0-9a-f]{64}$'
        ),
    expiry_at timestamptz,
    last_verified_at timestamptz not null,
    updated_at timestamptz not null default now()
);

alter table wmw_private.play_subscription_purchases enable row level security;

create index if not exists play_subscription_purchases_product_idx
    on wmw_private.play_subscription_purchases(product_id);

create index if not exists play_subscription_purchases_updated_idx
    on wmw_private.play_subscription_purchases(updated_at);

comment on table wmw_private.play_subscription_purchases is
    'Minimal server-side Google Play lifecycle ledger. Never alarm authority; never stores raw purchase tokens.';
comment on column wmw_private.play_subscription_purchases.purchase_token_sha256 is
    'SHA-256 digest of the Google Play purchase token for idempotency. Raw token is transient only.';

create table if not exists wmw_private.play_rtdn_messages (
    message_id text primary key
        check (char_length(message_id) between 1 and 256),
    processed_at timestamptz not null
);

create index if not exists play_rtdn_messages_processed_idx
    on wmw_private.play_rtdn_messages(processed_at);

alter table wmw_private.play_rtdn_messages enable row level security;

comment on table wmw_private.play_rtdn_messages is
    'Bounded RTDN message-id dedupe ledger. Contains no purchase token, order id, or user content.';

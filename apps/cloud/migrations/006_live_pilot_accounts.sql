-- Account-specific experimental GPT-Live approval, separate from signed device
-- credentials. No user is enabled by a fresh migration.
create table if not exists wmw_private.live_pilot_accounts (
    account_id uuid primary key references neon_auth."user"(id) on delete cascade,
    enabled boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table wmw_private.live_pilot_accounts enable row level security;

comment on table wmw_private.live_pilot_accounts is
    'Server-only GPT-Live opt-in ledger. A row is required for experimental voice access without the Vercel allowlist.';

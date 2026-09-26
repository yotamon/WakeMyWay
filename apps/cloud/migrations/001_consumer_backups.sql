-- WakeMyWay account backup storage.
--
-- This schema is private application storage. The Wake API owns all domain access. Android never
-- queries this table directly, and this data is never alarm or active-wake authority.

create schema if not exists wmw_private;

create table if not exists wmw_private.consumer_backups (
    account_id uuid primary key references neon_auth."user"(id) on delete cascade,
    schema_version smallint not null check (schema_version = 1),
    generated_at timestamptz not null,
    snapshot jsonb not null check (jsonb_typeof(snapshot) = 'object'),
    updated_at timestamptz not null default now()
);

alter table wmw_private.consumer_backups enable row level security;

comment on table wmw_private.consumer_backups is
    'Latest explicit WakeMyWay consumer-intent backup per authenticated account. Not alarm authority.';
comment on column wmw_private.consumer_backups.snapshot is
    'Versioned consumer intent only. Never Direct-Boot state, active wake state, terminal controls, or Tomorrow Contract private text.';

-- WakeMyWay account backup storage.
--
-- This schema is intentionally outside Supabase's exposed `public` API schema. The Wake API owns
-- all domain access. There are no client/RLS policies because Android never queries this table
-- directly.

create schema if not exists wmw_private;

create table if not exists wmw_private.consumer_backups (
    account_id uuid primary key references auth.users(id) on delete cascade,
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

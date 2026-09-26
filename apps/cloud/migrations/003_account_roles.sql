-- Server-owned WakeMyWay account authorization.
--
-- Authentication proves identity through Supabase Auth. Authorization is deliberately separate:
-- clients cannot write roles, email addresses are never treated as privileges, and missing rows
-- resolve to the least-privileged "user" role in the Wake API.

create schema if not exists wmw_private;

create table if not exists wmw_private.account_roles (
    account_id uuid primary key references auth.users(id) on delete cascade,
    role text not null
        check (role in ('user', 'admin')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table wmw_private.account_roles enable row level security;

comment on table wmw_private.account_roles is
    'Server-owned WakeMyWay authorization roles. No client policies; missing row means ordinary user.';
comment on column wmw_private.account_roles.role is
    'Authorization role enforced by the Wake API. Never inferred from email or user_metadata.';

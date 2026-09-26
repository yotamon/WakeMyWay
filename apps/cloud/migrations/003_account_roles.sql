-- Server-owned WakeMyWay account authorization.
--
-- Neon Managed Better Auth proves identity. WakeMyWay authorization is deliberately separate:
-- clients cannot write roles, provider profile fields are never privileges, and a missing row
-- resolves to the least-privileged "user" role in the Wake API.

create schema if not exists wmw_private;

create table if not exists wmw_private.account_roles (
    account_id uuid primary key references neon_auth."user"(id) on delete cascade,
    role text not null
        check (role in ('user', 'admin')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table wmw_private.account_roles enable row level security;

comment on table wmw_private.account_roles is
    'Server-owned WakeMyWay authorization roles. No client policies; missing row means ordinary user.';
comment on column wmw_private.account_roles.role is
    'Wake API authorization role. Never inferred from email, OAuth profile, or neon_auth.user.role.';

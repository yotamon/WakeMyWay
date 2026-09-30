-- Revocable authorization ledger for account-backed Realtime devices.
--
-- Android never receives database access. A signed device credential is accepted for billable
-- Realtime token minting only while the matching private server row is active and unexpired.
-- Re-provisioning the same account + installation rotates the credential id atomically.

create schema if not exists wmw_private;

create table if not exists wmw_private.account_realtime_devices (
    credential_id uuid primary key,
    account_id uuid not null references neon_auth."user"(id) on delete cascade,
    account_pseudonym text not null
        check (account_pseudonym ~ '^[a-f0-9]{40}$'),
    installation_id uuid not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    last_used_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (account_id, installation_id)
);

create index if not exists account_realtime_devices_active_expiry_idx
    on wmw_private.account_realtime_devices (expires_at)
    where revoked_at is null;

alter table wmw_private.account_realtime_devices enable row level security;

comment on table wmw_private.account_realtime_devices is
    'Server-only authorization ledger for revocable WakeMyWay Realtime device credentials.';
comment on column wmw_private.account_realtime_devices.credential_id is
    'Opaque credential id embedded in the signed Android bearer and rotated on re-provision.';

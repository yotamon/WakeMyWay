-- Short-lived, one-time browser-to-Android Neon Auth handoffs.
--
-- The real Neon session token is encrypted before storage. Handoff rows live for only a few
-- minutes and are atomically deleted when Android exchanges the PKCE-bound one-time code.

create schema if not exists wmw_private;

create table if not exists wmw_private.mobile_auth_handoffs (
    code_hash text primary key
        check (length(code_hash) = 64),
    pkce_challenge text not null
        check (pkce_challenge ~ '^[A-Za-z0-9_-]{43}$'),
    token_envelope text not null,
    expires_at timestamptz not null,
    created_at timestamptz not null default now()
);

alter table wmw_private.mobile_auth_handoffs enable row level security;

create index if not exists mobile_auth_handoffs_expires_at_idx
    on wmw_private.mobile_auth_handoffs (expires_at);

comment on table wmw_private.mobile_auth_handoffs is
    'One-time PKCE-bound mobile auth handoffs. Never alarm authority; server-only access.';

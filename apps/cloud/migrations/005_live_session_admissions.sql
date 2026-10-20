-- Durable daily GPT-Live admission control. Applies only to the experimental
-- GPT-Live path. Prevents repeated wake/startup retries from burning session costs.
-- This is not a live-session concurrency tracker; session duration/closing needs
-- to be enforced by the Android audio adapter and OpenAI project budget.
create table if not exists wmw_private.live_session_admissions (
    account_id uuid not null references neon_auth."user"(id) on delete cascade,
    utc_day date not null,
    sessions_started integer not null default 0 check (sessions_started >= 0),
    last_started_at timestamptz not null default now(),
    primary key (account_id, utc_day)
);
alter table wmw_private.live_session_admissions enable row level security;
comment on table wmw_private.live_session_admissions is
    'Server-only GPT-Live daily session admission counter. No transcripts, audio or SDP retained.';

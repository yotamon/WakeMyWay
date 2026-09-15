# WakeMyWay cloud

This is the optional, non-critical Vercel service for WakeMyWay.

It is intentionally separate from `apps/android`. The Android Alarm Kernel, Active Wake Execution, Stop/Snooze, Wake Runtime, and local Wake Learning remain fully functional without this service.

## Cloud responsibilities

The service currently has two deliberately separate boundaries:

```text
optional product account API              optional AI enrichment
          │                                         │
          ▼                                         ▼
   Supabase Auth JWT                          AI platform
          │                                         │
          ▼                                         ▼
   Wake API on Vercel                       Vercel AI SDK 7
          │                                         │
          ▼                                         ▼
       Kysely                               Vercel AI Gateway
          │
          ▼
 Supabase PostgreSQL
```

The account path stores only explicit consumer-intent backups. It is never an Android wake authority. The AI path enriches non-critical behavior and also remains outside Alarm Kernel and Wake Runtime authority.

`src/ai` is the single cloud AI boundary for model-backed enrichment:

```text
WakeMyWay cloud feature
          ↓
      AI platform
          ↓
 Vercel AI SDK 7
          ↓
 Vercel AI Gateway
    ├─ language models
    ├─ embeddings
    ├─ transcription
    ├─ speech
    └─ realtime token minting (M8 spike only)
```

Language model calls use named `fast` and `smart` policies rather than provider names. AI Gateway model fallback is configured centrally so product/domain code does not implement its own provider cascade.

## Account backup boundary

The first production account capability is explicit backup/restore, not continuous sync.

`PUT /api/v1/account/backup` stores one latest versioned consumer-intent snapshot for the authenticated Supabase account. `GET /api/v1/account/backup` returns that snapshot or `null` when no backup exists.

The snapshot may contain normal Profile/default preferences and rich `AlarmDefinition` intent. Its schema cannot represent:

- Direct-Boot Critical Wake state
- Android alarm registrations or next-occurrence authority
- active Wake Session / WakeRuntime state
- Stop/Snooze terminal state
- Tomorrow Contract / Prepared Wake Plan private text
- raw microphone audio or transcripts

Unknown fields are rejected rather than silently persisted. Android restore policy remains local and conservative: local alarm IDs win and remote-only alarms import disabled.

Account requests use a Supabase Auth user access token:

```text
Authorization: Bearer <supabase-user-access-token>
```

The Wake API verifies the JWT signature through the project's public JWKS and validates issuer, `authenticated` audience, expiry, and user subject. It does not use a Supabase service-role/secret API key for authentication.

Backup rows live in the private `wmw_private` PostgreSQL schema and are accessed only by the Wake API through Kysely. Android does not query Supabase database tables directly.

## Security and privacy boundary

All billable diagnostic endpoints under `/api/internal/*` require:

```text
Authorization: Bearer $WMW_INTERNAL_API_KEY
```

`WMW_INTERNAL_API_KEY` is an operator/development secret. **Never ship it in the Android APK.** It is separate from user account authentication.

Private text, structured generation and embeddings enforce request-level AI Gateway Zero Data Retention and prompt-training opt-out. They fail closed when no compliant provider route exists, and Gateway prompt caching is not enabled for private wake data.

The current default Gateway transcription, speech and realtime models do not satisfy WMW's ZDR requirement. Those paths are therefore disabled by default. `WMW_ENABLE_NON_ZDR_AUDIO_SPIKES=true` is permitted only for synthetic, non-sensitive engineering experiments. It must never be used with real wake audio, transcripts, Tomorrow Contract text, calendar content or generated private speech.

The service does not persist prompts, transcripts, microphone audio, or generated private speech. Error logging records only a request ID, operation name, and error type. Account backup request bodies and database error messages are not logged.

## Local setup

```bash
cd apps/cloud
cp .env.example .env.local
npm install
npm run check
```

For AI development, set either `AI_GATEWAY_API_KEY` or use the Vercel OIDC environment. Set a random `WMW_INTERNAL_API_KEY` with at least 32 characters before calling internal routes.

For the account API, configure:

```text
SUPABASE_URL=https://<project-ref>.supabase.co
DATABASE_URL=postgresql://<server-only-supabase-connection>
```

Then apply `migrations/001_consumer_backups.sql` to the WakeMyWay Supabase project before serving account backup traffic.

Keep `WMW_ENABLE_NON_ZDR_AUDIO_SPIKES=false` unless deliberately running a synthetic M8 engineering experiment.

## Vercel deployment

Create/configure the Vercel project with **Root Directory** `apps/cloud`. Node.js 22+ is required; the current Vercel Node runtime may be newer.

Runtime configuration depends on enabled capabilities:

- account API: `SUPABASE_URL` and server-only `DATABASE_URL`
- AI Gateway authentication: Vercel OIDC or `AI_GATEWAY_API_KEY`
- `WMW_INTERNAL_API_KEY` for internal diagnostics/spikes
- optional model-policy overrides from `.env.example`
- `WMW_ENABLE_NON_ZDR_AUDIO_SPIKES=false` in normal environments

Production use of private text/embedding context requires a Vercel plan/environment that supports the configured ZDR policy. If that capability is unavailable, private model calls are expected to fail closed rather than silently weaken privacy.

Do not make Android alarm readiness depend on this deployment.

## Endpoints

| Endpoint | Purpose | Auth | Privacy posture |
|---|---|---|---|
| `GET /api/health` | non-sensitive config/readiness | public | no private input |
| `GET /api/v1/account/backup` | fetch latest explicit consumer backup | Supabase user JWT | consumer intent only; no wake authority/private Tomorrow Contract text |
| `PUT /api/v1/account/backup` | replace latest explicit consumer backup | Supabase user JWT | strict bounded schema; consumer intent only |
| `POST /api/internal/ai/text` | text smoke/integration call | internal | ZDR required |
| `POST /api/internal/ai/stream` | streamed text smoke/integration call | internal | ZDR required |
| `POST /api/internal/ai/embed` | embedding smoke/integration call | internal | ZDR required |
| `POST /api/internal/ai/speech` | TTS, returns audio | internal | disabled by default; synthetic spike only |
| `POST /api/internal/ai/transcribe` | raw `audio/*` body to STT | internal | disabled by default; synthetic spike only |
| `POST /api/internal/ai/realtime-token` | short-lived M8 realtime credential | internal | disabled by default; synthetic spike only |

The account routes are the first Android-facing domain API. The `/api/internal/*` routes remain engineering integration surfaces and must not be called from production Android with operator secrets.

## Adding product AI

Product code should import the deep `src/ai/platform.ts` functions and pass constrained domain requests. It should not import provider SDKs directly.

Structured generation is available through `generateStructuredWithAI(...)` and must use a server-owned schema. Do not accept arbitrary client-provided schemas.

If a future capability needs a provider directly, record the measured reason. Realtime voice in particular remains subject to the M8 transport spike; this foundation does not select Vercel proxying over direct provider or RTC transport.

# Wake My Way cloud

This is the optional, non-critical Vercel service for Wake My Way.

It is intentionally separate from `apps/android`. The Android Alarm Kernel, Active Wake Execution, Stop/Snooze, Wake Runtime, and local Wake Learning remain fully functional without this service.

## Why it exists

`src/ai` is the single cloud AI boundary for model-backed enrichment:

```text
Wake My Way cloud feature
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

## Security and privacy boundary

All billable diagnostic endpoints under `/api/internal/*` require:

```text
Authorization: Bearer $WMW_INTERNAL_API_KEY
```

`WMW_INTERNAL_API_KEY` is an operator/development secret. **Never ship it in the Android APK.** Android-facing production endpoints must later use real installation/account/session authorization.

Private text, structured generation and embeddings enforce request-level AI Gateway Zero Data Retention and prompt-training opt-out. They fail closed when no compliant provider route exists, and Gateway prompt caching is not enabled for private wake data.

The current default Gateway transcription, speech and realtime models do not satisfy WMW's ZDR requirement. Those paths are therefore disabled by default. `WMW_ENABLE_NON_ZDR_AUDIO_SPIKES=true` is permitted only for synthetic, non-sensitive engineering experiments. It must never be used with real wake audio, transcripts, Tomorrow Contract text, calendar content or generated private speech.

The service does not persist prompts, transcripts, microphone audio, or generated private speech. Error logging records only a request ID, operation name, and error type.

## Local setup

```bash
cd apps/cloud
cp .env.example .env.local
npm install
npm run check
```

Set either `AI_GATEWAY_API_KEY` or use the Vercel OIDC environment. Set a random `WMW_INTERNAL_API_KEY` with at least 32 characters before calling internal routes.

Keep `WMW_ENABLE_NON_ZDR_AUDIO_SPIKES=false` unless deliberately running a synthetic M8 engineering experiment.

## Vercel deployment

Create/configure the Vercel project with **Root Directory** `apps/cloud`. Node.js 22+ is required; the current Vercel Node runtime may be newer.

Required runtime configuration:

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
| `POST /api/internal/ai/text` | text smoke/integration call | internal | ZDR required |
| `POST /api/internal/ai/stream` | streamed text smoke/integration call | internal | ZDR required |
| `POST /api/internal/ai/embed` | embedding smoke/integration call | internal | ZDR required |
| `POST /api/internal/ai/speech` | TTS, returns audio | internal | disabled by default; synthetic spike only |
| `POST /api/internal/ai/transcribe` | raw `audio/*` body to STT | internal | disabled by default; synthetic spike only |
| `POST /api/internal/ai/realtime-token` | short-lived M8 realtime credential | internal | disabled by default; synthetic spike only |

These routes are an engineering integration surface, not the eventual Android domain API.

## Adding product AI

Product code should import the deep `src/ai/platform.ts` functions and pass constrained domain requests. It should not import provider SDKs directly.

Structured generation is available through `generateStructuredWithAI(...)` and must use a server-owned schema. Do not accept arbitrary client-provided schemas.

If a future capability needs a provider directly, record the measured reason. Realtime voice in particular remains subject to the M8 transport spike; this foundation does not select Vercel proxying over direct provider or RTC transport.

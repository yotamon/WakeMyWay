# Vercel AI platform foundation

**Status:** implemented foundation, not production Android integration  
**ADR:** [`../adr/016-vercel-ai-platform.md`](../adr/016-vercel-ai-platform.md)

## Scope

This implementation establishes the optional cloud AI boundary requested before M7/M8 without changing wake authority or roadmap evidence gates.

```text
apps/android
  Alarm Kernel + Wake Runtime
          │
          │ future authenticated, non-critical domain request
          ▼
apps/cloud on Vercel
          │
          ▼
   src/ai/platform.ts
          │
          ▼
 Vercel AI SDK 7
          │
          ▼
 Vercel AI Gateway
```

There is currently **no Android call to this service**. That is intentional.

## Deep module contract

`src/ai/platform.ts` provides the model-facing operations:

```text
generateTextWithAI
streamTextWithAI
generateStructuredWithAI
embedTextWithAI
transcribeWithAI
generateSpeechWithAI
createRealtimeToken
```

Callers choose `fast` or `smart` for language work. Provider/model routing stays inside the module.

### Default language policy

```text
FAST
openai/gpt-5.4-mini
  → anthropic/claude-haiku-4.5
  → google/gemini-3.5-flash

SMART
anthropic/claude-sonnet-4.6
  → openai/gpt-5.4
  → google/gemini-3.5-flash
```

Defaults are configuration, not product identity. They can be changed through environment variables and must be revalidated against the current Gateway catalog before important releases.

### Default modality models

```text
embedding      openai/text-embedding-3-small
transcription  openai/whisper-1
speech         openai/tts-1
realtime       openai/gpt-realtime-2.1
```

Realtime is included only so M8 can benchmark it behind a safe credential boundary. It is not a production transport decision.

## Privacy routing

Private text, structured output and embeddings are routed with:

```text
zeroDataRetention = true
disallowPromptTraining = true
Gateway prompt caching = not enabled
```

If the requested model/fallback set has no provider route that satisfies ZDR, the request fails closed. This is acceptable because cloud AI is enrichment, never alarm authority.

Current Gateway status as of 2026-09-10:

| Capability | Default | WMW privacy posture |
|---|---|---|
| text / structured output | fast/smart policy | ZDR required |
| embeddings | `openai/text-embedding-3-small` | ZDR required |
| transcription | `openai/whisper-1` | no current ZDR; spike-only |
| speech | `openai/tts-1` | no current ZDR; spike-only |
| realtime | `openai/gpt-realtime-2.1` | no current ZDR; M8 spike-only |

`WMW_ENABLE_NON_ZDR_AUDIO_SPIKES` defaults to `false`. Enabling it is an explicit engineering action for **synthetic, non-sensitive input only**. Do not use it with real wake audio, user transcripts, Tomorrow Contract text, calendar content, or generated private speech.

This gate exists because short-lived credentials protect API keys, not data-retention policy. M8 must revalidate provider/Gateway privacy before any real-user voice path is approved.

## Failure behavior

- language calls use Gateway model fallbacks;
- private text/embedding calls fail closed when no ZDR route is available;
- non-ZDR audio/realtime calls fail locally unless the explicit spike gate is enabled;
- individual SDK calls have bounded retries and timeouts;
- the service returns sanitized errors with request IDs;
- unknown provider errors are not logged verbatim;
- cloud failure never changes Android alarm authority or local session state;
- callers must provide their own local fallback when this service becomes connected to a product feature.

## HTTP boundary

`GET /api/health` is the only public route in this slice. It exposes non-secret configuration/readiness, including whether the non-ZDR audio spike gate is enabled.

All `/api/internal/ai/*` routes require an operator bearer key. They exist for engineering validation and the M8 spike, not as a public model proxy and not as the future Android authentication design.

Input sizes are bounded:

- JSON requests: 32 KiB transport limit plus field limits;
- generated speech text: 4,000 characters;
- transcription body: 10 MiB and `audio/*` only.

## Privacy

The cloud module intentionally has no database dependency and no persistence path. It does not store raw audio, transcript text, prompts or generated speech.

Application telemetry must continue to prefer typed semantic inputs/outcomes instead of raw dialogue.

## Validation

`.github/workflows/cloud-ci.yml` installs the isolated cloud package on Node 22 and runs strict TypeScript checking plus unit tests on every relevant pull request.

Tests cover configuration/fallback/privacy invariants and the internal authorization boundary without making paid/live model calls.

A real Gateway smoke test belongs in a controlled environment after credentials/spend limits are configured; it should not become a mandatory paid CI step. Any private text smoke must verify ZDR routing metadata. Audio/realtime smoke input must remain synthetic until a privacy-compliant route exists.

## Deployment

Vercel project root:

```text
apps/cloud
```

Use Vercel OIDC where available or configure `AI_GATEWAY_API_KEY`. Configure `WMW_INTERNAL_API_KEY` only for operator routes.

Keep `WMW_ENABLE_NON_ZDR_AUDIO_SPIKES=false` in normal environments. If temporarily enabled for M8 engineering, use synthetic input only and turn it back off after the experiment.

Before production Android integration add a real session/installation authorization boundary. Never embed `WMW_INTERNAL_API_KEY` in Android.

## Next decisions

M7 remains local deterministic Wake Learning v0.

At M8, use this foundation as one measured candidate while still comparing the realtime transport shapes required by ADR-008. Select M9 transport from measurements, including verified privacy guarantees, not from the existence of this code.

# ADR-016: Vercel AI SDK + AI Gateway as the default cloud AI platform

**Status:** Accepted  
**Date:** 2026-09-10

## Context

Wake My Way will need several model modalities over time: constrained language generation, structured outputs, embeddings, speech-to-text, text-to-speech and a measured realtime-voice experiment.

The project already chose Vercel for optional non-critical cloud compute (ADR-012), while ADR-005 establishes that AI is never wake-state authority and ADR-008 keeps realtime voice transport spike-gated.

Without one cloud AI boundary, each capability would tend to grow provider-specific SDKs, credentials, retry logic, model names and observability conventions. That is unnecessary coupling for capabilities whose provider choice is expected to change.

## Decision

For optional cloud AI workloads, use **Vercel AI SDK 7** with **Vercel AI Gateway** as the default model access layer.

The first implementation lives in `apps/cloud/src/ai` and owns:

- named language-model policies (`fast`, `smart`);
- AI Gateway model fallbacks;
- structured generation with server-owned schemas;
- embeddings;
- transcription;
- speech generation;
- short-lived realtime credential minting for M8 experiments;
- bounded timeouts/retries and a privacy-safe HTTP/error boundary.

Product/domain code requests a capability or policy. It must not orchestrate a provider cascade itself.

### Privacy-class decision

Wake My Way treats provider-level retention as part of routing correctness, not as a documentation footnote.

- Private text, structured generation and embeddings require request-level **Zero Data Retention (ZDR)** through AI Gateway and opt out of prompt training. If no compliant provider route exists, the request fails rather than silently weakening privacy.
- Gateway prompt caching is not enabled for private wake data.
- The current default Gateway models used for transcription, speech and realtime do **not** provide ZDR as of this ADR date. Those capabilities are therefore disabled by default and may be enabled only for synthetic, non-sensitive engineering spikes.
- Real wake audio, transcripts, Tomorrow Contract text, calendar content and other private user context must not use a non-ZDR audio/realtime path.
- M8 must treat privacy support as a measured/verified transport requirement alongside latency, reconnect behavior and cost.

This boundary may be relaxed only by a future documented decision backed by an equivalent privacy guarantee, not merely because a provider is convenient.

## What this decision does not mean

This ADR does **not**:

- move alarm delivery, Active Wake Execution, Stop/Snooze or Wake Runtime to Vercel;
- make cloud/AI availability a Wake Ready prerequisite;
- make AI authoritative for phases, Activation Completion, Confirmed Wake Success or facts;
- select a final realtime transport;
- require Android to proxy realtime audio through Vercel;
- authorize shipping an operator API key in the Android app;
- move M7 Wake Learning v0 to the cloud.

ADR-008 remains active. M8 must still measure direct realtime provider, an RTC layer where justified, and a Vercel-mediated shape before M9 chooses a production transport.

## Provider exceptions

A feature may bypass AI Gateway only when a concrete measured requirement justifies it, for example:

- a specialized model absent from Gateway;
- latency/transport behavior that Gateway cannot meet;
- a local/on-device model with a meaningful privacy/reliability/cost advantage;
- a provider-native capability that cannot be expressed through the SDK boundary.

The exception should remain behind the same product-facing contract where practical and should be documented when durable.

## Security and privacy

- Provider/Gateway credentials are server-only.
- Internal smoke endpoints use an operator secret and are never an Android production auth mechanism.
- Realtime clients receive short-lived credentials only when the explicit non-ZDR spike gate is enabled.
- No raw microphone archive by default.
- Prompts, transcripts, private wake context and generated private speech are not written to generic logs/analytics.
- Request/error logging is metadata-only.
- ZDR/private routing is fail-closed; provider availability may reduce enrichment but can never reduce the local alarm's reliability.

## Reliability consequence

The degradation chain remains local-first:

```text
cloud/realtime enrichment
        ↓ unavailable or privacy-ineligible
prepared/local character speech
        ↓ unavailable
Wake Runtime + Alarm Kernel
        ↓
bundled emergency alarm
```

The optional AI platform can improve richness. It cannot become a condition for a valid wake attempt.

## Consequences

### Positive

- one SDK surface across model providers and modalities;
- centralized routing/fallback and privacy policy;
- easier model/cost/quality experiments;
- fewer provider credentials and SDKs in application code;
- a natural place for short-lived realtime credentials and spend controls;
- provider changes do not leak into Wake Runtime;
- private text cannot silently route to a provider lacking the required retention policy.

### Trade-offs

- AI Gateway and new audio/realtime APIs are another vendor dependency;
- current Gateway voice/audio routes cannot yet satisfy WMW's preferred ZDR boundary, so production cloud voice remains intentionally blocked;
- realtime/audio capabilities are still evolving and must be revalidated at M8;
- a framework-level abstraction cannot erase modality-specific transport and UX differences;
- the service adds a cloud deployment that must remain explicitly non-critical.

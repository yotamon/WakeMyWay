# M8 Realtime Voice Architecture Spike Harness

## Status

Implementation started in issue #28 after M7 Wake Learning v0 demonstrated a bounded and explainable policy adaptation from fixture Wake Outcomes.

This first M8 slice does **not** choose a realtime provider and does **not** connect realtime voice to a production wake. It creates the evidence contract that every candidate must satisfy before ADR-008 can move from a process decision to a measured architecture decision.

## Purpose

The provider decision must not be driven by SDK familiarity, demo quality, or a single happy-path latency number.

The harness normalizes measurements from candidate transport shapes into one metadata-only evidence model:

```text
isolated candidate run
       ↓
VoiceSpikeSample
       ↓
configuration-scoped validation
       ↓
latency / failure / route / cost summary
       ↓
coverage + privacy readiness
       ↓
comparable evidence matrix
       ↓
explicit human ADR decision later
```

There is deliberately no automatic winner, weighted provider score, or generated recommendation in the code.

## Candidate identities

The current transport candidates from ADR-008 are represented explicitly:

```text
direct-openai
livekit-rtc
vercel-gateway
```

A sample is also bound to a `configurationId` and model. Results from two different configurations are rejected if someone tries to summarize them as one evidence set.

That distinction matters because a provider SDK version, model, privacy route, credential topology, relay topology, or transport implementation can change independently of the provider name.

## What a sample may contain

`VoiceSpikeSample` intentionally contains only engineering metadata:

- candidate and configuration identity;
- network scenario;
- audio route;
- success/failure;
- failure stage;
- cold-connection latency;
- first-speech latency;
- barge-in latency when exercised;
- reconnect latency when exercised;
- estimated cost for the run when available.

It has no field for:

- microphone audio;
- generated audio;
- transcript;
- prompt;
- Tomorrow Contract;
- calendar/context payload;
- Wake Outcome/history;
- user identity.

The M8 measurement layer therefore cannot become a convenient accidental store for private voice content.

## Network scenarios

The normalized scenarios are:

```text
wifi
mobile
wifi-to-mobile
mobile-to-wifi
```

A failed transition is still valuable evidence, but it does not satisfy successful transition coverage.

## Audio routes

The normalized routes are:

```text
speaker
wired-headset
bluetooth
```

A failed Bluetooth attempt contributes to the failure breakdown but does not satisfy the Bluetooth-success requirement.

## Failure stages

Failures are classified without storing conversation content:

```text
connect
first-speech
barge-in
reconnect
network-transition
audio-route
unknown
```

Successful samples cannot carry a failure stage. Failed samples must carry one.

## Privacy eligibility

Each exact candidate configuration is classified as one of:

```text
private-data-eligible
synthetic-only
ineligible
unknown
```

A production architecture decision is not ready while the measured configuration is anything other than `private-data-eligible`.

A privacy evidence reference is also required by the default evidence floor. This is intentionally separate from latency success: a technically excellent non-private route cannot silently pass the decision gate.

The existing Vercel AI Gateway realtime-token path remains explicitly gated for synthetic/non-sensitive experiments in the current cloud foundation. M8 measurements through that route must therefore use `synthetic-only` until the exact measured configuration has an acceptable private-data posture.

## Operational footprint

The configuration captures objective topology facts instead of an arbitrary "complexity score":

- Android client library count;
- backend component count;
- credential-exchange hop count;
- whether a stateful relay is required;
- whether a managed RTC control plane is present.

ADR-008 can discuss the trade-off using these facts rather than hiding subjective complexity weights inside the harness.

## Deterministic statistics

Latency and cost distributions use dependency-free nearest-rank percentiles.

For every available metric the summary exposes:

```text
count
min
p50
p95
max
mean
```

The algorithm sorts measured values first, so input order cannot change the result.

Failures remain visible separately through success rate and failure-stage counts. Missing measurements are never converted to zero-latency successes.

## Default evidence floor

The initial smoke-level evidence floor requires one exact candidate configuration to have:

- at least 5 successful runs;
- successful Wi-Fi behavior;
- successful mobile behavior;
- at least one successful Wi-Fi/mobile transition in either direction;
- at least one successful Bluetooth run;
- at least 3 measured barge-in responses;
- at least 3 measured reconnects;
- at least 3 cost observations;
- `private-data-eligible` classification;
- a privacy evidence reference;
- an explicit operational footprint.

These numbers are **not** production SLAs, statistical confidence claims, or final benchmark sizes. They are a centralized engineering floor that prevents ADR-008 from being decided from one attractive demo. They live in `DEFAULT_VOICE_SPIKE_REQUIREMENTS` and can be tightened after the first real spike runs.

## Comparison readiness

A candidate can be individually complete without the architecture comparison being complete.

The default comparison requires at least two complete configurations. The evidence matrix reports exact missing-evidence codes such as:

```text
comparison:requires-2-configurations
direct-openai:direct-v1:bluetooth-success
livekit-rtc:livekit-v1:reconnect-measurements
```

The correct state is then `comparableForDecision = false`.

The harness intentionally has no `winner` or `score` field even when comparison becomes complete. Provider selection remains an explicit architecture decision in ADR-008 after reviewing latency, failure behavior, privacy, cost and operational footprint together.

## Reproducible manual spike protocol

For each exact candidate configuration:

1. Assign a stable `configurationId` before testing. Record model, privacy classification/evidence, and topology footprint.
2. Use only synthetic, non-sensitive phrases until the configuration is explicitly private-data-eligible.
3. Perform cold-start runs on Wi-Fi and mobile separately. Capture connection-ready and first-audible-response elapsed times from one monotonic clock domain where possible.
4. Exercise barge-in by interrupting generated speech at a repeatable point and capture the time until output is actually interrupted/responding.
5. Exercise reconnect by intentionally dropping/recreating the realtime session and measure until usable speech resumes.
6. Exercise at least one Wi-Fi ↔ mobile transition without changing the candidate configuration.
7. Exercise speaker and Bluetooth routes; record failed routing as failures rather than deleting the run.
8. Capture the actual provider/session cost if available; otherwise mark cost null rather than inventing a number.
9. Keep raw audio/transcripts out of the benchmark record. Temporary provider-side content handling must follow the candidate's declared privacy classification.
10. Summarize only samples that match the exact candidate + configuration identity. Do not merge model/provider/topology revisions into one distribution.
11. If readiness reports missing evidence, collect that evidence. Do not lower the requirement merely to obtain a provider decision.

## First candidate sequencing

ADR-008 currently prefers direct OpenAI realtime as the first prototype, not as the final architecture choice.

The intended order is:

```text
1. direct OpenAI isolated prototype + measurements
2. inspect what complexity/failures remain
3. add LiveKit candidate only if RTC/session management could materially improve them
4. measure the existing Vercel control-plane/gateway shape as a separate candidate where useful
5. evaluate ElevenLabs separately for character/voice quality, cost and licensing rather than treating it as the transport answer
6. amend ADR-008 only after comparable evidence exists
```

The order keeps the experiment economical while preserving the requirement that the final decision be evidence-driven.

## Relationship to wake authority

M8 is presentation enrichment only.

```text
Alarm Kernel ─────────────── critical wake authority
Wake Runtime ─────────────── behavioral authority
Wake Learning ────────────── future bounded policy derivation
Realtime voice candidate ─── optional speech/conversation transport
```

A realtime transport may eventually render or enrich speech intents and derive semantic observations through a controlled adapter. It may not own alarm delivery, Stop/Snooze durability, Activation Evidence, policy mutation, or the terminal Wake Session outcome.

Network/provider failure must therefore be recoverable by falling back to the already-functional local wake path.

## Current completion boundary

This first slice is complete when:

- the provider-neutral model compiles under strict TypeScript;
- deterministic tests cover distributions, failures, coverage, identity isolation and comparison readiness;
- the reproducible protocol is documented;
- CI is green.

It does **not** claim that any provider has been measured yet.

The next slice is an isolated first candidate prototype and real measurement capture. Bluetooth/network-transition evidence ultimately requires representative physical Android devices and cannot be proven by TypeScript tests or a desktop-only transport spike.

# M8 Realtime Voice Architecture Spike Harness

## Status

Implementation is active in issue #28. The provider-neutral evidence harness is merged, the first direct OpenAI WebRTC candidate is runnable in a debug-only Android lab, and the first operator-observed audible smoke-measurement seam is being added.

M8 still does **not** choose a realtime provider and does **not** connect realtime voice to a production wake. The evidence contract must be satisfied before ADR-008 can move from a process decision to a measured architecture decision.

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

## Metric clock semantics

Candidate implementations may expose many convenient protocol timestamps. Only timestamps that map to the defined user-visible metric may populate the canonical timing fields.

### Cold connection

`coldConnectionMs` measures from the local start of a fresh candidate connection attempt until the local candidate transport is ready for the controlled speech experiment.

For `direct-openai:webrtc-ephemeral-v1`, the current ready point is the `oai-events` WebRTC data channel reaching `OPEN` after credential minting and SDP exchange.

### First speech

`firstSpeechMs` measures from an **explicit local response request origin** to the **first physically audible response observation**.

It must not be populated from a server/control event such as an audio-delta event merely because that event is correlated with generated audio. A network event proves protocol progress, not that sound reached the listener.

The first direct-OpenAI smoke method is:

```text
protocolId         m8-direct-openai-manual-audible-v1
origin             local enqueue of fixed synthetic response.create
audio observation  operator tap at first audible syllable
quality             conservative upper bound; includes human reaction time
```

This manual method is suitable for smoke characterization and for discovering gross latency problems. It is **not automatically architecture-decision-grade evidence**. Before mixing such values into a final provider comparison, the same declared method must be used across candidates or replaced/supplemented by a more precise non-content audible-output signal.

Missing/uncertain audible observations remain `null`; they are never replaced with protocol-event latency.

### Barge-in

`bargeInMs` begins at a repeatable local interruption action and ends when ongoing output is actually observed to stop/respond. A provider acknowledgement alone is not equivalent to audible interruption.

### Reconnect

`reconnectMs` begins when the controlled reconnect attempt starts and ends when the candidate is again usable for speech under the same declared measurement protocol.

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

The current direct OpenAI candidate is also `synthetic-only` until the exact OpenAI organization/project data-control configuration used by WMW is independently verified.

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

Successful samples also require both cold-connection and first-speech measurements.

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
3. Perform cold-start runs on Wi-Fi and mobile separately. Capture the connection-ready time from the declared local connection origin.
4. For first speech, use a repeatable explicit local response request. Observe first physically audible output under the declared measurement method. Do **not** substitute an audio-delta/control event for audible output.
5. Record the measurement method/protocol alongside the experimental run notes. The current direct candidate's manual operator tap is an upper bound and must not be silently mixed with a more precise method as though they were identical.
6. Exercise barge-in by interrupting generated speech at a repeatable point and capture the time until output is actually interrupted/responding.
7. Exercise reconnect by intentionally dropping/recreating the realtime session and measure until usable speech resumes.
8. Exercise at least one Wi-Fi ↔ mobile transition without changing the candidate configuration.
9. Exercise speaker and Bluetooth routes; record failed routing as failures rather than deleting the run.
10. Capture the actual provider/session cost if available; otherwise mark cost null rather than inventing a number.
11. Keep raw audio/transcripts out of the benchmark record. Temporary provider-side content handling must follow the candidate's declared privacy classification.
12. Summarize only samples that match the exact candidate + configuration identity. Do not merge model/provider/topology revisions into one distribution.
13. If readiness reports missing evidence, collect that evidence. Do not lower the requirement merely to obtain a provider decision.

## First candidate sequencing

ADR-008 uses direct OpenAI realtime as the first prototype, not as the final architecture choice.

Current sequence:

```text
1. provider-neutral harness                         merged PR #29
2. direct OpenAI isolated WebRTC prototype          merged PR #31
3. direct candidate measurement seams + runs        in progress
4. inspect what complexity/failures remain
5. add LiveKit candidate only if RTC/session management could materially improve them
6. measure the Vercel control-plane/gateway shape separately where useful
7. evaluate ElevenLabs separately for character/voice quality, cost and licensing
8. amend ADR-008 only after comparable evidence exists
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

Completed:

- provider-neutral metadata-only model and validation;
- deterministic distributions/failure/coverage/readiness tests;
- reproducible comparison protocol;
- direct OpenAI WebRTC debug candidate with server-minted ephemeral credentials;
- debug-only Android RTC/permission isolation;
- first in-memory operator-observed first-audible smoke seam;
- lifecycle generation guard preventing stale credential/SDP callbacks from reviving a disconnected attempt.

Still open before M8 architecture decision:

- representative physical Android measurements;
- enough repeated first-speech samples under a comparable declared method;
- barge-in/reconnect/network-transition/Bluetooth evidence;
- cost observations;
- exact privacy/data-control proof;
- operational-footprint completion;
- at least one meaningful second comparison configuration;
- explicit ADR-008 decision after evidence review.

Bluetooth/network-transition evidence ultimately requires representative physical Android devices and cannot be proven by TypeScript tests or emulator-only transport validation.

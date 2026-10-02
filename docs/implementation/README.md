# Implementation notes

Implementation notes capture concrete milestone wiring and validation details. Canonical terminology lives in [`../../CONTEXT.md`](../../CONTEXT.md), while durable architectural choices belong in [`../adr/`](../adr/).

Current high-value notes include:

- [`permission-gated-wake-safety.md`](permission-gated-wake-safety.md) — strict pre-scheduling permissions and unsafe active-service shutdown after physical regression;
- [`m8-local-production-voice-wake.md`](m8-local-production-voice-wake.md) — production local Alfred TTS + on-device spoken replies + motion integration;
- [`product-design-foundation.md`](product-design-foundation.md) — Adaptive Dawn production design foundation;
- [`morning-instrument-redesign.md`](morning-instrument-redesign.md) — 2026-10-01 consumer redesign: Today / Alarms / You, Wake Horizon, and immersive active wake;
- [`reason-first-calm-companion.md`](reason-first-calm-companion.md) — 2026-10-02 convergence: when → why → readiness hierarchy, purpose-led alarm setup, and oriented wake context;
- [`d2-visual-regression.md`](d2-visual-regression.md) — curated visual-regression gate;
- [`d3-product-setup.md`](d3-product-setup.md) — production wake setup / Tomorrow Contract experience.

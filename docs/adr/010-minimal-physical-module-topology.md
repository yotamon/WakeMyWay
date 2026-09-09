# ADR-010: Start with minimal physical module topology

**Status:** Accepted
**Date:** 2026-09-09

## Context

Before code existed, the planned Android project had separate core/data/platform/feature/testkit modules and Hilt wiring. These boundaries were speculative: no compile graph, ownership, change-rate, build-time, or reuse evidence existed yet.

## Decision

M0 starts with only:

```text
:app
:wake-core
:benchmark
```

- `:wake-core`: pure Kotlin domain behavior
- `:app`: Android composition root, UI, persistence/platform implementations
- `:benchmark`: wake/startup performance tooling

Use packages to maintain locality inside `:app`. Add Gradle modules only after a concrete boundary earns physical isolation.

Start with manual constructor composition. Hilt/navigation frameworks are optional later decisions, not M0 requirements.

## Consequences

- lower build/configuration overhead
- fewer speculative contracts
- easier architecture refactoring during discovery
- requires discipline to keep `:app` packages cohesive until real module boundaries emerge

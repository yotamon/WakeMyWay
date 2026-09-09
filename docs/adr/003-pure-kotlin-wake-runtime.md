# ADR-003: Pure Kotlin deterministic Wake Runtime

**Status:** Accepted
**Date:** 2026-09-09
**Amended:** 2026-09-09 architecture review

## Context

Wake behavior must be predictable, replayable, testable, and independent of AI/provider variability.

An initial design split state, confidence estimation, strategy policy, and effects into several prospective abstractions. Before implementation, the architecture review found that those seams represented one cohesive decision: **what should Wake do next given observed facts and the current policy?**

## Decision

Implement one deep pure-Kotlin Wake Runtime.

Canonical phases:

```text
ALERTING → ENGAGING → ACTIVATING → ORIENTING → FINISHED
```

Its contract is expressed through typed Wake Inputs, Wake Directives, and a versioned Wake Policy.

The runtime owns internally:

- activation-evidence interpretation
- intervention/escalation decisions
- in-session snooze policy behavior
- phase transitions

Do not expose a public confidence-estimator/strategy-service forest unless future evidence shows an independent stable contract.

Do not use a general state-machine library initially.

## Consequences

- callers see a small behavioral contract
- product behavior remains deterministic/replayable
- internal heuristics can change without broad test/API churn
- future iOS can reproduce/share tested behavior without Android framework coupling

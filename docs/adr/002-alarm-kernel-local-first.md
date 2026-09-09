# ADR-002: Alarm Kernel isolation and local-first authority

**Status:** Accepted
**Date:** 2026-09-09
**Amended:** 2026-09-09 architecture review

## Context

A cloud/AI dependency on alarm delivery creates unacceptable failure modes. Android also has lifecycle states where normal credential-protected data may not yet be available after reboot.

## Decision

The Android Alarm Kernel is the local authority for the next Wake Occurrence.

It is a **deep module** whose caller-facing contract hides:

- durable normal wake state
- minimal Critical Wake Snapshot write/integrity
- exact OS alarm registration/cancellation
- snooze replacement
- readiness/reconciliation
- critical local wake start/fallback coordination

The caller must not orchestrate those steps itself.

Critical rules:

- use native user-facing exact-alarm primitives
- backend never triggers Android wake delivery
- critical schedule/recovery data is local
- bundled emergency alarm audio exists in the app
- prepared/private richness is optional
- minimal non-sensitive snapshot may live in device-protected storage for Direct Boot
- sensitive contextual content stays credential-protected

## Consequences

AI/network/normal-data outages reduce richness but must not turn a valid scheduled Wake Occurrence into silence inside the documented Android reliability envelope.

The Alarm Kernel implementation may be internally complex; that complexity is intentional and hidden behind a small contract.

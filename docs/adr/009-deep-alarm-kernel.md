# ADR-009: Alarm Kernel is a deep module

**Status:** Accepted
**Date:** 2026-09-09

## Context

The first architecture draft exposed prospective pieces such as alarm repository/store/scheduler/reconciler as separate caller-visible responsibilities. That would force UI/application code to know the critical ordering and partial-failure semantics of the most trust-sensitive system in the product.

## Decision

Expose the smallest useful Alarm Kernel contract and keep scheduling transaction/recovery complexity inside it.

A successful schedule/occurrence commit means the kernel has completed all required durable steps for its readiness contract. The caller must not separately persist, snapshot, register, and reconcile.

Thin Android receivers/activities are allowed at OS edges, but they delegate into the deep kernel rather than becoming additional policy owners.

## Consequences

- stronger locality of alarm reliability bugs
- fewer invalid partial states visible to callers
- easier contract/integration testing
- implementation may contain several private collaborators without turning each into a public abstraction

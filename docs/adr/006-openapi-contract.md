# ADR-006: OpenAPI as cross-platform service contract

**Status:** Accepted
**Date:** 2026-09-09
**Clarified:** 2026-09-09 architecture review

## Context

Android uses Kotlin and future iOS is expected to use Swift. If/when a TypeScript backend is introduced, sharing TypeScript-specific schemas directly with mobile clients would couple clients to server implementation language.

## Decision

When the first backend API boundary exists, use OpenAPI 3.1 as its language-neutral contract.

Do not scaffold an empty backend/OpenAPI project in M0 merely to satisfy this ADR. The contract begins with the first actual endpoint surface.

Typed client generation may be evaluated against a thin handwritten client after the API exists.

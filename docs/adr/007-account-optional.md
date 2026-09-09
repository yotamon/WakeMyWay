# ADR-007: No account required for first alarm

**Status:** Accepted
**Date:** 2026-09-09

## Context

Authentication before the first wake adds friction to a product whose core value is local and should be immediately testable.

## Decision

Local alarm/setup/wake use requires no account.

If a future cloud capability needs identity before accounts exist, introduce an anonymous installation identity only at that point. Accounts may later support sync, subscription identity, backup, or iOS migration.

## Consequence

Authentication/backend identity cannot become a dependency of Alarm Kernel readiness or Wake Runtime.

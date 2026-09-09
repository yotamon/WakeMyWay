# ADR-004: No KMP until iOS is justified

**Status:** Accepted
**Date:** 2026-09-09

## Context

Future iOS is likely, but current product learning must happen on Android. Premature KMP creates architecture/tooling complexity without knowing how much code will benefit.

## Decision

Do not introduce Kotlin Multiplatform now.

When iOS becomes real, measure the size/value of pure Kotlin domain code and choose between KMP and a Swift port.

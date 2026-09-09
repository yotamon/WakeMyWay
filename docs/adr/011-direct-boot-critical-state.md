# ADR-011: Direct Boot uses minimal non-sensitive critical state

**Status:** Accepted
**Date:** 2026-09-09

## Context

After device reboot, Android can fire scheduled/recovery work before the user has performed the first unlock. Credential-protected app data is unavailable in that state, but a wake alarm may still be expected.

Storing rich personalized context in device-protected storage would create unnecessary privacy exposure.

## Decision

Store only a versioned **Critical Wake Snapshot** in device-protected storage, containing the minimum non-sensitive information required to recover/register/start a safe Wake Occurrence.

Do not place in device-protected state:

- Tomorrow Contract text
- calendar data
- transcript/audio archives
- tokens
- private prompt/model context
- sensitive personalized generated speech

A Direct-Boot wake before first unlock uses generic bundled/branded local audio and basic controls. After unlock, richer credential-protected state reconciles.

## Consequences

- reboot-before-unlock wake reliability improves
- private morning context remains protected
- Direct Boot may be less personalized by design
- critical snapshot versioning/integrity requires dedicated tests

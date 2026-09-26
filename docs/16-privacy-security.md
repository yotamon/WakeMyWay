# Privacy and security

## Why privacy matters

Wake My Way operates in an intimate context. It may know wake routines, Tomorrow Contract intentions, calendar facts, morning voice interaction, and behavioral activation patterns.

The default architecture therefore minimizes collection rather than retaining data "in case it helps AI later".

## Storage classes

### Device-protected critical state

Only the minimal **Critical Wake Snapshot** required to recover/register/start a safe alarm before first unlock may live in Android device-protected storage.

Allowed examples:

- occurrence identity/version
- scheduled instant / safe schedule metadata
- safe alarm tone/fallback reference
- integrity/checksum information

Forbidden there:

- Tomorrow Contract text
- calendar event details
- transcripts/audio
- sensitive generated personalized speech
- tokens/credentials
- private model context

During Direct Boot, Wake My Way accepts lower personalization and uses safe generic local alarm content rather than exposing private context before unlock.

### Credential-protected local state

Normal Room/preferences/private files use credential-protected storage for:

- Wake Schedule/history
- Tomorrow Contract
- Prepared Wake Plan/private generated content
- derived behavioral history/profile
- provider/auth credentials (with Android Keystore as appropriate)

### Android OS backup and device transfer

WakeMyWay does not treat Android Auto Backup or device-to-device transfer as a product migration mechanism.

For 1.0, both Android 12+ `dataExtractionRules` and Android 11-and-lower `fullBackupContent` rules explicitly exclude credential-protected and device-protected app roots, files, databases, shared preferences, plus external app data from platform backup/transfer. Private Tomorrow Contract / Prepared Wake content and the Direct-Boot Critical Wake Snapshot additionally remain in `noBackupFilesDir`.

This avoids restoring rich alarm/preferences/history state onto a new installation without the local critical scheduling authority, permissions, capability checks and timezone/device reconciliation that make an enabled alarm trustworthy.

A future WakeMyWay-managed backup/migration flow may move selected state only through an explicit schema and conservative import semantics. Platform restore must not silently become that boundary.

## Raw microphone audio

Do **not** store raw morning audio by default. Realtime audio may transit a provider when the user enables live conversation, subject to provider/privacy settings, but WMW does not build an archive of recordings.

## Transcripts

Do not persist complete cloud transcripts by default. Prefer privacy-safe semantic facts such as:

```text
USER_RESPONDED
SNOOZE_CONFIRMED
FIRST_MOVE_SELECTED
```

If transient text is required for a realtime session, keep retention scoped to the technical need.

## Tomorrow Contract

Keep raw text local whenever possible.

If an optional cloud preparation feature requires it:

- disclose that fact
- send only required content
- configure provider retention conservatively where possible
- prefer returning a minimized structured/local plan rather than preserving raw text server-side

## Calendar

Read only a narrow time window and minimal fields. Never send calendar content to analytics/crash tooling.

## Motion

Do not persist continuous raw accelerometer/gyroscope traces. Derive local semantic movement facts/features.

## Analytics/crash redaction

Never send by default:

- raw Tomorrow Contract
- raw/full transcript or audio
- calendar titles/descriptions/locations
- complete prompts/model context
- high-frequency motion samples
- secrets/tokens

## Authentication

No account is required for first use.

A backend may later use an anonymous installation identity and rotating credentials only when a concrete cloud feature exists. Accounts arrive for sync/subscription/backup/iOS migration, not as an onboarding gate.

Neon Managed Better Auth is the account/auth provider when those capabilities are enabled. It is not required for base alarm use. A future mobile client may obtain an auth session directly from Neon Managed Better Auth, but application/domain data operations continue through the Wake API rather than binding the client directly to database tables.

## Secrets

No provider secret key is embedded in the APK. Use backend-issued short-lived credentials for client realtime access where required.

Use Android Keystore/platform APIs for local secret material.

## Android component security

Before beta verify:

- exported components minimized
- alarm/dismiss/snooze intents cannot be trivially forged/replayed
- `PendingIntent` mutability is minimal and explicit
- deep links are validated
- Android OS backup/D2D rules explicitly exclude app-managed local state
- logs contain no private speech/context
- device-protected snapshot integrity/versioning handles corrupt/stale state safely

## Network security

- TLS only
- explicit timeouts
- no critical alarm dependency on network success
- request/response logs redacted
- certificate pinning only if a concrete threat model justifies operational cost

## Retention

Define before beta:

- critical snapshot: only while needed for active next occurrence/recovery
- Tomorrow Contract: through intended occurrence plus a short recovery window unless user chooses history
- local session/outcome history: enough for product learning with user control/reset
- cloud data in Neon: only for actual enabled cloud features

Provide clear local reset and later account export/delete flows.

## GDPR / processor readiness

Before closed/public beta document:

- privacy notice and lawful basis/consent where applicable
- processor/provider list
- cloud export/delete
- analytics controls where required
- retention policies

## Character safety

Characters must not weaponize sensitive context. A user-created reminder can be repeated neutrally; the model must not invent manipulative social/emotional consequences.

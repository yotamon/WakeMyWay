# AI, voice and character system

## Principle

Conversation should feel alive while wake behavior remains controlled.

> **AI expresses the wake strategy. It never owns the wake strategy.**

Canonical Wake Runtime terms live in [`../CONTEXT.md`](../CONTEXT.md).

## Product sequencing

Realtime voice is intentionally **not** the first adaptive intelligence milestone.

WMW first proves deterministic local Wake Runtime, motion evidence, character expression, Tomorrow Contract and Wake Learning v0. Realtime voice then enriches an already-valid adaptive product rather than becoming the behavior engine.

Current sequence:

```text
M3 Wake Runtime
   ↓
M4 motion evidence
   ↓
M5 Alfred local character
   ↓
M6 Tomorrow Contract
   ↓
M7 Wake Learning v0
   ↓
M8 measured voice architecture spike
   ↓
M9 realtime conversation
```

## Stable authority boundary

```text
WakeRuntime
   │
   └─ WakeDirective.Speak(SpeechIntent)
                    │
                    ▼
          character renderer
          wording only
                    │
                    ▼
           speech implementation
                    │
             local/live audio
```

The character/speech layer may vary wording, voice, prosody and bounded humor. It cannot:

- transition Wake Phase;
- add Activation Evidence;
- declare the user awake;
- accept/complete snooze;
- stop Active Wake Execution;
- change Wake Policy;
- invent private/contextual facts.

Critical alarm playback lifetime belongs to the Alarm Kernel and ADR-014, never to character speech, realtime voice or `WakeActivity`.

## Current runtime SpeechIntent contract

The current implemented `SpeechIntent` set is canonical:

```text
InitialWake
AskToSitUp
AskToMove
ReEngage(escalationLevel)
SnoozeConfirmation
SnoozeFailed
Orientation
```

New character variants must cover every current intent. When the runtime adds a new intent, character coverage tests should fail until wording is deliberately supplied.

Semantic constraints matter as much as tone:

- `SnoozeConfirmation` is emitted before replacement scheduling is confirmed, so speech must ask/confirm rather than claim success.
- `Orientation` follows sufficient typed Activation Evidence, but it must not claim biological wakefulness or unsupported posture.
- `ReEngage` may become firmer with escalation but cannot gain behavioral authority.

## M5 concrete local character architecture

M5 implements Alfred without network or generative AI:

```text
SpeechIntent + WakeLineKey
          ↓
     AlfredCharacter
 deterministic curated renderer
          ↓
    RenderedWakeLine
          ↓
 OfflineVoiceSelector
          ↓
 LocalCharacterSpeaker
 Android TextToSpeech
          ↓
verified offline voice
```

See [`implementation/m5-alfred-local.md`](implementation/m5-alfred-local.md).

### Alfred v1

Alfred is dry, elegant, composed and direct.

Canonical presentation specification:

```text
id              alfred
version         1
voice locale    en-GB
speech rate     0.92
pitch           0.94
```

Version is part of replay identity. Copy changes that would make historical deterministic rendering misleading require a character version bump.

Curated copy is deliberately short for sleep inertia and guarded by tests for length, deterministic replay, bounded variation, semantic truthfulness and prohibited shaming/threat language.

### Sam

Future character direction: warm, steady, persistent.

### Chaos

Future character direction: playful and unpredictable without hostility.

Only Alfred is implemented today. Sam and Chaos remain product concepts, not code contracts.

## Offline speech rule

M5 fails closed to silence for character speech.

Android TextToSpeech voices are eligible only when Android reports that the voice does **not** require a network connection and it matches Alfred's language. Exact `en-GB` is preferred, then another local English voice may be used. An unrelated-language voice is rejected instead of producing misleading speech.

If local TTS is initializing, unavailable, interrupted or rejected:

```text
character speech fails
        ↓
critical alarm audio continues
        ↓
Wake Runtime may receive SpeechFailed only when production integration exists
```

M5 currently exposes this through Wake Alarm Lab only. Production connection to Active Wake Execution remains gated by physical reliability evidence.

## Morning voice flow, future production shape

```text
Alarm Kernel starts Active Wake Execution
       ↓
safe local Wake Motif / USAGE_ALARM output is already owned locally
       ↓
Wake Runtime starts deterministic session
       ↓
local Alfred rendering may speak a constrained SpeechIntent
       ↓
optional realtime voice may later enrich the same constrained intent
       ↓
provider/TTS fails → alarm path and Wake Runtime continue
```

Never show a critical-path spinner waiting for speech or AI.

## M8 provider spike

Do not freeze a generic provider abstraction before measurement. M8 will compare concrete architectures after local adaptive behavior exists.

### Direct OpenAI realtime

Measure:

- cold connection / first-response latency;
- interruption/barge-in;
- turn behavior in sleepy/quiet speech;
- reconnect/network transitions;
- speaker/Bluetooth behavior;
- SDK/transport complexity;
- credentials/security model;
- cost per wake.

### LiveKit

Evaluate only if it hides meaningful realtime/session complexity we would otherwise implement ourselves.

### ElevenLabs

Evaluate for differentiated character voice or prepared speech where quality/cost materially improves the product. Character identity must remain independent from one vendor voice ID.

## Controlled model envelope for future AI

A future model receives a constrained request such as:

```text
SPEECH INTENT
AskToMove

CURRENT INTERVENTION LEVEL
firm but not hostile

ALLOWED FACTS
- explicitly provided safe context only

CHARACTER
Alfred: dry, polite, concise

BOUNDARIES
- no invented facts
- no shame / insults / threats
- preserve requested action
- brief spoken output
```

Model output is language only. It cannot transition state or mutate alarm/runtime authority.

## Learning / voice boundary

Wake Learning may select bounded future behavioral policy parameters. A character renderer may express the selected intervention style, but it does not learn or mutate Wake Policy from arbitrary dialogue.

Do not use raw model transcripts as the default Wake Learning source. Prefer typed Wake Inputs, outcomes and optional calibration feedback.

## Character safety

Even strict characters do not:

- call the user lazy, stupid or weak;
- shame mental health, productivity or body;
- threaten consequences;
- invent social pressure;
- weaponize private context;
- yell continuously as default escalation.

Humor is allowed when it preserves dignity and the requested action.

## Data minimization

- no raw audio archive by default;
- no full transcript retention by default;
- no transcript persistence in M5 local speech;
- derive typed semantic Wake Inputs where possible;
- redact private context from diagnostics/analytics;
- use short-lived provider credentials when realtime arrives;
- enforce future live duration/turn/cost budgets.

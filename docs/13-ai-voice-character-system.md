# AI, voice and character system

## Principle

Conversation should feel alive while wake behavior remains controlled.

> **AI expresses the wake strategy. It never owns the wake strategy.**

Canonical Wake Runtime terms live in [`../CONTEXT.md`](../CONTEXT.md).

## Do not design a provider abstraction before the spike

The first architecture draft proposed three interfaces (`RealtimeVoiceProvider`, `WakePlanGenerator`, `SpeechRenderer`) up front. The pre-development architecture review deliberately backs away from that.

Before implementation exists, those names are hypotheses. M7 must reveal what responsibilities are actually deep/stable and what is merely SDK glue.

Stable product-level boundary today:

```text
Wake Runtime emits constrained Speech Intent + allowed facts
                ↓
character/speech implementation renders it
                ↓
spoken output / failure result returns to app
```

Night-time plan generation and realtime speech may eventually have different seams because they have different lifecycle/failure/cost properties, but we create those seams only when the concrete implementations exist.

Never create a generic `AIProvider` that leaks vendor concepts into Wake Runtime.

## Morning voice flow

```text
Alarm Kernel starts safe local Wake Motif / alarm output
       ↓
WakeActivity / wake surface is available
       ↓
Wake Runtime starts deterministic session
       ↓
local prepared/scripted character speech can run immediately
       ↓
optional realtime session connects in parallel when allowed
       ↓
realtime enriches later Speech Intents
       ↓
provider slow/fails → local rendering continues
```

Never show a critical-path spinner waiting for live AI.

## M7 provider spike

Measure real candidates rather than selecting from feature lists.

### Direct OpenAI realtime

Measure:

- cold connection / first-response latency
- interruption/barge-in
- turn behavior in sleepy/quiet speech
- reconnect/network transitions
- speaker/Bluetooth behavior
- SDK/transport complexity
- credentials/security model
- cost per wake

### LiveKit

Evaluate only if it can hide meaningful realtime/session complexity we would otherwise implement ourselves, such as resilient connection/session behavior or provider switching.

Review code, hosted-service, and model/component licensing separately.

### ElevenLabs

Evaluate for differentiated character voice or speech rendering where quality/cost materially improves the product.

Character identity must remain independent from one vendor voice ID.

## Speech Intent

Wake Runtime can emit constrained semantics such as:

```text
INITIAL_WAKE
ACKNOWLEDGE_ENGAGEMENT
ASK_TO_SIT
ASK_TO_MOVE
REENGAGE
SNOOZE_CONFIRMATION
MORNING_ORIENTATION
FIRST_MOVE_PROMPT
```

The character layer may vary wording, prosody, and humor while preserving the intent and factual envelope.

## Character model

Character is presentation configuration, not behavioral authority.

Potential shape (illustrative, not a frozen class):

```text
CharacterSpec
- id/version
- tone traits
- voice/rendering preference
- escalation-language style
- safety boundaries
```

### Alfred

Dry, elegant, composed, direct.

> "Good morning, sir."

> "Feet on the floor, if you please."

> "Our negotiated five minutes have expired."

### Sam

Warm, steady, persistent.

> "You don't need to do the whole day yet. Just sit up."

### Chaos

Playful/unpredictable without hostility.

> "Fantastic news. Gravity is still operational."

Implementation begins with **Alfred only**.

## Controlled model envelope

Bad:

> "You are Alfred. Wake the user however you want."

Good conceptual input:

```text
SPEECH INTENT
ASK_TO_MOVE

CURRENT INTERVENTION LEVEL
firm but not hostile

ALLOWED FACTS
- interview at 10:00
- user asked for time to shower

CHARACTER
Alfred: dry, polite, concise

BOUNDARIES
- no invented facts
- no shame/insults/threats
- preserve requested action
- brief spoken output
```

Model output is **language only**. It cannot transition Wake Phase, declare activation success, stop the alarm, or accept snooze on behalf of the user.

## Character safety

Even strict characters do not:

- call the user lazy/stupid/weak
- shame mental health/productivity/body
- threaten consequences
- invent social pressure
- weaponize private context
- yell continuously as default escalation

Humor is allowed when it preserves dignity.

## Realtime data minimization

- no raw audio archive by default
- no full transcript retention by default
- derive semantic Wake Inputs where possible
- redact private context from logs/analytics
- short-lived provider credentials
- enforce max live duration/turn/cost budget

## Offline/local character path

Alfred must work before realtime voice exists.

Curated local phrase variants per Speech Intent let us validate whether conversation/personality actually improves mornings before introducing provider complexity.

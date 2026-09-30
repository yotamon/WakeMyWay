# Personalized wake preferences and adaptive voice plan

## Status

**Product mode:** Shape  
**Implementation status:** documentation only; no production code is authorized by this document.  
**Purpose:** define the smallest coherent personalization system that makes the morning voice experience meaningfully different for different users without weakening Wake Runtime, Alarm Kernel, privacy, or user agency.

This plan complements:

- [04-ux-psychology.md](04-ux-psychology.md)
- [05-ux-flows.md](05-ux-flows.md)
- [11-wake-runtime-state-machine.md](11-wake-runtime-state-machine.md)
- [12-data-model.md](12-data-model.md)
- [13-ai-voice-character-system.md](13-ai-voice-character-system.md)
- [14-wake-strategy-learning.md](14-wake-strategy-learning.md)
- [22-product-metrics-experiments.md](22-product-metrics-experiments.md)
- [34-product-development-workflow.md](34-product-development-workflow.md)
- [43-character-system-product-plan.md](43-character-system-product-plan.md)

The active 1.0 feature freeze remains in force. This plan may enter Build only after the freeze ends or the product owner explicitly records an exception.

---

## Product hypothesis

WakeMyWay should not merely let a user choose a voice. It should learn and respect **how that person is best helped through the sleep-to-action transition**.

Two users may hear the same Character and still need meaningfully different morning behavior:

- one needs near-zero cognitive load and very short physical prompts;
- one responds to firm persistence;
- one becomes overwhelmed by morning context and needs a narrow first step;
- one is already awake but struggles to initiate action;
- one likes light social energy after engagement;
- one wants almost no conversation.

The user-observable outcome is:

> WakeMyWay feels like it knows how to wake me, while still behaving predictably, safely, and under my control.

Personalization earns its place only if it improves Wake Success and/or reduces Wake Friction. It must not optimize for conversation length, novelty, emotional attachment, or engagement with the agent.

---

## Architectural decision: separate explicit preference from learned profile

The repository already defines Wake Profile conceptually as **derived learning data**. Onboarding answers must therefore not be stored as a learned profile.

Use three separate inputs:

| Layer | Ownership | Example | May change automatically? |
| --- | --- | --- | --- |
| Wake Preferences | explicit user source data | “Be persistent, but keep it brief” | No |
| Alarm / occurrence context | explicit wake-specific source data | “Interview tomorrow; shower first” | No |
| Learned Wake Profile / Wake Policy | derived from outcomes and feedback | “Movement prompts tend to be needed earlier” | Yes, within existing bounded learning rules |

Character remains a fourth, orthogonal explicit choice:

- **Character** = who is speaking / emotional identity.
- **Wake Preferences** = how the user wants to be approached.
- **Wake Policy** = deterministic behavioral strategy.
- **Alarm context** = why this particular wake matters.
- **Wake Runtime** = final in-session authority.

Do not collapse these into one giant profile blob or one generative system prompt.

---

## Session strategy model

At the start of a Wake Session, resolve one immutable snapshot:

~~~text
Consumer Wake Preferences
          |
          |       Alarm Definition / Voice Style / Character
          |                    |
          |       Tomorrow Contract / First Move
          |                    |
          +----------+---------+
                     |
        Wake Learning policy snapshot
                     |
                     v
          Wake Session Strategy Resolver
                     |
                     v
             Wake Session Plan
          /          |          \
         /           |           \
 Wake Policy   Expression Profile   Allowed Context
     |                |                  |
     v                v                  v
 Wake Runtime    Realtime renderer   bounded voice facts
~~~

The Wake Session Plan is immutable for the duration of one wake attempt. Preference edits or learning updates take effect only on later sessions.

This preserves replayability and prevents mid-session personality or policy drift.

---

## Precedence and authority

When two inputs conflict, use this order:

1. safety, truthfulness, privacy, and Alarm Kernel / Wake Runtime invariants;
2. explicit per-alarm / per-occurrence user choices;
3. explicit global Wake Preferences;
4. learned policy/profile data;
5. stable product defaults.

Important consequences:

- Learning may not turn humor back on after a user disables it.
- Learning may not switch Character.
- A Character may not override a request for minimal conversation.
- “Firm” may change wording and bounded pacing, but it may not unlock shaming, yelling, threats, unsafe exercises, or hidden Stop/Snooze behavior.
- A Tomorrow Contract may enrich a permitted turn but may not cause the model to skip the current Speech Intent.
- No preference may directly mutate Alarm Kernel scheduling or critical execution state.

---

## Initial Wake Preferences model

The first implementation should keep explicit preferences deliberately small and interpretable.

Proposed domain model:

~~~text
WakePreferences
- morningBarrier
- perceivedWakeInertia
- interventionStyle
- motivationStyle
- conversationAmount
- humorPreference
~~~

### morningBarrier

Proposed values:

- HALF_ASLEEP
- SNOOZE_LOOP
- AWAKE_BUT_STUCK
- MORNING_OVERWHELM
- LOSE_TRACK_OF_TIME
- USUALLY_GET_UP

Purpose: select framing and re-engagement language. This is **not** a diagnosis.

### perceivedWakeInertia

Proposed values:

- FEW_MINUTES
- ABOUT_15_MINUTES
- ABOUT_30_MINUTES
- HOUR_OR_MORE
- UNSURE

Purpose: seed conservative pacing hypotheses. It must not be treated as measured physiology.

### interventionStyle

Proposed values:

- GENTLE
- ENCOURAGING
- PERSISTENT
- FIRM

Purpose: control wording/directness within the current Character and Speech Intent.

### motivationStyle

Proposed values:

- CONCRETE_ACTION
- ENCOURAGEMENT
- ACCOUNTABILITY
- LIGHT_CONVERSATION
- HUMOR

Purpose: decide which framing is useful **after the current physiological step allows it**. Early sleep-inertia turns still stay action-first.

### conversationAmount

Proposed values:

- MINIMAL
- BALANCED
- SOCIAL

Purpose: bound turn length, optional acknowledgements, and how much nonessential conversation is permitted after meaningful engagement.

### humorPreference

Proposed values:

- OFF
- LIGHT
- WELCOME

Purpose: constrain optional humor. Character safety rules still apply.

### Existing fields that remain separate

Do not duplicate these inside WakePreferences:

- display name
- default Wake Sound
- default Voice Check-In setting
- Voice Style
- default Snooze duration
- default First Move
- Character selection
- appearance

Where an existing field overlaps conceptually, prefer a clear resolver rather than storing the same choice twice.

---

## Onboarding UX

Current onboarding contains two product-education pages and may be skipped. Preserve that low-friction nature.

### Proposed flow

1. **WakeMyWay promise**
   - current “The alarm that learns how to wake you” story.

2. **Reliability and privacy**
   - current local-first explanation.

3. **Optional personalization entry**
   - title: “Teach WakeMyWay your mornings”
   - body: “Four quick choices help shape how the voice approaches you. You can change them anytime.”
   - actions: “Personalize my wake” / “Use balanced defaults”

4. **What usually happens when the alarm goes off?**
   - map to morningBarrier.

5. **How long until your brain feels properly awake?**
   - map to perceivedWakeInertia.
   - include “Not sure”.

6. **When you really do not want to get up, how should I approach you?**
   - map to interventionStyle.

7. **What usually gets you moving?**
   - map to motivationStyle.

8. **How much should I talk in the morning?**
   - map to conversationAmount.
   - humor remains a profile setting initially rather than adding another onboarding page.

9. **Optional First Move**
   - reuse existing defaultFirstMove model.
   - provide a few fast choices plus “Set later”.
   - do not require free text.

The personalization section is optional and skippable. Skipping stores stable balanced defaults rather than marking the profile incomplete forever.

### UX rules

- one decision per screen;
- large one-tap choices;
- no long explanatory copy while the user is trying to finish setup;
- always show that settings remain editable later;
- no pseudo-clinical labels such as “severe sleep inertia”;
- no claim that WakeMyWay already knows what will work;
- no more than one optional free-text field in onboarding;
- back navigation must preserve selections;
- onboarding completion must remain independent from account creation.

### Profile settings

Add one new Profile destination:

**Wake preferences**

It should show a compact summary:

~~~text
Approach        Persistent
Conversation    Minimal
Best motivator  Concrete action
Humor           Light
Morning pattern Snooze loop
~~~

Tapping a row opens the same choice components used in onboarding.

This screen is the source of truth for explicit preference. Users must not need to repeat onboarding to change it.

---

## Do not build “Teach me how to wake you” free text first

A free-text instruction such as:

> “Do not be fake positive. Be nice but do not let me bullshit you.”

is productively expressive, but it should not be the first implementation.

Reasons:

- arbitrary text must never be pasted directly into the Realtime system prompt;
- prompt injection and contradictory instructions become possible;
- free text is more privacy-sensitive than structured preferences;
- deterministic mapping and testing become harder;
- we do not yet know which preference dimensions matter enough to justify an interpreter.

Future safe version:

~~~text
user free text
      |
      v
strict structured preference extractor
      |
      v
allowlisted WakePreferences fields
      |
      v
user reviews the interpreted settings
~~~

The original free text should remain local by default and may be deleted after successful structured extraction if the product does not need it.

---

## Expression Profile

Wake Preferences do not go directly into model instructions. Compile them into a small typed presentation object.

Proposed shape:

~~~text
WakeExpressionProfile
- directness
- verbosity
- socialEnergy
- motivationFrame
- humorLevel
- responsePacing
~~~

Character + Voice Style + Wake Preferences resolve to this object.

Example:

~~~text
Character        Alfred
Voice Style      Minimal
Preferences      Persistent + Concrete action + Minimal + Humor off

Resolved expression
- directness: high
- verbosity: very low
- socialEnergy: low
- motivationFrame: action only
- humorLevel: off
- responsePacing: standard
~~~

Another user:

~~~text
Character        Sam
Voice Style      Default
Preferences      Gentle + Encouragement + Social + Humor light

Resolved expression
- directness: low
- verbosity: medium
- socialEnergy: medium
- motivationFrame: encouragement
- humorLevel: light
- responsePacing: patient
~~~

Character identity remains stable. A preference modifier may not make Alfred sound like Sam or vice versa.

---

## What should actually change in the morning

Each preference must have a visible behavioral consequence or it should not exist.

| Preference | Initial effect |
| --- | --- |
| HALF_ASLEEP | shorter early turns, fewer reflective questions, concrete action language |
| SNOOZE_LOOP | acknowledge bargaining briefly; return immediately to current action |
| AWAKE_BUT_STUCK | after engagement, use action-initiation framing instead of “wake up” framing |
| MORNING_OVERWHELM | suppress agenda dumping and future-task lists; focus on one next step |
| LOSE_TRACK_OF_TIME | later orientation may use concise time anchoring when truthful |
| GENTLE | softer wording, same required action |
| PERSISTENT | fewer conversational detours after resistance |
| FIRM | more direct wording at the same runtime escalation level |
| CONCRETE_ACTION | almost no motivational framing |
| ENCOURAGEMENT | brief supportive framing after engagement |
| ACCOUNTABILITY | refer to the user’s own stated plan only when explicit context exists |
| LIGHT_CONVERSATION | allow short social acknowledgement after engagement |
| HUMOR | allow bounded Character-compatible humor |
| MINIMAL | usually one short sentence |
| SOCIAL | allow a little more natural exchange after the current action is clear |
| humor OFF | suppress optional jokes even for a humorous Character |

The physiological wake progression remains governed by Wake Runtime. Preferences change expression and bounded pacing, not physical safety rules.

---

## Pacing personalization

Current voice timing contains fixed controller values such as the Realtime listen timeout and silence watchdog.

Do not scatter preference-specific timing branches through WakeVoiceSessionController.

Introduce one resolved, bounded session tuning object, for example:

~~~text
WakeConversationPacing
- listenTimeout
- idleReengageDelay
- preferredTurnLength
~~~

Initial values should remain narrow, conservative hypotheses. The exact durations must be validated in dogfood rather than declared from onboarding self-report as truth.

Rules:

- startup connection budget is capability/reliability behavior and is not personalized;
- Alarm Kernel audio behavior is never personalized here;
- pacing changes may not hide Stop/Snooze;
- one session snapshots one pacing configuration;
- future Wake Learning may tune pacing only after a dedicated bounded policy contract exists.

---

## Realtime contract changes

Current conversational boundary:

~~~text
respond(SpeechIntent, VoiceStyle)
~~~

Target boundary:

~~~text
respond(WakeSpeechRequest)
~~~

Representative request:

~~~text
WakeSpeechRequest
- speechIntent
- characterId / resolved Character expression
- voiceStyle
- expressionProfile
- allowedContext
~~~

The request remains presentation-only.

### Stable system prompt vs turn prompt

Keep the current identity-lock architecture:

- the session/system prompt defines the stable Character and global safety boundary;
- per-turn instructions define the current Speech Intent;
- personalization is a bounded modifier, never a new persona prompt.

Do not concatenate arbitrary user prose into the stable system prompt.

### Example modifier

A generated modifier should look conceptually like:

~~~text
USER WAKE PREFERENCES
- directness: high
- verbosity: minimal
- motivation: concrete action
- humor: off
- morning barrier: snooze loop

PRESENTATION CONSTRAINTS
- keep the same Character identity
- do not add motivation unless the runtime turn permits it
- do not mention these settings to the user
~~~

The model still receives one current runtime directive and may not infer state transitions.

---

## Per-alarm and per-occurrence context

Do not invent a second “reason to wake” model. Reuse Tomorrow Contract and First Move.

### Initial context allowed into conversation

Use only explicit, relevant facts:

- alarm label when safe and useful;
- user display name when configured and appropriate;
- Tomorrow Contract reason;
- First Move;
- scheduled local time if needed for truthful orientation.

### Privacy gate

Tomorrow Contract text is private local data today.

If raw or summarized contract text is sent to a Realtime provider, the UX must explicitly communicate that Voice Check-In may use that context in the cloud.

Recommended contract:

- local by default;
- per-occurrence “Use this in Voice Check-In” control when sensitive free text is present;
- no Tomorrow Contract content in Direct Boot state;
- no transcript/history persistence merely because context was used;
- do not send unrelated profile/account/calendar data.

### Context timing

Early wake:

- do not immediately dump the user’s reason, schedule, or tasks;
- physical activation still comes first.

After meaningful engagement / during appropriate runtime intent:

- the reason can be used as a brief self-authored anchor;
- First Move can become the immediate bridge into the morning.

Example:

~~~text
User contract: “Interview at 10. Shower first.”

Bad early turn:
“You have an interview at 10, need to shower, get dressed, prepare your notes...”

Allowed later turn:
“Interview morning. Shower is first. Feet down.”
~~~

The model may use only the facts the user supplied. It may not invent stakes or guilt.

---

## Learned Wake Profile and explicit preferences

Wake Learning remains responsible for derived strategy.

Explicit preferences and learned behavior must cooperate without silently overriding each other.

### Explicit preference wins for presentation

Examples:

- user chooses humor OFF -> learning never enables humor;
- user chooses MINIMAL conversation -> learning never decides to become chatty;
- user chooses Character Sam -> learning never switches to Coach.

### Learning may optimize behavioral policy

Existing Wake Learning can continue to reason about:

- Activation Completion;
- Confirmed Wake Success;
- intervention depth;
- snooze behavior;
- friction;
- activation threshold / max escalation within the current algorithm.

Future learning may add bounded timing parameters only through an explicit versioned Wake Policy expansion.

### Self-report is a seed, not truth

Onboarding answers should be treated as initial configuration, not empirical evidence.

For example:

> “It takes me an hour to wake up”

may justify more patient presentation, but should not directly lower activation thresholds or declare success later.

---

## Post-wake feedback loop

Personalization becomes useful when WakeMyWay can tell whether it helped.

Reuse the existing Morning Safety Check / calibration direction rather than creating constant surveys.

### Recommended occasional feedback

Question 1 already supported conceptually:

**Did you actually get up?**

- Yes
- I went back to bed
- I got up later
- Skip

Add a second lightweight question only on selected mornings:

**How did WakeMyWay feel today?**

- Too gentle
- About right
- Too intense

Do not ask every morning. Use sparse calibration.

### Storage

Do not infer this answer from transcript sentiment.

Store typed feedback attached to the Wake Outcome. If a new intervention-rating type is introduced, keep it separate from the existing annoyance/agency dimensions unless a deliberate mapping is validated.

### Learning use

One “too intense” answer should not instantly rewrite policy.

Repeated signals may:

- constrain future escalation;
- identify that stronger behavior increases friction without improving Confirmed Wake Success;
- suggest a user-facing preference adjustment;
- inform future bounded learning rules.

---

## Persistence plan

Wake Preferences are non-critical, credential-protected consumer data.

Initial implementation should extend ConsumerPreferencesRepository rather than inventing a database solely for six stable preferences.

### Schema migration

Current repository schema is v1.

Target:

~~~text
consumer-preferences schema v2
- all existing v1 fields
- wakePreferences { ... }
~~~

Requirements:

- explicit v1 -> v2 decoder path;
- safe defaults for absent fields;
- corrupt optional preference data must fail open to balanced defaults;
- in-place app upgrade preserves all existing preferences;
- no Wake Preferences copied into device-protected Alarm Kernel state;
- LocalWakeDataResetManager behavior must be deliberate and tested.

If preference complexity grows materially later, migration to a dedicated store can be reconsidered from evidence.

---

## Suggested implementation types

Likely new core/product types:

~~~text
core/personalization/
  MorningBarrier
  PerceivedWakeInertia
  InterventionStyle
  MotivationStyle
  ConversationAmount
  HumorPreference
  WakePreferences
  WakeExpressionProfile
  WakeConversationPacing
  WakeSessionPlan
  WakeSessionStrategyResolver
~~~

Exact package ownership should follow implementation locality. Avoid a generic “profile engine” module unless the contract becomes deep enough to justify one.

Likely Android/product touch points:

- ConsumerPreferencesRepository
- OnboardingScreen
- ProfileScreen
- new WakePreferencesScreen
- Alarm editor / preparation UI for Tomorrow Contract visibility
- WakeSessionViewModel / WakeVoiceSessionController session construction
- WakeConversationEnrichment
- DirectRealtimeWakeConversation
- DebugRealtimeWakeConversation
- Character prompt/rendering resolver
- WakeLearningRepository only where typed feedback/session snapshot needs integration

Do not touch Alarm Kernel scheduling, exact alarm registration, Direct Boot critical state, or terminal Stop/Snooze semantics for this feature.

---

## Rollout slices

### Slice 0 — documentation and review

This PR.

- finalize product semantics;
- resolve overlaps with Character / Voice Style / Wake Learning;
- define onboarding content;
- define data ownership and privacy;
- define testing/metrics;
- make no production changes.

Exit: implementation PRs no longer need to invent product behavior.

### Slice 1 — preference domain + persistence

No visible wake behavior change.

- add typed WakePreferences;
- add balanced defaults;
- ConsumerPreferences v2 migration;
- repository round-trip tests;
- update-persistence tests;
- reset behavior tests.

Exit: explicit preferences can be safely stored and edited programmatically without affecting a wake.

### Slice 2 — onboarding + profile editing

- extend onboarding with optional personalization flow;
- add Wake Preferences profile destination;
- reuse shared selection components;
- preserve skip/back/accessibility states;
- no Realtime behavior change yet.

Exit: user can complete, skip, review, and change all structured preferences.

### Slice 3 — session strategy resolver

- add WakeExpressionProfile;
- add bounded WakeConversationPacing;
- compile immutable WakeSessionPlan at session start;
- keep existing default users behaviorally equivalent;
- add resolver contract tests and precedence tests.

Exit: one tested session snapshot explains exactly which explicit/learned/context inputs will be used.

### Slice 4 — Realtime personalization

- replace respond(intent, style) with typed request;
- inject expression profile as bounded presentation instructions;
- preserve stable Character system identity;
- add prompt/snapshot tests;
- verify interruption and fallback behavior;
- ensure raw arbitrary user text never enters the system prompt.

Exit: two deliberately different profiles produce perceptibly different but semantically equivalent wake turns for the same Speech Intent.

### Slice 5 — Tomorrow Contract / First Move enrichment

- define explicit Voice Check-In privacy disclosure;
- add allowed-context snapshot;
- use reason only at appropriate runtime stages;
- use First Move during orientation;
- add redaction/privacy tests.

Exit: wake-specific context improves relevance without agenda dumping or leaking unrelated private data.

### Slice 6 — sparse feedback + learning integration

- add occasional “too gentle / right / too intense” feedback;
- attach typed signal to Wake Outcome;
- surface dogfood diagnostics;
- do not create automatic rules until enough evidence exists;
- only then decide which additional Wake Learning parameters are earned.

Exit: the system can measure whether personalization helps rather than merely feeling novel.

---

## Test strategy

### Domain tests

- every preference enum decodes safely;
- invalid/unrecognized persisted values fall back deliberately;
- preference precedence is deterministic;
- Character identity is never changed by preferences;
- explicit opt-outs are never overridden by learned data;
- session plan is immutable.

### Persistence tests

- v1 ConsumerPreferences -> v2 migration;
- v2 round trip;
- package upgrade preserves preferences;
- corruption fails open without blocking app startup;
- reset semantics are explicit.

### Prompt contract tests

For every SpeechIntent:

- requested physical/behavioral meaning remains unchanged across preference profiles;
- one profile cannot add a second action;
- FIRM does not introduce threats/shame/yelling;
- SOCIAL does not make early wake turns open-ended;
- humor OFF suppresses optional humor;
- MORNING_OVERWHELM does not introduce task lists;
- no raw preference/free-text field is concatenated into stable system instructions.

### UI tests

- onboarding personalize path;
- onboarding balanced-default path;
- skip;
- back navigation;
- process recreation/state restoration as appropriate;
- Profile edit/save/cancel;
- large font;
- TalkBack labels;
- dark/light product appearance;
- choice controls remain large enough for accessibility.

### Voice/device tests

- same Character remains perceptually stable across turns;
- personalized profile survives interruption/barge-in;
- conversation failure still degrades immediately to normal alarm-only behavior;
- personalization never changes critical alarm audibility;
- Stop/Snooze remain local and accessible.

---

## Measurement plan

### Primary product outcome

Personalization is successful only if it improves the existing product objective:

**Confirmed Wake Success with minimum effective friction.**

### Supporting metrics

Track privacy-safe typed values such as:

- time to first engagement;
- time to meaningful movement;
- time to Activation Completion;
- Confirmed Wake Success when calibrated;
- snooze count;
- maximum intervention depth;
- alarm-only fallback rate;
- “too gentle / right / too intense” feedback;
- preference completion rate;
- voluntary preference edits after real use.

Do not optimize for:

- total conversation duration;
- number of turns;
- emotional attachment to Character;
- app session length.

### Early dogfood comparison

Before broad rollout, compare within the same users:

- several recent wakes under existing balanced behavior;
- several wakes using explicit personalization;
- inspect Wake Success, movement timing, snooze, friction, and qualitative comments.

Treat before/after differences as directional because morning context is confounded. If the signal is promising, design a cleaner experiment before making strong causal claims.

---

## Failure and recovery behavior

Personalization is non-critical enrichment.

If any of the following fail:

- preference decoding;
- session strategy resolution;
- context preparation;
- prompt modifier generation;

then:

1. fall back to balanced known-safe expression defaults;
2. preserve the current Wake Policy snapshot where valid;
3. keep Realtime only if its normal contract remains valid;
4. otherwise degrade to alarm-only exactly as today.

A bad preference file must never make the alarm late, silent, non-stoppable, or dependent on the cloud.

---

## Privacy and safety checklist

- [ ] Wake Preferences remain credential-protected local data.
- [ ] No Wake Preferences enter Critical Wake State.
- [ ] No raw audio archive is introduced.
- [ ] No transcript retention is required.
- [ ] Raw arbitrary personalization prose is not sent as model instructions.
- [ ] Tomorrow Contract cloud use is clearly disclosed and bounded.
- [ ] Only context needed for the current Wake Session is sent to Realtime.
- [ ] Character safety restrictions remain global.
- [ ] User can edit/reset explicit preferences.
- [ ] Learned policy remains resettable derived data.
- [ ] Preference analytics use typed categories, not raw private text.

---

## Explicit non-goals for the first implementation

- medical sleep profiling or diagnosis;
- automatic Character switching;
- dozens of sliders;
- arbitrary custom system prompts;
- celebrity/voice impersonation;
- general-purpose morning assistant behavior;
- calendar/email/news briefing during early wake;
- free-form memory about the user;
- sentiment analysis of transcripts as a learning source;
- cloud-required preference storage;
- machine learning recommendation engine;
- changing Alarm Kernel behavior;
- making the wake harder merely because the user chose “firm”.

---

## Open product questions to resolve before Build

1. Should humorPreference be included in onboarding or only Profile settings?
2. Should default First Move remain onboarding-optional, or wait until first alarm setup?
3. Does Voice Style remain a visible per-alarm control after Wake Preferences exist, or should its role be simplified?
4. Which exact pacing dimensions are worth exposing to the resolver in Slice 3 after device dogfood?
5. What disclosure wording is clearest when a Tomorrow Contract may be used in Realtime?
6. Should “Too gentle / About right / Too intense” become a new typed outcome signal or map into existing Wake Friction feedback?
7. After the Character system expands, which preference combinations must be tested against every shipped Character?

These questions do not block documentation review. They must be resolved before the implementation slice they affect.

---

## Definition of Ready for Build

- [x] User/problem is clear.
- [x] Desired user-observable outcome is clear.
- [x] Authority boundaries are preserved.
- [x] Explicit vs learned data ownership is clear.
- [x] Main onboarding and Profile flows are defined.
- [x] Failure behavior is defined.
- [x] Privacy direction is defined.
- [x] Initial non-goals are explicit.
- [x] Success evidence is defined.
- [ ] Product owner reviews/accepts onboarding question wording.
- [ ] Product owner resolves the open questions required for Slice 1-3.
- [ ] 1.0 feature freeze ends or an explicit Build exception is recorded.

---

## Decision summary

WakeMyWay personalization should be built as a **typed strategy composition system**, not as a bigger AI prompt.

The implementation should combine:

- explicit Wake Preferences the user controls;
- explicit Character / Voice Style choices;
- per-wake Tomorrow Contract and First Move;
- derived Wake Policy from Wake Learning;
- one immutable Wake Session Plan;
- deterministic Wake Runtime authority;
- Realtime as a bounded natural-language renderer.

That architecture lets the morning voice become genuinely personal while keeping the product’s core promise intact: reliable waking, minimum effective friction, and user control.

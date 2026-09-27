# Character System — product and implementation plan

## Status

**Mode:** Shape

**Implementation status:** Planned only. No production behavior changes are authorized by this document.

**Launch-program constraint:** the active 1.0 program currently freezes net-new feature scope until TRUST / FINISH / PROVE evidence earns expansion. New characters are therefore a post-beta or explicitly re-scoped feature, not hidden 1.0 work.

## Why this exists

Wake My Way already has a strong single-character implementation: Alfred can render every Wake Runtime speech intent locally, Android TTS provides an offline fallback, and Direct builds can enrich the same deterministic wake flow with OpenAI Realtime.

The next product opportunity is not a generic voice picker. It is a small cast of clearly differentiated wake companions so a user can choose **who they want waking them up**, while preserving the product's core promise:

> The character may change expression. It must never become wake authority.

The desired user outcome is simple: **the user can choose a character whose manner makes the same wake process feel more tolerable, motivating, calming or entertaining without weakening alarm reliability or making behavior unpredictable.**

## Current implementation truth

Today:

- `AlarmDefinition` already persists a `characterId` per alarm.
- The only supported product character is Alfred.
- `AlfredCharacter` is a deterministic curated renderer in `:wake-core`.
- `WakeVoiceSessionController` is still hard-wired to `AlfredCharacter` for local rendering, local voice locale and speaker configuration.
- Direct Realtime is also hard-wired to an Alfred system prompt.
- `VoiceStyle` is already separate from Character and supports `DEFAULT`, `MOTIVATIONAL` and `MINIMAL`.
- Critical alarm delivery remains independent of Character, local TTS and Realtime.

There is also a small modeling inconsistency to remove before multi-character production work: `CharacterId` exists in both `core.alarm` and `core.character`. The persisted value is already a stable string, so this can be unified without changing the serialized alarm contract.

## Product rule: Character is not Wake Strategy

This is the most important boundary in the feature.

~~~text
Wake Policy / Wake Runtime
        │
        ├── decides phase
        ├── decides escalation timing
        ├── decides required activation evidence
        ├── emits SpeechIntent
        │
        ▼
Character
        ├── wording
        ├── warmth / humor / firmness of expression
        ├── prosody / voice preference
        └── visual identity
~~~

A Character must **not** secretly change:

- Wake Phase transitions;
- activation thresholds;
- snooze acceptance;
- escalation timing;
- completion criteria;
- physical-evidence requirements;
- whether the current Wake Session is successful.

This separation matters because Wake Learning must remain able to learn intervention policy independently from whether the user likes Alfred, Sam or Rocco.

If evidence later shows that users want a genuinely different behavioral approach, introduce an explicit separate concept such as a Wake Approach / policy preset. It may be co-located in UI with Character, but it must remain separately stored, explainable and testable.

## Smallest coherent experience

The first useful version should not launch with a giant character marketplace, sliders, memory, generated personas and custom voices.

The smallest coherent experience is:

1. A user who has Voice Check-In enabled can open the alarm editor.
2. They see a compact **Wake character** row with the currently selected character.
3. Opening it shows a small cast of character cards.
4. Each card has:
   - name;
   - one-line personality promise;
   - visual identity;
   - a short audio preview;
   - a selected state.
5. Selecting a character updates only that Alarm Definition.
6. The chosen character is resolved once at Wake Session start and remains immutable for that session.
7. If cloud Realtime is unavailable, the same character continues through its local curated renderer when local TTS is available.
8. If all voice capabilities fail, the critical alarm remains fully functional.

Character selection should not be mandatory during first-run onboarding. New alarms continue to default to Alfred. This keeps first-alarm creation simple and avoids forcing a personality decision before the user has experienced the product.

## Proposed first cast

Names are working product names until voice/art prototypes are reviewed. The roles are more important than the final names.

| Character | Core promise | Expression | Why it is distinct |
| --- | --- | --- | --- |
| **Alfred** | Composed competence | Dry, elegant, concise, lightly witty | Existing trusted baseline and default |
| **Sam** | Supportive friend | Warm, steady, human, persistent | Low-pressure emotional support without becoming sleepy |
| **Luna** | Calm presence | Soft, low-stimulation, reassuring, sparse | For users who dislike high-energy mornings |
| **Coach** | Clear momentum | Direct, encouraging, action-oriented | Stronger verbal push without shame or drill-sergeant behavior |
| **Rocco** | Humor as activation | Dry, cheeky, playful, lightly sarcastic | Makes repeated prompts feel less repetitive and more entertaining |

### Deliberately not in the first cast

**Nova / smart morning assistant** is interesting but should stay deferred initially. It naturally pulls the product toward calendar, weather, summaries and general-assistant behavior, while Wake My Way intentionally remains a waking product.

**Chaos** remains an earlier product direction, but Rocco is a clearer candidate for the same playful territory. Final naming should be decided after voice prototype review rather than by documentation alone.

## Character behavior contract

Every production Character must satisfy the same semantic coverage.

Current canonical `SpeechIntent` coverage:

~~~text
InitialWake
AskToSitUp
AskToMove
KeepEngaging
ReEngage(level 0..3)
SnoozeConfirmation
SnoozeFailed
Orientation
~~~

For every supported `VoiceStyle`, every Character must have deliberate language for every intent.

### Shared safety and dignity rules

All Characters must:

- be concise enough for sleep inertia;
- request only safe, small wake actions supplied by Wake Runtime;
- never shame the user for snoozing or being tired;
- never threaten, insult, humiliate or moralize;
- never diagnose, speculate about health or claim biological wakefulness;
- never claim Snooze succeeded before the Alarm Kernel confirms it;
- never claim the alarm stopped or the session completed unless the deterministic path says so;
- never invent calendar, weather, sensor or personal facts they were not explicitly given;
- remain interruptible during Realtime speech;
- avoid escalating profanity or cruelty merely because a character is sarcastic.

Humor can target the situation, not the user's worth.

## Character × Voice Style

`VoiceStyle` should remain a separate presentation setting rather than being replaced by character-specific sliders.

Phase-one UI can continue to expose the existing conceptual choices:

- Balanced / Default;
- Motivational;
- Minimal.

This gives useful combinations without a configuration explosion:

~~~text
Alfred + Minimal
Sam + Motivational
Luna + Minimal
Coach + Default
Rocco + Motivational
~~~

The previously discussed Gentle/Firm, Quiet/Energetic and Serious/Playful sliders should not be added in the first version. They overlap heavily with Character and Voice Style, create many hard-to-test combinations, and make it unclear what the user is actually choosing.

## UX specification

### Alarm editor

Voice Check-In section:

~~~text
Voice Check-In                    On

Wake character
[ Alfred                              > ]
Composed, dry and reliably concise

Voice style
[ Balanced                            > ]
~~~

When Voice Check-In is off, the Character row may remain visible but disabled with short helper text, or be hidden if product review finds the disabled state noisy. This is a Shape decision to settle in prototype review.

### Character picker

Working content hierarchy:

~~~text
Choose your wake character
Who do you want waking you up?

[ portrait / identity ]
Alfred
Composed, dry and reliably concise
[ Preview ]                         ✓

[ portrait / identity ]
Sam
Warm, steady and on your side
[ Preview ]

...
~~~

Do not expose implementation labels such as model provider, prompt version, TTS engine or Realtime status in the normal picker.

### Preview behavior

Preview should be short, safe and representative.

Requirements:

- one tap to play;
- one tap to stop / switching card stops the previous preview;
- no autoplay;
- preview copy is authored specifically as an introduction, not stolen from a live `SpeechIntent`;
- transcript/caption is available for accessibility;
- the Preview control remains usable with TalkBack and large text;
- provider/network failure does not block selecting the Character.

For Play/local-only builds, preview uses the local voice path.

For Direct/Realtime-capable builds, the first implementation may still use local preview for simplicity. A later slice may add a true Realtime preview only if the mismatch between preview and morning voice proves materially confusing.

### Active Wake

Character identity should be visible but subtle. The half-awake user should not receive a differently themed application per character.

Allowed variation:

- name / small identity mark;
- subtle accent or illustration;
- speech copy;
- voice/prosody.

Keep stable across Characters:

- Stop and Snooze affordances;
- state hierarchy;
- interaction layout;
- accessibility semantics;
- alarm-critical audio and controls;
- progress / activation meaning.

## State matrix

| State | Expected behavior |
| --- | --- |
| New alarm | Defaults to Alfred |
| Existing alarm from older version | Existing `alfred` value continues unchanged |
| Valid selected character | Use that character for the whole Wake Session |
| Unknown / retired character id | Resolve safely to Alfred; do not crash or invalidate the alarm |
| Realtime unavailable | Use selected character's local renderer if local voice is available |
| Local TTS unavailable | Voice Check-In degrades; critical alarm continues |
| Network unavailable | No effect on critical wake; local character remains eligible |
| Character preview fails | Selection still works; show a concise preview-unavailable state |
| User edits character while another Wake Session is active | Current session remains on its resolved character; next occurrence uses the new choice |
| Voice Check-In disabled | Character has no effect on the wake and does not become a readiness dependency |

## Architecture target

Keep the implementation deep and small. Do not create a provider/plugin framework merely because more than one Character exists.

~~~text
AlarmDefinition.characterId
          │
          ▼
   CharacterCatalog
          │
          ├──────── unknown id ───────► Alfred fallback
          │
          ▼
   ResolvedCharacter
          │
          ├── spec / version
          ├── deterministic local renderer
          ├── local voice preference
          ├── realtime expression profile
          └── UI presentation metadata
          │
          ▼
WakeVoiceSessionController
          │
          ├── LocalCharacterSpeaker
          └── WakeConversationEnrichment
~~~

### `:wake-core` responsibilities

Keep pure Kotlin:

- one canonical `CharacterId` type;
- stable `CharacterSpec` identity/version data;
- curated character renderers;
- deterministic variant selection;
- a compact `CharacterCatalog` / resolver;
- semantic coverage and safety tests.

Prefer an exhaustive known-character catalog over a general dependency-injection registry.

### Android app responsibilities

- character picker UI;
- visual assets and accessibility labels;
- preview orchestration;
- local Android voice resolution;
- passing the resolved Character into `WakeVoiceSessionController`;
- mapping a resolved Character into the optional Realtime adapter.

### Realtime responsibilities

Realtime remains expression only.

Instead of one hard-coded `AlfredRealtimePrompt`, the app should resolve an allowlisted Character expression profile containing:

- stable character id;
- prompt/presentation version;
- personality instructions;
- the same global safety/authority constraints;
- allowed provider voice selection.

The server/provider voice must be selected from an allowlist. Do not persist arbitrary provider voice identifiers as user-controlled alarm data.

The per-turn request must continue to contain the current Wake Runtime `SpeechIntent` and `VoiceStyle`, with the Character prompt acting only as stable presentation context.

## Persistence and compatibility

`AlarmDefinition.characterId` already serializes as a string. Adding valid IDs therefore does not require a new serialized shape by itself.

Rules:

- never rename a shipped Character id casually;
- character copy/prompt changes that would make deterministic replay misleading require a Character presentation-version bump;
- removal of a Character must keep a safe resolver fallback;
- a missing Character asset or provider voice must not make an Alarm Definition unreadable;
- alarm persistence tests must round-trip every shipped Character id;
- in-place app updates must preserve the user's character selection.

Unifying the duplicate `CharacterId` Kotlin types should preserve the serialized string exactly.

## Visual identity

Characters should feel distinct before playback without turning Wake My Way into a cartoon marketplace.

Direction:

- one restrained portrait / abstract identity per Character;
- one accent family per Character;
- shared typography, spacing, surfaces and controls;
- no face is required if abstract identities feel more premium and timeless;
- visual identity must work in dark mode, large text and RTL;
- color is never the only selection indicator.

Suggested working visual direction:

| Character | Visual mood |
| --- | --- |
| Alfred | tailored, warm neutral, understated sunrise |
| Sam | soft warm amber, rounded, approachable |
| Luna | deep blue / violet, quiet gradients |
| Coach | energetic high-contrast accent, clean geometry |
| Rocco | richer purple / red accent, mischievous but not aggressive |

These are prototype directions, not canonical design-system tokens yet.

## Learning and relationship

Do **not** mix Character personalization into Wake Learning v0.

Wake Learning should continue to learn policy effectiveness from outcomes.

Character preference can later be evaluated separately through lightweight product evidence:

- selected Character;
- voluntary switches;
- annoyance / comfort feedback;
- whether Character choice materially changes Confirmed Wake Success or snooze behavior.

Do not automatically switch a user's Character because another appears statistically more effective. Character is an explicit preference and should remain under user agency.

### Familiarity / relationship progression

Potential future enhancement:

- allow a Character to gain bounded familiarity in wording over repeated mornings;
- keep it local and non-creepy;
- do not fabricate memories;
- do not turn the product into a general companion.

This is intentionally not part of the first build slice.

## Custom characters

`Create your own character` is a future exploration, not part of the first production system.

It introduces materially harder problems:

- safety validation;
- prompt injection / abusive personas;
- offline fallback consistency;
- voice licensing and impersonation;
- moderation;
- deterministic replay;
- multilingual quality;
- provider cost.

The initial architecture should avoid blocking custom characters later, but must not add speculative generic-plugin machinery for them now.

## Success evidence

The feature earns expansion if it improves preference fit without damaging the wake outcome.

Useful evidence:

- percent of eligible users who intentionally choose a non-default Character;
- Character preview → selection conversion;
- Character switching after several mornings;
- self-reported annoyance / comfort;
- Confirmed Wake Success by Character, treated as observational rather than causal unless an experiment is designed;
- snooze / intervention depth;
- voice failure and fallback rates;
- Direct Realtime latency / interruption behavior per Character.

Do not optimize for time spent talking to a Character. Wake My Way succeeds when the user gets moving with minimum effective friction.

## Build slices

### Slice 0 — Shape and prototype

**No production behavior.**

- prototype the picker and alarm-editor row;
- prototype real preview copy and visual identity;
- produce short voice samples for the candidate cast;
- decide final launch names;
- verify that the roles are perceptually distinct without reading labels;
- decide whether Character row is hidden or disabled when Voice Check-In is off.

Exit evidence: a reviewer can understand the difference between Characters from the card + preview alone, and no production architecture decision is still being invented by UI code.

### Slice 1 — Character foundation, no UX change

- unify canonical `CharacterId`;
- introduce the small Character catalog/resolver;
- resolve Character once per Wake Session;
- route current Alfred local rendering through the catalog;
- route current Alfred Realtime instructions through a resolved expression profile;
- add unknown-id fallback;
- keep default behavior observably identical.

Exit evidence: existing alarms and Alfred behavior remain unchanged; all relevant tests pass.

### Slice 2 — Two-character local vertical slice

Add **Sam and one strongly contrasting Character** locally before the full cast.

- curated line coverage for every SpeechIntent × VoiceStyle;
- local voice preferences;
- character picker;
- preview;
- persistence/update coverage;
- visual regression / accessibility states.

Exit evidence: user can select, preview and wake with either Character fully offline, and critical alarm behavior is unchanged.

### Slice 3 — Direct Realtime parity

- add allowlisted per-character Realtime prompt/voice mapping;
- ensure the same Character selected locally is expressed in Realtime;
- verify interruption, fallback and budget limits;
- compare preview/morning mismatch.

Exit evidence: Realtime failure returns to the same selected Character locally rather than suddenly becoming Alfred unless Alfred is the selected/fallback Character.

### Slice 4 — Expand to the full cast

Only after dogfood evidence from the first additional Characters:

- add remaining approved cast;
- finish visual assets;
- validate cross-character differentiation;
- keep the picker small enough to scan half-awake / during setup.

### Slice 5 — Learn

After meaningful usage:

- identify unused / confusing Characters;
- inspect novelty wear-off;
- compare comfort and wake outcomes;
- decide whether to keep, rename, merge or remove roles;
- only then revisit additional Characters, familiarity, sliders or custom personas.

## Definition of Ready for Build

Before Slice 1 enters Build:

- [x] User/problem is clear.
- [x] Desired outcome is clear.
- [x] Reliability authority boundary is clear.
- [x] Persistence direction is clear.
- [x] Failure behavior is clear.
- [x] Initial non-goals are explicit.
- [ ] Candidate cast voice samples have been reviewed.
- [ ] Character picker interaction has been prototyped/reviewed.
- [ ] Final first-slice Character names are accepted.
- [ ] 1.0 scope freeze has ended or the feature has been explicitly re-scoped into the active program.

Before Slice 2 enters Build:

- [ ] exact local voice strategy for each added Character is accepted;
- [ ] preview copy is authored;
- [ ] complete SpeechIntent × VoiceStyle copy is reviewed;
- [ ] accessibility and compact-layout states are defined;
- [ ] relevant visual identity is approved.

## Explicit non-goals for the first release

- Character-specific Wake Runtime logic;
- Character-specific alarm reliability behavior;
- user-generated personas;
- celebrity impersonation;
- arbitrary uploaded voices;
- character marketplace;
- cloud-required character playback;
- relationship levels / gamification;
- automatic Character switching by learning;
- calendar/news/weather assistant behavior;
- dozens of personality sliders;
- unique Active Wake layouts per Character.

## Expected implementation touch points

Likely files / areas when Build is authorized:

- `apps/android/wake-core/src/main/kotlin/com/wakemyway/core/character/*`
- `apps/android/wake-core/src/main/kotlin/com/wakemyway/core/alarm/AlarmDefinition.kt`
- `apps/android/app/src/main/java/com/wakemyway/app/product/AlarmDefinitionRepository.kt`
- `apps/android/app/src/main/java/com/wakemyway/app/voice/WakeVoiceSessionController.kt`
- `apps/android/app/src/main/java/com/wakemyway/app/character/LocalCharacterSpeaker.kt`
- Direct Realtime character prompt/voice resolution
- alarm editor / setup UI
- character unit tests, persistence tests and visual/accessibility coverage

Do not touch Alarm Kernel scheduling, Direct Boot critical state, exact alarm registration or terminal Stop/Snooze semantics merely to add Characters.

## Decision summary

The product should evolve from **one hard-coded Alfred** into a **small, explicit Character catalog**.

The character system should create emotional and stylistic differentiation while deliberately keeping wake effectiveness, escalation and reliability under the existing deterministic systems.

That gives Wake My Way the memorable feeling of choosing *who wakes me up* without sacrificing the architecture that makes the alarm trustworthy.
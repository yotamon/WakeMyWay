# Morning Purpose — calendar-aware wake context (Explore / Shape)

**Status:** Proposed Shape contract, **not approved for Build**  
**Date:** 2026-10-21  
**Target:** WakeMyWay Android; optional future Google Calendar connection  
**Project scope:** Deferred beyond the active 1.0 core-experience quality reset unless explicitly accepted as a core-value exception  
**Owner of behavior:** Existing Wake Runtime and Wake Policy, never Calendar or the AI model  
**Related sources:** [Context](../../CONTEXT.md) · [Product workflow](../34-product-development-workflow.md) · [Calendar constraints](../15-context-calendar-weather.md) · [Tomorrow Contract / UX](../05-ux-flows.md) · [Wake Runtime](../11-wake-runtime-state-machine.md) · [Voice](../13-ai-voice-character-system.md) · [Privacy](../16-privacy-security.md) · [1.0 scope freeze](../45-core-experience-quality-reset.md) · [Open questions](../27-open-questions.md)

> **Thesis:** A clock knows *when*. Morning Purpose helps the chosen Character know *what makes this wake worthwhile* — without mistaking a calendar event for the user's own motivation.

## 1. Problem, user, and smallest promise

**User/job:** Someone has intentionally set a wake time, often has calendar commitments, and benefits from a brief truthful reminder of the morning's context — without waking into an unsolicited work agenda.

**Current alternatives:** No context; manually writing a Tomorrow Contract; opening Google Calendar; an AI assistant summarizing a schedule. An existing Tomorrow Contract is already useful. The incremental product hypothesis must be tested against it, not assumed.

**Proposed user-observable outcome:** After the user becomes meaningfully active, their chosen Character can naturally mention **one relevant, consented, current calendar fact** and, if useful, orient them toward one First Move. The user can decline, omit or request more detail. Before activation, only a bounded and explicitly opted-in reason cue may be considered for experiment.

**Success is NOT:** Completing a calendar briefing, eliciting more dialogue, maximizing engagement or making the user feel guilty about productivity.

**Hypotheses requiring evidence:**
1. One relevant fact produces more perceived usefulness than a generic wake and does not increase annoyance.
2. A manually supplied Tomorrow Contract plus limited calendar facts beats either a raw event list or autonomous "importance" inference.
3. Users trust opt-in context enough to allow it when private event titles are excluded by default.
4. The utility is real beyond novelty and measurable alongside Confirmed Wake Success.

**No new calendar tab, task manager, calendar edits, automatic alarm-time changes, background location, commute-time invention, all-day summarizer, or separate AI-based wake authority.**

## 2. Existing architecture and release boundary

- The Alarm Definition / Wake Occurrence / Alarm Kernel and safe local Wake Sound operate completely independently of optional context. Never make Calendar/OAuth/data freshness a base Wake Ready predicate.
- The Wake Runtime exclusively emits authorized Wake Directives and controls Alerting → Engaging → Activating → Orienting → Finished. Do **not** introduce a calendar-specific Wake Phase or mutate activation evidence from a model reply.
- Calendar may enrich the existing credential-protected Prepared Wake Plan for a specific Wake Occurrence; an unavailable plan must not block or delay Active Wake Execution.
- The Tomorrow Contract has explicit precedence over inferred context. Alarm label remains a truthful fallback as specified by reason-first UI.
- Live conversation is optional and currently under hardening. If unavailable or interrupted, the accepted consumer contract is local Wake Sound and safe Stop/Snooze only — **not generated TTS**.
- Current 1.0 quality reset defers new integrations. This is a Shape proposal, not an authorized 1.0 build or launch promise.

## 3. Narrative principles — no fake motivation

**Separate three kinds of knowledge:**

| Kind | Example | Allowed conclusion |
|---|---|---|
| Calendar fact | "Interview" at 10:00, accepted, timed event | There is an event at 10:00 |
| Deterministic context | It is the first selected relevant event after the wake; snapshot fresh | Its timing relative to the wake |
| User-provided meaning | "This interview matters to me" | It is personally important to this user |

An event title alone cannot prove urgency, happiness, dread, importance or the user's reason to live/get up. The model must never add invented emotions, personal stakes, travel time, preparation duration, or an unsupported promise that "there is plenty of time." Avoid coercion and shame.

**Priority of reason:** occurrence-specific Tomorrow Contract > user-marked relevant event/meaning > time-relevant accepted commitment (neutral fact, never a claim of emotional importance) > existing alarm label > no reason cue.

**Selection:** Default to zero or one event fact. User-requested expansion, only after Orienting, may give the next few selected facts. Do not read an agenda during sleep inertia.

**Data freshness:** "Nothing scheduled" and "I cannot verify your calendar right now" are distinct states. Treat schedule absence as true only after successful, adequately fresh and complete reads for the selected calendars/window.

## 4. Detailed experience (Shape content inventory)

### 4.1 Enrollment — optional, AFTER the first successful wake

Entry: **You → Morning context** (or a relevant, dismissible Today entry once the user has experienced the core wake). No permissions or account required for first alarm. Explain simply: "When you're ready, I can mention what matters from tomorrow's calendar — not your whole schedule."

- Action: **Connect calendar**. Source choice is deferred (section 7).
- Consent: read-only, selected calendars, title-sharing choice if remote AI is used, and how to disconnect.
- Default: private/confidential titles hidden; only selected calendars; minimal briefing; no automatic disclosure of event titles to a remote model without explicit informed consent.
- Success state: "Calendar connected · Last checked [local time]" with a preview of **what tomorrow's wake would hear**.
- States: not connected, connecting, needs permission, connected/no selected calendars, no upcoming events, connected/ready, last-sync stale, offline, token revoked, unsupported/empty provider, calendar changed, and disconnected.
- Denial/dismissal: return to ordinary wake with no nagging or readiness warning.

### 4.2 Evening — Tomorrow Contract is the primary purpose input

Inside existing Tonight / next-wake area, *only when connected and appropriate*, show one compact suggestion such as:
- "Tomorrow · 10:00 interview" (not an inferred emotional priority);
- actions **Include in my morning** / **Not this time**;
- optional private "What would you like morning-you to remember?" linked to the existing occurrence-specific Tomorrow Contract.

No daily required ritual, questionnaire or modal on every visit. Explicitly preserve the possibility of an empty Tomorrow Contract.

Each selected event applies to one Wake Occurrence, not every alarm sharing a schedule. Editing/snoozing alarm B may not alter context for alarm A.

### 4.3 Morning — progressive disclosure by existing Wake Phase

| Wake Phase | Proposed calendar behavior | Example permitted |
|---|---|---|
| Alerting | No event names or agenda; Alarm Kernel starts reliably | Local Wake Sound |
| Engaging | Default no unsolicited calendar disclosure. Test *optional* short user-chosen reason cue separately | "You've got something you wanted to remember today" **only** with an explicit corresponding user intention |
| Activating | Focus on one small present action, no task cascade or time-pressure dramatization | "Start by sitting up; we can talk about the day in a moment" |
| Orienting | May share one consented, fresh, relevant fact plus First Move, in the stable Character voice | "Your interview is at ten. Want the rest of your morning plan?" |
| Finished | No extra automatic briefing after completion | Conversation ends |

**User asks sooner:** The model may acknowledge the question gently and offer a short authorized answer only within the allowed privacy/timing policy; calendar context must not bypass Wake Runtime authority or become a reason to keep the wake alive. A subsequent post-wake summary is a separate future capability, not assumed.

**One-turn constraint:** Early spoken turns stay short and single-purpose. Never simultaneously demand speech, body movement and agenda processing. Calendar copy does not replace the typed current Speech Intent.

### 4.4 Morning conversation examples (illustrative, NOT a scripted model prompt)

**A. Selected meaningful event**
- User evening intention: "I want to feel prepared for my interview."
- After Orienting: "Your interview starts at ten. You wanted a calm start. Coffee first, or do you prefer a shower?"
- User: "I hate interviews."
- Character: "Fair. You don't have to handle the interview yet. Let's start with the first thing you chose."
- No claim the interview is positive; no invented coaching or pressure.

**B. Full work calendar, no marked purpose**
- Character after Orienting: "Your first calendar event is at nine thirty."
- User: "Anything else?"
- Character: "There are more items later. Want a brief overview?"
- No unrequested list; no "big day" emotional judgment.

**C. No events / offline / private**
- Valid fresh empty result: "Nothing on your selected calendar this morning. What's your first move?" (optional; never imply no other commitments exist).
- Offline / expired snapshot: avoid asserting an empty agenda, use alarm reason/First Move instead.
- Private event: at most a generic "There's an event later" **only if** the user opted into private-event existence disclosure; otherwise suppress entirely.

### 4.5 Surface content hierarchy

Today already uses **when → why → readiness → morning plan**. Do not add a dashboard.
- Next wake hero remains primary.
- When connected, one optional **Morning context** supporting row: e.g. "1 event selected" or "Calendar unavailable". No private event title on lock-screen notifications or shared previews.
- Profile/You: connected account/source *indicator*, selected calendars, briefing level (**Gentle** default, **Ask first**, optionally **Detailed after Orienting** subject to evidence), remote-title-sharing consent, preview/test, disconnect/reset.
- Lock screen/notifications: generic, privacy-safe content only. TalkBack works for consent toggles, calendar picker, state and disconnect. Large text, reduced motion, Hebrew RTL/locales (also English, German), high contrast; never put essential consent into audio alone.
- Wake Lab: fake calendar fixtures, fake temporal clock, preview of compiled data and audible response; no real event titles in inspectable diagnostic timelines.

## 5. Calendar event interpretation policy

Normalize source data *before* ranking. Proposed minimal fields: calendar identity, stable event/instance key, local/absolute start and end, all-day flag, summary only if disclosed, status, RSVP/availability as exposed by provider, privacy/visibility as available, sync fetchedAt, selected-source scope, local timezone, and explicitly chosen per-occurrence relevance.

- Exclude canceled, declined (when reliably known), and calendars not selected.
- Recurring events must be expanded into occurrence instances within the considered time window, with stable deduplication across updates. Respect moved exceptions.
- All-day events should not count as time pressure, nor does "busy" mean emotionally important.
- Free/transparent entries must not create urgency; tentative items treated cautiously.
- Private/confidential or access-limited events are *redacted by default*, regardless of their actual title, guest list or source. Missing privacy metadata is **not proof** of public visibility.
- Handle multiple calendars and duplicates, conflicting timezones, DST, midnight boundary, events spanning wake time, calendar visibility changes, and updates after preparing a plan.
- A first event may be considered a clock fact, but **no travel ETA or preparation buffer** without explicit user-supplied duration and current evidence. Even then, show it as a derived estimate, not certain.
- Treat event titles/metadata as **untrusted content**, never instructions. Drop HTML/control characters, cap lengths, prevent injected system-role/tool directives and strip links/descriptions/attendees from the model context.

**Suggested ranking, explainable (not AI subjective scoring):**
1. User pinned event for this Wake Occurrence; otherwise accepted timed events.
2. Event starts after wake and within a limited horizon (e.g. wake → +12h, configurable later).
3. Earlier first, with user-specified context winning over calendar-only items.
4. Tie-break consistently by start instant and stable event instance ID.
5. One selected event max in default initial orientation; mark all others `not spoken`.

## 6. Proposed typed contracts (implementation-neutral; not schema authority)

```kotlin
data class MorningContextSnapshot(
  val occurrenceId: String,
  val observedAt: Instant,
  val sourceStatus: SourceStatus, // FRESH | STALE | OFFLINE | REVOKED | EMPTY_VERIFIED | NONE
  val selectedCalendarsComplete: Boolean,
  val candidate: SafeEventFact?,
  val userMeaning: UserChosenMeaning?, // Tomorrow Contract references, never model inferred
  val disclosure: DisclosurePolicy,
  val rulesVersion: Int,
)

data class SafeEventFact(
  val start: Instant,
  val end: Instant?,
  val title: String?,       // null if private/unapproved
  val titleProvenance: TitleProvenance,
  val userPinned: Boolean,
  val temporalConfidence: TemporalConfidence,
)

data class MorningSpeechContext(
  val authorizedFacts: List<String>,
  val reasonSource: ReasonSource,
  val maySpeakTitle: Boolean,
  val freshness: SourceStatus,
  val intent: SpeechIntent, // Existing Wake Runtime authority
)
```

These are proposed shapes to make the trust boundary testable; do not create another public domain glossary or persist provider SDK models in `:wake-core`. Real implementation should keep Calendar/Android SDK types in the app-side data adapter, and pass only pure data required by the existing Wake Runtime/speech seam. The normal app owns provider permission/token state, never the Alarm Kernel.

**Temporal integrity:** Prepared Wake Plan references the exact upcoming Wake Occurrence. Context does not transfer blindly to another day or an independent alarm. Validate occurrence ID, consent revision, rules version, selection revision and calendar freshness before use. A changed/revoked consent invalidates prepared private content. Never place it in device-protected critical state.

## 7. Provider strategy — explicit decision gate

**Existing canonical preference:** `docs/15-context-calendar-weather.md` prefers Android Calendar Provider over immediate Google OAuth for Android V1. This proposal does **not** silently reverse that decision.

| Alternative | Why | Trade-off |
|---|---|---|
| A. Fake fixtures in Wake Lab | Validates wording/relevance/annoyance immediately; zero OAuth | Cannot prove sync or consent UX |
| B. Android Calendar Provider, selected calendars | Local-first Android pilot; can read already synced Google calendars | Requires READ_CALENDAR, depends on OS sync/provider metadata; Android-only |
| C. Direct Google Calendar API with least-privilege OAuth | Explicit Google connection, provider semantics and future multi-device reuse | OAuth consent/verification, token storage/revocation, backend/security costs |

**Recommendation:** A first. B as an optional *local dogfood pilot* if provider consistency is acceptable. Make a separate evidence-backed B-vs-C decision before promising "Connect Google Calendar" publicly. For C, review `calendar.events.readonly` and `calendar.calendarlist.readonly` vs `calendar.readonly` using actual required endpoints; ask for incremental read-only scopes separately from Google Sign-In. No write scopes. Google App Verification and privacy-policy review are release prerequisites as applicable, never presumed complete.

**References (review at Build time):**
- [Google Calendar API OAuth scopes](https://developers.google.com/workspace/calendar/api/auth)
- [Google events.list](https://developers.google.com/workspace/calendar/api/v3/reference/events/list)
- [Incremental sync; 410 invalidation](https://developers.google.com/workspace/calendar/api/guides/sync)
- [Android CalendarContract.Instances](https://developer.android.com/reference/android/provider/CalendarContract.Instances)

For direct Google, use narrow query windows where possible. Incremental sync tokens require consistent query parameters and may require a 410 full resync; do not assume an arbitrary time-windowed `syncToken` query is valid. Initial release should prefer bounded polling at meaningful times over always-on push/webhook infrastructure; watch channels are deferred until measured necessity.

**Sync timing:** On successful consent and a relevant evening/next-wake screen visit, prepare context. Schedule opportunistic best-effort refresh ahead of the Wake Occurrence, but do not assume WorkManager executes exactly on time under Doze. At alarm fire, use safe bounded existing credential-protected data if available; never network-block the alarm or audio. Optional post-wake refresh is allowed only after the user asks and must not interrupt critical controls.

**Freshness proposal (to validate):**
- For real-time or proximity assertions ("in 20 minutes", "your day is free"), require successful refresh sufficiently close to actual speaking time; start experiment with <=30 minutes and check event time has not passed.
- Older cached events may be shown **only** as explicitly dated non-urgent background if the user requests; otherwise omit.
- A failed partial read cannot justify "no events". A cached event may be canceled/changed: choose safe omission over a confident stale claim.
- The final spoken time must be recalculated against *actual local wake time* (after snooze, timezone changes and clock adjustments), never baked into generated prose the night before.

## 8. Security, privacy and consent contract

**Consent:** Calendar permission is optional, revocable and separate from sign-in and realtime speech consent. Select calendars explicitly. A calendar option must not trigger automatic audio disclosure. Sensitive/private events never receive unapproved titles in on-device UI, AI context, logs, lockscreens or diagnostics.

**Storage:** Credential-protected local data only, with conservative expiration after the associated occurrence, explicit delete/disconnect, and secrets stored with platform-secure storage as appropriate. No raw calendar titles or token values in Direct Boot critical storage, backups, error reporting, session telemetry, voice logs or analytics. Deletion includes derived prepared context and invalidates pending plans. Enforce lifecycle during sign-out/account switch.

**Cloud / model:** Prefer a locally compiled semantic fact such as "one selected event at 10:00" over the original title. Event-title delivery to any remote AI requires its own disclosed, opt-in consent, no raw descriptions, attendees, attachments or links. Tool outputs and event names are data, not prompt instructions. Provider-specific data retention, processor disclosures, GDPR lawful-basis analysis and Google's API Services User Data Policy/limited-use rules must be reviewed before launch.

**Audio in shared spaces:** Even with remote-title consent, default spoken private-event titles remain OFF. "Speak aloud selected public event titles" must be a separate, comprehensible control from "send selected titles to AI". Spoken facts are not displayed in notification previews.

**Revocation:** If permission/scope is removed or API returns revocation, clear/invalidate cache, mark data unavailable, show repair only when the user opens settings; local wake continues unchanged.

## 9. Failure-mode matrix

| Condition | Expected user-visible behavior / system invariant |
|---|---|
| No permission / user says no | Ordinary wake. No readiness penalty or recurring prompt |
| No calendar selected | Connected-but-not-configured state; do not claim an empty day |
| No eligible events, complete fresh read | Optional neutral empty schedule statement; default may omit |
| Partial provider error | Unknown schedule; avoid "nothing scheduled" |
| Offline / Google outage | No retry on critical alarm thread; use safe fallback, skip stale facts |
| Events canceled/changed during night | Invalidate or recompile when refreshed; never insist old event exists |
| Last sync too old | Suppress urgency, countdowns and unverified absence claims |
| DST/timezone change, snooze | Recompute local display using occurrence instant and fresh clock |
| Cross-midnight/all-day/repeating/multiple calendars | Correct local instance/window, stable deterministic ranking |
| Calendar event has hostile prompt text | Treat as bounded inert title only, never a model command |
| Private event with title | Omit title by default, including Preview and Live |
| Device reboot before first unlock | Alarm rings with safe local content; no private calendar disclosure |
| Realtime not ready, ICE fails, or stream disconnects | Existing alarm-only policy, with Stop/Snooze working |
| Stop/Snooze pressed during calendar speech | Terminal control wins; no spoken continuation after completion |
| AI hallucinates reason/time | Reject/restrict through bounded facts/intent contract; record privacy-safe defect |
| Permissions revoked mid-session | Stop using titles/facts immediately upon detected invalidation; don't claim event deletion |
| Two alarms, simultaneous wakes | Kernel owns sole active execution; plans never cross occurrence IDs |

## 10. Validation and experiments

**Baseline:** Current normal voice wake and existing Tomorrow Contract, not a hypothetical empty alarm.

**Lab fixtures:** (1) interview user-marked, (2) busy schedule, (3) no events fresh, (4) stale/offline, (5) declined/canceled, (6) duplicate/recurring override, (7) all-day/free, (8) cross-timezone/DST, (9) private/confidential, (10) malicious title, (11) last-minute changes, (12) multiple alarms and snooze, (13) permission revocation, (14) live failure.

**Contract tests (before consumer rollout):**
- deterministic selection and provenance, no invented emotional meaning;
- same input/rule version → same safe context, even with calendar ordering differences;
- private titles never appear in default payloads, screenshots or diagnostics;
- event title injection cannot invoke tools or mutate Speech Intent;
- invalid/stale snapshots never assert meeting immediacy or empty schedule;
- prepared plan bound to correct occurrence and consent version;
- Alarm Kernel, Stop/Snooze and Wake Runtime behavior unchanged when any context call fails;
- local/timezone/DST and recurrence property tests;
- Android permission and reboot/direct-boot safety;
- real-device locked screen, Bluetooth, Doze, process recreation, provider and Live failure.

**Dogfood protocol:** Within-person paired mornings, compare 0-context vs selected-event context (and optionally Tomorrow Contract-only) over enough mornings to reduce novelty. Short feedback after the wake, not during alarm: useful? accurate? too much? privacy concern? Collect **Confirmed Wake Success** calibration separately from Activation Completion. Avoid counting more spoken turns as success.

**Candidate success gates (hypotheses, not measured):**
- zero severe privacy/permission/terminal-action/reliability regressions;
- at least 80% of rated context mentions considered accurate/relevant, with adequate response sample;
- annoyance and perceived agency no worse than baseline;
- no negative trend in confirmed real-world wake success, activation delay or snooze outcome;
- measured variable cost per context-enriched live wake compatible with existing Plus economics.
Pre-register sample sizes/thresholds before user beta; do not call these validated targets today.

## 11. Incremental implementation plan (gated)

| Stage | Build slice | Exit / dependency |
|---|---|---|
| 0. Shape | Approve semantics, copy, privacy consent and test fixtures | Product review/Definition of Ready; no production changes |
| 1. Wake Lab | Synthetic context fixture compiler and in-session preview, no permissions | Users judge 1-event reminder natural, persona stable |
| 2. Local pilot | Calendar Provider read-only opt-in + selected calendars + protected snapshot | Instrumented privacy/recurrence/offline acceptance |
| 3. Consumer source decision | Compare B vs direct Google OAuth; explicit ADR if architecture/security warrants | Consent verification plan, cost, reliability evidence |
| 4. Live enrichment | Inject minimized prepared facts into existing authorized Speech Intent/Character path | Physical tests show no extra voice latency or runtime authority leak |
| 5. Controlled rollout | Progressive opt-in, metrics and off switch | Privacy, reliability, usefulness and annoyance gates met |

**Cut-line for V0:** One selected calendar, one next accepted timed event, one optional reminder after Orienting, no direct OAuth until source decision, no automatic alarm editing. Multiple calendars can remain source-supported behind a bounded surface if low cost, but must not force scope expansion.

## 12. Definition of Ready and unresolved decisions

| DoR condition | Status |
|---|---|
| User problem, desired outcome, non-goals | **Shaped** |
| Core happy path / empty, offline, denied, revoked / critical controls | **Shaped, needs UX prototype review** |
| Existing authority, Direct Boot and safety boundaries | **Shaped** |
| Calendar source / scope / OAuth verification | **Open** (B vs C) |
| Spoken title privacy defaults and separate AI-data consent | **Proposed; legal/security review open** |
| Briefing timing and default per-phase content | **Proposed; Wake Lab evidence open** |
| Freshness threshold, retention duration, calendar selection defaults | **Proposed; empirical validation open** |
| Success / annoyance / confirmed wake metrics | **Specified; baseline data open** |
| 1.0 feature-freeze exception or post-1.0 sequencing | **Not approved** |

**Do not enter Build just because this spec exists.** First run a small Wake Lab interaction study and review (a) real usefulness, (b) permission/data minimization, (c) provider strategy, (d) existing core reliability gates. If value cannot beat simple Tomorrow Contract reminders, archive or reduce the feature.

## 13. Explicit human/external actions once Build is authorized

No external action is needed to review **this documentation-only proposal**.

Future direct-Google integration, **only if selected**, will require project owner actions: Google Cloud app/project ownership, Calendar API enablement, OAuth consent and requested readonly scopes, OAuth Android/package-signing SHA setup and redirect/client verification as applicable, privacy policy/processor disclosures, provider verification if required, real-device sign-in/permission test accounts, and staged Play rollout approval. Do not assert these are configured now. Calendar Provider pilot instead requires runtime Android calendar permission and device sync test evidence, not Google OAuth setup.

## 14. Source verification and upkeep

Google platform documentation checked 2026-10-21; recheck during Build because scopes, OAuth verification and Play requirements may change:
- https://developers.google.com/workspace/calendar/api/auth
- https://developers.google.com/workspace/calendar/api/v3/reference/events/list
- https://developers.google.com/workspace/calendar/api/guides/sync
- https://developer.android.com/reference/android/provider/CalendarContract.Instances

Maintain this artifact as **Shape**, not a claim of implementation. Current shipped behavior remains authoritative in `docs/00-project-status.md`. New durable decisions require `docs/26-decisions-log.md` or an ADR; unresolved material choices live in `docs/27-open-questions.md`.

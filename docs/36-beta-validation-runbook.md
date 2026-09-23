# WakeMyWay 1.0 target-user beta runbook

**Gate:** PROVE #87  
**Purpose:** produce interpretable evidence about the existing WakeMyWay core without expanding product scope  
**Cohort:** 10–20 qualified habitual snoozers  
**Typical exposure:** 14–21 days where feasible; at least 5 intended WakeMyWay mornings for basic participant-level outcome summaries  
**Rule:** missing evidence remains unknown; never convert missing calibration into success

## What this beta must answer

The beta is not a popularity test and not an AI novelty test. It must answer four separate questions:

1. **Trust** — did the expected alarm arrive and remain controllable?
2. **Outcome** — did the person actually get up, rather than only satisfy phone-observable activation?
3. **Experience** — was WakeMyWay useful enough, with acceptable annoyance and preserved agency, that the person chose to keep using it?
4. **Commercial value** — after real mornings, does the person choose the proposed paid value at a concrete package/price rather than merely saying the idea sounds valuable?

Keep those questions separate in collection and reporting.

## Participant qualification

Use a short screener before enrollment. Include a participant only when all of these are true:

- they routinely snooze, dismiss reflexively, or stay in bed after an alarm often enough that the problem matters to them;
- they genuinely want to get up at a chosen time on multiple upcoming mornings;
- they can use a currently supported Android device;
- they expect at least five intended wake mornings during the study window;
- they agree to lightweight feedback and the privacy-safe evidence collection described for the beta.

Record context, not a medical diagnosis:

~~~text
participant id:
phone model / Android version:
usual alarm app:
typical intended wake days per week:
typical snoozes / dismiss pattern:
what usually causes the morning to fail:
what they currently do to compensate:
whether they use a backup alarm / another person:
what a meaningfully better morning would look like to them:
~~~

Do not recruit primarily from developers, AI enthusiasts, close collaborators, or people participating mainly to help the founder.

## Baseline

Where practical, capture 3–5 ordinary-alarm mornings before WakeMyWay. Do not exclude an otherwise useful participant only because a clean baseline is impossible.

For each baseline morning capture only what the participant can reasonably report:

~~~text
intended wake time
actually got up near intended time? yes / no / later / unsure
number of snoozes if known
returned to bed after first dismissal? yes / no / unsure
backup/person intervention used? yes / no
one-line friction note
~~~

Baseline is context, not a randomized control group. Do not overstate causal conclusions.

## Eligible WakeMyWay morning

An **expected wake** enters the reliability denominator when:

- the participant intentionally had WakeMyWay scheduled for that morning; and
- the occurrence was not deliberately cancelled/disabled before the agreed pre-sleep cutoff.

Exclude documented participant cancellations from the expected-wake denominator. Do not exclude an occurrence merely because the app failed.

Every expected wake must end as one of:

~~~text
DELIVERED_AND_DIAGNOSABLE
MISSED_OR_LATE
USER_CANCELLED_BEFORE_CUTOFF
UNKNOWN / EVIDENCE_INCOMPLETE
~~~

Keep UNKNOWN visible.

## Per-morning evidence

Prefer existing local semantic evidence. Never ask for raw microphone audio or full transcripts.

### Reliability

~~~text
expected wake? yes / no
target time
alarm delivered? yes / no / unknown
late/missed report? yes / no
fallback used? yes / no
Active Wake recovery event? yes / no
Safety Backup used? yes / no
diagnostic report available if needed? yes / no
~~~

### Phone-observable behavior

~~~text
time to first engagement
time to meaningful movement, when available
Activation Completion? yes / no
time to Activation Completion
snooze count
intervention depth
terminal reason
~~~

### Calibrated real-world outcome

Use the existing semantic calibration outcomes:

~~~text
GOT_UP
RETURNED_TO_BED
GOT_UP_LATER
NOT_SURE / SKIPPED
~~~

Never derive one of these from Activation Completion when calibration is missing.

### Lightweight experience feedback

Do not survey every morning unless needed. Sample periodically or after notable failures.

Use short bounded prompts such as:

~~~text
annoyance: 1 very low → 5 too much
agency: 1 felt controlled → 5 felt in control
did anything confuse you? optional short text
what, if anything, helped most? optional short text
~~~

Do not ask leading questions such as "Was Alfred helpful?"

## Canonical denominators

Always report numerator and denominator together.

### Alarm delivery success

~~~text
expected wakes with timely/diagnosable alarm delivery
÷ all expected wakes
~~~

Unknown/missed evidence stays in the denominator unless the wake was deliberately cancelled before the cutoff.

### Activation Completion rate

Report two views:

~~~text
Activation Completion ÷ expected wakes
Activation Completion ÷ successfully delivered wakes
~~~

The first preserves end-to-end product truth. The second isolates post-delivery wake behavior.

### Calibration coverage

~~~text
mornings with GOT_UP / RETURNED_TO_BED / GOT_UP_LATER
÷ mornings eligible for later calibration
~~~

NOT_SURE / SKIPPED is not a confirmed outcome.

### Confirmed Wake Success

~~~text
GOT_UP
÷ calibrated mornings with a determinate outcome
~~~

Always display calibration coverage beside this rate. Never report Confirmed Wake Success without its denominator and coverage.

### Activation Completion → returned-to-bed

~~~text
RETURNED_TO_BED after an Activation Completion
÷ calibrated Activation Completion mornings with a determinate outcome
~~~

This is the key guard against allowing WakeRuntime to certify its own success.

### Snooze and intervention depth

Report distributions, not only averages:

- mornings with zero / one / repeated snoozes;
- median and range of snooze count;
- intervention-depth distribution;
- confirmed outcome after snooze vs without snooze, when sample size permits.

Do not imply causation from small observational slices.

## Retention

WakeMyWay is not a social app. A participant can be retained without opening the app every day.

Define **retained at D7/D14** as:

> the participant intentionally continues using WakeMyWay for an eligible wake opportunity around that milestone, or explicitly states they intend to continue and had no wake opportunity in the window.

Report the number of participants with a meaningful wake opportunity in the denominator. Keep "no wake opportunity" separate from churn.

Also record voluntary behavior that signals trust:

- creates another alarm;
- keeps WakeMyWay as primary alarm;
- removes or retains a Safety Backup;
- disables Voice Check-In but keeps the core alarm;
- stops using WakeMyWay and why.

Do not optimize Safety Backup removal as a KPI.

## Weekly evidence snapshot

Use one table per cohort/week:

| Field | Evidence |
|---|---|
| Qualified participants enrolled | n |
| Participants with ≥1 expected wake | n |
| Expected wakes | n |
| Timely/diagnosable deliveries | n / expected |
| Missed/late wakes | n |
| Activation Completion | n / expected; n / delivered |
| Calibration coverage | n / eligible |
| Confirmed GOT_UP | n / determinate calibrated |
| Returned to bed after Activation Completion | n / determinate calibrated activation-complete |
| Median time to first engagement | value + sample n |
| Median time to Activation Completion | value + sample n |
| Snooze distribution | 0 / 1 / repeated |
| Fallback/recovery events | n + categories |
| Median annoyance | value + response n |
| Median agency | value + response n |
| D7 / D14 retained | n / eligible participants |
| Critical confusion/failure themes | concise grouped themes |

Attach distributions/failure cases when they matter. Do not hide bad mornings inside a cohort average.

## Failure and complaint log

Every material problem gets one classification:

~~~text
RELIABILITY_DEFECT
USABILITY_CONFUSION
ACCESSIBILITY_DEFECT
POLICY_TOO_EARLY_OR_LATE
DEVICE_OR_OEM_PATTERN
EXPECTED_LIMITATION
FEATURE_REQUEST
OTHER
~~~

For each entry retain:

~~~text
participant id
build/version
device/Android
date/wake occurrence
what the participant experienced
privacy-safe reliability evidence
reproduction status
severity
decision
linked GitHub issue/PR when applicable
~~~

A feature request is not automatically a beta action item.

## Beta change-control

During the cohort:

~~~text
reliability defect    → fix
usability confusion   → simplify / clarify
accessibility defect  → fix
bounded timing/policy issue with repeated evidence → tune and version
feature request       → defer by default
one-off preference    → observe
commercial objection  → record; do not manufacture a feature wall
~~~

When behavior changes materially, record the build/policy version and do not pool incompatible data as though nothing changed.

## Willingness-to-pay interview

Do this only after the participant has experienced multiple real WakeMyWay mornings, ideally at least five.

Ask in this order without selling the answer:

1. **If WakeMyWay disappeared tomorrow, what would you do instead?**
2. **What would you miss, if anything?**
3. **Which part changed your morning enough to matter?**
4. Present the current package hypothesis plainly:
   - Free: trustworthy WakeMyWay alarm that helps you begin moving.
   - Pro: wake strategy becomes more personal/adaptive over time, with the premium experiences supporting that outcome.
5. Present the current discovery price:
   - €4.99/month
   - ~€39/year
6. Ask for a concrete choice:
   - stay on Free;
   - choose monthly Pro;
   - choose annual Pro;
   - would not keep WakeMyWay;
   - unsure.
7. Ask **why that choice**, then what alternative they compare the price with.

Do not count "sounds fair" or "yes, I would pay" as equivalent to choosing a package.

Until a real billing path exists, label this evidence **stated package choice**, not purchase conversion. Actual checkout/trial behavior becomes stronger evidence later.

## End-of-cohort synthesis

Write the synthesis before building anything new.

Use four buckets:

### Keep

What repeatedly creates the desired outcome or trust.

### Change

Reliability, confusion, accessibility or bounded policy problems supported by evidence.

### Remove / simplify

Existing behavior that adds friction without enough outcome value.

### Defer

Requests/opportunities that are interesting but not required for 1.0.

Then answer explicitly:

~~~text
Do target users trust WakeMyWay enough to use it for real mornings?
Does it improve the intended real-world wake outcome enough to matter?
Do they continue voluntarily?
Is annoyance acceptable and agency preserved?
Is the adaptive/conversational layer useful beyond novelty?
What package/price choices did experienced users actually make?
What device/reliability envelope is supported by evidence?
What would block a paid staged rollout without adding a new feature?
~~~

## Gate exit

PROVE #87 is ready for a paid-launch decision only when:

- 10–20 qualified participants have meaningful real-morning exposure;
- reliability and outcome evidence have explicit denominators;
- D7/D14 are interpretable for the cohort;
- important failure/confusion patterns are fixed or explicitly accepted;
- willingness-to-pay evidence is tied to a concrete package and price;
- the conclusion can be defended without using app opens, AI novelty, or internal Activation Completion as a substitute for the real wake outcome.

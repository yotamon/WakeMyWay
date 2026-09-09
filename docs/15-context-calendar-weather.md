# Context: calendar and weather

## Principle

Context improves the wake strategy only when it helps answer:

- Why does the user need to get up?
- How much urgency is real?
- What is the first useful orientation after they are active?

Context is not a reason to build a morning dashboard.

## Calendar

For Android V1, prefer Android Calendar Provider over immediate Google Calendar OAuth.

Request read-only calendar access only if the user opts in.

Read a narrow window such as:

```text
now -> next 12 hours
```

Collect only what is needed:

```text
title
start
end
optional location
```

Avoid by default:

- event descriptions
- attendee lists
- attachments
- broad historical calendar ingestion

## Context ranking

Do not recite a list of all events.

Rank locally/server-side into relevant facts, for example:

```text
first important event: Interview at 10:00
free time before event: ~2h
```

Morning speech:

> "Your interview is at ten. Nothing else before then."

Not:

> "You have seven events today. The first is..."

## Weather

Weather is useful only if it changes morning action.

Examples:

- rain means leave earlier / take umbrella
- extreme temperature changes clothing/commute

Avoid reading generic forecast trivia every morning.

## Location

No background location in V1.

Options:

- user-selected city
- one-time/foreground approximate location

Weather does not justify continuous location access.

## Provider

Open-Meteo was considered for prototype use. Its commercial/data/licensing terms must be reviewed before production, especially if the commercial API is required.

Choose the first weather provider directly when M9 arrives. Add a provider seam only if a second implementation, testing need, licensing volatility, or operational failure domain makes substitution valuable; do not create an adapter solely for hypothetical portability.

## Privacy

Calendar titles and Tomorrow Contract content must not be sent to analytics/crash tools.

Only the minimal facts required for plan generation should leave the device when cloud generation is used.

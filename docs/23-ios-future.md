# Future iOS strategy

## Decision

Do not build iOS now.

Android gives immediate dogfood and lets us solve the hardest alarm/reliability behavior with direct platform control. Future iOS readiness means preserving **product semantics and tested behavior**, not forcing Android through hypothetical shared interfaces.

## What we preserve today

Portable, canonical concepts from [`../CONTEXT.md`](../CONTEXT.md):

- Wake Schedule
- Wake Occurrence
- Snooze Occurrence
- Wake Session
- Wake Phase
- Wake Input
- Wake Directive
- Wake Policy
- Wake Outcome
- Tomorrow Contract
- Prepared Wake Plan
- Character / Speech Intent

Also preserve:

- behavior/invariant test scenarios
- privacy rules
- reliability principles
- OpenAPI contract once a backend exists
- design-system semantics

## What remains platform-native

Android and future iOS independently own:

- alarm scheduling primitives
- lock-screen/system presentation
- audio routing/session behavior
- microphone lifecycle
- motion APIs
- permissions
- notification system
- background/direct-boot-equivalent lifecycle constraints

Do not invent `AlarmScheduler`, `MotionProvider`, or similar cross-platform interfaces in Android solely because iOS may exist later. Create seams when Android itself earns them; port/map the product contract later.

## Future shape

```text
                  optional Wake backend
                         │
                      OpenAPI
                    /         \
              Android          iOS
                 │              │
         native Alarm Kernel   native alarm layer
                 │              │
         Wake behavior model   equivalent behavior model
```

## KMP remains deferred

When iOS is justified, measure the Android implementation.

Consider KMP if:

- substantial pure-Kotlin Wake Runtime/recurrence/policy behavior exists
- shared tests materially reduce divergence
- iOS integration/tooling cost is acceptable

Prefer a native Swift port if:

- portable domain remains modest
- clarity/native integration outweighs code reuse
- most optional intelligence sits behind network contracts

Do not pay KMP/tooling cost before the amount of reusable behavior is known.

## Product parity

Equivalent outcome/philosophy matters more than identical screens or system behavior. Platform constraints may make the first wake interaction structurally different.

Never weaken Android's native reliability to preserve symmetry with a hypothetical iOS implementation.

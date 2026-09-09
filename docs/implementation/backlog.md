# Initial engineering backlog

This backlog is ordered to expose reliability and product risk early. Convert items into GitHub issues when implementation begins; do not create speculative implementation issues for later milestones until the preceding evidence exists.

## M0 — Foundation

1. Bootstrap Android project
2. Configure Gradle version catalog
3. Create physical modules `:app`, `:wake-core`, `:benchmark`
4. Add Kotlin, Compose, Material primitives, Coroutines/Flow
5. Configure Android Lint + formatting
6. Configure unit-test baseline and CI
7. Add architecture check that `:wake-core` has no Android/framework dependencies
8. Define typed ID/value-object conventions
9. Define `WallClock` and monotonic-time test seams
10. Define `WakeSchedule`
11. Define `WakeOccurrence`
12. Enforce one-active-Wake-Schedule V1 invariant
13. Implement recurrence / next-occurrence calculation
14. Define timezone/DST resolution policy
15. Add DST/property test suite
16. Record actual SDK/toolchain choices in project status/ADR if they differ from docs

## M1 — Deep Alarm Kernel

17. Add Room for normal credential-protected wake state
18. Define Alarm Kernel caller contract and invariants
19. Implement Wake Schedule → next Wake Occurrence commit internally
20. Define versioned Critical Wake Snapshot format
21. Implement device-protected Critical Wake Snapshot store with integrity/checksum
22. Ensure Critical Wake Snapshot contains no private contextual content
23. Implement `AlarmManager.setAlarmClock()` scheduling internally
24. Define immutable/appropriately-scoped `PendingIntent` identities
25. Implement exact-alarm capability/readiness evaluation
26. Implement alarm receiver as a thin edge
27. Create alarm notification channel/presentation
28. Implement dedicated `WakeActivity` shell
29. Add bundled emergency alarm asset
30. Implement critical local alarm audio controller
31. Implement intentional stop/cancel flow
32. Implement snooze replacement as a new exact Wake Occurrence
33. Guarantee snooze replacement is durable before current session finishes
34. Implement Alarm Kernel reconciliation after schedule edit/cancel
35. Handle wall-clock/timezone changes
36. Handle normal boot/app-start reconciliation
37. Build Wake Ready/readiness UI
38. Add local diagnostic timeline for critical alarm operations
39. Run first locked-device end-to-end wake test

## M2 — Reliability Harness / Direct Boot

40. Build Alarm Lab T+30s/T+2m helpers
41. Add repeated automated alarm-cycle runner
42. Measure trigger → first audible output
43. Measure trigger → usable wake controls/presentation
44. Test process death
45. Test Doze/idle
46. Test locked-device presentation
47. Test device-already-unlocked presentation
48. Test notification permission denied
49. Test full-screen-intent unavailable/revoked
50. Implement Direct-Boot-aware critical component(s)
51. Handle `ACTION_LOCKED_BOOT_COMPLETED`
52. Verify pre-first-unlock alarm works from device-protected snapshot
53. Verify private Tomorrow Contract/calendar/generated speech is unavailable pre-unlock
54. Test reboot after normal unlock
55. Test duplicate trigger/idempotent handling
56. Test missing/corrupt normal DB while critical snapshot remains usable
57. Test explicit Android 15+ Force Stop expected limitation
58. Detect/reconcile post-force-stop state on next launch where supported
59. Establish measured percentile reliability targets from test data
60. Add Android 17/API-37 audio compatibility case when applicable

## M3 — Wake Runtime

61. Define canonical Wake phases: Alerting, Engaging, Activating, Orienting, Finished
62. Define typed `WakeInput`
63. Define typed `WakeDirective`
64. Define versioned `WakePolicy`
65. Implement deterministic Wake Runtime/reducer
66. Keep activation evidence internal to Wake Runtime
67. Keep escalation/intervention selection internal to Wake Runtime
68. Add invariants/property tests
69. Implement Android directive executor in `:app`
70. Implement typed Wake Session timeline persistence
71. Implement replay/debug view from recorded inputs/directives
72. Connect `WakeActivity` to Wake Runtime

## M4 — Motion evidence

73. Implement Android motion observation source
74. Implement acceleration/orientation feature extraction
75. Implement pickup detection
76. Implement meaningful/sustained movement detection
77. Implement stationary-period detection
78. Convert platform observations to typed Wake Inputs
79. Ensure no raw high-frequency sensor stream is retained by default
80. Add motion scenarios to Wake Lab

## M5 — Alfred local experience

81. Define stable `SpeechIntent` vocabulary
82. Define Alfred character boundaries/voice direction
83. Write curated offline phrase variants per intent
84. Implement local/seedable phrase rendering
85. Add character safety tests
86. Implement local speech/audio playback path

## M6 — Tomorrow Contract / preparation

87. Build Tomorrow Contract text-first UI
88. Define local private Tomorrow Contract model
89. Define/version Prepared Wake Plan
90. Add deferrable WorkManager preparation
91. Store prepared private content in credential-protected storage
92. Add prepared audio/content integrity + fallback behavior
93. Prove wake still works when prepared content is absent

## M7 — Voice architecture spike

94. Prototype direct OpenAI realtime in isolation
95. Measure cold connection / first response / barge-in / reconnect
96. Test Wi-Fi ↔ mobile and Bluetooth route behavior
97. Prototype LiveKit only if it may hide meaningful RTC/session complexity
98. Evaluate ElevenLabs role for character quality/cost/licensing
99. Record measured voice architecture decision in ADR-008

## M8+ — create only after earlier evidence

100. Integrate selected realtime implementation without changing Wake Runtime authority
101. Add voice budget/fallback controls
102. Add optional calendar context
103. Add useful weather context
104. Derive Wake Outcomes/profile policy updates
105. Add sanitized crash/product observability when dogfood needs it
106. Build history/insight UI
107. Accessibility audit
108. OEM real-device matrix
109. Closed Play testing readiness

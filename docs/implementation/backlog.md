# Initial engineering backlog

This backlog is ordered to expose reliability and product risk early. Convert items into GitHub issues when implementation begins; do not create speculative implementation issues for later milestones until the preceding evidence exists.

## M0 — Foundation

1. Bootstrap Android project
2. Configure Gradle version catalog
3. Create physical modules `:app`, `:wake-core`, `:benchmark`
4. Add Kotlin, Compose, Material primitives, Coroutines/Flow
5. Set `targetSdk = 36` baseline and current stable `compileSdk >= 36`
6. Select `minSdk` deliberately from supported-device/reliability needs
7. Configure Android Lint + formatting
8. Configure unit-test baseline and CI
9. Add architecture check that `:wake-core` has no Android/framework dependencies
10. Define typed ID/value-object conventions
11. Define `WallClock` and monotonic-time test seams
12. Define `WakeSchedule`
13. Define `WakeOccurrence`
14. Enforce one-active-Wake-Schedule V1 invariant
15. Implement recurrence / next-occurrence calculation
16. Define timezone/DST resolution policy
17. Add DST/property test suite
18. Revalidate current Google Play `USE_EXACT_ALARM` eligibility/restricted-permission policy
19. Record actual SDK/toolchain/permission choices if they differ from current docs

## M1 — Deep Alarm Kernel + Active Wake Execution

20. Add Room for normal credential-protected wake state
21. Define Alarm Kernel caller contract and invariants
22. Implement Wake Schedule → next Wake Occurrence commit internally
23. Define versioned Critical Wake Snapshot format
24. Implement device-protected Critical Wake Snapshot store with integrity/checksum
25. Ensure Critical Wake Snapshot contains no private contextual content
26. Implement `AlarmManager.setAlarmClock()` scheduling internally
27. Implement preferred `USE_EXACT_ALARM` manifest/capability path after current-policy revalidation
28. Define immutable/appropriately-scoped `PendingIntent` identities
29. Implement exact-alarm capability/readiness evaluation
30. Implement alarm receiver as a thin edge
31. Create alarm notification channel/presentation
32. Add bundled emergency alarm asset
33. Define Active Wake Execution state/identity semantics
34. Implement foreground alarm playback service/controller with `USAGE_ALARM` semantics per ADR-014
35. Ensure critical playback lifetime is independent from `WakeActivity`
36. Implement exactly-one-active-playback/idempotent attach/start behavior
37. Implement dedicated `WakeActivity` shell that attaches to active occurrence
38. Implement intentional Stop as an accessible/idempotent terminal action
39. Ensure stopped occurrence cannot resurrect after component recreation/stale trigger
40. Implement snooze replacement as a new exact Wake Occurrence
41. Guarantee snooze replacement is durable before old active execution terminates
42. Ensure durably snoozed/replaced occurrence cannot resurrect
43. Implement Alarm Kernel reconciliation after schedule edit/cancel
44. Handle wall-clock/timezone changes
45. Handle normal boot/app-start reconciliation
46. Build Wake Ready/readiness UI including active-execution-path readiness
47. Add local diagnostic timeline for critical schedule/active-execution operations
48. Measure whether any wake-lock/power primitive is actually needed; keep it absent unless evidence requires it
49. Run first locked-device end-to-end wake test

## M2 — Reliability Harness / Direct Boot / active recovery

50. Build Alarm Lab T+30s/T+2m helpers
51. Add repeated automated alarm-cycle runner
52. Measure trigger → Active Wake Execution start
53. Measure trigger → first audible output
54. Measure trigger → usable wake controls/presentation
55. Test process death before trigger
56. Test `WakeActivity` destroy/recreate while alarm sounds
57. Test playback service/controller recreation while alarm sounds
58. Test controlled process kill/recreation while active alarm sounds where Android permits
59. Verify recreation produces no duplicate overlapping critical audio
60. Verify Stop then recreation does not resurrect active occurrence
61. Verify Snooze then recreation/process kill leaves only replacement occurrence authoritative
62. Verify stale trigger identity cannot revive older occurrence
63. Verify terminal paths release foreground/audio/power resources
64. Test Doze/idle
65. Test locked-device presentation
66. Test device-already-unlocked presentation
67. Test notification permission denied
68. Test full-screen-intent unavailable/revoked
69. Implement Direct-Boot-aware critical component(s)
70. Handle `ACTION_LOCKED_BOOT_COMPLETED`
71. Verify pre-first-unlock alarm works from device-protected snapshot
72. Verify private Tomorrow Contract/calendar/generated speech is unavailable pre-unlock
73. Test reboot after normal unlock
74. Test duplicate trigger/idempotent handling
75. Test missing/corrupt normal DB while critical snapshot remains usable
76. Test explicit Android 15+ Force Stop expected limitation
77. Detect/reconcile post-force-stop state on next launch where supported
78. Clear/repair stale active-execution state after stopped-state recovery
79. Establish measured percentile reliability targets from test data
80. Add Android 17/API-37 background-audio compatibility tests (`USAGE_ALARM`, foreground execution, focus/volume)

## M3 — Wake Runtime

81. Define canonical Wake phases: Alerting, Engaging, Activating, Orienting, Finished
82. Define typed `WakeInput`
83. Define typed `WakeDirective`
84. Define versioned `WakePolicy`
85. Implement deterministic Wake Runtime/reducer
86. Keep activation evidence internal to Wake Runtime
87. Keep escalation/intervention selection internal to Wake Runtime
88. Define **Activation Completion** as runtime operational outcome
89. Keep Confirmed Wake Success outside direct runtime authority
90. Add invariants/property tests
91. Implement Android directive executor in `:app`
92. Implement typed Wake Session timeline persistence
93. Implement replay/debug view from recorded inputs/directives
94. Connect `WakeActivity` to Wake Runtime while Alarm Kernel remains active-playback owner

## M4 — Motion evidence

95. Implement Android motion observation source
96. Implement acceleration/orientation feature extraction
97. Implement pickup detection
98. Implement meaningful/sustained movement detection
99. Implement stationary-period detection
100. Convert platform observations to typed Wake Inputs
101. Ensure no raw high-frequency sensor stream is retained by default
102. Add motion scenarios to Wake Lab

## M5 — Alfred local experience

103. Define stable `SpeechIntent` vocabulary
104. Define Alfred character boundaries/voice direction
105. Write curated offline phrase variants per intent
106. Implement local/seedable phrase rendering
107. Add character safety tests
108. Implement local speech/audio rendering without taking ownership of critical alarm playback lifecycle

## M6 — Tomorrow Contract / preparation

109. Build Tomorrow Contract text-first UI
110. Define local private Tomorrow Contract model
111. Define/version Prepared Wake Plan
112. Add deferrable WorkManager preparation
113. Store prepared private content in credential-protected storage
114. Add prepared audio/content integrity + fallback behavior
115. Prove wake still works when prepared content is absent
116. Prototype optional dogfood-only Safety Backup setup
117. Ensure Safety Backup is not modeled as a second adaptive Wake Schedule
118. Track WMW reliability independently from whether Safety Backup later fires

## M7 — Wake Learning v0

119. Derive compact Wake Outcomes from typed timelines
120. Add optional semantic calibration model: `GOT_UP`, `RETURNED_TO_BED`, `GOT_UP_LATER`, `SKIPPED`
121. Keep Activation Completion distinct from Confirmed Wake Success in storage/analysis
122. Define first bounded learning parameter surface (small set only)
123. Define algorithm version and versioned derived policy/profile snapshot
124. Implement deterministic local Wake Learning transformation
125. Add minimum-evidence/hysteresis rule before changing policy
126. Bound parameter step sizes and safe ranges
127. Include annoyance/agency and calibration evidence in adaptation decisions
128. Add human-readable debug reason for each meaningful policy change
129. Guarantee one immutable policy version per Wake Session
130. Add learned-policy validation and fallback to safe default
131. Add user/developer reset to default policy
132. Add deterministic fixture/property tests
133. Test false-positive case: Activation Completion + `RETURNED_TO_BED`
134. Test missing calibration does not become automatic success
135. Prove one bounded explainable policy adaptation from dogfood/fixture outcomes

## M8 — Voice architecture spike

136. Prototype direct OpenAI realtime in isolation
137. Measure cold connection / first response / barge-in / reconnect
138. Test Wi-Fi ↔ mobile and Bluetooth route behavior
139. Prototype LiveKit only if it may hide meaningful RTC/session complexity
140. Evaluate Vercel WebSocket gateway as a measured candidate, not default
141. Evaluate ElevenLabs role for character quality/cost/licensing
142. Record measured voice architecture decision in ADR-008

## M9 — Realtime conversation

143. Integrate selected realtime implementation without changing Wake Runtime authority
144. Add voice budget/fallback controls
145. Ensure realtime failure cannot invalidate local learned policy or Active Wake Execution
146. Derive semantic inputs without raw transcript persistence by default

## M10 — Useful context

147. Add optional read-only calendar context
148. Add useful weather context
149. Keep context relevance narrow and privacy-minimized

## M11 — Dogfood hardening

150. Add sanitized crash/product observability when dogfood needs it
151. Capture active-alarm lifecycle/recovery reliability
152. Capture Activation Completion + Confirmed Wake Success calibration coverage
153. Capture adaptation effectiveness/friction and policy changes
154. Evaluate whether Safety Backup remains necessary
155. Build history/insight UI without misleading uncalibrated success percentages
156. Accessibility audit
157. OEM real-device matrix
158. Move regular dogfood through Google Play Internal Testing

## M12 — Closed beta readiness

159. Revalidate target API / `USE_EXACT_ALARM` / FSI / notification declarations under current Play policy
160. Privacy policy / processor list / deletion-export direction
161. Support diagnostics
162. Provider cost limits / kill switches
163. Trademark/domain finalization
164. Closed Play testing readiness

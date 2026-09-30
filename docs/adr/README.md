# Architecture Decision Records

ADRs preserve durable engineering constraints. Canonical terminology lives in [`../../CONTEXT.md`](../../CONTEXT.md).

| ADR | Decision |
|---|---|
| [`001`](001-android-first-native.md) | Android-first native implementation; portability via concepts/contracts, not speculative iOS interfaces |
| [`002`](002-alarm-kernel-local-first.md) | Local-first Alarm Kernel authority |
| [`003`](003-pure-kotlin-wake-runtime.md) | Deep pure-Kotlin deterministic Wake Runtime |
| [`004`](004-no-kmp-yet.md) | No KMP until iOS is justified |
| [`005`](005-ai-is-not-state-authority.md) | AI is not behavioral/state authority |
| [`006`](006-openapi-contract.md) | OpenAPI as service contract once backend exists |
| [`007`](007-account-optional.md) | No account required for initial alarm use |
| [`008`](008-voice-provider-spike.md) | Realtime voice architecture selected by measured M8 spike, after local Wake Learning v0 |
| [`009`](009-deep-alarm-kernel.md) | Alarm Kernel is a deep module |
| [`010`](010-minimal-physical-module-topology.md) | Minimal initial Gradle module topology |
| [`011`](011-direct-boot-critical-state.md) | Direct Boot uses minimal non-sensitive critical state |
| [`012`](012-vercel-non-critical-cloud.md) | Vercel preferred for initial non-critical cloud/web deployment; realtime remains M8 spike-gated |
| [`013`](013-supabase-managed-data-platform.md) | Neon for managed PostgreSQL and Auth, conditional Storage; mobile domain stays behind Wake API |
| [`014`](014-active-wake-execution-lifecycle.md) | Critical alarm playback/recovery outlives `WakeActivity`; Alarm Kernel owns Active Wake Execution |
| [`015`](015-alarm-permissions-and-execution.md) | Exact alarm, full-screen presentation, Direct Boot reconciliation, and foreground alarm playback baseline |
| [`016`](016-vercel-ai-platform.md) | Vercel AI SDK + AI Gateway are the default optional cloud AI layer; realtime transport remains M8 spike-gated |
| [`017`](017-local-production-voice-wake.md) | Historical local TTS/STT baseline; consumer fallback is superseded by Realtime-or-alarm-only while service-owned audio safety remains |
| [`018`](018-controllable-wake-presentation-readiness.md) | Wake Ready requires controllable full-screen/notification presentation and modern Android BAL-safe launch |
| [`019`](019-permission-gated-wake-scheduling.md) | Wake schedules require critical alarm/presentation readiness; voice is degradable enrichment and never scheduling authority |
| [`020`](020-conversational-wake-enrichment.md) | Natural Realtime speech is optional non-authoritative Wake enrichment; failure degrades alarm-only |
| [`021`](021-founder-realtime-pairing.md) | **Superseded by ADR 028:** historical founder access-code pairing hardening |
| [`022`](022-multi-alarm-product-model.md) | Multi-alarm product state and critical execution slots are independent per alarm |
| [`023`](023-app-update-distribution.md) | Direct distribution uses WakeMyWay-controlled signed app updates |
| [`024`](024-update-persistence-contract.md) | App updates preserve local alarm/product state across signed upgrades |
| [`025`](025-direct-openai-realtime-dogfood.md) | OpenAI Realtime is promoted into Direct dogfood only, with local authority and bounded fallback |
| [`026`](026-zero-setup-direct-realtime.md) | **Superseded:** anonymous zero-setup bootstrap had insufficient authorization |
| [`028`](028-account-authenticated-realtime.md) | Direct Realtime provisions invisibly from an authenticated account; no consumer access code/setup page |

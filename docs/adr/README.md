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
| [`013`](013-supabase-managed-data-platform.md) | Supabase preferred for managed PostgreSQL, later Auth, and conditional Storage; mobile domain stays behind Wake API |
| [`014`](014-active-wake-execution-lifecycle.md) | Critical alarm playback/recovery outlives `WakeActivity`; Alarm Kernel owns Active Wake Execution |
| [`015`](015-alarm-permissions-and-execution.md) | Exact alarm, full-screen presentation, Direct Boot reconciliation, and foreground alarm playback baseline |
| [`016`](016-vercel-ai-platform.md) | Vercel AI SDK + AI Gateway are the default optional cloud AI layer; realtime transport remains M8 spike-gated |
| [`017`](017-local-production-voice-wake.md) | Production wake uses local TTS + on-device voice replies behind deterministic Wake Runtime, with service-owned fail-safe alarm ducking |
| [`018`](018-controllable-wake-presentation-readiness.md) | Wake Ready requires controllable full-screen/notification presentation and modern Android BAL-safe launch |
| [`019`](019-permission-gated-wake-scheduling.md) | Voice Wake schedules are committed only after required alarm and voice permissions are ready, with fire/restart safety rechecks |
| [`020`](020-founder-realtime-pairing.md) | Founder Realtime uses fixed WakeMyWay backend routing plus scoped, expiring installation pairing; reusable server/OpenAI keys never enter Android |

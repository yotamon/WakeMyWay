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

package com.wakemyway.app.alarm

/** Session-second metrics are deliberately distinct from millisecond latency. */
internal object WakeSessionDurationSummary {
    fun describe(label: String, durations: List<Int?>): String {
        val valid = durations.filterNotNull().filter { it >= 0 }.sorted()
        if (valid.isEmpty()) return "$label: n=0"
        fun rank(percent: Int): Int = valid[
            ((valid.size * percent + 99) / 100 - 1).coerceIn(0, valid.lastIndex)
        ]
        return "$label: n=${valid.size}; p50=${rank(50)}s; p95=${rank(95)}s"
    }
}

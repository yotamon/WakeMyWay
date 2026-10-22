package com.wakemyway.app.alarm

/** Bounded, local-only performance summaries; never process transcripts. */
internal object WakeLatencySummary {
    fun describe(label: String, values: List<Long?>): String {
        val samples = values.filterNotNull().filter { it >= 0L }.sorted()
        if (samples.isEmpty()) return label + ": n=0"
        fun nearestRank(percent: Int): Long {
            val index = ((samples.size * percent + 99) / 100 - 1).coerceIn(0, samples.lastIndex)
            return samples[index]
        }
        return label + ": n=" + samples.size +
            "; p50=" + nearestRank(50) + "ms; p95=" + nearestRank(95) + "ms"
    }
}

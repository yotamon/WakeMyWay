package com.wakemyway.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeLatencySummaryTest {
    @Test fun sparseOrInvalidSamplesAreNotPretendedToBeSuccess() {
        assertEquals("voice: n=0", WakeLatencySummary.describe("voice", listOf(null, -1L)))
    }

    @Test fun nearestRankIncludesCountAndRealBounds() {
        assertEquals("native: n=7; p50=100ms; p95=400ms",
            WakeLatencySummary.describe("native",
                listOf(50L, 25L, 300L, 100L, null, 200L, 75L, 400L)))
    }

    @Test fun oneSampleDoesNotClaimPopulationPrecision() {
        assertEquals("start: n=1; p50=22ms; p95=22ms",
            WakeLatencySummary.describe("start", listOf(22L)))
    }
}

package com.wakemyway.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeSessionDurationSummaryTest {
    @Test fun unknownUsageIsNotMisreportedAsFree() {
        assertEquals("live: n=0", WakeSessionDurationSummary.describe("live", listOf(null)))
    }

    @Test fun providerSessionSecondsHaveTheirOwnUnits() {
        assertEquals("live: n=4; p50=30s; p95=180s",
            WakeSessionDurationSummary.describe("live", listOf(180, 10, null, 30, 90)))
    }
}

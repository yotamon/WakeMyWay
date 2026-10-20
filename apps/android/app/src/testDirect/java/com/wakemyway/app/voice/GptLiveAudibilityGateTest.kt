package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveAudibilityGateTest {
    @Test fun noRatingFromTranscriptOrConnectionAlone() {
        assertFalse(GptLiveAudibilityGate.canRate(
            previewConnected = true, sessionClosed = true, userConfirmedAudible = false,
        ))
    }

    @Test fun noReportBeforeConnectionOrWhileSessionActive() {
        assertFalse(GptLiveAudibilityGate.canReport(previewConnected = false, sessionClosed = true))
        assertFalse(GptLiveAudibilityGate.canReport(previewConnected = true, sessionClosed = false))
        assertFalse(GptLiveAudibilityGate.canRate(
            previewConnected = true, sessionClosed = false, userConfirmedAudible = true,
        ))
    }

    @Test fun directListenerConfirmationUnlocksReviewOnlyAfterSessionClosed() {
        assertTrue(GptLiveAudibilityGate.canRate(
            previewConnected = true, sessionClosed = true, userConfirmedAudible = true,
        ))
    }
}

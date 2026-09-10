package com.wakemyway.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSpikeMeasurementSessionTest {
    @Test
    fun startsEmptyAndUsesExplicitMeasurementIdentity() {
        val snapshot = VoiceSpikeMeasurementSession().snapshot()

        assertEquals("m8-direct-openai-manual-audible-v1", snapshot.protocolId)
        assertEquals("operator-tap-upper-bound-v1", snapshot.firstSpeechObservationMethod)
        assertNull(snapshot.coldConnectionMs)
        assertNull(snapshot.firstSpeechMs)
        assertFalse(snapshot.responseProbePending)
    }

    @Test
    fun derivesFirstSpeechFromExplicitResponseRequestToAudibleObservation() {
        val session = VoiceSpikeMeasurementSession()
        session.recordColdConnection(412L)
        session.recordSyntheticResponseRequested(10_000L)

        assertTrue(session.snapshot().responseProbePending)
        assertEquals(638L, session.recordFirstAudibleObserved(10_638L))

        val snapshot = session.snapshot()
        assertEquals(412L, snapshot.coldConnectionMs)
        assertEquals(638L, snapshot.firstSpeechMs)
        assertFalse(snapshot.responseProbePending)
    }

    @Test
    fun newSyntheticResponseInvalidatesThePreviousAudibleObservation() {
        val session = VoiceSpikeMeasurementSession()
        session.recordSyntheticResponseRequested(100L)
        session.recordFirstAudibleObserved(220L)
        session.recordSyntheticResponseRequested(1_000L)

        val snapshot = session.snapshot()
        assertNull(snapshot.firstSpeechMs)
        assertTrue(snapshot.responseProbePending)
    }

    @Test
    fun resetClearsOnlyTimingMetadata() {
        val session = VoiceSpikeMeasurementSession()
        session.recordColdConnection(300L)
        session.recordSyntheticResponseRequested(1_000L)
        session.recordFirstAudibleObserved(1_400L)

        session.reset()

        val snapshot = session.snapshot()
        assertNull(snapshot.coldConnectionMs)
        assertNull(snapshot.firstSpeechMs)
        assertFalse(snapshot.responseProbePending)
        assertEquals("m8-direct-openai-manual-audible-v1", snapshot.protocolId)
    }

    @Test
    fun rejectsInvalidTemporalOrderingAndDuplicateObservation() {
        val session = VoiceSpikeMeasurementSession()

        assertFails { session.recordColdConnection(-1L) }
        assertFails { session.recordFirstAudibleObserved(10L) }

        session.recordSyntheticResponseRequested(100L)
        assertFails { session.recordFirstAudibleObserved(99L) }
        session.recordFirstAudibleObserved(110L)
        assertFails { session.recordFirstAudibleObserved(120L) }
    }

    private fun assertFails(block: () -> Unit) {
        var failed = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            failed = true
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue("Expected operation to fail", failed)
    }
}

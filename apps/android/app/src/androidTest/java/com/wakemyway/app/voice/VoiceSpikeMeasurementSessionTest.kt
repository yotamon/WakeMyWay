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
        session.recordColdConnection(412)
        session.recordSyntheticResponseRequested(10_000)

        assertTrue(session.snapshot().responseProbePending)
        assertEquals(638, session.recordFirstAudibleObserved(10_638))

        val snapshot = session.snapshot()
        assertEquals(412, snapshot.coldConnectionMs)
        assertEquals(638, snapshot.firstSpeechMs)
        assertFalse(snapshot.responseProbePending)
    }

    @Test
    fun newSyntheticResponseInvalidatesThePreviousAudibleObservation() {
        val session = VoiceSpikeMeasurementSession()
        session.recordSyntheticResponseRequested(100)
        session.recordFirstAudibleObserved(220)
        session.recordSyntheticResponseRequested(1_000)

        val snapshot = session.snapshot()
        assertNull(snapshot.firstSpeechMs)
        assertTrue(snapshot.responseProbePending)
    }

    @Test
    fun resetClearsOnlyTimingMetadata() {
        val session = VoiceSpikeMeasurementSession()
        session.recordColdConnection(300)
        session.recordSyntheticResponseRequested(1_000)
        session.recordFirstAudibleObserved(1_400)

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

        assertFails { session.recordColdConnection(-1) }
        assertFails { session.recordFirstAudibleObserved(10) }

        session.recordSyntheticResponseRequested(100)
        assertFails { session.recordFirstAudibleObserved(99) }
        session.recordFirstAudibleObserved(110)
        assertFails { session.recordFirstAudibleObserved(120) }
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

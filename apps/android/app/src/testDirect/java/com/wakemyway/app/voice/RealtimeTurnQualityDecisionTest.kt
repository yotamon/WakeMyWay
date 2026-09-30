package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeTurnQualityDecisionTest {
    @Test
    fun `usable decision survives harmless whitespace and case`() {
        assertTrue(RealtimeTurnQualityDecision.fromModelOutput("USABLE"))
        assertTrue(RealtimeTurnQualityDecision.fromModelOutput("  usable\n"))
    }

    @Test
    fun `unusable and malformed output fail closed`() {
        assertFalse(RealtimeTurnQualityDecision.fromModelOutput("UNUSABLE"))
        assertFalse(RealtimeTurnQualityDecision.fromModelOutput(""))
        assertFalse(RealtimeTurnQualityDecision.fromModelOutput("USABLE."))
        assertFalse(RealtimeTurnQualityDecision.fromModelOutput("The answer is USABLE"))
        assertFalse(RealtimeTurnQualityDecision.fromModelOutput("UNUSABLE because it was a groan"))
    }
}

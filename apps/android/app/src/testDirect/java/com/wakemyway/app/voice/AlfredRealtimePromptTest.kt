package com.wakemyway.app.voice

import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.runtime.SpeechIntent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlfredRealtimePromptTest {
    @Test
    fun `session prompt locks one stable Alfred identity`() {
        val prompt = AlfredRealtimePrompt.SYSTEM

        assertTrue(prompt.contains("SAME PERSON"))
        assertTrue(prompt.contains("Do not reset, re-cast or reinterpret your personality"))
        assertTrue(prompt.contains("not customer-service assistant"))
        assertTrue(prompt.contains("dry wit"))
        assertTrue(prompt.contains("same accent"))
    }

    @Test
    fun `session prompt explicitly rejects generic assistant mannerisms`() {
        val prompt = AlfredRealtimePrompt.SYSTEM.lowercase()

        listOf(
            "absolutely",
            "of course",
            "i'm here to help",
            "great job",
            "you've got this",
            "i'd be happy to",
        ).forEach { phrase ->
            assertTrue(prompt.contains(phrase))
        }
        assertTrue(prompt.contains("do not praise routine compliance"))
    }

    @Test
    fun `per turn prompt changes task without replacing persona`() {
        VoiceStyle.entries.forEach { style ->
            val prompt = AlfredRealtimePrompt.turn(SpeechIntent.AskToMove, style)

            assertTrue(prompt.contains("Identity lock"))
            assertTrue(prompt.contains("Remain the exact Alfred"))
            assertTrue(prompt.contains("Do not adopt a new persona"))
            assertTrue(prompt.contains("Current Wake Runtime directive"))
            assertTrue(prompt.contains("feet on the floor"))
            assertFalse(prompt.contains("You are Wake My Way's morning wake companion"))
        }
    }

    @Test
    fun `early wake protocol keeps cognitive load low and physical progression explicit`() {
        val system = AlfredRealtimePrompt.SYSTEM.lowercase()

        assertTrue(system.contains("cognition is temporarily reduced"))
        assertTrue(system.contains("never ask open-ended questions"))
        assertTrue(system.contains("sit upright -> feet down -> brief upper-body movement -> stand only if safe"))
        assertTrue(system.contains("never ask the user to prove wakefulness with arithmetic"))
    }

    @Test
    fun `physiological intents stay bounded and safe`() {
        val upperBody = AlfredRealtimePrompt.turn(
            SpeechIntent.ActivateUpperBody,
            VoiceStyle.DEFAULT,
        )
        val standing = AlfredRealtimePrompt.turn(
            SpeechIntent.StandIfSafe,
            VoiceStyle.DEFAULT,
        )
        val reengage = AlfredRealtimePrompt.turn(
            SpeechIntent.ReEngage(2),
            VoiceStyle.DEFAULT,
        )

        assertTrue(upperBody.contains("two slow shoulder rolls"))
        assertTrue(upperBody.contains("Do not add breathing drills"))
        assertTrue(standing.contains("only if standing is safe"))
        assertTrue(standing.contains("seated alternative"))
        assertTrue(reengage.contains("do not introduce a new or harder action"))
    }

    @Test
    fun `motivational style stays Alfred instead of becoming a coach`() {
        val prompt = AlfredRealtimePrompt.turn(
            SpeechIntent.KeepEngaging,
            VoiceStyle.MOTIVATIONAL,
        )

        assertTrue(prompt.contains("Stay the same Alfred"))
        assertTrue(prompt.contains("Do not become a coach, cheerleader or enthusiastic assistant"))
    }
}

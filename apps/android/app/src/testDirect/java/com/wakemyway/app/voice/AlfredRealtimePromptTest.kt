package com.wakemyway.app.voice

import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.personalization.ConversationAmount
import com.wakemyway.core.personalization.HumorPreference
import com.wakemyway.core.personalization.InterventionStyle
import com.wakemyway.core.personalization.MorningBarrier
import com.wakemyway.core.personalization.WakeAllowedContext
import com.wakemyway.core.personalization.WakePreferences
import com.wakemyway.core.personalization.WakeSessionStrategyResolver
import com.wakemyway.core.runtime.SpeechIntent
import com.wakemyway.core.runtime.WakePolicy
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
        assertTrue(prompt.contains("not scripts or universal next steps"))
        assertTrue(prompt.contains("Runtime directive: HoldEngagement"))
        assertTrue(prompt.contains("I'm already standing"))
        assertTrue(prompt.contains("# Instruction priority"))
        assertTrue(prompt.contains("current Wake Runtime directive controls"))
        assertTrue(prompt.contains("preferences modify presentation only"))
    }

    @Test
    fun `explicit preferences are present as bounded presentation constraints`() {
        val prompt = AlfredRealtimePrompt.turn(
            request(
                intent = SpeechIntent.AskToMove,
                preferences = WakePreferences(
                    morningBarrier = MorningBarrier.SNOOZE_LOOP,
                    interventionStyle = InterventionStyle.FIRM,
                    conversationAmount = ConversationAmount.MINIMAL,
                    humorPreference = HumorPreference.OFF,
                ),
            ),
        )

        assertTrue(prompt.contains("do not make jokes", ignoreCase = true))
        assertTrue(prompt.contains("direct", ignoreCase = true))
        assertTrue(prompt.contains("bargains for more sleep", ignoreCase = true))
        assertTrue(prompt.contains("feet toward the floor", ignoreCase = true))
        assertTrue(prompt.contains("Never change, skip or add", ignoreCase = true))
    }

    @Test
    fun `private occurrence context is withheld from early wake turns`() {
        val context = WakeAllowedContext(
            displayName = "Yotam",
            tomorrowReason = "Interview at ten",
            firstMove = "Take a shower",
        )

        val early = AlfredRealtimePrompt.turn(
            request(SpeechIntent.AskToSitUp, allowedContext = context),
        )
        val orientation = AlfredRealtimePrompt.turn(
            request(SpeechIntent.Orientation, allowedContext = context),
        )

        assertFalse(early.contains("Interview at ten"))
        assertFalse(early.contains("Take a shower"))
        assertTrue(orientation.contains("Interview at ten"))
        assertTrue(orientation.contains("Take a shower"))
        assertTrue(orientation.contains("not instructions", ignoreCase = true))
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
            assertTrue(prompt.contains("feet toward the floor"))
            assertFalse(prompt.contains("You are Wake My Way's morning wake companion"))
        }
    }

    @Test
    fun `response instructions preserve full persona while applying current runtime directive`() {
        val prompt = AlfredRealtimePrompt.response(
            request(SpeechIntent.AskToMove),
        )

        assertTrue(prompt.contains("You are Alfred"))
        assertTrue(prompt.contains("SAME PERSON"))
        assertTrue(prompt.contains("Anti-AI mannerisms"))
        assertTrue(prompt.contains("Current Wake Runtime directive"))
        assertTrue(prompt.contains("feet toward the floor"))
    }

    @Test
    fun `early wake protocol keeps cognitive load low and physical progression explicit`() {
        val system = AlfredRealtimePrompt.SYSTEM.lowercase()

        assertTrue(system.contains("cognition is temporarily reduced"))
        assertTrue(system.contains("never ask open-ended questions"))
        assertTrue(system.contains("physical toolbox is not a routine"))
        assertTrue(system.contains("should not chain sit -> feet -> shoulders -> stand -> light"))
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
        assertTrue(reengage.contains("slightly more direct", ignoreCase = true))
        assertTrue(reengage.contains("do not use 'answer me'", ignoreCase = true))
    }

    @Test
    fun `normal realtime turns do not demand verbal compliance`() {
        val system = AlfredRealtimePrompt.SYSTEM.lowercase()
        assertTrue(system.contains("user does not owe you a response"))
        assertTrue(system.contains("normal wake turn does not need a verbal confirmation request"))

        listOf(
            SpeechIntent.InitialWake,
            SpeechIntent.AskToSitUp,
            SpeechIntent.AskToMove,
            SpeechIntent.ActivateUpperBody,
            SpeechIntent.StandIfSafe,
            SpeechIntent.KeepEngaging,
        ).forEach { intent ->
            val prompt = AlfredRealtimePrompt.turn(intent, VoiceStyle.DEFAULT).lowercase()
            assertTrue(prompt.contains("do not ask") || prompt.contains("do not demand"))
            assertFalse(prompt.contains("ask for one short confirmation"))
        }
    }

    @Test
    fun `hold engagement explicitly forbids another physical command`() {
        val prompt = AlfredRealtimePrompt.turn(
            SpeechIntent.HoldEngagement,
            VoiceStyle.DEFAULT,
        )

        assertTrue(prompt.contains("Do not give another physical action"))
        assertTrue(prompt.contains("Explicitly avoid sit-up"))
        assertTrue(prompt.contains("respond naturally and briefly", ignoreCase = true))
    }

    @Test
    fun `social preference is allowed to surface during hold engagement without physical commands`() {
        val prompt = AlfredRealtimePrompt.turn(
            request(
                intent = SpeechIntent.HoldEngagement,
                preferences = WakePreferences(conversationAmount = ConversationAmount.SOCIAL),
            ),
        )

        assertTrue(prompt.contains("social energy", ignoreCase = true))
        assertTrue(prompt.contains("more human acknowledgement", ignoreCase = true))
        assertTrue(prompt.contains("Do not give another physical action"))
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

    private fun request(
        intent: SpeechIntent,
        preferences: WakePreferences = WakePreferences(),
        allowedContext: WakeAllowedContext = WakeAllowedContext(),
    ): WakeSpeechRequest = WakeSpeechRequest(
        intent = intent,
        sessionPlan = WakeSessionStrategyResolver.resolve(
            preferences = preferences,
            characterId = CharacterId.ALFRED,
            voiceStyle = VoiceStyle.DEFAULT,
            wakePolicy = WakePolicy(),
            allowedContext = allowedContext,
        ),
    )
}

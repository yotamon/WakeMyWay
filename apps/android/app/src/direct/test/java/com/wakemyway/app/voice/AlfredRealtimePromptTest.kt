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
    fun `explicit preferences are presentation constraints`() {
        val request = request(
            intent = SpeechIntent.AskToMove,
            preferences = WakePreferences(
                morningBarrier = MorningBarrier.SNOOZE_LOOP,
                interventionStyle = InterventionStyle.FIRM,
                conversationAmount = ConversationAmount.MINIMAL,
                humorPreference = HumorPreference.OFF,
            ),
        )

        val prompt = AlfredRealtimePrompt.turn(request)

        assertTrue(prompt.contains("do not make jokes", ignoreCase = true))
        assertTrue(prompt.contains("direct", ignoreCase = true))
        assertTrue(prompt.contains("snooze", ignoreCase = true))
        assertTrue(prompt.contains("put their feet on the floor", ignoreCase = true))
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

package com.wakemyway.app.voice

import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.personalization.ConversationAmount
import com.wakemyway.core.personalization.MotivationStyle
import com.wakemyway.core.personalization.PerceivedWakeInertia
import com.wakemyway.core.personalization.WakePreferences
import com.wakemyway.core.personalization.WakeSessionStrategyResolver
import com.wakemyway.core.runtime.SpeechIntent
import com.wakemyway.core.runtime.WakePolicy
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveWakePersonalizationTest {
    @Test fun minimalPreferenceConstrainsTheActualCue() {
        val plan = WakeSessionStrategyResolver.resolve(
            WakePreferences(conversationAmount = ConversationAmount.MINIMAL),
            CharacterId.ALFRED, VoiceStyle.MINIMAL, WakePolicy(),
        )
        val cue = GptLiveWakeCue.forRequest(WakeSpeechRequest(SpeechIntent.InitialWake, plan))
        assertTrue(cue.contains("eight spoken words"))
        assertTrue(cue.contains("tiny practical action"))
        assertTrue(cue.length <= 500)
    }

    @Test fun patientSocialWakeCreatesSpaceToRespond() {
        val plan = WakeSessionStrategyResolver.resolve(
            WakePreferences(
                perceivedWakeInertia = PerceivedWakeInertia.HOUR_OR_MORE,
                conversationAmount = ConversationAmount.SOCIAL,
                motivationStyle = MotivationStyle.LIGHT_CONVERSATION,
            ),
            CharacterId("luna"), VoiceStyle.DEFAULT, WakePolicy(),
        )
        val cue = GptLiveWakeCue.forRequest(WakeSpeechRequest(SpeechIntent.InitialWake, plan))
        assertTrue(cue.contains("generous quiet pause"))
        assertTrue(cue.contains("familiar friend"))
        assertTrue(cue.length <= 500)
    }
}

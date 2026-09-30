package com.wakemyway.core.personalization

import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.runtime.WakePolicy
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class WakePersonalizationTest {
    @Test
    fun `balanced defaults preserve current conversation timing`() {
        val plan = WakeSessionStrategyResolver.resolve(
            preferences = WakePreferences(),
            characterId = CharacterId.ALFRED,
            voiceStyle = VoiceStyle.DEFAULT,
            wakePolicy = WakePolicy(),
        )

        assertEquals(Duration.ofSeconds(10), plan.conversationPacing.listenTimeout)
        assertEquals(Duration.ofSeconds(12), plan.conversationPacing.idleReengageDelay)
        assertEquals(WakeTurnLength.SHORT, plan.conversationPacing.preferredTurnLength)
        assertEquals(WakeDirectness.BALANCED, plan.expressionProfile.directness)
        assertEquals(WakeHumorLevel.LIGHT, plan.expressionProfile.humorLevel)
    }
    @Test
    fun `explicit firm minimal preferences stay presentation bounded`() {
        val policy = WakePolicy(activationThreshold = 5)
        val plan = WakeSessionStrategyResolver.resolve(
            preferences = WakePreferences(
                morningBarrier = MorningBarrier.SNOOZE_LOOP,
                interventionStyle = InterventionStyle.FIRM,
                conversationAmount = ConversationAmount.MINIMAL,
                humorPreference = HumorPreference.OFF,
            ),
            characterId = CharacterId.ALFRED,
            voiceStyle = VoiceStyle.DEFAULT,
            wakePolicy = policy,
        )

        assertEquals(policy, plan.wakePolicy)
        assertEquals(MorningBarrier.SNOOZE_LOOP, plan.expressionProfile.morningBarrier)
        assertEquals(WakeDirectness.DIRECT, plan.expressionProfile.directness)
        assertEquals(WakeVerbosity.VERY_LOW, plan.expressionProfile.verbosity)
        assertEquals(WakeHumorLevel.OFF, plan.expressionProfile.humorLevel)
        assertEquals(Duration.ofSeconds(11), plan.conversationPacing.idleReengageDelay)
    }
    @Test
    fun `self reported slower mornings only widen bounded pacing`() {
        val plan = WakeSessionStrategyResolver.resolve(
            preferences = WakePreferences(
                perceivedWakeInertia = PerceivedWakeInertia.HOUR_OR_MORE,
                conversationAmount = ConversationAmount.SOCIAL,
            ),
            characterId = CharacterId.ALFRED,
            voiceStyle = VoiceStyle.DEFAULT,
            wakePolicy = WakePolicy(),
        )

        assertEquals(WakeResponsePacing.PATIENT, plan.expressionProfile.responsePacing)
        assertEquals(Duration.ofSeconds(12), plan.conversationPacing.listenTimeout)
        assertEquals(Duration.ofSeconds(14), plan.conversationPacing.idleReengageDelay)
        assertEquals(WakeTurnLength.NATURAL, plan.conversationPacing.preferredTurnLength)
    }

    @Test
    fun `minimal voice style wins over social conversation preference`() {
        val plan = WakeSessionStrategyResolver.resolve(
            preferences = WakePreferences(conversationAmount = ConversationAmount.SOCIAL),
            characterId = CharacterId.ALFRED,
            voiceStyle = VoiceStyle.MINIMAL,
            wakePolicy = WakePolicy(),
        )

        assertEquals(WakeVerbosity.VERY_LOW, plan.expressionProfile.verbosity)
        assertEquals(WakeTurnLength.TERSE, plan.conversationPacing.preferredTurnLength)
    }
    @Test
    fun `allowed context is normalized and bounded before a session`() {
        val longReason = "reason ".repeat(300)
        val plan = WakeSessionStrategyResolver.resolve(
            preferences = WakePreferences(),
            characterId = CharacterId.ALFRED,
            voiceStyle = VoiceStyle.DEFAULT,
            wakePolicy = WakePolicy(),
            allowedContext = WakeAllowedContext(
                displayName = "  Yotam  ",
                tomorrowReason = "  $longReason  ",
                firstMove = "  Open   the curtains  ",
            ),
        )

        assertEquals("Yotam", plan.allowedContext.displayName)
        assertEquals("Open the curtains", plan.allowedContext.firstMove)
        assertEquals(1_200, plan.allowedContext.tomorrowReason?.length)
    }
}

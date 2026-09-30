package com.wakemyway.core.personalization

import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.runtime.WakePolicy
import java.time.Duration

enum class MorningBarrier {
    UNSURE,
    HALF_ASLEEP,
    SNOOZE_LOOP,
    AWAKE_BUT_STUCK,
    MORNING_OVERWHELM,
    LOSE_TRACK_OF_TIME,
    USUALLY_GET_UP,
}

enum class PerceivedWakeInertia {
    UNSURE,
    FEW_MINUTES,
    ABOUT_15_MINUTES,
    ABOUT_30_MINUTES,
    HOUR_OR_MORE,
}

enum class InterventionStyle {
    GENTLE,
    ENCOURAGING,
    PERSISTENT,
    FIRM,
}
enum class MotivationStyle {
    CONCRETE_ACTION,
    ENCOURAGEMENT,
    ACCOUNTABILITY,
    LIGHT_CONVERSATION,
    HUMOR,
}

enum class ConversationAmount {
    MINIMAL,
    BALANCED,
    SOCIAL,
}

enum class HumorPreference {
    OFF,
    LIGHT,
    WELCOME,
}

data class WakePreferences(
    val morningBarrier: MorningBarrier = MorningBarrier.UNSURE,
    val perceivedWakeInertia: PerceivedWakeInertia = PerceivedWakeInertia.UNSURE,
    val interventionStyle: InterventionStyle = InterventionStyle.ENCOURAGING,
    val motivationStyle: MotivationStyle = MotivationStyle.CONCRETE_ACTION,
    val conversationAmount: ConversationAmount = ConversationAmount.BALANCED,
    val humorPreference: HumorPreference = HumorPreference.LIGHT,
)

enum class WakeDirectness {
    SOFT,
    BALANCED,
    DIRECT,
}
enum class WakeVerbosity {
    VERY_LOW,
    LOW,
    MEDIUM,
}

enum class WakeSocialEnergy {
    LOW,
    BALANCED,
    WARM,
}

enum class WakeMotivationFrame {
    ACTION,
    SUPPORT,
    ACCOUNTABILITY,
    SOCIAL,
    HUMOR,
}

enum class WakeHumorLevel {
    OFF,
    LIGHT,
    OPEN,
}

enum class WakeResponsePacing {
    QUICK,
    STANDARD,
    PATIENT,
}

enum class WakeTurnLength {
    TERSE,
    SHORT,
    NATURAL,
}
data class WakeExpressionProfile(
    val morningBarrier: MorningBarrier,
    val directness: WakeDirectness,
    val verbosity: WakeVerbosity,
    val socialEnergy: WakeSocialEnergy,
    val motivationFrame: WakeMotivationFrame,
    val humorLevel: WakeHumorLevel,
    val responsePacing: WakeResponsePacing,
)

data class WakeConversationPacing(
    val listenTimeout: Duration,
    val idleReengageDelay: Duration,
    val preferredTurnLength: WakeTurnLength,
) {
    init {
        require(!listenTimeout.isNegative && !listenTimeout.isZero)
        require(!idleReengageDelay.isNegative && !idleReengageDelay.isZero)
        require(listenTimeout <= MAX_LISTEN_TIMEOUT)
        require(idleReengageDelay <= MAX_IDLE_REENGAGE_DELAY)
    }

    companion object {
        val MAX_LISTEN_TIMEOUT: Duration = Duration.ofSeconds(15)
        val MAX_IDLE_REENGAGE_DELAY: Duration = Duration.ofSeconds(18)
    }
}

data class WakeAllowedContext(
    val displayName: String? = null,
    val tomorrowReason: String? = null,
    val firstMove: String? = null,
)
data class WakeSessionPlan(
    val characterId: CharacterId,
    val voiceStyle: VoiceStyle,
    val wakePolicy: WakePolicy,
    val preferences: WakePreferences,
    val expressionProfile: WakeExpressionProfile,
    val conversationPacing: WakeConversationPacing,
    val allowedContext: WakeAllowedContext = WakeAllowedContext(),
) {
    init {
        require(allowedContext.displayName == null || allowedContext.displayName.length <= 48)
        require(allowedContext.tomorrowReason == null || allowedContext.tomorrowReason.length <= 1_200)
        require(allowedContext.firstMove == null || allowedContext.firstMove.length <= 120)
    }
}

object WakeSessionStrategyResolver {
    fun resolve(
        preferences: WakePreferences,
        characterId: CharacterId,
        voiceStyle: VoiceStyle,
        wakePolicy: WakePolicy,
        allowedContext: WakeAllowedContext = WakeAllowedContext(),
    ): WakeSessionPlan {
        val verbosity = when {
            voiceStyle == VoiceStyle.MINIMAL -> WakeVerbosity.VERY_LOW
            preferences.conversationAmount == ConversationAmount.MINIMAL -> WakeVerbosity.VERY_LOW
            preferences.conversationAmount == ConversationAmount.SOCIAL -> WakeVerbosity.MEDIUM
            else -> WakeVerbosity.LOW
        }
        val responsePacing = when (preferences.perceivedWakeInertia) {
            PerceivedWakeInertia.FEW_MINUTES -> WakeResponsePacing.QUICK
            PerceivedWakeInertia.ABOUT_30_MINUTES,
            PerceivedWakeInertia.HOUR_OR_MORE,
            -> WakeResponsePacing.PATIENT
            else -> WakeResponsePacing.STANDARD
        }
        val expression = WakeExpressionProfile(
            morningBarrier = preferences.morningBarrier,
            directness = when (preferences.interventionStyle) {
                InterventionStyle.GENTLE -> WakeDirectness.SOFT
                InterventionStyle.ENCOURAGING -> WakeDirectness.BALANCED
                InterventionStyle.PERSISTENT,
                InterventionStyle.FIRM,
                -> WakeDirectness.DIRECT
            },
            verbosity = verbosity,
            socialEnergy = when {
                preferences.conversationAmount == ConversationAmount.MINIMAL -> WakeSocialEnergy.LOW
                preferences.conversationAmount == ConversationAmount.SOCIAL -> WakeSocialEnergy.WARM
                preferences.motivationStyle == MotivationStyle.LIGHT_CONVERSATION -> WakeSocialEnergy.WARM
                else -> WakeSocialEnergy.BALANCED
            },
            motivationFrame = when (preferences.motivationStyle) {
                MotivationStyle.CONCRETE_ACTION -> WakeMotivationFrame.ACTION
                MotivationStyle.ENCOURAGEMENT -> WakeMotivationFrame.SUPPORT
                MotivationStyle.ACCOUNTABILITY -> WakeMotivationFrame.ACCOUNTABILITY
                MotivationStyle.LIGHT_CONVERSATION -> WakeMotivationFrame.SOCIAL
                MotivationStyle.HUMOR -> WakeMotivationFrame.HUMOR
            },
            humorLevel = when (preferences.humorPreference) {
                HumorPreference.OFF -> WakeHumorLevel.OFF
                HumorPreference.LIGHT -> WakeHumorLevel.LIGHT
                HumorPreference.WELCOME -> WakeHumorLevel.OPEN
            },
            responsePacing = responsePacing,
        )
        val baseListenSeconds = when (responsePacing) {
            WakeResponsePacing.QUICK -> 9L
            WakeResponsePacing.STANDARD -> 10L
            WakeResponsePacing.PATIENT -> 12L
        }
        val baseIdleSeconds = when (responsePacing) {
            WakeResponsePacing.QUICK -> 10L
            WakeResponsePacing.STANDARD -> 12L
            WakeResponsePacing.PATIENT -> 14L
        }
        val firmnessOffset = if (preferences.interventionStyle == InterventionStyle.FIRM) -1L else 0L
        val pacing = WakeConversationPacing(
            listenTimeout = Duration.ofSeconds(baseListenSeconds),
            idleReengageDelay = Duration.ofSeconds((baseIdleSeconds + firmnessOffset).coerceAtLeast(9L)),
            preferredTurnLength = when (verbosity) {
                WakeVerbosity.VERY_LOW -> WakeTurnLength.TERSE
                WakeVerbosity.LOW -> WakeTurnLength.SHORT
                WakeVerbosity.MEDIUM -> WakeTurnLength.NATURAL
            },
        )

        return WakeSessionPlan(
            characterId = characterId,
            voiceStyle = voiceStyle,
            wakePolicy = wakePolicy,
            preferences = preferences,
            expressionProfile = expression,
            conversationPacing = pacing,
            allowedContext = allowedContext.normalized(),
        )
    }

    private fun WakeAllowedContext.normalized(): WakeAllowedContext = copy(
        displayName = displayName.normalizedOrNull(48),
        tomorrowReason = tomorrowReason.normalizedOrNull(1_200),
        firstMove = firstMove.normalizedOrNull(120),
    )

    private fun String?.normalizedOrNull(maxLength: Int): String? =
        this?.trim()?.replace(Regex("\\s+"), " ")?.take(maxLength)?.takeIf(String::isNotBlank)
}

package com.wakemyway.core.alarm

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@JvmInline
value class AlarmDefinitionId(val value: String) {
    init {
        require(value.isNotBlank()) { "AlarmDefinitionId must not be blank" }
    }
}

@JvmInline
value class WakeSoundId(val value: String) {
    init {
        require(value.isNotBlank()) { "WakeSoundId must not be blank" }
    }

    companion object {
        val MORNING_LIGHT = WakeSoundId("morning-light")
        val SOFT_START = WakeSoundId("soft-start")
        val MORNING_PULSE = WakeSoundId("morning-pulse")
    }
}

@JvmInline
value class CharacterId(val value: String) {
    init {
        require(value.isNotBlank()) { "CharacterId must not be blank" }
    }

    companion object {
        val ALFRED = CharacterId("alfred")
    }
}

enum class VoiceStyle {
    /** Calm, concise and supportive. */
    DEFAULT,

    /** More energetic phrasing while preserving the same Wake Runtime directives. */
    MOTIVATIONAL,

    /** Very short phrasing with minimal conversational overhead. */
    MINIMAL,
}

enum class TomorrowContractMode {
    /** Tomorrow Contract is offered for this alarm but remains optional. */
    OPTIONAL,

    /** Prompt the user more prominently during evening preparation. */
    ALWAYS_PROMPT,

    /** Hide Tomorrow Contract for this alarm. */
    DISABLED,
}

data class SnoozePolicy(
    val enabled: Boolean = true,
    val duration: Duration = Duration.ofMinutes(5),
    val maxCount: Int? = null,
) {
    init {
        require(!duration.isZero && !duration.isNegative) {
            "Snooze duration must be positive"
        }
        require(duration <= MAX_SNOOZE_DURATION) {
            "Snooze duration must not exceed $MAX_SNOOZE_DURATION"
        }
        require(maxCount == null || maxCount > 0) {
            "Snooze maxCount must be positive when specified"
        }
    }

    companion object {
        val MAX_SNOOZE_DURATION: Duration = Duration.ofMinutes(30)
    }
}

sealed interface AlarmSchedulePattern {
    val time: LocalTime

    data class Weekly(
        val days: Set<DayOfWeek>,
        override val time: LocalTime,
    ) : AlarmSchedulePattern {
        init {
            require(days.isNotEmpty()) { "A weekly alarm needs at least one active day" }
        }
    }

    data class OneShot(
        val date: LocalDate,
        override val time: LocalTime,
    ) : AlarmSchedulePattern
}

/**
 * Consumer-facing alarm configuration.
 *
 * This is product state, not Android scheduling state and not Direct Boot critical state. The
 * scheduling layer compiles enabled definitions into the minimal deterministic schedule data the
 * Alarm Kernel needs. Presentation-only or private data must not leak into Critical Wake storage.
 */
data class AlarmDefinition(
    val id: AlarmDefinitionId,
    val label: String = "",
    val enabled: Boolean = true,
    val zoneId: ZoneId,
    val schedule: AlarmSchedulePattern,
    val soundId: WakeSoundId = WakeSoundId.MORNING_LIGHT,
    val voiceCheckInEnabled: Boolean = true,
    val characterId: CharacterId = CharacterId.ALFRED,
    val voiceStyle: VoiceStyle = VoiceStyle.DEFAULT,
    val snoozePolicy: SnoozePolicy = SnoozePolicy(),
    val tomorrowContractMode: TomorrowContractMode = TomorrowContractMode.OPTIONAL,
    val firstMoveDefault: String? = null,
    val revision: Long = 1,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(label.length <= MAX_LABEL_CHARACTERS) {
            "Alarm label must be at most $MAX_LABEL_CHARACTERS characters"
        }
        require(firstMoveDefault == null || firstMoveDefault.length <= MAX_FIRST_MOVE_CHARACTERS) {
            "First Move default must be at most $MAX_FIRST_MOVE_CHARACTERS characters"
        }
        require(revision > 0) { "AlarmDefinition revision must be positive" }
        require(!updatedAt.isBefore(createdAt)) {
            "AlarmDefinition updatedAt must not be before createdAt"
        }
    }

    companion object {
        const val MAX_LABEL_CHARACTERS = 80
        const val MAX_FIRST_MOVE_CHARACTERS = 120
    }
}

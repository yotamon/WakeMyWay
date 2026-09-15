package com.wakemyway.app.alarm

import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import java.time.Duration

/**
 * Non-sensitive runtime policy that must be available before credential unlock.
 *
 * This deliberately contains only values required to execute the configured wake while the device
 * is locked. User-authored text, labels, Tomorrow Contract content, transcripts and account data
 * remain in credential-protected product storage.
 */
data class CriticalWakePolicy(
    val soundId: WakeSoundId = WakeSoundId.MORNING_LIGHT,
    val voiceCheckInEnabled: Boolean = true,
    val characterId: CharacterId = CharacterId.ALFRED,
    val voiceStyle: VoiceStyle = VoiceStyle.DEFAULT,
    val snoozeEnabled: Boolean = true,
    val snoozeDuration: Duration = DEFAULT_SNOOZE_DURATION,
) {
    init {
        require(!snoozeDuration.isNegative && !snoozeDuration.isZero) {
            "Critical snooze duration must be positive"
        }
        require(snoozeDuration <= MAX_SNOOZE_DURATION) {
            "Critical snooze duration is unreasonably long"
        }
    }

    companion object {
        val DEFAULT_SNOOZE_DURATION: Duration = Duration.ofMinutes(5)
        val MAX_SNOOZE_DURATION: Duration = Duration.ofHours(2)
        val DEFAULT = CriticalWakePolicy()

        fun from(definition: AlarmDefinition): CriticalWakePolicy = CriticalWakePolicy(
            soundId = definition.soundId,
            voiceCheckInEnabled = definition.voiceCheckInEnabled,
            characterId = definition.characterId,
            voiceStyle = definition.voiceStyle,
            snoozeEnabled = definition.snoozePolicy.enabled,
            snoozeDuration = definition.snoozePolicy.duration,
        )
    }
}
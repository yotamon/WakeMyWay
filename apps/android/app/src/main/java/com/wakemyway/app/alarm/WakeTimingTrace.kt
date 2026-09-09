package com.wakemyway.app.alarm

import android.content.Context
import android.os.SystemClock
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId

/**
 * Non-sensitive, best-effort diagnostics for M1/M2 reliability measurement.
 * This is not analytics and never contains user-entered/private context.
 */
class WakeTimingTrace(context: Context) {
    private val prefs = context
        .createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun scheduled(occurrence: WakeOccurrence) {
        prefs.edit()
            .clear()
            .putString(KEY_OCCURRENCE_ID, occurrence.id.value)
            .putLong(KEY_TARGET_WALL_MS, occurrence.scheduledAt.toInstant().toEpochMilli())
            .putLong(KEY_SCHEDULED_WALL_MS, System.currentTimeMillis())
            .apply()
    }

    fun receiver(occurrenceId: WakeOccurrenceId) = mark(occurrenceId, KEY_RECEIVER_WALL_MS, KEY_RECEIVER_ELAPSED_MS)

    fun foreground(occurrenceId: WakeOccurrenceId) = mark(occurrenceId, KEY_FOREGROUND_WALL_MS, KEY_FOREGROUND_ELAPSED_MS)

    fun audioStarted(occurrenceId: WakeOccurrenceId) = mark(occurrenceId, KEY_AUDIO_WALL_MS, KEY_AUDIO_ELAPSED_MS)

    fun uiVisible(occurrenceId: WakeOccurrenceId) = mark(occurrenceId, KEY_UI_WALL_MS, KEY_UI_ELAPSED_MS)

    fun snapshot(): TimingSnapshot? {
        val occurrenceId = prefs.getString(KEY_OCCURRENCE_ID, null) ?: return null
        return TimingSnapshot(
            occurrenceId = occurrenceId,
            targetWallMillis = prefs.getLong(KEY_TARGET_WALL_MS, 0),
            scheduledWallMillis = prefs.getLong(KEY_SCHEDULED_WALL_MS, 0),
            receiverWallMillis = prefs.optionalLong(KEY_RECEIVER_WALL_MS),
            foregroundWallMillis = prefs.optionalLong(KEY_FOREGROUND_WALL_MS),
            audioWallMillis = prefs.optionalLong(KEY_AUDIO_WALL_MS),
            uiWallMillis = prefs.optionalLong(KEY_UI_WALL_MS),
        )
    }

    private fun mark(
        occurrenceId: WakeOccurrenceId,
        wallKey: String,
        elapsedKey: String,
    ) {
        if (prefs.getString(KEY_OCCURRENCE_ID, null) != occurrenceId.value) return
        prefs.edit()
            .putLong(wallKey, System.currentTimeMillis())
            .putLong(elapsedKey, SystemClock.elapsedRealtime())
            .apply()
    }

    private fun android.content.SharedPreferences.optionalLong(key: String): Long? =
        if (contains(key)) getLong(key, 0) else null

    private companion object {
        const val PREFS_NAME = "wake-timing-trace"
        const val KEY_OCCURRENCE_ID = "occurrence_id"
        const val KEY_TARGET_WALL_MS = "target_wall_ms"
        const val KEY_SCHEDULED_WALL_MS = "scheduled_wall_ms"
        const val KEY_RECEIVER_WALL_MS = "receiver_wall_ms"
        const val KEY_RECEIVER_ELAPSED_MS = "receiver_elapsed_ms"
        const val KEY_FOREGROUND_WALL_MS = "foreground_wall_ms"
        const val KEY_FOREGROUND_ELAPSED_MS = "foreground_elapsed_ms"
        const val KEY_AUDIO_WALL_MS = "audio_wall_ms"
        const val KEY_AUDIO_ELAPSED_MS = "audio_elapsed_ms"
        const val KEY_UI_WALL_MS = "ui_wall_ms"
        const val KEY_UI_ELAPSED_MS = "ui_elapsed_ms"
    }
}

data class TimingSnapshot(
    val occurrenceId: String,
    val targetWallMillis: Long,
    val scheduledWallMillis: Long,
    val receiverWallMillis: Long?,
    val foregroundWallMillis: Long?,
    val audioWallMillis: Long?,
    val uiWallMillis: Long?,
) {
    val triggerDelayMillis: Long? = receiverWallMillis?.minus(targetWallMillis)
    val triggerToForegroundMillis: Long? = receiverWallMillis?.let { receiver ->
        foregroundWallMillis?.minus(receiver)
    }
    val triggerToAudioMillis: Long? = receiverWallMillis?.let { receiver ->
        audioWallMillis?.minus(receiver)
    }
    val triggerToUiMillis: Long? = receiverWallMillis?.let { receiver ->
        uiWallMillis?.minus(receiver)
    }
}

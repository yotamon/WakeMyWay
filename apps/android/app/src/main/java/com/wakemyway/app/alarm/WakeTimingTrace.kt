package com.wakemyway.app.alarm

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bounded, non-sensitive, best-effort reliability journal.
 *
 * This is diagnostic state, not analytics. Every write is failure-isolated so reliability
 * instrumentation can never prevent the alarm path from continuing. No user-entered wake
 * context, transcript, prompt, calendar title or microphone data belongs here.
 */
class WakeTimingTrace(context: Context) {
    private val prefs = context
        .createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun receiver(occurrence: WakeOccurrence) = safelyMutate { sessions ->
        sessions.removeAll { it.optString(KEY_OCCURRENCE_ID) == occurrence.id.value }
        sessions.add(
            0,
            JSONObject().apply {
                put(KEY_OCCURRENCE_ID, occurrence.id.value)
                put(KEY_SCHEDULE_ID, occurrence.wakeScheduleId.value)
                put(KEY_OCCURRENCE_KIND, occurrence.kind.name)
                put(KEY_TARGET_WALL_MS, occurrence.scheduledAt.toInstant().toEpochMilli())
                put(KEY_RECEIVER_WALL_MS, System.currentTimeMillis())
                put(KEY_RECEIVER_ELAPSED_MS, SystemClock.elapsedRealtime())
                put(KEY_SERVICE_RECOVERY_COUNT, 0)
            },
        )
    }

    fun foreground(occurrenceId: WakeOccurrenceId) = mark(
        occurrenceId = occurrenceId,
        wallKey = KEY_FOREGROUND_WALL_MS,
        elapsedKey = KEY_FOREGROUND_ELAPSED_MS,
    )

    fun audioStarted(occurrenceId: WakeOccurrenceId) = mark(
        occurrenceId = occurrenceId,
        wallKey = KEY_AUDIO_WALL_MS,
        elapsedKey = KEY_AUDIO_ELAPSED_MS,
    )

    fun uiVisible(occurrenceId: WakeOccurrenceId) = mark(
        occurrenceId = occurrenceId,
        wallKey = KEY_UI_WALL_MS,
        elapsedKey = KEY_UI_ELAPSED_MS,
    )

    fun serviceRecovered(occurrenceId: WakeOccurrenceId) = safelyMutate { sessions ->
        sessions.findSession(occurrenceId)?.apply {
            put(KEY_SERVICE_RECOVERY_COUNT, optInt(KEY_SERVICE_RECOVERY_COUNT, 0) + 1)
            put(KEY_LAST_RECOVERY_WALL_MS, System.currentTimeMillis())
            put(KEY_LAST_RECOVERY_ELAPSED_MS, SystemClock.elapsedRealtime())
        }
    }

    fun stopped(occurrenceId: WakeOccurrenceId) = terminal(occurrenceId, TerminalAction.STOPPED)

    fun snoozed(occurrenceId: WakeOccurrenceId) = terminal(occurrenceId, TerminalAction.SNOOZED)

    fun snapshot(): TimingSnapshot? = history(limit = 1).firstOrNull()

    fun history(limit: Int = MAX_SESSIONS): List<TimingSnapshot> = runCatching {
        synchronized(LOCK) {
            readSessions()
                .take(limit.coerceIn(0, MAX_SESSIONS))
                .mapNotNull(::toSnapshot)
        }
    }.getOrDefault(emptyList())

    fun clearHistory() {
        runCatching { prefs.edit().remove(KEY_SESSIONS).apply() }
    }

    private fun mark(
        occurrenceId: WakeOccurrenceId,
        wallKey: String,
        elapsedKey: String,
    ) = safelyMutate { sessions ->
        sessions.findSession(occurrenceId)?.apply {
            put(wallKey, System.currentTimeMillis())
            put(elapsedKey, SystemClock.elapsedRealtime())
        }
    }

    private fun terminal(
        occurrenceId: WakeOccurrenceId,
        action: TerminalAction,
    ) = safelyMutate { sessions ->
        sessions.findSession(occurrenceId)?.apply {
            put(KEY_TERMINAL_ACTION, action.name)
            put(KEY_TERMINAL_WALL_MS, System.currentTimeMillis())
            put(KEY_TERMINAL_ELAPSED_MS, SystemClock.elapsedRealtime())
        }
    }

    private inline fun safelyMutate(crossinline mutation: (MutableList<JSONObject>) -> Unit) {
        runCatching {
            synchronized(LOCK) {
                val sessions = readSessions()
                mutation(sessions)
                persistSessions(sessions)
            }
        }
    }

    private fun readSessions(): MutableList<JSONObject> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return mutableListOf()
        return runCatching {
            val array = JSONArray(raw)
            MutableList(array.length()) { index -> array.getJSONObject(index) }
        }.getOrElse { mutableListOf() }
    }

    private fun persistSessions(sessions: List<JSONObject>) {
        val bounded = sessions.take(MAX_SESSIONS)
        val array = JSONArray().apply { bounded.forEach(::put) }
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }

    private fun MutableList<JSONObject>.findSession(occurrenceId: WakeOccurrenceId): JSONObject? =
        firstOrNull { it.optString(KEY_OCCURRENCE_ID) == occurrenceId.value }

    private fun toSnapshot(json: JSONObject): TimingSnapshot? {
        val occurrenceId = json.optString(KEY_OCCURRENCE_ID).takeIf { it.isNotBlank() } ?: return null
        return TimingSnapshot(
            occurrenceId = occurrenceId,
            scheduleId = json.optString(KEY_SCHEDULE_ID),
            occurrenceKind = json.optString(KEY_OCCURRENCE_KIND),
            targetWallMillis = json.optLong(KEY_TARGET_WALL_MS, 0),
            receiverWallMillis = json.optionalLong(KEY_RECEIVER_WALL_MS),
            receiverElapsedMillis = json.optionalLong(KEY_RECEIVER_ELAPSED_MS),
            foregroundWallMillis = json.optionalLong(KEY_FOREGROUND_WALL_MS),
            foregroundElapsedMillis = json.optionalLong(KEY_FOREGROUND_ELAPSED_MS),
            audioWallMillis = json.optionalLong(KEY_AUDIO_WALL_MS),
            audioElapsedMillis = json.optionalLong(KEY_AUDIO_ELAPSED_MS),
            uiWallMillis = json.optionalLong(KEY_UI_WALL_MS),
            uiElapsedMillis = json.optionalLong(KEY_UI_ELAPSED_MS),
            terminalAction = json.optString(KEY_TERMINAL_ACTION).takeIf { it.isNotBlank() },
            terminalWallMillis = json.optionalLong(KEY_TERMINAL_WALL_MS),
            terminalElapsedMillis = json.optionalLong(KEY_TERMINAL_ELAPSED_MS),
            serviceRecoveryCount = json.optInt(KEY_SERVICE_RECOVERY_COUNT, 0),
            lastRecoveryWallMillis = json.optionalLong(KEY_LAST_RECOVERY_WALL_MS),
        )
    }

    private fun JSONObject.optionalLong(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private enum class TerminalAction {
        STOPPED,
        SNOOZED,
    }

    private companion object {
        val LOCK = Any()
        const val MAX_SESSIONS = 24
        const val PREFS_NAME = "wake-reliability-journal"
        const val KEY_SESSIONS = "sessions"
        const val KEY_OCCURRENCE_ID = "occurrence_id"
        const val KEY_SCHEDULE_ID = "schedule_id"
        const val KEY_OCCURRENCE_KIND = "occurrence_kind"
        const val KEY_TARGET_WALL_MS = "target_wall_ms"
        const val KEY_RECEIVER_WALL_MS = "receiver_wall_ms"
        const val KEY_RECEIVER_ELAPSED_MS = "receiver_elapsed_ms"
        const val KEY_FOREGROUND_WALL_MS = "foreground_wall_ms"
        const val KEY_FOREGROUND_ELAPSED_MS = "foreground_elapsed_ms"
        const val KEY_AUDIO_WALL_MS = "audio_wall_ms"
        const val KEY_AUDIO_ELAPSED_MS = "audio_elapsed_ms"
        const val KEY_UI_WALL_MS = "ui_wall_ms"
        const val KEY_UI_ELAPSED_MS = "ui_elapsed_ms"
        const val KEY_TERMINAL_ACTION = "terminal_action"
        const val KEY_TERMINAL_WALL_MS = "terminal_wall_ms"
        const val KEY_TERMINAL_ELAPSED_MS = "terminal_elapsed_ms"
        const val KEY_SERVICE_RECOVERY_COUNT = "service_recovery_count"
        const val KEY_LAST_RECOVERY_WALL_MS = "last_recovery_wall_ms"
        const val KEY_LAST_RECOVERY_ELAPSED_MS = "last_recovery_elapsed_ms"
    }
}

data class TimingSnapshot(
    val occurrenceId: String,
    val scheduleId: String,
    val occurrenceKind: String,
    val targetWallMillis: Long,
    val receiverWallMillis: Long?,
    val receiverElapsedMillis: Long?,
    val foregroundWallMillis: Long?,
    val foregroundElapsedMillis: Long?,
    val audioWallMillis: Long?,
    val audioElapsedMillis: Long?,
    val uiWallMillis: Long?,
    val uiElapsedMillis: Long?,
    val terminalAction: String?,
    val terminalWallMillis: Long?,
    val terminalElapsedMillis: Long?,
    val serviceRecoveryCount: Int,
    val lastRecoveryWallMillis: Long?,
) {
    val triggerDelayMillis: Long? = receiverWallMillis?.minus(targetWallMillis)
    val triggerToForegroundMillis: Long? = elapsedDelta(foregroundElapsedMillis)
    val triggerToAudioMillis: Long? = elapsedDelta(audioElapsedMillis)
    val triggerToUiMillis: Long? = elapsedDelta(uiElapsedMillis)
    val triggerToTerminalMillis: Long? = elapsedDelta(terminalElapsedMillis)

    private fun elapsedDelta(stageElapsedMillis: Long?): Long? =
        receiverElapsedMillis?.let { receiver -> stageElapsedMillis?.minus(receiver) }
}

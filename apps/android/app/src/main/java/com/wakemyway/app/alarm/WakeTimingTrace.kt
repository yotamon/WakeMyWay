package com.wakemyway.app.alarm

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.Instant
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

    /**
     * Records an expected OS wake before its target time. This is what lets the lab diagnose
     * the most important failure mode: an alarm that was scheduled but never reached the receiver.
     */
    fun expected(
        occurrence: WakeOccurrence,
        scenario: String,
        expectFullScreen: Boolean,
    ) = safelyMutate { sessions ->
        val session = sessions.findSession(occurrence.id) ?: newSession(occurrence).also {
            sessions.add(0, it)
        }
        session.put(KEY_SCENARIO, scenario)
        session.put(KEY_EXPECT_FULL_SCREEN, expectFullScreen)
        session.put(KEY_EXPECTED_WALL_MS, System.currentTimeMillis())
        appendEvent(session, EVENT_EXPECTED, "scenario=$scenario;fullScreen=$expectFullScreen")
    }

    fun receiver(occurrence: WakeOccurrence) = safelyMutate { sessions ->
        val session = sessions.findSession(occurrence.id) ?: newSession(occurrence).also {
            sessions.add(0, it)
        }
        session.put(KEY_RECEIVER_WALL_MS, System.currentTimeMillis())
        session.put(KEY_RECEIVER_ELAPSED_MS, SystemClock.elapsedRealtime())
        appendEvent(session, EVENT_RECEIVER)
    }

    fun foreground(occurrenceId: WakeOccurrenceId) = mark(
        occurrenceId = occurrenceId,
        wallKey = KEY_FOREGROUND_WALL_MS,
        elapsedKey = KEY_FOREGROUND_ELAPSED_MS,
        eventType = EVENT_FOREGROUND,
    )

    fun audioStarted(occurrenceId: WakeOccurrenceId) = mark(
        occurrenceId = occurrenceId,
        wallKey = KEY_AUDIO_WALL_MS,
        elapsedKey = KEY_AUDIO_ELAPSED_MS,
        eventType = EVENT_AUDIO_STARTED,
    )

    fun uiVisible(occurrenceId: WakeOccurrenceId) = mark(
        occurrenceId = occurrenceId,
        wallKey = KEY_UI_WALL_MS,
        elapsedKey = KEY_UI_ELAPSED_MS,
        eventType = EVENT_UI_VISIBLE,
    )

    fun serviceRecovered(occurrenceId: WakeOccurrenceId) = safelyMutate { sessions ->
        sessions.findSession(occurrenceId)?.apply {
            put(KEY_SERVICE_RECOVERY_COUNT, optInt(KEY_SERVICE_RECOVERY_COUNT, 0) + 1)
            put(KEY_LAST_RECOVERY_WALL_MS, System.currentTimeMillis())
            put(KEY_LAST_RECOVERY_ELAPSED_MS, SystemClock.elapsedRealtime())
            appendEvent(this, EVENT_SERVICE_RECOVERED)
        }
    }

    fun reconciled(
        occurrenceId: WakeOccurrenceId,
        reason: String,
        wakeReadyAfter: Boolean,
    ) = safelyMutate { sessions ->
        sessions.findSession(occurrenceId)?.let { session ->
            appendEvent(session, EVENT_RECONCILED, "reason=$reason;wakeReady=$wakeReadyAfter")
        }
    }

    fun capabilities(
        occurrenceId: WakeOccurrenceId,
        exactAlarmAllowed: Boolean,
        notificationsAllowed: Boolean,
        fullScreenIntentAllowed: Boolean,
    ) = safelyMutate { sessions ->
        sessions.findSession(occurrenceId)?.let { session ->
            appendEvent(
                session,
                EVENT_CAPABILITIES,
                "exact=$exactAlarmAllowed;notifications=$notificationsAllowed;fullScreen=$fullScreenIntentAllowed",
            )
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

    fun reportText(limit: Int = MAX_SESSIONS): String {
        val sessions = history(limit)
        return buildString {
            appendLine("Wake My Way Reliability Report")
            appendLine("generated=${Instant.ofEpochMilli(System.currentTimeMillis())}")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}; sdk=${Build.VERSION.SDK_INT}")
            appendLine("sessions=${sessions.size}")
            appendLine()

            sessions.forEachIndexed { index, session ->
                appendLine("#${index + 1} ${session.scenario ?: session.occurrenceKind.ifBlank { "UNKNOWN" }} ${session.state().name}")
                appendLine("occurrence=${session.occurrenceId}")
                appendLine("target=${Instant.ofEpochMilli(session.targetWallMillis)}")
                appendLine(
                    "latency trigger=${formatMillis(session.triggerDelayMillis)} " +
                        "foreground=${formatMillis(session.triggerToForegroundMillis)} " +
                        "audio=${formatMillis(session.triggerToAudioMillis)} " +
                        "ui=${formatMillis(session.triggerToUiMillis)}",
                )
                appendLine("terminal=${session.terminalAction ?: "NONE"}; serviceRecoveries=${session.serviceRecoveryCount}")
                appendLine("events:")
                session.events.forEach { event ->
                    val detail = event.detail?.let { " [$it]" }.orEmpty()
                    appendLine("  ${Instant.ofEpochMilli(event.wallMillis)} ${event.type}$detail")
                }
                appendLine()
            }
        }
    }

    fun clearHistory() {
        runCatching { prefs.edit().remove(KEY_SESSIONS).apply() }
    }

    private fun newSession(occurrence: WakeOccurrence): JSONObject = JSONObject().apply {
        put(KEY_OCCURRENCE_ID, occurrence.id.value)
        put(KEY_SCHEDULE_ID, occurrence.wakeScheduleId.value)
        put(KEY_OCCURRENCE_KIND, occurrence.kind.name)
        put(KEY_TARGET_WALL_MS, occurrence.scheduledAt.toInstant().toEpochMilli())
        put(KEY_SERVICE_RECOVERY_COUNT, 0)
        put(KEY_EVENTS, JSONArray())
    }

    private fun mark(
        occurrenceId: WakeOccurrenceId,
        wallKey: String,
        elapsedKey: String,
        eventType: String,
    ) = safelyMutate { sessions ->
        sessions.findSession(occurrenceId)?.apply {
            put(wallKey, System.currentTimeMillis())
            put(elapsedKey, SystemClock.elapsedRealtime())
            appendEvent(this, eventType)
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
            appendEvent(this, action.name)
        }
    }

    private fun appendEvent(
        session: JSONObject,
        type: String,
        detail: String? = null,
    ) {
        val events = session.optJSONArray(KEY_EVENTS) ?: JSONArray().also { session.put(KEY_EVENTS, it) }
        events.put(
            JSONObject().apply {
                put(KEY_EVENT_TYPE, type)
                put(KEY_EVENT_WALL_MS, System.currentTimeMillis())
                put(KEY_EVENT_ELAPSED_MS, SystemClock.elapsedRealtime())
                if (detail != null) put(KEY_EVENT_DETAIL, detail)
            },
        )
        if (events.length() > MAX_EVENTS_PER_SESSION) {
            val trimmed = JSONArray()
            val first = events.length() - MAX_EVENTS_PER_SESSION
            for (index in first until events.length()) {
                trimmed.put(events.getJSONObject(index))
            }
            session.put(KEY_EVENTS, trimmed)
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
        val array = JSONArray().apply {
            sessions.take(MAX_SESSIONS).forEach { put(it) }
        }
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
            scenario = json.optString(KEY_SCENARIO).takeIf { it.isNotBlank() },
            expectFullScreen = json.optBoolean(KEY_EXPECT_FULL_SCREEN, false),
            targetWallMillis = json.optLong(KEY_TARGET_WALL_MS, 0),
            expectedWallMillis = json.optionalLong(KEY_EXPECTED_WALL_MS),
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
            events = json.events(),
        )
    }

    private fun JSONObject.events(): List<TimingEventSnapshot> {
        val events = optJSONArray(KEY_EVENTS) ?: return emptyList()
        return buildList {
            for (index in 0 until events.length()) {
                val event = events.optJSONObject(index) ?: continue
                val type = event.optString(KEY_EVENT_TYPE).takeIf { it.isNotBlank() } ?: continue
                add(
                    TimingEventSnapshot(
                        type = type,
                        wallMillis = event.optLong(KEY_EVENT_WALL_MS, 0),
                        elapsedMillis = event.optionalLong(KEY_EVENT_ELAPSED_MS),
                        detail = event.optString(KEY_EVENT_DETAIL).takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    private fun JSONObject.optionalLong(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private enum class TerminalAction {
        STOPPED,
        SNOOZED,
    }

    companion object {
        const val SCENARIO_NORMAL_T_PLUS_2M = "NORMAL_T_PLUS_2M"
        const val SCENARIO_SNOOZE_REPLACEMENT = "SNOOZE_REPLACEMENT"
        const val MISSED_RECEIVER_GRACE_MS = 60_000L
        const val DELIVERY_STAGE_GRACE_MS = 5_000L

        private val LOCK = Any()
        private const val MAX_SESSIONS = 24
        private const val MAX_EVENTS_PER_SESSION = 64
        private const val PREFS_NAME = "wake-reliability-journal"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_OCCURRENCE_ID = "occurrence_id"
        private const val KEY_SCHEDULE_ID = "schedule_id"
        private const val KEY_OCCURRENCE_KIND = "occurrence_kind"
        private const val KEY_SCENARIO = "scenario"
        private const val KEY_EXPECT_FULL_SCREEN = "expect_full_screen"
        private const val KEY_TARGET_WALL_MS = "target_wall_ms"
        private const val KEY_EXPECTED_WALL_MS = "expected_wall_ms"
        private const val KEY_RECEIVER_WALL_MS = "receiver_wall_ms"
        private const val KEY_RECEIVER_ELAPSED_MS = "receiver_elapsed_ms"
        private const val KEY_FOREGROUND_WALL_MS = "foreground_wall_ms"
        private const val KEY_FOREGROUND_ELAPSED_MS = "foreground_elapsed_ms"
        private const val KEY_AUDIO_WALL_MS = "audio_wall_ms"
        private const val KEY_AUDIO_ELAPSED_MS = "audio_elapsed_ms"
        private const val KEY_UI_WALL_MS = "ui_wall_ms"
        private const val KEY_UI_ELAPSED_MS = "ui_elapsed_ms"
        private const val KEY_TERMINAL_ACTION = "terminal_action"
        private const val KEY_TERMINAL_WALL_MS = "terminal_wall_ms"
        private const val KEY_TERMINAL_ELAPSED_MS = "terminal_elapsed_ms"
        private const val KEY_SERVICE_RECOVERY_COUNT = "service_recovery_count"
        private const val KEY_LAST_RECOVERY_WALL_MS = "last_recovery_wall_ms"
        private const val KEY_LAST_RECOVERY_ELAPSED_MS = "last_recovery_elapsed_ms"
        private const val KEY_EVENTS = "events"
        private const val KEY_EVENT_TYPE = "type"
        private const val KEY_EVENT_WALL_MS = "wall_ms"
        private const val KEY_EVENT_ELAPSED_MS = "elapsed_ms"
        private const val KEY_EVENT_DETAIL = "detail"
        private const val EVENT_EXPECTED = "EXPECTED"
        private const val EVENT_RECEIVER = "RECEIVER"
        private const val EVENT_FOREGROUND = "FOREGROUND"
        private const val EVENT_AUDIO_STARTED = "AUDIO_STARTED"
        private const val EVENT_UI_VISIBLE = "UI_VISIBLE"
        private const val EVENT_SERVICE_RECOVERED = "SERVICE_RECOVERED"
        private const val EVENT_RECONCILED = "RECONCILED"
        private const val EVENT_CAPABILITIES = "CAPABILITIES"
    }
}

data class TimingEventSnapshot(
    val type: String,
    val wallMillis: Long,
    val elapsedMillis: Long?,
    val detail: String?,
)

data class TimingSnapshot(
    val occurrenceId: String,
    val scheduleId: String,
    val occurrenceKind: String,
    val scenario: String?,
    val expectFullScreen: Boolean,
    val targetWallMillis: Long,
    val expectedWallMillis: Long?,
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
    val events: List<TimingEventSnapshot>,
) {
    val triggerDelayMillis: Long? = receiverWallMillis?.minus(targetWallMillis)
    val triggerToForegroundMillis: Long? = elapsedDelta(foregroundElapsedMillis)
    val triggerToAudioMillis: Long? = elapsedDelta(audioElapsedMillis)
    val triggerToUiMillis: Long? = elapsedDelta(uiElapsedMillis)
    val triggerToTerminalMillis: Long? = elapsedDelta(terminalElapsedMillis)

    fun state(
        nowWallMillis: Long = System.currentTimeMillis(),
        missedReceiverGraceMillis: Long = WakeTimingTrace.MISSED_RECEIVER_GRACE_MS,
        deliveryStageGraceMillis: Long = WakeTimingTrace.DELIVERY_STAGE_GRACE_MS,
    ): ReliabilityState = when {
        terminalAction == "STOPPED" -> ReliabilityState.STOPPED
        terminalAction == "SNOOZED" -> ReliabilityState.SNOOZED
        receiverWallMillis == null && nowWallMillis > targetWallMillis + missedReceiverGraceMillis -> ReliabilityState.MISSED_RECEIVER
        receiverWallMillis == null -> ReliabilityState.EXPECTED
        audioWallMillis == null && nowWallMillis > receiverWallMillis + deliveryStageGraceMillis -> ReliabilityState.AUDIO_TIMEOUT
        audioWallMillis == null -> ReliabilityState.RECEIVED_NO_AUDIO_YET
        uiWallMillis == null && expectFullScreen && nowWallMillis > receiverWallMillis + deliveryStageGraceMillis -> ReliabilityState.UI_TIMEOUT
        uiWallMillis == null -> ReliabilityState.AUDIBLE_NO_UI_YET
        else -> ReliabilityState.ACTIVE
    }

    private fun elapsedDelta(stageElapsedMillis: Long?): Long? =
        receiverElapsedMillis?.let { receiver -> stageElapsedMillis?.minus(receiver) }
}

enum class ReliabilityState {
    EXPECTED,
    MISSED_RECEIVER,
    RECEIVED_NO_AUDIO_YET,
    AUDIO_TIMEOUT,
    AUDIBLE_NO_UI_YET,
    UI_TIMEOUT,
    ACTIVE,
    STOPPED,
    SNOOZED,
}

private fun formatMillis(value: Long?): String = value?.let { "${it}ms" } ?: "—"

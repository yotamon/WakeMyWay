package com.wakemyway.app.product.history

import android.content.Context
import android.util.AtomicFile
import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

/**
 * Credential-protected local Wake history.
 *
 * History is explicitly non-critical. Corruption fails open to an empty history and a subsequent
 * successful record atomically repairs the document. A failed write must never affect alarm
 * execution or terminal Stop/Snooze acknowledgement.
 */
class WakeHistoryRepository(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    private val atomicFile = AtomicFile(File(context.filesDir, fileName))

    init {
        require(maxEntries > 0) { "Wake history max entries must be positive" }
    }

    @Synchronized
    fun list(): List<WakeHistoryEntry> = readDocument()
        .sortedWith(ENTRY_ORDER)

    /**
     * Records a terminal occurrence at most once. The first durable record for an occurrence wins.
     *
     * This makes terminal callbacks idempotent across duplicate surfaces or process/lifecycle noise.
     */
    @Synchronized
    fun record(entry: WakeHistoryEntry) {
        val current = readDocument()
        if (current.any { it.occurrenceId == entry.occurrenceId }) return

        val next = (current + entry)
            .sortedWith(ENTRY_ORDER)
            .take(maxEntries)
        writeDocument(next)
    }

    @Synchronized
    fun clear() {
        writeDocument(emptyList())
    }

    private fun readDocument(): List<WakeHistoryEntry> {
        val text = try {
            atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            return emptyList()
        } catch (_: Throwable) {
            return emptyList()
        }

        if (text.isBlank()) return emptyList()
        return runCatching { decodeDocument(JSONObject(text)) }
            .getOrDefault(emptyList())
    }

    private fun writeDocument(entries: List<WakeHistoryEntry>) {
        val bytes = encodeDocument(entries).toString().toByteArray(Charsets.UTF_8)
        val output = atomicFile.startWrite()
        try {
            output.write(bytes)
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun encodeDocument(entries: List<WakeHistoryEntry>): JSONObject = JSONObject().apply {
        put(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
        put(
            KEY_ENTRIES,
            JSONArray().apply {
                entries.forEach { put(encodeEntry(it)) }
            },
        )
    }

    private fun decodeDocument(root: JSONObject): List<WakeHistoryEntry> {
        val schemaVersion = root.getInt(KEY_SCHEMA_VERSION)
        require(schemaVersion in SUPPORTED_SCHEMA_VERSIONS) {
            "Unsupported Wake history schema version"
        }
        val values = root.getJSONArray(KEY_ENTRIES)
        val entries = buildList(values.length()) {
            repeat(values.length()) { index ->
                add(decodeEntry(values.getJSONObject(index), schemaVersion))
            }
        }
        require(entries.map { it.occurrenceId }.distinct().size == entries.size) {
            "Wake history contains duplicate occurrence ids"
        }
        return entries.sortedWith(ENTRY_ORDER).take(maxEntries)
    }

    private fun encodeEntry(entry: WakeHistoryEntry): JSONObject = JSONObject().apply {
        put(KEY_SESSION_ID, entry.sessionId.value)
        put(KEY_OCCURRENCE_ID, entry.occurrenceId.value)
        put(KEY_SCHEDULE_ID, entry.scheduleId.value)
        put(KEY_OCCURRENCE_KIND, entry.occurrenceKind.name)
        put(KEY_SCHEDULE_REVISION, entry.scheduleRevision)
        put(KEY_SCHEDULED_AT, entry.scheduledAt.toString())
        entry.scheduledLocalDateTime?.let {
            put(KEY_SCHEDULED_LOCAL_DATE_TIME, it.toString())
            put(KEY_SCHEDULED_ZONE_ID, requireNotNull(entry.scheduledZoneId).id)
        }
        put(KEY_STARTED_AT, entry.startedAt.toString())
        put(KEY_FINISHED_AT, entry.finishedAt.toString())
        put(KEY_TERMINAL_REASON, entry.terminalReason.name)
        entry.replacementOccurrenceId?.let { put(KEY_REPLACEMENT_OCCURRENCE_ID, it.value) }
        entry.behavior?.let {
            put(KEY_BEHAVIOR, encodeBehavior(it))
            put(
                KEY_BEHAVIOR_TIMING_ORIGIN,
                requireNotNull(entry.behaviorTimingOrigin).name,
            )
        }
    }

    private fun decodeEntry(
        json: JSONObject,
        schemaVersion: Int,
    ): WakeHistoryEntry {
        val behavior = json.optJSONObject(KEY_BEHAVIOR)?.let(::decodeBehavior)
        val behaviorTimingOrigin = when {
            behavior == null -> null
            schemaVersion == 1 -> WakeHistoryBehaviorTimingOrigin.LEGACY_UNSPECIFIED
            else -> WakeHistoryBehaviorTimingOrigin.valueOf(
                json.getString(KEY_BEHAVIOR_TIMING_ORIGIN),
            )
        }
        val scheduledLocalDateTimeValue = json.optString(KEY_SCHEDULED_LOCAL_DATE_TIME)
            .takeIf(String::isNotBlank)
        val scheduledZoneIdValue = json.optString(KEY_SCHEDULED_ZONE_ID)
            .takeIf(String::isNotBlank)
        require((scheduledLocalDateTimeValue == null) == (scheduledZoneIdValue == null)) {
            "Wake history local schedule metadata is incomplete"
        }
        val scheduledLocalDateTime = scheduledLocalDateTimeValue?.let(LocalDateTime::parse)
        val scheduledZoneId = scheduledZoneIdValue?.let(ZoneId::of)

        return WakeHistoryEntry(
            sessionId = WakeSessionId(json.getString(KEY_SESSION_ID)),
            occurrenceId = WakeOccurrenceId(json.getString(KEY_OCCURRENCE_ID)),
            scheduleId = WakeScheduleId(json.getString(KEY_SCHEDULE_ID)),
            occurrenceKind = WakeOccurrenceKind.valueOf(json.getString(KEY_OCCURRENCE_KIND)),
            scheduleRevision = json.getLong(KEY_SCHEDULE_REVISION),
            scheduledAt = Instant.parse(json.getString(KEY_SCHEDULED_AT)),
            scheduledLocalDateTime = scheduledLocalDateTime,
            scheduledZoneId = scheduledZoneId,
            startedAt = Instant.parse(json.getString(KEY_STARTED_AT)),
            finishedAt = Instant.parse(json.getString(KEY_FINISHED_AT)),
            terminalReason = WakeHistoryTerminalReason.valueOf(json.getString(KEY_TERMINAL_REASON)),
            replacementOccurrenceId = json.optString(KEY_REPLACEMENT_OCCURRENCE_ID)
                .takeIf(String::isNotBlank)
                ?.let(::WakeOccurrenceId),
            behavior = behavior,
            behaviorTimingOrigin = behaviorTimingOrigin,
        )
    }

    private fun encodeBehavior(behavior: WakeBehaviorObservation): JSONObject = JSONObject().apply {
        put(KEY_POLICY_VERSION, behavior.policyVersion)
        behavior.timeToFirstEngagement?.let {
            put(KEY_FIRST_ENGAGEMENT_MILLIS, it.toMillis())
        }
        behavior.timeToMeaningfulMovement?.let {
            put(KEY_MEANINGFUL_MOVEMENT_MILLIS, it.toMillis())
        }
        behavior.timeToActivationCompletion?.let {
            put(KEY_ACTIVATION_COMPLETION_MILLIS, it.toMillis())
        }
        put(KEY_MAX_INTERVENTION_DEPTH, behavior.maxInterventionDepth)
    }

    private fun decodeBehavior(json: JSONObject): WakeBehaviorObservation = WakeBehaviorObservation(
        policyVersion = json.getInt(KEY_POLICY_VERSION),
        timeToFirstEngagement = json.optionalDuration(KEY_FIRST_ENGAGEMENT_MILLIS),
        timeToMeaningfulMovement = json.optionalDuration(KEY_MEANINGFUL_MOVEMENT_MILLIS),
        timeToActivationCompletion = json.optionalDuration(KEY_ACTIVATION_COMPLETION_MILLIS),
        maxInterventionDepth = json.getInt(KEY_MAX_INTERVENTION_DEPTH),
    )

    private fun JSONObject.optionalDuration(key: String): Duration? =
        if (has(key)) Duration.ofMillis(getLong(key)) else null

    companion object {
        const val DEFAULT_FILE_NAME = "wake-history-v1.json"
        const val DEFAULT_MAX_ENTRIES = 512

        private const val SCHEMA_VERSION = 3
        private val SUPPORTED_SCHEMA_VERSIONS = setOf(1, 2, SCHEMA_VERSION)
        private const val KEY_SCHEMA_VERSION = "schemaVersion"
        private const val KEY_ENTRIES = "entries"
        private const val KEY_SESSION_ID = "sessionId"
        private const val KEY_OCCURRENCE_ID = "occurrenceId"
        private const val KEY_SCHEDULE_ID = "scheduleId"
        private const val KEY_OCCURRENCE_KIND = "occurrenceKind"
        private const val KEY_SCHEDULE_REVISION = "scheduleRevision"
        private const val KEY_SCHEDULED_AT = "scheduledAt"
        private const val KEY_SCHEDULED_LOCAL_DATE_TIME = "scheduledLocalDateTime"
        private const val KEY_SCHEDULED_ZONE_ID = "scheduledZoneId"
        private const val KEY_STARTED_AT = "startedAt"
        private const val KEY_FINISHED_AT = "finishedAt"
        private const val KEY_TERMINAL_REASON = "terminalReason"
        private const val KEY_REPLACEMENT_OCCURRENCE_ID = "replacementOccurrenceId"
        private const val KEY_BEHAVIOR = "behavior"
        private const val KEY_BEHAVIOR_TIMING_ORIGIN = "behaviorTimingOrigin"
        private const val KEY_POLICY_VERSION = "policyVersion"
        private const val KEY_FIRST_ENGAGEMENT_MILLIS = "firstEngagementMillis"
        private const val KEY_MEANINGFUL_MOVEMENT_MILLIS = "meaningfulMovementMillis"
        private const val KEY_ACTIVATION_COMPLETION_MILLIS = "activationCompletionMillis"
        private const val KEY_MAX_INTERVENTION_DEPTH = "maxInterventionDepth"

        private val ENTRY_ORDER = compareByDescending<WakeHistoryEntry> { it.finishedAt }
            .thenByDescending { it.occurrenceId.value }
    }
}

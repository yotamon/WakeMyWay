package com.wakemyway.app.alarm

import com.wakemyway.core.schedule.LocalTimeResolution
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.json.JSONObject

/**
 * Minimal non-sensitive state required to recover wake delivery before first unlock.
 * Private context, transcripts, prompts and personalized speech must never be added here.
 *
 * [enabled] is persisted as a cancellation tombstone. A disabled snapshot intentionally
 * keeps the non-sensitive schedule definition so a crash cannot turn a user cancellation
 * back into a future alarm during reconciliation.
 */
data class CriticalWakeSnapshot(
    val schedule: WakeSchedule,
    val nextOccurrence: WakeOccurrence?,
    val activeOccurrence: WakeOccurrence?,
    val registeredOccurrenceId: WakeOccurrenceId?,
    val generation: Long,
    val enabled: Boolean = true,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
) {
    init {
        require(schemaVersion == CURRENT_SCHEMA_VERSION) { "Unsupported snapshot schema $schemaVersion" }
        require(generation >= 0) { "Snapshot generation must be non-negative" }
        require(nextOccurrence == null || activeOccurrence == null) {
            "A critical snapshot cannot have both a next and active occurrence"
        }
        require(registeredOccurrenceId == null || registeredOccurrenceId == nextOccurrence?.id) {
            "Registered occurrence must match the persisted next occurrence"
        }
        require(enabled || (nextOccurrence == null && activeOccurrence == null && registeredOccurrenceId == null)) {
            "A disabled wake schedule cannot retain an occurrence or OS registration"
        }
    }

    fun encode(): String = JSONObject().apply {
        put("schemaVersion", schemaVersion)
        put("generation", generation)
        put("enabled", enabled)
        put("schedule", schedule.toJson())
        put("nextOccurrence", nextOccurrence?.toJson())
        put("activeOccurrence", activeOccurrence?.toJson())
        put("registeredOccurrenceId", registeredOccurrenceId?.value ?: JSONObject.NULL)
    }.toString()

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun decode(raw: String): CriticalWakeSnapshot {
            val json = JSONObject(raw)
            val schema = json.getInt("schemaVersion")
            require(schema == CURRENT_SCHEMA_VERSION) { "Unsupported snapshot schema $schema" }
            val schedule = scheduleFromJson(json.getJSONObject("schedule"))
            return CriticalWakeSnapshot(
                schedule = schedule,
                nextOccurrence = json.optJSONObject("nextOccurrence")?.let { occurrenceFromJson(it, schedule) },
                activeOccurrence = json.optJSONObject("activeOccurrence")?.let { occurrenceFromJson(it, schedule) },
                registeredOccurrenceId = if (json.isNull("registeredOccurrenceId")) {
                    null
                } else {
                    WakeOccurrenceId(json.getString("registeredOccurrenceId"))
                },
                generation = json.getLong("generation"),
                enabled = if (json.has("enabled")) json.getBoolean("enabled") else true,
                schemaVersion = schema,
            )
        }

        private fun WakeSchedule.toJson(): JSONObject = JSONObject().apply {
            put("id", id.value)
            put("zoneId", zoneId.id)
            put("revision", revision)
            put("timesByDay", JSONObject().also { times ->
                timesByDay.forEach { (day, time) -> times.put(day.name, time.toString()) }
            })
        }

        private fun WakeOccurrence.toJson(): JSONObject = JSONObject().apply {
            put("id", id.value)
            put("kind", kind.name)
            put("scheduledLocalDateTime", scheduledLocalDateTime.toString())
            put("scheduledEpochMillis", scheduledAt.toInstant().toEpochMilli())
            put("scheduleRevision", scheduleRevision)
            put("localTimeResolution", localTimeResolution.name)
        }

        private fun scheduleFromJson(json: JSONObject): WakeSchedule {
            val timesJson = json.getJSONObject("timesByDay")
            val times = buildMap {
                timesJson.keys().forEach { key ->
                    put(DayOfWeek.valueOf(key), LocalTime.parse(timesJson.getString(key)))
                }
            }
            return WakeSchedule(
                id = WakeScheduleId(json.getString("id")),
                zoneId = ZoneId.of(json.getString("zoneId")),
                timesByDay = times,
                revision = json.getLong("revision"),
            )
        }

        private fun occurrenceFromJson(json: JSONObject, schedule: WakeSchedule): WakeOccurrence {
            val instant = Instant.ofEpochMilli(json.getLong("scheduledEpochMillis"))
            return WakeOccurrence(
                id = WakeOccurrenceId(json.getString("id")),
                wakeScheduleId = schedule.id,
                kind = WakeOccurrenceKind.valueOf(json.getString("kind")),
                scheduledLocalDateTime = LocalDateTime.parse(json.getString("scheduledLocalDateTime")),
                scheduledAt = instant.atZone(schedule.zoneId),
                scheduleRevision = json.getLong("scheduleRevision"),
                localTimeResolution = LocalTimeResolution.valueOf(json.getString("localTimeResolution")),
            )
        }
    }
}

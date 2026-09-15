package com.wakemyway.app.alarm

import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import com.wakemyway.core.schedule.LocalTimeResolution
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal device-protected authority for every locally scheduled alarm.
 *
 * Private product metadata such as labels, Tomorrow Contract text, First Move text, transcripts and
 * account information must never be added here. A tiny non-sensitive [CriticalWakePolicy] is stored
 * per slot because sound/voice/snooze behavior must remain deterministic before credential unlock.
 * Multiple future schedule slots may coexist, but exactly one wake execution may be active at a time.
 */
data class CriticalAlarmState(
    val slots: Map<WakeScheduleId, CriticalScheduleSlot>,
    val activeOccurrence: WakeOccurrence?,
    val generation: Long,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
) {
    init {
        require(schemaVersion == CURRENT_SCHEMA_VERSION) { "Unsupported critical alarm schema $schemaVersion" }
        require(generation >= 0) { "Critical alarm generation must be non-negative" }
        slots.forEach { (id, slot) ->
            require(id == slot.schedule.id) { "Critical slot key must match its WakeSchedule id" }
        }
        activeOccurrence?.let { active ->
            val owner = slots[active.wakeScheduleId]
            require(owner != null && owner.enabled) {
                "Active occurrence must belong to an enabled critical slot"
            }
            require(owner.schedule.revision == active.scheduleRevision) {
                "Active occurrence revision must match its owning schedule"
            }
            require(owner.nextOccurrence == null && owner.registeredOccurrenceId == null) {
                "Active schedule slot cannot retain a future/registered occurrence"
            }
        }
    }

    fun encode(): String = JSONObject().apply {
        put(KEY_SCHEMA_VERSION, schemaVersion)
        put(KEY_GENERATION, generation)
        put(
            KEY_SLOTS,
            JSONArray().apply {
                slots.values.forEach { slot -> put(slot.toJson()) }
            },
        )
        put(KEY_ACTIVE_OCCURRENCE, activeOccurrence?.toJson() ?: JSONObject.NULL)
    }.toString()

    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
        private const val PREVIOUS_MULTI_SLOT_SCHEMA_VERSION = 2

        private const val KEY_SCHEMA_VERSION = "schemaVersion"
        private const val KEY_GENERATION = "generation"
        private const val KEY_SLOTS = "slots"
        private const val KEY_ACTIVE_OCCURRENCE = "activeOccurrence"

        fun decodeOrMigrate(raw: String): CriticalAlarmState {
            val json = JSONObject(raw)
            return when (val schema = json.getInt(KEY_SCHEMA_VERSION)) {
                CURRENT_SCHEMA_VERSION -> decodeV3(json)
                PREVIOUS_MULTI_SLOT_SCHEMA_VERSION -> migrateV2(json)
                CriticalWakeSnapshot.CURRENT_SCHEMA_VERSION -> migrateLegacy(
                    CriticalWakeSnapshot.decode(raw),
                )
                else -> error("Unsupported critical alarm schema $schema")
            }
        }

        private fun decodeV3(json: JSONObject): CriticalAlarmState = decodeMultiSlot(
            json = json,
            includePolicy = true,
        )

        /** Schema 2 already had isolated schedule slots but no pre-unlock execution policy. */
        private fun migrateV2(json: JSONObject): CriticalAlarmState = decodeMultiSlot(
            json = json,
            includePolicy = false,
        )

        private fun decodeMultiSlot(
            json: JSONObject,
            includePolicy: Boolean,
        ): CriticalAlarmState {
            val slotArray = json.getJSONArray(KEY_SLOTS)
            val slots = buildMap {
                repeat(slotArray.length()) { index ->
                    val slot = slotFromJson(
                        json = slotArray.getJSONObject(index),
                        includePolicy = includePolicy,
                    )
                    require(put(slot.schedule.id, slot) == null) {
                        "Duplicate critical schedule slot ${slot.schedule.id.value}"
                    }
                }
            }
            val activeJson = json.optJSONObject(KEY_ACTIVE_OCCURRENCE)
            val active = activeJson?.let { occurrenceJson ->
                val scheduleId = WakeScheduleId(occurrenceJson.getString(KEY_WAKE_SCHEDULE_ID))
                val schedule = requireNotNull(slots[scheduleId]?.schedule) {
                    "Active occurrence refers to missing schedule ${scheduleId.value}"
                }
                occurrenceFromJson(occurrenceJson, schedule)
            }
            return CriticalAlarmState(
                slots = slots,
                activeOccurrence = active,
                generation = json.getLong(KEY_GENERATION),
            )
        }

        private fun migrateLegacy(snapshot: CriticalWakeSnapshot): CriticalAlarmState =
            CriticalAlarmState(
                slots = mapOf(
                    snapshot.schedule.id to CriticalScheduleSlot(
                        schedule = snapshot.schedule,
                        nextOccurrence = snapshot.nextOccurrence,
                        registeredOccurrenceId = snapshot.registeredOccurrenceId,
                        enabled = snapshot.enabled,
                        policy = CriticalWakePolicy.DEFAULT,
                    ),
                ),
                activeOccurrence = snapshot.activeOccurrence,
                generation = snapshot.generation,
            )

        private fun CriticalScheduleSlot.toJson(): JSONObject = JSONObject().apply {
            put(KEY_SCHEDULE, schedule.toJson())
            put(KEY_NEXT_OCCURRENCE, nextOccurrence?.toJson() ?: JSONObject.NULL)
            put(KEY_REGISTERED_OCCURRENCE_ID, registeredOccurrenceId?.value ?: JSONObject.NULL)
            put(KEY_ENABLED, enabled)
            put(KEY_POLICY, policy.toJson())
        }

        private fun slotFromJson(
            json: JSONObject,
            includePolicy: Boolean,
        ): CriticalScheduleSlot {
            val schedule = scheduleFromJson(json.getJSONObject(KEY_SCHEDULE))
            return CriticalScheduleSlot(
                schedule = schedule,
                nextOccurrence = json.optJSONObject(KEY_NEXT_OCCURRENCE)?.let {
                    occurrenceFromJson(it, schedule)
                },
                registeredOccurrenceId = if (json.isNull(KEY_REGISTERED_OCCURRENCE_ID)) {
                    null
                } else {
                    WakeOccurrenceId(json.getString(KEY_REGISTERED_OCCURRENCE_ID))
                },
                enabled = json.getBoolean(KEY_ENABLED),
                policy = if (includePolicy) {
                    json.optJSONObject(KEY_POLICY)?.let(::policyFromJson) ?: CriticalWakePolicy.DEFAULT
                } else {
                    CriticalWakePolicy.DEFAULT
                },
            )
        }

        private fun CriticalWakePolicy.toJson(): JSONObject = JSONObject().apply {
            put(KEY_SOUND_ID, soundId.value)
            put(KEY_VOICE_CHECK_IN_ENABLED, voiceCheckInEnabled)
            put(KEY_CHARACTER_ID, characterId.value)
            put(KEY_VOICE_STYLE, voiceStyle.name)
            put(KEY_SNOOZE_ENABLED, snoozeEnabled)
            put(KEY_SNOOZE_DURATION_SECONDS, snoozeDuration.seconds)
        }

        private fun policyFromJson(json: JSONObject): CriticalWakePolicy = CriticalWakePolicy(
            soundId = WakeSoundId(json.getString(KEY_SOUND_ID)),
            voiceCheckInEnabled = json.getBoolean(KEY_VOICE_CHECK_IN_ENABLED),
            characterId = CharacterId(json.getString(KEY_CHARACTER_ID)),
            voiceStyle = VoiceStyle.valueOf(json.getString(KEY_VOICE_STYLE)),
            snoozeEnabled = json.getBoolean(KEY_SNOOZE_ENABLED),
            snoozeDuration = Duration.ofSeconds(json.getLong(KEY_SNOOZE_DURATION_SECONDS)),
        )

        private fun WakeSchedule.toJson(): JSONObject = JSONObject().apply {
            put(KEY_ID, id.value)
            put(KEY_ZONE_ID, zoneId.id)
            put(KEY_REVISION, revision)
            put(KEY_COMPLETION_POLICY, completionPolicy.name)
            put(
                KEY_TIMES_BY_DAY,
                JSONObject().also { times ->
                    timesByDay.forEach { (day, time) -> times.put(day.name, time.toString()) }
                },
            )
            put(KEY_ONE_SHOT_DATE, oneShotDate?.toString() ?: JSONObject.NULL)
        }

        private fun scheduleFromJson(json: JSONObject): WakeSchedule {
            val timesJson = json.getJSONObject(KEY_TIMES_BY_DAY)
            val times = buildMap {
                timesJson.keys().forEach { key ->
                    put(DayOfWeek.valueOf(key), LocalTime.parse(timesJson.getString(key)))
                }
            }
            return WakeSchedule(
                id = WakeScheduleId(json.getString(KEY_ID)),
                zoneId = ZoneId.of(json.getString(KEY_ZONE_ID)),
                timesByDay = times,
                revision = json.getLong(KEY_REVISION),
                completionPolicy = WakeCompletionPolicy.valueOf(json.getString(KEY_COMPLETION_POLICY)),
                oneShotDate = if (json.isNull(KEY_ONE_SHOT_DATE)) {
                    null
                } else {
                    LocalDate.parse(json.getString(KEY_ONE_SHOT_DATE))
                },
            )
        }

        private fun WakeOccurrence.toJson(): JSONObject = JSONObject().apply {
            put(KEY_ID, id.value)
            put(KEY_WAKE_SCHEDULE_ID, wakeScheduleId.value)
            put(KEY_KIND, kind.name)
            put(KEY_SCHEDULED_LOCAL_DATE_TIME, scheduledLocalDateTime.toString())
            put(KEY_SCHEDULED_EPOCH_MILLIS, scheduledAt.toInstant().toEpochMilli())
            put(KEY_SCHEDULE_REVISION, scheduleRevision)
            put(KEY_LOCAL_TIME_RESOLUTION, localTimeResolution.name)
        }

        private fun occurrenceFromJson(json: JSONObject, schedule: WakeSchedule): WakeOccurrence {
            val scheduleId = if (json.has(KEY_WAKE_SCHEDULE_ID)) {
                WakeScheduleId(json.getString(KEY_WAKE_SCHEDULE_ID))
            } else {
                schedule.id
            }
            require(scheduleId == schedule.id) {
                "Occurrence owner does not match critical schedule slot"
            }
            val instant = Instant.ofEpochMilli(json.getLong(KEY_SCHEDULED_EPOCH_MILLIS))
            return WakeOccurrence(
                id = WakeOccurrenceId(json.getString(KEY_ID)),
                wakeScheduleId = scheduleId,
                kind = WakeOccurrenceKind.valueOf(json.getString(KEY_KIND)),
                scheduledLocalDateTime = LocalDateTime.parse(json.getString(KEY_SCHEDULED_LOCAL_DATE_TIME)),
                scheduledAt = instant.atZone(schedule.zoneId),
                scheduleRevision = json.getLong(KEY_SCHEDULE_REVISION),
                localTimeResolution = LocalTimeResolution.valueOf(json.getString(KEY_LOCAL_TIME_RESOLUTION)),
            )
        }

        private const val KEY_SCHEDULE = "schedule"
        private const val KEY_NEXT_OCCURRENCE = "nextOccurrence"
        private const val KEY_REGISTERED_OCCURRENCE_ID = "registeredOccurrenceId"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_POLICY = "policy"
        private const val KEY_SOUND_ID = "soundId"
        private const val KEY_VOICE_CHECK_IN_ENABLED = "voiceCheckInEnabled"
        private const val KEY_CHARACTER_ID = "characterId"
        private const val KEY_VOICE_STYLE = "voiceStyle"
        private const val KEY_SNOOZE_ENABLED = "snoozeEnabled"
        private const val KEY_SNOOZE_DURATION_SECONDS = "snoozeDurationSeconds"
        private const val KEY_ID = "id"
        private const val KEY_ZONE_ID = "zoneId"
        private const val KEY_REVISION = "revision"
        private const val KEY_COMPLETION_POLICY = "completionPolicy"
        private const val KEY_TIMES_BY_DAY = "timesByDay"
        private const val KEY_ONE_SHOT_DATE = "oneShotDate"
        private const val KEY_WAKE_SCHEDULE_ID = "wakeScheduleId"
        private const val KEY_KIND = "kind"
        private const val KEY_SCHEDULED_LOCAL_DATE_TIME = "scheduledLocalDateTime"
        private const val KEY_SCHEDULED_EPOCH_MILLIS = "scheduledEpochMillis"
        private const val KEY_SCHEDULE_REVISION = "scheduleRevision"
        private const val KEY_LOCAL_TIME_RESOLUTION = "localTimeResolution"
    }
}

data class CriticalScheduleSlot(
    val schedule: WakeSchedule,
    val nextOccurrence: WakeOccurrence?,
    val registeredOccurrenceId: WakeOccurrenceId?,
    val enabled: Boolean = true,
    val policy: CriticalWakePolicy = CriticalWakePolicy.DEFAULT,
) {
    init {
        nextOccurrence?.let { occurrence ->
            require(occurrence.wakeScheduleId == schedule.id) {
                "Next occurrence must belong to its critical schedule slot"
            }
            require(occurrence.scheduleRevision == schedule.revision) {
                "Next occurrence revision must match its critical schedule"
            }
        }
        require(registeredOccurrenceId == null || registeredOccurrenceId == nextOccurrence?.id) {
            "Registered occurrence must match the slot's persisted next occurrence"
        }
        require(enabled || (nextOccurrence == null && registeredOccurrenceId == null)) {
            "A disabled critical schedule slot cannot retain an occurrence or OS registration"
        }
    }
}
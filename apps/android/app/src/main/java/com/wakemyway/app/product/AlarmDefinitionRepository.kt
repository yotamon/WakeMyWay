package com.wakemyway.app.product

import android.content.Context
import android.util.AtomicFile
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.SnoozePolicy
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import java.io.File
import java.io.FileNotFoundException
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

/**
 * Credential-protected, local-first product storage for consumer alarm definitions.
 *
 * This repository intentionally does not use device-protected storage. Alarm labels, sound/voice
 * preferences and other consumer metadata are normal product state. Alarm Kernel separately owns
 * the minimal critical state required for Direct Boot delivery.
 */
class AlarmDefinitionRepository(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
) {
    private val atomicFile = AtomicFile(File(context.filesDir, fileName))

    @Synchronized
    fun list(): List<AlarmDefinition> = readDocument()

    @Synchronized
    fun get(id: AlarmDefinitionId): AlarmDefinition? =
        readDocument().firstOrNull { it.id == id }

    /**
     * Inserts a new alarm or replaces the same alarm with a strictly newer revision.
     * Repeating the exact same object is idempotent.
     */
    @Synchronized
    fun upsert(definition: AlarmDefinition): AlarmDefinition {
        val current = readDocument().toMutableList()
        val index = current.indexOfFirst { it.id == definition.id }
        if (index >= 0) {
            val existing = current[index]
            require(definition == existing || definition.revision > existing.revision) {
                "AlarmDefinition replacement must advance revision"
            }
            if (definition == existing) return existing
            current[index] = definition
        } else {
            current += definition
        }
        writeDocument(current)
        return definition
    }

    @Synchronized
    fun delete(id: AlarmDefinitionId): Boolean {
        val current = readDocument()
        val next = current.filterNot { it.id == id }
        if (next.size == current.size) return false
        writeDocument(next)
        return true
    }

    @Synchronized
    fun replaceAll(definitions: List<AlarmDefinition>) {
        val duplicate = definitions.groupBy { it.id }.entries.firstOrNull { it.value.size > 1 }
        require(duplicate == null) { "Duplicate AlarmDefinition id: ${duplicate?.key?.value}" }
        writeDocument(definitions)
    }

    @Synchronized
    fun clear() {
        writeDocument(emptyList())
    }

    private fun readDocument(): List<AlarmDefinition> {
        val text = try {
            atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            return emptyList()
        }

        if (text.isBlank()) return emptyList()
        return runCatching { decodeDocument(JSONObject(text)) }
            .getOrElse { error ->
                throw IllegalStateException("Alarm definition store is unreadable", error)
            }
    }

    private fun writeDocument(definitions: List<AlarmDefinition>) {
        val bytes = encodeDocument(definitions).toString().toByteArray(Charsets.UTF_8)
        val output = atomicFile.startWrite()
        try {
            output.write(bytes)
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun encodeDocument(definitions: List<AlarmDefinition>): JSONObject = JSONObject().apply {
        put(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION)
        put(
            KEY_ALARMS,
            JSONArray().apply {
                definitions.forEach { put(encodeAlarm(it)) }
            },
        )
    }

    private fun decodeDocument(root: JSONObject): List<AlarmDefinition> {
        val version = root.getInt(KEY_SCHEMA_VERSION)
        return when (version) {
            1 -> decodeV1Document(root)
            else -> error("Unsupported alarm definition schema version: $version")
        }
    }

    /**
     * Schema-v1 is the first durable consumer-alarm contract.
     *
     * Keep this decoder permanently compatible when a future schema is introduced. New schema
     * writers may add a v2/v3 decoder, but existing alarms must never require a destructive reset.
     */
    private fun decodeV1Document(root: JSONObject): List<AlarmDefinition> {
        val alarms = root.getJSONArray(KEY_ALARMS)
        return buildList(alarms.length()) {
            repeat(alarms.length()) { index -> add(decodeAlarm(alarms.getJSONObject(index))) }
        }.also { definitions ->
            require(definitions.map { it.id }.distinct().size == definitions.size) {
                "Alarm definition store contains duplicate ids"
            }
        }
    }

    private fun encodeAlarm(alarm: AlarmDefinition): JSONObject = JSONObject().apply {
        put(KEY_ID, alarm.id.value)
        put(KEY_LABEL, alarm.label)
        put(KEY_ENABLED, alarm.enabled)
        put(KEY_ZONE_ID, alarm.zoneId.id)
        put(KEY_SCHEDULE, encodeSchedule(alarm.schedule))
        put(KEY_SOUND_ID, alarm.soundId.value)
        put(KEY_VOICE_CHECK_IN, alarm.voiceCheckInEnabled)
        put(KEY_CHARACTER_ID, alarm.characterId.value)
        put(KEY_VOICE_STYLE, alarm.voiceStyle.name)
        put(
            KEY_SNOOZE,
            JSONObject().apply {
                put(KEY_ENABLED, alarm.snoozePolicy.enabled)
                put(KEY_DURATION_SECONDS, alarm.snoozePolicy.duration.seconds)
                alarm.snoozePolicy.maxCount?.let { put(KEY_MAX_COUNT, it) }
            },
        )
        put(KEY_TOMORROW_CONTRACT_MODE, alarm.tomorrowContractMode.name)
        alarm.firstMoveDefault?.let { put(KEY_FIRST_MOVE_DEFAULT, it) }
        put(KEY_REVISION, alarm.revision)
        put(KEY_CREATED_AT, alarm.createdAt.toString())
        put(KEY_UPDATED_AT, alarm.updatedAt.toString())
    }

    private fun decodeAlarm(json: JSONObject): AlarmDefinition = AlarmDefinition(
        id = AlarmDefinitionId(json.getString(KEY_ID)),
        label = json.getString(KEY_LABEL),
        enabled = json.getBoolean(KEY_ENABLED),
        zoneId = ZoneId.of(json.getString(KEY_ZONE_ID)),
        schedule = decodeSchedule(json.getJSONObject(KEY_SCHEDULE)),
        soundId = WakeSoundId(json.getString(KEY_SOUND_ID)),
        voiceCheckInEnabled = json.getBoolean(KEY_VOICE_CHECK_IN),
        characterId = CharacterId(json.getString(KEY_CHARACTER_ID)),
        voiceStyle = VoiceStyle.valueOf(json.getString(KEY_VOICE_STYLE)),
        snoozePolicy = json.getJSONObject(KEY_SNOOZE).let { snooze ->
            SnoozePolicy(
                enabled = snooze.getBoolean(KEY_ENABLED),
                duration = Duration.ofSeconds(snooze.getLong(KEY_DURATION_SECONDS)),
                maxCount = if (snooze.has(KEY_MAX_COUNT)) snooze.getInt(KEY_MAX_COUNT) else null,
            )
        },
        tomorrowContractMode = TomorrowContractMode.valueOf(
            json.getString(KEY_TOMORROW_CONTRACT_MODE),
        ),
        firstMoveDefault = if (json.has(KEY_FIRST_MOVE_DEFAULT)) {
            json.getString(KEY_FIRST_MOVE_DEFAULT)
        } else {
            null
        },
        revision = json.getLong(KEY_REVISION),
        createdAt = Instant.parse(json.getString(KEY_CREATED_AT)),
        updatedAt = Instant.parse(json.getString(KEY_UPDATED_AT)),
    )

    private fun encodeSchedule(schedule: AlarmSchedulePattern): JSONObject = when (schedule) {
        is AlarmSchedulePattern.Weekly -> JSONObject().apply {
            put(KEY_TYPE, TYPE_WEEKLY)
            put(KEY_TIME, schedule.time.toString())
            put(
                KEY_DAYS,
                JSONArray().apply {
                    schedule.days.sortedBy(DayOfWeek::getValue).forEach { put(it.name) }
                },
            )
        }

        is AlarmSchedulePattern.OneShot -> JSONObject().apply {
            put(KEY_TYPE, TYPE_ONE_SHOT)
            put(KEY_DATE, schedule.date.toString())
            put(KEY_TIME, schedule.time.toString())
        }
    }

    private fun decodeSchedule(json: JSONObject): AlarmSchedulePattern = when (json.getString(KEY_TYPE)) {
        TYPE_WEEKLY -> AlarmSchedulePattern.Weekly(
            days = buildSet {
                val days = json.getJSONArray(KEY_DAYS)
                repeat(days.length()) { index ->
                    add(DayOfWeek.valueOf(days.getString(index)))
                }
            },
            time = LocalTime.parse(json.getString(KEY_TIME)),
        )

        TYPE_ONE_SHOT -> AlarmSchedulePattern.OneShot(
            date = LocalDate.parse(json.getString(KEY_DATE)),
            time = LocalTime.parse(json.getString(KEY_TIME)),
        )

        else -> error("Unsupported alarm schedule type: ${json.getString(KEY_TYPE)}")
    }

    companion object {
        const val DEFAULT_FILE_NAME = "alarm-definitions-v1.json"

        private const val CURRENT_SCHEMA_VERSION = 1
        private const val KEY_SCHEMA_VERSION = "schemaVersion"
        private const val KEY_ALARMS = "alarms"
        private const val KEY_ID = "id"
        private const val KEY_LABEL = "label"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ZONE_ID = "zoneId"
        private const val KEY_SCHEDULE = "schedule"
        private const val KEY_SOUND_ID = "soundId"
        private const val KEY_VOICE_CHECK_IN = "voiceCheckInEnabled"
        private const val KEY_CHARACTER_ID = "characterId"
        private const val KEY_VOICE_STYLE = "voiceStyle"
        private const val KEY_SNOOZE = "snooze"
        private const val KEY_DURATION_SECONDS = "durationSeconds"
        private const val KEY_MAX_COUNT = "maxCount"
        private const val KEY_TOMORROW_CONTRACT_MODE = "tomorrowContractMode"
        private const val KEY_FIRST_MOVE_DEFAULT = "firstMoveDefault"
        private const val KEY_REVISION = "revision"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_UPDATED_AT = "updatedAt"
        private const val KEY_TYPE = "type"
        private const val KEY_TIME = "time"
        private const val KEY_DAYS = "days"
        private const val KEY_DATE = "date"
        private const val TYPE_WEEKLY = "weekly"
        private const val TYPE_ONE_SHOT = "oneShot"
    }
}

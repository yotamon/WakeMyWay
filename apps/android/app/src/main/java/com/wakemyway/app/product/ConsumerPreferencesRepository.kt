package com.wakemyway.app.product

import android.content.Context
import android.util.AtomicFile
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import java.io.File
import java.io.FileNotFoundException
import org.json.JSONObject

data class ConsumerPreferences(
    val onboardingCompleted: Boolean = false,
    val displayName: String? = null,
    val defaultSoundId: WakeSoundId = WakeSoundId.MORNING_LIGHT,
    val defaultVoiceCheckInEnabled: Boolean = true,
    val defaultVoiceStyle: VoiceStyle = VoiceStyle.DEFAULT,
    val defaultSnoozeMinutes: Int = 5,
    val defaultFirstMove: String? = null,
) {
    init {
        require(displayName == null || displayName.length <= MAX_DISPLAY_NAME_LENGTH) {
            "Display name must be at most $MAX_DISPLAY_NAME_LENGTH characters"
        }
        require(defaultSnoozeMinutes in ALLOWED_SNOOZE_MINUTES) {
            "Default snooze must be one of $ALLOWED_SNOOZE_MINUTES"
        }
        require(defaultFirstMove == null || defaultFirstMove.length <= MAX_FIRST_MOVE_LENGTH) {
            "Default First Move must be at most $MAX_FIRST_MOVE_LENGTH characters"
        }
    }

    companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 48
        const val MAX_FIRST_MOVE_LENGTH = 120
        val ALLOWED_SNOOZE_MINUTES = setOf(5, 10, 15)
    }
}

/**
 * Credential-protected local consumer preferences.
 *
 * These settings shape normal UI and the defaults used when creating a new alarm. They are never
 * needed to deliver an already committed wake and therefore must not be copied into device-
 * protected Alarm Kernel state. Critical per-alarm execution policy remains compiled separately
 * from each saved AlarmDefinition.
 *
 * Preference corruption is deliberately non-fatal. This repository fails open to safe product
 * defaults so non-critical personalization can never prevent the app from opening or interfere
 * with an already committed wake. A later successful preference mutation atomically replaces the
 * unreadable document.
 */
class ConsumerPreferencesRepository(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
) {
    private val atomicFile = AtomicFile(File(context.filesDir, fileName))

    @Synchronized
    fun get(): ConsumerPreferences = read()

    @Synchronized
    fun update(transform: (ConsumerPreferences) -> ConsumerPreferences): ConsumerPreferences {
        val next = transform(read())
        write(next)
        return next
    }

    @Synchronized
    fun replace(preferences: ConsumerPreferences): ConsumerPreferences {
        write(preferences)
        return preferences
    }

    private fun read(): ConsumerPreferences {
        val text = try {
            atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            return ConsumerPreferences()
        }

        if (text.isBlank()) return ConsumerPreferences()
        return runCatching { decode(JSONObject(text)) }
            .getOrElse { ConsumerPreferences() }
    }

    private fun write(preferences: ConsumerPreferences) {
        val bytes = encode(preferences).toString().toByteArray(Charsets.UTF_8)
        val output = atomicFile.startWrite()
        try {
            output.write(bytes)
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun encode(preferences: ConsumerPreferences): JSONObject = JSONObject().apply {
        put(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
        put(KEY_ONBOARDING_COMPLETED, preferences.onboardingCompleted)
        preferences.displayName?.let { put(KEY_DISPLAY_NAME, it) }
        put(KEY_DEFAULT_SOUND_ID, preferences.defaultSoundId.value)
        put(KEY_DEFAULT_VOICE_CHECK_IN, preferences.defaultVoiceCheckInEnabled)
        put(KEY_DEFAULT_VOICE_STYLE, preferences.defaultVoiceStyle.name)
        put(KEY_DEFAULT_SNOOZE_MINUTES, preferences.defaultSnoozeMinutes)
        preferences.defaultFirstMove?.let { put(KEY_DEFAULT_FIRST_MOVE, it) }
    }

    private fun decode(root: JSONObject): ConsumerPreferences {
        val version = root.getInt(KEY_SCHEMA_VERSION)
        require(version == SCHEMA_VERSION) {
            "Unsupported consumer preferences schema version: $version"
        }
        return ConsumerPreferences(
            onboardingCompleted = root.optBoolean(KEY_ONBOARDING_COMPLETED, false),
            displayName = root.optString(KEY_DISPLAY_NAME).takeIf(String::isNotBlank),
            defaultSoundId = WakeSoundId(
                root.optString(KEY_DEFAULT_SOUND_ID, WakeSoundId.MORNING_LIGHT.value),
            ),
            defaultVoiceCheckInEnabled = root.optBoolean(KEY_DEFAULT_VOICE_CHECK_IN, true),
            defaultVoiceStyle = VoiceStyle.valueOf(
                root.optString(KEY_DEFAULT_VOICE_STYLE, VoiceStyle.DEFAULT.name),
            ),
            defaultSnoozeMinutes = root.optInt(KEY_DEFAULT_SNOOZE_MINUTES, 5),
            defaultFirstMove = root.optString(KEY_DEFAULT_FIRST_MOVE).takeIf(String::isNotBlank),
        )
    }

    companion object {
        const val DEFAULT_FILE_NAME = "consumer-preferences-v1.json"
        private const val SCHEMA_VERSION = 1
        private const val KEY_SCHEMA_VERSION = "schemaVersion"
        private const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"
        private const val KEY_DISPLAY_NAME = "displayName"
        private const val KEY_DEFAULT_SOUND_ID = "defaultSoundId"
        private const val KEY_DEFAULT_VOICE_CHECK_IN = "defaultVoiceCheckInEnabled"
        private const val KEY_DEFAULT_VOICE_STYLE = "defaultVoiceStyle"
        private const val KEY_DEFAULT_SNOOZE_MINUTES = "defaultSnoozeMinutes"
        private const val KEY_DEFAULT_FIRST_MOVE = "defaultFirstMove"
    }
}

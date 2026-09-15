package com.wakemyway.app.product

import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConsumerPreferencesRepositoryTest {
    private val context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun `missing store returns safe local defaults`() {
        val repository = repository()

        val preferences = repository.get()

        assertSafeDefaults(preferences)
    }

    @Test
    fun `preferences round trip through credential protected document`() {
        val repository = repository()
        val expected = ConsumerPreferences(
            onboardingCompleted = true,
            displayName = "Yotam",
            defaultSoundId = WakeSoundId.SOFT_START,
            defaultVoiceCheckInEnabled = false,
            defaultVoiceStyle = VoiceStyle.MINIMAL,
            defaultSnoozeMinutes = 15,
            defaultFirstMove = "Open the curtains",
            appearance = AppAppearance.SOFT_DAWN,
        )

        repository.replace(expected)

        assertEquals(expected, repository.get())
    }

    @Test
    fun `update changes only requested preference`() {
        val repository = repository()
        repository.replace(
            ConsumerPreferences(
                displayName = "Yotam",
                defaultSoundId = WakeSoundId.MORNING_PULSE,
                appearance = AppAppearance.WARM_SUNRISE,
            ),
        )

        val updated = repository.update { it.copy(onboardingCompleted = true) }

        assertTrue(updated.onboardingCompleted)
        assertEquals("Yotam", updated.displayName)
        assertEquals(WakeSoundId.MORNING_PULSE, updated.defaultSoundId)
        assertEquals(AppAppearance.WARM_SUNRISE, updated.appearance)
    }

    @Test
    fun `schema v1 document without appearance keeps existing preferences`() {
        val fileName = uniqueFileName()
        File(context.filesDir, fileName).writeText(
            """
            {
              "schemaVersion": 1,
              "onboardingCompleted": true,
              "displayName": "Yotam",
              "defaultSoundId": "soft-start",
              "defaultVoiceCheckInEnabled": false,
              "defaultVoiceStyle": "MINIMAL",
              "defaultSnoozeMinutes": 15,
              "defaultFirstMove": "Open the curtains"
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )
        val preferences = ConsumerPreferencesRepository(context, fileName).get()

        assertTrue(preferences.onboardingCompleted)
        assertEquals("Yotam", preferences.displayName)
        assertEquals(WakeSoundId.SOFT_START, preferences.defaultSoundId)
        assertFalse(preferences.defaultVoiceCheckInEnabled)
        assertEquals(VoiceStyle.MINIMAL, preferences.defaultVoiceStyle)
        assertEquals(15, preferences.defaultSnoozeMinutes)
        assertEquals("Open the curtains", preferences.defaultFirstMove)
        assertEquals(AppAppearance.DAYLIGHT, preferences.appearance)
    }

    @Test
    fun `corrupt non critical preferences fail open and can be replaced`() {
        val fileName = uniqueFileName()
        File(context.filesDir, fileName).writeText("{not-json", Charsets.UTF_8)
        val repository = ConsumerPreferencesRepository(context, fileName)

        assertSafeDefaults(repository.get())

        val recovered = repository.update {
            it.copy(
                onboardingCompleted = true,
                displayName = "Yotam",
                defaultSoundId = WakeSoundId.SOFT_START,
                appearance = AppAppearance.WARM_SUNRISE,
            )
        }

        assertTrue(recovered.onboardingCompleted)
        assertEquals("Yotam", recovered.displayName)
        assertEquals(WakeSoundId.SOFT_START, recovered.defaultSoundId)
        assertEquals(AppAppearance.WARM_SUNRISE, recovered.appearance)
        assertEquals(recovered, repository.get())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported snooze default is rejected`() {
        ConsumerPreferences(defaultSnoozeMinutes = 7)
    }

    private fun assertSafeDefaults(preferences: ConsumerPreferences) {
        assertFalse(preferences.onboardingCompleted)
        assertNull(preferences.displayName)
        assertEquals(WakeSoundId.MORNING_LIGHT, preferences.defaultSoundId)
        assertTrue(preferences.defaultVoiceCheckInEnabled)
        assertEquals(VoiceStyle.DEFAULT, preferences.defaultVoiceStyle)
        assertEquals(5, preferences.defaultSnoozeMinutes)
        assertNull(preferences.defaultFirstMove)
        assertEquals(AppAppearance.DAYLIGHT, preferences.appearance)
    }

    private fun repository(): ConsumerPreferencesRepository = ConsumerPreferencesRepository(
        context = context,
        fileName = uniqueFileName(),
    )

    private fun uniqueFileName(): String =
        "consumer-preferences-${System.nanoTime()}.json"
}

package com.wakemyway.app.product

import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import com.wakemyway.core.personalization.ConversationAmount
import com.wakemyway.core.personalization.HumorPreference
import com.wakemyway.core.personalization.InterventionStyle
import com.wakemyway.core.personalization.MorningBarrier
import com.wakemyway.core.personalization.MotivationStyle
import com.wakemyway.core.personalization.PerceivedWakeInertia
import com.wakemyway.core.personalization.WakePreferences
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
            wakePreferences = WakePreferences(
                morningBarrier = MorningBarrier.SNOOZE_LOOP,
                perceivedWakeInertia = PerceivedWakeInertia.ABOUT_30_MINUTES,
                interventionStyle = InterventionStyle.FIRM,
                motivationStyle = MotivationStyle.ACCOUNTABILITY,
                conversationAmount = ConversationAmount.MINIMAL,
                humorPreference = HumorPreference.OFF,
            ),
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
    fun `unknown appearance falls back without discarding other preferences`() {
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
              "defaultFirstMove": "Open the curtains",
              "appearance": "FUTURE_ATMOSPHERE"
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

    @Test
    fun `schema v1 migrates to balanced wake preferences`() {
        val fileName = uniqueFileName()
        File(context.filesDir, fileName).writeText(
            """{"schemaVersion":1,"onboardingCompleted":true,"defaultSoundId":"morning-light","defaultVoiceCheckInEnabled":true,"defaultVoiceStyle":"DEFAULT","defaultSnoozeMinutes":5,"appearance":"DAYLIGHT"}""",
            Charsets.UTF_8,
        )

        val preferences = ConsumerPreferencesRepository(context, fileName).get()

        assertEquals(WakePreferences(), preferences.wakePreferences)
        assertTrue(preferences.onboardingCompleted)
    }

    @Test
    fun `unknown wake preference values fall back field by field`() {
        val fileName = uniqueFileName()
        File(context.filesDir, fileName).writeText(
            """
            {
              "schemaVersion": 2,
              "onboardingCompleted": true,
              "defaultSoundId": "morning-light",
              "defaultVoiceCheckInEnabled": true,
              "defaultVoiceStyle": "DEFAULT",
              "defaultSnoozeMinutes": 5,
              "appearance": "DAYLIGHT",
              "wakePreferences": {
                "morningBarrier": "FUTURE_BARRIER",
                "interventionStyle": "FIRM",
                "conversationAmount": "MINIMAL",
                "humorPreference": "OFF"
              }
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val wake = ConsumerPreferencesRepository(context, fileName).get().wakePreferences

        assertEquals(MorningBarrier.UNSURE, wake.morningBarrier)
        assertEquals(InterventionStyle.FIRM, wake.interventionStyle)
        assertEquals(ConversationAmount.MINIMAL, wake.conversationAmount)
        assertEquals(HumorPreference.OFF, wake.humorPreference)
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
        assertEquals(WakePreferences(), preferences.wakePreferences)
    }

    private fun repository(): ConsumerPreferencesRepository = ConsumerPreferencesRepository(
        context = context,
        fileName = uniqueFileName(),
    )

    private fun uniqueFileName(): String =
        "consumer-preferences-${System.nanoTime()}.json"
}

package com.wakemyway.app.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.CriticalWakeReadResult
import com.wakemyway.app.alarm.CriticalWakeStore
import com.wakemyway.app.product.AlarmDefinitionRepository
import com.wakemyway.app.product.AlarmProductController
import com.wakemyway.app.product.AppAppearance
import com.wakemyway.app.product.ConsumerPreferences
import com.wakemyway.app.product.ConsumerPreferencesRepository
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryRepository
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.alarm.SnoozePolicy
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Two-phase package-replacement contract.
 *
 * CI invokes [seedPersistentStateForUpgrade] against the baseline APK, installs the candidate APK
 * with `adb install -r` (never uninstalling or clearing data), then invokes
 * [verifyPersistentStateAfterUpgrade] against the candidate.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class UpgradePersistenceContractOnly

@RunWith(AndroidJUnit4::class)
@UpgradePersistenceContractOnly
class UpdatePersistenceContractInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun seedPersistentStateForUpgrade() {
        requirePhase(PHASE_SEED)
        cleanup()

        AlarmProductController(context).save(expectedAlarm())
        ConsumerPreferencesRepository(context).replace(EXPECTED_PREFERENCES)
        WakeHistoryRepository(context).record(expectedHistory())

        assertEquals(expectedAlarm(), AlarmDefinitionRepository(context).get(ALARM_ID))
        assertEquals(EXPECTED_PREFERENCES, ConsumerPreferencesRepository(context).get())
        assertEquals(1, WakeHistoryRepository(context).list().size)

        val critical = CriticalWakeStore(context).readResult()
        assertTrue(critical is CriticalWakeReadResult.State)
        val state = (critical as CriticalWakeReadResult.State).value
        assertNotNull(state.slots[WakeScheduleId(ALARM_ID.value)])
    }

    @Test
    fun verifyPersistentStateAfterUpgrade() {
        requirePhase(PHASE_VERIFY)
        assertEquals(expectedAlarm(), AlarmDefinitionRepository(context).get(ALARM_ID))
        assertEquals(EXPECTED_PREFERENCES, ConsumerPreferencesRepository(context).get())

        val history = WakeHistoryRepository(context).list()
        assertEquals(1, history.size)
        assertEquals(expectedHistory(), history.single())

        val critical = CriticalWakeStore(context).readResult()
        assertTrue("Critical wake state must survive package replacement", critical is CriticalWakeReadResult.State)
        val state = (critical as CriticalWakeReadResult.State).value
        val slot = state.slots[WakeScheduleId(ALARM_ID.value)]
        assertNotNull("Critical schedule slot must survive package replacement", slot)
        assertEquals(ALARM_ID.value, slot!!.schedule.id.value)

        // Package replacement may deliberately disable critical registration if Android presentation
        // access is unsafe on the test device. Product truth must still survive and remain repairable.
        assertEquals(ALARM_ID, AlarmProductController(context).get(ALARM_ID)?.id)
    }

    @Test
    fun cleanupUpgradeContractState() {
        requirePhase(PHASE_CLEANUP)
        cleanup()
        assertTrue(AlarmDefinitionRepository(context).list().isEmpty())
        assertTrue(WakeHistoryRepository(context).list().isEmpty())
    }

    private fun requirePhase(expected: String) {
        val actual = InstrumentationRegistry.getArguments().getString(ARG_UPGRADE_PHASE)
        assumeTrue(
            "Package-upgrade contract phases run only in the dedicated upgrade workflow",
            actual == expected,
        )
    }

    private fun cleanup() {
        runCatching { AlarmKernel(context).cancelSchedule(WakeScheduleId(ALARM_ID.value)) }
        AlarmDefinitionRepository(context).clear()
        ConsumerPreferencesRepository(context).replace(ConsumerPreferences())
        WakeHistoryRepository(context).clear()
        CriticalWakeStore(context).clear()
    }

    private fun expectedAlarm(): AlarmDefinition = AlarmDefinition(
        id = ALARM_ID,
        label = "Upgrade Contract Alarm",
        enabled = true,
        zoneId = ZoneId.of("Europe/Berlin"),
        schedule = AlarmSchedulePattern.Weekly(
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            time = LocalTime.of(6, 37),
        ),
        soundId = WakeSoundId.MORNING_PULSE,
        voiceCheckInEnabled = true,
        voiceStyle = VoiceStyle.MINIMAL,
        snoozePolicy = SnoozePolicy(
            enabled = true,
            duration = Duration.ofMinutes(10),
            maxCount = 2,
        ),
        tomorrowContractMode = TomorrowContractMode.ALWAYS_PROMPT,
        firstMoveDefault = "Open the curtains",
        revision = 7,
        createdAt = CREATED_AT,
        updatedAt = UPDATED_AT,
    )

    private fun expectedHistory(): WakeHistoryEntry = WakeHistoryEntry(
        sessionId = WakeSessionId("upgrade-contract-session"),
        occurrenceId = WakeOccurrenceId("upgrade-contract-occurrence"),
        scheduleId = WakeScheduleId(ALARM_ID.value),
        occurrenceKind = WakeOccurrenceKind.PRIMARY,
        scheduleRevision = 7,
        scheduledAt = Instant.parse("2026-09-18T04:37:00Z"),
        startedAt = Instant.parse("2026-09-18T04:37:02Z"),
        finishedAt = Instant.parse("2026-09-18T04:40:00Z"),
        terminalReason = WakeHistoryTerminalReason.STOPPED,
    )

    private companion object {
        const val ARG_UPGRADE_PHASE = "wmwUpgradePhase"
        const val PHASE_SEED = "seed"
        const val PHASE_VERIFY = "verify"
        const val PHASE_CLEANUP = "cleanup"

        val ALARM_ID = AlarmDefinitionId("upgrade-contract-alarm")
        val CREATED_AT: Instant = Instant.parse("2026-09-01T08:00:00Z")
        val UPDATED_AT: Instant = Instant.parse("2026-09-10T09:30:00Z")

        val EXPECTED_PREFERENCES = ConsumerPreferences(
            onboardingCompleted = true,
            displayName = "Upgrade Contract",
            defaultSoundId = WakeSoundId.SOFT_START,
            defaultVoiceCheckInEnabled = true,
            defaultVoiceStyle = VoiceStyle.MINIMAL,
            defaultSnoozeMinutes = 10,
            defaultFirstMove = "Drink water",
            appearance = AppAppearance.WARM_SUNRISE,
        )
    }
}

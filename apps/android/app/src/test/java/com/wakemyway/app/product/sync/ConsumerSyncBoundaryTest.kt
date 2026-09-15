package com.wakemyway.app.product.sync

import com.wakemyway.app.product.AppAppearance
import com.wakemyway.app.product.ConsumerPreferences
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.alarm.SnoozePolicy
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsumerSyncBoundaryTest {
    @Test
    fun `backup contains consumer intent but not device onboarding authority`() {
        val local = ConsumerSyncLocalState(
            preferences = ConsumerPreferences(
                onboardingCompleted = true,
                displayName = "Yotam",
                defaultSoundId = WakeSoundId.SOFT_START,
                defaultVoiceCheckInEnabled = false,
                defaultVoiceStyle = VoiceStyle.MINIMAL,
                defaultSnoozeMinutes = 15,
                defaultFirstMove = "Open the curtains",
                appearance = AppAppearance.SOFT_DAWN,
            ),
            alarms = listOf(alarm("z-alarm"), alarm("a-alarm")),
        )

        val snapshot = ConsumerSyncPolicy.createSnapshot(local, SNAPSHOT_TIME)

        assertEquals(SNAPSHOT_TIME, snapshot.generatedAt)
        assertEquals(listOf("a-alarm", "z-alarm"), snapshot.alarms.map { it.id.value })
        assertEquals("Yotam", snapshot.preferences.displayName)
        assertEquals(WakeSoundId.SOFT_START, snapshot.preferences.defaultSoundId)
        assertEquals(AppAppearance.SOFT_DAWN, snapshot.preferences.appearance)
        assertFalse(
            "Onboarding is device-local and must not enter the cloud payload",
            ConsumerSyncPreferences::class.java.declaredFields.any { it.name == "onboardingCompleted" },
        )
    }

    @Test
    fun `fresh-device migration restores preferences but preserves local onboarding state`() {
        val localPreferences = ConsumerPreferences(
            onboardingCompleted = false,
            displayName = null,
        )
        val remotePreferences = ConsumerSyncPreferences.from(
            ConsumerPreferences(
                onboardingCompleted = true,
                displayName = "Yotam",
                defaultSoundId = WakeSoundId.MORNING_PULSE,
                defaultVoiceCheckInEnabled = false,
                defaultVoiceStyle = VoiceStyle.MOTIVATIONAL,
                defaultSnoozeMinutes = 10,
                defaultFirstMove = "Drink water",
                appearance = AppAppearance.WARM_SUNRISE,
            ),
        )
        val remote = snapshot(
            preferences = remotePreferences,
            alarms = emptyList(),
        )

        val plan = ConsumerSyncPolicy.planMigration(
            local = ConsumerSyncLocalState(localPreferences, emptyList()),
            remote = remote,
            mode = ConsumerRestoreMode.MIGRATE_TO_FRESH_DEVICE,
            importedAt = IMPORT_TIME,
        )

        assertFalse(plan.targetPreferences.onboardingCompleted)
        assertEquals("Yotam", plan.targetPreferences.displayName)
        assertEquals(WakeSoundId.MORNING_PULSE, plan.targetPreferences.defaultSoundId)
        assertEquals(false, plan.targetPreferences.defaultVoiceCheckInEnabled)
        assertEquals(VoiceStyle.MOTIVATIONAL, plan.targetPreferences.defaultVoiceStyle)
        assertEquals(10, plan.targetPreferences.defaultSnoozeMinutes)
        assertEquals("Drink water", plan.targetPreferences.defaultFirstMove)
        assertEquals(AppAppearance.WARM_SUNRISE, plan.targetPreferences.appearance)
    }

    @Test
    fun `merge into existing device keeps local preferences`() {
        val localPreferences = ConsumerPreferences(
            onboardingCompleted = true,
            displayName = "Local",
            defaultSoundId = WakeSoundId.SOFT_START,
            appearance = AppAppearance.SOFT_DAWN,
        )
        val remote = snapshot(
            preferences = ConsumerSyncPreferences.from(
                ConsumerPreferences(
                    displayName = "Remote",
                    defaultSoundId = WakeSoundId.MORNING_PULSE,
                    appearance = AppAppearance.WARM_SUNRISE,
                ),
            ),
            alarms = emptyList(),
        )

        val plan = ConsumerSyncPolicy.planMigration(
            local = ConsumerSyncLocalState(localPreferences, emptyList()),
            remote = remote,
            mode = ConsumerRestoreMode.MERGE_INTO_EXISTING,
            importedAt = IMPORT_TIME,
        )

        assertEquals(localPreferences, plan.targetPreferences)
    }

    @Test
    fun `migration never replaces local alarm and remote-only alarm imports disabled`() {
        val localAlarm = alarm(
            id = "same-id",
            enabled = true,
            label = "Local truth",
            revision = 8,
        )
        val conflictingRemote = alarm(
            id = "same-id",
            enabled = false,
            label = "Remote must lose",
            revision = 99,
        )
        val remoteOnly = alarm(
            id = "remote-only",
            enabled = true,
            label = "Remote morning",
            revision = 4,
        )
        val remote = snapshot(alarms = listOf(conflictingRemote, remoteOnly))

        val plan = ConsumerSyncPolicy.planMigration(
            local = ConsumerSyncLocalState(ConsumerPreferences(), listOf(localAlarm)),
            remote = remote,
            mode = ConsumerRestoreMode.MERGE_INTO_EXISTING,
            importedAt = IMPORT_TIME,
        )

        assertEquals(setOf(AlarmDefinitionId("same-id")), plan.preservedLocalAlarmIds)
        assertEquals(1, plan.alarmsToImport.size)
        val imported = plan.alarmsToImport.single()
        assertEquals(AlarmDefinitionId("remote-only"), imported.id)
        assertFalse(imported.enabled)
        assertEquals(5L, imported.revision)
        assertEquals(IMPORT_TIME, imported.updatedAt)
        assertEquals("Remote morning", imported.label)
        assertTrue(plan.alarmsToImport.none { it.id == localAlarm.id })
    }

    @Test
    fun `restore cloud outage performs zero local reads or writes`() {
        val initial = ConsumerSyncLocalState(
            preferences = ConsumerPreferences(displayName = "Local"),
            alarms = listOf(alarm("local", enabled = true)),
        )
        val store = RecordingLocalStore(initial)
        val gateway = RecordingGateway(downloadFailure = IllegalStateException("offline"))
        val coordinator = ConsumerSyncCoordinator(
            gateway = gateway,
            localStore = store,
            clock = FIXED_CLOCK,
        )

        val result = coordinator.restore(ConsumerRestoreMode.MIGRATE_TO_FRESH_DEVICE)

        assertTrue(result is ConsumerSyncResult.Failed)
        assertEquals(ConsumerSyncFailureStage.RESTORE_DOWNLOAD, (result as ConsumerSyncResult.Failed).stage)
        assertEquals(0, store.readCount)
        assertEquals(0, store.applyCount)
        assertEquals(initial, store.state)
    }

    @Test
    fun `backup cloud outage never mutates local state`() {
        val initial = ConsumerSyncLocalState(
            preferences = ConsumerPreferences(displayName = "Local"),
            alarms = listOf(alarm("local", enabled = true)),
        )
        val store = RecordingLocalStore(initial)
        val gateway = RecordingGateway(uploadFailure = IllegalStateException("offline"))
        val coordinator = ConsumerSyncCoordinator(
            gateway = gateway,
            localStore = store,
            clock = FIXED_CLOCK,
        )

        val result = coordinator.backup()

        assertTrue(result is ConsumerSyncResult.Failed)
        assertEquals(ConsumerSyncFailureStage.BACKUP_UPLOAD, (result as ConsumerSyncResult.Failed).stage)
        assertEquals(1, store.readCount)
        assertEquals(0, store.applyCount)
        assertEquals(initial, store.state)
    }

    @Test
    fun `missing remote backup performs zero local mutation`() {
        val initial = ConsumerSyncLocalState(
            preferences = ConsumerPreferences(displayName = "Local"),
            alarms = listOf(alarm("local", enabled = true)),
        )
        val store = RecordingLocalStore(initial)
        val coordinator = ConsumerSyncCoordinator(
            gateway = RecordingGateway(downloadSnapshot = null),
            localStore = store,
            clock = FIXED_CLOCK,
        )

        val result = coordinator.restore(ConsumerRestoreMode.MIGRATE_TO_FRESH_DEVICE)

        assertEquals(ConsumerSyncResult.NoRemoteBackup, result)
        assertEquals(0, store.readCount)
        assertEquals(0, store.applyCount)
        assertEquals(initial, store.state)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `snapshot rejects duplicate remote alarm ids`() {
        snapshot(alarms = listOf(alarm("duplicate"), alarm("duplicate")))
    }

    private fun snapshot(
        preferences: ConsumerSyncPreferences = ConsumerSyncPreferences.from(ConsumerPreferences()),
        alarms: List<AlarmDefinition> = emptyList(),
    ): ConsumerSyncSnapshot = ConsumerSyncSnapshot(
        generatedAt = SNAPSHOT_TIME,
        preferences = preferences,
        alarms = alarms,
    )

    private fun alarm(
        id: String,
        enabled: Boolean = true,
        label: String = id,
        revision: Long = 1,
    ): AlarmDefinition = AlarmDefinition(
        id = AlarmDefinitionId(id),
        label = label,
        enabled = enabled,
        zoneId = ZoneId.of("Europe/Berlin"),
        schedule = AlarmSchedulePattern.Weekly(
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            time = LocalTime.of(7, 30),
        ),
        soundId = WakeSoundId.MORNING_LIGHT,
        voiceCheckInEnabled = true,
        voiceStyle = VoiceStyle.DEFAULT,
        snoozePolicy = SnoozePolicy(enabled = true, duration = Duration.ofMinutes(5)),
        tomorrowContractMode = TomorrowContractMode.OPTIONAL,
        firstMoveDefault = "Open the curtains",
        revision = revision,
        createdAt = CREATED_AT,
        updatedAt = UPDATED_AT,
    )

    private class RecordingGateway(
        private val downloadSnapshot: ConsumerSyncSnapshot? = null,
        private val uploadFailure: Throwable? = null,
        private val downloadFailure: Throwable? = null,
    ) : ConsumerSyncGateway {
        val uploads = mutableListOf<ConsumerSyncSnapshot>()

        override fun upload(snapshot: ConsumerSyncSnapshot) {
            uploadFailure?.let { throw it }
            uploads += snapshot
        }

        override fun download(): ConsumerSyncSnapshot? {
            downloadFailure?.let { throw it }
            return downloadSnapshot
        }
    }

    private class RecordingLocalStore(
        var state: ConsumerSyncLocalState,
    ) : ConsumerSyncLocalStore {
        var readCount = 0
        var applyCount = 0

        override fun read(): ConsumerSyncLocalState {
            readCount++
            return state
        }

        override fun applyMigration(plan: ConsumerMigrationPlan): ConsumerMigrationReport {
            applyCount++
            val existingIds = state.alarms.mapTo(linkedSetOf()) { it.id }
            val imports = plan.alarmsToImport.filterNot { it.id in existingIds }
            state = ConsumerSyncLocalState(
                preferences = plan.targetPreferences,
                alarms = state.alarms + imports,
            )
            return ConsumerMigrationReport(
                importedAlarmIds = imports.mapTo(linkedSetOf()) { it.id },
                skippedAlarmIds = plan.preservedLocalAlarmIds,
            )
        }
    }

    companion object {
        private val CREATED_AT = Instant.parse("2026-09-10T05:00:00Z")
        private val UPDATED_AT = Instant.parse("2026-09-11T05:00:00Z")
        private val SNAPSHOT_TIME = Instant.parse("2026-09-15T20:00:00Z")
        private val IMPORT_TIME = Instant.parse("2026-09-16T06:00:00Z")
        private val FIXED_CLOCK = Clock.fixed(SNAPSHOT_TIME, ZoneId.of("UTC"))
    }
}

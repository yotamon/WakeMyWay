package com.wakemyway.app.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.core.schedule.LocalTimeResolution
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateCoordinatorTest {
    private lateinit var context: Context
    private lateinit var provider: FakeUpdateProvider
    private lateinit var states: MutableList<UpdateState>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("update-check", Context.MODE_PRIVATE).edit().clear().commit()
        provider = FakeUpdateProvider()
        states = mutableListOf()
    }

    @Test
    fun automaticCheckIsThrottledForTwentyFourHours() {
        val coordinator = coordinator(now = Instant.parse("2026-09-19T08:00:00Z"))

        coordinator.checkIfDue()
        coordinator.checkIfDue()

        assertEquals(1, provider.checkCount)
        coordinator.close()
    }

    @Test
    fun manualCheckBypassesThrottle() {
        val coordinator = coordinator(now = Instant.parse("2026-09-19T08:00:00Z"))

        coordinator.checkIfDue()
        coordinator.checkNow()

        assertEquals(2, provider.checkCount)
        coordinator.close()
    }

    @Test
    fun automaticCheckDoesNotRunDuringActiveWake() {
        val coordinator = coordinator(
            now = Instant.parse("2026-09-19T08:00:00Z"),
            health = emptyHealth(active = true),
        )

        coordinator.checkIfDue()

        assertEquals(0, provider.checkCount)
        coordinator.close()
    }

    @Test
    fun readyUpdateDefersInstallationNearNextWake() {
        val release = release()
        val now = Instant.parse("2026-09-19T08:00:00Z")
        val coordinator = coordinator(
            now = now,
            health = emptyHealth(nextWakeAt = now.plusSeconds(60 * 60)),
        )

        provider.checkResult = UpdateProviderCheck.ReadyToInstall(release)
        coordinator.checkNow()
        coordinator.installUpdate()

        assertTrue(states.last() is UpdateState.Deferred)
        assertEquals(0, provider.completeCount)
        coordinator.close()
    }

    private fun coordinator(
        now: Instant,
        health: AlarmHealth = emptyHealth(),
    ): UpdateCoordinator = UpdateCoordinator(
        context = context,
        healthProvider = { health },
        providerFactory = { callback ->
            provider.callback = callback
            provider
        },
        onStateChanged = states::add,
        now = { now },
    )

    private fun release() = UpdateRelease(
        source = UpdateSource.DIRECT,
        versionCode = 99,
        versionName = "9.9.9",
    )

    private fun emptyHealth(
        active: Boolean = false,
        nextWakeAt: Instant? = null,
    ): AlarmHealth = AlarmHealth(
        ready = false,
        exactAlarmAllowed = true,
        notificationsAllowed = true,
        notificationChannelHighImportance = true,
        fullScreenIntentAllowed = true,
        nextOccurrence = nextWakeAt?.let { occurrence("next", it) },
        activeOccurrence = if (active) {
            occurrence("active", Instant.parse("2026-09-19T08:00:00Z"))
        } else {
            null
        },
        detail = "test",
    )

    private fun occurrence(
        id: String,
        instant: Instant,
    ): WakeOccurrence {
        val scheduledAt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
        return WakeOccurrence(
            id = WakeOccurrenceId(id),
            wakeScheduleId = WakeScheduleId("test-schedule"),
            kind = WakeOccurrenceKind.PRIMARY,
            scheduledLocalDateTime = scheduledAt.toLocalDateTime(),
            scheduledAt = scheduledAt,
            scheduleRevision = 1,
            localTimeResolution = LocalTimeResolution.EXACT,
        )
    }

    private class FakeUpdateProvider : UpdateProvider {
        var callback: ((UpdateProviderEvent) -> Unit)? = null
        var checkCount = 0
        var completeCount = 0
        var checkResult: UpdateProviderCheck = UpdateProviderCheck.UpToDate

        override fun check(
            currentVersionCode: Long,
            result: (Result<UpdateProviderCheck>) -> Unit,
        ) {
            checkCount++
            result(Result.success(checkResult))
        }

        override fun beginUpdate(release: UpdateRelease) = Unit

        override fun completeUpdate(release: UpdateRelease) {
            completeCount++
        }

        override fun openInstallPermissionSettings() = Unit
        override fun resume(currentVersionCode: Long) = Unit
        override fun handleActivityResult(resultCode: Int) = Unit
        override fun close() = Unit
    }
}

package com.wakemyway.app.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wakemyway.app.alarm.AlarmHealth
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

    /**
     * Keep the fixture deliberately minimal. These tests care only about active/next occurrence.
     * AlarmHealth defaults are used for unrelated platform readiness fields.
     */
    private fun emptyHealth(
        active: Boolean = false,
        nextWakeAt: Instant? = null,
    ): AlarmHealth {
        val base = AlarmHealth.empty()
        return base.copy(
            activeOccurrence = if (active) base.testOccurrence(Instant.parse("2026-09-19T08:00:00Z")) else null,
            nextOccurrence = nextWakeAt?.let(base::testOccurrence),
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

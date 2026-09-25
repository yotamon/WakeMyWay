package com.wakemyway.app.alarm

import android.app.Application
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class AlarmPlaybackServiceRobolectricTest {
    // API 32 (Android 12L): presentation capability checks that Robolectric cannot satisfy
    // (POST_NOTIFICATIONS grant, canUseFullScreenIntent) are version-short-circuited there,
    // so the test exercises the real ensureActiveWake playback path. API 32 is also the
    // highest legacy generation targeted by the SCHEDULE_EXACT_ALARM manifest declaration.
    private val now = Instant.parse("2026-09-12T04:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val context
        get() = RuntimeEnvironment.getApplication()

    private lateinit var kernel: AlarmKernel

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        // The service constructs its own kernel with the default critical state file, so the test
        // kernel must share that file to be observable. Robolectric sandboxes the filesystem per
        // test, so the default name cannot leak state between tests.
        kernel = AlarmKernel(context = context, clock = clock)
    }

    @After
    fun tearDown() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        runCatching { kernel.cancelSchedule() }
    }

    @Test
    fun `an active wake with a fully degraded audio stack stays alive and foreground`() {
        val primary = requireNotNull(
            kernel.commitSchedule(oneShotSchedule("degraded-audio")).nextOccurrence,
        )
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(primary.id))

        AlarmPlaybackService.start(context, primary.id)
        val startIntent = shadowOf(context).nextStartedService
        assertNotNull(startIntent)

        // Robolectric has no audio hardware: media playback and the platform tone both fail. The
        // service must contain every playback failure and keep the wake foreground-controllable
        // instead of crashing into a silent START_REDELIVER_INTENT loop.
        val controller = Robolectric.buildService(AlarmPlaybackService::class.java, startIntent)
            .create()
            .startCommand(0, 1)

        // Reaching this point means no playback failure escaped onStartCommand, and the wake
        // execution is still presented as a controllable foreground notification.
        assertNotNull(shadowOf(controller.get()).lastForegroundNotification)
        assertEquals(primary.id, kernel.activeOccurrence()?.id)
    }

    private fun oneShotSchedule(suffix: String): WakeSchedule {
        val target = now.plus(Duration.ofMinutes(20)).atZone(ZoneOffset.UTC)
        return WakeSchedule(
            id = WakeScheduleId("service-robolectric-$suffix"),
            zoneId = ZoneOffset.UTC,
            timesByDay = mapOf(target.dayOfWeek to target.toLocalTime()),
            revision = 1,
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
            oneShotDate = target.toLocalDate(),
        )
    }
}

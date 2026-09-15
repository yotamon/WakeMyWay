package com.wakemyway.app

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LaunchThemeBoundaryTest {
    @Test
    fun `branded launch theme is consumer-only`() {
        val context = RuntimeEnvironment.getApplication()
        val packageManager = context.packageManager

        val mainActivity = packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )
        val wakeActivity = packageManager.getActivityInfo(
            ComponentName(context, WakeActivity::class.java),
            0,
        )

        assertEquals(R.style.Theme_WakeMyWay_Launch, mainActivity.theme)
        assertNotEquals(R.style.Theme_WakeMyWay_Launch, wakeActivity.theme)
    }
}

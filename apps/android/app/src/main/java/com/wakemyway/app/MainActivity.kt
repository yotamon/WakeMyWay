package com.wakemyway.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.ui.navigation.WakeMyWayApp
import com.wakemyway.app.ui.theme.WakeMyWayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val kernel = AlarmKernel(this)
        val health = kernel.reconcile()
        recordCapabilities(WakeTimingTrace(this), health)

        setContent {
            WakeMyWayTheme {
                WakeMyWayApp()
            }
        }
    }
}

private fun recordCapabilities(
    timingTrace: WakeTimingTrace,
    health: AlarmHealth,
) {
    val occurrence = health.nextOccurrence ?: health.activeOccurrence ?: return
    timingTrace.capabilities(
        occurrenceId = occurrence.id,
        exactAlarmAllowed = health.exactAlarmAllowed,
        notificationsAllowed = health.notificationsAllowed,
        fullScreenIntentAllowed = health.fullScreenIntentAllowed,
    )
}

package com.wakemyway.app.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Android presentation capabilities required for a controllable Wake My Way alarm.
 *
 * These are execution-safety capabilities. Exact-alarm access is deliberately not part of this
 * value because it answers a different question: whether Android may schedule a future occurrence.
 */
data class AlarmPresentationCapabilities(
    val notificationsAllowed: Boolean,
    val highImportanceChannel: Boolean,
    val fullScreenIntentAllowed: Boolean,
) {
    val ready: Boolean
        get() = notificationsAllowed && highImportanceChannel && fullScreenIntentAllowed
}

enum class AlarmRepairTarget {
    EXACT_ALARM,
    NOTIFICATIONS,
    ACTIVE_WAKE_CHANNEL,
    FULL_SCREEN_INTENT,
    NONE,
}

/**
 * Future-scheduling readiness. A new/reconciled occurrence needs exact-alarm capability as well as
 * a presentation path that keeps Stop/Snooze immediately reachable.
 */
fun AlarmHealth.repairTarget(): AlarmRepairTarget = when {
    !exactAlarmAllowed -> AlarmRepairTarget.EXACT_ALARM
    else -> activeWakeRepairTarget()
}

/**
 * Safety of an occurrence that is firing or already active.
 *
 * Exact-alarm access is intentionally ignored here. Once Android has delivered an occurrence,
 * losing the ability to schedule another exact alarm must not silence a wake that still has safe,
 * reachable terminal controls. Snooze remains independently fail-closed if exact scheduling is no
 * longer possible.
 */
fun AlarmHealth.activeWakeRepairTarget(): AlarmRepairTarget = when {
    !notificationsAllowed -> AlarmRepairTarget.NOTIFICATIONS
    !notificationChannelHighImportance -> AlarmRepairTarget.ACTIVE_WAKE_CHANNEL
    !fullScreenIntentAllowed -> AlarmRepairTarget.FULL_SCREEN_INTENT
    else -> AlarmRepairTarget.NONE
}

object AlarmPresentationAccess {
    const val CHANNEL_ID = "active-wake"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Active wake alarms",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Critical Wake My Way alarm playback and wake controls"
                // AlarmPlaybackService owns audible alarm playback. The channel itself stays silent
                // so Android cannot produce a second overlapping notification sound.
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    fun snapshot(context: Context): AlarmPresentationCapabilities {
        ensureChannel(context)
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(NotificationManager::class.java)
        val notificationPermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        val notificationsAllowed = notificationPermissionGranted && manager.areNotificationsEnabled()
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        val highImportanceChannel = channel != null &&
            channel.importance >= NotificationManager.IMPORTANCE_HIGH
        val fullScreenIntentAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                manager.canUseFullScreenIntent()

        return AlarmPresentationCapabilities(
            notificationsAllowed = notificationsAllowed,
            highImportanceChannel = highImportanceChannel,
            fullScreenIntentAllowed = fullScreenIntentAllowed,
        )
    }
}

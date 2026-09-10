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
 * Android presentation capabilities that are required for a controllable Wake My Way alarm.
 *
 * Critical audio can still execute when one of these capabilities is missing, but the product must
 * not call that state Wake Ready: without notifications / a high-importance channel / full-screen
 * alarm access, a sleeping user can end up hearing critical audio without the intended Wake Surface
 * or immediately reachable Stop/Snooze controls.
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
 * One canonical priority for critical wake repair. Product copy and the action it launches must
 * never disagree when more than one Android capability is missing.
 */
fun AlarmHealth.repairTarget(): AlarmRepairTarget = when {
    !exactAlarmAllowed -> AlarmRepairTarget.EXACT_ALARM
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

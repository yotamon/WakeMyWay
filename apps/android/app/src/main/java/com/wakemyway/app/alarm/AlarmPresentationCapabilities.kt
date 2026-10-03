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
 * Android presentation capabilities required for a controllable WakeMyWay alarm.
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

enum class AlarmReadinessState {
    OFF,
    READY,
    NEEDS_ATTENTION,
}

data class AlarmReadinessProjection(
    val state: AlarmReadinessState,
    val repairTarget: AlarmRepairTarget,
)

fun AlarmReadinessProjection.shouldOfferRepair(): Boolean =
    state == AlarmReadinessState.NEEDS_ATTENTION

fun projectAlarmReadiness(
    enabled: Boolean,
    scheduleHealth: AlarmScheduleHealth?,
    systemHealth: AlarmHealth,
): AlarmReadinessProjection = when {
    !enabled -> AlarmReadinessProjection(AlarmReadinessState.OFF, AlarmRepairTarget.NONE)
    scheduleHealth?.ready == true -> AlarmReadinessProjection(AlarmReadinessState.READY, AlarmRepairTarget.NONE)
    else -> AlarmReadinessProjection(
        state = AlarmReadinessState.NEEDS_ATTENTION,
        repairTarget = systemHealth.futureSchedulingRepairTarget(),
    )
}

fun AlarmHealth.repairTarget(): AlarmRepairTarget = when {
    activeOccurrence != null -> activeWakeRepairTarget()
    !exactAlarmAllowed -> AlarmRepairTarget.EXACT_ALARM
    else -> activeWakeRepairTarget()
}

fun AlarmHealth.futureSchedulingRepairTarget(): AlarmRepairTarget = when {
    !exactAlarmAllowed -> AlarmRepairTarget.EXACT_ALARM
    else -> activeWakeRepairTarget()
}

fun AlarmHealth.activeWakeRepairTarget(): AlarmRepairTarget = when {
    !notificationsAllowed -> AlarmRepairTarget.NOTIFICATIONS
    !notificationChannelHighImportance -> AlarmRepairTarget.ACTIVE_WAKE_CHANNEL
    !fullScreenIntentAllowed -> AlarmRepairTarget.FULL_SCREEN_INTENT
    else -> AlarmRepairTarget.NONE
}

object AlarmPresentationAccess {
    const val CHANNEL_ID = "active-wake"
    const val IN_APP_CHANNEL_ID = "active-wake-in-app"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_ID,
                    "Active wake alarms",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Critical WakeMyWay alarm playback and wake controls"
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    IN_APP_CHANNEL_ID,
                    "Active wake status",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Background status while the WakeMyWay wake screen is already open"
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                },
            ),
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

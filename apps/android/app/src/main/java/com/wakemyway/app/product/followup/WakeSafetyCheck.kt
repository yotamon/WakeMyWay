package com.wakemyway.app.product.followup

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wakemyway.app.MainActivity
import com.wakemyway.app.R
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryRepository
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.app.product.learning.WakeLearningRepository
import com.wakemyway.core.learning.WakeCalibrationOutcome
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal fun WakeHistoryEntry.needsMorningSafetyCheck(): Boolean =
    terminalReason == WakeHistoryTerminalReason.STOPPED && calibration == null

/**
 * Non-critical follow-up after an explicit early Stop.
 *
 * WorkManager is intentionally used here rather than AlarmManager: this check may drift and may be
 * dropped entirely without affecting alarm reliability. Unique work prevents duplicate follow-ups.
 */
object WakeSafetyCheckScheduler {
    const val DELAY_MINUTES = 15L
    private val enqueueExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "WakeSafetyCheck").apply { isDaemon = true }
    }

    fun scheduleAsync(
        context: Context,
        occurrenceId: WakeOccurrenceId,
    ) {
        val appContext = context.applicationContext
        enqueueExecutor.execute {
            runCatching { schedule(appContext, occurrenceId) }
        }
    }

    internal fun schedule(
        context: Context,
        occurrenceId: WakeOccurrenceId,
    ) {
        val appContext = context.applicationContext
        WakeSafetyCheckNotifications.ensureChannel(appContext)
        val request = OneTimeWorkRequest.Builder(WakeSafetyCheckWorker::class.java)
            .setInitialDelay(DELAY_MINUTES, TimeUnit.MINUTES)
            .setInputData(
                Data.Builder()
                    .putString(KEY_OCCURRENCE_ID, occurrenceId.value)
                    .build(),
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            workName(occurrenceId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun resolve(
        context: Context,
        occurrenceId: WakeOccurrenceId,
    ) {
        val appContext = context.applicationContext
        WorkManager.getInstance(appContext).cancelUniqueWork(workName(occurrenceId))
        WakeSafetyCheckNotifications.cancel(appContext, occurrenceId)
    }

    internal fun workName(occurrenceId: WakeOccurrenceId): String =
        "wake-safety-check:${occurrenceId.value}"

    internal const val KEY_OCCURRENCE_ID = "wake_occurrence_id"
}

class WakeSafetyCheckWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result {
        val occurrenceId = inputData.getString(WakeSafetyCheckScheduler.KEY_OCCURRENCE_ID)
            ?.takeIf(String::isNotBlank)
            ?.let(::WakeOccurrenceId)
            ?: return Result.success()

        val entry = WakeHistoryRepository(applicationContext)
            .list()
            .firstOrNull { it.occurrenceId == occurrenceId }
            ?: return Result.success()

        if (!entry.needsMorningSafetyCheck()) return Result.success()

        WakeSafetyCheckNotifications.show(applicationContext, occurrenceId)
        return Result.success()
    }
}

/**
 * Quick notification actions close the feedback loop without reopening the alarm surface.
 * The full four-way calibration remains available in Insights.
 */
class WakeSafetyCheckActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID)
            ?.takeIf(String::isNotBlank)
            ?.let(::WakeOccurrenceId)
            ?: return
        val outcome = when (intent.action) {
            ACTION_STILL_UP -> WakeCalibrationOutcome.GOT_UP
            ACTION_BACK_TO_BED -> WakeCalibrationOutcome.RETURNED_TO_BED
            else -> return
        }

        val entry = WakeHistoryRepository(context.applicationContext)
            .list()
            .firstOrNull { it.occurrenceId == occurrenceId }
        if (entry?.needsMorningSafetyCheck() == true) {
            runCatching {
                WakeLearningRepository(context.applicationContext)
                    .submitCalibration(occurrenceId, outcome)
            }
        }

        WakeSafetyCheckScheduler.resolve(context, occurrenceId)
    }

    companion object {
        const val ACTION_STILL_UP = "com.wakemyway.app.action.SAFETY_CHECK_STILL_UP"
        const val ACTION_BACK_TO_BED = "com.wakemyway.app.action.SAFETY_CHECK_BACK_TO_BED"
        const val EXTRA_OCCURRENCE_ID = "wake_occurrence_id"
    }
}

private object WakeSafetyCheckNotifications {
    private const val CHANNEL_ID = "morning_follow_up"
    private const val CHANNEL_NAME = "Morning follow-ups"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.wake_safety_check_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        occurrenceId: WakeOccurrenceId,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)
        val openAppIntent = PendingIntent.getActivity(
            context,
            notificationId(occurrenceId),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stillUpIntent = actionPendingIntent(
            context = context,
            occurrenceId = occurrenceId,
            action = WakeSafetyCheckActionReceiver.ACTION_STILL_UP,
            requestCodeSalt = 1,
        )
        val backToBedIntent = actionPendingIntent(
            context = context,
            occurrenceId = occurrenceId,
            action = WakeSafetyCheckActionReceiver.ACTION_BACK_TO_BED,
            requestCodeSalt = 2,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.wake_safety_check_title))
            .setContentText(context.getString(R.string.wake_safety_check_body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.wake_safety_check_body)),
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .addAction(
                android.R.drawable.ic_menu_compass,
                context.getString(R.string.wake_safety_check_still_up),
                stillUpIntent,
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.wake_safety_check_back_to_bed),
                backToBedIntent,
            )
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(
                notificationId(occurrenceId),
                notification,
            )
        }
    }

    fun cancel(
        context: Context,
        occurrenceId: WakeOccurrenceId,
    ) {
        NotificationManagerCompat.from(context).cancel(notificationId(occurrenceId))
    }

    private fun actionPendingIntent(
        context: Context,
        occurrenceId: WakeOccurrenceId,
        action: String,
        requestCodeSalt: Int,
    ): PendingIntent {
        val requestCode = notificationId(occurrenceId) xor requestCodeSalt
        val intent = Intent(context, WakeSafetyCheckActionReceiver::class.java)
            .setAction(action)
            .putExtra(WakeSafetyCheckActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId.value)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(occurrenceId: WakeOccurrenceId): Int =
        0x574D0000 xor (occurrenceId.value.hashCode() and 0x0000FFFF)
}

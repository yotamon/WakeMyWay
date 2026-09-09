package com.wakemyway.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wakemyway.app.R
import com.wakemyway.app.WakeActivity
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.Duration

class AlarmPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var toneFallback: ToneGenerator? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val kernel = AlarmKernel(this)

        // Android can recreate a service without redelivering its previous Intent. The durable
        // active occurrence is the recovery authority, so a process restart cannot silently
        // convert an already-firing alarm into silence.
        if (intent == null) {
            val active = kernel.activeOccurrence()
            if (active == null) {
                stopSelf()
                return START_NOT_STICKY
            }
            return ensureActiveWake(kernel, active.id)
        }

        val rawId = intent.getStringExtra(EXTRA_OCCURRENCE_ID) ?: return START_NOT_STICKY
        val occurrenceId = WakeOccurrenceId(rawId)

        return when (intent.action) {
            ACTION_START -> ensureActiveWake(kernel, occurrenceId)

            ACTION_STOP -> {
                kernel.stopActive(occurrenceId)
                stopExecution()
                START_NOT_STICKY
            }

            ACTION_SNOOZE -> {
                kernel.snoozeActive(occurrenceId, DEFAULT_SNOOZE)
                stopExecution()
                START_NOT_STICKY
            }

            else -> START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        releasePlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureActiveWake(
        kernel: AlarmKernel,
        occurrenceId: WakeOccurrenceId,
    ): Int {
        val beginResult = kernel.beginActive(occurrenceId)
        if (beginResult == BeginActiveResult.STALE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val playbackAlreadyActive = mediaPlayer?.isPlaying == true || toneFallback != null
        startForeground(NOTIFICATION_ID, alarmNotification(occurrenceId))
        if (!playbackAlreadyActive) {
            WakeTimingTrace(this).foreground(occurrenceId)
        }
        startPlayback(occurrenceId)
        return START_REDELIVER_INTENT
    }

    private fun startPlayback(occurrenceId: WakeOccurrenceId) {
        if (mediaPlayer?.isPlaying == true || toneFallback != null) return

        runCatching {
            resources.openRawResourceFd(R.raw.emergency_alarm).use { descriptor ->
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                    isLooping = true
                    prepare()
                    start()
                }
            }
            WakeTimingTrace(this).audioStarted(occurrenceId)
        }.onFailure {
            mediaPlayer?.release()
            mediaPlayer = null
            toneFallback = ToneGenerator(AudioManager.STREAM_ALARM, 100).also { tone ->
                tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
            }
            WakeTimingTrace(this).audioStarted(occurrenceId)
        }
    }

    private fun stopExecution() {
        releasePlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayback() {
        mediaPlayer?.runCatching { stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        toneFallback?.stopTone()
        toneFallback?.release()
        toneFallback = null
    }

    private fun alarmNotification(occurrenceId: WakeOccurrenceId) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("Wake My Way")
        .setContentText("Time to wake up")
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setAutoCancel(false)
        .setContentIntent(wakeActivityIntent(occurrenceId))
        .setFullScreenIntent(wakeActivityIntent(occurrenceId), true)
        .addAction(
            android.R.drawable.ic_lock_idle_alarm,
            "Snooze 5 min",
            commandIntent(ACTION_SNOOZE, occurrenceId),
        )
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            commandIntent(ACTION_STOP, occurrenceId),
        )
        .build()

    private fun wakeActivityIntent(occurrenceId: WakeOccurrenceId): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, WakeActivity::class.java)
            .setData(intentIdentity("wake-ui", occurrenceId))
            .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun commandIntent(
        action: String,
        occurrenceId: WakeOccurrenceId,
    ): PendingIntent = PendingIntent.getService(
        this,
        0,
        Intent(this, AlarmPlaybackService::class.java)
            .setAction(action)
            .setData(intentIdentity(action.substringAfterLast('.').lowercase(), occurrenceId))
            .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun intentIdentity(kind: String, occurrenceId: WakeOccurrenceId): Uri =
        Uri.Builder()
            .scheme("wakemyway")
            .authority("wake")
            .appendPath(kind)
            .appendPath(occurrenceId.value)
            .build()

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Active wake alarms",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Critical Wake My Way alarm playback"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "active-wake"
        private const val NOTIFICATION_ID = 4100
        private const val ACTION_START = "com.wakemyway.action.START_WAKE"
        private const val ACTION_STOP = "com.wakemyway.action.STOP_WAKE"
        private const val ACTION_SNOOZE = "com.wakemyway.action.SNOOZE_WAKE"
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"
        private val DEFAULT_SNOOZE: Duration = Duration.ofMinutes(5)

        fun start(context: Context, occurrenceId: WakeOccurrenceId) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_START)
                    .setData(commandIdentity("start", occurrenceId))
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }

        fun requestStop(context: Context, occurrenceId: WakeOccurrenceId) {
            context.startService(
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_STOP)
                    .setData(commandIdentity("stop", occurrenceId))
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }

        fun requestSnooze(context: Context, occurrenceId: WakeOccurrenceId) {
            context.startService(
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_SNOOZE)
                    .setData(commandIdentity("snooze", occurrenceId))
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }

        private fun commandIdentity(kind: String, occurrenceId: WakeOccurrenceId): Uri =
            Uri.Builder()
                .scheme("wakemyway")
                .authority("wake-command")
                .appendPath(kind)
                .appendPath(occurrenceId.value)
                .build()
    }
}

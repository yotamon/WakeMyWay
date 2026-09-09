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
        val rawId = intent?.getStringExtra(EXTRA_OCCURRENCE_ID) ?: return START_NOT_STICKY
        val occurrenceId = WakeOccurrenceId(rawId)

        when (intent.action) {
            ACTION_START -> {
                val beginResult = AlarmKernel(this).beginActive(occurrenceId)
                if (beginResult == BeginActiveResult.STALE) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, alarmNotification(occurrenceId))
                startPlayback()
                return START_REDELIVER_INTENT
            }

            ACTION_STOP -> {
                AlarmKernel(this).stopActive(occurrenceId)
                stopExecution()
            }

            ACTION_SNOOZE -> {
                AlarmKernel(this).snoozeActive(occurrenceId, DEFAULT_SNOOZE)
                stopExecution()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releasePlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPlayback() {
        if (mediaPlayer?.isPlaying == true || toneFallback != null) return

        runCatching {
            val descriptor = resources.openRawResourceFd(R.raw.emergency_alarm)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                descriptor.close()
                isLooping = true
                prepare()
                start()
            }
        }.onFailure {
            mediaPlayer?.release()
            mediaPlayer = null
            toneFallback = ToneGenerator(AudioManager.STREAM_ALARM, 100).also { tone ->
                tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
            }
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
            commandIntent(ACTION_SNOOZE, occurrenceId, 1),
        )
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            commandIntent(ACTION_STOP, occurrenceId, 2),
        )
        .build()

    private fun wakeActivityIntent(occurrenceId: WakeOccurrenceId): PendingIntent = PendingIntent.getActivity(
        this,
        occurrenceId.value.hashCode() and Int.MAX_VALUE,
        Intent(this, WakeActivity::class.java)
            .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun commandIntent(
        action: String,
        occurrenceId: WakeOccurrenceId,
        discriminator: Int,
    ): PendingIntent = PendingIntent.getService(
        this,
        (occurrenceId.value.hashCode() * 31 + discriminator) and Int.MAX_VALUE,
        Intent(this, AlarmPlaybackService::class.java)
            .setAction(action)
            .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

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
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }

        fun requestStop(context: Context, occurrenceId: WakeOccurrenceId) {
            context.startService(
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_STOP)
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }

        fun requestSnooze(context: Context, occurrenceId: WakeOccurrenceId) {
            context.startService(
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_SNOOZE)
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }
    }
}

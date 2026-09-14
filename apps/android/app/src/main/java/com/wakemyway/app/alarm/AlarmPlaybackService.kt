package com.wakemyway.app.alarm

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wakemyway.app.R
import com.wakemyway.app.WakeActivity
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.Duration

class AlarmPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var toneFallback: ToneGenerator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val restoreAlarmVolume = Runnable { setCriticalPlaybackVolume(FULL_VOLUME) }

    override fun onCreate() {
        super.onCreate()
        AlarmPresentationAccess.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val kernel = AlarmKernel(this)
        val trace = WakeTimingTrace(this)

        // Android can recreate a service without redelivering its previous Intent. The durable
        // active occurrence is the recovery authority, but only while the wake remains controllable.
        if (intent == null) {
            val active = kernel.activeOccurrence()
            if (active == null) {
                stopSelf()
                return START_NOT_STICKY
            }
            trace.serviceRecovered(active.id)
            return ensureActiveWake(kernel, active.id)
        }

        val rawId = intent.getStringExtra(EXTRA_OCCURRENCE_ID)
            ?: return preserveCurrentExecutionOrStop(kernel)
        val occurrenceId = WakeOccurrenceId(rawId)

        return when (intent.action) {
            ACTION_START -> ensureActiveWake(kernel, occurrenceId)

            ACTION_STOP -> {
                if (!kernel.stopActive(occurrenceId)) {
                    preserveCurrentExecutionOrStop(kernel)
                } else {
                    trace.stopped(occurrenceId)
                    stopExecution()
                    START_NOT_STICKY
                }
            }

            ACTION_SNOOZE -> {
                val replacement = kernel.snoozeActive(occurrenceId, DEFAULT_SNOOZE)
                if (replacement == null) {
                    // A delayed PendingIntent from an older occurrence, or a failed exact-alarm
                    // replacement, must never tear down the current wake. If a durable active wake
                    // exists, re-assert its foreground/audio execution instead.
                    preserveCurrentExecutionOrStop(kernel)
                } else {
                    trace.snoozed(occurrenceId)
                    trace.expected(
                        occurrence = replacement,
                        scenario = WakeTimingTrace.SCENARIO_SNOOZE_REPLACEMENT,
                        expectFullScreen = kernel.health().fullScreenIntentAllowed,
                    )
                    stopExecution()
                    START_NOT_STICKY
                }
            }

            ACTION_VOICE_WINDOW -> {
                if (kernel.activeOccurrence()?.id == occurrenceId) {
                    beginVoiceWindow()
                    START_STICKY
                } else {
                    preserveCurrentExecutionOrStop(kernel)
                }
            }

            ACTION_RESTORE_CRITICAL_VOLUME -> {
                if (kernel.activeOccurrence()?.id == occurrenceId) {
                    endVoiceWindow()
                    START_STICKY
                } else {
                    preserveCurrentExecutionOrStop(kernel)
                }
            }

            else -> preserveCurrentExecutionOrStop(kernel)
        }
    }

    override fun onDestroy() {
        releasePlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun preserveCurrentExecutionOrStop(kernel: AlarmKernel): Int {
        val current = kernel.activeOccurrence()
        if (current != null) return ensureActiveWake(kernel, current.id)

        stopExecution()
        return START_NOT_STICKY
    }

    private fun ensureActiveWake(
        kernel: AlarmKernel,
        occurrenceId: WakeOccurrenceId,
    ): Int {
        // Defense in depth for service recreation / redelivered START intents. If notification,
        // channel, exact-alarm or full-screen access is no longer healthy, never start or resurrect
        // critical audio that may be impossible for the user to control.
        if (kernel.health().repairTarget() != AlarmRepairTarget.NONE) {
            kernel.cancelSchedule()
            stopExecution()
            return START_NOT_STICKY
        }

        return when (kernel.beginActive(occurrenceId)) {
            BeginActiveResult.STALE -> {
                // Do not tear down an unrelated valid active wake because a delayed START arrived.
                preserveCurrentExecutionOrStop(kernel)
            }

            BeginActiveResult.CONFLICT -> {
                // Another schedule already owns the one physical Active Wake Execution. Reassert it
                // rather than letting the colliding occurrence steal foreground/audio authority.
                preserveCurrentExecutionOrStop(kernel)
            }

            BeginActiveResult.STARTED,
            BeginActiveResult.ALREADY_ACTIVE,
            -> {
                val activeId = kernel.activeOccurrence()?.id ?: occurrenceId
                val playbackAlreadyActive = mediaPlayer?.isPlaying == true || toneFallback != null
                startForeground(NOTIFICATION_ID, alarmNotification(activeId))
                if (!playbackAlreadyActive) {
                    WakeTimingTrace(this).foreground(activeId)
                }
                startPlayback(activeId)
                START_REDELIVER_INTENT
            }
        }
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
                    setVolume(FULL_VOLUME, FULL_VOLUME)
                    start()
                }
            }
            WakeTimingTrace(this).audioStarted(occurrenceId)
        }.onFailure {
            mediaPlayer?.release()
            mediaPlayer = null
            // The tone fallback intentionally stays at full alarm volume. We only duck the bundled
            // MediaPlayer path because a degraded playback path must remain maximally reliable.
            toneFallback = ToneGenerator(AudioManager.STREAM_ALARM, 100).also { tone ->
                tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
            }
            WakeTimingTrace(this).audioStarted(occurrenceId)
        }
    }

    /**
     * Makes a spoken/listening turn intelligible without surrendering alarm ownership.
     * The service, not WakeActivity, owns the lease and restores full volume automatically.
     */
    private fun beginVoiceWindow() {
        setCriticalPlaybackVolume(VOICE_WINDOW_VOLUME)
        mainHandler.removeCallbacks(restoreAlarmVolume)
        mainHandler.postDelayed(restoreAlarmVolume, VOICE_WINDOW_MAX_MILLIS)
    }

    private fun endVoiceWindow() {
        mainHandler.removeCallbacks(restoreAlarmVolume)
        setCriticalPlaybackVolume(FULL_VOLUME)
    }

    private fun setCriticalPlaybackVolume(volume: Float) {
        mediaPlayer?.runCatching { setVolume(volume, volume) }
    }

    private fun stopExecution() {
        releasePlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayback() {
        mainHandler.removeCallbacks(restoreAlarmVolume)
        mediaPlayer?.runCatching { stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        toneFallback?.stopTone()
        toneFallback?.release()
        toneFallback = null
    }

    private fun alarmNotification(occurrenceId: WakeOccurrenceId) = NotificationCompat.Builder(
        this,
        AlarmPresentationAccess.CHANNEL_ID,
    )
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(getString(R.string.app_name))
        .setContentText("Wake alarm is active")
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setAutoCancel(false)
        .setSound(null)
        .setFullScreenIntent(wakePendingIntent(occurrenceId), true)
        .addAction(
            android.R.drawable.ic_media_pause,
            "Snooze 5 min",
            commandPendingIntent(ACTION_SNOOZE, occurrenceId),
        )
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop alarm",
            commandPendingIntent(ACTION_STOP, occurrenceId),
        )
        .build()

    private fun wakePendingIntent(occurrenceId: WakeOccurrenceId): PendingIntent {
        val intent = Intent(this, WakeActivity::class.java)
            .setData(commandIdentity("wake", occurrenceId))
            .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    setPendingIntentCreatorBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                    )
                }
            }.toBundle()
        } else {
            Bundle.EMPTY
        }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            options,
        )
    }

    private fun commandPendingIntent(
        action: String,
        occurrenceId: WakeOccurrenceId,
    ): PendingIntent = PendingIntent.getService(
        this,
        0,
        Intent(this, AlarmPlaybackService::class.java)
            .setAction(action)
            .setData(commandIdentity(action, occurrenceId))
            .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun commandIdentity(kind: String, occurrenceId: WakeOccurrenceId): Uri =
        Uri.Builder()
            .scheme("wakemyway")
            .authority("active-wake")
            .appendPath(kind)
            .appendPath(occurrenceId.value)
            .build()

    companion object {
        const val ACTION_START = "com.wakemyway.action.START_ALARM_PLAYBACK"
        const val ACTION_STOP = "com.wakemyway.action.STOP_ALARM_PLAYBACK"
        const val ACTION_SNOOZE = "com.wakemyway.action.SNOOZE_ALARM_PLAYBACK"
        const val ACTION_VOICE_WINDOW = "com.wakemyway.action.VOICE_WINDOW"
        const val ACTION_RESTORE_CRITICAL_VOLUME = "com.wakemyway.action.RESTORE_CRITICAL_VOLUME"
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"
        val DEFAULT_SNOOZE: Duration = Duration.ofMinutes(5)

        private const val NOTIFICATION_ID = 1001
        private const val FULL_VOLUME = 1.0f
        private const val VOICE_WINDOW_VOLUME = 0.22f
        private const val VOICE_WINDOW_MAX_MILLIS = 15_000L

        fun start(context: Context, occurrenceId: WakeOccurrenceId) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_START)
                    .setData(
                        Uri.Builder()
                            .scheme("wakemyway")
                            .authority("active-wake-command")
                            .appendPath("start")
                            .appendPath(occurrenceId.value)
                            .build(),
                    )
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }

        fun requestStop(context: Context, occurrenceId: WakeOccurrenceId) {
            context.startService(
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_STOP)
                    .setData(
                        Uri.Builder()
                            .scheme("wakemyway")
                            .authority("active-wake-command")
                            .appendPath("stop")
                            .appendPath(occurrenceId.value)
                            .build(),
                    )
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }

        fun requestSnooze(context: Context, occurrenceId: WakeOccurrenceId) {
            context.startService(
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_SNOOZE)
                    .setData(
                        Uri.Builder()
                            .scheme("wakemyway")
                            .authority("active-wake-command")
                            .appendPath("snooze")
                            .appendPath(occurrenceId.value)
                            .build(),
                    )
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }

        fun beginVoiceWindow(context: Context, occurrenceId: WakeOccurrenceId) {
            context.startService(
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_VOICE_WINDOW)
                    .setData(
                        Uri.Builder()
                            .scheme("wakemyway")
                            .authority("active-wake-command")
                            .appendPath("voice-window")
                            .appendPath(occurrenceId.value)
                            .build(),
                    )
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }

        fun restoreCriticalVolume(context: Context, occurrenceId: WakeOccurrenceId) {
            context.startService(
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_RESTORE_CRITICAL_VOLUME)
                    .setData(
                        Uri.Builder()
                            .scheme("wakemyway")
                            .authority("active-wake-command")
                            .appendPath("restore-critical-volume")
                            .appendPath(occurrenceId.value)
                            .build(),
                    )
                    .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            )
        }
    }
}

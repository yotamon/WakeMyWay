package com.wakemyway.app.alarm

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wakemyway.app.WakeActivity
import com.wakemyway.core.schedule.WakeOccurrenceId

class AlarmPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var toneFallback: ToneGenerator? = null
    private var activePlaybackSpec: WakeSoundPlaybackSpec = WakeSoundCatalog.emergencyPlaybackSpec
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hapticsStarted = false
    private var startupCpuLock: PowerManager.WakeLock? = null
    private var emergencyToneCpuLock: PowerManager.WakeLock? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val restoreAlarmVolume = Runnable {
        setCriticalPlaybackVolume(activePlaybackSpec.criticalGain)
    }
    private val reArmToneFallback = object : Runnable {
        override fun run() {
            val tone = toneFallback ?: return
            // The emergency tone is a finite one-shot; without re-arming, the last-resort audio
            // layer would degrade to silence while the wake is still active.
            runCatching { tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD) }
            mainHandler.postDelayed(this, TONE_RE_ARM_MILLIS)
        }
    }
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { _ ->
        // Deliberately inert. Critical wake audio never ducks or yields: only Stop/Snooze end the
        // wake, and the voice-window volume lease is owned by beginVoiceWindow/endVoiceWindow.
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

        val redeliveredStart =
            intent.action == ACTION_START && flags and Service.START_FLAG_REDELIVERY != 0
        if (redeliveredStart && kernel.activeOccurrence()?.id == occurrenceId) {
            trace.serviceRecovered(occurrenceId)
        }

        return when (intent.action) {
            ACTION_START -> ensureActiveWake(
                kernel = kernel,
                occurrenceId = occurrenceId,
                validatePresentation = redeliveredStart,
            )

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
                val policy = kernel.activePolicy(occurrenceId) ?: CriticalWakePolicy.DEFAULT
                if (!policy.snoozeEnabled) {
                    preserveCurrentExecutionOrStop(kernel)
                } else {
                    val replacement = kernel.snoozeActive(occurrenceId, policy.snoozeDuration)
                    if (replacement == null) {
                        // A delayed PendingIntent from an older occurrence, or a failed exact-alarm
                        // replacement, must never tear down the current wake. If a durable active
                        // wake exists, re-assert its foreground/audio execution instead.
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
            }

            ACTION_VOICE_WINDOW -> {
                val policy = kernel.activePolicy(occurrenceId) ?: CriticalWakePolicy.DEFAULT
                if (kernel.activeOccurrence()?.id == occurrenceId && policy.voiceCheckInEnabled) {
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
        validatePresentation: Boolean = true,
    ): Int {
        // The broadcast wake lock dies with AlarmReceiver; hold the CPU across kernel reads and
        // MediaPlayer prepare so the device cannot re-suspend in the fire-to-audio window.
        holdCpuUntilAudioStarts()

        // AlarmReceiver has already performed the presentation-safety preflight immediately before
        // a fresh ACTION_START. Repeating NotificationManager / special-access calls here can block
        // the direct-boot critical path before startForeground(). Recovery and redelivery still
        // revalidate because they no longer have that fresh receiver authority.
        if (validatePresentation && kernel.health().repairTarget() != AlarmRepairTarget.NONE) {
            kernel.cancelSchedule()
            stopExecution()
            return START_NOT_STICKY
        }

        when (kernel.beginActive(occurrenceId)) {
            BeginActiveResult.STALE -> {
                // A delayed start for one alarm must not tear down a different valid active wake.
                return preserveCurrentExecutionOrStop(kernel)
            }

            BeginActiveResult.CONFLICT -> {
                // Exactly one physical wake execution may own audio/foreground presentation. A
                // colliding valid occurrence remains kernel state to reconcile after the current
                // chain ends, while the existing active occurrence keeps service authority.
                return preserveCurrentExecutionOrStop(kernel)
            }

            BeginActiveResult.STARTED,
            BeginActiveResult.ALREADY_ACTIVE,
            -> Unit
        }

        val activeId = kernel.activeOccurrence()?.id ?: occurrenceId
        val policy = kernel.activePolicy(activeId) ?: CriticalWakePolicy.DEFAULT
        val playbackAlreadyActive = playbackAudiblyActive()
        startForeground(NOTIFICATION_ID, alarmNotification(activeId, policy))
        if (!playbackAlreadyActive) {
            WakeTimingTrace(this).foreground(activeId)
        }
        startPlayback(activeId, policy)
        if (mediaPlayer != null) {
            // The looping player owns CPU retention for the rest of the wake via setWakeMode.
            releaseCpuHold()
        }
        return START_REDELIVER_INTENT
    }

    private fun playbackAudiblyActive(): Boolean =
        runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false) || toneFallback != null

    private fun startPlayback(
        occurrenceId: WakeOccurrenceId,
        policy: CriticalWakePolicy,
    ) {
        if (playbackAudiblyActive()) return

        // A player that exists but can no longer report playback must be released before its
        // replacement is built, or a degraded redelivery would leak media decoders.
        releaseIdleMediaPlayer()

        requestCriticalAudioFocus()
        startCriticalHaptics()

        val resolved = WakeSoundCatalog.resolve(this, policy.soundId)
        activePlaybackSpec = resolved.playback

        val selectedStarted = runCatching {
            mediaPlayer = createLoopingPlayer(
                rawResourceId = resolved.rawResourceId,
                volume = resolved.playback.criticalGain,
            )
        }.isSuccess

        if (selectedStarted) {
            WakeTimingTrace(this).audioStarted(occurrenceId)
            return
        }

        // A branded resource can still fail to decode even when it is present in the APK. Retry the
        // known emergency WAV before using the platform tone so corrupt media can never become
        // silence and never weaken Stop/Snooze controllability.
        if (!resolved.usedEmergencyFallback) {
            activePlaybackSpec = WakeSoundCatalog.emergencyPlaybackSpec
            val emergencyStarted = runCatching {
                mediaPlayer = createLoopingPlayer(
                    rawResourceId = com.wakemyway.app.R.raw.emergency_alarm,
                    volume = activePlaybackSpec.criticalGain,
                )
            }.isSuccess
            if (emergencyStarted) {
                WakeTimingTrace(this).audioStarted(occurrenceId)
                return
            }
        }

        // Last-resort platform tone stays at full alarm volume. It deliberately does not duck for
        // speech because degraded critical playback must remain maximally reliable. Its constructor
        // can itself fail on a degraded audio HAL, so it is guarded: on total audio failure the
        // wake stays visible, haptic and controllable through the foreground notification instead
        // of crashing the service into a silent restart loop.
        activePlaybackSpec = WakeSoundCatalog.emergencyPlaybackSpec
        val tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 100) }.getOrNull()
        if (tone != null) {
            toneFallback = tone
            runCatching { tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD) }
            mainHandler.removeCallbacks(reArmToneFallback)
            mainHandler.postDelayed(reArmToneFallback, TONE_RE_ARM_MILLIS)
            holdCpuForToneFallback()
            WakeTimingTrace(this).audioStarted(occurrenceId)
        }
    }

    private fun createLoopingPlayer(
        @RawRes rawResourceId: Int,
        volume: Float,
    ): MediaPlayer {
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(alarmAudioAttributes())
            resources.openRawResourceFd(rawResourceId).use { descriptor ->
                player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
            player.isLooping = true
            // Keeps the CPU up through prepare and looping playback when the screen stays off.
            player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            player.prepare()
            player.setVolume(volume, volume)
            player.start()
            return player
        } catch (error: Throwable) {
            // A player that failed mid-construction must never leak below its replacement.
            runCatching { player.release() }
            throw error
        }
    }

    private fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun releaseIdleMediaPlayer() {
        mediaPlayer?.runCatching { release() }
        mediaPlayer = null
    }

    private fun requestCriticalAudioFocus() {
        if (audioFocusRequest != null) return
        audioFocusRequest = runCatching {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            // The granted/failed result never gates critical playback: an alarm must remain
            // audible across media sessions and calls, and only Stop/Snooze end the wake.
            getSystemService(AudioManager::class.java).requestAudioFocus(request)
            request
        }.getOrNull()
    }

    private fun abandonCriticalAudioFocus() {
        val request = audioFocusRequest ?: return
        audioFocusRequest = null
        runCatching {
            getSystemService(AudioManager::class.java).abandonAudioFocusRequest(request)
        }
    }

    private fun startCriticalHaptics() {
        if (hapticsStarted) return
        val vibrator = systemVibrator() ?: return
        hapticsStarted = runCatching {
            // Redundant physical layer that survives a fully silent audio stack: vibration, like
            // audio, is owned exclusively by the active wake execution.
            vibrator.vibrate(
                VibrationEffect.createWaveform(HAPTIC_TIMINGS, HAPTIC_AMPLITUDES, 0),
            )
        }.isSuccess
    }

    private fun stopCriticalHaptics() {
        hapticsStarted = false
        systemVibrator()?.let { vibrator -> runCatching { vibrator.cancel() } }
    }

    private fun systemVibrator(): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
    }.getOrNull()

    private fun holdCpuUntilAudioStarts() {
        if (startupCpuLock != null) return
        startupCpuLock = runCatching {
            getSystemService(PowerManager::class.java).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "wakemyway:activeWakeStartup",
            ).apply {
                setReferenceCounted(false)
                acquire(CPU_HOLD_TIMEOUT_MILLIS)
            }
        }.getOrNull()
    }

    private fun holdCpuForToneFallback() {
        if (emergencyToneCpuLock != null) return
        emergencyToneCpuLock = runCatching {
            getSystemService(PowerManager::class.java).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "wakemyway:activeWakeTone",
            ).apply {
                setReferenceCounted(false)
                // The tone path has no system-managed player wake lock to lean on, and its
                // handler-driven re-arming needs the CPU. Released in releasePlayback.
                acquire()
            }
        }.getOrNull()
    }

    private fun releaseCpuHold() {
        startupCpuLock?.let { lock -> runCatching { if (lock.isHeld) lock.release() } }
        startupCpuLock = null
    }

    private fun releaseToneCpuHold() {
        emergencyToneCpuLock?.let { lock -> runCatching { if (lock.isHeld) lock.release() } }
        emergencyToneCpuLock = null
    }

    /**
     * Makes a spoken/listening turn intelligible without surrendering alarm ownership.
     * The service, not WakeActivity, owns the lease and restores the selected profile's critical
     * volume automatically. A ToneGenerator fallback is intentionally never ducked.
     */
    private fun beginVoiceWindow() {
        setCriticalPlaybackVolume(activePlaybackSpec.voiceWindowGain)
        mainHandler.removeCallbacks(restoreAlarmVolume)
        mainHandler.postDelayed(restoreAlarmVolume, VOICE_WINDOW_MAX_MILLIS)
    }

    private fun endVoiceWindow() {
        mainHandler.removeCallbacks(restoreAlarmVolume)
        setCriticalPlaybackVolume(activePlaybackSpec.criticalGain)
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
        mainHandler.removeCallbacks(reArmToneFallback)
        releaseIdleMediaPlayer()
        toneFallback?.let { tone ->
            runCatching { tone.stopTone() }
            runCatching { tone.release() }
        }
        toneFallback = null
        abandonCriticalAudioFocus()
        stopCriticalHaptics()
        releaseCpuHold()
        releaseToneCpuHold()
        activePlaybackSpec = WakeSoundCatalog.emergencyPlaybackSpec
    }

    private fun alarmNotification(
        occurrenceId: WakeOccurrenceId,
        policy: CriticalWakePolicy,
    ) = NotificationCompat.Builder(this, AlarmPresentationAccess.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("Wake My Way")
        .setContentText("Time to wake up")
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(wakeActivityIntent(occurrenceId))
        .setFullScreenIntent(wakeActivityIntent(occurrenceId), true)
        .apply {
            if (policy.snoozeEnabled) {
                addAction(
                    android.R.drawable.ic_lock_idle_alarm,
                    "Snooze ${policy.snoozeDuration.toMinutes().coerceAtLeast(1)} min",
                    commandIntent(ACTION_SNOOZE, occurrenceId),
                )
            }
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                commandIntent(ACTION_STOP, occurrenceId),
            )
        }
        .build()

    /**
     * Android 15+ no longer grants a PendingIntent creator's background-activity-launch privilege
     * by default. A full-screen alarm is one of the narrow cases that genuinely must be able to
     * start while WMW itself is not visible, so opt this PendingIntent into creator BAL explicitly.
     *
     * API 36 split the old ALLOWED mode. For a user-scheduled locked-screen alarm we need the
     * background-capable ALLOW_ALWAYS mode; ALLOW_IF_VISIBLE would defeat the full-screen alarm
     * because the app is intentionally not visible before wake time.
     */
    private fun wakeActivityIntent(occurrenceId: WakeOccurrenceId): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, WakeActivity::class.java)
            .setData(intentIdentity("wake-ui", occurrenceId))
            .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        wakeActivityPendingIntentOptions(),
    )

    @Suppress("DEPRECATION")
    private fun wakeActivityPendingIntentOptions(): Bundle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null

        val backgroundStartMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        } else {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
        return ActivityOptions.makeBasic()
            .setPendingIntentCreatorBackgroundActivityStartMode(backgroundStartMode)
            .toBundle()
    }

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

    companion object {
        private const val NOTIFICATION_ID = 4100
        private const val ACTION_START = "com.wakemyway.action.START_WAKE"
        private const val ACTION_STOP = "com.wakemyway.action.STOP_WAKE"
        private const val ACTION_SNOOZE = "com.wakemyway.action.SNOOZE_WAKE"
        private const val ACTION_VOICE_WINDOW = "com.wakemyway.action.VOICE_WINDOW"
        private const val ACTION_RESTORE_CRITICAL_VOLUME = "com.wakemyway.action.RESTORE_CRITICAL_VOLUME"
        private const val VOICE_WINDOW_MAX_MILLIS = 12_000L
        private const val TONE_RE_ARM_MILLIS = 4_000L
        private const val CPU_HOLD_TIMEOUT_MILLIS = 20_000L
        private val HAPTIC_TIMINGS = longArrayOf(0L, 900L, 700L)
        private val HAPTIC_AMPLITUDES = intArrayOf(0, 255, 0)
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"

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
            sendCommand(context, ACTION_STOP, "stop", occurrenceId)
        }

        fun requestSnooze(context: Context, occurrenceId: WakeOccurrenceId) {
            sendCommand(context, ACTION_SNOOZE, "snooze", occurrenceId)
        }

        fun requestVoiceWindow(context: Context, occurrenceId: WakeOccurrenceId) {
            sendCommand(context, ACTION_VOICE_WINDOW, "voice-window", occurrenceId)
        }

        fun requestCriticalVolume(context: Context, occurrenceId: WakeOccurrenceId) {
            sendCommand(context, ACTION_RESTORE_CRITICAL_VOLUME, "critical-volume", occurrenceId)
        }

        private fun sendCommand(
            context: Context,
            action: String,
            kind: String,
            occurrenceId: WakeOccurrenceId,
        ) {
            context.startService(
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(action)
                    .setData(commandIdentity(kind, occurrenceId))
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

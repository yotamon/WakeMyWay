package com.wakemyway.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.wakemyway.core.alarm.WakeSoundId

/**
 * Non-critical editor preview for bundled WakeMyWay sounds.
 *
 * Preview intentionally uses the media audio path rather than alarm playback. It never owns Alarm
 * Kernel authority, never starts the foreground alarm service, and never substitutes the private
 * emergency sound for a missing branded resource.
 */
class WakeSoundPreviewPlayer internal constructor(
    private val resolveResource: (WakeSoundId) -> Int?,
    private val createSession: (Int) -> PreviewSession?,
) : AutoCloseable {
    internal interface PreviewSession {
        fun stopAndRelease()
    }

    var currentSoundId: WakeSoundId? = null
        private set

    fun play(soundId: WakeSoundId): Boolean {
        stop()

        val resourceId = resolveResource(soundId) ?: return false
        val newSession = runCatching { createSession(resourceId) }.getOrNull() ?: return false
        session = newSession
        currentSoundId = soundId
        return true
    }

    fun stop() {
        session?.runCatching { stopAndRelease() }
        session = null
        currentSoundId = null
    }

    override fun close() = stop()

    private var session: PreviewSession? = null

    companion object {
        private const val PREVIEW_GAIN = 0.42f

        fun create(context: Context): WakeSoundPreviewPlayer {
            val appContext = context.applicationContext
            return WakeSoundPreviewPlayer(
                resolveResource = { soundId ->
                    WakeSoundCatalog.resolve(appContext, soundId)
                        .takeUnless { it.usedEmergencyFallback }
                        ?.rawResourceId
                },
                createSession = { resourceId ->
                    AndroidPreviewSession.start(
                        context = appContext,
                        rawResourceId = resourceId,
                        gain = PREVIEW_GAIN,
                    )
                },
            )
        }
    }
}

private class AndroidPreviewSession(
    private val player: MediaPlayer,
    private val audioManager: AudioManager,
    private val focusRequest: AudioFocusRequest,
) : WakeSoundPreviewPlayer.PreviewSession {
    override fun stopAndRelease() {
        player.runCatching { stop() }
        player.release()
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    companion object {
        fun start(
            context: Context,
            rawResourceId: Int,
            gain: Float,
        ): AndroidPreviewSession? {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val audioManager = context.getSystemService(AudioManager::class.java) ?: return null
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { }
                .build()

            if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return null
            }

            var player: MediaPlayer? = null
            return runCatching {
                context.resources.openRawResourceFd(rawResourceId).use { descriptor ->
                    player = MediaPlayer().apply {
                        setAudioAttributes(attributes)
                        setDataSource(
                            descriptor.fileDescriptor,
                            descriptor.startOffset,
                            descriptor.length,
                        )
                        isLooping = false
                        prepare()
                        setVolume(gain, gain)
                        start()
                    }
                }
                AndroidPreviewSession(
                    player = requireNotNull(player),
                    audioManager = audioManager,
                    focusRequest = focusRequest,
                )
            }.getOrElse {
                player?.release()
                audioManager.abandonAudioFocusRequest(focusRequest)
                null
            }
        }
    }
}

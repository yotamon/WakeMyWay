package com.wakemyway.app.character

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.wakemyway.core.character.CharacterSpec
import com.wakemyway.core.character.LocalVoiceCandidate
import com.wakemyway.core.character.OfflineVoiceSelector
import com.wakemyway.core.character.RenderedWakeLine
import java.util.concurrent.ConcurrentHashMap

sealed interface LocalSpeechState {
    data object Initializing : LocalSpeechState

    data class Ready(
        val voiceId: String,
        val languageTag: String,
    ) : LocalSpeechState

    data class Unavailable(val reason: Reason) : LocalSpeechState {
        enum class Reason {
            ENGINE_INIT_FAILED,
            NO_OFFLINE_VOICE,
            VOICE_CONFIGURATION_FAILED,
            CLOSED,
        }
    }
}

sealed interface LocalSpeechResult {
    data object Completed : LocalSpeechResult
    data class Failed(val reason: Reason) : LocalSpeechResult

    enum class Reason {
        NOT_READY,
        ENGINE_REJECTED_UTTERANCE,
        ENGINE_PLAYBACK_ERROR,
        CLOSED,
    }
}

/**
 * Thin Android adapter for local scripted character speech.
 *
 * It deliberately refuses voices that report a network requirement. Failure never affects
 * critical alarm audio; callers should translate failure into WakeInput.SpeechFailed only when
 * this adapter is actually attached to a Wake Runtime session.
 */
class LocalCharacterSpeaker(
    context: Context,
    private val character: CharacterSpec,
    private val onStateChanged: (LocalSpeechState) -> Unit = {},
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val callbacks = ConcurrentHashMap<String, (LocalSpeechResult) -> Unit>()

    @Volatile
    private var state: LocalSpeechState = LocalSpeechState.Initializing

    @Volatile
    private var closed = false

    private var engine: TextToSpeech? = null

    init {
        engine = TextToSpeech(appContext) { status ->
            configureEngine(status)
        }
    }

    fun state(): LocalSpeechState = state

    /**
     * Starts speech only when a verified local voice is ready. If not, failure is delivered
     * immediately and no text is queued for later/network playback.
     */
    fun speak(
        line: RenderedWakeLine,
        utteranceId: String,
        onResult: (LocalSpeechResult) -> Unit = {},
    ): Boolean {
        require(utteranceId.isNotBlank()) { "Utterance id must not be blank" }
        require(line.characterId == character.id) { "Rendered line belongs to another character" }
        require(line.characterVersion == character.version) { "Rendered line uses another character version" }

        if (closed) {
            dispatchResult(onResult, LocalSpeechResult.Failed(LocalSpeechResult.Reason.CLOSED))
            return false
        }
        if (state !is LocalSpeechState.Ready) {
            dispatchResult(onResult, LocalSpeechResult.Failed(LocalSpeechResult.Reason.NOT_READY))
            return false
        }

        val tts = engine
        if (tts == null) {
            dispatchResult(onResult, LocalSpeechResult.Failed(LocalSpeechResult.Reason.NOT_READY))
            return false
        }

        callbacks.remove(utteranceId)?.let { previous ->
            dispatchResult(previous, LocalSpeechResult.Failed(LocalSpeechResult.Reason.ENGINE_PLAYBACK_ERROR))
        }
        callbacks[utteranceId] = onResult

        val result = runCatching {
            tts.speak(line.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }.getOrElse { TextToSpeech.ERROR }

        if (result != TextToSpeech.SUCCESS) {
            callbacks.remove(utteranceId)
            dispatchResult(
                onResult,
                LocalSpeechResult.Failed(LocalSpeechResult.Reason.ENGINE_REJECTED_UTTERANCE),
            )
            return false
        }
        return true
    }

    fun stop() {
        if (closed) return
        engine?.stop()
        failPending(LocalSpeechResult.Reason.ENGINE_PLAYBACK_ERROR)
    }

    override fun close() {
        if (closed) return
        closed = true
        failPending(LocalSpeechResult.Reason.CLOSED)
        engine?.runCatching {
            stop()
            shutdown()
        }
        engine = null
        publishState(LocalSpeechState.Unavailable(LocalSpeechState.Unavailable.Reason.CLOSED))
    }

    private fun configureEngine(status: Int) {
        if (closed) return
        val tts = engine
        if (status != TextToSpeech.SUCCESS || tts == null) {
            publishState(
                LocalSpeechState.Unavailable(LocalSpeechState.Unavailable.Reason.ENGINE_INIT_FAILED),
            )
            return
        }

        val voices = runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
        val candidates = voices.map { voice ->
            LocalVoiceCandidate(
                id = voice.name,
                languageTag = voice.locale.toLanguageTag(),
                networkRequired = voice.isNetworkConnectionRequired,
                quality = voice.quality,
            )
        }
        val selected = OfflineVoiceSelector.select(candidates, character.voiceLocaleTag)
        val selectedVoice = selected?.let { candidate ->
            voices.firstOrNull { voice ->
                voice.name == candidate.id && !voice.isNetworkConnectionRequired
            }
        }

        if (selected == null || selectedVoice == null) {
            publishState(
                LocalSpeechState.Unavailable(LocalSpeechState.Unavailable.Reason.NO_OFFLINE_VOICE),
            )
            return
        }

        val configured = runCatching {
            tts.voice = selectedVoice
            val rateResult = tts.setSpeechRate(character.speechRate)
            val pitchResult = tts.setPitch(character.pitch)
            rateResult == TextToSpeech.SUCCESS && pitchResult == TextToSpeech.SUCCESS
        }.getOrDefault(false)

        if (!configured || tts.voice?.isNetworkConnectionRequired != false) {
            publishState(
                LocalSpeechState.Unavailable(LocalSpeechState.Unavailable.Reason.VOICE_CONFIGURATION_FAILED),
            )
            return
        }

        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    finishUtterance(utteranceId, LocalSpeechResult.Completed)
                }

                @Deprecated("Deprecated by Android; retained for compatibility with older engines")
                override fun onError(utteranceId: String?) {
                    finishUtterance(
                        utteranceId,
                        LocalSpeechResult.Failed(LocalSpeechResult.Reason.ENGINE_PLAYBACK_ERROR),
                    )
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    finishUtterance(
                        utteranceId,
                        LocalSpeechResult.Failed(LocalSpeechResult.Reason.ENGINE_PLAYBACK_ERROR),
                    )
                }
            },
        )

        publishState(
            LocalSpeechState.Ready(
                voiceId = selectedVoice.name,
                languageTag = selectedVoice.locale.toLanguageTag(),
            ),
        )
    }

    private fun finishUtterance(
        utteranceId: String?,
        result: LocalSpeechResult,
    ) {
        if (utteranceId == null) return
        callbacks.remove(utteranceId)?.let { callback -> dispatchResult(callback, result) }
    }

    private fun failPending(reason: LocalSpeechResult.Reason) {
        val pending = callbacks.entries.toList()
        callbacks.clear()
        pending.forEach { (_, callback) ->
            dispatchResult(callback, LocalSpeechResult.Failed(reason))
        }
    }

    private fun publishState(next: LocalSpeechState) {
        state = next
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onStateChanged(next)
        } else {
            mainHandler.post { onStateChanged(next) }
        }
    }

    private fun dispatchResult(
        callback: (LocalSpeechResult) -> Unit,
        result: LocalSpeechResult,
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback(result)
        } else {
            mainHandler.post { callback(result) }
        }
    }
}

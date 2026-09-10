package com.wakemyway.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.time.Duration

sealed interface LocalVoiceAvailability {
    data object Ready : LocalVoiceAvailability

    data class Unavailable(val reason: Reason) : LocalVoiceAvailability {
        enum class Reason {
            API_TOO_OLD,
            PERMISSION_MISSING,
            ON_DEVICE_RECOGNIZER_UNAVAILABLE,
            CREATE_FAILED,
            CLOSED,
        }
    }
}

sealed interface LocalVoiceResult {
    data class Recognized(val coherent: Boolean) : LocalVoiceResult
    data object NoResponse : LocalVoiceResult
    data class Failed(val errorCode: Int) : LocalVoiceResult
    data object Cancelled : LocalVoiceResult
}

/**
 * One-turn, on-device-only speech recognition for a production Wake Session.
 *
 * Raw microphone audio and recognized text never leave this adapter and are never persisted.
 * The only semantic value exposed to Wake Runtime is whether a short response was coherent
 * enough to count as typed activation evidence.
 */
class LocalVoiceListener(
    context: Context,
    private val languageTag: String,
    private val turnTimeout: Duration = Duration.ofSeconds(9),
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var callback: ((LocalVoiceResult) -> Unit)? = null
    private var timeoutRunnable: Runnable? = null
    private var closed = false

    fun availability(): LocalVoiceAvailability {
        if (closed) return LocalVoiceAvailability.Unavailable(LocalVoiceAvailability.Unavailable.Reason.CLOSED)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return LocalVoiceAvailability.Unavailable(LocalVoiceAvailability.Unavailable.Reason.API_TOO_OLD)
        }
        if (
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return LocalVoiceAvailability.Unavailable(LocalVoiceAvailability.Unavailable.Reason.PERMISSION_MISSING)
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)) {
            return LocalVoiceAvailability.Unavailable(
                LocalVoiceAvailability.Unavailable.Reason.ON_DEVICE_RECOGNIZER_UNAVAILABLE,
            )
        }
        return LocalVoiceAvailability.Ready
    }

    fun listen(onResult: (LocalVoiceResult) -> Unit): Boolean {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "LocalVoiceListener must be used from the main thread"
        }

        if (availability() !is LocalVoiceAvailability.Ready) {
            onResult(LocalVoiceResult.Failed(SpeechRecognizer.ERROR_CLIENT))
            return false
        }

        cancel(deliverCancellation = false)
        val engine = recognizer ?: createRecognizer()?.also { recognizer = it }
        if (engine == null) {
            onResult(LocalVoiceResult.Failed(SpeechRecognizer.ERROR_CLIENT))
            return false
        }

        callback = onResult
        val timeout = Runnable {
            val active = callback ?: return@Runnable
            callback = null
            timeoutRunnable = null
            runCatching { engine.cancel() }
            active(LocalVoiceResult.NoResponse)
        }
        timeoutRunnable = timeout
        mainHandler.postDelayed(timeout, turnTimeout.toMillis())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        return runCatching {
            engine.startListening(intent)
            true
        }.getOrElse {
            complete(LocalVoiceResult.Failed(SpeechRecognizer.ERROR_CLIENT))
            false
        }
    }

    fun cancel(deliverCancellation: Boolean = true) {
        if (closed) return
        val active = callback
        callback = null
        clearTimeout()
        runCatching { recognizer?.cancel() }
        if (deliverCancellation && active != null) {
            active(LocalVoiceResult.Cancelled)
        }
    }

    override fun close() {
        if (closed) return
        cancel(deliverCancellation = false)
        closed = true
        recognizer?.runCatching { destroy() }
        recognizer = null
    }

    private fun createRecognizer(): SpeechRecognizer? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return runCatching {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext).apply {
                setRecognitionListener(listener)
            }
        }.getOrNull()
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (callback == null) return
            when (error) {
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_NO_MATCH,
                -> complete(LocalVoiceResult.NoResponse)

                else -> complete(LocalVoiceResult.Failed(error))
            }
        }

        override fun onResults(results: Bundle?) {
            val candidates = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                .orEmpty()
            val confidence = results
                ?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                ?.firstOrNull()
                ?.takeIf { it >= 0f }
            val best = candidates.firstOrNull().orEmpty().trim()
            val hasMeaningfulContent = best.count { it.isLetterOrDigit() } >= 2
            val confidenceAcceptable = confidence == null || confidence >= MIN_CONFIDENCE

            // The recognized text is deliberately discarded here. Wake Runtime receives only
            // the privacy-minimized semantic observation below.
            complete(LocalVoiceResult.Recognized(hasMeaningfulContent && confidenceAcceptable))
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun complete(result: LocalVoiceResult) {
        val active = callback ?: return
        callback = null
        clearTimeout()
        active(result)
    }

    private fun clearTimeout() {
        timeoutRunnable?.let(mainHandler::removeCallbacks)
        timeoutRunnable = null
    }

    private companion object {
        const val MIN_CONFIDENCE = 0.20f
    }
}

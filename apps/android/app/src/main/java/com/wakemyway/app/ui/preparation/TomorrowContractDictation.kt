package com.wakemyway.app.ui.preparation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * One-shot, on-device dictation for Tomorrow Contract drafting.
 *
 * Recognition text remains in the calling UI's in-memory draft and is not persisted until the
 * user explicitly saves the contract. This adapter never uses a network recognizer.
 */
internal class TomorrowContractDictation(
    context: Context,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var callback: ((Result) -> Unit)? = null
    private var closed = false

    sealed interface Result {
        data class Transcript(val text: String) : Result
        data object NoSpeech : Result
        data object Unavailable : Result
        data object Cancelled : Result
    }

    fun listen(onResult: (Result) -> Unit): Boolean {
        if (!isAvailable()) {
            onResult(Result.Unavailable)
            return false
        }

        cancel(deliverCancellation = false)
        val engine = recognizer ?: createRecognizer()?.also { recognizer = it }
        if (engine == null) {
            onResult(Result.Unavailable)
            return false
        }

        callback = onResult
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        return runCatching {
            engine.startListening(intent)
            true
        }.getOrElse {
            complete(Result.Unavailable)
            false
        }
    }

    fun cancel(deliverCancellation: Boolean = true) {
        if (closed) return
        val active = callback
        callback = null
        runCatching { recognizer?.cancel() }
        if (deliverCancellation && active != null) active(Result.Cancelled)
    }

    override fun close() {
        if (closed) return
        cancel(deliverCancellation = false)
        closed = true
        recognizer?.runCatching { destroy() }
        recognizer = null
    }

    private fun isAvailable(): Boolean {
        if (closed || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        if (
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext) }
            .getOrDefault(false)
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
            complete(
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    -> Result.NoSpeech
                    else -> Result.Unavailable
                },
            )
        }

        override fun onResults(results: Bundle?) {
            val transcript = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            complete(if (transcript.isBlank()) Result.NoSpeech else Result.Transcript(transcript))
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun complete(result: Result) {
        val active = callback ?: return
        callback = null
        active(result)
    }
}

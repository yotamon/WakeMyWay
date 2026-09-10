package com.wakemyway.app.voice

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/** Debug-only founder lab for direct OpenAI WebRTC and conversational Wake setup. */
class VoiceSpikeActivity : ComponentActivity(), DirectOpenAiWebRtcSpike.Listener {
    private lateinit var brokerUrlInput: EditText
    private lateinit var operatorTokenInput: EditText
    private lateinit var requestSpeechButton: Button
    private lateinit var markAudibleButton: Button
    private lateinit var statusView: TextView
    private lateinit var metricView: TextView
    private lateinit var eventView: TextView

    private var pendingBrokerUrl: String? = null
    private var pendingOperatorToken: String? = null
    private lateinit var spike: DirectOpenAiWebRtcSpike
    private lateinit var founderSettings: FounderRealtimeSettings
    private val measurement = VoiceSpikeMeasurementSession()

    private val microphonePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                connectWithPendingValues()
            } else {
                clearPendingSecret()
                onFailure("permission", "Microphone permission is required for the voice spike")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        spike = DirectOpenAiWebRtcSpike(applicationContext, this)
        founderSettings = FounderRealtimeSettings(applicationContext)
        setContentView(buildContent())
        renderMeasurement()
        founderSettings.load()?.let { saved ->
            brokerUrlInput.setText(saved.brokerUrl)
            statusView.text = "Conversational Wake is configured for founder dogfood. The token remains encrypted and hidden."
        }
    }

    override fun onStop() {
        super.onStop()
        spike.disconnect()
        operatorTokenInput.text?.clear()
        clearPendingSecret()
        resetMeasurementUi()
    }

    override fun onDestroy() {
        spike.close()
        super.onDestroy()
    }

    override fun onStatus(status: String) {
        statusView.text = status
    }

    override fun onConnected(coldConnectionMs: Long) {
        measurement.recordColdConnection(coldConnectionMs)
        requestSpeechButton.isEnabled = true
        markAudibleButton.isEnabled = false
        renderMeasurement()
        statusView.text =
            "Connected. For first-audible timing, stay quiet and request the fixed synthetic response."
    }

    override fun onServerEventType(type: String) {
        eventView.text = "Last server event type: $type"
    }

    override fun onFailure(stage: String, message: String) {
        statusView.text = "Failed at $stage: $message"
    }

    private fun buildContent(): ScrollView {
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(40))
        }
        scroll.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        content.addView(TextView(this).apply {
            text = "Realtime Founder Lab"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text =
                "Configure live conversational Alfred for the debug founder Wake, or run the isolated synthetic WebRTC measurement. Alarm Kernel and Wake Runtime remain local authorities."
            textSize = 15f
            setPadding(0, dp(8), 0, dp(20))
        })

        brokerUrlInput = EditText(this).apply {
            hint = "https://…${FounderRealtimeSettings.FOUNDER_WAKE_PATH}"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        content.addView(brokerUrlInput, matchWidth())

        operatorTokenInput = EditText(this).apply {
            hint = "Internal founder bearer token"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        content.addView(operatorTokenInput, matchWidth())

        val saveWakeConversationButton = Button(this).apply {
            text = "Enable conversational Wake"
            setOnClickListener { requestSaveFounderWake() }
        }
        content.addView(saveWakeConversationButton, matchWidth(topMargin = 16))

        val clearWakeConversationButton = Button(this).apply {
            text = "Clear conversational Wake setup"
            setOnClickListener {
                founderSettings.clear()
                operatorTokenInput.text?.clear()
                statusView.text = "Conversational Wake configuration cleared. Local Alfred remains available."
            }
        }
        content.addView(clearWakeConversationButton, matchWidth(topMargin = 8))

        content.addView(TextView(this).apply {
            text =
                "Founder dogfood privacy: live microphone audio and model audio use the OpenAI Realtime API. WMW does not persist transcripts or audio and does not send Tomorrow Contract or prepared private context in this phase. API retention may still apply unless the OpenAI project has approved Zero Data Retention."
            textSize = 13f
            setPadding(0, dp(10), 0, dp(20))
        })

        val connectButton = Button(this).apply {
            text = "Connect synthetic voice measurement"
            setOnClickListener { requestConnect() }
        }
        content.addView(connectButton, matchWidth())

        val disconnectButton = Button(this).apply {
            text = "Disconnect measurement"
            setOnClickListener {
                spike.disconnect()
                operatorTokenInput.text?.clear()
                clearPendingSecret()
                resetMeasurementUi()
                statusView.text = "Synthetic measurement disconnected."
            }
        }
        content.addView(disconnectButton, matchWidth(topMargin = 8))

        statusView = TextView(this).apply {
            text = "Idle."
            textSize = 16f
            setPadding(0, dp(24), 0, dp(8))
        }
        content.addView(statusView)

        metricView = TextView(this).apply {
            textSize = 15f
        }
        content.addView(metricView)

        requestSpeechButton = Button(this).apply {
            text = "1 · Request fixed synthetic response"
            isEnabled = false
            setOnClickListener { requestSyntheticFirstSpeechProbe() }
        }
        content.addView(requestSpeechButton, matchWidth(topMargin = 16))

        markAudibleButton = Button(this).apply {
            text = "2 · I heard the first syllable"
            isEnabled = false
            setOnClickListener { markFirstAudibleSpeech() }
        }
        content.addView(markAudibleButton, matchWidth(topMargin = 8))

        content.addView(TextView(this).apply {
            text =
                "The synthetic measurement remains non-sensitive and separate from the real Wake Session."
            textSize = 13f
            setPadding(0, dp(8), 0, dp(12))
        })

        eventView = TextView(this).apply {
            text = "Last server event type: none"
            textSize = 14f
            setPadding(0, dp(8), 0, dp(16))
        }
        content.addView(eventView)

        return scroll
    }

    private fun requestSaveFounderWake() {
        val brokerUrl = brokerUrlInput.text?.toString()?.trim().orEmpty()
        val operatorToken = operatorTokenInput.text?.toString().orEmpty()
        if (brokerUrl.isBlank() || operatorToken.isBlank()) {
            statusView.text = "Founder wake broker endpoint and bearer token are required."
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Enable live wake conversation?")
            .setMessage(
                "During a founder Wake Session, microphone audio and Alfred's responses will use OpenAI Realtime. Wake My Way will not persist audio/transcripts or send Tomorrow Contract context. OpenAI API retention may still apply unless this project has Zero Data Retention enabled. The alarm always remains local and independently controllable.",
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Enable") { _, _ ->
                runCatching { founderSettings.save(brokerUrl, operatorToken) }
                    .onSuccess {
                        operatorTokenInput.text?.clear()
                        statusView.text =
                            "Conversational Wake configured. The founder token is encrypted with Android Keystore."
                    }
                    .onFailure { error ->
                        statusView.text =
                            "Could not save conversational Wake: ${error.message?.take(160) ?: error.javaClass.simpleName}"
                    }
            }
            .show()
    }

    private fun requestConnect() {
        val founderBrokerUrl = brokerUrlInput.text?.toString()?.trim().orEmpty()
        val operatorToken = operatorTokenInput.text?.toString().orEmpty()
        if (founderBrokerUrl.isBlank() || operatorToken.isBlank()) {
            statusView.text = "Broker endpoint and operator token are required."
            return
        }

        pendingBrokerUrl = founderBrokerUrl.replace(
            FounderRealtimeSettings.FOUNDER_WAKE_PATH,
            SYNTHETIC_SPIKE_PATH,
        )
        pendingOperatorToken = operatorToken
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            connectWithPendingValues()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun connectWithPendingValues() {
        val brokerUrl = pendingBrokerUrl
        val operatorToken = pendingOperatorToken
        if (brokerUrl.isNullOrBlank() || operatorToken.isNullOrBlank()) {
            onFailure("configuration", "Spike configuration was cleared")
            return
        }

        operatorTokenInput.text?.clear()
        clearPendingSecret()
        resetMeasurementUi()
        runCatching { spike.connect(brokerUrl, operatorToken) }
            .onFailure { error ->
                onFailure(
                    "configuration",
                    error.message?.take(160) ?: error.javaClass.simpleName,
                )
            }
    }

    private fun requestSyntheticFirstSpeechProbe() {
        runCatching { spike.requestSyntheticMeasurementResponse() }
            .onSuccess { requestedAtMs ->
                measurement.recordSyntheticResponseRequested(requestedAtMs)
                requestSpeechButton.isEnabled = false
                markAudibleButton.isEnabled = true
                renderMeasurement()
                statusView.text =
                    "Synthetic response requested. Tap the second button at the first audible syllable."
            }
            .onFailure { error ->
                onFailure("first-speech", error.message?.take(160) ?: error.javaClass.simpleName)
            }
    }

    private fun markFirstAudibleSpeech() {
        runCatching {
            measurement.recordFirstAudibleObserved(SystemClock.elapsedRealtime())
        }.onSuccess { elapsedMs ->
            markAudibleButton.isEnabled = false
            requestSpeechButton.isEnabled = true
            renderMeasurement()
            statusView.text =
                "First audible upper bound recorded: ${elapsedMs} ms. This smoke result is not persisted."
        }.onFailure { error ->
            onFailure("first-speech", error.message?.take(160) ?: error.javaClass.simpleName)
        }
    }

    private fun resetMeasurementUi() {
        measurement.reset()
        if (::requestSpeechButton.isInitialized) requestSpeechButton.isEnabled = false
        if (::markAudibleButton.isInitialized) markAudibleButton.isEnabled = false
        if (::metricView.isInitialized) renderMeasurement()
    }

    private fun renderMeasurement() {
        val snapshot = measurement.snapshot()
        val cold = snapshot.coldConnectionMs?.let { "$it ms" } ?: "not measured"
        val firstSpeech = snapshot.firstSpeechMs?.let { "$it ms" }
            ?: if (snapshot.responseProbePending) "waiting for audible tap" else "not measured"
        metricView.text = buildString {
            append("Cold connection: ").append(cold)
            append("\nFirst audible response: ").append(firstSpeech)
            append("\nObservation: operator tap upper bound")
            append("\nProtocol: ").append(snapshot.protocolId)
            append("\nIn-memory smoke evidence only; no automatic benchmark sample is written.")
        }
    }

    private fun clearPendingSecret() {
        pendingOperatorToken = null
        pendingBrokerUrl = null
    }

    private fun matchWidth(topMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { this.topMargin = dp(topMargin) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val SYNTHETIC_SPIKE_PATH = "/api/internal/voice-spike/direct-openai-token"
    }
}

package com.wakemyway.app.voice

import android.Manifest
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

/** Debug-only founder lab for the isolated M8 direct OpenAI WebRTC candidate. */
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
        setContentView(buildContent())
        renderMeasurement()
    }

    override fun onStop() {
        super.onStop()
        spike.disconnect()
        operatorTokenInput.text?.clear()
        clearPendingSecret()
        resetMeasurementUi()
        statusView.text = "Disconnected because the lab left the foreground."
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
            text = "M8 · Direct OpenAI WebRTC Spike"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text =
                "Debug-only, synthetic/non-sensitive engineering lab. It is not connected to the alarm or Wake Runtime. No transcripts or audio are stored."
            textSize = 15f
            setPadding(0, dp(8), 0, dp(20))
        })

        brokerUrlInput = EditText(this).apply {
            hint = "Token broker endpoint (HTTPS)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        content.addView(brokerUrlInput, matchWidth())

        operatorTokenInput = EditText(this).apply {
            hint = "Internal operator bearer token"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        content.addView(operatorTokenInput, matchWidth())

        val connectButton = Button(this).apply {
            text = "Connect synthetic voice spike"
            setOnClickListener { requestConnect() }
        }
        content.addView(connectButton, matchWidth(topMargin = 16))

        val disconnectButton = Button(this).apply {
            text = "Disconnect"
            setOnClickListener {
                spike.disconnect()
                operatorTokenInput.text?.clear()
                clearPendingSecret()
                resetMeasurementUi()
                statusView.text = "Disconnected."
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
                "First-audible timing uses your tap as a conservative upper bound. Human reaction time is included. Protocol audio/control events are diagnostics only and never substitute for the audible observation."
            textSize = 13f
            setPadding(0, dp(8), 0, dp(12))
        })

        eventView = TextView(this).apply {
            text = "Last server event type: none"
            textSize = 14f
            setPadding(0, dp(8), 0, dp(16))
        }
        content.addView(eventView)

        content.addView(TextView(this).apply {
            text =
                "Privacy rule: use synthetic content only. Until the exact OpenAI project data-control posture is verified, do not speak Tomorrow Contract, calendar, health, identity, or other private content here."
            textSize = 13f
        })

        return scroll
    }

    private fun requestConnect() {
        val brokerUrl = brokerUrlInput.text?.toString()?.trim().orEmpty()
        val operatorToken = operatorTokenInput.text?.toString().orEmpty()
        if (brokerUrl.isBlank() || operatorToken.isBlank()) {
            statusView.text = "Broker endpoint and operator token are required."
            return
        }

        pendingBrokerUrl = brokerUrl
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

        // The operator token is handed to the in-memory client and immediately removed
        // from the editable UI/pending state. It is never persisted or passed by Intent.
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
}

package com.wakemyway.app.voice

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
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
    private lateinit var statusView: TextView
    private lateinit var metricView: TextView
    private lateinit var eventView: TextView

    private var pendingBrokerUrl: String? = null
    private var pendingOperatorToken: String? = null
    private lateinit var spike: DirectOpenAiWebRtcSpike

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
    }

    override fun onStop() {
        super.onStop()
        spike.disconnect()
        operatorTokenInput.text?.clear()
        clearPendingSecret()
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
        metricView.text = "Cold connection: ${coldConnectionMs} ms\nMeasurement is in-memory only."
        statusView.text = "Connected. Speak a short synthetic phrase to test turn-taking."
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
            text = "Cold connection: not measured"
            textSize = 15f
        }
        content.addView(metricView)

        eventView = TextView(this).apply {
            text = "Last server event type: none"
            textSize = 14f
            setPadding(0, dp(8), 0, dp(16))
        }
        content.addView(eventView)

        content.addView(TextView(this).apply {
            text =
                "Privacy rule: use a neutral synthetic phrase only. Until the exact OpenAI project data-control posture is verified, do not speak Tomorrow Contract, calendar, health, identity, or other private content here."
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
        runCatching { spike.connect(brokerUrl, operatorToken) }
            .onFailure { error ->
                onFailure(
                    "configuration",
                    error.message?.take(160) ?: error.javaClass.simpleName,
                )
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

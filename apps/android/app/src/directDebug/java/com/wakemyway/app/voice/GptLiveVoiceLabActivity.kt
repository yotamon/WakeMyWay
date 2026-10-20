package com.wakemyway.app.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * DirectDebug-only GPT-Live experiment. Never sets an alarm, never writes
 * Wake History, never permits AI to change Stop/Snooze or WakeRuntime.
 * A real account and the server WMW_GPT_LIVE_ENABLED gate are required.
 */
class GptLiveVoiceLabActivity : ComponentActivity(), GptLiveWebRtcSession.Observer {
    private var session: GptLiveWebRtcSession? = null
    private var selected = GptLiveWebRtcSession.Character.ALFRED
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startSession() else showStatus("Microphone permission is required for Live preview.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 32)
        }
        val scroller = ScrollView(this).apply { addView(body) }
        setContentView(scroller)
        body.addView(TextView(this).apply {
            text = "GPT-Live Voice Lab"
            textSize = 28f
        })
        body.addView(TextView(this).apply {
            text = "Experimental. Voice sessions cost money; one at a time. Local alarms are unaffected."
            textSize = 14f
        })
        status = TextView(this).apply {
            text = "Not connected."
            textSize = 16f
        }
        body.addView(status)
        for (character in GptLiveWebRtcSession.Character.entries) {
            body.addView(Button(this).apply {
                text = character.wire.replaceFirstChar { it.uppercase() }
                setOnClickListener {
                    selected = character
                    showStatus("Selected ${character.wire}. Start to hear this voice.")
                }
            })
        }
        startButton = Button(this).apply {
            text = "Start a short preview"
            setOnClickListener { askToStart() }
        }
        stopButton = Button(this).apply {
            text = "Stop and close Live"
            isEnabled = false
            setOnClickListener { stopSession() }
        }
        body.addView(startButton)
        body.addView(stopButton)
        transcript = TextView(this).apply { textSize = 14f }
        body.addView(transcript)
    }

    private fun askToStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) startSession() else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startSession() {
        if (session != null) return
        showStatus("Opening approved GPT-Live session…")
        startButton.isEnabled = false
        stopButton.isEnabled = true
        transcript.text = ""
        GptLiveWebRtcSession(this, this).also {
            session = it
            it.connect(selected)
        }
    }

    private fun stopSession() {
        showStatus("Closing…")
        stopButton.isEnabled = false
        session?.requestClose()
    }

    override fun onReady() = runOnUiThread {
        showStatus("Connected. Preview started; speak normally to test interruptions.")
        session?.startLabConversation()
    }

    override fun onTranscript(fromUser: Boolean, delta: String) = runOnUiThread {
        val label = if (fromUser) "You" else selected.wire
        // No persistence. Bound UI text in memory; closing clears it.
        transcript.text = (transcript.text.toString() + "\n" + label + ": " + delta).takeLast(3_000)
    }

    override fun onSessionClosed(usageConfirmed: Boolean) = runOnUiThread {
        session = null
        startButton.isEnabled = true
        stopButton.isEnabled = false
        showStatus(if (usageConfirmed) "Session closed." else "Disconnected; final usage unconfirmed.")
    }

    override fun onFailure(stage: String) = runOnUiThread {
        showStatus("Live unavailable: $stage. No impact on alarms.")
    }

    private fun showStatus(message: String) { status.text = message }

    override fun onStop() {
        // A voice preview must never keep a hidden microphone/session running.
        session?.requestClose()
        super.onStop()
    }

    override fun onDestroy() {
        session?.close()
        session = null
        super.onDestroy()
    }
}

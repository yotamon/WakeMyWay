package com.wakemyway.app.voice

import android.Manifest
import com.wakemyway.app.alarm.AlarmKernel
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * Direct distribution GPT-Live preview experiment. Never sets an alarm, never writes
 * Wake History, never permits AI to change Stop/Snooze or WakeRuntime.
 * A real account and the server WMW_GPT_LIVE_ENABLED gate are required.
 */
class GptLiveVoiceLabActivity : ComponentActivity(), GptLiveWebRtcSession.Observer {
    private var session: GptLiveWebRtcSession? = null
    private var selected = GptLiveWebRtcSession.Character.ALFRED
    private var settings = GptLiveWebRtcSession.VoiceLabSettings()
    private val choiceButtons = mutableListOf<Button>()
    private var lastTranscriptFromUser: Boolean? = null
    private var lastTestedCharacter = selected
    private var lastTestedSettings = settings
    private var lastSessionUsageSeconds = 0
    private var assistantSpoke = false
    private lateinit var naturalnessRating: RatingBar
    private lateinit var pleasantnessRating: RatingBar
    private lateinit var rateButton: Button
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
        body.addView(TextView(this).apply { text = "Choose a voice and expression profile" })
        for (character in GptLiveWebRtcSession.Character.entries) {
            body.addView(Button(this).apply {
                text = character.wire.replaceFirstChar { it.uppercase() }
                setOnClickListener {
                    selected = character
                    showStatus("Selected ${character.wire}. Start to hear this voice.")
                }
                choiceButtons.add(this)
            })
        }
        addChoice(body, "Energy", GptLiveWebRtcSession.Energy.entries,
            { settings.energy }, { settings = settings.copy(energy = it) })
        addChoice(body, "Directness", GptLiveWebRtcSession.Directness.entries,
            { settings.directness }, { settings = settings.copy(directness = it) })
        addChoice(body, "Conversation", GptLiveWebRtcSession.ConversationAmount.entries,
            { settings.conversationAmount }, { settings = settings.copy(conversationAmount = it) })
        addChoice(body, "Humour", GptLiveWebRtcSession.Humor.entries,
            { settings.humor }, { settings = settings.copy(humor = it) })
        addChoice(body, "Language", GptLiveWebRtcSession.Locale.entries,
            { settings.locale }, { settings = settings.copy(locale = it) })
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
        body.addView(TextView(this).apply {
            text = "After a preview, rate only audio you actually heard. Scores stay on this device; speech/transcripts are not saved."
        })
        naturalnessRating = RatingBar(this).apply {
            numStars = 5
            stepSize = 1f
            rating = 3f
            contentDescription = "Naturalness rating from one to five stars"
        }
        pleasantnessRating = RatingBar(this).apply {
            numStars = 5
            stepSize = 1f
            rating = 3f
            contentDescription = "Morning pleasantness rating from one to five stars"
        }
        body.addView(TextView(this).apply { text = "How human and natural?" })
        body.addView(naturalnessRating)
        body.addView(TextView(this).apply { text = "Would you enjoy hearing this every morning?" })
        body.addView(pleasantnessRating)
        rateButton = Button(this).apply {
            text = "Save local voice ratings"
            isEnabled = false
            setOnClickListener { saveRating() }
        }
        body.addView(rateButton)
    }

    private fun <T : Enum<T>> addChoice(
        container: LinearLayout,
        label: String,
        options: List<T>,
        current: () -> T,
        set: (T) -> Unit,
    ) {
        container.addView(Button(this).apply {
            fun refresh() { text = "$label: ${current().name.lowercase()}" }
            refresh()
            setOnClickListener {
                val index = options.indexOf(current())
                set(options[(index + 1) % options.size])
                refresh()
            }
            choiceButtons.add(this)
        })
    }

    private fun saveRating() {
        if (!assistantSpoke || session != null || !rateButton.isEnabled) return
        val preferences = getSharedPreferences("wmw_gpt_live_lab_ratings", MODE_PRIVATE)
        val list = runCatching {
            JSONArray(preferences.getString("reviews", "[]") ?: "[]")
        }.getOrElse { JSONArray() }
        // Bound retention; exclude audio, transcript, email, token, SDP and private context.
        while (list.length() >= 50) list.remove(0)
        list.put(JSONObject()
            .put("at", System.currentTimeMillis())
            .put("character", lastTestedCharacter.wire)
            .put("expression", JSONObject()
                .put("energy", lastTestedSettings.energy.name.lowercase())
                .put("directness", lastTestedSettings.directness.name.lowercase())
                .put("conversation", lastTestedSettings.conversationAmount.name.lowercase())
                .put("humor", lastTestedSettings.humor.name.lowercase())
                .put("locale", lastTestedSettings.locale.name.lowercase()))
            .put("naturalness", naturalnessRating.rating.toInt().coerceIn(1, 5))
            .put("pleasantness", pleasantnessRating.rating.toInt().coerceIn(1, 5))
            .put("observedUsageSeconds", lastSessionUsageSeconds))
        preferences.edit().putString("reviews", list.toString()).apply()
        rateButton.isEnabled = false
        showStatus("Saved local rating #${list.length()}. No voice recording retained.")
    }

    private fun askToStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) startSession() else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startSession() {
        if (session != null) return
        // The lab is not allowed to change audio routing while a real alarm
        // owns critical playback. If the kernel cannot be read, fail closed.
        val noActiveAlarm = runCatching { AlarmKernel(this).activeOccurrence() == null }
            .getOrDefault(false)
        if (!noActiveAlarm) {
            showStatus("Finish the active alarm before testing Live audio.")
            return
        }
        showStatus("Opening approved GPT-Live session…")
        startButton.isEnabled = false
        choiceButtons.forEach { it.isEnabled = false }
        stopButton.isEnabled = true
        transcript.text = ""
        lastTranscriptFromUser = null
        lastTestedCharacter = selected
        lastTestedSettings = settings
        lastSessionUsageSeconds = 0
        assistantSpoke = false
        rateButton.isEnabled = false
        GptLiveWebRtcSession(this, this).also {
            session = it
            it.connect(selected, settings)
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
        // Transcript deltas are fragments, not turns. Preserve whitespace and
        // keep adjacent fragments from one speaker on the same readable line.
        val heading = if (lastTranscriptFromUser == fromUser) "" else
            "\n" + (if (fromUser) "You" else selected.wire) + ": "
        lastTranscriptFromUser = fromUser
        if (!fromUser && delta.isNotBlank()) assistantSpoke = true
        // Never persist conversation text. Bound only the on-screen memory buffer.
        transcript.text = (transcript.text.toString() + heading + delta).takeLast(3_000)
    }

    override fun onUsageSeconds(seconds: Int, final: Boolean) = runOnUiThread {
        lastSessionUsageSeconds = maxOf(lastSessionUsageSeconds, seconds)
        showStatus("Live voice usage: ${seconds}s" + if (final) " (final)." else ".")
    }

    override fun onSessionClosed(usageConfirmed: Boolean) = runOnUiThread {
        session = null
        startButton.isEnabled = true
        choiceButtons.forEach { it.isEnabled = true }
        stopButton.isEnabled = false
        rateButton.isEnabled = assistantSpoke
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

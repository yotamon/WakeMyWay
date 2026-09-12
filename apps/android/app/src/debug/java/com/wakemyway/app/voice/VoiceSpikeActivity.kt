package com.wakemyway.app.voice

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Founder-only conversational Alfred setup.
 *
 * Infrastructure URLs, OpenAI credentials and WMW internal bearer secrets are intentionally absent
 * from this surface. The founder enters one access code once; the app exchanges it for an expiring
 * installation credential and stores that credential encrypted with Android Keystore.
 */
class VoiceSpikeActivity : ComponentActivity() {
    private lateinit var settings: FounderRealtimeSettings
    private val pairingClient = FounderRealtimePairingClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "wmw-founder-pairing").apply { isDaemon = true }
    }

    private lateinit var statusPill: TextView
    private lateinit var statusTitle: TextView
    private lateinit var statusBody: TextView
    private lateinit var codeInput: EditText
    private lateinit var primaryButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var progress: ProgressBar

    @Volatile
    private var destroyed = false
    private var serverReady = false
    private var firstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        settings = FounderRealtimeSettings(applicationContext)
        setContentView(buildContent())
    }

    override fun onResume() {
        super.onResume()
        if (!firstResume && settings.configured()) {
            // Returning to the screen after another foreground transition should refresh the
            // server truth, but do not create duplicate startup requests from onCreate + onResume.
            refreshState()
            return
        }
        firstResume = false
        refreshState()
    }

    override fun onDestroy() {
        destroyed = true
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildContent(): ScrollView {
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(36), dp(28), dp(48))
        }
        scroll.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        content.addView(TextView(this).apply {
            text = "Conversational Alfred"
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Natural, two-way morning conversation with the same local alarm safety underneath."
            textSize = 16f
            alpha = 0.75f
            setPadding(0, dp(8), 0, dp(24))
        })

        statusPill = TextView(this).apply {
            text = "CHECKING"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
        }
        content.addView(statusPill)

        statusTitle = TextView(this).apply {
            text = "Checking Alfred…"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        }
        content.addView(statusTitle)

        statusBody = TextView(this).apply {
            textSize = 15f
            alpha = 0.78f
            setPadding(0, dp(6), 0, dp(18))
        }
        content.addView(statusBody)

        progress = ProgressBar(this).apply {
            isIndeterminate = true
        }
        content.addView(progress, wrapContent())

        codeInput = EditText(this).apply {
            hint = "Founder access code"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        content.addView(codeInput, matchWidth(topMargin = 18))

        primaryButton = Button(this).apply {
            text = "Connect Alfred"
            isEnabled = false
            setOnClickListener {
                if (settings.configured()) {
                    verifyExistingConnection()
                } else {
                    requestPairingConsent()
                }
            }
        }
        content.addView(primaryButton, matchWidth(topMargin = 12))

        disconnectButton = Button(this).apply {
            text = "Disconnect conversational Alfred"
            visibility = View.GONE
            setOnClickListener {
                settings.clear()
                codeInput.text?.clear()
                refreshState()
            }
        }
        content.addView(disconnectButton, matchWidth(topMargin = 8))

        content.addView(TextView(this).apply {
            text = "What stays local"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(30), 0, dp(6))
        })
        content.addView(TextView(this).apply {
            text = "Alarm timing, critical alarm audio, Stop, Snooze, motion evidence and the decision that the wake is complete never depend on OpenAI or the network. If Realtime is unavailable, Alfred falls back to the local voice path."
            textSize = 14f
            alpha = 0.75f
        })

        content.addView(TextView(this).apply {
            text = "Privacy"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(24), 0, dp(6))
        })
        content.addView(TextView(this).apply {
            text = "During a conversational wake, live microphone audio and Alfred's generated audio use OpenAI Realtime. Wake My Way does not persist the audio or transcripts, and this founder build does not send Tomorrow Contract or prepared private context to Realtime."
            textSize = 14f
            alpha = 0.75f
        })

        val back = Button(this).apply {
            text = "Back"
            setOnClickListener { finish() }
        }
        content.addView(back, matchWidth(topMargin = 30))
        return scroll
    }

    private fun refreshState() {
        showLoading("Checking Alfred…", "Verifying the WakeMyWay Realtime service.")
        executor.execute {
            val result = runCatching { pairingClient.status() }
            mainHandler.post {
                if (destroyed) return@post
                result.fold(
                    onSuccess = { status ->
                        serverReady = status.available
                        renderIdle(status)
                    },
                    onFailure = {
                        serverReady = false
                        renderUnavailable(
                            "WakeMyWay cloud is unreachable",
                            "Your alarm remains fully local and safe. Check your connection and try again.",
                        )
                    },
                )
            }
        }
    }

    private fun renderIdle(status: FounderRealtimePairingClient.ServerStatus) {
        progress.visibility = View.GONE
        primaryButton.visibility = View.VISIBLE
        primaryButton.isEnabled = true
        disconnectButton.isEnabled = true
        val configured = settings.configured()
        disconnectButton.visibility = if (configured) View.VISIBLE else View.GONE
        codeInput.visibility = if (configured) View.GONE else View.VISIBLE

        when {
            configured && status.available -> {
                statusPill.text = "READY"
                statusTitle.text = "Alfred is connected"
                statusBody.text = "Realtime conversation is configured for this phone. The local wake path remains the automatic fallback."
                primaryButton.text = "Re-check connection"
            }
            configured && !status.available -> {
                statusPill.text = "SERVER SETUP"
                statusTitle.text = "Alfred is paired, but cloud setup is incomplete"
                statusBody.text = missingCopy(status.missing)
                primaryButton.text = "Check again"
            }
            !configured && status.available -> {
                statusPill.text = "ONE-TIME SETUP"
                statusTitle.text = "Connect Alfred"
                statusBody.text = "Enter the founder access code once. Wake My Way handles the server and OpenAI connection automatically after that."
                primaryButton.text = "Connect Alfred"
            }
            else -> renderUnavailable(
                "Conversational Alfred needs server setup",
                missingCopy(status.missing),
            )
        }
    }

    private fun requestPairingConsent() {
        if (!serverReady) {
            refreshState()
            return
        }
        val code = codeInput.text?.toString()?.trim().orEmpty()
        if (code.length < FounderRealtimePairingClient.MIN_ACCESS_CODE_LENGTH) {
            statusBody.text = "Enter the founder access code first."
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Enable conversational Alfred?")
            .setMessage(
                "During a conversational wake, live microphone audio and Alfred's generated audio use OpenAI Realtime. Wake My Way does not save the audio or transcripts. Alarm delivery and Stop/Snooze remain local even if the network fails.",
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Enable") { _, _ -> pair(code) }
            .show()
    }

    private fun pair(code: String) {
        showLoading("Connecting Alfred…", "Creating a secure credential for this installation and verifying OpenAI Realtime.")
        val installationId = settings.installationId()
        executor.execute {
            val result = runCatching {
                val paired = pairingClient.pair(code, installationId)
                pairingClient.probe(paired.deviceToken)
                paired
            }
            mainHandler.post {
                if (destroyed) return@post
                result.fold(
                    onSuccess = { paired ->
                        settings.saveInstallationCredential(
                            deviceToken = paired.deviceToken,
                            expiresAtEpochSeconds = paired.expiresAtEpochSeconds,
                        )
                        codeInput.text?.clear()
                        renderConnected()
                    },
                    onFailure = { error -> renderPairingFailure(error) },
                )
            }
        }
    }

    private fun verifyExistingConnection() {
        val config = settings.load()
        if (config == null) {
            refreshState()
            return
        }
        showLoading("Checking Alfred…", "Verifying the complete WakeMyWay → OpenAI Realtime path.")
        executor.execute {
            val result = runCatching { pairingClient.probe(config.operatorToken) }
            mainHandler.post {
                if (destroyed) return@post
                result.fold(
                    onSuccess = { renderConnected() },
                    onFailure = { error ->
                        if (
                            error is FounderRealtimePairingClient.PairingException &&
                            error.kind == FounderRealtimePairingClient.PairingException.Kind.ACCESS_CODE_REJECTED
                        ) {
                            settings.clear()
                        }
                        renderPairingFailure(error)
                    },
                )
            }
        }
    }

    private fun renderConnected() {
        progress.visibility = View.GONE
        serverReady = true
        statusPill.text = "READY"
        statusTitle.text = "Alfred is connected"
        statusBody.text = "This phone is ready for natural Realtime conversation. If the cloud path is slow or unavailable at wake time, Alfred falls back locally without affecting the alarm."
        codeInput.visibility = View.GONE
        primaryButton.visibility = View.VISIBLE
        primaryButton.text = "Re-check connection"
        primaryButton.isEnabled = true
        disconnectButton.visibility = View.VISIBLE
        disconnectButton.isEnabled = true
    }

    private fun renderPairingFailure(error: Throwable) {
        progress.visibility = View.GONE
        primaryButton.visibility = View.VISIBLE
        primaryButton.isEnabled = true
        disconnectButton.isEnabled = true
        val pairingError = error as? FounderRealtimePairingClient.PairingException
        when (pairingError?.kind) {
            FounderRealtimePairingClient.PairingException.Kind.ACCESS_CODE_REJECTED -> {
                statusPill.text = "NOT CONNECTED"
                statusTitle.text = "That access code wasn't accepted"
                statusBody.text = "Check the founder access code and try again. No alarm settings were changed."
                codeInput.visibility = View.VISIBLE
                disconnectButton.visibility = View.GONE
            }
            FounderRealtimePairingClient.PairingException.Kind.SERVER_NOT_READY -> renderUnavailable(
                "Conversational Alfred needs server setup",
                "The WakeMyWay backend is reachable, but its OpenAI Realtime configuration is not complete yet.",
            )
            else -> renderUnavailable(
                "Alfred couldn't connect",
                "The local alarm and local Alfred fallback are unaffected. Check the network and try again.",
            )
        }
    }

    private fun renderUnavailable(title: String, body: String) {
        progress.visibility = View.GONE
        statusPill.text = "NOT READY"
        statusTitle.text = title
        statusBody.text = body
        val configured = settings.configured()
        codeInput.visibility = if (configured) View.GONE else View.VISIBLE
        primaryButton.visibility = View.VISIBLE
        primaryButton.text = if (configured) "Check again" else "Connect Alfred"
        primaryButton.isEnabled = configured || serverReady
        disconnectButton.visibility = if (configured) View.VISIBLE else View.GONE
        disconnectButton.isEnabled = true
    }

    private fun showLoading(title: String, body: String) {
        statusPill.text = "CHECKING"
        statusTitle.text = title
        statusBody.text = body
        progress.visibility = View.VISIBLE
        primaryButton.isEnabled = false
        disconnectButton.isEnabled = false
    }

    private fun missingCopy(missing: List<String>): String =
        if (missing.isEmpty()) {
            "The WakeMyWay Realtime service is not ready yet."
        } else {
            "Server setup still needs: ${missing.joinToString(", ")}. Your local alarm remains unaffected."
        }

    private fun matchWidth(topMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { this.topMargin = dp(topMargin) }

    private fun wrapContent(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

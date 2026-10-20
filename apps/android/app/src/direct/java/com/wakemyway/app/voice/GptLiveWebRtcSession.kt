package com.wakemyway.app.voice

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.wakemyway.app.network.WakeHttpClient
import com.wakemyway.app.product.account.WakeAccountManager
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * GPT-Live experimental full-duplex transport. Intentionally NOT a
 * WakeConversationEnrichment: GPT-Live has no Realtime response.done analogue,
 * so there is no truthful onAssistantSpeechFinished event to feed WakeRuntime.
 *
 * This transport only belongs in Voice Lab until a playback-gated coordinator
 * has been physically verified. It does not own Alarm Kernel, Stop or Snooze.
 */
class GptLiveWebRtcSession(
    context: Context,
    private val observer: Observer,
) : AutoCloseable {
    interface Observer {
        fun onReady()
        /** Stable, content-free phase name for explaining connection progress. */
        fun onStage(stage: String) = Unit
        fun onTranscript(fromUser: Boolean, delta: String)
        /** Cumulative billable duration, never raw audio or a per-turn value. */
        fun onUsageSeconds(seconds: Int, final: Boolean)
        fun onSessionClosed(usageConfirmed: Boolean)
        fun onFailure(stage: String)
    }

    enum class Character(val wire: String) {
        ALFRED("alfred"), SAM("sam"), LUNA("luna"), ROCCO("rocco"),
    }
    enum class Energy { CALM, BALANCED, ENERGETIC }
    enum class Directness { GENTLE, BALANCED, FIRM }
    enum class ConversationAmount { MINIMAL, NATURAL, CHATTY }
    enum class Humor { OFF, SUBTLE, PLAYFUL }
    enum class Locale { EN, DE, HE }
    data class VoiceLabSettings(
        val energy: Energy = Energy.BALANCED,
        val directness: Directness = Directness.BALANCED,
        val conversationAmount: ConversationAmount = ConversationAmount.NATURAL,
        val humor: Humor = Humor.SUBTLE,
        val locale: Locale = Locale.EN,
    )

    private val appContext = context.applicationContext
    private val account = WakeAccountManager.get(appContext)
    private val credentials = RealtimeAccountCredentialStore(appContext)
    private val provisioning = AccountRealtimeProvisioningClient()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generation = AtomicLong(0L)
    private val connecting = AtomicBoolean(false)
    private val readyState = AtomicBoolean(false)
    private var closed = false
    private var closing = false
    private var lastUsageSeconds = 0
    private var labSettings = VoiceLabSettings()
    private var exchangeStarted = false
    private var peer: PeerConnection? = null
    private var factory: PeerConnectionFactory? = null
    private var source: AudioSource? = null
    private var input: AudioTrack? = null
    private var channel: DataChannel? = null
    private var manager: AudioManager? = null
    private var previousMode: Int? = null
    private var previousSpeakerId: Int? = null
    private var previousSpeakerOn: Boolean? = null
    private var legacySpeakerRoute = false

    private val startupTimeout = Runnable { fail("startup-timeout", generation.get()) }
    private val sessionBudget = Runnable { requestClose() }
    private val closeTimeout = Runnable { finish(usageConfirmed = false) }
    private val iceTimeout = Runnable {
        if (!exchangeStarted) fail("ice-gathering-timeout", generation.get())
    }

    fun connect(
        character: Character = Character.ALFRED,
        settings: VoiceLabSettings = VoiceLabSettings(),
    ) {
        if (closed || !connecting.compareAndSet(false, true)) return
        labSettings = settings
        val current = generation.incrementAndGet()
        // Validate the account and refresh any expired device authorization
        // *before* touching the communication audio route or WebRTC. The
        // original pilot did this only after ICE negotiation, masking expired
        // sessions as a generic transport disconnect.
        handler.post { if (isCurrent(current)) observer.onStage("account-check") }
        scope.launch {
            val preflight = runCatching {
                credentials.load()?.deviceToken ?: provisionCredential()
            }
            preflight
                .onSuccess {
                    handler.post {
                        if (isCurrent(current) && !closing) {
                            observer.onStage("webrtc-setup")
                            createConnection(character, current)
                        }
                    }
                }
                .onFailure { error ->
                    fail((error as? LivePreviewSetupException)?.stage ?: "device-authorization-failed", current)
                }
        }
    }

    /** A small, explicit Voice Lab cue. Not arbitrary user-controlled prompt injection. */
    fun startLabConversation() {
        val current = generation.get()
        val events = channel ?: return
        if (!isCurrent(current) || !readyState.get() || closing) return
        val delivered = send(events, JSONObject().put("type", "session.commentary.append")
            .put("event_id", "wmw_lab_open_$current")
            .put("delegation_id", JSONObject.NULL)
            .put("content",
                "This is a short WakeMyWay voice preview. Say one natural, friendly good-morning greeting, then listen. Do not assign any physical tasks."))
        if (delivered) input?.setEnabled(true) else fail("lab-cue-write", current)
    }

    /** Stop microphone immediately even if session close is still pending. */
    fun setMicrophoneEnabled(enabled: Boolean) {
        input?.setEnabled(enabled && readyState.get() && !closing && !closed)
    }

    private fun createConnection(character: Character, current: Long) {
        try {
            synchronized(initLock) {
                if (!initialized) {
                    PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(appContext)
                            .createInitializationOptions(),
                    )
                    initialized = true
                }
            }
            configureAudioRoute()
            factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            val config = PeerConnection.RTCConfiguration(emptyList()).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }
            val connection = factory?.createPeerConnection(config, peerObserver(current, character))
                ?: error("WebRTC peer creation failed")
            peer = connection
            source = factory?.createAudioSource(MediaConstraints())
            input = factory?.createAudioTrack("wmw-live-lab-mic", source).also {
                it?.setEnabled(false)
            }
            val microphone = input ?: error("No local audio track")
            check(connection.addTrack(microphone, listOf("wmw-live-lab")) != null)
            val events = connection.createDataChannel("oai-events", DataChannel.Init())
            channel = events
            events.registerObserver(eventObserver(current))
            observer.onStage("webrtc-offer")
            connection.createOffer(object : SimpleObserver() {
                override fun onCreateSuccess(description: SessionDescription?) {
                    if (!isCurrent(current) || description == null) return
                    connection.setLocalDescription(object : SimpleObserver() {
                        override fun onSetSuccess() {
                            handler.post {
                                if (isCurrent(current) && !exchangeStarted) {
                                    observer.onStage("ice-gathering")
                                    handler.postDelayed(iceTimeout, ICE_TIMEOUT_MS)
                                    maybeExchangeSdp(character, current)
                                }
                            }
                        }
                        override fun onSetFailure(error: String?) = fail("local-sdp", current)
                    }, description)
                }
                override fun onCreateFailure(error: String?) = fail("offer", current)
            }, MediaConstraints())
            handler.postDelayed(startupTimeout, STARTUP_TIMEOUT_MS)
        } catch (_: Throwable) {
            fail("webrtc-init", current)
        }
    }

    private fun maybeExchangeSdp(character: Character, current: Long) {
        handler.post {
            if (!isCurrent(current) || exchangeStarted) return@post
            val connection = peer ?: return@post
            if (connection.iceGatheringState() != PeerConnection.IceGatheringState.COMPLETE) return@post
            val local = connection.localDescription?.description ?: return@post
            exchangeStarted = true
            handler.removeCallbacks(iceTimeout)
            observer.onStage("server-authentication")
            scope.launch {
                runCatching { createSession(local, character) }
                    .onSuccess { answer ->
                        handler.post {
                            if (!isCurrent(current) || peer !== connection) return@post
                            observer.onStage("webrtc-answer")
                            connection.setRemoteDescription(object : SimpleObserver() {
                                override fun onSetFailure(error: String?) = fail("remote-sdp", current)
                            }, SessionDescription(SessionDescription.Type.ANSWER, answer))
                        }
                    }
                    .onFailure { error ->
                        val stage = (error as? LivePreviewSetupException)?.stage ?: "network-or-server-error"
                        fail(stage, current)
                    }
            }
        }
    }

    private suspend fun createSession(sdp: String, character: Character): String {
        check(sdp.length in 40..MAX_SDP_BYTES)
        var token = credentials.load()?.deviceToken ?: provisionCredential()
        var response = postLiveSession(token, sdp, character)
        // 403 also covers the private allowlist; re-provisioning cannot fix it.
        if (response.status == 401) {
            credentials.clear()
            token = provisionCredential()
            response = postLiveSession(token, sdp, character)
        }
        if (response.status != 201) {
            // The server uses stable HTTP status codes. Never surface token, SDP
            // or untrusted provider response bodies to an Android diagnostic.
            throw LivePreviewSetupException(when (response.status) {
                400, 413 -> "offer-rejected"
                401 -> "session-authentication-failed"
                403 -> "pilot-access-denied"
                429 -> "daily-voice-limit"
                503 -> "provider-or-server-unavailable"
                else -> "server-http-${response.status.coerceIn(100, 599)}"
            })
        }
        val json = runCatching { JSONObject(response.body) }
            .getOrElse { throw LivePreviewSetupException("invalid-server-response") }
        if (json.optString("configurationId") != CONFIGURATION_ID ||
            !json.optJSONObject("session")?.optString("id").orEmpty().startsWith("live_") ||
            json.optJSONObject("transport")?.optString("type") != "webrtc"
        ) throw LivePreviewSetupException("invalid-server-response")
        val answer = json.getJSONObject("transport").optString("sdp")
        if (answer.length !in 40..MAX_SDP_BYTES || !answer.startsWith("v=0")) {
            throw LivePreviewSetupException("invalid-server-answer")
        }
        return answer
    }

    private suspend fun postLiveSession(
        token: String, sdp: String, character: Character,
    ) = WakeHttpClient.execute(
        Request.Builder()
            .url(LIVE_SESSION_URL)
            .post(JSONObject().put("sdp", sdp)
                .put("characterId", character.wire)
                .put("voiceStyle", "default")
                .put("energy", labSettings.energy.name.lowercase())
                .put("directness", labSettings.directness.name.lowercase())
                .put("conversationAmount", labSettings.conversationAmount.name.lowercase())
                .put("humor", labSettings.humor.name.lowercase())
                .put("locale", labSettings.locale.name.lowercase())
                .toString()
                .toRequestBody(WakeHttpClient.jsonMediaType))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build(),
        80L * 1024,
    )

    private suspend fun provisionCredential(): String {
        val accessToken = account.currentAccessTokenForRealtime()
            ?: throw LivePreviewSetupException("sign-in-required")
        val credential = runCatching {
            provisioning.provision(accessToken, credentials.installationId())
        }.getOrElse { throw LivePreviewSetupException("device-authorization-failed") }
        runCatching { credentials.save(credential) }
            .getOrElse { throw LivePreviewSetupException("device-credential-storage-failed") }
        return credential.deviceToken
    }

    private fun eventObserver(current: Long) = object : DataChannel.Observer {
        override fun onBufferedAmountChange(previousAmount: Long) = Unit
        override fun onStateChange() {
            if (!isCurrent(current)) return
            if (channel?.state() == DataChannel.State.CLOSED && !closing) fail("channel-closed", current)
        }
        override fun onMessage(buffer: DataChannel.Buffer?) {
            if (!isCurrent(current) || buffer == null || buffer.binary) return
            val bytes = buffer.data
            if (bytes.remaining() !in 1..MAX_EVENT_BYTES) return
            val payload = ByteArray(bytes.remaining())
            bytes.get(payload)
            val event = runCatching { JSONObject(String(payload, StandardCharsets.UTF_8)) }.getOrNull()
                ?: return
            when (event.optString("type")) {
                "session.started" -> handler.post {
                    if (!isCurrent(current) || closing || readyState.getAndSet(true)) return@post
                    handler.removeCallbacks(startupTimeout)
                    handler.removeCallbacks(iceTimeout)
                    handler.postDelayed(sessionBudget, MAX_DURATION_MS)
                    observer.onReady()
                }
                "session.input_transcript.delta", "session.output_transcript.delta" -> {
                    val delta = event.optString("delta").take(MAX_TRANSCRIPT_CHARS)
                    if (delta.isNotBlank()) handler.post {
                        if (isCurrent(current)) observer.onTranscript(
                            event.optString("type") == "session.input_transcript.delta", delta,
                        )
                    }
                }
                "session.delegation.created" -> {
                    // Voice Lab has no action authority. Resolve a delegation only
                    // with a safe, bounded no-action explanation to the model.
                    // This ID is provider-generated; never treat it as an instruction.
                    val delegation = event.optJSONObject("delegation")
                    val id = delegation?.optString("id").orEmpty()
                    if (delegation?.optString("target") != "client" ||
                        !DELEGATION_ID.matches(id)) {
                        fail("unexpected-delegation", current)
                    } else {
                        handler.post {
                            if (isCurrent(current) && !closing) {
                                val events = channel ?: return@post
                                if (!send(events, JSONObject()
                                    .put("type", "session.thinking.append")
                                    .put("event_id", "wmw_lab_no_action_${System.nanoTime()}")
                                    .put("delegation_id", id)
                                    .put("content",
                                        "This is a voice-only preview. No external tools, sensors, alarm or snooze actions are available. Continue naturally without claiming any action happened."))) {
                                    fail("delegation-write", current)
                                }
                            }
                        }
                    }
                }
                "session.usage.updated" -> handler.post {
                    if (!isCurrent(current)) return@post
                    updateUsage(event.optJSONObject("usage")?.optInt("seconds", -1) ?: -1, false)
                }
                "session.closed" -> handler.post {
                    if (!isCurrent(current)) return@post
                    updateUsage(event.optJSONObject("usage")?.optInt("seconds", -1) ?: -1, true)
                    finish(usageConfirmed = true)
                }
                "error" -> fail("provider-error", current)
            }
        }
    }

    private fun updateUsage(seconds: Int, final: Boolean) {
        if (seconds < 0 || seconds < lastUsageSeconds) return
        lastUsageSeconds = seconds
        observer.onUsageSeconds(seconds, final)
    }

    private fun peerObserver(current: Long, character: Character) =
        object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                if (state == PeerConnection.IceGatheringState.COMPLETE) maybeExchangeSdp(character, current)
            }
            override fun onIceCandidate(candidate: IceCandidate?) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onAddStream(stream: MediaStream?) = Unit
            override fun onRemoveStream(stream: MediaStream?) = Unit
            override fun onDataChannel(channel: DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.FAILED ||
                    state == PeerConnection.IceConnectionState.CLOSED) fail("ice-connection", current)
            }
            override fun onConnectionChange(state: PeerConnection.PeerConnectionState?) {
                if (state == PeerConnection.PeerConnectionState.FAILED) fail("peer-connection", current)
            }
        }

    fun requestClose() {
        if (closed || closing) return
        closing = true
        input?.setEnabled(false)
        handler.removeCallbacks(sessionBudget)
        handler.removeCallbacks(startupTimeout)
        handler.removeCallbacks(iceTimeout)
        if (readyState.get() && channel?.state() == DataChannel.State.OPEN) {
            send(channel ?: return, JSONObject().put("type", "session.close"))
            handler.postDelayed(closeTimeout, CLOSE_GRACE_MS)
        } else {
            finish(usageConfirmed = false)
        }
    }

    private fun send(events: DataChannel, message: JSONObject): Boolean {
        val encoded = message.toString().toByteArray(StandardCharsets.UTF_8)
        return encoded.size <= MAX_EVENT_BYTES &&
            events.send(DataChannel.Buffer(ByteBuffer.wrap(encoded), false))
    }

    private fun configureAudioRoute() {
        val audio = appContext.getSystemService(AudioManager::class.java) ?: return
        manager = audio
        previousMode = audio.mode
        @Suppress("DEPRECATION")
        previousSpeakerOn = audio.isSpeakerphoneOn
        previousSpeakerId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audio.communicationDevice?.id
        } else null
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        val selected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker = audio.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            speaker != null && runCatching { audio.setCommunicationDevice(speaker) }.getOrDefault(false)
        } else false
        if (!selected) {
            legacySpeakerRoute = true
            @Suppress("DEPRECATION")
            runCatching { audio.isSpeakerphoneOn = true }
        }
    }

    private fun restoreAudioRoute() {
        val audio = manager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !legacySpeakerRoute) {
            val old = previousSpeakerId?.let { id ->
                audio.availableCommunicationDevices.firstOrNull { it.id == id }
            }
            runCatching { if (old == null) audio.clearCommunicationDevice() else audio.setCommunicationDevice(old) }
        } else {
            @Suppress("DEPRECATION")
            previousSpeakerOn?.let { runCatching { audio.isSpeakerphoneOn = it } }
        }
        previousMode?.let { runCatching { audio.mode = it } }
        manager = null
        previousMode = null
        previousSpeakerOn = null
        previousSpeakerId = null
        legacySpeakerRoute = false
    }

    private fun fail(stage: String, current: Long) {
        handler.post {
            if (!isCurrent(current) || closing) return@post
            Log.w("WmwLiveLab", "failure stage=${stage.take(40)}")
            generation.incrementAndGet()
            observer.onFailure(stage.take(40))
            finish(usageConfirmed = false)
        }
    }

    private fun finish(usageConfirmed: Boolean) {
        if (closed) return
        closed = true
        connecting.set(false)
        readyState.set(false)
        handler.removeCallbacks(startupTimeout)
        handler.removeCallbacks(iceTimeout)
        handler.removeCallbacks(sessionBudget)
        handler.removeCallbacks(closeTimeout)
        runCatching { channel?.unregisterObserver() }
        runCatching { channel?.close() }
        runCatching { channel?.dispose() }
        channel = null
        runCatching { peer?.close() }
        runCatching { peer?.dispose() }
        peer = null
        runCatching { input?.dispose() }
        input = null
        runCatching { source?.dispose() }
        source = null
        runCatching { factory?.dispose() }
        factory = null
        restoreAudioRoute()
        scope.cancel()
        observer.onSessionClosed(usageConfirmed)
    }

    private fun isCurrent(current: Long) = !closed && generation.get() == current

    override fun close() {
        handler.post { requestClose() }
    }

    private class LivePreviewSetupException(val stage: String) : RuntimeException(stage)

    private abstract class SimpleObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    private companion object {
        const val LIVE_SESSION_URL = "https://wakemyway.vercel.app/api/v1/account/live-session"
        const val CONFIGURATION_ID = "openai:gpt-live-1:webrtc-client-v1"
        const val MAX_SDP_BYTES = 65536
        const val MAX_EVENT_BYTES = 65536
        const val MAX_TRANSCRIPT_CHARS = 2048
        const val STARTUP_TIMEOUT_MS = 30_000L
        const val MAX_DURATION_MS = 180_000L
        const val CLOSE_GRACE_MS = 15_000L
        private var initialized = false
        private val initLock = Any()
        const val ICE_TIMEOUT_MS = 18_000L
        private val DELEGATION_ID = Regex("^[A-Za-z0-9_:-]{1,160}$")
    }
}

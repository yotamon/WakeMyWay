package com.wakemyway.app.voice

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.wakemyway.core.runtime.SpeechIntent
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.json.JSONArray
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

/** Debug-only conversational enrichment. Alarm Kernel and Wake Runtime remain authoritative. */
class DebugRealtimeWakeConversation(
    context: Context,
    private val listener: WakeConversationEnrichment.Listener,
) : WakeConversationEnrichment {
    private data class BrokerSecret(val token: String, val callsUrl: String, val voice: String)

    private val appContext = context.applicationContext
    private val settings = FounderRealtimeSettings(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val networkExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "wmw-founder-realtime").apply { isDaemon = true }
    }
    private val generation = AtomicLong(0L)
    private val readyState = AtomicBoolean(false)

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var dataChannel: DataChannel? = null
    private var audioManager: AudioManager? = null
    private var previousAudioMode: Int? = null
    private var previousSpeakerphone: Boolean? = null
    private var inputEnabled = false
    private var responseAudible = false
    private var speechStartedAtMs: Long? = null

    @Volatile private var closed = false

    override val ready: Boolean get() = !closed && readyState.get()

    override fun connect() {
        if (closed || ready) return
        val config = settings.load() ?: run {
            emitFailure("not-configured")
            return
        }
        if (runCatching { validateBrokerUrl(config.brokerUrl) }.isFailure) {
            emitFailure("configuration")
            return
        }

        disconnectResources()
        val current = generation.incrementAndGet()
        networkExecutor.execute {
            runCatching { requestBrokerSecret(config) }
                .onSuccess { secret ->
                    mainHandler.post {
                        if (isCurrent(current)) startPeerConnection(secret, current)
                    }
                }
                .onFailure { emitFailure("credential", current) }
        }
    }

    override fun respond(intent: SpeechIntent): Boolean {
        if (!ready) return false
        val channel = dataChannel ?: return false
        if (channel.state() != DataChannel.State.OPEN) return false
        return sendEvent(
            channel,
            JSONObject().put("type", "response.create").put(
                "response",
                JSONObject()
                    .put("conversation", "auto")
                    .put("output_modalities", JSONArray().put("audio"))
                    .put("instructions", AlfredRealtimePrompt.turn(intent)),
            ),
        )
    }

    override fun setInputEnabled(enabled: Boolean) {
        inputEnabled = enabled
        audioTrack?.setEnabled(enabled)
    }

    override fun close() {
        if (closed) return
        closed = true
        generation.incrementAndGet()
        disconnectResources()
        networkExecutor.shutdownNow()
    }

    private fun startPeerConnection(secret: BrokerSecret, current: Long) {
        if (!isCurrent(current)) return
        try {
            initializeWebRtcOnce(appContext)
            configureWakeAudioRoute()
            val factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            peerConnectionFactory = factory
            val configuration = PeerConnection.RTCConfiguration(emptyList()).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }
            val peer = factory.createPeerConnection(configuration, peerObserver(current))
                ?: error("WebRTC peer connection creation failed")
            peerConnection = peer
            val source = factory.createAudioSource(MediaConstraints())
            audioSource = source
            val microphone = factory.createAudioTrack("wmw-founder-wake-mic", source).apply {
                setEnabled(inputEnabled)
            }
            audioTrack = microphone
            check(peer.addTrack(microphone, listOf("wmw-founder-wake")) != null)
            val channel = peer.createDataChannel("oai-events", DataChannel.Init())
            dataChannel = channel
            channel.registerObserver(dataChannelObserver(channel, secret, current))
            peer.createOffer(object : SimpleSdpObserver() {
                override fun onCreateSuccess(description: SessionDescription?) {
                    if (!isCurrent(current)) return
                    val offer = description ?: run {
                        emitFailure("offer", current)
                        return
                    }
                    peer.setLocalDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() = exchangeSdpAsync(peer, offer, secret, current)
                        override fun onSetFailure(error: String?) = emitFailure("local-sdp", current)
                    }, offer)
                }

                override fun onCreateFailure(error: String?) = emitFailure("offer", current)
            }, MediaConstraints())
        } catch (_: Throwable) {
            emitFailure("webrtc-init", current)
        }
    }

    private fun exchangeSdpAsync(
        peer: PeerConnection,
        offer: SessionDescription,
        secret: BrokerSecret,
        current: Long,
    ) {
        networkExecutor.execute {
            runCatching { exchangeSdp(secret, offer.description) }
                .onSuccess { answer ->
                    mainHandler.post {
                        if (!isCurrent(current) || peerConnection !== peer) return@post
                        peer.setRemoteDescription(object : SimpleSdpObserver() {
                            override fun onSetFailure(error: String?) = emitFailure("remote-sdp", current)
                        }, SessionDescription(SessionDescription.Type.ANSWER, answer))
                    }
                }
                .onFailure { emitFailure("sdp-exchange", current) }
        }
    }

    private fun dataChannelObserver(
        channel: DataChannel,
        secret: BrokerSecret,
        current: Long,
    ) = object : DataChannel.Observer {
        override fun onBufferedAmountChange(previousAmount: Long) = Unit

        override fun onStateChange() {
            if (!isCurrent(current) || channel.state() != DataChannel.State.OPEN) return
            if (!sendSessionConfiguration(channel, secret.voice)) {
                emitFailure("session-config", current)
                return
            }
            readyState.set(true)
            mainHandler.post {
                if (isCurrent(current)) listener.onConversationReady()
            }
        }

        override fun onMessage(buffer: DataChannel.Buffer?) {
            if (!isCurrent(current) || buffer == null || buffer.binary) return
            val data = buffer.data
            if (data.remaining() <= 0 || data.remaining() > MAX_EVENT_BYTES) return
            val bytes = ByteArray(data.remaining())
            data.get(bytes)
            val event = runCatching {
                JSONObject(String(bytes, StandardCharsets.UTF_8))
            }.getOrNull() ?: return
            handleServerEvent(event, current)
        }
    }

    private fun handleServerEvent(event: JSONObject, current: Long) {
        when (event.optString("type").takeIf(EVENT_TYPE_PATTERN::matches) ?: return) {
            "input_audio_buffer.speech_started" -> {
                if (!inputEnabled) return
                speechStartedAtMs = event.optLong("audio_start_ms").takeIf { it >= 0L }
                mainHandler.post {
                    if (isCurrent(current)) listener.onUserSpeechStarted()
                }
            }

            "input_audio_buffer.speech_stopped" -> {
                if (!inputEnabled) return
                val start = speechStartedAtMs
                val end = event.optLong("audio_end_ms").takeIf { it >= 0L }
                speechStartedAtMs = null
                val duration = if (start != null && end != null) end - start else MIN_USER_TURN_MS
                if (duration >= MIN_USER_TURN_MS) {
                    mainHandler.post {
                        if (isCurrent(current)) listener.onUserTurnObserved()
                    }
                }
            }

            "output_audio_buffer.started" -> if (!responseAudible) {
                responseAudible = true
                mainHandler.post {
                    if (isCurrent(current)) listener.onAssistantSpeechStarted()
                }
            }

            "output_audio_buffer.stopped" -> finishAssistantAudio(current, false)
            "output_audio_buffer.cleared" -> finishAssistantAudio(current, true)
            "response.done" -> {
                val status = event.optJSONObject("response")?.optString("status")
                if (status == "failed" || status == "incomplete") {
                    emitFailure("response", current)
                }
            }

            "error" -> emitFailure("provider", current)
        }
    }

    private fun finishAssistantAudio(current: Long, interrupted: Boolean) {
        if (!responseAudible) return
        responseAudible = false
        mainHandler.post {
            if (isCurrent(current)) listener.onAssistantSpeechFinished(interrupted)
        }
    }

    private fun sendSessionConfiguration(channel: DataChannel, voice: String): Boolean {
        val turnDetection = JSONObject()
            .put("type", "server_vad")
            .put("threshold", 0.58)
            .put("prefix_padding_ms", 250)
            .put("silence_duration_ms", 650)
            .put("create_response", false)
            .put("interrupt_response", true)
        val session = JSONObject()
            .put("type", "realtime")
            .put("instructions", AlfredRealtimePrompt.SYSTEM)
            .put("output_modalities", JSONArray().put("audio"))
            .put(
                "audio",
                JSONObject()
                    .put("input", JSONObject().put("turn_detection", turnDetection))
                    .put("output", JSONObject().put("voice", voice).put("speed", 1.04)),
            )
        return sendEvent(
            channel,
            JSONObject().put("type", "session.update").put("session", session),
        )
    }

    private fun sendEvent(channel: DataChannel, event: JSONObject): Boolean {
        val bytes = event.toString().toByteArray(StandardCharsets.UTF_8)
        return bytes.size <= MAX_CLIENT_EVENT_BYTES &&
            channel.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false))
    }

    private fun peerObserver(current: Long) = object : PeerConnection.Observer {
        override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) = Unit
        override fun onIceCandidate(candidate: IceCandidate?) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
        override fun onAddStream(stream: MediaStream?) = Unit
        override fun onRemoveStream(stream: MediaStream?) = Unit
        override fun onDataChannel(channel: DataChannel?) = Unit
        override fun onRenegotiationNeeded() = Unit

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            if (isCurrent(current) && newState == PeerConnection.IceConnectionState.FAILED) {
                emitFailure("ice", current)
            }
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            if (isCurrent(current) && newState == PeerConnection.PeerConnectionState.FAILED) {
                emitFailure("peer", current)
            }
        }
    }

    private fun requestBrokerSecret(config: FounderRealtimeConfig): BrokerSecret {
        val connection = (URL(config.brokerUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            useCaches = false
            doInput = true
            setRequestProperty("Authorization", "Bearer ${config.operatorToken}")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Length", "0")
        }
        try {
            if (connection.responseCode !in 200..299) error("Broker request failed")
            val json = JSONObject(readBounded(connection.inputStream, MAX_BROKER_RESPONSE_BYTES))
            check(json.optString("candidate") == "direct-openai")
            check(json.optString("connectionMode") == "webrtc-ephemeral")
            check(json.optString("configurationId") == EXPECTED_CONFIGURATION_ID)
            check(json.optString("privacyEligibility") == EXPECTED_PRIVACY_CLASSIFICATION)
            val token = json.getString("token").trim()
            val callsUrl = json.getString("realtimeCallsUrl").trim()
            val voice = json.getString("voice").trim()
            check(
                token.isNotBlank() &&
                    voice.isNotBlank() &&
                    callsUrl == OPENAI_REALTIME_CALLS_URL,
            )
            return BrokerSecret(token, callsUrl, voice)
        } finally {
            connection.disconnect()
        }
    }

    private fun exchangeSdp(secret: BrokerSecret, offerSdp: String): String {
        check(offerSdp.isNotBlank() && offerSdp.length <= MAX_SDP_BYTES)
        val connection = (URL(secret.callsUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            useCaches = false
            doInput = true
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${secret.token}")
            setRequestProperty("Content-Type", "application/sdp")
            setRequestProperty("Accept", "application/sdp")
        }
        try {
            connection.outputStream.use {
                it.write(offerSdp.toByteArray(StandardCharsets.UTF_8))
            }
            if (connection.responseCode !in 200..299) error("Realtime SDP exchange failed")
            return readBounded(connection.inputStream, MAX_SDP_BYTES).also {
                check(it.isNotBlank())
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun configureWakeAudioRoute() {
        val manager = appContext.getSystemService(AudioManager::class.java) ?: return
        audioManager = manager
        previousAudioMode = manager.mode
        @Suppress("DEPRECATION")
        previousSpeakerphone = manager.isSpeakerphoneOn
        manager.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        runCatching { manager.isSpeakerphoneOn = true }
    }

    private fun restoreAudioRoute() {
        val manager = audioManager ?: return
        previousAudioMode?.let { runCatching { manager.mode = it } }
        previousSpeakerphone?.let { enabled ->
            @Suppress("DEPRECATION")
            runCatching { manager.isSpeakerphoneOn = enabled }
        }
        audioManager = null
        previousAudioMode = null
        previousSpeakerphone = null
    }

    private fun disconnectResources() {
        readyState.set(false)
        inputEnabled = false
        responseAudible = false
        speechStartedAtMs = null
        runCatching { dataChannel?.unregisterObserver() }
        runCatching { dataChannel?.close() }
        runCatching { dataChannel?.dispose() }
        dataChannel = null
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        peerConnection = null
        runCatching { audioTrack?.dispose() }
        audioTrack = null
        runCatching { audioSource?.dispose() }
        audioSource = null
        runCatching { peerConnectionFactory?.dispose() }
        peerConnectionFactory = null
        restoreAudioRoute()
    }

    private fun validateBrokerUrl(raw: String) {
        val uri = Uri.parse(raw)
        require(
            uri.scheme.equals("https", true) &&
                !uri.host.isNullOrBlank() &&
                uri.path.orEmpty().endsWith(FounderRealtimeSettings.FOUNDER_WAKE_PATH),
        )
    }

    private fun readBounded(stream: InputStream, maxBytes: Int): String = stream.use { input ->
        val output = ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            total += count
            check(total <= maxBytes)
            output.write(buffer, 0, count)
        }
        String(output.toByteArray(), StandardCharsets.UTF_8)
    }

    /**
     * A failure belongs to the connection generation that observed it. Invalidating that generation
     * before resource teardown prevents late callbacks from an old peer/request from failing a newer
     * reconnect. Resource teardown also restores the pre-Realtime audio route before local fallback.
     */
    private fun emitFailure(stage: String, expectedGeneration: Long = generation.get()) {
        mainHandler.post {
            if (!isCurrent(expectedGeneration)) return@post
            generation.incrementAndGet()
            disconnectResources()
            if (!closed) listener.onConversationFailure(stage.take(48))
        }
    }

    private fun isCurrent(current: Long) = !closed && generation.get() == current

    private abstract class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    private object AlfredRealtimePrompt {
        const val SYSTEM = """You are Alfred, Wake My Way's calm British morning wake companion. Your only job is helping a sleepy person move from sleep inertia into being physically upright and engaged. Sound warm, intelligent, dryly witty, concise and human. Speak in one or two short sentences. React naturally to what the user just said. Never shame, threaten, diagnose, make medical claims, or pretend to know sensor/context facts you were not given. Never claim the alarm stopped, wake completed, or snooze succeeded. If the user bargains, complains or jokes, engage naturally but keep steering toward one small wake action. Yield immediately if interrupted. Never ask 'How can I help?'."""

        fun turn(intent: SpeechIntent): String = SYSTEM + "\nCurrent Wake Runtime directive: " + when (intent) {
            SpeechIntent.InitialWake -> "Open naturally with a brief greeting."
            SpeechIntent.AskToSitUp -> "Ask them to sit upright and answer out loud when they are sitting."
            SpeechIntent.AskToMove -> "React to their reply, then ask for feet on the floor or one similarly safe small movement and ask them to tell you when done."
            SpeechIntent.KeepEngaging -> "React genuinely to their latest reply. Continue the thread, request one safe tiny wake action, and end with a natural prompt so they answer again. Avoid repeating wording."
            is SpeechIntent.ReEngage -> "They did not give usable engagement. Re-engage at firmness ${intent.escalationLevel} of 3, respectfully requesting one spoken reply and one small physical action."
            SpeechIntent.SnoozeConfirmation -> "Briefly ask them to confirm snooze. Never say it succeeded."
            SpeechIntent.SnoozeFailed -> "Say snooze did not schedule and gently continue the wake."
            SpeechIntent.Orientation -> "Wake Runtime has enough evidence. Give one brief satisfying closing line without claiming biological wakefulness."
        }
    }

    private companion object {
        const val NETWORK_TIMEOUT_MS = 12_000
        const val MAX_BROKER_RESPONSE_BYTES = 32 * 1024
        const val MAX_SDP_BYTES = 512 * 1024
        const val MAX_EVENT_BYTES = 64 * 1024
        const val MAX_CLIENT_EVENT_BYTES = 16 * 1024
        const val MIN_USER_TURN_MS = 320L
        const val OPENAI_REALTIME_CALLS_URL = "https://api.openai.com/v1/realtime/calls"
        const val EXPECTED_CONFIGURATION_ID = "direct-openai:webrtc-founder-wake-v1"
        const val EXPECTED_PRIVACY_CLASSIFICATION = "founder-consented-default-api-retention"
        val EVENT_TYPE_PATTERN = Regex("^[A-Za-z0-9._:-]{1,128}$")

        @Volatile
        var webRtcInitialized = false
        val webRtcInitializationLock = Any()

        fun initializeWebRtcOnce(context: Context) {
            if (webRtcInitialized) return
            synchronized(webRtcInitializationLock) {
                if (webRtcInitialized) return
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions
                        .builder(context.applicationContext)
                        .createInitializationOptions(),
                )
                webRtcInitialized = true
            }
        }
    }
}

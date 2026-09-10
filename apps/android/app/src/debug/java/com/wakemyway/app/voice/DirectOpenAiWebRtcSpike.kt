package com.wakemyway.app.voice

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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

/**
 * Founder-only M8 transport spike. This class exists only in the debug source set.
 *
 * It intentionally does not know about Alarm Kernel, WakeRuntime, WakePolicy, wake
 * outcomes, Tomorrow Contract, or production character state. It also never stores
 * microphone audio, generated audio, transcripts, provider event payloads, or secrets.
 */
internal class DirectOpenAiWebRtcSpike(
    context: Context,
    private val listener: Listener,
) : AutoCloseable {
    interface Listener {
        fun onStatus(status: String)
        fun onConnected(coldConnectionMs: Long)
        fun onServerEventType(type: String)
        fun onFailure(stage: String, message: String)
    }

    private data class BrokerSecret(
        val token: String,
        val callsUrl: String,
        val configurationId: String,
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val networkExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "wmw-m8-direct-openai-network").apply { isDaemon = true }
    }
    private val connectedReported = AtomicBoolean(false)
    private val connectionGeneration = AtomicLong(0L)

    private var connectionStartedAtMs: Long = 0L
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var dataChannel: DataChannel? = null

    @Volatile
    private var closed = false

    fun connect(brokerUrl: String, internalBearerToken: String) {
        check(!closed) { "Voice spike client is closed" }
        require(internalBearerToken.isNotBlank()) { "Internal spike token is required" }
        validateBrokerUrl(brokerUrl)

        // Invalidate any credential/SDP callbacks still in flight from an earlier attempt.
        disconnect()
        val generation = connectionGeneration.incrementAndGet()

        connectionStartedAtMs = SystemClock.elapsedRealtime()
        connectedReported.set(false)
        emitStatus("Minting short-lived Realtime credential…")

        networkExecutor.execute {
            runCatching { requestBrokerSecret(brokerUrl, internalBearerToken) }
                .onSuccess { secret ->
                    mainHandler.post {
                        if (isCurrent(generation)) startPeerConnection(secret, generation)
                    }
                }
                .onFailure { error ->
                    if (isCurrent(generation)) emitFailure("credential", safeMessage(error))
                }
        }
    }

    /**
     * Sends one fixed, non-sensitive out-of-band audio response request and returns
     * the local monotonic enqueue timestamp. The caller may use that timestamp as
     * the origin for an operator-observed first-audible upper-bound measurement.
     */
    fun requestSyntheticMeasurementResponse(): Long {
        check(!closed) { "Voice spike client is closed" }
        val channel = checkNotNull(dataChannel) { "Realtime data channel is not available" }
        check(channel.state() == DataChannel.State.OPEN) { "Realtime data channel is not open" }

        val event = JSONObject()
            .put("type", "response.create")
            .put(
                "response",
                JSONObject()
                    .put("conversation", "none")
                    .put("output_modalities", JSONArray().put("audio"))
                    .put("input", JSONArray())
                    .put("instructions", SYNTHETIC_FIRST_SPEECH_INSTRUCTIONS),
            )
        val bytes = event.toString().toByteArray(StandardCharsets.UTF_8)
        check(bytes.size <= MAX_CLIENT_EVENT_BYTES) { "Synthetic measurement event exceeds the limit" }

        val requestedAtMs = SystemClock.elapsedRealtime()
        check(channel.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false))) {
            "Realtime data channel rejected the synthetic measurement request"
        }
        return requestedAtMs
    }

    fun disconnect() {
        connectionGeneration.incrementAndGet()
        disconnectResources()
    }

    override fun close() {
        if (closed) return
        closed = true
        connectionGeneration.incrementAndGet()
        disconnectResources()
        networkExecutor.shutdownNow()
    }

    private fun disconnectResources() {
        connectedReported.set(false)
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
    }

    private fun startPeerConnection(secret: BrokerSecret, generation: Long) {
        if (!isCurrent(generation)) return
        try {
            initializeWebRtcOnce(appContext)
            emitStatus("Creating WebRTC peer connection…")

            val factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            if (!isCurrent(generation)) {
                factory.dispose()
                return
            }
            peerConnectionFactory = factory

            val rtcConfiguration = PeerConnection.RTCConfiguration(emptyList())
            rtcConfiguration.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            rtcConfiguration.continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY

            val peer = factory.createPeerConnection(rtcConfiguration, peerObserver(generation))
                ?: error("WebRTC could not create a peer connection")
            if (!isCurrent(generation)) {
                peer.close()
                peer.dispose()
                return
            }
            peerConnection = peer

            val source = factory.createAudioSource(MediaConstraints())
            audioSource = source
            val localAudioTrack = factory.createAudioTrack("wmw-m8-mic", source)
            audioTrack = localAudioTrack
            check(peer.addTrack(localAudioTrack, listOf("wmw-m8-stream")) != null) {
                "WebRTC could not attach the microphone track"
            }

            val channel = peer.createDataChannel("oai-events", DataChannel.Init())
            dataChannel = channel
            channel.registerObserver(dataChannelObserver(channel, generation))

            peer.createOffer(
                object : SimpleSdpObserver() {
                    override fun onCreateSuccess(description: SessionDescription?) {
                        if (!isCurrent(generation)) return
                        if (description == null) {
                            emitFailure("offer", "WebRTC returned an empty SDP offer")
                            return
                        }
                        setLocalDescriptionAndExchange(peer, description, secret, generation)
                    }

                    override fun onCreateFailure(error: String?) {
                        if (isCurrent(generation)) {
                            emitFailure("offer", error?.take(160) ?: "SDP offer creation failed")
                        }
                    }
                },
                MediaConstraints(),
            )
        } catch (error: Throwable) {
            if (isCurrent(generation)) {
                // This method is entered on the main thread. Report synchronously before
                // invalidating the generation so the legitimate failure is not suppressed.
                listener.onFailure("webrtc-init", safeMessage(error))
                disconnect()
            }
        }
    }

    private fun setLocalDescriptionAndExchange(
        peer: PeerConnection,
        offer: SessionDescription,
        secret: BrokerSecret,
        generation: Long,
    ) {
        peer.setLocalDescription(
            object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    if (!isCurrent(generation)) return
                    emitStatus("Exchanging SDP directly with OpenAI…")
                    networkExecutor.execute {
                        runCatching { exchangeSdp(secret, offer.description) }
                            .onSuccess { answerSdp ->
                                mainHandler.post {
                                    if (!isCurrent(generation) || peerConnection !== peer) return@post
                                    setRemoteAnswer(peer, answerSdp, generation)
                                }
                            }
                            .onFailure { error ->
                                if (isCurrent(generation)) {
                                    emitFailure("sdp-exchange", safeMessage(error))
                                }
                            }
                    }
                }

                override fun onSetFailure(error: String?) {
                    if (isCurrent(generation)) {
                        emitFailure("local-sdp", error?.take(160) ?: "Setting local SDP failed")
                    }
                }
            },
            offer,
        )
    }

    private fun setRemoteAnswer(peer: PeerConnection, answerSdp: String, generation: Long) {
        if (!isCurrent(generation)) return
        peer.setRemoteDescription(
            object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    if (isCurrent(generation)) {
                        emitStatus("Remote SDP accepted; waiting for Realtime data channel…")
                    }
                }

                override fun onSetFailure(error: String?) {
                    if (isCurrent(generation)) {
                        emitFailure("remote-sdp", error?.take(160) ?: "Setting remote SDP failed")
                    }
                }
            },
            SessionDescription(SessionDescription.Type.ANSWER, answerSdp),
        )
    }

    private fun peerObserver(generation: Long): PeerConnection.Observer = object : PeerConnection.Observer {
        override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            if (!isCurrent(generation)) return
            when (newState) {
                PeerConnection.IceConnectionState.FAILED ->
                    emitFailure("ice", "ICE connection failed")
                PeerConnection.IceConnectionState.DISCONNECTED ->
                    emitStatus("ICE disconnected")
                else -> Unit
            }
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) = Unit
        override fun onIceCandidate(candidate: IceCandidate?) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
        override fun onAddStream(stream: MediaStream?) = Unit
        override fun onRemoveStream(stream: MediaStream?) = Unit
        override fun onDataChannel(channel: DataChannel?) = Unit
        override fun onRenegotiationNeeded() = Unit

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            if (!isCurrent(generation)) return
            when (newState) {
                PeerConnection.PeerConnectionState.CONNECTING -> emitStatus("WebRTC connecting…")
                PeerConnection.PeerConnectionState.CONNECTED -> emitStatus("WebRTC media connected")
                PeerConnection.PeerConnectionState.DISCONNECTED -> emitStatus("WebRTC disconnected")
                PeerConnection.PeerConnectionState.FAILED ->
                    emitFailure("peer-connection", "WebRTC peer connection failed")
                else -> Unit
            }
        }
    }

    private fun dataChannelObserver(
        channel: DataChannel,
        generation: Long,
    ): DataChannel.Observer = object : DataChannel.Observer {
        override fun onBufferedAmountChange(previousAmount: Long) = Unit

        override fun onStateChange() {
            if (!isCurrent(generation)) return
            when (channel.state()) {
                DataChannel.State.OPEN -> {
                    val elapsed = SystemClock.elapsedRealtime() - connectionStartedAtMs
                    if (connectedReported.compareAndSet(false, true)) {
                        mainHandler.post {
                            if (isCurrent(generation)) listener.onConnected(elapsed)
                        }
                    }
                    emitStatus("Realtime data channel open")
                }
                DataChannel.State.CLOSED -> emitStatus("Realtime data channel closed")
                else -> Unit
            }
        }

        override fun onMessage(buffer: DataChannel.Buffer?) {
            if (!isCurrent(generation) || buffer == null || buffer.binary) return
            val data = buffer.data
            if (data.remaining() <= 0 || data.remaining() > MAX_EVENT_BYTES) return
            val bytes = ByteArray(data.remaining())
            data.get(bytes)

            // Parse only the bounded event type and immediately discard the payload.
            // Transcripts/audio deltas/private model content are never retained or logged.
            val type = runCatching {
                JSONObject(String(bytes, StandardCharsets.UTF_8)).optString("type")
            }.getOrNull()?.takeIf { EVENT_TYPE_PATTERN.matches(it) }
            if (type != null) {
                mainHandler.post {
                    if (isCurrent(generation)) listener.onServerEventType(type)
                }
            }
        }
    }

    private fun requestBrokerSecret(brokerUrl: String, internalBearerToken: String): BrokerSecret {
        val connection = (URL(brokerUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            useCaches = false
            doInput = true
            setRequestProperty("Authorization", "Bearer $internalBearerToken")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Length", "0")
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) error("Credential broker returned HTTP $status")
            val body = readBounded(connection.inputStream, MAX_BROKER_RESPONSE_BYTES)
            val json = JSONObject(body)

            check(json.optString("candidate") == "direct-openai") {
                "Credential broker returned the wrong candidate"
            }
            check(json.optString("connectionMode") == "webrtc-ephemeral") {
                "Credential broker returned an unsupported connection mode"
            }
            check(json.optString("privacyEligibility") == "synthetic-only") {
                "Direct OpenAI spike privacy classification changed unexpectedly"
            }

            val token = json.getString("token").trim()
            val callsUrl = json.getString("realtimeCallsUrl").trim()
            val configurationId = json.getString("configurationId").trim()
            check(token.isNotEmpty()) { "Credential broker returned an empty token" }
            check(configurationId == EXPECTED_CONFIGURATION_ID) {
                "Credential broker returned an unexpected configuration"
            }
            check(callsUrl == OPENAI_REALTIME_CALLS_URL) {
                "Credential broker returned an unexpected Realtime endpoint"
            }
            return BrokerSecret(token, callsUrl, configurationId)
        } finally {
            connection.disconnect()
        }
    }

    private fun exchangeSdp(secret: BrokerSecret, offerSdp: String): String {
        check(offerSdp.isNotBlank()) { "SDP offer is empty" }
        check(offerSdp.length <= MAX_SDP_BYTES) { "SDP offer exceeds the spike limit" }

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
            connection.outputStream.use { output ->
                output.write(offerSdp.toByteArray(StandardCharsets.UTF_8))
            }
            val status = connection.responseCode
            if (status !in 200..299) error("OpenAI Realtime SDP exchange returned HTTP $status")
            return readBounded(connection.inputStream, MAX_SDP_BYTES).also { answer ->
                check(answer.isNotBlank()) { "OpenAI Realtime returned an empty SDP answer" }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun validateBrokerUrl(rawUrl: String) {
        val uri = Uri.parse(rawUrl.trim())
        val isHttps = uri.scheme.equals("https", ignoreCase = true)
        val isEmulatorHost = uri.scheme.equals("http", ignoreCase = true) && uri.host == "10.0.2.2"
        require(uri.host != null && (isHttps || isEmulatorHost)) {
            "Broker URL must use HTTPS (or http://10.0.2.2 for the Android emulator)"
        }
    }

    private fun readBounded(stream: InputStream, maxBytes: Int): String = stream.use { input ->
        val output = ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
        val buffer = ByteArray(8 * 1024)
        var totalBytes = 0
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            totalBytes += count
            check(totalBytes <= maxBytes) { "Network response exceeds the spike limit" }
            output.write(buffer, 0, count)
        }
        String(output.toByteArray(), StandardCharsets.UTF_8)
    }

    private fun isCurrent(generation: Long): Boolean =
        !closed && connectionGeneration.get() == generation

    private fun emitStatus(status: String) {
        val generation = connectionGeneration.get()
        mainHandler.post {
            if (isCurrent(generation)) listener.onStatus(status.take(200))
        }
    }

    private fun emitFailure(stage: String, message: String) {
        val generation = connectionGeneration.get()
        mainHandler.post {
            if (isCurrent(generation)) {
                listener.onFailure(stage.take(64), message.take(200))
            }
        }
    }

    private fun safeMessage(error: Throwable): String =
        when (error) {
            is IllegalArgumentException,
            is IllegalStateException,
            -> error.message?.take(200) ?: error.javaClass.simpleName
            else -> error.javaClass.simpleName
        }

    private abstract class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    companion object {
        private const val NETWORK_TIMEOUT_MS = 15_000
        private const val MAX_BROKER_RESPONSE_BYTES = 32 * 1024
        private const val MAX_SDP_BYTES = 512 * 1024
        private const val MAX_EVENT_BYTES = 32 * 1024
        private const val MAX_CLIENT_EVENT_BYTES = 8 * 1024
        private const val OPENAI_REALTIME_CALLS_URL = "https://api.openai.com/v1/realtime/calls"
        private const val EXPECTED_CONFIGURATION_ID = "direct-openai:webrtc-ephemeral-v1"
        private const val SYNTHETIC_FIRST_SPEECH_INSTRUCTIONS =
            "This is a synthetic engineering latency probe. Say exactly: Good morning. Voice path ready."
        private val EVENT_TYPE_PATTERN = Regex("^[A-Za-z0-9._:-]{1,128}$")

        @Volatile
        private var webRtcInitialized = false
        private val webRtcInitializationLock = Any()

        private fun initializeWebRtcOnce(context: Context) {
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

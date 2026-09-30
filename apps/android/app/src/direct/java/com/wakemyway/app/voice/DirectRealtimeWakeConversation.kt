package com.wakemyway.app.voice

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.wakemyway.app.network.WakeHttpClient
import com.wakemyway.app.product.account.WakeAccountManager
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.runtime.SpeechIntent
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

/** Distribution-scoped conversational enrichment. Alarm Kernel and Wake Runtime remain authoritative. */
class DirectRealtimeWakeConversation(
    context: Context,
    private val listener: WakeConversationEnrichment.Listener,
) : WakeConversationEnrichment {
    private data class BrokerSecret(val token: String, val callsUrl: String, val voice: String)
    private class BrokerCredentialRejected : Exception()

    private val appContext = context.applicationContext
    private val accountManager = WakeAccountManager.get(appContext)
    private val credentialStore = RealtimeAccountCredentialStore(appContext)
    private val provisioningClient = AccountRealtimeProvisioningClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generation = AtomicLong(0L)
    private val readyState = AtomicBoolean(false)
    private val connectingState = AtomicBoolean(false)

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
    private val turnCommitGate = RealtimeTurnCommitGate(MIN_USER_TURN_MS)
    private var pendingTurnClassificationItemId: String? = null
    private var assistantTurnCount = 0

    private val sessionConfigurationTimeout = Runnable {
        if (!closed && connectingState.get() && !readyState.get()) emitFailure("session-config-timeout")
    }
    private val sessionBudgetTimeout = Runnable {
        if (!closed && readyState.get()) emitFailure("session-budget")
    }
    private val turnClassificationTimeout = Runnable {
        val itemId = pendingTurnClassificationItemId ?: return@Runnable
        pendingTurnClassificationItemId = null
        Log.w(LOG_TAG, "turn-classification-timeout item=${itemId.take(32)}")
        if (!closed && readyState.get() && inputEnabled) {
            listener.onUserTurnObserved(coherent = false)
        }
    }

    @Volatile private var closed = false

    override val ready: Boolean get() = !closed && readyState.get()

    override fun connect() {
        if (closed || ready || !connectingState.compareAndSet(false, true)) return

        disconnectResources(resetConnecting = false)
        val current = generation.incrementAndGet()
        Log.i(LOG_TAG, "connect generation=$current")
        networkScope.launch {
            runCatching { requestBrokerSecretWithAccount() }
                .onSuccess { secret ->
                    Log.i(LOG_TAG, "broker-ready generation=$current")
                    mainHandler.post {
                        if (isCurrent(current)) startPeerConnection(secret, current)
                    }
                }
                .onFailure { emitFailure("credential", current) }
        }
    }

    override fun respond(
        intent: SpeechIntent,
        style: VoiceStyle,
    ): Boolean {
        if (!ready) return false
        if (assistantTurnCount >= MAX_ASSISTANT_TURNS) {
            emitFailure("turn-budget")
            return false
        }
        val channel = dataChannel ?: return false
        if (channel.state() != DataChannel.State.OPEN) return false
        val sent = sendEvent(
            channel,
            JSONObject().put("type", "response.create").put(
                "response",
                JSONObject()
                    .put("conversation", "auto")
                    .put("output_modalities", JSONArray().put("audio"))
                    .put("max_output_tokens", MAX_OUTPUT_TOKENS)
                    .put("instructions", AlfredRealtimePrompt.turn(intent, style)),
            ),
        )
        if (sent) assistantTurnCount += 1
        return sent
    }

    override fun setInputEnabled(enabled: Boolean) {
        inputEnabled = enabled
        if (!enabled) {
            turnCommitGate.reset()
            clearPendingTurnClassification()
        }
        audioTrack?.setEnabled(enabled)
    }

    override fun close() {
        if (closed) return
        closed = true
        generation.incrementAndGet()
        disconnectResources()
        networkScope.cancel()
    }

    private fun startPeerConnection(secret: BrokerSecret, current: Long) {
        if (!isCurrent(current)) return
        try {
            Log.i(LOG_TAG, "webrtc-start generation=$current")
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
            val microphone = factory.createAudioTrack("wmw-account-wake-mic", source).apply {
                setEnabled(inputEnabled)
            }
            audioTrack = microphone
            check(peer.addTrack(microphone, listOf("wmw-account-wake")) != null)
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
        networkScope.launch {
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
            Log.i(LOG_TAG, "data-channel-open generation=$current")
            if (!sendSessionConfiguration(channel, secret.voice)) {
                emitFailure("session-config", current)
                return
            }
            mainHandler.removeCallbacks(sessionConfigurationTimeout)
            mainHandler.postDelayed(sessionConfigurationTimeout, SESSION_CONFIGURATION_TIMEOUT_MS)
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
            "session.updated" -> markSessionReady(current)

            "input_audio_buffer.speech_started" -> {
                if (!inputEnabled) return
                turnCommitGate.onSpeechStarted(
                    event.optLong("audio_start_ms", -1L).takeIf { it >= 0L },
                )
                mainHandler.post {
                    if (isCurrent(current)) listener.onUserSpeechStarted()
                }
            }

            "input_audio_buffer.speech_stopped" -> {
                if (!inputEnabled) return
                turnCommitGate.onSpeechStopped(
                    event.optLong("audio_end_ms", -1L).takeIf { it >= 0L },
                )
            }

            "input_audio_buffer.committed" -> {
                if (!inputEnabled) return
                if (!turnCommitGate.onCommitted()) {
                    // speech_started cancelled the listening timeout already. Never leave a
                    // too-short/noisy commit in a permanent listening state.
                    mainHandler.post {
                        if (isCurrent(current) && inputEnabled) {
                            listener.onUserTurnObserved(coherent = false)
                        }
                    }
                    return
                }
                val itemId = event.optString("item_id").trim()
                if (itemId.isBlank() || !ITEM_ID_PATTERN.matches(itemId)) {
                    mainHandler.post {
                        if (isCurrent(current) && inputEnabled) {
                            listener.onUserTurnObserved(coherent = false)
                        }
                    }
                    return
                }
                requestTurnQualityClassification(itemId, current)
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
                val response = event.optJSONObject("response")
                if (handleTurnQualityClassification(response, current)) return
                val status = response?.optString("status").orEmpty()
                val reason = response
                    ?.optJSONObject("status_details")
                    ?.optString("reason")
                    .orEmpty()
                if (status != "completed") {
                    Log.w(
                        LOG_TAG,
                        "response-done status=${status.take(32)} reason=${reason.take(48)} generation=$current",
                    )
                }
                if (RealtimeResponseTerminalPolicy.shouldFailSession(status)) {
                    emitFailure("response", current)
                }
            }

            "error" -> {
                val providerError = event.optJSONObject("error")
                val type = providerError?.optString("type").orEmpty().take(48)
                val code = providerError?.optString("code").orEmpty().take(48)
                Log.w(LOG_TAG, "provider-error type=$type code=$code generation=$current")
                emitFailure("provider", current)
            }
        }
    }

    private fun requestTurnQualityClassification(itemId: String, current: Long) {
        if (!isCurrent(current) || !inputEnabled) return
        // One wake response is enough to advance the deterministic runtime. If semantic VAD
        // splits a sleepy utterance while classification is running, keep the later item in the
        // conversation context but do not create duplicate activation evidence.
        if (pendingTurnClassificationItemId != null) return

        val channel = dataChannel
        if (channel == null || channel.state() != DataChannel.State.OPEN) {
            mainHandler.post {
                if (isCurrent(current) && inputEnabled) {
                    listener.onUserTurnObserved(coherent = false)
                }
            }
            return
        }

        val classification = JSONObject()
            .put("type", "response.create")
            .put(
                "response",
                JSONObject()
                    .put("conversation", "none")
                    .put(
                        "metadata",
                        JSONObject()
                            .put("topic", TURN_CLASSIFICATION_TOPIC)
                            .put("wake_item_id", itemId),
                    )
                    .put("output_modalities", JSONArray().put("text"))
                    .put("max_output_tokens", TURN_CLASSIFICATION_MAX_OUTPUT_TOKENS)
                    .put(
                        "input",
                        JSONArray().put(
                            JSONObject()
                                .put("type", "item_reference")
                                .put("id", itemId),
                        ),
                    )
                    .put("instructions", AlfredRealtimePrompt.TURN_QUALITY_CLASSIFIER),
            )

        pendingTurnClassificationItemId = itemId
        if (!sendEvent(channel, classification)) {
            pendingTurnClassificationItemId = null
            mainHandler.post {
                if (isCurrent(current) && inputEnabled) {
                    listener.onUserTurnObserved(coherent = false)
                }
            }
            return
        }

        mainHandler.removeCallbacks(turnClassificationTimeout)
        mainHandler.postDelayed(turnClassificationTimeout, TURN_CLASSIFICATION_TIMEOUT_MS)
    }

    /**
     * Converts a hidden out-of-band classifier result into a typed observation only.
     * Wake Runtime remains the sole owner of behavioral state transitions and completion.
     */
    private fun handleTurnQualityClassification(response: JSONObject?, current: Long): Boolean {
        val completedResponse = response ?: return false
        val metadata = completedResponse.optJSONObject("metadata") ?: return false
        if (metadata.optString("topic") != TURN_CLASSIFICATION_TOPIC) return false

        val itemId = metadata.optString("wake_item_id").trim()
        if (itemId.isBlank() || itemId != pendingTurnClassificationItemId) return true

        mainHandler.removeCallbacks(turnClassificationTimeout)
        pendingTurnClassificationItemId = null

        val status = completedResponse.optString("status")
        val coherent = if (status == "completed") {
            RealtimeTurnQualityDecision.fromModelOutput(extractResponseText(completedResponse))
        } else {
            false
        }
        Log.i(
            LOG_TAG,
            "turn-quality coherent=$coherent status=${status.take(24)} item=${itemId.take(32)}",
        )
        mainHandler.post {
            if (isCurrent(current) && inputEnabled) {
                listener.onUserTurnObserved(coherent = coherent)
            }
        }
        return true
    }

    private fun extractResponseText(response: JSONObject): String {
        val output = response.optJSONArray("output") ?: return ""
        for (outputIndex in 0 until output.length()) {
            val item = output.optJSONObject(outputIndex) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val part = content.optJSONObject(contentIndex) ?: continue
                if (part.optString("type") == "output_text") return part.optString("text")
            }
        }
        return ""
    }

    private fun clearPendingTurnClassification() {
        pendingTurnClassificationItemId = null
        mainHandler.removeCallbacks(turnClassificationTimeout)
    }
    private fun markSessionReady(current: Long) {
        if (!isCurrent(current) || readyState.getAndSet(true)) return
        Log.i(LOG_TAG, "session-ready generation=$current persona=${AlfredRealtimePrompt.PERSONA_VERSION}")
        connectingState.set(false)
        mainHandler.removeCallbacks(sessionConfigurationTimeout)
        mainHandler.removeCallbacks(sessionBudgetTimeout)
        mainHandler.postDelayed(sessionBudgetTimeout, MAX_SESSION_DURATION_MS)
        mainHandler.post {
            if (isCurrent(current)) listener.onConversationReady()
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
            .put("type", "semantic_vad")
            // A just-woken user pauses, trails off and speaks softly. Low eagerness gives the
            // semantic end-of-turn detector more room before it chunks the utterance.
            .put("eagerness", "low")
            .put("create_response", false)
            .put("interrupt_response", true)
        val session = JSONObject()
            .put("type", "realtime")
            .put("reasoning", JSONObject().put("effort", "low"))
            .put("instructions", AlfredRealtimePrompt.SYSTEM)
            .put("output_modalities", JSONArray().put("audio"))
            .put("max_output_tokens", MAX_OUTPUT_TOKENS)
            .put(
                "truncation",
                JSONObject()
                    .put("type", "retention_ratio")
                    .put("retention_ratio", 0.8)
                    .put("token_limits", JSONObject().put("post_instructions", 4_000)),
            )
            .put(
                "audio",
                JSONObject()
                    .put(
                        "input",
                        JSONObject()
                            .put("noise_reduction", JSONObject().put("type", "far_field"))
                            .put("turn_detection", turnDetection),
                    )
                    .put("output", JSONObject().put("voice", voice).put("speed", 0.96)),
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

    private suspend fun requestBrokerSecretWithAccount(): BrokerSecret {
        val credential = credentialStore.load() ?: provisionDeviceCredential()
        return try {
            requestBrokerSecret(credential.deviceToken)
        } catch (_: BrokerCredentialRejected) {
            // Account-authorized credentials may expire, be rotated, or be revoked server-side.
            // Clear and re-provision once; any remaining failure degrades to the local alarm.
            credentialStore.clear()
            requestBrokerSecret(provisionDeviceCredential().deviceToken)
        }
    }

    private suspend fun provisionDeviceCredential(): RealtimeDeviceCredential {
        val accessToken = accountManager.currentAccessTokenForRealtime()
            ?: error("WakeMyWay account session is unavailable")
        val credential = provisioningClient.provision(
            accessToken = accessToken,
            installationId = credentialStore.installationId(),
        )
        credentialStore.save(credential)
        ConversationalAlfredState.setReady(appContext, true)
        return credential
    }

    private suspend fun requestBrokerSecret(deviceToken: String): BrokerSecret {
        val response = WakeHttpClient.execute(
            Request.Builder()
                .url(AccountRealtimeProvisioningClient.TOKEN_URL)
                .post(EMPTY_REQUEST_BODY)
                .header("Authorization", "Bearer $deviceToken")
                .header("Accept", "application/json")
                .build(),
            MAX_BROKER_RESPONSE_BYTES,
        )
        if (response.status == 401 || response.status == 403) throw BrokerCredentialRejected()
        if (response.status !in 200..299) error("Realtime credential request failed")

        val json = JSONObject(response.body)
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
    }

    private suspend fun exchangeSdp(secret: BrokerSecret, offerSdp: String): String {
        check(offerSdp.isNotBlank() && offerSdp.length <= MAX_SDP_BYTES)
        val response = WakeHttpClient.execute(
            Request.Builder()
                .url(secret.callsUrl)
                .post(offerSdp.toRequestBody(WakeHttpClient.sdpMediaType))
                .header("Authorization", "Bearer ${secret.token}")
                .header("Accept", "application/sdp")
                .build(),
            MAX_SDP_BYTES,
        )
        if (response.status !in 200..299) {
            Log.w(LOG_TAG, "sdp-exchange-rejected status=${response.status}")
            error("Realtime SDP exchange failed")
        }
        Log.i(LOG_TAG, "sdp-exchange-accepted")
        return response.body.also { check(it.isNotBlank()) }
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

    private fun disconnectResources(resetConnecting: Boolean = true) {
        readyState.set(false)
        if (resetConnecting) connectingState.set(false)
        mainHandler.removeCallbacks(sessionConfigurationTimeout)
        mainHandler.removeCallbacks(sessionBudgetTimeout)
        inputEnabled = false
        responseAudible = false
        assistantTurnCount = 0
        turnCommitGate.reset()
        clearPendingTurnClassification()
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

    /**
     * A failure belongs to the connection generation that observed it. Invalidating that generation
     * before resource teardown prevents late callbacks from an old peer/request from failing a newer
     * reconnect. Resource teardown also restores the pre-Realtime audio route before local fallback.
     */
    private fun emitFailure(stage: String, expectedGeneration: Long = generation.get()) {
        mainHandler.post {
            if (!isCurrent(expectedGeneration)) return@post
            Log.w(LOG_TAG, "failure stage=${stage.take(48)} generation=$expectedGeneration")
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

    private companion object {
        const val MAX_BROKER_RESPONSE_BYTES = 32L * 1024
        const val MAX_SDP_BYTES = 512L * 1024
        const val MAX_EVENT_BYTES = 64 * 1024
        const val MAX_CLIENT_EVENT_BYTES = 16 * 1024
        const val MIN_USER_TURN_MS = 160L
        const val TURN_CLASSIFICATION_TIMEOUT_MS = 2_500L
        const val TURN_CLASSIFICATION_MAX_OUTPUT_TOKENS = 16
        const val MAX_ASSISTANT_TURNS = 8
        // Realtime audio output consumes many more output tokens than equivalent text. Keep a
        // bounded but generous ceiling so a normal one- or two-sentence wake prompt is not clipped.
        const val MAX_OUTPUT_TOKENS = 1_024
        const val SESSION_CONFIGURATION_TIMEOUT_MS = 4_000L
        const val MAX_SESSION_DURATION_MS = 180_000L
        const val OPENAI_REALTIME_CALLS_URL = "https://api.openai.com/v1/realtime/calls"
        const val EXPECTED_CONFIGURATION_ID = "direct-openai:webrtc-account-wake-v1"
        const val EXPECTED_PRIVACY_CLASSIFICATION = "authenticated-account-default-api-retention"
        const val LOG_TAG = "WmwRealtime"
        const val TURN_CLASSIFICATION_TOPIC = "wake-turn-quality"
        val EVENT_TYPE_PATTERN = Regex("^[A-Za-z0-9._:-]{1,128}$")
        val ITEM_ID_PATTERN = Regex("^[A-Za-z0-9._:-]{1,160}$")
        val EMPTY_REQUEST_BODY = ByteArray(0).toRequestBody(null)

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

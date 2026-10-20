package com.wakemyway.app.voice

/**
 * Explicit experimental Live Lab deadlines. These are not consumer wake
 * latency targets; the committed local Android alarm stays independent.
 */
internal object GptLiveConnectionTiming {
    // GPT-Live upstream creation is bounded to 15 seconds on the cloud side.
    const val PROVIDER_DEADLINE_MS = 15_000L

    // Credential refresh happens before WebRTC takes over the audio route.
    const val ACCOUNT_PREFLIGHT_TIMEOUT_MS = 20_000L

    // Prefer COMPLETE. With usable candidates after this grace period, send
    // the candidate-rich offer rather than timing out indefinitely.
    const val ICE_GATHERING_TIMEOUT_MS = 8_000L

    // Cover provider deadline plus authorization, cold-start and transit.
    const val LIVE_BACKEND_TIMEOUT_MS = 28_000L

    // ICE + backend + a small remote-SDP/session.started grace period.
    const val WEBRTC_STARTUP_TIMEOUT_MS = 50_000L
}

package com.wakemyway.app.voice

/**
 * Single-shot SDP policy for GPT-Live's non-trickle WebRTC signaling endpoint.
 *
 * Prefer a complete candidate gathering result. Some Android implementations
 * keep gathering past the useful candidate stage: after a short grace period,
 * send an offer ONLY if its SDP already contains candidates. Never initiate a
 * potentially billed session without a usable ICE candidate or ICE credentials.
 *
 * Pure Kotlin so this contract can be tested without device hardware.
 */
internal object GptLiveIceOfferGate {
    enum class Decision { WAIT, SEND, REJECT }

    fun decision(
        localSdp: String?,
        gatheringComplete: Boolean,
        graceExpired: Boolean,
    ): Decision {
        if (!gatheringComplete && !graceExpired) return Decision.WAIT
        if (localSdp.isNullOrBlank()) return Decision.REJECT
        val valid = localSdp.startsWith("v=0") &&
            localSdp.contains("m=audio ") &&
            localSdp.contains("a=ice-ufrag:") &&
            localSdp.contains("a=ice-pwd:") &&
            localSdp.contains("a=fingerprint:") &&
            localSdp.lineSequence().any { it.trim().startsWith("a=candidate:") }
        return if (valid) Decision.SEND else Decision.REJECT
    }
}

package com.wakemyway.app.voice

/**
 * Stable, privacy-safe Voice Lab failure descriptions. Never include the user's
 * speech, raw provider errors, OpenAI key, bearer token, SDP or account details.
 */
internal object GptLivePreviewMessages {
    fun progress(stage: String): String = when (stage) {
        "account-check" -> "Checking your WakeMyWay sign-in…"
        "webrtc-setup" -> "Preparing your microphone and speaker…"
        "webrtc-offer" -> "Preparing secure audio connection…"
        "ice-gathering" -> "Checking network audio route…"
        "partial-ice-offer" -> "Connecting with available network candidates…"
        "server-authentication" -> "Authorizing the GPT-Live preview…"
        "webrtc-answer" -> "Finishing the secure audio handshake…"
        else -> "Connecting to voice preview…"
    }

    fun failure(stage: String): String {
        val recommendation = when (stage) {
            "sign-in-required" ->
                "You need to sign in to WakeMyWay again under Profile, then retry."
            "account-preflight-timeout" ->
                "Sign-in verification took too long. Check the connection and retry before starting audio."
            "device-authorization-failed", "session-authentication-failed" ->
                "Your account could not authorize voice preview. Sign out and sign in again."
            "pilot-access-denied" ->
                "GPT-Live preview isn't enabled for this account. Check pilot access."
            "daily-voice-limit" ->
                "The daily voice preview limit was reached. Try again tomorrow."
            "provider-or-server-unavailable", "server-http-503" ->
                "The voice backend or OpenAI session is unavailable. Check model access and billing."
            "offer-rejected", "invalid-server-answer", "invalid-server-response" ->
                "The WebRTC session handshake failed. This needs an application-side fix."
            "ice-gathering-timeout", "ice-no-candidates" ->
                "Android couldn't establish a usable audio route (no ICE candidates). Try mobile data, disable VPN, or check network permissions."
            "ice-candidates-missing-from-sdp" ->
                "Android found network candidates but did not include them in the offer. This needs a WebRTC signaling fix."
            "startup-timeout", "ice-connection", "peer-connection", "remote-sdp" ->
                "The secure audio connection failed. Try another network or audio device."
            "device-credential-storage-failed" ->
                "Secure device authorization couldn't be stored. Unlock the device and retry."
            "lab-cue-write", "provider-error", "unexpected-delegation",
            "unhandled-delegation", "delegation-write", "channel-closed" ->
                "GPT-Live ended the audio session unexpectedly. Share the diagnostic code."
            "webrtc-init", "offer", "local-sdp" ->
                "Android couldn't initialize the audio session. Check microphone permissions."
            "network-or-server-error", "live-sdp" ->
                "The phone couldn't reach the GPT-Live backend. Check your connection."
            else -> "Preview disconnected. Use the diagnostic code to help identify why."
        }
        return "GPT-Live unavailable ($stage). $recommendation Regular alarms are unaffected."
    }
}

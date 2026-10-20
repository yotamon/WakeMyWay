package com.wakemyway.app.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class GptLiveIceOfferGateTest {
    private val valid = """v=0
o=- 1 1 IN IP4 127.0.0.1
s=-
t=0 0
m=audio 9 UDP/TLS/RTP/SAVPF 111
a=ice-ufrag:abc
a=ice-pwd:def
a=fingerprint:sha-256 00:11
a=candidate:1 1 udp 2122260223 192.0.2.1 55001 typ host
""".trimIndent()

    @Test fun waitsUntilGatheringComplete() {
        assertEquals(GptLiveIceOfferGate.Decision.WAIT,
            GptLiveIceOfferGate.decision(valid, gatheringComplete = false, graceExpired = false))
    }

    @Test fun sendsCompleteCandidateRichOffer() {
        assertEquals(GptLiveIceOfferGate.Decision.SEND,
            GptLiveIceOfferGate.decision(valid, gatheringComplete = true, graceExpired = false))
    }

    @Test fun sendsCandidateRichOfferAfterBoundedWaitEvenIfGatheringNotComplete() {
        assertEquals(GptLiveIceOfferGate.Decision.SEND,
            GptLiveIceOfferGate.decision(valid, gatheringComplete = false, graceExpired = true))
    }

    @Test fun neverSpendsSessionOnMissingCandidates() {
        val noCandidates = valid.lineSequence().filterNot { it.startsWith("a=candidate:") }
            .joinToString("\n")
        assertEquals(GptLiveIceOfferGate.Decision.REJECT,
            GptLiveIceOfferGate.decision(noCandidates, gatheringComplete = true, graceExpired = false))
        assertEquals(GptLiveIceOfferGate.Decision.REJECT,
            GptLiveIceOfferGate.decision(noCandidates, gatheringComplete = false, graceExpired = true))
    }

    @Test fun rejectsMissingIceCredentialsOrMalformedSdp() {
        assertEquals(GptLiveIceOfferGate.Decision.REJECT,
            GptLiveIceOfferGate.decision(valid.replace("a=ice-pwd:", "x="),
                gatheringComplete = true, graceExpired = false))
        assertEquals(GptLiveIceOfferGate.Decision.REJECT,
            GptLiveIceOfferGate.decision("not SDP", gatheringComplete = true, graceExpired = false))
        assertEquals(GptLiveIceOfferGate.Decision.REJECT,
            GptLiveIceOfferGate.decision(null, gatheringComplete = true, graceExpired = false))
    }

    @Test fun doesNotConsiderIceOptionsAloneAUsableCandidate() {
        val trickleOnly = valid.replace(
            "a=candidate:1 1 udp 2122260223 192.0.2.1 55001 typ host",
            "a=ice-options:trickle",
        )
        assertEquals(GptLiveIceOfferGate.Decision.REJECT,
            GptLiveIceOfferGate.decision(trickleOnly, gatheringComplete = true, graceExpired = false))
    }
}

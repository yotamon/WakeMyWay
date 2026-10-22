package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveSessionIdentityTest {
    @Test fun validProviderIdentifiersMustNotRequireAStablePrefix() {
        for (id in listOf("sess_123", "live_abcd", "provider-future-v2")) {
            assertTrue(GptLiveSessionIdentity.isValid(id))
        }
    }

    @Test fun invalidIdsStayRejectedEvenWithoutPrefixPinning() {
        assertFalse(GptLiveSessionIdentity.isValid(""))
        assertFalse(GptLiveSessionIdentity.isValid("123"))
        assertFalse(GptLiveSessionIdentity.isValid("a bad id"))
        assertFalse(GptLiveSessionIdentity.isValid("bad\nid"))
        assertFalse(GptLiveSessionIdentity.isValid("x".repeat(257)))
    }
}

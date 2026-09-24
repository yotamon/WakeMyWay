package com.wakemyway.app.commerce

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayPurchaseVerifierTest {
    @Test
    fun `verification stays unavailable unless explicitly enabled with HTTPS endpoint`() {
        assertFalse(
            PlayPurchaseVerifier.create(
                enabled = false,
                apiBaseUrl = "https://wakemyway.example",
            ).available,
        )
        assertFalse(
            PlayPurchaseVerifier.create(
                enabled = true,
                apiBaseUrl = "",
            ).available,
        )
        assertFalse(
            PlayPurchaseVerifier.create(
                enabled = true,
                apiBaseUrl = "http://wakemyway.example",
            ).available,
        )
        assertTrue(
            PlayPurchaseVerifier.create(
                enabled = true,
                apiBaseUrl = "https://wakemyway.example/",
            ).available,
        )
    }
}

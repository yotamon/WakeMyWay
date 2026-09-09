package com.wakemyway.core.character

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfflineVoiceSelectorTest {
    @Test
    fun `network-required voices are never selected`() {
        val selected = OfflineVoiceSelector.select(
            candidates = listOf(
                LocalVoiceCandidate(
                    id = "network-premium-en-gb",
                    languageTag = "en-GB",
                    networkRequired = true,
                    quality = 1000,
                ),
                LocalVoiceCandidate(
                    id = "local-en-gb",
                    languageTag = "en-GB",
                    networkRequired = false,
                    quality = 100,
                ),
            ),
            preferredLanguageTag = "en-GB",
        )

        assertEquals("local-en-gb", selected?.id)
    }

    @Test
    fun `exact locale beats higher quality fallback locale`() {
        val selected = OfflineVoiceSelector.select(
            candidates = listOf(
                LocalVoiceCandidate(
                    id = "local-en-us-premium",
                    languageTag = "en-US",
                    networkRequired = false,
                    quality = 900,
                ),
                LocalVoiceCandidate(
                    id = "local-en-gb-standard",
                    languageTag = "en-GB",
                    networkRequired = false,
                    quality = 100,
                ),
            ),
            preferredLanguageTag = "en-GB",
        )

        assertEquals("local-en-gb-standard", selected?.id)
    }

    @Test
    fun `quality breaks ties within the same locale`() {
        val selected = OfflineVoiceSelector.select(
            candidates = listOf(
                LocalVoiceCandidate("standard", "en-GB", networkRequired = false, quality = 100),
                LocalVoiceCandidate("enhanced", "en-GB", networkRequired = false, quality = 500),
            ),
            preferredLanguageTag = "en-GB",
        )

        assertEquals("enhanced", selected?.id)
    }

    @Test
    fun `same-language local fallback is allowed when exact locale is absent`() {
        val selected = OfflineVoiceSelector.select(
            candidates = listOf(
                LocalVoiceCandidate("local-en-us", "en-US", networkRequired = false, quality = 200),
            ),
            preferredLanguageTag = "en-GB",
        )

        assertEquals("local-en-us", selected?.id)
    }

    @Test
    fun `unrelated-language local voices fail closed`() {
        val selected = OfflineVoiceSelector.select(
            candidates = listOf(
                LocalVoiceCandidate("local-de", "de-DE", networkRequired = false, quality = 1000),
                LocalVoiceCandidate("local-fr", "fr-FR", networkRequired = false, quality = 1000),
            ),
            preferredLanguageTag = "en-GB",
        )

        assertNull(selected)
    }

    @Test
    fun `network-only catalog fails closed`() {
        val selected = OfflineVoiceSelector.select(
            candidates = listOf(
                LocalVoiceCandidate("network-only", "en-GB", networkRequired = true, quality = 1000),
            ),
            preferredLanguageTag = "en-GB",
        )

        assertNull(selected)
    }
}

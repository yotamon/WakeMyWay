package com.wakemyway.core.character

import com.wakemyway.core.runtime.SpeechIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AlfredCharacterTest {
    private val intents = listOf(
        SpeechIntent.InitialWake,
        SpeechIntent.AskToSitUp,
        SpeechIntent.AskToMove,
        SpeechIntent.ReEngage(0),
        SpeechIntent.ReEngage(1),
        SpeechIntent.ReEngage(2),
        SpeechIntent.ReEngage(3),
        SpeechIntent.SnoozeConfirmation,
        SpeechIntent.SnoozeFailed,
        SpeechIntent.Orientation,
    )

    @Test
    fun `Alfred has a versioned stable British English character spec`() {
        val spec = AlfredCharacter.spec

        assertEquals(CharacterId("alfred"), spec.id)
        assertEquals(1, spec.version)
        assertEquals("Alfred", spec.displayName)
        assertEquals("en-GB", spec.voiceLocaleTag)
        assertTrue(spec.speechRate < 1f)
    }

    @Test
    fun `every current speech intent renders a concise non-empty Alfred line`() {
        intents.forEach { intent ->
            val rendered = AlfredCharacter.render(intent, WakeLineKey("coverage-${intent.hashCode()}"))

            assertEquals(AlfredCharacter.spec.id, rendered.characterId)
            assertEquals(AlfredCharacter.spec.version, rendered.characterVersion)
            assertEquals(intent, rendered.intent)
            assertTrue(rendered.text.isNotBlank())
            assertTrue(rendered.text.length <= RenderedWakeLine.MAX_WAKE_LINE_CHARACTERS)
            assertTrue(rendered.text.wordCount() <= MAX_EXPECTED_WORDS)
        }
    }

    @Test
    fun `same intent and render key always produces the same line`() {
        intents.forEach { intent ->
            val key = WakeLineKey("session-42-step-7")
            val first = AlfredCharacter.render(intent, key)
            val replayed = AlfredCharacter.render(intent, key)

            assertEquals(first, replayed)
        }
    }

    @Test
    fun `different render keys provide bounded variation where useful`() {
        intents.forEach { intent ->
            val variants = (0 until 128)
                .map { index -> AlfredCharacter.render(intent, WakeLineKey("variant-$index")) }
                .map { rendered -> rendered.variantIndex to rendered.text }
                .toSet()

            assertTrue(variants.size >= 2, "Expected variation for $intent")
            assertTrue(variants.size <= 4, "Unexpected unbounded variant count for $intent")
        }
    }

    @Test
    fun `re-engage escalation changes language without unbounded levels`() {
        val key = WakeLineKey("escalation")
        val gentle = AlfredCharacter.render(SpeechIntent.ReEngage(0), key)
        val firm = AlfredCharacter.render(SpeechIntent.ReEngage(3), key)
        val belowRange = AlfredCharacter.render(SpeechIntent.ReEngage(-50), key)
        val aboveRange = AlfredCharacter.render(SpeechIntent.ReEngage(50), key)

        assertNotEquals(gentle.text, firm.text)
        assertEquals(gentle.text, belowRange.text)
        assertEquals(gentle.variantIndex, belowRange.variantIndex)
        assertEquals(firm.text, aboveRange.text)
        assertEquals(firm.variantIndex, aboveRange.variantIndex)
    }

    @Test
    fun `curated Alfred catalog never uses shame insult or threat vocabulary`() {
        val renderedCatalog = intents.flatMap { intent ->
            (0 until 256).map { index ->
                AlfredCharacter.render(intent, WakeLineKey("safety-$index")).text.lowercase()
            }
        }.toSet()

        renderedCatalog.forEach { line ->
            FORBIDDEN_TERMS.forEach { forbidden ->
                assertTrue(
                    forbidden !in line,
                    "Alfred line contains forbidden term '$forbidden': $line",
                )
            }
        }
    }

    private fun String.wordCount(): Int = trim().split(Regex("\\s+")).size

    private companion object {
        const val MAX_EXPECTED_WORDS = 16
        val FORBIDDEN_TERMS = setOf(
            "lazy",
            "stupid",
            "weak",
            "pathetic",
            "loser",
            "worthless",
            "ashamed",
            "shame on",
            "disappointed in you",
            "you will regret",
            "punishment",
        )
    }
}

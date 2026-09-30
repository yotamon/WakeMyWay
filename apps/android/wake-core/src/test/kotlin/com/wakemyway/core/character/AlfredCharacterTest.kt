package com.wakemyway.core.character

import com.wakemyway.core.alarm.VoiceStyle
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
        SpeechIntent.ActivateUpperBody,
        SpeechIntent.StandIfSafe,
        SpeechIntent.KeepEngaging,
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
        assertEquals(7, spec.version)
        assertEquals("Alfred", spec.displayName)
        assertEquals("en-GB", spec.voiceLocaleTag)
        assertTrue(spec.speechRate < 1f)
    }

    @Test
    fun `every current speech intent and style renders a concise non-empty Alfred line`() {
        VoiceStyle.entries.forEach { style ->
            intents.forEach { intent ->
                val rendered = AlfredCharacter.render(
                    intent = intent,
                    key = WakeLineKey("coverage-${style.name}-${intent.hashCode()}"),
                    style = style,
                )

                assertEquals(AlfredCharacter.spec.id, rendered.characterId)
                assertEquals(AlfredCharacter.spec.version, rendered.characterVersion)
                assertEquals(intent, rendered.intent)
                assertTrue(rendered.text.isNotBlank())
                assertTrue(rendered.text.length <= RenderedWakeLine.MAX_WAKE_LINE_CHARACTERS)
                assertTrue(rendered.text.wordCount() <= MAX_EXPECTED_WORDS)
            }
        }
    }

    @Test
    fun `same intent style and render key always produces the same line`() {
        VoiceStyle.entries.forEach { style ->
            intents.forEach { intent ->
                val key = WakeLineKey("session-42-step-7")
                val first = AlfredCharacter.render(intent, key, style)
                val replayed = AlfredCharacter.render(intent, key, style)
                assertEquals(first, replayed)
            }
        }
    }

    @Test
    fun `different render keys provide bounded variation for every style`() {
        VoiceStyle.entries.forEach { style ->
            intents.forEach { intent ->
                val variants = (0 until 128)
                    .map { index -> AlfredCharacter.render(intent, WakeLineKey("variant-$index"), style) }
                    .map { rendered -> rendered.variantIndex to rendered.text }
                    .toSet()

                assertTrue(variants.size >= 2, "Expected variation for $style / $intent")
                assertTrue(variants.size <= 4, "Unexpected unbounded variant count for $style / $intent")
            }
        }
    }

    @Test
    fun `voice styles produce genuinely different language`() {
        intents.forEach { intent ->
            val key = WakeLineKey("style-difference-${intent.hashCode()}")
            val default = AlfredCharacter.render(intent, key, VoiceStyle.DEFAULT).text
            val motivational = AlfredCharacter.render(intent, key, VoiceStyle.MOTIVATIONAL).text
            val minimal = AlfredCharacter.render(intent, key, VoiceStyle.MINIMAL).text

            assertNotEquals(default, motivational, "Motivational style must alter $intent")
            assertNotEquals(default, minimal, "Minimal style must alter $intent")
            assertNotEquals(motivational, minimal, "Styles must remain distinct for $intent")
        }
    }

    @Test
    fun `minimal style stays especially terse`() {
        intents.forEach { intent ->
            renderedTexts(intent, VoiceStyle.MINIMAL).forEach { line ->
                assertTrue(
                    line.wordCount() <= MAX_MINIMAL_WORDS,
                    "Minimal line is too long for $intent: $line",
                )
            }
        }
    }

    @Test
    fun `opening turn asks for one sit-up action and a spoken reply`() {
        VoiceStyle.entries.forEach { style ->
            renderedTexts(SpeechIntent.InitialWake, style).forEach { line ->
                assertTrue("sit" in line, "Opening line must contain the first physical action: $line")
                assertTrue(
                    listOf("tell", "answer", "hello").any { cue -> cue in line },
                    "Opening line must invite a short spoken reply: $line",
                )
            }
        }
    }

    @Test
    fun `upper body activation stays seated and bounded`() {
        VoiceStyle.entries.forEach { style ->
            renderedTexts(SpeechIntent.ActivateUpperBody, style).forEach { line ->
                assertTrue("shoulder" in line, "Upper-body cue must stay concrete: $line")
                assertTrue(
                    listOf("jump", "squat", "run", "breath").none { forbidden -> forbidden in line },
                    "Upper-body cue introduced unsafe escalation: $line",
                )
            }
        }
    }

    @Test
    fun `standing cue always includes a safety alternative`() {
        VoiceStyle.entries.forEach { style ->
            renderedTexts(SpeechIntent.StandIfSafe, style).forEach { line ->
                assertTrue("safe" in line, "Standing cue must be explicitly conditional: $line")
                assertTrue(
                    listOf("otherwise", "if not").any { alternative -> alternative in line },
                    "Standing cue must include a seated alternative: $line",
                )
                assertTrue(
                    listOf("sit", "seated").any { seated -> seated in line },
                    "Standing cue must preserve a seated alternative: $line",
                )
            }
        }
    }

    @Test
    fun `re-engage never escalates posture or movement`() {
        VoiceStyle.entries.forEach { style ->
            (0..3).forEach { level ->
                renderedTexts(SpeechIntent.ReEngage(level), style).forEach { line ->
                    listOf("feet", "upright", "stand", "sit up", "move now").forEach { forbidden ->
                        assertTrue(
                            forbidden !in line,
                            "Re-engage must not invent a harder physical action with '$forbidden': $line",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `re-engage escalation changes language without unbounded levels`() {
        VoiceStyle.entries.forEach { style ->
            val key = WakeLineKey("escalation-${style.name}")
            val gentle = AlfredCharacter.render(SpeechIntent.ReEngage(0), key, style)
            val firm = AlfredCharacter.render(SpeechIntent.ReEngage(3), key, style)
            val belowRange = AlfredCharacter.render(SpeechIntent.ReEngage(-50), key, style)
            val aboveRange = AlfredCharacter.render(SpeechIntent.ReEngage(50), key, style)

            assertNotEquals(gentle.text, firm.text)
            assertEquals(gentle.text, belowRange.text)
            assertEquals(gentle.variantIndex, belowRange.variantIndex)
            assertEquals(firm.text, aboveRange.text)
            assertEquals(firm.variantIndex, aboveRange.variantIndex)
        }
    }

    @Test
    fun `snooze confirmation never claims that replacement scheduling already succeeded`() {
        VoiceStyle.entries.forEach { style ->
            val lines = renderedTexts(SpeechIntent.SnoozeConfirmation, style)
            lines.forEach { line ->
                PREMATURE_SNOOZE_SUCCESS_TERMS.forEach { forbidden ->
                    assertTrue(
                        forbidden !in line,
                        "Snooze confirmation over-claimed durable state with '$forbidden': $line",
                    )
                }
            }
        }
    }

    @Test
    fun `orientation never claims biological wakefulness or unsupported posture`() {
        VoiceStyle.entries.forEach { style ->
            val lines = renderedTexts(SpeechIntent.Orientation, style)
            lines.forEach { line ->
                UNSUPPORTED_ORIENTATION_CLAIMS.forEach { forbidden ->
                    assertTrue(
                        forbidden !in line,
                        "Orientation line over-claimed wake state with '$forbidden': $line",
                    )
                }
            }
        }
    }

    @Test
    fun `curated Alfred catalog never uses shame insult or threat vocabulary`() {
        val renderedCatalog = VoiceStyle.entries.flatMap { style ->
            intents.flatMap { intent -> renderedTexts(intent, style) }
        }.toSet()
        renderedCatalog.forEach { line ->
            FORBIDDEN_TERMS.forEach { forbidden ->
                assertTrue(forbidden !in line, "Alfred line contains forbidden term '$forbidden': $line")
            }
        }
    }

    private fun renderedTexts(
        intent: SpeechIntent,
        style: VoiceStyle = VoiceStyle.DEFAULT,
    ): Set<String> = (0 until 256)
        .map { index -> AlfredCharacter.render(intent, WakeLineKey("catalog-$index"), style).text.lowercase() }
        .toSet()

    private fun String.wordCount(): Int = trim().split(Regex("\\s+")).size

    private companion object {
        const val MAX_EXPECTED_WORDS = 16
        const val MAX_MINIMAL_WORDS = 8

        val PREMATURE_SNOOZE_SUCCESS_TERMS = setOf(
            "snooze accepted",
            "snooze scheduled",
            "was scheduled",
            "has been scheduled",
            "snooze confirmed",
        )

        val UNSUPPORTED_ORIENTATION_CLAIMS = setOf(
            "you are awake",
            "you're awake",
            "fully awake",
            "you are upright",
            "you're upright",
        )

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

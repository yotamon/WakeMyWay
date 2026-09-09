package com.wakemyway.core.motion

import com.wakemyway.core.runtime.MotionEvidenceKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MotionEvidenceExtractorTest {
    @Test
    fun `pickup requires a high acceleration and meaningful tilt change close in time`() {
        val extractor = MotionEvidenceExtractor()
        extractor.onTilt(ms(0), 5.0)

        assertTrue(extractor.onAcceleration(ms(100), 2.0).isEmpty())
        val evidence = extractor.onTilt(ms(500), 25.0)

        assertEquals(listOf(MotionEvidenceKind.DEVICE_PICKUP), evidence.map { it.kind })
    }

    @Test
    fun `large tilt change emits orientation evidence without acceleration`() {
        val extractor = MotionEvidenceExtractor()
        extractor.onTilt(ms(0), 0.0)

        val evidence = extractor.onTilt(ms(400), 35.0)

        assertEquals(listOf(MotionEvidenceKind.ORIENTATION_CHANGE), evidence.map { it.kind })
    }

    @Test
    fun `small orientation jitter does not create evidence`() {
        val extractor = MotionEvidenceExtractor()
        extractor.onTilt(ms(0), 4.0)

        listOf(6.0, 2.0, 8.0, 10.0, 7.0).forEachIndexed { index, tilt ->
            assertTrue(extractor.onTilt(ms((index + 1) * 200L), tilt).isEmpty())
        }
    }

    @Test
    fun `sustained movement requires multiple samples across time`() {
        val extractor = MotionEvidenceExtractor()

        assertTrue(extractor.onAcceleration(ms(0), 0.9).isEmpty())
        assertTrue(extractor.onAcceleration(ms(250), 1.0).isEmpty())
        assertTrue(extractor.onAcceleration(ms(500), 0.85).isEmpty())
        val evidence = extractor.onAcceleration(ms(800), 0.95)

        assertEquals(listOf(MotionEvidenceKind.SUSTAINED_MOVEMENT), evidence.map { it.kind })
    }

    @Test
    fun `movement samples outside the bounded window do not accumulate forever`() {
        val extractor = MotionEvidenceExtractor()

        extractor.onAcceleration(ms(0), 1.0)
        extractor.onAcceleration(ms(250), 1.0)
        extractor.onAcceleration(ms(500), 1.0)
        val muchLater = extractor.onAcceleration(ms(3_000), 1.0)

        assertTrue(muchLater.none { it.kind == MotionEvidenceKind.SUSTAINED_MOVEMENT })
    }

    @Test
    fun `pickup cooldown prevents one physical action from flooding evidence`() {
        val extractor = MotionEvidenceExtractor()
        extractor.onTilt(ms(0), 0.0)
        extractor.onAcceleration(ms(100), 2.0)
        val first = extractor.onTilt(ms(300), 20.0)
        assertEquals(1, first.count { it.kind == MotionEvidenceKind.DEVICE_PICKUP })

        extractor.onAcceleration(ms(500), 2.0)
        val withinCooldown = extractor.onTilt(ms(700), 40.0)
        assertTrue(withinCooldown.none { it.kind == MotionEvidenceKind.DEVICE_PICKUP })

        extractor.onAcceleration(ms(3_600), 2.0)
        val afterCooldown = extractor.onTilt(ms(3_800), 60.0)
        assertEquals(1, afterCooldown.count { it.kind == MotionEvidenceKind.DEVICE_PICKUP })
    }

    @Test
    fun `reset removes prior anchors and temporal evidence`() {
        val extractor = MotionEvidenceExtractor()
        extractor.onTilt(ms(0), 0.0)
        extractor.onAcceleration(ms(100), 2.0)
        extractor.reset()

        assertTrue(extractor.onTilt(ms(200), 70.0).isEmpty())
        assertTrue(extractor.onAcceleration(ms(300), 2.0).isEmpty())
    }

    private fun ms(value: Long): Long = value * 1_000_000L
}

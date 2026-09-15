package com.wakemyway.app.alarm

import com.wakemyway.core.alarm.WakeSoundId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeSoundPreviewPlayerTest {
    @Test
    fun `play starts only an exactly resolved branded resource`() {
        val sessions = mutableListOf<FakeSession>()
        val player = WakeSoundPreviewPlayer(
            resolveResource = { id -> if (id == WakeSoundId.MORNING_LIGHT) 101 else null },
            createSession = { resourceId -> FakeSession(resourceId).also(sessions::add) },
        )

        assertTrue(player.play(WakeSoundId.MORNING_LIGHT))
        assertEquals(WakeSoundId.MORNING_LIGHT, player.currentSoundId)
        assertEquals(listOf(101), sessions.map { it.resourceId })

        player.close()
        assertTrue(sessions.single().released)
        assertNull(player.currentSoundId)
    }

    @Test
    fun `switching preview releases the previous sound first`() {
        val events = mutableListOf<String>()
        val player = WakeSoundPreviewPlayer(
            resolveResource = { id ->
                when (id) {
                    WakeSoundId.MORNING_LIGHT -> 101
                    WakeSoundId.SOFT_START -> 202
                    else -> null
                }
            },
            createSession = { resourceId ->
                object : WakeSoundPreviewPlayer.PreviewSession {
                    override fun stopAndRelease() {
                        events += "release-$resourceId"
                    }
                }.also { events += "start-$resourceId" }
            },
        )

        assertTrue(player.play(WakeSoundId.MORNING_LIGHT))
        assertTrue(player.play(WakeSoundId.SOFT_START))

        assertEquals(
            listOf("start-101", "release-101", "start-202"),
            events,
        )
        assertEquals(WakeSoundId.SOFT_START, player.currentSoundId)
    }

    @Test
    fun `missing branded resource never creates fallback preview`() {
        var createCount = 0
        val player = WakeSoundPreviewPlayer(
            resolveResource = { null },
            createSession = {
                createCount += 1
                FakeSession(it)
            },
        )

        assertFalse(player.play(WakeSoundId.MORNING_PULSE))
        assertEquals(0, createCount)
        assertNull(player.currentSoundId)
    }

    @Test
    fun `failed preview session leaves no active sound`() {
        val player = WakeSoundPreviewPlayer(
            resolveResource = { 303 },
            createSession = { null },
        )

        assertFalse(player.play(WakeSoundId.MORNING_PULSE))
        assertNull(player.currentSoundId)
    }

    @Test
    fun `stop is idempotent`() {
        val session = FakeSession(101)
        val player = WakeSoundPreviewPlayer(
            resolveResource = { 101 },
            createSession = { session },
        )

        assertTrue(player.play(WakeSoundId.MORNING_LIGHT))
        player.stop()
        player.stop()

        assertEquals(1, session.releaseCount)
        assertNull(player.currentSoundId)
    }

    private class FakeSession(val resourceId: Int) : WakeSoundPreviewPlayer.PreviewSession {
        var releaseCount = 0
        val released: Boolean
            get() = releaseCount > 0

        override fun stopAndRelease() {
            releaseCount += 1
        }
    }
}

package com.wakemyway.app

import com.wakemyway.app.product.ConsumerPreferences
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.preparation.TomorrowContractId
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WakeAllowedContextResolverTest {
    @Test
    fun `tomorrow contract text is withheld without explicit voice consent`() {
        val context = resolveWakeAllowedContext(
            preferences = ConsumerPreferences(displayName = "Yotam"),
            alarmDefinition = alarm(firstMove = "Open the curtains"),
            tomorrowContract = contract(
                reason = "Interview at ten",
                firstMove = "Take a shower",
                consent = false,
            ),
        )

        assertEquals("Yotam", context.displayName)
        assertNull(context.tomorrowReason)
        assertEquals("Open the curtains", context.firstMove)
    }

    @Test
    fun `consented tomorrow contract becomes the occurrence voice context`() {
        val context = resolveWakeAllowedContext(
            preferences = ConsumerPreferences(displayName = "Yotam"),
            alarmDefinition = alarm(firstMove = "Open the curtains"),
            tomorrowContract = contract(
                reason = "Interview at ten",
                firstMove = "Take a shower",
                consent = true,
            ),
        )

        assertEquals("Interview at ten", context.tomorrowReason)
        assertEquals("Take a shower", context.firstMove)
    }

    private fun alarm(firstMove: String?) = AlarmDefinition(
        id = AlarmDefinitionId("alarm"),
        zoneId = ZoneId.of("Europe/Berlin"),
        schedule = AlarmSchedulePattern.Weekly(
            days = setOf(DayOfWeek.MONDAY),
            time = LocalTime.of(7, 30),
        ),
        firstMoveDefault = firstMove,
        createdAt = Instant.parse("2026-09-30T20:00:00Z"),
        updatedAt = Instant.parse("2026-09-30T20:00:00Z"),
    )

    private fun contract(
        reason: String,
        firstMove: String?,
        consent: Boolean,
    ) = TomorrowContract(
        id = TomorrowContractId("contract"),
        wakeOccurrenceId = WakeOccurrenceId("occurrence"),
        rawText = reason,
        firstMove = firstMove,
        useInVoiceCheckIn = consent,
        createdAtEpochMillis = 1L,
    )
}

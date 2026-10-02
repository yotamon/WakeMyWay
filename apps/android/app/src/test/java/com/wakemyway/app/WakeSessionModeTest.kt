package com.wakemyway.app

import com.wakemyway.core.schedule.WakeScheduleId
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeSessionModeTest {
    @Test
    fun `all wake lab schedules are test sessions`() {
        assertEquals(WakeSessionMode.TEST, wakeSessionMode(WakeScheduleId("lab-immediate")))
        assertEquals(WakeSessionMode.TEST, wakeSessionMode(WakeScheduleId("lab-doze-idle-123")))
    }

    @Test
    fun `consumer alarm schedules are normal sessions`() {
        assertEquals(WakeSessionMode.NORMAL, wakeSessionMode(WakeScheduleId("alarm-weekday")))
    }

    @Test
    fun `test sessions never become user history evidence`() {
        assertEquals(false, shouldRecordWakeHistory(WakeSessionMode.TEST))
        assertEquals(true, shouldRecordWakeHistory(WakeSessionMode.NORMAL))
    }
}

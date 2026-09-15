package com.wakemyway.app.alarm

import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeScheduleId

/** Consumer-facing health projection for one critical alarm schedule slot. */
data class AlarmScheduleHealth(
    val scheduleId: WakeScheduleId,
    val enabled: Boolean,
    val ready: Boolean,
    val nextOccurrence: WakeOccurrence?,
    val activeOccurrence: WakeOccurrence?,
)

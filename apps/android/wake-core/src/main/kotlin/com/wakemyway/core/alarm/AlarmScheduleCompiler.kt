package com.wakemyway.core.alarm

import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId

/**
 * Pure translation from rich consumer alarm state into the minimal deterministic schedule contract
 * owned by Alarm Kernel.
 */
class AlarmScheduleCompiler {
    fun compile(alarm: AlarmDefinition): WakeSchedule = when (val pattern = alarm.schedule) {
        is AlarmSchedulePattern.Weekly -> WakeSchedule(
            id = WakeScheduleId(alarm.id.value),
            zoneId = alarm.zoneId,
            timesByDay = pattern.days.associateWith { pattern.time },
            revision = alarm.revision,
            completionPolicy = WakeCompletionPolicy.RECURRING,
        )

        is AlarmSchedulePattern.OneShot -> WakeSchedule(
            id = WakeScheduleId(alarm.id.value),
            zoneId = alarm.zoneId,
            timesByDay = mapOf(pattern.date.dayOfWeek to pattern.time),
            revision = alarm.revision,
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
            oneShotDate = pattern.date,
        )
    }
}

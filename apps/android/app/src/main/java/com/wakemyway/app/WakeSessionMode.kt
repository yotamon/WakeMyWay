package com.wakemyway.app

import com.wakemyway.core.schedule.WakeScheduleId

enum class WakeSessionMode {
    NORMAL,
    TEST,
}

internal fun wakeSessionMode(scheduleId: WakeScheduleId): WakeSessionMode =
    if (scheduleId.value.startsWith(TEST_WAKE_SCHEDULE_PREFIX)) {
        WakeSessionMode.TEST
    } else {
        WakeSessionMode.NORMAL
    }

internal fun shouldRecordWakeHistory(mode: WakeSessionMode): Boolean =
    mode == WakeSessionMode.NORMAL

private const val TEST_WAKE_SCHEDULE_PREFIX = "lab-"

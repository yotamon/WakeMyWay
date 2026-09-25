package com.wakemyway.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeWidgetActionPolicyTest {
    @Test
    fun `active wake always wins contextual priority`() {
        assertEquals(
            WakeWidgetPrimaryAction.OPEN_WAKE,
            selectWakeWidgetPrimaryAction(
                activeWake = true,
                hasNextWake = true,
                nextWakeReady = false,
                pendingMorningCheckIn = true,
                planState = WakeWidgetPlanState.AVAILABLE,
            ),
        )
    }

    @Test
    fun `no next wake routes to alarm creation when no check in is pending`() {
        assertEquals(
            WakeWidgetPrimaryAction.CREATE_ALARM,
            selectWakeWidgetPrimaryAction(
                activeWake = false,
                hasNextWake = false,
                nextWakeReady = false,
                pendingMorningCheckIn = false,
                planState = WakeWidgetPlanState.NONE,
            ),
        )
    }

    @Test
    fun `pending morning check in survives a consumed one shot wake`() {
        assertEquals(
            WakeWidgetPrimaryAction.MORNING_CHECK_IN,
            selectWakeWidgetPrimaryAction(
                activeWake = false,
                hasNextWake = false,
                nextWakeReady = false,
                pendingMorningCheckIn = true,
                planState = WakeWidgetPlanState.NONE,
            ),
        )
    }

    @Test
    fun `readiness repair outranks check in and preparation`() {
        assertEquals(
            WakeWidgetPrimaryAction.FIX_WAKE,
            selectWakeWidgetPrimaryAction(
                activeWake = false,
                hasNextWake = true,
                nextWakeReady = false,
                pendingMorningCheckIn = true,
                planState = WakeWidgetPlanState.AVAILABLE,
            ),
        )
    }

    @Test
    fun `morning check in outranks optional preparation`() {
        assertEquals(
            WakeWidgetPrimaryAction.MORNING_CHECK_IN,
            selectWakeWidgetPrimaryAction(
                activeWake = false,
                hasNextWake = true,
                nextWakeReady = true,
                pendingMorningCheckIn = true,
                planState = WakeWidgetPlanState.AVAILABLE,
            ),
        )
    }

    @Test
    fun `unprepared tomorrow routes to preparation when wake is healthy`() {
        assertEquals(
            WakeWidgetPrimaryAction.PREPARE_TOMORROW,
            selectWakeWidgetPrimaryAction(
                activeWake = false,
                hasNextWake = true,
                nextWakeReady = true,
                pendingMorningCheckIn = false,
                planState = WakeWidgetPlanState.AVAILABLE,
            ),
        )
    }

    @Test
    fun `steady ready state opens home`() {
        assertEquals(
            WakeWidgetPrimaryAction.OPEN_HOME,
            selectWakeWidgetPrimaryAction(
                activeWake = false,
                hasNextWake = true,
                nextWakeReady = true,
                pendingMorningCheckIn = false,
                planState = WakeWidgetPlanState.READY,
            ),
        )
    }

    @Test
    fun `widget body opens the exact next alarm editor`() {
        assertEquals(
            WakeWidgetBodyTarget(
                destination = WakeWidgetDestination.ALARM_EDITOR,
                alarmId = "alarm-123",
            ),
            selectWakeWidgetBodyTarget(
                activeWake = false,
                nextAlarmId = "alarm-123",
                readiness = WakeWidgetReadiness.READY,
            ),
        )
    }

    @Test
    fun `widget body still edits an alarm when morning check in is the contextual action`() {
        assertEquals(
            WakeWidgetBodyTarget(
                destination = WakeWidgetDestination.ALARM_EDITOR,
                alarmId = "alarm-123",
            ),
            selectWakeWidgetBodyTarget(
                activeWake = false,
                nextAlarmId = "alarm-123",
                readiness = WakeWidgetReadiness.READY,
            ),
        )
    }

    @Test
    fun `widget body creates an alarm when none exists`() {
        assertEquals(
            WakeWidgetBodyTarget(WakeWidgetDestination.ALARM_EDITOR),
            selectWakeWidgetBodyTarget(
                activeWake = false,
                nextAlarmId = null,
                readiness = WakeWidgetReadiness.NONE,
            ),
        )
    }

    @Test
    fun `widget body reenters active wake instead of editing`() {
        assertEquals(
            WakeWidgetBodyTarget(WakeWidgetDestination.HOME),
            selectWakeWidgetBodyTarget(
                activeWake = true,
                nextAlarmId = "alarm-123",
                readiness = WakeWidgetReadiness.READY,
            ),
        )
    }

    @Test
    fun `unavailable widget body opens the app rather than creating phantom alarm`() {
        assertEquals(
            WakeWidgetBodyTarget(WakeWidgetDestination.HOME),
            selectWakeWidgetBodyTarget(
                activeWake = false,
                nextAlarmId = null,
                readiness = WakeWidgetReadiness.UNAVAILABLE,
            ),
        )
    }

    @Test
    fun `narrow 2x2 style bounds stay compact`() {
        assertEquals(
            WakeWidgetLayout.COMPACT,
            selectWakeWidgetLayout(widthDp = 110f, heightDp = 110f),
        )
    }

    @Test
    fun `wide 4x2 style bounds render the medium hero`() {
        assertEquals(
            WakeWidgetLayout.MEDIUM,
            selectWakeWidgetLayout(widthDp = 250f, heightDp = 110f),
        )
    }

    @Test
    fun `wide 4x4 style bounds render expanded content`() {
        assertEquals(
            WakeWidgetLayout.EXPANDED,
            selectWakeWidgetLayout(widthDp = 250f, heightDp = 250f),
        )
    }

    @Test
    fun `narrow tall bounds never become expanded`() {
        assertEquals(
            WakeWidgetLayout.COMPACT,
            selectWakeWidgetLayout(widthDp = 110f, heightDp = 300f),
        )
    }
}

package com.wakemyway.app.product

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.preparation.PrepareWakePlanScheduler
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.product.followup.WakeSafetyCheckScheduler
import com.wakemyway.app.product.history.WakeHistoryRepository
import com.wakemyway.app.product.learning.WakeLearningRepository
import com.wakemyway.app.voice.ConversationalAlfredState
import com.wakemyway.app.widget.WakeWidgetUpdater

sealed interface LocalWakeDataResetResult {
    data object Completed : LocalWakeDataResetResult
    data object ActiveWakeInProgress : LocalWakeDataResetResult
    data class Failed(val detail: String) : LocalWakeDataResetResult
}

/**
 * Privacy-facing owner for deleting WakeMyWay's local wake data.
 *
 * Alarm authority is removed first through [AlarmProductController]. Non-critical state is erased
 * only after no active execution or future critical schedule remains.
 */
class LocalWakeDataResetManager(context: Context) {
    private val appContext = context.applicationContext
    private val alarmKernel = AlarmKernel(appContext)
    private val alarmController = AlarmProductController(appContext)
    private val preferences = ConsumerPreferencesRepository(appContext)
    private val preparation = WakePreparationManager(appContext)
    private val history = WakeHistoryRepository(appContext)
    private val learning = WakeLearningRepository(appContext, history)
    private val timingTrace = WakeTimingTrace(appContext)

    @Synchronized
    fun erase(): LocalWakeDataResetResult = try {
        if (alarmKernel.health().activeOccurrence != null) {
            LocalWakeDataResetResult.ActiveWakeInProgress
        } else {
            val historyBeforeReset = history.list()

            alarmController.list().forEach { alarm ->
                check(alarmController.delete(alarm.id)) {
                    "Could not remove alarm ${alarm.id.value}."
                }
            }

            // Normal per-alarm cancellation intentionally retains disabled Direct-Boot slot
            // metadata. Privacy reset removes that residual critical schedule/policy state as well.
            alarmKernel.purgeAllScheduleStateForDataReset()

            check(alarmController.list().isEmpty()) {
                "Some alarm definitions still exist after reset."
            }
            check(alarmKernel.currentSchedules().isEmpty()) {
                "Some critical wake schedules still exist after reset."
            }

            historyBeforeReset.forEach { entry ->
                runCatching { WakeSafetyCheckScheduler.resolve(appContext, entry.occurrenceId) }
            }
            // Deferrable work is not retained user data. If WorkManager cleanup itself is
            // unavailable, deleting history/preparation still makes any orphaned worker harmless.
            runCatching { PrepareWakePlanScheduler.cancel(appContext) }
            preparation.clear()
            history.clear()
            learning.resetToDefault()
            preferences.replace(ConsumerPreferences())
            timingTrace.clearHistory()
            ConversationalAlfredState.clearProvisioningIfSupported(appContext)
            runCatching { NotificationManagerCompat.from(appContext).cancelAll() }
            WakeWidgetUpdater.request(appContext)

            LocalWakeDataResetResult.Completed
        }
    } catch (error: Throwable) {
        LocalWakeDataResetResult.Failed(
            detail = error.message ?: "WakeMyWay could not safely erase all local wake data.",
        )
    }
}

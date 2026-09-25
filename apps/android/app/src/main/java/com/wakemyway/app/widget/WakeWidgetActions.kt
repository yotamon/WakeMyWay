package com.wakemyway.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import com.wakemyway.app.product.followup.submitMorningSafetyCheck
import com.wakemyway.core.learning.WakeCalibrationOutcome
import com.wakemyway.core.schedule.WakeOccurrenceId

internal val MorningCheckInOccurrenceKey = ActionParameters.Key<String>("morning_check_in_occurrence")
internal val MorningCheckInOutcomeKey = ActionParameters.Key<String>("morning_check_in_outcome")

internal fun morningCheckInAction(
    occurrenceId: String,
    outcome: WakeCalibrationOutcome,
) = actionRunCallback<MorningCheckInAction>(
    actionParametersOf(
        MorningCheckInOccurrenceKey to occurrenceId,
        MorningCheckInOutcomeKey to outcome.name,
    ),
)

class MorningCheckInAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val occurrenceId = parameters[MorningCheckInOccurrenceKey]
            ?.takeIf(String::isNotBlank)
            ?.let(::WakeOccurrenceId)
            ?: return
        val outcome = parameters[MorningCheckInOutcomeKey]
            ?.let { raw -> WakeCalibrationOutcome.entries.firstOrNull { it.name == raw } }
            ?: return

        val appContext = context.applicationContext
        submitMorningSafetyCheck(appContext, occurrenceId, outcome)
        WakeMyWayWidget().update(appContext, glanceId)
    }
}

package com.wakemyway.app.preparation

import android.content.Context
import com.wakemyway.core.preparation.PreparedPlanValidation
import com.wakemyway.core.preparation.PreparedWakePlan
import com.wakemyway.core.preparation.PreparedWakePlanPreparer
import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.preparation.TomorrowContractId
import com.wakemyway.core.schedule.WakeOccurrenceId

/**
 * Deep application-facing owner for M6 private content.
 *
 * Callers save an intention or request wake-time content; they do not coordinate storage, plan
 * preparation, validation, or background refresh ordering themselves.
 */
class WakePreparationManager(
    context: Context,
    private val store: PrivateWakePreparationStore = PrivateWakePreparationStore(context),
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext

    fun saveAndPrepare(
        wakeOccurrenceId: WakeOccurrenceId,
        rawText: String,
        firstMove: String?,
    ): WakePreparationSnapshot {
        val normalizedText = rawText.trim()
        require(normalizedText.isNotBlank()) { "Tomorrow Contract text must not be blank" }
        val normalizedFirstMove = firstMove?.trim()?.takeIf { it.isNotBlank() }
        val now = nowEpochMillis().coerceAtLeast(1)
        val previous = runCatching { store.readContract() }.getOrNull()
        val continuesSameContract = previous?.wakeOccurrenceId == wakeOccurrenceId
        val contract = TomorrowContract(
            id = TomorrowContractId("contract:${wakeOccurrenceId.value}"),
            wakeOccurrenceId = wakeOccurrenceId,
            rawText = normalizedText,
            firstMove = normalizedFirstMove,
            revision = if (continuesSameContract) previous!!.revision + 1 else 1,
            createdAtEpochMillis = if (continuesSameContract) previous!!.createdAtEpochMillis else now,
            updatedAtEpochMillis = now,
        )

        store.writeContract(contract)
        val plan = PreparedWakePlanPreparer.prepare(contract, now)
        store.writePlan(plan)
        runCatching { PrepareWakePlanScheduler.enqueue(appContext) }
        return WakePreparationSnapshot(
            contract = contract,
            plan = plan,
            status = WakePreparationStatus.READY,
            detail = "Prepared locally for this wake occurrence.",
        )
    }

    /** Rebuild the latest stored contract. Intended for deferrable WorkManager execution. */
    fun prepareLatest(): Boolean {
        val contract = store.readContract() ?: return false
        val plan = PreparedWakePlanPreparer.prepare(
            contract = contract,
            preparedAtEpochMillis = nowEpochMillis().coerceAtLeast(1),
        )
        store.writePlan(plan)
        return true
    }

    fun snapshotFor(wakeOccurrenceId: WakeOccurrenceId): WakePreparationSnapshot {
        val contract = try {
            store.readContract()
        } catch (_: Throwable) {
            return WakePreparationSnapshot(
                contract = null,
                plan = null,
                status = WakePreparationStatus.PRIVATE_STATE_UNAVAILABLE,
                detail = "Private preparation state could not be read. Wake will use generic local content.",
            )
        }

        if (contract == null || contract.wakeOccurrenceId != wakeOccurrenceId) {
            return WakePreparationSnapshot(
                contract = null,
                plan = null,
                status = WakePreparationStatus.NO_CONTRACT,
                detail = "No Tomorrow Contract is attached to this wake occurrence.",
            )
        }

        val plan = try {
            store.readPlan()
        } catch (_: Throwable) {
            null
        }
        if (plan == null) {
            return WakePreparationSnapshot(
                contract = contract,
                plan = null,
                status = WakePreparationStatus.NOT_PREPARED,
                detail = "The intention is saved, but no valid prepared plan is available yet.",
            )
        }

        return when (PreparedWakePlanPreparer.validate(plan, contract)) {
            is PreparedPlanValidation.Valid -> WakePreparationSnapshot(
                contract = contract,
                plan = plan,
                status = WakePreparationStatus.READY,
                detail = "Prepared plan is valid and available offline.",
            )

            is PreparedPlanValidation.Invalid -> WakePreparationSnapshot(
                contract = contract,
                plan = null,
                status = WakePreparationStatus.INVALID_OR_STALE,
                detail = "Prepared plan is stale or invalid. Wake will use generic local content.",
            )
        }
    }

    fun loadForWake(wakeOccurrenceId: WakeOccurrenceId): WakeTimePreparedContent {
        val snapshot = snapshotFor(wakeOccurrenceId)
        val plan = snapshot.plan
        return if (snapshot.status == WakePreparationStatus.READY && plan != null) {
            WakeTimePreparedContent.Prepared(plan)
        } else {
            WakeTimePreparedContent.GenericFallback(
                lines = PreparedWakePlanPreparer.genericFallbackLines,
                reason = snapshot.status,
            )
        }
    }

    fun clear() {
        store.clearAll()
    }
}

enum class WakePreparationStatus {
    NO_CONTRACT,
    NOT_PREPARED,
    READY,
    INVALID_OR_STALE,
    PRIVATE_STATE_UNAVAILABLE,
}

data class WakePreparationSnapshot(
    val contract: TomorrowContract?,
    val plan: PreparedWakePlan?,
    val status: WakePreparationStatus,
    val detail: String,
)

sealed interface WakeTimePreparedContent {
    data class Prepared(val plan: PreparedWakePlan) : WakeTimePreparedContent

    data class GenericFallback(
        val lines: List<String>,
        val reason: WakePreparationStatus,
    ) : WakeTimePreparedContent
}

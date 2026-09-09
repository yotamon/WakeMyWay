package com.wakemyway.app.learning

import android.content.Context
import com.wakemyway.core.learning.LearnedWakeProfile
import com.wakemyway.core.learning.WakeCalibration
import com.wakemyway.core.learning.WakeFeedbackRating
import com.wakemyway.core.learning.WakeLearning
import com.wakemyway.core.learning.WakeLearningDecision
import com.wakemyway.core.learning.WakeLearningPolicy
import com.wakemyway.core.learning.WakeOutcomeId
import com.wakemyway.core.learning.WakeOutcomeRecord
import com.wakemyway.core.runtime.WakePolicy

class WakeLearningManager(
    context: Context,
    private val store: PrivateWakeLearningStore = PrivateWakeLearningStore(context),
    private val defaultPolicy: WakePolicy = WakePolicy(),
    private val learningPolicy: WakeLearningPolicy = WakeLearningPolicy(),
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    fun snapshot(): WakeLearningSnapshot = synchronized(LOCK) {
        val state = try {
            store.readState()
        } catch (_: Throwable) {
            return@synchronized fallbackSnapshot(
                status = WakeLearningStorageStatus.STORAGE_FALLBACK,
                detail = "Wake Learning state could not be read. New wake sessions use the stable default policy.",
            )
        }
        snapshotFrom(state)
    }

    fun activePolicy(): WakePolicy = snapshot().activePolicy

    fun recordOutcome(outcome: WakeOutcomeRecord): WakeLearningResult = synchronized(LOCK) {
        val read = runCatching { store.readState() }
        val recoveredFromStorageError = read.isFailure
        val state = read.getOrElse { StoredWakeLearningState() }
        recordLocked(state, outcome, recoveredFromStorageError)
    }

    fun applyCalibration(
        outcomeId: WakeOutcomeId,
        calibration: WakeCalibration,
        annoyance: WakeFeedbackRating? = null,
        agency: WakeFeedbackRating? = null,
    ): WakeLearningResult? = synchronized(LOCK) {
        val read = runCatching { store.readState() }
        val recoveredFromStorageError = read.isFailure
        val state = read.getOrElse { StoredWakeLearningState() }
        val existing = state.outcomes.firstOrNull { it.id == outcomeId } ?: return@synchronized null
        recordLocked(
            state = state,
            outcome = existing.copy(
                calibration = calibration,
                annoyance = annoyance ?: existing.annoyance,
                agency = agency ?: existing.agency,
            ),
            recoveredFromStorageError = recoveredFromStorageError,
        )
    }

    fun reset() = synchronized(LOCK) {
        store.clear()
    }

    private fun recordLocked(
        state: StoredWakeLearningState,
        outcome: WakeOutcomeRecord,
        recoveredFromStorageError: Boolean,
    ): WakeLearningResult {
        val validProfile = state.profile?.takeIf { profile -> isSupportedProfile(profile) }
        val currentPolicy = validProfile?.activePolicy ?: defaultPolicy
        val outcomes = (state.outcomes.filterNot { it.id == outcome.id } + outcome)
            .sortedWith(compareBy<WakeOutcomeRecord> { it.startedAtEpochMillis }.thenBy { it.id.value })
            .takeLast(PrivateWakeLearningStore.MAX_OUTCOMES)
        val decision = WakeLearning.evaluate(
            currentPolicy = currentPolicy,
            outcomes = outcomes,
            learningPolicy = learningPolicy,
        )
        val nextProfile = when (decision) {
            is WakeLearningDecision.PolicyUpdated -> LearnedWakeProfile(
                learningAlgorithmVersion = learningPolicy.version,
                activePolicy = decision.nextPolicy,
                sourceOutcomeIds = decision.consideredOutcomeIds,
                explanation = decision.explanation,
                updatedAtEpochMillis = nowEpochMillis().coerceAtLeast(1),
            )
            is WakeLearningDecision.NoChange -> validProfile
        }
        val nextState = StoredWakeLearningState(
            outcomes = outcomes,
            profile = nextProfile,
        )
        store.writeState(nextState)
        val snapshot = snapshotFrom(
            nextState,
            forcedStatus = if (recoveredFromStorageError) {
                WakeLearningStorageStatus.RECOVERED_DERIVED_STATE
            } else {
                null
            },
        )
        return WakeLearningResult(snapshot = snapshot, decision = decision)
    }

    private fun snapshotFrom(
        state: StoredWakeLearningState,
        forcedStatus: WakeLearningStorageStatus? = null,
    ): WakeLearningSnapshot {
        val supportedProfile = state.profile?.takeIf { profile -> isSupportedProfile(profile) }
        val unsupportedProfile = state.profile != null && supportedProfile == null
        val status = forcedStatus ?: when {
            unsupportedProfile -> WakeLearningStorageStatus.PROFILE_FALLBACK
            supportedProfile != null -> WakeLearningStorageStatus.LEARNED
            state.outcomes.isNotEmpty() -> WakeLearningStorageStatus.OBSERVING
            else -> WakeLearningStorageStatus.DEFAULT
        }
        val detail = when (status) {
            WakeLearningStorageStatus.DEFAULT -> "No learned policy yet. New wake sessions use the stable default policy."
            WakeLearningStorageStatus.OBSERVING -> "Wake Outcomes are accumulating; repeated evidence is required before policy changes."
            WakeLearningStorageStatus.LEARNED -> "A bounded learned policy is available for future Wake Sessions."
            WakeLearningStorageStatus.PROFILE_FALLBACK -> "Stored learned policy is unsupported. New wake sessions use the stable default policy."
            WakeLearningStorageStatus.STORAGE_FALLBACK -> "Wake Learning state is unavailable. New wake sessions use the stable default policy."
            WakeLearningStorageStatus.RECOVERED_DERIVED_STATE -> "Corrupt/unreadable derived learning state was replaced from safe defaults."
        }
        return WakeLearningSnapshot(
            activePolicy = supportedProfile?.activePolicy ?: defaultPolicy,
            outcomes = state.outcomes,
            profile = supportedProfile,
            status = status,
            detail = detail,
        )
    }

    private fun fallbackSnapshot(
        status: WakeLearningStorageStatus,
        detail: String,
    ): WakeLearningSnapshot = WakeLearningSnapshot(
        activePolicy = defaultPolicy,
        outcomes = emptyList(),
        profile = null,
        status = status,
        detail = detail,
    )

    private fun isSupportedProfile(profile: LearnedWakeProfile): Boolean =
        profile.profileVersion == SUPPORTED_PROFILE_VERSION &&
            profile.learningAlgorithmVersion == learningPolicy.version

    companion object {
        private const val SUPPORTED_PROFILE_VERSION = 1
        private val LOCK = Any()
    }
}

enum class WakeLearningStorageStatus {
    DEFAULT,
    OBSERVING,
    LEARNED,
    PROFILE_FALLBACK,
    STORAGE_FALLBACK,
    RECOVERED_DERIVED_STATE,
}

data class WakeLearningSnapshot(
    val activePolicy: WakePolicy,
    val outcomes: List<WakeOutcomeRecord>,
    val profile: LearnedWakeProfile?,
    val status: WakeLearningStorageStatus,
    val detail: String,
)

data class WakeLearningResult(
    val snapshot: WakeLearningSnapshot,
    val decision: WakeLearningDecision,
)

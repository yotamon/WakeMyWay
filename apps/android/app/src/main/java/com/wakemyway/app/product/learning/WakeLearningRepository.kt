package com.wakemyway.app.product.learning

import android.content.Context
import android.util.AtomicFile
import com.wakemyway.app.product.history.WakeHistoryBehaviorTimingOrigin
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryRepository
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.core.learning.WakeCalibration
import com.wakemyway.core.learning.WakeCalibrationOutcome
import com.wakemyway.core.learning.WakeLearning
import com.wakemyway.core.learning.WakeOutcomeSummary
import com.wakemyway.core.learning.WakePolicySnapshot
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration
import org.json.JSONObject

data class WakeLearningState(
    val policy: WakePolicy,
    val evidenceSessionCount: Int,
    val latestExplanation: String,
    val lastAdjustment: String?,
    val changedOnLatestRefresh: Boolean,
) {
    val hasLearnedAdjustment: Boolean
        get() = policy.version > WakePolicy().version || lastAdjustment != null
}

/**
 * Credential-protected, non-authoritative bridge from real Wake history into Wake Learning v0.
 *
 * The Alarm Kernel never reads this state. A learned policy is resolved once before an interactive
 * WakeRuntime session starts; corruption or unsupported state falls back to the stable default.
 */
class WakeLearningRepository(
    context: Context,
    private val historyRepository: WakeHistoryRepository = WakeHistoryRepository(context),
    private val learner: WakeLearning = WakeLearning(),
    fileName: String = DEFAULT_FILE_NAME,
) {
    private val atomicFile = AtomicFile(File(context.noBackupFilesDir, fileName))

    @Synchronized
    fun resolvePolicy(): WakePolicy {
        val stored = readStored() ?: return WakePolicy()
        return validatePersistedPolicy(stored.policy)
    }

    @Synchronized
    fun state(): WakeLearningState {
        val stored = readStored()
        val policy = stored?.policy?.let(::validatePersistedPolicy) ?: WakePolicy()
        val relevantCount = learningOutcomes(historyRepository.list())
            .count { it.policyVersion == policy.version }
        return WakeLearningState(
            policy = policy,
            evidenceSessionCount = stored?.evidenceSessionCount ?: relevantCount,
            latestExplanation = stored?.latestExplanation
                ?: "WakeMyWay is using its stable starting strategy while it gathers real morning evidence.",
            lastAdjustment = stored?.lastAdjustment,
            changedOnLatestRefresh = stored?.changedOnLatestRefresh ?: false,
        )
    }

    /**
     * Re-derives the next policy from immutable local history.
     *
     * This is best-effort product state. Any failure leaves the last valid learned policy intact and
     * can never affect an active alarm or terminal Stop/Snooze transaction.
     */
    @Synchronized
    fun refresh(): WakeLearningState {
        val previous = readStored()
        val currentPolicy = previous?.policy?.let(::validatePersistedPolicy) ?: WakePolicy()
        val outcomes = learningOutcomes(historyRepository.list())
        val decision = learner.derive(currentPolicy, outcomes)
        val nextPolicy = decision.snapshot.policy
        val lastAdjustment = if (decision.changed) {
            consumerAdjustment(decision.explanation.summary)
        } else {
            previous?.lastAdjustment
        }
        val stored = StoredState(
            policy = nextPolicy,
            evidenceSessionCount = decision.explanation.relevantSessionCount,
            latestExplanation = decision.explanation.summary,
            lastAdjustment = lastAdjustment,
            changedOnLatestRefresh = decision.changed,
        )
        writeStored(stored)
        return WakeLearningState(
            policy = nextPolicy,
            evidenceSessionCount = stored.evidenceSessionCount,
            latestExplanation = stored.latestExplanation,
            lastAdjustment = stored.lastAdjustment,
            changedOnLatestRefresh = stored.changedOnLatestRefresh,
        )
    }

    @Synchronized
    fun submitCalibration(
        occurrenceId: WakeOccurrenceId,
        outcome: WakeCalibrationOutcome,
    ): WakeLearningState {
        historyRepository.attachCalibration(
            occurrenceId = occurrenceId,
            calibration = WakeCalibration(outcome),
        )
        return refresh()
    }

    /**
     * Resets only the learned-policy snapshot.
     *
     * Durable Wake history/calibration remains intact so founder debugging and future derivation can
     * still explain what happened. The next interactive session therefore receives the stable
     * default policy until a later explicit refresh derives another bounded policy.
     */
    @Synchronized
    fun resetToDefault(): WakeLearningState {
        atomicFile.delete()
        return state()
    }

    private fun validatePersistedPolicy(policy: WakePolicy): WakePolicy {
        val selfSnapshot = WakePolicySnapshot(
            sourcePolicy = policy,
            policy = policy,
            sourceSessionIds = emptyList(),
            changes = emptyList(),
        )
        return learner.resolveLearnedPolicy(
            candidate = selfSnapshot,
            defaultPolicy = WakePolicy(),
        ).policy
    }

    private fun learningOutcomes(entries: List<WakeHistoryEntry>): List<WakeOutcomeSummary> =
        entries.mapNotNull(::toOutcome)

    private fun toOutcome(entry: WakeHistoryEntry): WakeOutcomeSummary? {
        val behavior = entry.behavior ?: return null
        if (entry.behaviorTimingOrigin != WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START) {
            return null
        }
        val activation = behavior.timeToActivationCompletion
        return WakeOutcomeSummary(
            sessionId = entry.sessionId,
            policyVersion = behavior.policyVersion,
            activationCompleted = activation != null,
            metActivationWindow = activation != null && activation <= ACTIVATION_WINDOW,
            timeToFirstEngagement = behavior.timeToFirstEngagement,
            timeToMeaningfulMovement = behavior.timeToMeaningfulMovement,
            timeToActivationCompletion = activation,
            snoozeCount = if (entry.terminalReason == WakeHistoryTerminalReason.SNOOZED) 1 else 0,
            maxInterventionDepth = behavior.maxInterventionDepth,
            finishReason = when (entry.terminalReason) {
                WakeHistoryTerminalReason.COMPLETED -> WakeOutcome.COMPLETED
                WakeHistoryTerminalReason.STOPPED -> WakeOutcome.STOPPED
                WakeHistoryTerminalReason.SNOOZED -> WakeOutcome.SNOOZED
                WakeHistoryTerminalReason.UNRECOVERABLE -> WakeOutcome.UNRECOVERABLE
            },
            calibration = entry.calibration,
        )
    }

    private fun consumerAdjustment(summary: String): String = when {
        "activationThreshold" in summary || "Activation Completion overestimated" in summary ->
            "WakeMyWay made the activation check a little stronger for future mornings."
        "additional bounded escalation level" in summary ->
            "WakeMyWay can go one step further when you are not moving yet."
        "reduce maximum intervention depth" in summary ->
            "WakeMyWay will use a little less intervention when your recent mornings already worked."
        else -> "WakeMyWay made one small, bounded adjustment for future mornings."
    }

    private fun readStored(): StoredState? {
        val text = try {
            atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            return null
        } catch (_: Throwable) {
            return null
        }
        if (text.isBlank()) return null
        return runCatching {
            val root = JSONObject(text)
            require(root.getInt(KEY_SCHEMA_VERSION) == SCHEMA_VERSION)
            StoredState(
                policy = decodePolicy(root.getJSONObject(KEY_POLICY)),
                evidenceSessionCount = root.getInt(KEY_EVIDENCE_SESSION_COUNT),
                latestExplanation = root.getString(KEY_LATEST_EXPLANATION),
                lastAdjustment = root.optString(KEY_LAST_ADJUSTMENT)
                    .takeIf(String::isNotBlank),
                changedOnLatestRefresh = root.optBoolean(KEY_CHANGED_ON_LATEST_REFRESH, false),
            )
        }.getOrNull()
    }

    private fun writeStored(state: StoredState) {
        val root = JSONObject().apply {
            put(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            put(KEY_POLICY, encodePolicy(state.policy))
            put(KEY_EVIDENCE_SESSION_COUNT, state.evidenceSessionCount)
            put(KEY_LATEST_EXPLANATION, state.latestExplanation)
            state.lastAdjustment?.let { put(KEY_LAST_ADJUSTMENT, it) }
            put(KEY_CHANGED_ON_LATEST_REFRESH, state.changedOnLatestRefresh)
        }
        val output = atomicFile.startWrite()
        try {
            output.write(root.toString().toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun encodePolicy(policy: WakePolicy): JSONObject = JSONObject().apply {
        put(KEY_VERSION, policy.version)
        put(KEY_ACTIVATION_THRESHOLD, policy.activationThreshold)
        put(KEY_MEANINGFUL_INTERACTION_WEIGHT, policy.meaningfulInteractionWeight)
        put(KEY_COHERENT_VOICE_WEIGHT, policy.coherentVoiceWeight)
        put(KEY_DEVICE_PICKUP_WEIGHT, policy.devicePickupWeight)
        put(KEY_ORIENTATION_CHANGE_WEIGHT, policy.orientationChangeWeight)
        put(KEY_SUSTAINED_MOVEMENT_WEIGHT, policy.sustainedMovementWeight)
        put(KEY_MAX_ESCALATION_LEVEL, policy.maxEscalationLevel)
        put(KEY_DEFAULT_SNOOZE_MILLIS, policy.defaultSnoozeDuration.toMillis())
        put(KEY_REMEMBERED_INPUT_LIMIT, policy.rememberedInputLimit)
    }

    private fun decodePolicy(json: JSONObject): WakePolicy = WakePolicy(
        version = json.getInt(KEY_VERSION),
        activationThreshold = json.getInt(KEY_ACTIVATION_THRESHOLD),
        meaningfulInteractionWeight = json.getInt(KEY_MEANINGFUL_INTERACTION_WEIGHT),
        coherentVoiceWeight = json.getInt(KEY_COHERENT_VOICE_WEIGHT),
        devicePickupWeight = json.getInt(KEY_DEVICE_PICKUP_WEIGHT),
        orientationChangeWeight = json.getInt(KEY_ORIENTATION_CHANGE_WEIGHT),
        sustainedMovementWeight = json.getInt(KEY_SUSTAINED_MOVEMENT_WEIGHT),
        maxEscalationLevel = json.getInt(KEY_MAX_ESCALATION_LEVEL),
        defaultSnoozeDuration = Duration.ofMillis(json.getLong(KEY_DEFAULT_SNOOZE_MILLIS)),
        rememberedInputLimit = json.getInt(KEY_REMEMBERED_INPUT_LIMIT),
    )

    private data class StoredState(
        val policy: WakePolicy,
        val evidenceSessionCount: Int,
        val latestExplanation: String,
        val lastAdjustment: String?,
        val changedOnLatestRefresh: Boolean,
    )

    private companion object {
        const val DEFAULT_FILE_NAME = "wake-learning-v1.json"
        const val SCHEMA_VERSION = 1
        val ACTIVATION_WINDOW: Duration = Duration.ofMinutes(2)

        const val KEY_SCHEMA_VERSION = "schemaVersion"
        const val KEY_POLICY = "policy"
        const val KEY_EVIDENCE_SESSION_COUNT = "evidenceSessionCount"
        const val KEY_LATEST_EXPLANATION = "latestExplanation"
        const val KEY_LAST_ADJUSTMENT = "lastAdjustment"
        const val KEY_CHANGED_ON_LATEST_REFRESH = "changedOnLatestRefresh"
        const val KEY_VERSION = "version"
        const val KEY_ACTIVATION_THRESHOLD = "activationThreshold"
        const val KEY_MEANINGFUL_INTERACTION_WEIGHT = "meaningfulInteractionWeight"
        const val KEY_COHERENT_VOICE_WEIGHT = "coherentVoiceWeight"
        const val KEY_DEVICE_PICKUP_WEIGHT = "devicePickupWeight"
        const val KEY_ORIENTATION_CHANGE_WEIGHT = "orientationChangeWeight"
        const val KEY_SUSTAINED_MOVEMENT_WEIGHT = "sustainedMovementWeight"
        const val KEY_MAX_ESCALATION_LEVEL = "maxEscalationLevel"
        const val KEY_DEFAULT_SNOOZE_MILLIS = "defaultSnoozeMillis"
        const val KEY_REMEMBERED_INPUT_LIMIT = "rememberedInputLimit"
    }
}

package com.wakemyway.app.product.sync

import android.content.Context
import com.wakemyway.app.product.AlarmProductController
import com.wakemyway.app.product.AppAppearance
import com.wakemyway.app.product.ConsumerPreferences
import com.wakemyway.app.product.ConsumerPreferencesRepository
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import java.time.Clock
import java.time.Instant

/**
 * Account-scoped cloud seam for optional backup/migration.
 *
 * The eventual adapter may use the Wake API backed by Neon Auth/PostgreSQL, but this contract
 * deliberately knows nothing about a provider. Implementations must already be scoped to the
 * authenticated installation/account session supplied by the future account layer.
 *
 * Calls are synchronous by contract so the failure boundary is explicit. A network adapter must
 * invoke them from an appropriate worker/dispatcher; no alarm-critical component calls this API.
 */
interface ConsumerSyncGateway {
    fun upload(snapshot: ConsumerSyncSnapshot)
    fun download(): ConsumerSyncSnapshot?
}

/** Normal credential-protected product state that may participate in explicit migration. */
data class ConsumerSyncLocalState(
    val preferences: ConsumerPreferences,
    val alarms: List<AlarmDefinition>,
)

/**
 * Deliberately omits device-local onboarding state and all private Tomorrow Contract text.
 *
 * These values are convenience/default preferences only. They are never alarm execution authority.
 */
data class ConsumerSyncPreferences(
    val displayName: String?,
    val defaultSoundId: WakeSoundId,
    val defaultVoiceCheckInEnabled: Boolean,
    val defaultVoiceStyle: VoiceStyle,
    val defaultSnoozeMinutes: Int,
    val defaultFirstMove: String?,
    val appearance: AppAppearance,
) {
    companion object {
        fun from(local: ConsumerPreferences): ConsumerSyncPreferences = ConsumerSyncPreferences(
            displayName = local.displayName,
            defaultSoundId = local.defaultSoundId,
            defaultVoiceCheckInEnabled = local.defaultVoiceCheckInEnabled,
            defaultVoiceStyle = local.defaultVoiceStyle,
            defaultSnoozeMinutes = local.defaultSnoozeMinutes,
            defaultFirstMove = local.defaultFirstMove,
            appearance = local.appearance,
        )
    }

    fun applyTo(local: ConsumerPreferences): ConsumerPreferences = local.copy(
        // onboardingCompleted is intentionally preserved from the current device.
        displayName = displayName,
        defaultSoundId = defaultSoundId,
        defaultVoiceCheckInEnabled = defaultVoiceCheckInEnabled,
        defaultVoiceStyle = defaultVoiceStyle,
        defaultSnoozeMinutes = defaultSnoozeMinutes,
        defaultFirstMove = defaultFirstMove,
        appearance = appearance,
    )
}

/**
 * Backup payload containing consumer intent only.
 *
 * AlarmDefinition is rich normal product state, not the Direct-Boot Critical Wake Snapshot. The
 * payload contains no next-occurrence authority, AlarmManager registration, active Wake Session,
 * Stop/Snooze state, private Tomorrow Contract text, raw audio or transcript data.
 */
data class ConsumerSyncSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val generatedAt: Instant,
    val preferences: ConsumerSyncPreferences,
    val alarms: List<AlarmDefinition>,
) {
    init {
        require(schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported consumer sync snapshot schema version: $schemaVersion"
        }
        require(alarms.map { it.id }.distinct().size == alarms.size) {
            "Consumer sync snapshot contains duplicate alarm ids"
        }
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

enum class ConsumerRestoreMode {
    /** Explicit migration onto a fresh device: restore convenience preferences plus alarm intent. */
    MIGRATE_TO_FRESH_DEVICE,

    /** Existing device wins: keep all current preferences and only offer remote-only alarms. */
    MERGE_INTO_EXISTING,
}

data class ConsumerMigrationPlan(
    val targetPreferences: ConsumerPreferences,
    /** Remote-only alarms normalized to disabled local product state. */
    val alarmsToImport: List<AlarmDefinition>,
    /** Remote ids deliberately ignored because local product state already owns them. */
    val preservedLocalAlarmIds: Set<AlarmDefinitionId>,
) {
    init {
        require(alarmsToImport.all { !it.enabled }) {
            "Cloud migration may never import an enabled alarm"
        }
        require(alarmsToImport.map { it.id }.distinct().size == alarmsToImport.size) {
            "Migration plan contains duplicate alarm ids"
        }
        require(alarmsToImport.none { it.id in preservedLocalAlarmIds }) {
            "Migration plan may not replace a local alarm"
        }
    }
}

data class ConsumerMigrationReport(
    val importedAlarmIds: Set<AlarmDefinitionId>,
    val skippedAlarmIds: Set<AlarmDefinitionId>,
)

/**
 * Pure policy for backup and conservative migration.
 *
 * This is intentionally not live two-way synchronization. The cloud is a backup/migration copy of
 * product intent. Local alarm execution stays authoritative, conflicts resolve to local state, and
 * remote-only alarms require an explicit local enable action after import.
 */
object ConsumerSyncPolicy {
    fun createSnapshot(
        local: ConsumerSyncLocalState,
        generatedAt: Instant,
    ): ConsumerSyncSnapshot = ConsumerSyncSnapshot(
        generatedAt = generatedAt,
        preferences = ConsumerSyncPreferences.from(local.preferences),
        alarms = local.alarms.sortedBy { it.id.value },
    )

    fun planMigration(
        local: ConsumerSyncLocalState,
        remote: ConsumerSyncSnapshot,
        mode: ConsumerRestoreMode,
        importedAt: Instant,
    ): ConsumerMigrationPlan {
        val localIds = local.alarms.mapTo(linkedSetOf()) { it.id }
        val remoteOnly = remote.alarms
            .asSequence()
            .filterNot { it.id in localIds }
            .map { it.asSafeImportedAlarm(importedAt) }
            .sortedBy { it.id.value }
            .toList()

        val targetPreferences = when (mode) {
            ConsumerRestoreMode.MIGRATE_TO_FRESH_DEVICE -> remote.preferences.applyTo(local.preferences)
            ConsumerRestoreMode.MERGE_INTO_EXISTING -> local.preferences
        }

        return ConsumerMigrationPlan(
            targetPreferences = targetPreferences,
            alarmsToImport = remoteOnly,
            preservedLocalAlarmIds = remote.alarms
                .asSequence()
                .map { it.id }
                .filter { it in localIds }
                .toCollection(linkedSetOf()),
        )
    }

    private fun AlarmDefinition.asSafeImportedAlarm(importedAt: Instant): AlarmDefinition {
        val safeUpdatedAt = maxOf(createdAt, updatedAt, importedAt)
        val safeRevision = revision.coerceAtMost(Long.MAX_VALUE - 1) + 1
        return copy(
            enabled = false,
            revision = safeRevision,
            updatedAt = safeUpdatedAt,
        )
    }
}

/**
 * Local migration edge. Implementations may mutate normal product state only after a remote backup
 * has been downloaded and converted to a conservative [ConsumerMigrationPlan].
 */
interface ConsumerSyncLocalStore {
    fun read(): ConsumerSyncLocalState
    fun applyMigration(plan: ConsumerMigrationPlan): ConsumerMigrationReport
}

/**
 * Android local-state adapter.
 *
 * Existing alarms are never rewritten. Every import is disabled and flows through
 * [AlarmProductController]. Before saving, the adapter rechecks both rich product state and the
 * Alarm Kernel slot so a cloud id can never cancel/replace a locally committed wake even under a
 * stale migration plan. Preference changes are non-critical and are compensated if import fails.
 */
class AndroidConsumerSyncLocalStore(
    context: Context,
    private val alarmController: AlarmProductController = AlarmProductController(context),
    private val preferencesRepository: ConsumerPreferencesRepository = ConsumerPreferencesRepository(context),
) : ConsumerSyncLocalStore {
    override fun read(): ConsumerSyncLocalState = ConsumerSyncLocalState(
        preferences = preferencesRepository.get(),
        alarms = alarmController.list(),
    )

    @Synchronized
    override fun applyMigration(plan: ConsumerMigrationPlan): ConsumerMigrationReport {
        val beforePreferences = preferencesRepository.get()
        val imported = linkedSetOf<AlarmDefinitionId>()
        val skipped = linkedSetOf<AlarmDefinitionId>()

        try {
            plan.alarmsToImport.forEach { candidate ->
                require(!candidate.enabled) { "Migration import must remain disabled" }

                val localProductExists = alarmController.get(candidate.id) != null
                val criticalSlotExists = alarmController.health(candidate.id) != null
                if (localProductExists || criticalSlotExists) {
                    skipped += candidate.id
                } else {
                    alarmController.save(candidate)
                    imported += candidate.id
                }
            }
            preferencesRepository.replace(plan.targetPreferences)
        } catch (error: Throwable) {
            imported.toList().asReversed().forEach { id ->
                runCatching { alarmController.delete(id) }
            }
            runCatching { preferencesRepository.replace(beforePreferences) }
            throw error
        }

        return ConsumerMigrationReport(
            importedAlarmIds = imported,
            skippedAlarmIds = skipped + plan.preservedLocalAlarmIds,
        )
    }
}

enum class ConsumerSyncFailureStage {
    BACKUP_UPLOAD,
    RESTORE_DOWNLOAD,
    RESTORE_APPLY,
}

sealed interface ConsumerSyncResult {
    data class BackupUploaded(
        val generatedAt: Instant,
        val alarmCount: Int,
    ) : ConsumerSyncResult

    data class MigrationApplied(
        val importedAlarmIds: Set<AlarmDefinitionId>,
        val skippedAlarmIds: Set<AlarmDefinitionId>,
    ) : ConsumerSyncResult

    data object NoRemoteBackup : ConsumerSyncResult

    data class Failed(
        val stage: ConsumerSyncFailureStage,
        val cause: Throwable,
    ) : ConsumerSyncResult
}

/**
 * Failure-domain coordinator for optional backup/restore.
 *
 * Download happens before any local read/apply. Therefore an account/cloud outage during restore
 * cannot mutate product state. Backup only reads local state before upload. Nothing in this class
 * is referenced by Alarm Kernel, Wake Runtime, WakeActivity or terminal Stop/Snooze handling.
 */
class ConsumerSyncCoordinator(
    private val gateway: ConsumerSyncGateway,
    private val localStore: ConsumerSyncLocalStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun backup(): ConsumerSyncResult {
        val local = localStore.read()
        val snapshot = ConsumerSyncPolicy.createSnapshot(local, Instant.now(clock))
        return runCatching { gateway.upload(snapshot) }
            .fold(
                onSuccess = {
                    ConsumerSyncResult.BackupUploaded(
                        generatedAt = snapshot.generatedAt,
                        alarmCount = snapshot.alarms.size,
                    )
                },
                onFailure = { ConsumerSyncResult.Failed(ConsumerSyncFailureStage.BACKUP_UPLOAD, it) },
            )
    }

    fun restore(mode: ConsumerRestoreMode): ConsumerSyncResult {
        val remote = try {
            gateway.download()
        } catch (error: Throwable) {
            return ConsumerSyncResult.Failed(ConsumerSyncFailureStage.RESTORE_DOWNLOAD, error)
        } ?: return ConsumerSyncResult.NoRemoteBackup

        val local = localStore.read()
        val plan = ConsumerSyncPolicy.planMigration(
            local = local,
            remote = remote,
            mode = mode,
            importedAt = Instant.now(clock),
        )
        return runCatching { localStore.applyMigration(plan) }
            .fold(
                onSuccess = { report ->
                    ConsumerSyncResult.MigrationApplied(
                        importedAlarmIds = report.importedAlarmIds,
                        skippedAlarmIds = report.skippedAlarmIds,
                    )
                },
                onFailure = { ConsumerSyncResult.Failed(ConsumerSyncFailureStage.RESTORE_APPLY, it) },
            )
    }
}

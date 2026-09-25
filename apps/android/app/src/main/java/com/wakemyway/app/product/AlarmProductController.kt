package com.wakemyway.app.product

import android.content.Context
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.AlarmScheduleHealth
import com.wakemyway.app.alarm.CriticalWakePolicy
import com.wakemyway.app.widget.WakeWidgetUpdater
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmScheduleCompiler
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.alarm.SnoozePolicy
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Clock
import java.time.Instant

/**
 * Product boundary between consumer alarm definitions and the trust-critical Alarm Kernel.
 *
 * UI code must mutate alarms through this controller rather than writing [AlarmDefinitionRepository]
 * and [AlarmKernel] independently. Product persistence is rolled back when critical scheduling fails,
 * and the previous critical slot is compensated where possible so product metadata and Direct Boot
 * authority do not knowingly diverge after a partial Android-registration failure.
 */
class AlarmProductController(
    context: Context,
    private val repository: AlarmDefinitionRepository = AlarmDefinitionRepository(context),
    private val kernel: AlarmKernel = AlarmKernel(context),
    private val compiler: AlarmScheduleCompiler = AlarmScheduleCompiler(),
    private val clock: Clock = Clock.systemUTC(),
) {
    private val appContext = context.applicationContext

    init {
        migrateExistingKernelSchedulesIfNeeded()
    }

    @Synchronized
    fun list(): List<AlarmDefinition> = repository.list()
        .sortedWith(compareByDescending<AlarmDefinition> { it.enabled }.thenBy { nextInstant(it) ?: Instant.MAX })

    @Synchronized
    fun get(id: AlarmDefinitionId): AlarmDefinition? = repository.get(id)

    fun health(id: AlarmDefinitionId): AlarmScheduleHealth? =
        kernel.health(WakeScheduleId(id.value))

    /**
     * Reconciles rich product truth back into the Alarm Kernel before asking the Kernel to repair
     * Android registrations.
     *
     * Package replacement or a temporary capability failure may intentionally remove an OS
     * registration without deleting the user's AlarmDefinition. Normal app startup must therefore
     * be able to recover a missing/disabled critical slot without requiring the user to edit and
     * save the alarm again.
     */
    @Synchronized
    fun reconcile(): com.wakemyway.app.alarm.AlarmHealth {
        val definitions = repository.list()
        val activeScheduleId = kernel.activeOccurrence()?.wakeScheduleId
        val currentSchedules = kernel.currentSchedules().associateBy { it.id }
        val productScheduleIds = definitions.mapTo(linkedSetOf()) { WakeScheduleId(it.id.value) }

        currentSchedules.keys
            .asSequence()
            .filter { scheduleId -> scheduleId !in productScheduleIds && scheduleId != activeScheduleId }
            .forEach(kernel::cancelSchedule)

        definitions.forEach { definition ->
            val scheduleId = WakeScheduleId(definition.id.value)
            if (scheduleId == activeScheduleId) return@forEach

            val slotHealth = kernel.health(scheduleId)
            if (!definition.enabled) {
                if (slotHealth?.enabled == true) kernel.cancelSchedule(scheduleId)
                return@forEach
            }

            runCatching {
                val expectedSchedule = compiler.compile(definition)
                val expectedPolicy = CriticalWakePolicy.from(definition)
                val needsRepair =
                    currentSchedules[scheduleId] != expectedSchedule ||
                        kernel.policy(scheduleId) != expectedPolicy ||
                        slotHealth?.enabled != true ||
                        slotHealth.nextOccurrence == null

                if (needsRepair) {
                    kernel.commitSchedule(expectedSchedule, expectedPolicy)
                }
            }
        }

        return kernel.reconcile()
    }

    /**
     * Persist one rich alarm and synchronize only its critical schedule slot.
     *
     * Alarm Kernel writes durable Direct Boot authority before touching AlarmManager. If Android
     * registration fails after that durable write, restoring only product JSON would create a split
     * brain. This method therefore compensates the affected kernel slot back to its previous alarm
     * definition, or disables a newly-created slot, before surfacing the original failure.
     */
    @Synchronized
    fun save(definition: AlarmDefinition): AlarmDefinition {
        val before = repository.list()
        val previous = before.firstOrNull { it.id == definition.id }
        repository.upsert(definition)
        return try {
            synchronizeKernel(definition)
            WakeWidgetUpdater.request(appContext)
            definition
        } catch (error: Throwable) {
            repository.replaceAll(before)
            compensateKernelAfterFailedSave(
                failedDefinition = definition,
                previousDefinition = previous,
            )
            throw error
        }
    }

    @Synchronized
    fun setEnabled(
        id: AlarmDefinitionId,
        enabled: Boolean,
    ): AlarmDefinition {
        val existing = requireNotNull(repository.get(id)) { "Unknown alarm ${id.value}" }
        if (existing.enabled == enabled) return existing
        val now = Instant.now(clock)
        return save(
            existing.copy(
                enabled = enabled,
                revision = existing.revision + 1,
                updatedAt = now,
            ),
        )
    }

    /** Delete product metadata and disable only this alarm's critical slot. */
    @Synchronized
    fun delete(id: AlarmDefinitionId): Boolean {
        val before = repository.list()
        val existing = before.firstOrNull { it.id == id } ?: return false
        kernel.cancelSchedule(WakeScheduleId(id.value))
        return try {
            repository.delete(id).also { deleted ->
                if (deleted) WakeWidgetUpdater.request(appContext)
            }
        } catch (error: Throwable) {
            repository.replaceAll(before)
            if (existing.enabled) {
                runCatching { synchronizeKernel(existing) }
            }
            throw error
        }
    }

    private fun synchronizeKernel(definition: AlarmDefinition) {
        if (definition.enabled) {
            kernel.commitSchedule(
                schedule = compiler.compile(definition),
                policy = CriticalWakePolicy.from(definition),
            )
        } else {
            kernel.cancelSchedule(WakeScheduleId(definition.id.value))
        }
    }

    /**
     * Best-effort compensation for the single affected slot.
     *
     * We intentionally preserve the original exception as the caller-visible failure. Even if
     * AlarmManager remains unavailable, [AlarmKernel.commitSchedule] writes the previous durable
     * schedule before attempting registration, so a failed compensation still trends toward the
     * previous product truth rather than retaining the attempted replacement.
     */
    private fun compensateKernelAfterFailedSave(
        failedDefinition: AlarmDefinition,
        previousDefinition: AlarmDefinition?,
    ) {
        runCatching {
            when {
                previousDefinition == null || !previousDefinition.enabled ->
                    kernel.cancelSchedule(WakeScheduleId(failedDefinition.id.value))

                else -> synchronizeKernel(previousDefinition)
            }
        }
    }

    private fun nextInstant(alarm: AlarmDefinition): Instant? =
        health(alarm.id)?.nextOccurrence?.scheduledAt?.toInstant()

    /**
     * Phase-B upgrade bridge.
     *
     * Builds before the consumer alarm repository could already own valid critical schedules. On the
     * first upgraded launch, mirror those schedules into product storage with conservative defaults.
     * This does not commit or replace any kernel schedule, so an already-set wake keeps exactly the
     * occurrence authority it had before the upgrade.
     */
    private fun migrateExistingKernelSchedulesIfNeeded() {
        if (repository.list().isNotEmpty()) return
        val schedules = kernel.currentSchedules()
        if (schedules.isEmpty()) return
        val now = Instant.now(clock)
        val nextBySchedule = kernel.nextOccurrences().associateBy { it.wakeScheduleId }
        val migrated = schedules.mapNotNull { schedule ->
            schedule.toAlarmDefinition(
                now = now,
                fallbackOneShotDate = nextBySchedule[schedule.id]?.scheduledAt?.toLocalDate(),
                policy = kernel.policy(schedule.id) ?: CriticalWakePolicy.DEFAULT,
            )
        }
        if (migrated.isNotEmpty()) repository.replaceAll(migrated)
    }

    private fun WakeSchedule.toAlarmDefinition(
        now: Instant,
        fallbackOneShotDate: java.time.LocalDate?,
        policy: CriticalWakePolicy,
    ): AlarmDefinition? {
        val pattern = when (completionPolicy) {
            WakeCompletionPolicy.RECURRING -> {
                val uniqueTimes = timesByDay.values.toSet()
                if (timesByDay.isEmpty() || uniqueTimes.size != 1) return null
                AlarmSchedulePattern.Weekly(
                    days = timesByDay.keys,
                    time = uniqueTimes.first(),
                )
            }

            WakeCompletionPolicy.ONE_SHOT -> {
                val date = oneShotDate ?: fallbackOneShotDate ?: return null
                val time = timesByDay[date.dayOfWeek] ?: timesByDay.values.firstOrNull() ?: return null
                AlarmSchedulePattern.OneShot(date = date, time = time)
            }
        }
        return AlarmDefinition(
            id = AlarmDefinitionId(id.value),
            label = "",
            enabled = true,
            zoneId = zoneId,
            schedule = pattern,
            soundId = policy.soundId,
            voiceCheckInEnabled = policy.voiceCheckInEnabled,
            characterId = policy.characterId,
            voiceStyle = policy.voiceStyle,
            snoozePolicy = SnoozePolicy(
                enabled = policy.snoozeEnabled,
                duration = policy.snoozeDuration,
            ),
            tomorrowContractMode = TomorrowContractMode.OPTIONAL,
            revision = revision.coerceAtLeast(1),
            createdAt = now,
            updatedAt = now,
        )
    }
}

package com.wakemyway.app.product

import android.content.Context
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.AlarmScheduleHealth
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmScheduleCompiler
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.SnoozePolicy
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
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
 * so the app never knowingly presents an enabled alarm that it failed to hand to the kernel.
 */
class AlarmProductController(
    context: Context,
    private val repository: AlarmDefinitionRepository = AlarmDefinitionRepository(context),
    private val kernel: AlarmKernel = AlarmKernel(context),
    private val compiler: AlarmScheduleCompiler = AlarmScheduleCompiler(),
    private val clock: Clock = Clock.systemUTC(),
) {
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

    fun reconcile() = kernel.reconcile()

    /**
     * Persist one rich alarm and synchronize only its critical schedule slot.
     *
     * The previous product document is restored if scheduling throws. The Alarm Kernel itself owns
     * its durable-before-OS-registration guarantees, so this layer never attempts to rewrite kernel
     * internals directly.
     */
    @Synchronized
    fun save(definition: AlarmDefinition): AlarmDefinition {
        val before = repository.list()
        repository.upsert(definition)
        return try {
            if (definition.enabled) {
                kernel.commitSchedule(compiler.compile(definition))
            } else {
                kernel.cancelSchedule(WakeScheduleId(definition.id.value))
            }
            definition
        } catch (error: Throwable) {
            repository.replaceAll(before)
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
            repository.delete(id)
        } catch (error: Throwable) {
            repository.replaceAll(before)
            if (existing.enabled) {
                runCatching { kernel.commitSchedule(compiler.compile(existing)) }
            }
            throw error
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
            )
        }
        if (migrated.isNotEmpty()) repository.replaceAll(migrated)
    }

    private fun WakeSchedule.toAlarmDefinition(
        now: Instant,
        fallbackOneShotDate: java.time.LocalDate?,
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
            soundId = WakeSoundId.MORNING_LIGHT,
            voiceCheckInEnabled = true,
            characterId = CharacterId.ALFRED,
            voiceStyle = VoiceStyle.DEFAULT,
            snoozePolicy = SnoozePolicy(),
            tomorrowContractMode = TomorrowContractMode.OPTIONAL,
            revision = revision.coerceAtLeast(1),
            createdAt = now,
            updatedAt = now,
        )
    }
}
package com.wakemyway.app.alarm

import android.content.Context
import com.wakemyway.core.schedule.NextWakeOccurrenceResolver
import com.wakemyway.core.schedule.SnoozeOccurrenceFactory
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeSchedule
import java.time.Clock
import java.time.Duration
import java.time.Instant

class AlarmKernel(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
    criticalStateFileName: String = CriticalWakeStore.DEFAULT_FILE_NAME,
) {
    private val appContext = context.applicationContext
    private val store = CriticalWakeStore(appContext, criticalStateFileName)
    private val registrar = AlarmRegistrar(appContext)
    private val resolver = NextWakeOccurrenceResolver()
    private val snoozeFactory = SnoozeOccurrenceFactory()

    @Synchronized
    fun commitSchedule(schedule: WakeSchedule): AlarmHealth {
        val previous = store.read()
        check(previous?.activeOccurrence == null) {
            "Cannot replace a wake schedule while a wake execution is active"
        }
        val next = resolver.resolve(schedule, Instant.now(clock))

        // New durable authority is written before the old OS alarm is cancelled. If the
        // process dies between these steps, an old alarm can still arrive but will be stale
        // against this snapshot and therefore cannot wake the user.
        persistPlannedOccurrence(
            snapshot = CriticalWakeSnapshot(
                schedule = schedule,
                nextOccurrence = next,
                activeOccurrence = null,
                registeredOccurrenceId = null,
                generation = (previous?.generation ?: 0) + 1,
                enabled = true,
            ),
            occurrence = next,
            obsoleteOccurrenceId = previous?.nextOccurrence?.id,
        )
        return health()
    }

    @Synchronized
    fun cancelSchedule() {
        val snapshot = store.read() ?: return
        val obsoleteOccurrenceId = snapshot.nextOccurrence?.id

        // Persist the cancellation tombstone first. If the process dies before AlarmManager
        // cancellation, the stale PendingIntent may still arrive but beginActive() rejects it.
        disable(snapshot)
        obsoleteOccurrenceId?.let(registrar::cancel)
    }

    @Synchronized
    fun beginActive(occurrenceId: WakeOccurrenceId): BeginActiveResult {
        val snapshot = store.read() ?: return BeginActiveResult.STALE
        if (!snapshot.enabled) return BeginActiveResult.STALE
        if (snapshot.activeOccurrence?.id == occurrenceId) return BeginActiveResult.ALREADY_ACTIVE
        val next = snapshot.nextOccurrence ?: return BeginActiveResult.STALE
        if (next.id != occurrenceId) return BeginActiveResult.STALE

        store.write(
            snapshot.copy(
                nextOccurrence = null,
                activeOccurrence = next,
                registeredOccurrenceId = null,
                generation = snapshot.generation + 1,
            ),
        )
        return BeginActiveResult.STARTED
    }

    @Synchronized
    fun stopActive(occurrenceId: WakeOccurrenceId): Boolean {
        val snapshot = store.read() ?: return false
        if (!snapshot.enabled || snapshot.activeOccurrence?.id != occurrenceId) return false
        completeOrAdvance(snapshot)
        return true
    }

    @Synchronized
    fun snoozeActive(
        occurrenceId: WakeOccurrenceId,
        duration: Duration,
    ): WakeOccurrence? {
        val snapshot = store.read() ?: return null
        if (!snapshot.enabled || snapshot.activeOccurrence?.id != occurrenceId) return null
        if (!registrar.canScheduleExactAlarms()) return null

        val snooze = snoozeFactory.create(
            schedule = snapshot.schedule,
            now = Instant.now(clock),
            duration = duration,
        )

        // Snooze has a stricter ordering requirement than a normal future schedule: the active
        // wake must remain authoritative (and therefore audible/controlable) until Android has
        // accepted the replacement exact alarm. If registration or the durable hand-off fails,
        // cancel any partial replacement and leave the current active snapshot untouched.
        return try {
            registrar.register(snooze)
            store.write(
                snapshot.copy(
                    nextOccurrence = snooze,
                    activeOccurrence = null,
                    registeredOccurrenceId = snooze.id,
                    generation = snapshot.generation + 1,
                ),
            )
            snooze
        } catch (_: Exception) {
            runCatching { registrar.cancel(snooze.id) }
            null
        }
    }

    /**
     * Returns the current enabled Wake Schedule without exposing Critical Wake Snapshot/storage
     * details to product UI. Reading schedule intent remains part of the Alarm Kernel contract.
     */
    fun currentSchedule(): WakeSchedule? =
        store.read()?.takeIf { it.enabled }?.schedule

    fun activeOccurrence(): WakeOccurrence? =
        store.read()?.takeIf { it.enabled }?.activeOccurrence

    /**
     * Repairs Android registration from durable state.
     * On boot, an execution that was active before power loss is treated as interrupted and completed.
     */
    @Synchronized
    fun reconcile(afterBoot: Boolean = false): AlarmHealth {
        val snapshot = store.read() ?: return health()
        if (!snapshot.enabled) return health()

        if (snapshot.activeOccurrence != null) {
            if (afterBoot) completeOrAdvance(snapshot)
            return health()
        }

        val next = snapshot.nextOccurrence ?: run {
            completeOrAdvance(snapshot)
            return health()
        }

        if (!next.scheduledAt.toInstant().isAfter(Instant.now(clock))) {
            completeOrAdvance(snapshot)
            return health()
        }

        if (!registrar.canScheduleExactAlarms()) {
            if (snapshot.registeredOccurrenceId != null) {
                store.write(
                    snapshot.copy(
                        registeredOccurrenceId = null,
                        generation = snapshot.generation + 1,
                    ),
                )
            }
            return health()
        }

        registrar.register(next)
        store.write(
            snapshot.copy(
                registeredOccurrenceId = next.id,
                generation = snapshot.generation + 1,
            ),
        )
        return health()
    }

    fun health(): AlarmHealth {
        val snapshot = store.read()
        val exactAllowed = registrar.canScheduleExactAlarms()
        val presentation = AlarmPresentationAccess.snapshot(appContext)
        val enabled = snapshot?.enabled == true
        val registered = enabled && snapshot?.nextOccurrence != null &&
            snapshot.registeredOccurrenceId == snapshot.nextOccurrence.id
        val active = enabled && snapshot?.activeOccurrence != null
        val ready = exactAllowed && presentation.ready && (registered || active)

        return AlarmHealth(
            ready = ready,
            exactAlarmAllowed = exactAllowed,
            notificationsAllowed = presentation.notificationsAllowed,
            notificationChannelHighImportance = presentation.highImportanceChannel,
            fullScreenIntentAllowed = presentation.fullScreenIntentAllowed,
            nextOccurrence = snapshot?.takeIf { it.enabled }?.nextOccurrence,
            activeOccurrence = snapshot?.takeIf { it.enabled }?.activeOccurrence,
            detail = when {
                snapshot == null -> "No wake schedule configured"
                !snapshot.enabled -> "Wake schedule disabled"
                !exactAllowed -> "Exact alarm capability unavailable"
                !presentation.notificationsAllowed -> "Notification access required for alarm controls"
                !presentation.highImportanceChannel -> "Active wake alerts must be high priority"
                !presentation.fullScreenIntentAllowed -> "Full-screen alarm access required"
                active -> "Wake execution is active"
                !registered -> "Wake occurrence needs reconciliation"
                else -> "Wake Ready"
            },
        )
    }

    private fun completeOrAdvance(snapshot: CriticalWakeSnapshot) {
        when (snapshot.schedule.completionPolicy) {
            WakeCompletionPolicy.ONE_SHOT -> disable(snapshot)
            WakeCompletionPolicy.RECURRING -> scheduleNextPrimary(snapshot)
        }
    }

    private fun disable(snapshot: CriticalWakeSnapshot) {
        store.write(
            snapshot.copy(
                nextOccurrence = null,
                activeOccurrence = null,
                registeredOccurrenceId = null,
                generation = snapshot.generation + 1,
                enabled = false,
            ),
        )
    }

    private fun scheduleNextPrimary(snapshot: CriticalWakeSnapshot) {
        val next = resolver.resolve(snapshot.schedule, Instant.now(clock))
        persistPlannedOccurrence(
            snapshot = snapshot.copy(
                nextOccurrence = next,
                activeOccurrence = null,
                registeredOccurrenceId = null,
                generation = snapshot.generation + 1,
                enabled = true,
            ),
            occurrence = next,
        )
    }

    private fun persistPlannedOccurrence(
        snapshot: CriticalWakeSnapshot,
        occurrence: WakeOccurrence,
        obsoleteOccurrenceId: WakeOccurrenceId? = null,
    ) {
        store.write(snapshot)

        obsoleteOccurrenceId
            ?.takeIf { it != occurrence.id }
            ?.let(registrar::cancel)

        if (!registrar.canScheduleExactAlarms()) return

        registrar.register(occurrence)
        store.write(
            snapshot.copy(
                registeredOccurrenceId = occurrence.id,
                generation = snapshot.generation + 1,
            ),
        )
    }
}

enum class BeginActiveResult {
    STARTED,
    ALREADY_ACTIVE,
    STALE,
}

data class AlarmHealth(
    val ready: Boolean,
    val exactAlarmAllowed: Boolean,
    val notificationsAllowed: Boolean,
    val notificationChannelHighImportance: Boolean,
    val fullScreenIntentAllowed: Boolean,
    val nextOccurrence: WakeOccurrence?,
    val activeOccurrence: WakeOccurrence?,
    val detail: String,
)

package com.wakemyway.app.alarm

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.wakemyway.core.schedule.NextWakeOccurrenceResolver
import com.wakemyway.core.schedule.SnoozeOccurrenceFactory
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeSchedule
import java.time.Clock
import java.time.Duration
import java.time.Instant

class AlarmKernel(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val appContext = context.applicationContext
    private val store = CriticalWakeStore(appContext)
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
        store.write(
            snapshot.copy(
                nextOccurrence = null,
                activeOccurrence = null,
                registeredOccurrenceId = null,
                generation = snapshot.generation + 1,
                enabled = false,
            ),
        )
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
        scheduleNextPrimary(snapshot)
        return true
    }

    @Synchronized
    fun snoozeActive(
        occurrenceId: WakeOccurrenceId,
        duration: Duration,
    ): WakeOccurrence? {
        val snapshot = store.read() ?: return null
        if (!snapshot.enabled || snapshot.activeOccurrence?.id != occurrenceId) return null

        val snooze = snoozeFactory.create(
            schedule = snapshot.schedule,
            now = Instant.now(clock),
            duration = duration,
        )
        persistPlannedOccurrence(
            snapshot = snapshot.copy(
                nextOccurrence = snooze,
                activeOccurrence = null,
                registeredOccurrenceId = null,
                generation = snapshot.generation + 1,
            ),
            occurrence = snooze,
        )
        return snooze
    }

    fun activeOccurrence(): WakeOccurrence? =
        store.read()?.takeIf { it.enabled }?.activeOccurrence

    /**
     * Repairs Android registration from durable state.
     * On boot, an execution that was active before power loss is treated as interrupted and advanced.
     */
    @Synchronized
    fun reconcile(afterBoot: Boolean = false): AlarmHealth {
        val snapshot = store.read() ?: return health()
        if (!snapshot.enabled) return health()

        if (snapshot.activeOccurrence != null) {
            if (afterBoot) scheduleNextPrimary(snapshot)
            return health()
        }

        val next = snapshot.nextOccurrence ?: run {
            scheduleNextPrimary(snapshot)
            return health()
        }

        if (!next.scheduledAt.toInstant().isAfter(Instant.now(clock))) {
            scheduleNextPrimary(snapshot)
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
        val notificationAllowed = notificationsAllowed()
        val fullScreenAllowed = fullScreenIntentAllowed()
        val enabled = snapshot?.enabled == true
        val registered = enabled && snapshot?.nextOccurrence != null &&
            snapshot.registeredOccurrenceId == snapshot.nextOccurrence.id
        val active = enabled && snapshot?.activeOccurrence != null
        val ready = exactAllowed && (registered || active)

        return AlarmHealth(
            ready = ready,
            exactAlarmAllowed = exactAllowed,
            notificationsAllowed = notificationAllowed,
            fullScreenIntentAllowed = fullScreenAllowed,
            nextOccurrence = snapshot?.takeIf { it.enabled }?.nextOccurrence,
            activeOccurrence = snapshot?.takeIf { it.enabled }?.activeOccurrence,
            detail = when {
                snapshot == null -> "No wake schedule configured"
                !snapshot.enabled -> "Wake schedule disabled"
                !exactAllowed -> "Exact alarm capability unavailable"
                active -> "Wake execution is active"
                !registered -> "Wake occurrence needs reconciliation"
                !notificationAllowed -> "Ready with degraded notification presentation"
                !fullScreenAllowed -> "Ready with degraded full-screen presentation"
                else -> "Wake Ready"
            },
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

    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun fullScreenIntentAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return appContext.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
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
    val fullScreenIntentAllowed: Boolean,
    val nextOccurrence: WakeOccurrence?,
    val activeOccurrence: WakeOccurrence?,
    val detail: String,
)

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
        previous?.nextOccurrence?.let { registrar.cancel(it.id) }

        val next = resolver.resolve(schedule, Instant.now(clock))
        val pending = CriticalWakeSnapshot(
            schedule = schedule,
            nextOccurrence = next,
            activeOccurrence = null,
            registeredOccurrenceId = null,
            generation = (previous?.generation ?: 0) + 1,
        )
        store.write(pending)
        registrar.register(next)
        store.write(
            pending.copy(
                registeredOccurrenceId = next.id,
                generation = pending.generation + 1,
            ),
        )
        return health()
    }

    @Synchronized
    fun cancelSchedule() {
        val snapshot = store.read()
        snapshot?.nextOccurrence?.let { registrar.cancel(it.id) }
        store.clear()
    }

    @Synchronized
    fun beginActive(occurrenceId: WakeOccurrenceId): BeginActiveResult {
        val snapshot = store.read() ?: return BeginActiveResult.STALE
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
        if (snapshot.activeOccurrence?.id != occurrenceId) return false
        scheduleNextPrimary(snapshot)
        return true
    }

    @Synchronized
    fun snoozeActive(
        occurrenceId: WakeOccurrenceId,
        duration: Duration,
    ): WakeOccurrence? {
        val snapshot = store.read() ?: return null
        if (snapshot.activeOccurrence?.id != occurrenceId) return null

        val snooze = snoozeFactory.create(
            schedule = snapshot.schedule,
            now = Instant.now(clock),
            duration = duration,
        )
        val pending = snapshot.copy(
            nextOccurrence = snooze,
            activeOccurrence = null,
            registeredOccurrenceId = null,
            generation = snapshot.generation + 1,
        )
        store.write(pending)
        registrar.register(snooze)
        store.write(
            pending.copy(
                registeredOccurrenceId = snooze.id,
                generation = pending.generation + 1,
            ),
        )
        return snooze
    }

    /**
     * Repairs Android registration from durable state.
     * On boot, an execution that was active before power loss is treated as interrupted and advanced.
     */
    @Synchronized
    fun reconcile(afterBoot: Boolean = false): AlarmHealth {
        val snapshot = store.read() ?: return health()

        if (snapshot.activeOccurrence != null) {
            if (afterBoot) {
                scheduleNextPrimary(snapshot)
            }
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
        val registered = snapshot?.nextOccurrence != null &&
            snapshot.registeredOccurrenceId == snapshot.nextOccurrence.id
        val active = snapshot?.activeOccurrence != null
        val ready = exactAllowed && (registered || active)

        return AlarmHealth(
            ready = ready,
            exactAlarmAllowed = exactAllowed,
            notificationsAllowed = notificationAllowed,
            fullScreenIntentAllowed = fullScreenAllowed,
            nextOccurrence = snapshot?.nextOccurrence,
            activeOccurrence = snapshot?.activeOccurrence,
            detail = when {
                snapshot == null -> "No wake schedule configured"
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
        val pending = snapshot.copy(
            nextOccurrence = next,
            activeOccurrence = null,
            registeredOccurrenceId = null,
            generation = snapshot.generation + 1,
        )
        store.write(pending)
        registrar.register(next)
        store.write(
            pending.copy(
                registeredOccurrenceId = next.id,
                generation = pending.generation + 1,
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

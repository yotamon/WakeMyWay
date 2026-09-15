package com.wakemyway.app.alarm

import android.content.Context
import com.wakemyway.core.schedule.NextWakeOccurrenceResolver
import com.wakemyway.core.schedule.SnoozeOccurrenceFactory
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
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

    /**
     * Creates or replaces exactly one schedule slot. Unrelated alarm slots remain untouched.
     *
     * Durable authority is always written before an obsolete OS registration is cancelled. A stale
     * PendingIntent may therefore still arrive after a crash, but [beginActive] rejects it by exact
     * occurrence identity and schedule revision.
     */
    @Synchronized
    fun commitSchedule(schedule: WakeSchedule): AlarmHealth {
        val state = stateOrEmpty()
        check(state.activeOccurrence?.wakeScheduleId != schedule.id) {
            "Cannot replace a wake schedule while its wake execution is active"
        }

        val previousSlot = state.slots[schedule.id]
        val next = resolver.resolve(schedule, Instant.now(clock))
        val plannedSlot = CriticalScheduleSlot(
            schedule = schedule,
            nextOccurrence = next,
            registeredOccurrenceId = null,
            enabled = true,
        )
        var plannedState = state.copy(
            slots = state.slots + (schedule.id to plannedSlot),
            generation = state.generation + 1,
        )
        store.write(plannedState)

        previousSlot?.registeredOccurrenceId
            ?.takeIf { it != next.id }
            ?.let(registrar::cancel)
        previousSlot?.nextOccurrence?.id
            ?.takeIf { it != next.id && it != previousSlot.registeredOccurrenceId }
            ?.let(registrar::cancel)

        if (registrar.canScheduleExactAlarms()) {
            registrar.register(next)
            plannedState = plannedState.withRegistered(schedule.id, next.id)
            store.write(plannedState)
        }
        return health()
    }

    /** Disable only one schedule slot, preserving every unrelated alarm. */
    @Synchronized
    fun cancelSchedule(scheduleId: WakeScheduleId) {
        val state = store.read() ?: return
        val slot = state.slots[scheduleId] ?: return
        if (!slot.enabled && state.activeOccurrence?.wakeScheduleId != scheduleId) return

        val activeOwnedBySlot = state.activeOccurrence?.wakeScheduleId == scheduleId
        val disabled = slot.copy(
            nextOccurrence = null,
            registeredOccurrenceId = null,
            enabled = false,
        )
        store.write(
            state.copy(
                slots = state.slots + (scheduleId to disabled),
                activeOccurrence = if (activeOwnedBySlot) null else state.activeOccurrence,
                generation = state.generation + 1,
            ),
        )
        slot.registeredOccurrenceId?.let(registrar::cancel)
        slot.nextOccurrence?.id
            ?.takeIf { it != slot.registeredOccurrenceId }
            ?.let(registrar::cancel)
    }

    /**
     * Legacy/global cancellation used when critical Android presentation capability is unsafe.
     * Product UI for the multi-alarm model should call [cancelSchedule] with a concrete id.
     */
    @Synchronized
    fun cancelSchedule() {
        val state = store.read() ?: return
        if (state.slots.isEmpty() && state.activeOccurrence == null) return

        val obsolete = state.slots.values.flatMap { slot ->
            listOfNotNull(slot.registeredOccurrenceId, slot.nextOccurrence?.id)
        }.distinct()
        val disabledSlots = state.slots.mapValues { (_, slot) ->
            slot.copy(
                nextOccurrence = null,
                registeredOccurrenceId = null,
                enabled = false,
            )
        }
        store.write(
            state.copy(
                slots = disabledSlots,
                activeOccurrence = null,
                generation = state.generation + 1,
            ),
        )
        obsolete.forEach(registrar::cancel)
    }

    @Synchronized
    fun beginActive(occurrenceId: WakeOccurrenceId): BeginActiveResult {
        val state = store.read() ?: return BeginActiveResult.STALE
        if (state.activeOccurrence?.id == occurrenceId) return BeginActiveResult.ALREADY_ACTIVE

        val ownerEntry = state.slots.entries.firstOrNull { (_, slot) ->
            slot.enabled && slot.nextOccurrence?.id == occurrenceId
        } ?: return BeginActiveResult.STALE
        val scheduleId = ownerEntry.key
        val slot = ownerEntry.value
        val next = requireNotNull(slot.nextOccurrence)

        if (state.activeOccurrence != null) {
            // Android has consumed this PendingIntent, but another wake owns physical execution.
            // Keep the due occurrence durable so reconciliation can deterministically mark it missed
            // or advance it after the active chain ends. Do not claim it is still OS-registered.
            if (slot.registeredOccurrenceId != null) {
                store.write(
                    state.copy(
                        slots = state.slots + (
                            scheduleId to slot.copy(registeredOccurrenceId = null)
                        ),
                        generation = state.generation + 1,
                    ),
                )
            }
            return BeginActiveResult.CONFLICT
        }

        store.write(
            state.copy(
                slots = state.slots + (
                    scheduleId to slot.copy(
                        nextOccurrence = null,
                        registeredOccurrenceId = null,
                    )
                ),
                activeOccurrence = next,
                generation = state.generation + 1,
            ),
        )
        return BeginActiveResult.STARTED
    }

    @Synchronized
    fun stopActive(occurrenceId: WakeOccurrenceId): Boolean {
        val state = store.read() ?: return false
        val active = state.activeOccurrence ?: return false
        if (active.id != occurrenceId) return false

        completeOrAdvanceActive(state)
        // A second alarm may have fired while this wake was active. Resolve any now-overdue slot
        // immediately rather than waiting for a later boot/time-change reconciliation.
        runCatching { reconcile() }
        return true
    }

    @Synchronized
    fun snoozeActive(
        occurrenceId: WakeOccurrenceId,
        duration: Duration,
    ): WakeOccurrence? {
        val state = store.read() ?: return null
        val active = state.activeOccurrence ?: return null
        if (active.id != occurrenceId) return null
        val scheduleId = active.wakeScheduleId
        val slot = state.slots[scheduleId]?.takeIf { it.enabled } ?: return null
        if (!registrar.canScheduleExactAlarms()) return null

        val snooze = snoozeFactory.create(
            schedule = slot.schedule,
            now = Instant.now(clock),
            duration = duration,
        )

        // The currently active wake stays authoritative until Android has accepted the exact
        // replacement. If registration or the atomic hand-off fails, cancel any partial replacement
        // and leave the previous active state intact.
        return try {
            registrar.register(snooze)
            store.write(
                state.copy(
                    slots = state.slots + (
                        scheduleId to slot.copy(
                            nextOccurrence = snooze,
                            registeredOccurrenceId = snooze.id,
                        )
                    ),
                    activeOccurrence = null,
                    generation = state.generation + 1,
                ),
            )
            runCatching { reconcile() }
            snooze
        } catch (_: Exception) {
            runCatching { registrar.cancel(snooze.id) }
            null
        }
    }

    /**
     * Legacy UI compatibility: returns the earliest enabled schedule. New multi-alarm product code
     * should use [currentSchedules] or a concrete schedule id.
     */
    fun currentSchedule(): WakeSchedule? =
        enabledSlots(store.read()).minByOrNull { slot ->
            slot.nextOccurrence?.scheduledAt?.toInstant() ?: Instant.MAX
        }?.schedule

    fun currentSchedules(): List<WakeSchedule> =
        enabledSlots(store.read()).map(CriticalScheduleSlot::schedule)

    fun activeOccurrence(): WakeOccurrence? = store.read()?.activeOccurrence

    fun nextOccurrences(): List<WakeOccurrence> = enabledSlots(store.read())
        .mapNotNull(CriticalScheduleSlot::nextOccurrence)
        .sortedBy { it.scheduledAt.toInstant() }

    /**
     * Repairs every independent OS registration from durable critical state.
     *
     * [afterBoot] forces re-registration because AlarmManager registrations are not trusted across
     * reboot. [recalculateFuture] is used for wall-clock/timezone changes and recomputes recurring
     * or exact-date schedules from local intent rather than preserving an obsolete instant.
     */
    @Synchronized
    fun reconcile(
        afterBoot: Boolean = false,
        recalculateFuture: Boolean = false,
    ): AlarmHealth {
        val original = store.read() ?: return health()
        var state = original
        val now = Instant.now(clock)
        val obsoleteIds = linkedSetOf<WakeOccurrenceId>()

        if (state.activeOccurrence != null) {
            if (!afterBoot) return health()
            state = advancedAfterActive(state, now)
        }

        var slots = state.slots
        var changed = state != original

        state.slots.forEach { (scheduleId, existing) ->
            if (!existing.enabled) return@forEach
            var slot = slots.getValue(scheduleId)
            val oldNext = slot.nextOccurrence

            val shouldRecalculate = recalculateFuture &&
                (slot.schedule.completionPolicy == WakeCompletionPolicy.RECURRING ||
                    slot.schedule.oneShotDate != null)

            slot = when {
                oldNext == null -> advanceMissingSlot(slot, now)
                !oldNext.scheduledAt.toInstant().isAfter(now) -> advanceMissedSlot(slot, now)
                shouldRecalculate -> recalculateSlot(slot, now)
                else -> slot
            }

            if (slot.nextOccurrence?.id != oldNext?.id) {
                existing.registeredOccurrenceId?.let(obsoleteIds::add)
                oldNext?.id?.takeIf { it != existing.registeredOccurrenceId }?.let(obsoleteIds::add)
                slot = slot.copy(registeredOccurrenceId = null)
            }

            if (!registrar.canScheduleExactAlarms() && slot.registeredOccurrenceId != null) {
                slot = slot.copy(registeredOccurrenceId = null)
            }

            if (slot != existing) {
                slots = slots + (scheduleId to slot)
                changed = true
            }
        }

        if (changed) {
            state = state.copy(slots = slots, generation = state.generation + 1)
            store.write(state)
        }
        obsoleteIds.forEach(registrar::cancel)

        if (!registrar.canScheduleExactAlarms()) return health()

        var registeredState = state
        var registrationChanged = false
        registeredState.slots.forEach { (scheduleId, slot) ->
            if (!slot.enabled) return@forEach
            val next = slot.nextOccurrence ?: return@forEach
            val mustRegister = afterBoot || recalculateFuture || slot.registeredOccurrenceId != next.id
            if (!mustRegister) return@forEach

            val registered = runCatching {
                registrar.register(next)
                true
            }.getOrDefault(false)
            if (registered) {
                registeredState = registeredState.withRegistered(scheduleId, next.id)
                registrationChanged = true
            }
        }
        if (registrationChanged) {
            store.write(
                registeredState.copy(generation = registeredState.generation + 1),
            )
        }
        return health()
    }

    fun health(): AlarmHealth {
        val state = store.read()
        val enabled = enabledSlots(state)
        val exactAllowed = registrar.canScheduleExactAlarms()
        val presentation = AlarmPresentationAccess.snapshot(appContext)
        val active = state?.activeOccurrence
        val readyCount = enabled.count { slot ->
            val isActiveOwner = active?.wakeScheduleId == slot.schedule.id
            isActiveOwner || (
                slot.nextOccurrence != null &&
                    slot.registeredOccurrenceId == slot.nextOccurrence.id
                )
        }
        val next = enabled.mapNotNull(CriticalScheduleSlot::nextOccurrence)
            .minByOrNull { it.scheduledAt.toInstant() }
        val allRegistered = enabled.isNotEmpty() && readyCount == enabled.size
        val ready = exactAllowed && presentation.ready && allRegistered

        return AlarmHealth(
            ready = ready,
            exactAlarmAllowed = exactAllowed,
            notificationsAllowed = presentation.notificationsAllowed,
            notificationChannelHighImportance = presentation.highImportanceChannel,
            fullScreenIntentAllowed = presentation.fullScreenIntentAllowed,
            nextOccurrence = next,
            activeOccurrence = active,
            detail = when {
                enabled.isEmpty() -> "No enabled wake schedules"
                !exactAllowed -> "Exact alarm capability unavailable"
                !presentation.notificationsAllowed -> "Notification access required for alarm controls"
                !presentation.highImportanceChannel -> "Active wake alerts must be high priority"
                !presentation.fullScreenIntentAllowed -> "Full-screen alarm access required"
                active != null && readyCount == enabled.size -> "Wake execution is active"
                readyCount != enabled.size -> "${enabled.size - readyCount} wake schedule(s) need reconciliation"
                else -> "Wake Ready"
            },
            enabledScheduleCount = enabled.size,
            readyScheduleCount = readyCount,
        )
    }

    fun health(scheduleId: WakeScheduleId): AlarmScheduleHealth? {
        val state = store.read() ?: return null
        val slot = state.slots[scheduleId] ?: return null
        val exactAllowed = registrar.canScheduleExactAlarms()
        val presentation = AlarmPresentationAccess.snapshot(appContext)
        val active = state.activeOccurrence?.takeIf { it.wakeScheduleId == scheduleId }
        val registered = slot.enabled && slot.nextOccurrence != null &&
            slot.registeredOccurrenceId == slot.nextOccurrence.id
        return AlarmScheduleHealth(
            scheduleId = scheduleId,
            enabled = slot.enabled,
            ready = slot.enabled && exactAllowed && presentation.ready && (registered || active != null),
            nextOccurrence = slot.nextOccurrence,
            activeOccurrence = active,
        )
    }

    private fun completeOrAdvanceActive(state: CriticalAlarmState) {
        val now = Instant.now(clock)
        val advanced = advancedAfterActive(state, now)
        store.write(advanced.copy(generation = state.generation + 1))

        val ownerId = state.activeOccurrence?.wakeScheduleId ?: return
        val next = advanced.slots[ownerId]?.nextOccurrence ?: return
        if (!registrar.canScheduleExactAlarms()) return
        registrar.register(next)
        store.write(
            advanced.withRegistered(ownerId, next.id)
                .copy(generation = state.generation + 2),
        )
    }

    private fun advancedAfterActive(
        state: CriticalAlarmState,
        now: Instant,
    ): CriticalAlarmState {
        val active = state.activeOccurrence ?: return state
        val scheduleId = active.wakeScheduleId
        val slot = state.slots[scheduleId]
            ?: return state.copy(activeOccurrence = null)
        val advanced = when (slot.schedule.completionPolicy) {
            WakeCompletionPolicy.ONE_SHOT -> disabled(slot)
            WakeCompletionPolicy.RECURRING -> planned(slot, resolver.resolve(slot.schedule, now))
        }
        return state.copy(
            slots = state.slots + (scheduleId to advanced),
            activeOccurrence = null,
        )
    }

    private fun advanceMissingSlot(slot: CriticalScheduleSlot, now: Instant): CriticalScheduleSlot =
        when (slot.schedule.completionPolicy) {
            WakeCompletionPolicy.ONE_SHOT -> disabled(slot)
            WakeCompletionPolicy.RECURRING -> planned(slot, resolver.resolve(slot.schedule, now))
        }

    private fun advanceMissedSlot(slot: CriticalScheduleSlot, now: Instant): CriticalScheduleSlot =
        when (slot.schedule.completionPolicy) {
            WakeCompletionPolicy.ONE_SHOT -> disabled(slot)
            WakeCompletionPolicy.RECURRING -> planned(slot, resolver.resolve(slot.schedule, now))
        }

    private fun recalculateSlot(slot: CriticalScheduleSlot, now: Instant): CriticalScheduleSlot =
        runCatching { planned(slot, resolver.resolve(slot.schedule, now)) }
            .getOrElse {
                if (slot.schedule.completionPolicy == WakeCompletionPolicy.ONE_SHOT) disabled(slot)
                else throw it
            }

    private fun planned(
        slot: CriticalScheduleSlot,
        occurrence: WakeOccurrence,
    ) = slot.copy(
        nextOccurrence = occurrence,
        registeredOccurrenceId = null,
        enabled = true,
    )

    private fun disabled(slot: CriticalScheduleSlot) = slot.copy(
        nextOccurrence = null,
        registeredOccurrenceId = null,
        enabled = false,
    )

    private fun stateOrEmpty(): CriticalAlarmState = store.read() ?: CriticalAlarmState(
        slots = emptyMap(),
        activeOccurrence = null,
        generation = 0,
    )

    private fun enabledSlots(state: CriticalAlarmState?): List<CriticalScheduleSlot> =
        state?.slots?.values?.filter(CriticalScheduleSlot::enabled).orEmpty()

    private fun CriticalAlarmState.withRegistered(
        scheduleId: WakeScheduleId,
        occurrenceId: WakeOccurrenceId,
    ): CriticalAlarmState {
        val slot = requireNotNull(slots[scheduleId])
        require(slot.nextOccurrence?.id == occurrenceId) {
            "Cannot register occurrence outside its current critical schedule slot"
        }
        return copy(
            slots = slots + (
                scheduleId to slot.copy(registeredOccurrenceId = occurrenceId)
            ),
        )
    }
}

enum class BeginActiveResult {
    STARTED,
    ALREADY_ACTIVE,
    /** A different valid Wake Occurrence already owns physical active execution. */
    CONFLICT,
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
    val enabledScheduleCount: Int = 0,
    val readyScheduleCount: Int = 0,
)

data class AlarmScheduleHealth(
    val scheduleId: WakeScheduleId,
    val enabled: Boolean,
    val ready: Boolean,
    val nextOccurrence: WakeOccurrence?,
    val activeOccurrence: WakeOccurrence?,
)

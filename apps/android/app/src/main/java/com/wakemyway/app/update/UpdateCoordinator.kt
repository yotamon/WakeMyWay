package com.wakemyway.app.update

import android.content.Context
import com.wakemyway.app.BuildConfig
import com.wakemyway.app.alarm.AlarmHealth
import java.time.Duration
import java.time.Instant

class UpdateCoordinator(
    context: Context,
    private val healthProvider: () -> AlarmHealth,
    providerFactory: ((UpdateProviderEvent) -> Unit) -> UpdateProvider,
    private val onStateChanged: (UpdateState) -> Unit,
    private val now: () -> Instant = Instant::now,
) {
    private val throttle = UpdateCheckThrottle(context)
    private val safetyGate = UpdateSafetyGate()
    private val provider = providerFactory(::onProviderEvent)

    private var state: UpdateState = UpdateState.Idle
    private var manualCheckInFlight = false

    fun checkIfDue() {
        if (!throttle.isDue(now())) return
        if (healthProvider().activeOccurrence != null) return
        check(force = false)
    }

    fun checkNow() {
        check(force = true)
    }

    fun beginUpdate() {
        val release = state.releaseOrNull() ?: return
        updateState(UpdateState.Downloading(release, progress = null))
        provider.beginUpdate(release)
    }

    fun installUpdate() {
        val release = state.releaseOrNull() ?: return
        val deferral = safetyGate.evaluate(
            snapshot = healthProvider().toUpdateSafetySnapshot(),
            now = now(),
        )
        if (deferral != null) {
            updateState(UpdateState.Deferred(release, deferral))
            return
        }
        provider.completeUpdate(release)
    }

    fun openInstallPermissionSettings() {
        provider.openInstallPermissionSettings()
    }

    fun resume() {
        provider.resume(BuildConfig.VERSION_CODE.toLong())

        val deferred = state as? UpdateState.Deferred ?: return
        val stillBlocked = safetyGate.evaluate(
            snapshot = healthProvider().toUpdateSafetySnapshot(),
            now = now(),
        )
        if (stillBlocked == null) {
            updateState(UpdateState.ReadyToInstall(deferred.release))
        }
    }

    fun handleActivityResult(resultCode: Int) {
        provider.handleActivityResult(resultCode)
    }

    fun close() {
        provider.close()
    }

    private fun check(force: Boolean) {
        manualCheckInFlight = force
        throttle.recordAttempt(now())
        updateState(UpdateState.Checking)

        provider.check(BuildConfig.VERSION_CODE.toLong()) { result ->
            result.fold(
                onSuccess = { checkResult ->
                    when (checkResult) {
                        UpdateProviderCheck.UpToDate -> updateState(UpdateState.UpToDate)
                        is UpdateProviderCheck.Available -> {
                            updateState(UpdateState.Available(checkResult.release))
                        }
                        is UpdateProviderCheck.ReadyToInstall -> {
                            updateState(UpdateState.ReadyToInstall(checkResult.release))
                        }
                    }
                },
                onFailure = { error ->
                    if (manualCheckInFlight) {
                        updateState(
                            UpdateState.Error(
                                message = error.message ?: "Could not check for updates.",
                            ),
                        )
                    } else {
                        updateState(UpdateState.Idle)
                    }
                },
            )
            manualCheckInFlight = false
        }
    }

    private fun onProviderEvent(event: UpdateProviderEvent) {
        when (event) {
            is UpdateProviderEvent.Downloading -> {
                updateState(UpdateState.Downloading(event.release, event.progress))
            }
            is UpdateProviderEvent.ReadyToInstall -> {
                updateState(UpdateState.ReadyToInstall(event.release))
            }
            is UpdateProviderEvent.InstallPermissionRequired -> {
                updateState(UpdateState.InstallPermissionRequired(event.release))
            }
            is UpdateProviderEvent.Installing -> {
                updateState(UpdateState.Installing(event.release))
            }
            is UpdateProviderEvent.Cancelled -> {
                updateState(UpdateState.Available(event.release))
            }
            is UpdateProviderEvent.Failed -> {
                updateState(UpdateState.Error(event.message, event.release))
            }
        }
    }

    private fun updateState(next: UpdateState) {
        state = next
        onStateChanged(next)
    }

    private fun AlarmHealth.toUpdateSafetySnapshot(): UpdateSafetySnapshot =
        UpdateSafetySnapshot(
            activeWake = activeOccurrence != null,
            nextWakeAt = nextOccurrence?.scheduledAt?.toInstant(),
        )

    private fun UpdateState.releaseOrNull(): UpdateRelease? = when (this) {
        is UpdateState.Available -> release
        is UpdateState.Downloading -> release
        is UpdateState.ReadyToInstall -> release
        is UpdateState.InstallPermissionRequired -> release
        is UpdateState.Deferred -> release
        is UpdateState.Installing -> release
        is UpdateState.Error -> release
        UpdateState.Idle,
        UpdateState.Checking,
        UpdateState.UpToDate,
        -> null
    }
}

private class UpdateCheckThrottle(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun isDue(now: Instant): Boolean {
        val lastAttempt = preferences.getLong(KEY_LAST_ATTEMPT, 0L)
        if (lastAttempt <= 0L) return true
        return Duration.between(Instant.ofEpochMilli(lastAttempt), now) >= CHECK_INTERVAL
    }

    fun recordAttempt(now: Instant) {
        preferences.edit().putLong(KEY_LAST_ATTEMPT, now.toEpochMilli()).apply()
    }

    private companion object {
        const val PREFERENCES = "update-check"
        const val KEY_LAST_ATTEMPT = "last-attempt-epoch-ms"
        val CHECK_INTERVAL: Duration = Duration.ofHours(24)
    }
}

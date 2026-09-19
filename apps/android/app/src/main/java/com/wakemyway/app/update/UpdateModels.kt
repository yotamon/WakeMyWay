package com.wakemyway.app.update

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class UpdateSource {
    DIRECT,
    PLAY,
}

@Serializable
enum class UpdateUrgency {
    NORMAL,
    IMPORTANT,
    CRITICAL,
}

@Serializable
data class UpdateRelease(
    val source: UpdateSource,
    val versionCode: Long,
    val versionName: String? = null,
    val urgency: UpdateUrgency = UpdateUrgency.NORMAL,
    val releaseNotes: List<String> = emptyList(),
)

enum class UpdateDeferralReason {
    ACTIVE_WAKE,
    UPCOMING_WAKE,
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState

    data class Available(val release: UpdateRelease) : UpdateState

    data class Downloading(
        val release: UpdateRelease,
        val progress: Float?,
    ) : UpdateState

    data class ReadyToInstall(val release: UpdateRelease) : UpdateState
    data class InstallPermissionRequired(val release: UpdateRelease) : UpdateState

    data class Deferred(
        val release: UpdateRelease,
        val reason: UpdateDeferralReason,
    ) : UpdateState

    data class Installing(val release: UpdateRelease) : UpdateState

    data class Error(
        val message: String,
        val release: UpdateRelease? = null,
    ) : UpdateState
}

data class UpdateSafetySnapshot(
    val activeWake: Boolean,
    val nextWakeAt: Instant?,
)

sealed interface UpdateProviderCheck {
    data object UpToDate : UpdateProviderCheck
    data class Available(val release: UpdateRelease) : UpdateProviderCheck
    data class ReadyToInstall(val release: UpdateRelease) : UpdateProviderCheck
}

sealed interface UpdateProviderEvent {
    data class Downloading(
        val release: UpdateRelease,
        val progress: Float?,
    ) : UpdateProviderEvent

    data class ReadyToInstall(val release: UpdateRelease) : UpdateProviderEvent
    data class InstallPermissionRequired(val release: UpdateRelease) : UpdateProviderEvent
    data class Installing(val release: UpdateRelease) : UpdateProviderEvent
    data class Cancelled(val release: UpdateRelease) : UpdateProviderEvent

    data class Failed(
        val message: String,
        val release: UpdateRelease? = null,
    ) : UpdateProviderEvent
}

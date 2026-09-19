package com.wakemyway.app.update

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

internal class PlayUpdateProvider(
    activity: ComponentActivity,
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
    private val onEvent: (UpdateProviderEvent) -> Unit,
) : UpdateProvider {
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var lastInfo: AppUpdateInfo? = null
    private var lastRelease: UpdateRelease? = null

    private val installListener = InstallStateUpdatedListener { installState ->
        val release = lastRelease ?: return@InstallStateUpdatedListener
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = installState.totalBytesToDownload()
                val progress = if (total > 0L) {
                    (installState.bytesDownloaded().toFloat() / total.toFloat()).coerceIn(0f, 1f)
                } else {
                    null
                }
                onEvent(UpdateProviderEvent.Downloading(release, progress))
            }

            InstallStatus.DOWNLOADED -> {
                onEvent(UpdateProviderEvent.ReadyToInstall(release))
            }

            InstallStatus.CANCELED -> {
                onEvent(UpdateProviderEvent.Cancelled(release))
            }

            InstallStatus.FAILED -> {
                onEvent(UpdateProviderEvent.Failed("Google Play could not download the update.", release))
            }
        }
    }

    init {
        manager.registerListener(installListener)
    }

    override fun check(
        currentVersionCode: Long,
        result: (Result<UpdateProviderCheck>) -> Unit,
    ) {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                lastInfo = info

                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    val release = info.toRelease()
                    lastRelease = release
                    result(Result.success(UpdateProviderCheck.ReadyToInstall(release)))
                    return@addOnSuccessListener
                }

                if (
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.availableVersionCode().toLong() > currentVersionCode
                ) {
                    if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        result(
                            Result.failure(
                                IllegalStateException(
                                    "Google Play has an update, but an in-app flexible update is not currently available.",
                                ),
                            ),
                        )
                        return@addOnSuccessListener
                    }

                    val release = info.toRelease()
                    lastRelease = release
                    result(Result.success(UpdateProviderCheck.Available(release)))
                } else {
                    result(Result.success(UpdateProviderCheck.UpToDate))
                }
            }
            .addOnFailureListener { error ->
                result(Result.failure(error))
            }
    }

    override fun beginUpdate(release: UpdateRelease) {
        val info = lastInfo
        if (info == null || info.availableVersionCode().toLong() != release.versionCode) {
            check(currentVersionCode = 0L) { outcome ->
                outcome.onSuccess { checked ->
                    if (checked is UpdateProviderCheck.Available) {
                        beginUpdate(checked.release)
                    } else {
                        onEvent(UpdateProviderEvent.Failed("The Google Play update is no longer available.", release))
                    }
                }.onFailure { error ->
                    onEvent(
                        UpdateProviderEvent.Failed(
                            error.message ?: "Could not start the Google Play update.",
                            release,
                        ),
                    )
                }
            }
            return
        }

        lastRelease = release
        val started = manager.startUpdateFlowForResult(
            info,
            updateLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
        )
        if (!started) {
            lastInfo = null
            onEvent(UpdateProviderEvent.Failed("Google Play could not start the update flow.", release))
        }
    }

    override fun completeUpdate(release: UpdateRelease) {
        lastRelease = release
        onEvent(UpdateProviderEvent.Installing(release))
        manager.completeUpdate()
            .addOnFailureListener { error ->
                onEvent(
                    UpdateProviderEvent.Failed(
                        error.message ?: "Google Play could not complete the update.",
                        release,
                    ),
                )
            }
    }

    override fun openInstallPermissionSettings() = Unit

    override fun resume(currentVersionCode: Long) {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                lastInfo = info
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    val release = info.toRelease()
                    lastRelease = release
                    onEvent(UpdateProviderEvent.ReadyToInstall(release))
                }
            }
    }

    override fun handleActivityResult(resultCode: Int) {
        val release = lastRelease ?: return
        when (resultCode) {
            Activity.RESULT_OK -> Unit
            Activity.RESULT_CANCELED -> {
                lastInfo = null
                onEvent(UpdateProviderEvent.Cancelled(release))
            }
            else -> {
                lastInfo = null
                onEvent(UpdateProviderEvent.Failed("Google Play update flow failed.", release))
            }
        }
    }

    override fun close() {
        manager.unregisterListener(installListener)
    }

    private fun AppUpdateInfo.toRelease(): UpdateRelease =
        UpdateRelease(
            source = UpdateSource.PLAY,
            versionCode = availableVersionCode().toLong(),
            versionName = null,
            urgency = when (updatePriority().coerceIn(0, 5)) {
                4, 5 -> UpdateUrgency.CRITICAL
                2, 3 -> UpdateUrgency.IMPORTANT
                else -> UpdateUrgency.NORMAL
            },
        )
}

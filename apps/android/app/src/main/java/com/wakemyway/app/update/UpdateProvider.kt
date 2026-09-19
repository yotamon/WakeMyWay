package com.wakemyway.app.update

interface UpdateProvider {
    fun check(
        currentVersionCode: Long,
        result: (Result<UpdateProviderCheck>) -> Unit,
    )

    fun beginUpdate(release: UpdateRelease)

    fun completeUpdate(release: UpdateRelease)

    fun openInstallPermissionSettings()

    fun resume(currentVersionCode: Long)

    fun handleActivityResult(resultCode: Int)

    fun close()
}

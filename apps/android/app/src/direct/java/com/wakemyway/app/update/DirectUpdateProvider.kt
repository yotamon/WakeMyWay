package com.wakemyway.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class DirectUpdateProvider(
    private val activity: ComponentActivity,
    private val onEvent: (UpdateProviderEvent) -> Unit,
) : UpdateProvider {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val packageManager = activity.packageManager
    private val preferences = activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override fun check(
        currentVersionCode: Long,
        result: (Result<UpdateProviderCheck>) -> Unit,
    ) {
        executor.execute {
            runCatching {
                val pending = loadPending()
                if (pending != null && pending.release.versionCode > currentVersionCode) {
                    val file = File(pending.path)
                    if (file.isFile) {
                        verifyPackageIdentity(file, pending.release)
                        return@runCatching UpdateProviderCheck.ReadyToInstall(pending.release)
                    } else {
                        clearPending()
                    }
                }

                val manifest = readManifest()
                validateManifest(manifest)

                if (manifest.versionCode <= currentVersionCode) {
                    UpdateProviderCheck.UpToDate
                } else {
                    UpdateProviderCheck.Available(manifest.toRelease())
                }
            }.also { outcome ->
                post { result(outcome) }
            }
        }
    }

    override fun beginUpdate(release: UpdateRelease) {
        executor.execute {
            runCatching {
                val manifest = readManifest()
                validateManifest(manifest)
                require(manifest.versionCode == release.versionCode) {
                    "The available update changed. Check for updates again."
                }

                val updatesDir = File(activity.cacheDir, "updates").apply { mkdirs() }
                val finalFile = File(updatesDir, "wakemyway-${release.versionCode}.apk")
                val partialFile = File(updatesDir, "wakemyway-${release.versionCode}.apk.part")
                partialFile.delete()

                downloadApk(
                    url = manifest.apk.url,
                    destination = partialFile,
                    expectedSha256 = manifest.apk.sha256,
                    release = release,
                )
                verifyPackageIdentity(partialFile, release)

                if (finalFile.exists()) finalFile.delete()
                check(partialFile.renameTo(finalFile)) { "Could not finalize the downloaded update." }

                persistPending(release, finalFile)
                finalFile
            }.fold(
                onSuccess = {
                    post { onEvent(UpdateProviderEvent.ReadyToInstall(release)) }
                },
                onFailure = { error ->
                    post {
                        onEvent(
                            UpdateProviderEvent.Failed(
                                message = error.message ?: "Could not download the update.",
                                release = release,
                            ),
                        )
                    }
                },
            )
        }
    }

    override fun completeUpdate(release: UpdateRelease) {
        if (!packageManager.canRequestPackageInstalls()) {
            onEvent(UpdateProviderEvent.InstallPermissionRequired(release))
            return
        }

        executor.execute {
            val pending = loadPending()
            runCatching {
                require(pending != null && pending.release.versionCode == release.versionCode) {
                    "The downloaded update is no longer available."
                }
                val file = File(pending.path)
                require(file.isFile) { "The downloaded update file is missing." }
                verifyPackageIdentity(file, release)
                file
            }.fold(
                onSuccess = { file ->
                    post {
                        val uri = FileProvider.getUriForFile(
                            activity,
                            "${activity.packageName}.update-files",
                            file,
                        )
                        val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, APK_MIME_TYPE)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        runCatching {
                            activity.startActivity(intent)
                            onEvent(UpdateProviderEvent.Installing(release))
                        }.onFailure { error ->
                            onEvent(
                                UpdateProviderEvent.Failed(
                                    message = error.message ?: "Could not open Android's installer.",
                                    release = release,
                                ),
                            )
                        }
                    }
                },
                onFailure = { error ->
                    post {
                        onEvent(
                            UpdateProviderEvent.Failed(
                                message = error.message ?: "Could not verify the downloaded update.",
                                release = release,
                            ),
                        )
                    }
                },
            )
        }
    }

    override fun openInstallPermissionSettings() {
        activity.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"),
            ),
        )
    }

    override fun resume(currentVersionCode: Long) {
        val pending = loadPending() ?: return
        if (currentVersionCode >= pending.release.versionCode) {
            clearPending()
            return
        }

        executor.execute {
            runCatching {
                val file = File(pending.path)
                require(file.isFile) { "The downloaded update file is missing." }
                verifyPackageIdentity(file, pending.release)
            }.fold(
                onSuccess = {
                    post { onEvent(UpdateProviderEvent.ReadyToInstall(pending.release)) }
                },
                onFailure = {
                    clearPending()
                },
            )
        }
    }

    override fun handleActivityResult(resultCode: Int) = Unit

    override fun close() {
        executor.shutdownNow()
    }

    private fun readManifest(): DirectUpdateManifest {
        val connection = openHttps(MANIFEST_URL)
        return try {
            connection.inputStream.bufferedReader().use { reader ->
                json.decodeFromString<DirectUpdateManifest>(reader.readText())
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun validateManifest(manifest: DirectUpdateManifest) {
        require(manifest.schemaVersion == SUPPORTED_SCHEMA) { "Unsupported update metadata." }
        require(manifest.channel == EXPECTED_CHANNEL) { "Unexpected update channel." }
        require(manifest.versionCode > 0) { "Invalid update version." }
        require(manifest.versionName.isNotBlank()) { "Invalid update version name." }
        require(manifest.apk.url.startsWith("https://")) { "Update download must use HTTPS." }
        require(manifest.apk.sha256.matches(SHA_256_PATTERN)) { "Invalid update checksum." }
    }

    private fun downloadApk(
        url: String,
        destination: File,
        expectedSha256: String,
        release: UpdateRelease,
    ) {
        val connection = openHttps(url)
        val expectedBytes = connection.contentLengthLong.takeIf { it > 0L }
        val digest = MessageDigest.getInstance("SHA-256")
        var downloaded = 0L
        var lastReportedPercent = -1

        try {
            connection.inputStream.use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read

                        val percent = expectedBytes
                            ?.let { total -> ((downloaded * 100L) / total).toInt().coerceIn(0, 100) }
                        if (percent == null || percent >= lastReportedPercent + 2) {
                            lastReportedPercent = percent ?: lastReportedPercent
                            val progress = expectedBytes
                                ?.let { total -> (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) }
                            post { onEvent(UpdateProviderEvent.Downloading(release, progress)) }
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            connection.disconnect()
        }

        val actualSha256 = digest.digest().toHex()
        if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
            destination.delete()
            error("Downloaded update failed integrity verification.")
        }
    }

    private fun verifyPackageIdentity(
        apkFile: File,
        release: UpdateRelease,
    ) {
        val archive = requireNotNull(
            packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNING_CERTIFICATES,
            ),
        ) { "Downloaded file is not a valid Android package." }

        require(archive.packageName == activity.packageName) {
            "Downloaded update belongs to a different application."
        }
        require(archive.longVersionCode == release.versionCode) {
            "Downloaded update version does not match its metadata."
        }

        val installed = packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
        )
        val installedSigners = installed.signerDigests()
        val archiveSigners = archive.signerDigests()

        require(installedSigners.isNotEmpty() && archiveSigners.isNotEmpty()) {
            "Could not verify the update signing certificate."
        }
        require(installedSigners.intersect(archiveSigners).isNotEmpty()) {
            "Downloaded update is signed by a different key."
        }
    }

    private fun PackageInfo.signerDigests(): Set<String> {
        val signing = signingInfo ?: return emptySet()
        val signatures = if (signing.hasMultipleSigners()) {
            signing.apkContentsSigners
        } else {
            signing.signingCertificateHistory
        }
        return signatures
            .map { signature ->
                MessageDigest.getInstance("SHA-256")
                    .digest(signature.toByteArray())
                    .toHex()
            }
            .toSet()
    }

    private fun persistPending(
        release: UpdateRelease,
        file: File,
    ) {
        preferences.edit()
            .putString(KEY_PENDING_RELEASE, json.encodeToString(release))
            .putString(KEY_PENDING_PATH, file.absolutePath)
            .apply()
    }

    private fun loadPending(): PendingDirectUpdate? {
        val encoded = preferences.getString(KEY_PENDING_RELEASE, null) ?: return null
        val path = preferences.getString(KEY_PENDING_PATH, null) ?: return null
        val release = runCatching { json.decodeFromString<UpdateRelease>(encoded) }.getOrNull()
            ?: return null
        return PendingDirectUpdate(release, path)
    }

    private fun clearPending() {
        preferences.edit()
            .remove(KEY_PENDING_RELEASE)
            .remove(KEY_PENDING_PATH)
            .apply()
        File(activity.cacheDir, "updates").listFiles()?.forEach { it.delete() }
    }

    private fun openHttps(url: String): HttpsURLConnection {
        val connection = URL(url).openConnection() as? HttpsURLConnection
            ?: error("Update URL must use HTTPS.")
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json, application/octet-stream")
        connection.connect()
        require(
            connection.responseCode in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE,
        ) {
            "Update server returned HTTP ${connection.responseCode}."
        }
        return connection
    }

    private fun post(block: () -> Unit) {
        mainHandler.post(block)
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

    private data class PendingDirectUpdate(
        val release: UpdateRelease,
        val path: String,
    )

    @Serializable
    private data class DirectUpdateManifest(
        val schemaVersion: Int,
        val channel: String,
        val versionCode: Long,
        val versionName: String,
        val priority: Int = 0,
        val releaseNotes: List<String> = emptyList(),
        val apk: DirectApk,
    ) {
        fun toRelease(): UpdateRelease =
            UpdateRelease(
                source = UpdateSource.DIRECT,
                versionCode = versionCode,
                versionName = versionName,
                urgency = when (priority.coerceIn(0, 5)) {
                    4, 5 -> UpdateUrgency.CRITICAL
                    2, 3 -> UpdateUrgency.IMPORTANT
                    else -> UpdateUrgency.NORMAL
                },
                releaseNotes = releaseNotes,
            )
    }

    @Serializable
    private data class DirectApk(
        val url: String,
        val sha256: String,
    )

    private companion object {
        const val MANIFEST_URL =
            "https://github.com/yotamon/WakeMyWay/releases/latest/download/update.json"
        const val EXPECTED_CHANNEL = "stable"
        const val SUPPORTED_SCHEMA = 1
        const val PREFERENCES = "direct-update"
        const val KEY_PENDING_RELEASE = "pending-release"
        const val KEY_PENDING_PATH = "pending-path"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 30_000
        val SHA_256_PATTERN = Regex("^[a-fA-F0-9]{64}$")
    }
}

package com.wakemyway.app.alarm

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException

class CriticalWakeStore(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
) {
    private val protectedContext = context.createDeviceProtectedStorageContext()
    private val atomicFile = AtomicFile(
        File(protectedContext.noBackupFilesDir, fileName),
    )

    /**
     * Reads schema v2 critical state and transparently decodes the legacy schema-v1 single snapshot.
     * The next successful kernel mutation/reconciliation rewrites the same atomic file as v2.
     */
    @Synchronized
    fun read(): CriticalAlarmState? = try {
        atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
            CriticalAlarmState.decodeOrMigrate(reader.readText())
        }
    } catch (_: FileNotFoundException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IllegalStateException) {
        null
    } catch (_: org.json.JSONException) {
        null
    }

    @Synchronized
    fun write(state: CriticalAlarmState) {
        val stream = atomicFile.startWrite()
        try {
            stream.write(state.encode().toByteArray(Charsets.UTF_8))
            stream.flush()
            atomicFile.finishWrite(stream)
        } catch (error: Throwable) {
            atomicFile.failWrite(stream)
            throw error
        }
    }

    @Synchronized
    fun clear() {
        atomicFile.delete()
    }

    companion object {
        // Keep the legacy filename so upgrades can decode/migrate the existing scheduled wake.
        const val DEFAULT_FILE_NAME = "critical-wake-snapshot.json"
    }
}

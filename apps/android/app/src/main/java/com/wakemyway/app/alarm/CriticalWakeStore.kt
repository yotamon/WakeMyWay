package com.wakemyway.app.alarm

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException

sealed interface CriticalWakeReadResult {
    data class State(val value: CriticalAlarmState) : CriticalWakeReadResult
    data object Missing : CriticalWakeReadResult
    data class Corrupt(val reason: String) : CriticalWakeReadResult
}

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
     * Missing state and unreadable/corrupt state remain distinct for diagnostics while mutation paths
     * continue to fail closed through [read].
     */
    @Synchronized
    fun readResult(): CriticalWakeReadResult = try {
        val state = atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
            CriticalAlarmState.decodeOrMigrate(reader.readText())
        }
        CriticalWakeReadResult.State(state)
    } catch (_: FileNotFoundException) {
        CriticalWakeReadResult.Missing
    } catch (error: IllegalArgumentException) {
        CriticalWakeReadResult.Corrupt(error.message ?: "invalid critical wake state")
    } catch (error: IllegalStateException) {
        CriticalWakeReadResult.Corrupt(error.message ?: "invalid critical wake state")
    } catch (error: org.json.JSONException) {
        CriticalWakeReadResult.Corrupt(error.message ?: "invalid critical wake JSON")
    }

    /** Fail-closed compatibility accessor for Alarm Kernel mutation paths. */
    @Synchronized
    fun read(): CriticalAlarmState? = when (val result = readResult()) {
        is CriticalWakeReadResult.State -> result.value
        CriticalWakeReadResult.Missing,
        is CriticalWakeReadResult.Corrupt,
        -> null
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

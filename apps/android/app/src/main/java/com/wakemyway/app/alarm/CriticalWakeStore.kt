package com.wakemyway.app.alarm

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException

sealed interface CriticalWakeReadResult {
    data class Snapshot(val value: CriticalWakeSnapshot) : CriticalWakeReadResult
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

    @Synchronized
    fun readResult(): CriticalWakeReadResult = try {
        val snapshot = atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
            CriticalWakeSnapshot.decode(reader.readText())
        }
        CriticalWakeReadResult.Snapshot(snapshot)
    } catch (_: FileNotFoundException) {
        CriticalWakeReadResult.Missing
    } catch (error: IllegalArgumentException) {
        CriticalWakeReadResult.Corrupt(error.message ?: "invalid critical wake state")
    } catch (error: org.json.JSONException) {
        CriticalWakeReadResult.Corrupt(error.message ?: "invalid critical wake JSON")
    }

    /** Fail-closed compatibility accessor for Alarm Kernel mutation paths. */
    @Synchronized
    fun read(): CriticalWakeSnapshot? = when (val result = readResult()) {
        is CriticalWakeReadResult.Snapshot -> result.value
        CriticalWakeReadResult.Missing,
        is CriticalWakeReadResult.Corrupt,
        -> null
    }

    @Synchronized
    fun write(snapshot: CriticalWakeSnapshot) {
        val stream = atomicFile.startWrite()
        try {
            stream.write(snapshot.encode().toByteArray(Charsets.UTF_8))
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
        const val DEFAULT_FILE_NAME = "critical-wake-snapshot.json"
    }
}

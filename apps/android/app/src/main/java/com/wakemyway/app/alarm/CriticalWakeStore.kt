package com.wakemyway.app.alarm

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException

class CriticalWakeStore(context: Context) {
    private val protectedContext = context.createDeviceProtectedStorageContext()
    private val atomicFile = AtomicFile(
        File(protectedContext.noBackupFilesDir, FILE_NAME),
        "wake-my-way-critical-state",
    )

    @Synchronized
    fun read(): CriticalWakeSnapshot? = try {
        atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
            CriticalWakeSnapshot.decode(reader.readText())
        }
    } catch (_: FileNotFoundException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: org.json.JSONException) {
        null
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

    private companion object {
        const val FILE_NAME = "critical-wake-snapshot.json"
    }
}

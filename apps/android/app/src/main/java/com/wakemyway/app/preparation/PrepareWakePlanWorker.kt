package com.wakemyway.app.preparation

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.IOException

class PrepareWakePlanWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result = try {
        WakePreparationManager(applicationContext).prepareLatest()
        Result.success()
    } catch (_: IOException) {
        Result.retry()
    } catch (_: Throwable) {
        // Corrupt/unsupported private state must fail closed rather than retry forever.
        Result.failure()
    }
}

object PrepareWakePlanScheduler {
    private const val UNIQUE_WORK_NAME = "wmw-prepare-next-wake"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequest.Builder(PrepareWakePlanWorker::class.java).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

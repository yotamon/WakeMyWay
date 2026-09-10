package com.wakemyway.app

import android.app.Application
import androidx.work.Configuration
import com.wakemyway.app.alarm.AlarmPresentationAccess

/**
 * Keeps WorkManager off cold app/process startup so deferrable M6 preparation cannot add work ahead
 * of AlarmReceiver / Active Wake Execution. WorkManager initializes only on the first explicit
 * WorkManager.getInstance(Context) call from the preparation path.
 */
class WakeMyWayApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        // Create the user-visible alarm channel before readiness is evaluated. Android preserves
        // user changes to an existing channel, so AlarmKernel can then detect if priority was
        // lowered or the channel was disabled and route the user to repair it before bedtime.
        AlarmPresentationAccess.ensureChannel(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}

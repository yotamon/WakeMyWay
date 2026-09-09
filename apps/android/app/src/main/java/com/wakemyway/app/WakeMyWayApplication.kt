package com.wakemyway.app

import android.app.Application
import androidx.work.Configuration

/**
 * Keeps WorkManager off cold app/process startup so deferrable M6 preparation cannot add work ahead
 * of AlarmReceiver / Active Wake Execution. WorkManager initializes only on the first explicit
 * WorkManager.getInstance(Context) call from the preparation path.
 */
class WakeMyWayApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}

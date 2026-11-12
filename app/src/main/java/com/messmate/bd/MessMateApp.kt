package com.messmate.bd

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.messmate.bd.data.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Besides the usual Hilt bootstrap it configures **WorkManager** with the Hilt
 * worker factory (so [com.messmate.bd.data.work.SyncWorker] gets its
 * dependencies injected) and schedules the periodic background sync.
 */
@HiltAndroidApp
class MessMateApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    /**
     * WorkManager configuration – the default initializer is removed in the
     * manifest, so this is the only place where it is configured.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Keep the sheet in sync even when the app is not opened for days.
        syncScheduler.schedulePeriodic()
    }
}

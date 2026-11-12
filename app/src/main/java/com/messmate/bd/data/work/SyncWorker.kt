package com.messmate.bd.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.messmate.bd.core.network.NetworkMonitor
import com.messmate.bd.data.repository.MessRepositoryImpl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes the queued changes to the Google Sheet and pulls the changes of the
 * other members back into the local cache.
 *
 * The worker is the safety net for everything that happened while the device
 * was offline. It runs
 *
 * 1. with a network constraint (`CONNECTED`), and
 * 2. periodically (every 15 minutes – the minimum WorkManager allows).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MessRepositoryImpl,
    private val networkMonitor: NetworkMonitor,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!networkMonitor.isOnline()) return Result.retry()

        // 1. Push everything that is still queued.
        val flushed = repository.flushQueue()
        if (flushed.isFailure) return Result.retry()

        // 2. Pull the changes of the other members.
        val refreshed = repository.refresh()
        return if (refreshed.isSuccess) Result.success() else Result.retry()
    }

    companion object {

        private const val PERIODIC_WORK_NAME = "messmate-periodic-sync"
        private const val ONETIME_WORK_NAME = "messmate-sync-now"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Schedules the recurring sync. Safe to call multiple times. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Triggers an immediate sync (e.g. after the app comes to the foreground). */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}

/** Convenience helper so the UI can trigger a sync with one line. */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun syncNow() = SyncWorker.syncNow(context)
    fun schedulePeriodic() = SyncWorker.schedulePeriodic(context)
}

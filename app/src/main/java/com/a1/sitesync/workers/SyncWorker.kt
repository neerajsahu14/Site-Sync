package com.a1.sitesync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.a1.sitesync.data.repository.SiteSyncRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * A CoroutineWorker responsible for performing the bi-directional data synchronization.
 * WorkManager will execute this task in the background, respecting battery optimizations
 * and retrying on failure.
 *
 * @param appContext The application context.
 * @param workerParams Parameters for the worker.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    // Inject repository via Koin
    private val repository: SiteSyncRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            // Perform the full synchronization logic
            repository.performDataSync()
            Result.success()
        } catch (e: Exception) {
            // If any part of the sync fails, retry the work later
            Result.retry()
        }
    }
}

// Placeholder for your Application class where you'd initialize the database
// and potentially WorkManager scheduling.
// abstract class YourApplicationClass : android.app.Application() {
//     abstract val database: YourAppDatabase
// }

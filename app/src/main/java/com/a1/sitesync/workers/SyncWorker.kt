package com.a1.sitesync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.a1.sitesync.data.repository.SiteSyncRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * A background worker that syncs a single survey to the cloud.
 * This worker is designed to be triggered by WorkManager.
 */
class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    // Inject the repository using Koin
    private val siteSyncRepository: SiteSyncRepository by inject()

    companion object {
        const val KEY_SURVEY_ID = "KEY_SURVEY_ID"
    }

    override suspend fun doWork(): Result {
        // Get the survey ID from the input data
        val surveyId = inputData.getString(KEY_SURVEY_ID)

        return try {
            if (surveyId.isNullOrBlank()) {
                // If no specific survey ID, perform a full data sync.
                siteSyncRepository.performDataSync()
            } else {
                // If a specific survey ID is provided, sync only that survey.
                siteSyncRepository.syncSurveyById(surveyId)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // If there's an error, retry the work later
            Result.retry()
        }
    }
}

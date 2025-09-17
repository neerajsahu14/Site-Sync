package com.a1.sitesync.data.repository

import android.content.Context
import androidx.core.content.FileProvider
import androidx.work.*
import com.a1.sitesync.data.database.dao.SurveyDao
import com.a1.sitesync.data.database.model.Dimensions
import com.a1.sitesync.data.database.model.Photo
import com.a1.sitesync.data.database.model.Provisions
import com.a1.sitesync.data.database.model.Survey
import com.a1.sitesync.data.database.model.SurveyWithPhotos
import com.a1.sitesync.data.models.FirestoreSurvey
import com.a1.sitesync.data.models.SurveyDimensions
import com.a1.sitesync.data.models.SurveyPhoto
import com.a1.sitesync.data.models.SurveyProvisions
import com.a1.sitesync.data.service.FirebaseSyncService
import com.a1.sitesync.workers.SyncWorker
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Date
import java.util.UUID

class SiteSyncRepository(
    private val context: Context,
    private val surveyDao: SurveyDao,
    private val firebaseService: FirebaseSyncService
) {

    private val workManager = WorkManager.getInstance(context)

    // --- Local Data Creation and Modification ---

    suspend fun createNewSurvey(
        surveyorId: String, clientName: String, siteAddress: String?, gateType: String,
        dimensions: Dimensions, provisions: Provisions, openingDirection: String?,
        recommendedGate: String?, photoPaths: List<String>, latitude: Double?, longitude: Double?
    ): String {
        val newSurveyId = UUID.randomUUID().toString()
        val survey = Survey(
            surveyId = newSurveyId, surveyorId = surveyorId, clientName = clientName,
            siteAddress = siteAddress, latitude = latitude, longitude = longitude, gateType = gateType,
            dimensions = dimensions, provisions = provisions, openingDirection = openingDirection,
            recommendedGate = recommendedGate, status = "pending", isSynced = false, createdAt = Date()
        )
        val photos = photoPaths.map { path ->
            Photo(UUID.randomUUID().toString(), newSurveyId, path, null, isSuperimposed = false, Date())
        }
        surveyDao.insertSurveyWithPhotos(survey, photos)
        scheduleSyncWorker(newSurveyId)
        return newSurveyId
    }

    suspend fun updateSurvey(survey: Survey) {
        surveyDao.updateSurvey(survey)
        scheduleSyncWorker(survey.surveyId)
    }

    suspend fun deleteSurvey(survey: Survey) {
        surveyDao.deleteSurvey(survey)
        // Optionally, you might want to schedule a worker to delete from the cloud too.
    }

    suspend fun addPhotoToSurvey(surveyId: String, localFilePath: String, isSuperimposed: Boolean) {
        val photo = Photo(UUID.randomUUID().toString(), surveyId, localFilePath, null, isSuperimposed, Date())
        surveyDao.insertPhotos(listOf(photo))
        scheduleSyncWorker(surveyId)
    }

    suspend fun deletePhotoById(photoId: String) {
        val survey = surveyDao.getSurveyByPhotoId(photoId)
        surveyDao.deletePhotoById(photoId)
        survey?.let { scheduleSyncWorker(it.surveyId) }
    }

    // --- Synchronization Logic ---

    private fun scheduleSyncWorker(surveyId: String) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(SyncWorker.KEY_SURVEY_ID to surveyId))
            .build()
        workManager.enqueueUniqueWork("sync_survey_$surveyId", ExistingWorkPolicy.REPLACE, workRequest)
    }

    suspend fun performDataSync() {
        uploadUnsyncedLocalChanges()
        downloadRemoteChanges()
    }

    suspend fun syncSurveyById(surveyId: String) {
        val surveyWithPhotos = surveyDao.getSurveyById(surveyId).first()
        uploadSurvey(surveyWithPhotos)
    }

    private suspend fun uploadUnsyncedLocalChanges() {
        val unsyncedSurveys = surveyDao.getUnsyncedSurveys()
        unsyncedSurveys.forEach { uploadSurvey(it) }
    }

    private suspend fun uploadSurvey(surveyWithPhotos: SurveyWithPhotos) {
        val survey = surveyWithPhotos.survey
        val photos = surveyWithPhotos.photos
        val uploadedPhotos = photos.map { photo ->
            val localFile = File(photo.localFilePath)
            if (!localFile.exists()) return@map null // Skip if file doesn't exist
            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", localFile)
            val cloudUrl = firebaseService.uploadFile(fileUri, survey.surveyId, localFile.name)
            SurveyPhoto(photo.photoId, cloudUrl, photo.isSuperimposed, photo.capturedAt)
        }.filterNotNull()

        val firestoreSurvey = FirestoreSurvey(
            surveyId = survey.surveyId, surveyorId = survey.surveyorId, clientName = survey.clientName,
            siteAddress = survey.siteAddress, gateType = survey.gateType, openingDirection = survey.openingDirection,
            recommendedGate = survey.recommendedGate, photos = uploadedPhotos,
            location = if (survey.latitude != null && survey.longitude != null) GeoPoint(survey.latitude, survey.longitude) else null,
            dimensions = SurveyDimensions(survey.dimensions.clearOpeningWidth, survey.dimensions.requiredHeight, survey.dimensions.parkingSpaceLength, survey.dimensions.openingAngleLeaf),
            provisions = SurveyProvisions(survey.provisions.hasCabling, survey.provisions.hasStorage)
        )

        firebaseService.uploadSurveyDocument(firestoreSurvey)
        surveyDao.markSurveyAsSynced(survey.surveyId)
    }

    private suspend fun downloadRemoteChanges() {
        val remoteIds = firebaseService.fetchAllSurveyIds().toSet()
        val localIds = surveyDao.getAllSurveyIds().toSet()

        val staleLocalIds = localIds - remoteIds
        staleLocalIds.forEach { surveyDao.deleteSurveyById(it) }

        val remoteSurveys = firebaseService.fetchAllSurveys()
        remoteSurveys.forEach { fs ->
            val survey = Survey(
                surveyId = fs.surveyId, surveyorId = fs.surveyorId, clientName = fs.clientName,
                siteAddress = fs.siteAddress, latitude = fs.location?.latitude, longitude = fs.location?.longitude,
                gateType = fs.gateType, dimensions = Dimensions(fs.dimensions.clearOpeningWidth, fs.dimensions.requiredHeight, fs.dimensions.parkingSpaceLength, fs.dimensions.openingAngleLeaf),
                provisions = Provisions(fs.provisions.hasCabling, fs.provisions.hasStorage), openingDirection = fs.openingDirection,
                recommendedGate = fs.recommendedGate, status = fs.status, isSynced = true, createdAt = fs.createdAt ?: Date()
            )

            val photos = fs.photos.mapNotNull { sp ->
                if (sp.cloudStorageUrl.isNotBlank()) {
                    try {
                        val localFile = firebaseService.downloadFile(sp.cloudStorageUrl)
                        Photo(sp.photoId, fs.surveyId, localFile.absolutePath, sp.cloudStorageUrl, sp.isSuperimposed, sp.capturedAt)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else { null }
            }
            surveyDao.insertSurveyWithPhotos(survey, photos)
        }
    }

    // --- Local Data Access ---
    fun getSurveyById(surveyId: String): Flow<SurveyWithPhotos> = surveyDao.getSurveyById(surveyId)

    // Re-added for SiteSyncService compatibility
    suspend fun getAllSurveys(): List<SurveyWithPhotos> = surveyDao.getPagedSurveys(1000, 0) // A temporary implementation
    suspend fun getPagedSurveys(page: Int, pageSize: Int): List<SurveyWithPhotos> {
        val offset = (page - 1) * pageSize
        return surveyDao.getPagedSurveys(pageSize, offset)
    }

    // --- Online Methods (delegating to FirebaseSyncService) ---
    suspend fun upsertSurveyOnline(firestoreSurvey: FirestoreSurvey) = firebaseService.uploadSurveyDocument(firestoreSurvey)
    suspend fun getAllSurveysOnline(): List<FirestoreSurvey> = firebaseService.fetchAllSurveys()
    suspend fun getSurveyByIdOnline(surveyId: String): FirestoreSurvey? = null // This would require a new service method
    suspend fun deleteSurveyOnline(surveyId: String) {} // This would require a new service method
}
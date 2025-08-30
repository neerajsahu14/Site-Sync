package com.a1.sitesync.data.repository

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
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * The Repository now orchestrates data flow between the local Room DB
 * and the remote Firebase services.
 *
 * @param surveyDao The Data Access Object for surveys.
 * @param firebaseService The service for Firebase communications.
 */
class SiteSyncRepository(
    private val surveyDao: SurveyDao,
    private val firebaseService: FirebaseSyncService,
    private val firebaseSyncRepository: FirebaseSyncRepository
) {
    suspend fun createNewSurvey(
        // ... (same parameters as before)
        surveyorId: String,
        clientName: String,
        siteAddress: String?,
        gateType: String,
        dimensions: Dimensions,
        provisions: Provisions,
        openingDirection: String?,
        recommendedGate: String?,
        photoPaths: List<String>
    ) {
        // ... (same implementation as before to save locally)
        val newSurveyId = UUID.randomUUID().toString()

        val survey = Survey(
            surveyId = newSurveyId,
            surveyorId = surveyorId,
            clientName = clientName,
            siteAddress = siteAddress,
            latitude = null,
            longitude = null,
            gateType = gateType,
            dimensions = dimensions,
            provisions = provisions,
            openingDirection = openingDirection,
            recommendedGate = recommendedGate,
            status = "Completed",
            isSynced = false,
            createdAt = Date()
        )

        val photos = photoPaths.map { path ->
            Photo(
                photoId = UUID.randomUUID().toString(),
                surveyIdRef = newSurveyId,
                localFilePath = path,
                cloudStorageUrl = null,
                isSuperimposed = false,
                capturedAt = Date()
            )
        }

        surveyDao.insertSurveyWithPhotos(survey, photos)
    }

    /**
     * Performs a full bi-directional data sync.
     */
    suspend fun performDataSync() {
        // 1. Upstream Sync (Local -> Cloud)
        uploadUnsyncedSurveys()

        // 2. Downstream Sync (Cloud -> Local)
        downloadRemoteSurveys()
    }

    /**
     * Handles the upload of locally created/updated surveys to Firebase.
     */
    private suspend fun uploadUnsyncedSurveys() {
        val unsyncedSurveys = surveyDao.getUnsyncedSurveys()

        unsyncedSurveys.forEach { surveyWithPhotos ->
            val survey = surveyWithPhotos.survey
            val photos = surveyWithPhotos.photos

            // 1. Upload all photos for the survey to Cloud Storage
            val uploadedPhotos = photos.map { photo ->
                val localFile = File(photo.localFilePath)
                val cloudUrl = firebaseService.uploadFile(localFile, survey.surveyId)
                SurveyPhoto(
                    photoId = photo.photoId,
                    cloudStorageUrl = cloudUrl,
                    isSuperimposed = photo.isSuperimposed
                    // 'capturedAt' will be set by server timestamp
                )
            }

            // 2. Prepare the Firestore document
            val firestoreSurvey = FirestoreSurvey(
                surveyId = survey.surveyId,
                surveyorId = survey.surveyorId,
                clientName = survey.clientName,
                siteAddress = survey.siteAddress,
                gateType = survey.gateType,
                dimensions = SurveyDimensions(
                    clearOpeningWidth = survey.dimensions.clearOpeningWidth,
                    requiredHeight = survey.dimensions.requiredHeight,
                    parkingSpaceLength = survey.dimensions.parkingSpaceLength,
                    openingAngleLeaf = survey.dimensions.openingAngleLeaf
                ),
                provisions = SurveyProvisions(
                    hasCabling = survey.provisions.hasCabling,
                    hasStorage = survey.provisions.hasStorage
                ),
                openingDirection = survey.openingDirection,
                recommendedGate = survey.recommendedGate,
                photos = uploadedPhotos
            )

            // 3. Upload the document to Firestore
            firebaseService.uploadSurveyDocument(firestoreSurvey)

            // 4. Mark the local survey as synced
            surveyDao.markSurveyAsSynced(survey.surveyId)
        }
    }

    /**
     * Fetches surveys from Firestore and updates local DB
     */
    private suspend fun downloadRemoteSurveys() {
        val remoteSurveys = firebaseService.fetchAllSurveys()
        remoteSurveys.forEach { fs ->
            val survey = Survey(
                surveyId = fs.surveyId,
                surveyorId = fs.surveyorId,
                clientName = fs.clientName,
                siteAddress = fs.siteAddress,
                latitude = fs.location?.latitude,
                longitude = fs.location?.longitude,
                gateType = fs.gateType,
                dimensions = Dimensions(
                    clearOpeningWidth = fs.dimensions.clearOpeningWidth,
                    requiredHeight = fs.dimensions.requiredHeight,
                    parkingSpaceLength = fs.dimensions.parkingSpaceLength,
                    openingAngleLeaf = fs.dimensions.openingAngleLeaf
                ),
                provisions = Provisions(
                    hasCabling = fs.provisions.hasCabling,
                    hasStorage = fs.provisions.hasStorage
                ),
                openingDirection = fs.openingDirection,
                recommendedGate = fs.recommendedGate,
                status = fs.status,
                isSynced = true,
                createdAt = fs.createdAt ?: Date()
            )
            val photos = fs.photos.map { sp ->
                Photo(
                    photoId = sp.photoId,
                    surveyIdRef = fs.surveyId,
                    localFilePath = "", // Remote photos, no local file
                    cloudStorageUrl = sp.cloudStorageUrl,
                    isSuperimposed = sp.isSuperimposed,
                    capturedAt = sp.capturedAt ?: Date()
                )
            }
            surveyDao.insertSurveyWithPhotos(survey, photos)
        }
    }

    /** CRUD operations (OFFLINE - Room) **/
    fun getAllSurveys(): Flow<List<SurveyWithPhotos>> = surveyDao.getAllSurveys()

    fun getSurveyById(surveyId: String): Flow<SurveyWithPhotos> = surveyDao.getSurveyById(surveyId)

    suspend fun updateSurvey(survey: Survey) = surveyDao.updateSurvey(survey)

    suspend fun deleteSurvey(survey: Survey) = surveyDao.deleteSurvey(survey)

    /** CRUD operations (ONLINE - Firestore) **/
    suspend fun upsertSurveyOnline(firestoreSurvey: FirestoreSurvey) =
        firebaseSyncRepository.upsertSurvey(firestoreSurvey)

    suspend fun getAllSurveysOnline(): List<FirestoreSurvey> =
        firebaseSyncRepository.getAllSurveys()

    suspend fun getSurveyByIdOnline(surveyId: String): FirestoreSurvey? =
        firebaseSyncRepository.getSurveyById(surveyId)

    suspend fun deleteSurveyOnline(surveyId: String) =
        firebaseSyncRepository.deleteSurvey(surveyId)

    /**
     * Adds a photo entry to an existing survey.
     */
    suspend fun addPhotoToSurvey(surveyId: String, localFilePath: String) {
        val photo = Photo(
            photoId = UUID.randomUUID().toString(),
            surveyIdRef = surveyId,
            localFilePath = localFilePath,
            cloudStorageUrl = null,
            isSuperimposed = false,
            capturedAt = Date()
        )
        surveyDao.insertPhotos(listOf(photo))
    }
}
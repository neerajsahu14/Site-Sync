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
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Date
import java.util.UUID

class SiteSyncRepository(
    private val surveyDao: SurveyDao,
    private val firebaseService: FirebaseSyncService,
    private val firebaseSyncRepository: FirebaseSyncRepository
) {

    suspend fun createNewSurvey(
        surveyorId: String,
        clientName: String,
        siteAddress: String?,
        gateType: String,
        dimensions: Dimensions,
        provisions: Provisions,
        openingDirection: String?,
        recommendedGate: String?,
        photoPaths: List<String>,
        latitude: Double?,
        longitude: Double?
    ): String {

        val newSurveyId = UUID.randomUUID().toString()
        val survey = Survey(
            surveyId = newSurveyId,
            surveyorId = surveyorId,
            clientName = clientName,
            siteAddress = siteAddress,
            latitude = latitude,
            longitude = longitude,
            gateType = gateType,
            dimensions = dimensions,
            provisions = provisions,
            openingDirection = openingDirection,
            recommendedGate = recommendedGate,
            status = "pending",
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
        return newSurveyId
    }

    suspend fun performDataSync() {
        uploadUnsyncedSurveys()
        downloadRemoteSurveys()
    }

    suspend fun syncSurveyById(surveyId: String) {
        val surveyWithPhotos = surveyDao.getSurveyById(surveyId).first()
        uploadSurvey(surveyWithPhotos)
    }

    private suspend fun uploadUnsyncedSurveys() {
        val unsyncedSurveys = surveyDao.getUnsyncedSurveys()
        unsyncedSurveys.forEach { surveyWithPhotos ->
            uploadSurvey(surveyWithPhotos)
        }
    }

    private suspend fun uploadSurvey(surveyWithPhotos: SurveyWithPhotos) {
        val survey = surveyWithPhotos.survey
        val photos = surveyWithPhotos.photos

        val uploadedPhotos = photos.map { photo ->
            val localFile = File(photo.localFilePath)
            val cloudUrl = firebaseService.uploadFile(localFile, survey.surveyId)
            SurveyPhoto(
                photoId = photo.photoId,
                cloudStorageUrl = cloudUrl,
                isSuperimposed = photo.isSuperimposed,
                capturedAt = photo.capturedAt
            )
        }

        val firestoreSurvey = FirestoreSurvey(
            surveyId = survey.surveyId,
            surveyorId = survey.surveyorId,
            clientName = survey.clientName,
            siteAddress = survey.siteAddress,
            location = if (survey.latitude != null && survey.longitude != null) GeoPoint(survey.latitude, survey.longitude) else null,
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

        firebaseService.uploadSurveyDocument(firestoreSurvey)
        surveyDao.markSurveyAsSynced(survey.surveyId)
    }

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
                    localFilePath = "",
                    cloudStorageUrl = sp.cloudStorageUrl,
                    isSuperimposed = sp.isSuperimposed,
                    capturedAt = sp.capturedAt
                )
            }
            surveyDao.insertSurveyWithPhotos(survey, photos)
        }
    }

    fun getAllSurveys(): Flow<List<SurveyWithPhotos>> = surveyDao.getAllSurveys()

    fun getSurveyById(surveyId: String): Flow<SurveyWithPhotos> = surveyDao.getSurveyById(surveyId)

    suspend fun updateSurvey(survey: Survey) = surveyDao.updateSurvey(survey)

    suspend fun deleteSurvey(survey: Survey) = surveyDao.deleteSurvey(survey)

    suspend fun upsertSurveyOnline(firestoreSurvey: FirestoreSurvey) =
        firebaseSyncRepository.upsertSurvey(firestoreSurvey)

    suspend fun getAllSurveysOnline(): List<FirestoreSurvey> =
        firebaseSyncRepository.getAllSurveys()

    suspend fun getSurveyByIdOnline(surveyId: String): FirestoreSurvey? =
        firebaseSyncRepository.getSurveyById(surveyId)

    suspend fun deleteSurveyOnline(surveyId: String) =
        firebaseSyncRepository.deleteSurvey(surveyId)

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
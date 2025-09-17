package com.a1.sitesync.data.service

import com.a1.sitesync.data.database.model.Survey
import com.a1.sitesync.data.database.model.SurveyWithPhotos
import com.a1.sitesync.data.models.FirestoreSurvey
import com.a1.sitesync.data.repository.SiteSyncRepository
import kotlinx.coroutines.flow.Flow

class SiteSyncService(
    private val repository: SiteSyncRepository
) {
    // OFFLINE (Room)
    suspend fun getAllSurveys(): Flow<List<SurveyWithPhotos>> = repository.getAllSurveys() as Flow<List<SurveyWithPhotos>>
    fun getSurveyById(surveyId: String): Flow<SurveyWithPhotos> = repository.getSurveyById(surveyId)
    suspend fun updateSurvey(survey: Survey) = repository.updateSurvey(survey)
    suspend fun deleteSurvey(survey: Survey) = repository.deleteSurvey(survey)

    // ONLINE (Firestore)
    suspend fun upsertSurveyOnline(firestoreSurvey: FirestoreSurvey) = repository.upsertSurveyOnline(firestoreSurvey)
    suspend fun getAllSurveysOnline(): List<FirestoreSurvey> = repository.getAllSurveysOnline()
    suspend fun getSurveyByIdOnline(surveyId: String): FirestoreSurvey? = repository.getSurveyByIdOnline(surveyId)
    suspend fun deleteSurveyOnline(surveyId: String) = repository.deleteSurveyOnline(surveyId)
}
package com.a1.sitesync.data.database.dao

import androidx.room.*
import com.a1.sitesync.data.database.model.Photo
import com.a1.sitesync.data.database.model.Survey
import com.a1.sitesync.data.database.model.SurveyWithPhotos
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: Survey): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<Photo>)

    @Transaction
    suspend fun insertSurveyWithPhotos(survey: Survey, photos: List<Photo>) {
        insertSurvey(survey)
        insertPhotos(photos)
    }

    @Transaction
    @Query("SELECT * FROM surveys WHERE is_synced = 0")
    suspend fun getUnsyncedSurveys(): List<SurveyWithPhotos>

    @Query("UPDATE surveys SET is_synced = 1 WHERE survey_id = :surveyId")
    suspend fun markSurveyAsSynced(surveyId: String)

    @Transaction
    @Query("SELECT * FROM surveys")
    fun getAllSurveys(): Flow<List<SurveyWithPhotos>>

    @Transaction
    @Query("SELECT * FROM surveys WHERE survey_id = :surveyId")
    fun getSurveyById(surveyId: String): Flow<SurveyWithPhotos>

    @Delete
    suspend fun deleteSurvey(survey: Survey)

    @Update
    suspend fun updateSurvey(survey: Survey)

    @Update
    suspend fun updatePhotos(photos: List<Photo>)
}
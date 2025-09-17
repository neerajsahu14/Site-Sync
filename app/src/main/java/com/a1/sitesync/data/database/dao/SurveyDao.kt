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
    @Query("SELECT * FROM surveys ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedSurveys(limit: Int, offset: Int): List<SurveyWithPhotos>

    @Transaction
    @Query("SELECT * FROM surveys WHERE survey_id = :surveyId")
    fun getSurveyById(surveyId: String): Flow<SurveyWithPhotos>

    @Query("SELECT s.* FROM surveys s JOIN photos p ON s.survey_id = p.survey_id_ref WHERE p.photo_id = :photoId")
    suspend fun getSurveyByPhotoId(photoId: String): Survey?

    @Query("SELECT survey_id FROM surveys")
    suspend fun getAllSurveyIds(): List<String>

    @Delete
    suspend fun deleteSurvey(survey: Survey)

    @Query("DELETE FROM surveys WHERE survey_id = :surveyId")
    suspend fun deleteSurveyById(surveyId: String)

    @Update
    suspend fun updateSurvey(survey: Survey)

    @Update
    suspend fun updatePhotos(photos: List<Photo>)

    @Query("DELETE FROM photos WHERE photo_id = :photoId")
    suspend fun deletePhotoById(photoId: String)
}
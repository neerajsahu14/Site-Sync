package com.a1.sitesync.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.a1.sitesync.data.database.dao.SurveyDao
import com.a1.sitesync.data.database.model.Photo
import com.a1.sitesync.data.database.model.Survey

/**
 * Room database for SiteSync app storing surveys and photos.
 */
@Database(entities = [Survey::class, Photo::class], version = 1, exportSchema = false)
@TypeConverters(com.a1.sitesync.data.database.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
}

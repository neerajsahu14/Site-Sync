package com.a1.sitesync.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = Survey::class,
            parentColumns = ["survey_id"],
            childColumns = ["survey_id_ref"],
            onDelete = ForeignKey.CASCADE // If a survey is deleted, its photos are also deleted
        )
    ]
)
data class Photo(
    @PrimaryKey
    @ColumnInfo(name = "photo_id")
    val photoId: String,

    @ColumnInfo(name = "survey_id_ref", index = true)
    val surveyIdRef: String,

    @ColumnInfo(name = "local_file_path")
    val localFilePath: String,

    @ColumnInfo(name = "cloud_storage_url")
    var cloudStorageUrl: String?,

    @ColumnInfo(name = "is_superimposed")
    val isSuperimposed: Boolean,

    @ColumnInfo(name = "captured_at")
    val capturedAt: Date
)
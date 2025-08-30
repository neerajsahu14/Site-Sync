package com.a1.sitesync.data.database.model

import androidx.room.Embedded
import androidx.room.Relation

data class SurveyWithPhotos(
    @Embedded
    val survey: Survey,

    @Relation(
        parentColumn = "survey_id",
        entityColumn = "survey_id_ref"
    )
    val photos: List<Photo>
)
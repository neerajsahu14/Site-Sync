package com.a1.sitesync.data.models

import java.util.Date

data class SurveyPhoto(
    val photoId: String = "",
    val cloudStorageUrl: String = "",
    val isSuperimposed: Boolean = false,
    val capturedAt: Date = Date()
)
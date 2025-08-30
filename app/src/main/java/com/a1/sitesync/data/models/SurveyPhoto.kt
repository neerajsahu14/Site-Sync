package com.a1.sitesync.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SurveyPhoto(
    val photoId: String = "",
    val cloudStorageUrl: String = "",
    val isSuperimposed: Boolean = false,
    @ServerTimestamp
    val capturedAt: Date? = null
)
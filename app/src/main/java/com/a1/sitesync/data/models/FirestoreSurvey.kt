package com.a1.sitesync.data.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FirestoreSurvey(
    // Fields must have default values for Firestore's automatic data mapping.
    val surveyId: String = "",
    val surveyorId: String = "",
    val clientName: String = "",
    val siteAddress: String? = null,
    val location: GeoPoint? = null,
    val gateType: String = "",
    val dimensions: SurveyDimensions = SurveyDimensions(),
    val provisions: SurveyProvisions = SurveyProvisions(),
    val openingDirection: String? = null,
    val recommendedGate: String? = null,
    val status: String = "Completed",
    val photos: List<SurveyPhoto> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null
)
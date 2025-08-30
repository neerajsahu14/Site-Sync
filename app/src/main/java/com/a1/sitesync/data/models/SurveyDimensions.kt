package com.a1.sitesync.data.models

data class SurveyDimensions(
    val clearOpeningWidth: Double = 0.0,
    val requiredHeight: Double = 0.0,
    val parkingSpaceLength: Double? = null,
    val openingAngleLeaf: Int? = null
)
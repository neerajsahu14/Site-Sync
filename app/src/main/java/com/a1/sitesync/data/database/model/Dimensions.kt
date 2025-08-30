package com.a1.sitesync.data.database.model

import androidx.room.ColumnInfo

data class Dimensions(
    @ColumnInfo(name = "clear_opening_width")
    val clearOpeningWidth: Double = 0.0,

    @ColumnInfo(name = "required_height")
    val requiredHeight: Double = 0.0,

    @ColumnInfo(name = "parking_space_length")
    val parkingSpaceLength: Double? = null, // Specific to Sliding gates

    @ColumnInfo(name = "opening_angle_leaf")
    val openingAngleLeaf: Int? = null // Specific to Swing gates
)
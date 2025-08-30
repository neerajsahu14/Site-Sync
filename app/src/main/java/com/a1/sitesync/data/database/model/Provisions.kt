package com.a1.sitesync.data.database.model

import androidx.room.ColumnInfo

data class Provisions(
    @ColumnInfo(name = "has_cabling")
    val hasCabling: Boolean = false,

    @ColumnInfo(name = "has_storage")
    val hasStorage: Boolean = false
)
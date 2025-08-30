package com.a1.sitesync.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Represents the main survey record. The table name is 'surveys'.
@Entity(tableName = "surveys")
data class Survey(
    @PrimaryKey
    @ColumnInfo(name = "survey_id")
    val surveyId: String,

    @ColumnInfo(name = "surveyor_id")
    val surveyorId: String,

    @ColumnInfo(name = "client_name")
    val clientName: String,

    @ColumnInfo(name = "site_address")
    val siteAddress: String?,

    val latitude: Double?,
    val longitude: Double?,

    @ColumnInfo(name = "gate_type")
    val gateType: String,

    // Embeds the Dimensions object directly into the surveys table
    @Embedded
    val dimensions: Dimensions,

    // Embeds the Provisions object
    @Embedded
    val provisions: Provisions,

    @ColumnInfo(name = "opening_direction")
    val openingDirection: String?,

    @ColumnInfo(name = "recommended_gate")
    val recommendedGate: String?,

    var status: String,

    // Flag to track if the record needs to be synced with Firestore
    @ColumnInfo(name = "is_synced", defaultValue = "0")
    var isSynced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Date
)

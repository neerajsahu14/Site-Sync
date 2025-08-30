package com.a1.sitesync.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.a1.sitesync.data.database.model.Dimensions
import com.a1.sitesync.data.database.model.Provisions
import java.util.Date

/**
 * Type converters to allow Room to reference complex data types.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun dimensionsToJson(dimensions: Dimensions?): String? {
        return dimensions?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun jsonToDimensions(json: String?): Dimensions? {
        return json?.let { gson.fromJson(it, Dimensions::class.java) }
    }

    @TypeConverter
    fun provisionsToJson(provisions: Provisions?): String? {
        return provisions?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun jsonToProvisions(json: String?): Provisions? {
        return json?.let { gson.fromJson(it, Provisions::class.java) }
    }
}

package com.app.railnav.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromEdgeGeometry(value: List<List<Double>>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toEdgeGeometry(value: String): List<List<Double>> {
        return json.decodeFromString(value)
    }
}
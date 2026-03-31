package com.app.railnav.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    // 1. Converter for Edge Geometry: List<List<Double>> -> String
    @TypeConverter
    fun fromGeometry(value: String): List<List<Double>> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toGeometry(list: List<List<Double>>): String {
        return Json.encodeToString(list)
    }

    // 2. Converter for Node Coordinates: List<Double> -> String (Useful for NodeEntity)
    @TypeConverter
    fun fromCoordinates(value: String): List<Double> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toCoordinates(list: List<Double>): String {
        return Json.encodeToString(list)
    }
}
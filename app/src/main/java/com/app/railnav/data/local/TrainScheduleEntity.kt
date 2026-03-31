package com.app.railnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "train_schedules")
data class TrainScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trainNumber: String,       // e.g., "11029"
    val stationCode: String,       // e.g., "PUNE"
    val stationName: String,       // e.g., "PUNE JN"
    val platformNumber: Int,       // e.g., 4
    val arrivalTimeMin: Int,       // e.g., 765
    val dayCount: Int,             // e.g., 1
    val latitude: Double?,
    val longitude: Double?
)
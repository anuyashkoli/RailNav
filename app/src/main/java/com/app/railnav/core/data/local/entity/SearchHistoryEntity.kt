package com.app.railnav.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchType: String, // "PNR", "LIVETRAIN", "STATION", "SCHEDULE"
    val timestamp: Long = System.currentTimeMillis()
)

package com.app.railnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edges")
data class EdgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val edgeId: String,           // Maps to edge_id
    val startNodeId: Int,         // Maps to start_id (converted to Int)
    val endNodeId: Int,           // Maps to end_id (converted to Int)
    val distance: Double,         // Maps to edge_distance (converted to Double)
    val edgeType: String?,        // Maps to edge_type (STAIR, LIFT, etc.)
    val geometry: List<List<Double>> // Handled by TypeConverter
)
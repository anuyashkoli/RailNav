package com.app.railnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val id: Int,      // Maps to node_id
    val name: String?,            // Maps to node_name
    val type: String?,            // Maps to node_type
    val level: Int,               // Maps to node_level
    val lat: Double,              // Maps to coordinates[1]
    val lon: Double               // Maps to coordinates[0]
)
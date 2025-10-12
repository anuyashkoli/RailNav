package com.app.railnav.data // Your correct package name

import kotlinx.serialization.Serializable

@Serializable
data class EdgeProperties(
    val edge_id: String,
    val edge_type: String?,
    val start_id: String,
    val end_id: String,
    val edge_level: String?,
    val edge_distance: String,
    val edge_accessibilty: String?
)
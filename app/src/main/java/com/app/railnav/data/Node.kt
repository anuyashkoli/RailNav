package com.app.railnav.data // Your correct package name

import kotlinx.serialization.Serializable

@Serializable
data class NodeProperties(
    val node_id: Int,
    val node_name: String?,
    val node_type: String?,
    val node_level: Int,
    val note: String?
)
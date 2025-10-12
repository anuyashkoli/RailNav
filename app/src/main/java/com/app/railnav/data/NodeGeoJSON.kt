package com.app.railnav.data

import kotlinx.serialization.Serializable

@Serializable
data class NodeFeatureCollection(
    val features: List<NodeFeature>
)

@Serializable
data class NodeFeature(
    val properties: NodeProperties,
    val geometry: NodeGeometry
)

@Serializable
data class NodeGeometry(
    val coordinates: List<Double> // A simple list of numbers, for Points
)
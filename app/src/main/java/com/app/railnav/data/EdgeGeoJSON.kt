package com.app.railnav.data

import kotlinx.serialization.Serializable

@Serializable
data class EdgeFeatureCollection(
    val features: List<EdgeFeature>
)

@Serializable
data class EdgeFeature(
    val properties: EdgeProperties,
    // Add the geometry field to the edge's blueprint
    val geometry: EdgeGeometry
)

@Serializable
data class EdgeGeometry(
    // A LineString is a list of coordinate pairs (a list of lists)
    val coordinates: List<List<Double>>
)
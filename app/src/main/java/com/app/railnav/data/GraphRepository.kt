package com.app.railnav.data

import android.content.Context
import kotlinx.serialization.json.Json

object GraphRepository {

    private val json = Json { ignoreUnknownKeys = true }

    fun loadNodes(context: Context): List<NodeFeature> {
        val jsonString = context.assets.open("nodes.geojson").bufferedReader().use { it.readText() }
        val featureCollection = json.decodeFromString<NodeFeatureCollection>(jsonString)
        return featureCollection.features
    }

    // --- THIS FUNCTION IS THE ONLY PART THAT CHANGES ---
    // It now returns the full EdgeFeature list, which includes the geometry.
    fun loadEdges(context: Context): List<EdgeFeature> {
        val jsonString = context.assets.open("edges.geojson").bufferedReader().use { it.readText() }
        // We now use the new EdgeFeatureCollection blueprint to parse the file.
        val featureCollection = json.decodeFromString<EdgeFeatureCollection>(jsonString)
        return featureCollection.features
    }
}
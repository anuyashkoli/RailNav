package com.app.railnav.core.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Train / Station Search Autocomplete ─────────────────────

@Serializable
data class TrainSearchResponse(
    val success: Boolean = false,
    val data: List<TrainSearchResult>? = null
)

@Serializable
data class TrainSearchResult(
    @SerialName("train_number") val trainNumber: String = "",
    @SerialName("train_name") val trainName: String = ""
)

@Serializable
data class StationSearchResponse(
    val success: Boolean = false,
    val data: List<StationSearchResult>? = null
)

@Serializable
data class StationSearchResult(
    @SerialName("station_code") val stationCode: String = "",
    @SerialName("station_name") val stationName: String = ""
)

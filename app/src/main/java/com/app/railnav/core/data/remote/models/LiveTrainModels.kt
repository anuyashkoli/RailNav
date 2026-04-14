package com.app.railnav.core.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Live Train Status ───────────────────────────────────────

@Serializable
data class LiveTrainResponse(
    val success: Boolean = false,
    val error: String? = null,
    val data: LiveTrainData? = null
)

@Serializable
data class LiveTrainData(
    @SerialName("train_number") val trainNumber: String = "",
    @SerialName("train_name") val trainName: String = "",
    @SerialName("current_station_name") val currentStationName: String = "",
    val delay: Int = 0,
    @SerialName("distance_from_source") val distanceFromSource: Int = 0,
    @SerialName("total_distance") val totalDistance: Int = 0,
    @SerialName("next_stoppage_info") val nextStoppageInfo: NextStoppageInfo? = null,
    @SerialName("current_location_info") val currentLocationInfo: List<CurrentLocationInfo> = emptyList()
)

@Serializable
data class NextStoppageInfo(
    @SerialName("next_stoppage_title") val nextStoppageTitle: String = "",
    @SerialName("next_stoppage") val nextStoppage: String = "",
    @SerialName("next_stoppage_time_diff") val nextStoppageTimeDiff: String = ""
)

@Serializable
data class CurrentLocationInfo(
    val label: String = "",
    @SerialName("readable_message") val readableMessage: String = "",
    val hint: String = ""
)

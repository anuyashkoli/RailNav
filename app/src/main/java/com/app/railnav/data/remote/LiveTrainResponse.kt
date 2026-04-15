package com.app.railnav.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveTrainResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: LiveTrainData? = null
)

@Serializable
data class LiveTrainData(
    @SerialName("train_number") val trainNumber: String,
    @SerialName("train_name") val trainName: String,
    @SerialName("current_station_name") val currentStationName: String,
    @SerialName("status") val status: String,
    @SerialName("distance_from_source") val distanceFromSource: Int,
    @SerialName("total_distance") val totalDistance: Int,
    @SerialName("eta") val eta: String,
    @SerialName("etd") val etd: String,
    @SerialName("delay") val delay: Int,
    @SerialName("next_stoppage_info") val nextStoppageInfo: NextStoppageInfo? = null,
    @SerialName("current_location_info") val currentLocationInfo: List<LocationInfo> = emptyList(),
    @SerialName("upcoming_stations_count") val upcomingStationsCount: Int = 0
)

@Serializable
data class NextStoppageInfo(
    @SerialName("next_stoppage_title") val title: String = "Next:",
    @SerialName("next_stoppage") val nextStoppage: String,
    @SerialName("next_stoppage_time_diff") val timeDiff: String,
    @SerialName("next_stoppage_delay") val delay: Int = 0
)

@Serializable
data class LocationInfo(
    @SerialName("type") val type: Int,
    @SerialName("label") val label: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("readable_message") val readableMessage: String = "",
    @SerialName("hint") val hint: String = ""
)

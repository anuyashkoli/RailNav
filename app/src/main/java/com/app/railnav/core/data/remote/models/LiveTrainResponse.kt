package com.app.railnav.core.data.remote.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class LiveTrainResponse(
    val success: Boolean,
    val data: LiveTrainData? = null,
    val error: String? = null
)

@Serializable
data class LiveTrainData(
    @SerialName("train_number") val trainNumber: String,
    @SerialName("train_name") val trainName: String,
    @SerialName("current_station_name") val currentStationName: String,
    val status: String,
    @SerialName("distance_from_source") val distanceFromSource: Int,
    @SerialName("total_distance") val totalDistance: Int,
    val eta: String,
    val etd: String,
    val delay: Int,
    @SerialName("next_stoppage_info") val nextStoppageInfo: NextStoppageInfo? = null,
    @SerialName("current_location_info") val currentLocationInfo: List<CurrentLocationInfo> = emptyList(),
    @SerialName("upcoming_stations_count") val upcomingStationsCount: Int? = null
)

@Serializable
data class NextStoppageInfo(
    @SerialName("next_stoppage_title") val nextStoppageTitle: String,
    @SerialName("next_stoppage") val nextStoppage: String,
    @SerialName("next_stoppage_time_diff") val nextStoppageTimeDiff: String,
    @SerialName("next_stoppage_delay") val nextStoppageDelay: Int
)

@Serializable
data class CurrentLocationInfo(
    val type: Int,
    val deeplink: String? = null,
    @SerialName("img_url") val imgUrl: String? = null,
    val label: String,
    val message: String,
    @SerialName("readable_message") val readableMessage: String,
    val hint: String
)

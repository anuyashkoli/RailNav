package com.app.railnav.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveStationResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: LiveStationData? = null
)

@Serializable
data class LiveStationData(
    @SerialName("source") val source: String,
    @SerialName("destination") val destination: String? = null,
    @SerialName("hours") val hours: String,
    @SerialName("trainCount") val trainCount: Int,
    @SerialName("trains") val trains: List<LiveTrainDto>
)

@Serializable
data class LiveTrainDto(
    @SerialName("trainNumber") val trainNumber: String,
    @SerialName("trainName") val trainName: String,
    @SerialName("scheduledArrival") val scheduledArrival: String,
    @SerialName("scheduledDeparture") val scheduledDeparture: String,
    @SerialName("expectedArrival") val expectedArrival: String,
    @SerialName("expectedDeparture") val expectedDeparture: String,
    @SerialName("delay") val delay: String,
    @SerialName("platform") val platform: String
)

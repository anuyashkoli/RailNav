package com.app.railnav.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainRouteResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: List<StationDto>
)

@Serializable
data class StationDto(
    @SerialName("station_name") val stationName: String,
    @SerialName("station_code") val stationCode: String,
    @SerialName("state_name") val stateName: String? = null,
    @SerialName("day") val day: Int,
    @SerialName("std_min") val stdMin: Int, // Time in minutes from start
    @SerialName("stop") val isStop: Boolean,
    @SerialName("platform_number") val platformNumber: Int,
    @SerialName("lat") val lat: String? = null,
    @SerialName("lng") val lng: String? = null,
    @SerialName("food_available") val isFoodAvailable: Boolean = false,
    @SerialName("hospital_available") val isHospitalAvailable: Boolean = false
)
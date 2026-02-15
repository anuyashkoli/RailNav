package com.app.railnav.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainStatusDto(
    @SerialName("train_number") val trainNumber: String,
    @SerialName("train_name") val trainName: String,
    @SerialName("current_station_code") val currentStationCode: String?,
    @SerialName("current_station_name") val currentStationName: String?,
    @SerialName("delay_minutes") val delayMinutes: Int = 0,
    @SerialName("status_message") val statusMessage: String? = null, // e.g., "On Time", "Delayed"
    @SerialName("platform_number") val platformNumber: String? = null // THE KEY FEATURE for Navigation
)
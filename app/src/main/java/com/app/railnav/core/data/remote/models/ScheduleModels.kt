package com.app.railnav.core.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Train Schedule ──────────────────────────────────────────

@Serializable
data class ScheduleResponse(
    val success: Boolean = false,
    @SerialName("error") val errorMessage: String? = null,
    val data: List<ScheduleStation>? = null
)

@Serializable
data class ScheduleStation(
    @SerialName("station_name") val stationName: String = "",
    @SerialName("station_code") val stationCode: String = "",
    @SerialName("arrival_time") val arrivalTime: String? = null,
    @SerialName("departure_time") val departureTime: String? = null,
    @SerialName("halt_time") val haltTime: String? = null,
    val distance: String = "0",
    val day: String = "1",
    @SerialName("is_stopping") val isStopping: Boolean = true
)

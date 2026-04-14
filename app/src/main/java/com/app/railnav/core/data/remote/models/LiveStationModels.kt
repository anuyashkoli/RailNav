package com.app.railnav.core.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── Live Station Departures ─────────────────────────────────

@Serializable
data class LiveStationResponse(
    val success: Boolean = false,
    val error: JsonElement? = null,
    val data: LiveStationData? = null
) {
    val errorMessage: String? get() = error?.extractMessage()
}

@Serializable
data class LiveStationData(
    val trains: List<LiveStationTrain> = emptyList()
)

@Serializable
data class LiveStationTrain(
    @SerialName("train_name") val trainName: String = "",
    @SerialName("train_number") val trainNumber: String = "",
    @SerialName("source_station") val sourceStation: String = "",
    @SerialName("destination_station") val destinationStation: String = "",
    val platform: String? = null,
    @SerialName("scheduled_departure") val scheduledDeparture: String? = null,
    @SerialName("delay_in_departure") val delayInDeparture: String? = null,
    @SerialName("is_cancelled") val isCancelled: Boolean = false
)

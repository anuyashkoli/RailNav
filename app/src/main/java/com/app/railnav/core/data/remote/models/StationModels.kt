package com.app.railnav.core.data.remote.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

// ── Helper to extract error message from flexible error field ────────────────
fun JsonElement?.toErrorString(): String? {
    if (this == null) return null
    try {
        return this.jsonPrimitive.contentOrNull
    } catch (_: Exception) {}
    return this.toString()
}

// ── 3. Live Station Departures ──────────────────────────────────────────────

@Serializable
data class LiveStationResponse(
    val success: Boolean,
    val data: LiveStationData? = null,
    val error: JsonElement? = null
) {
    val errorMessage: String? get() = error.toErrorString()
}

@Serializable
data class LiveStationData(
    val source: String = "",
    val destination: String = "",
    val trains: List<LiveStationTrain> = emptyList()
)

// Try BOTH camelCase (Kotlin defaults) AND snake_case via @SerialName alternatives
// The API may use either convention. We keep camelCase as primary names.
@Serializable
data class LiveStationTrain(
    val trainNumber: String = "",
    val trainName: String = "",
    val sourceStation: String = "",
    val destinationStation: String = "",
    val scheduledArrival: String? = null,
    val scheduledDeparture: String? = null,
    val expectedArrival: String? = null,
    val expectedDeparture: String? = null,
    val delayInArrival: String? = null,
    val delayInDeparture: String? = null,
    val platform: String? = null,
    val haltTime: String? = null,
    val isDiverted: Boolean = false,
    val isCancelled: Boolean = false
)

// ── 4. Train Schedule ───────────────────────────────────────────────────────

@Serializable
data class TrainScheduleResponse(
    val success: Boolean,
    val data: List<ScheduleStation>? = null,
    val error: JsonElement? = null
) {
    val errorMessage: String? get() = error.toErrorString()
}

@Serializable
data class ScheduleStation(
    @SerialName("station_code") val stationCode: String = "",
    @SerialName("station_name") val stationName: String = "",
    @SerialName("arrives") val arrivalTime: String? = null,
    @SerialName("departs") val departureTime: String? = null,
    @SerialName("halt") val haltTime: String? = null,
    @SerialName("stop") val isStop: Boolean = false,
    val day: Int = 1,
    val distance: String = "0",
    @SerialName("stop_number") val stopNumber: Int = 0
) {
    /** True if this is a stopping station (not a pass-through) */
    val isStopping: Boolean get() = isStop || (haltTime != null && haltTime != "0" && haltTime != "--")
}

// ── 5. Train Search (Name from Number) ──────────────────────────────────────

@Serializable
data class TrainSearchResponse(
    val success: Boolean,
    val data: List<TrainSearchResult>? = null,
    val error: JsonElement? = null
) {
    val errorMessage: String? get() = error.toErrorString()
}

@Serializable
data class TrainSearchResult(
    @SerialName("train_number") val trainNumber: String = "",
    @SerialName("train_name") val trainName: String = "",
    @SerialName("run_days") val runDays: List<String> = emptyList(),
    @SerialName("train_src") val trainSource: String = "",
    @SerialName("train_dstn") val trainDestination: String = ""
)

// ── 6. Station Search (Name from Code) ──────────────────────────────────────

@Serializable
data class StationSearchResponse(
    val success: Boolean,
    val data: List<StationSearchResult>? = null,
    val error: JsonElement? = null
) {
    val errorMessage: String? get() = error.toErrorString()
}

@Serializable
data class StationSearchResult(
    @SerialName("station_code") val stationCode: String = "",
    @SerialName("station_name") val stationName: String = "",
    val state: String = "",
    val city: String = ""
)

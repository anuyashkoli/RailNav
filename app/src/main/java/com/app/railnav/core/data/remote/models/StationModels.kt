package com.app.railnav.core.data.remote.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ── 3. Live Station Departures ──────────────────────────────────────────────

@Serializable
data class LiveStationResponse(
    val success: Boolean,
    val data: List<LiveStationTrain>? = null,
    val error: String? = null
)

@Serializable
data class LiveStationTrain(
    @SerialName("train_number") val trainNumber: String = "",
    @SerialName("train_name") val trainName: String = "",
    @SerialName("source_station") val sourceStation: String = "",
    @SerialName("destination_station") val destinationStation: String = "",
    @SerialName("scheduled_arrival") val scheduledArrival: String? = null,
    @SerialName("scheduled_departure") val scheduledDeparture: String? = null,
    @SerialName("expected_arrival") val expectedArrival: String? = null,
    @SerialName("expected_departure") val expectedDeparture: String? = null,
    @SerialName("delay_in_arrival") val delayInArrival: String? = null,
    @SerialName("delay_in_departure") val delayInDeparture: String? = null,
    val platform: String? = null,
    @SerialName("halt_time") val haltTime: String? = null,
    @SerialName("is_diverted") val isDiverted: Boolean = false,
    @SerialName("is_cancelled") val isCancelled: Boolean = false
)

// ── 4. Train Schedule ───────────────────────────────────────────────────────

@Serializable
data class TrainScheduleResponse(
    val success: Boolean,
    val data: TrainScheduleData? = null,
    val error: String? = null
)

@Serializable
data class TrainScheduleData(
    @SerialName("train_number") val trainNumber: String = "",
    @SerialName("train_name") val trainName: String = "",
    val stations: List<ScheduleStation> = emptyList()
)

@Serializable
data class ScheduleStation(
    @SerialName("station_code") val stationCode: String = "",
    @SerialName("station_name") val stationName: String = "",
    @SerialName("arrival_time") val arrivalTime: String? = null,
    @SerialName("departure_time") val departureTime: String? = null,
    @SerialName("halt_time") val haltTime: String? = null,
    @SerialName("day") val day: Int = 1,
    @SerialName("distance") val distance: Int = 0,
    @SerialName("stop_number") val stopNumber: Int = 0
)

// ── 5. Train Search (Name from Number) ──────────────────────────────────────

@Serializable
data class TrainSearchResponse(
    val success: Boolean,
    val data: List<TrainSearchResult>? = null,
    val error: String? = null
)

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
    val error: String? = null
)

@Serializable
data class StationSearchResult(
    @SerialName("station_code") val stationCode: String = "",
    @SerialName("station_name") val stationName: String = "",
    val state: String = "",
    val city: String = ""
)

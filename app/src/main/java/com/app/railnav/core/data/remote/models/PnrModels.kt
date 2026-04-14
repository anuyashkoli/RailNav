package com.app.railnav.core.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── PNR Status ──────────────────────────────────────────────

@Serializable
data class PnrResponse(
    val success: Boolean = false,
    val error: String? = null,
    val data: PnrData? = null
)

@Serializable
data class PnrData(
    @SerialName("Pnr") val pnr: String = "",
    @SerialName("TrainNo") val trainNumber: String = "",
    @SerialName("TrainName") val trainName: String = "",
    @SerialName("Doj") val journeyDate: String = "",
    @SerialName("SourceStation") val source: String = "",
    @SerialName("DestinationStation") val destination: String = "",
    @SerialName("DepartureTime") val departureTime: String = "",
    @SerialName("ArrivalTime") val arrivalTime: String = "",
    @SerialName("Duration") val duration: String = "",
    @SerialName("Class") val travelClass: String = "",
    @SerialName("ChartPrepared") val chartPrepared: Boolean = false,
    @SerialName("PassengerStatus") val passengers: List<PnrPassenger> = emptyList()
)

@Serializable
data class PnrPassenger(
    @SerialName("Number") val number: Int = 0,
    @SerialName("BookingStatus") val bookingStatus: String = "",
    @SerialName("CurrentStatus") val currentStatus: String = "",
    @SerialName("Coach") val coach: String = "N/A",
    @SerialName("Berth") val berth: Int? = null
)

package com.app.railnav.core.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ── PNR Status ──────────────────────────────────────────────

@Serializable
data class PnrResponse(
    val success: Boolean = false,
    val error: JsonElement? = null,
    val data: PnrData? = null
) {
    /** Flattens any error shape into a human-readable string. */
    val errorMessage: String?
        get() = error?.extractMessage()
}

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

/**
 * Extracts a flat string from a JsonElement regardless of whether it is
 * a primitive string, an object of arrays (e.g. {"pnr":["Required"]}),
 * or an array of strings.
 */
internal fun JsonElement.extractMessage(): String? = try {
    when {
        // Plain string: "Something went wrong"
        this.jsonPrimitive.isString -> this.jsonPrimitive.content
        else -> null
    }
} catch (_: Exception) {
    try {
        // Object: {"pnr":["Required"], "source":["Invalid"]}
        this.jsonObject.entries.joinToString("; ") { (key, value) ->
            try {
                val msgs = value.jsonArray.joinToString(", ") { it.jsonPrimitive.content }
                "$key: $msgs"
            } catch (_: Exception) { "$key: ${value}" }
        }
    } catch (_: Exception) {
        this.toString()
    }
}

package com.app.railnav.core.data.remote.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class PnrResponse(
    val success: Boolean,
    val data: PnrData? = null,
    val error: String? = null
)

@Serializable
data class PnrData(
    val pnr: String,
    val trainNumber: String,
    val trainName: String,
    val journeyDate: String,
    val bookingDate: String? = null,
    val source: String,
    val destination: String,
    val boardingPoint: String,
    @SerialName("class") val travelClass: String,
    val chartPrepared: Boolean,
    val trainStatus: String? = null,
    val departureTime: String,
    val arrivalTime: String,
    val duration: String,
    val passengers: List<PnrPassenger>,
    val fare: PnrFare? = null,
    val ratings: PnrRatings? = null,
    val hasPantry: Boolean? = null,
    val isCancelled: Boolean? = null
)

@Serializable
data class PnrPassenger(
    val number: Int,
    val bookingStatus: String,
    val currentStatus: String,
    val coach: String,
    val berth: Int? = null
)

@Serializable
data class PnrFare(
    val bookingFare: String? = null,
    val ticketFare: String? = null
)

@Serializable
data class PnrRatings(
    val overall: Double? = null,
    val food: Double? = null,
    val punctuality: Double? = null,
    val cleanliness: Double? = null,
    val ratingCount: Int? = null
)

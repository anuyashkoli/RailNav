package com.app.railnav.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PnrResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: PnrData? = null
)

@Serializable
data class PnrData(
    @SerialName("pnr") val pnr: String,
    @SerialName("trainNumber") val trainNumber: String,
    @SerialName("trainName") val trainName: String,
    @SerialName("journeyDate") val journeyDate: String,
    @SerialName("bookingDate") val bookingDate: String,
    @SerialName("source") val source: String,
    @SerialName("destination") val destination: String,
    @SerialName("boardingPoint") val boardingPoint: String,
    @SerialName("class") val travelClass: String,
    @SerialName("chartPrepared") val chartPrepared: Boolean,
    @SerialName("trainStatus") val trainStatus: String,
    @SerialName("departureTime") val departureTime: String,
    @SerialName("arrivalTime") val arrivalTime: String,
    @SerialName("duration") val duration: String,
    @SerialName("passengers") val passengers: List<PassengerDto>,
    @SerialName("fare") val fare: FareDto,
    @SerialName("ratings") val ratings: RatingsDto,
    @SerialName("hasPantry") val hasPantry: Boolean,
    @SerialName("isCancelled") val isCancelled: Boolean
)

@Serializable
data class PassengerDto(
    @SerialName("number") val number: Int,
    @SerialName("bookingStatus") val bookingStatus: String,
    @SerialName("currentStatus") val currentStatus: String,
    @SerialName("coach") val coach: String,
    @SerialName("berth") val berth: Int
)

@Serializable
data class FareDto(
    @SerialName("bookingFare") val bookingFare: String,
    @SerialName("ticketFare") val ticketFare: String
)

@Serializable
data class RatingsDto(
    @SerialName("overall") val overall: Double,
    @SerialName("food") val food: Double,
    @SerialName("punctuality") val punctuality: Double,
    @SerialName("cleanliness") val cleanliness: Double,
    @SerialName("ratingCount") val ratingCount: Int
)
package com.app.railnav.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TrainApi {

    @GET("trainSchedule")
    suspend fun getTrainSchedule(
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") host: String = "irctc-api2.p.rapidapi.com",
        @Query("trainNumber") trainNumber: String
    ): TrainRouteResponse

    @GET("pnrStatus")
    suspend fun getPnrStatus(
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") host: String = "irctc-api2.p.rapidapi.com",
        @Query("pnr") pnrNumber: String
    ): PnrResponse

    @GET("liveStation")
    suspend fun getLiveStationBoard(
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") host: String = "irctc-api2.p.rapidapi.com",
        @Query("source") source: String,
        @Query("hours") hours: String,
        @Query("destination") destination: String? = null
    ): LiveStationResponse

    @GET("liveTrain")
    suspend fun getLiveTrainStatus(
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") host: String = "irctc-api2.p.rapidapi.com",
        @Query("trainNumber") trainNumber: String,
        @Query("startDay") startDay: String
    ): LiveTrainResponse
}
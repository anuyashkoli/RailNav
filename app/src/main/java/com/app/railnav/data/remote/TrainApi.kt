package com.app.railnav.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TrainApi {

    // UPDATE: Matches your snippet: /trainSchedule?trainNumber=...
    @GET("trainSchedule")
    suspend fun getTrainSchedule(
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") host: String = "irctc-api2.p.rapidapi.com",
        @Query("trainNumber") trainNumber: String
    ): TrainRouteResponse // Ensure this DTO matches the new JSON structure!
}
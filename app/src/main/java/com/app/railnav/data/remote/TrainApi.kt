package com.app.railnav.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TrainApi {

    // Example endpoint based on common RapidAPI structures
    // GET /live-status?trainNo=12345&date=2026-03-11
    @GET("live-status") 
    suspend fun getLiveTrainStatus(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Query("trainNo") trainNumber: String,
        @Query("date") date: String // YYYY-MM-DD
    ): TrainStatusDto
}
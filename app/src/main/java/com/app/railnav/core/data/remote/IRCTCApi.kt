package com.app.railnav.core.data.remote

import com.app.railnav.core.data.remote.models.PnrResponse
import com.app.railnav.core.data.remote.models.LiveTrainResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface IRCTCApi {
    
    // Live PNR Status Check
    // Example: GET /pnrStatus?pnr=6351738552
    @GET("pnrStatus")
    suspend fun getPnrStatus(
        @Query("pnr") pnrNumber: String
    ): PnrResponse
    
    // Live Train Status
    // Example: GET /liveTrainStatus?trainNo=12321&date=14-04-2026
    @GET("liveTrainStatus")
    suspend fun getLiveTrainStatus(
        @Query("trainNo") trainNumber: String,
        @Query("date") date: String
    ): LiveTrainResponse
}

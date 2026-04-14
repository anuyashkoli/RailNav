package com.app.railnav.core.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface IRCTCApi {
    
    // Live PNR Status Check
    // Example: GET /pnrStatus?pnr=6351738552
    @GET("pnrStatus")
    suspend fun getPnrStatus(
        @Query("pnr") pnrNumber: String
    ): JsonObject
    
    // We will add more endpoints (Live Station, Train Schedule) here!
}

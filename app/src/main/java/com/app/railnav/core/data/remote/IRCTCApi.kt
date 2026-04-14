package com.app.railnav.core.data.remote

import com.app.railnav.core.data.remote.models.PnrResponse
import com.app.railnav.core.data.remote.models.LiveTrainResponse
import com.app.railnav.core.data.remote.models.LiveStationResponse
import com.app.railnav.core.data.remote.models.TrainScheduleResponse
import com.app.railnav.core.data.remote.models.TrainSearchResponse
import com.app.railnav.core.data.remote.models.StationSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface IRCTCApi {

    // 1. PNR Status Check
    @GET("pnrStatus")
    suspend fun getPnrStatus(
        @Query("pnr") pnrNumber: String
    ): PnrResponse

    // 2. Live Train Status
    @GET("liveTrain")
    suspend fun getLiveTrainStatus(
        @Query("trainNumber") trainNumber: String,
        @Query("startDay") startDay: String
    ): LiveTrainResponse

    // 3. Live Station Departures
    @GET("liveStation")
    suspend fun getLiveStation(
        @Query("source") stationCode: String,
        @Query("hours") hours: String = "4"
    ): LiveStationResponse

    // 4. Train Schedule
    @GET("trainSchedule")
    suspend fun getTrainSchedule(
        @Query("trainNumber") trainNumber: String
    ): TrainScheduleResponse

    // 5. Train Name from Number
    @GET("trainSearch")
    suspend fun searchTrain(
        @Query("query") trainNumber: String
    ): TrainSearchResponse

    // 6. Station Name from Code
    @GET("stationSearch")
    suspend fun searchStation(
        @Query("query") stationQuery: String
    ): StationSearchResponse
}

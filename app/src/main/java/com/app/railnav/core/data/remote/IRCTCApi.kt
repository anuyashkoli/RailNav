package com.app.railnav.core.data.remote

import com.app.railnav.core.data.remote.models.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the IRCTC RapidAPI (irctc-api2.p.rapidapi.com).
 * Headers (x-rapidapi-key, x-rapidapi-host) are injected by OkHttp interceptor in NetworkModule.
 *
 * Endpoint names verified by live HTTP probe:
 *   /pnrStatus, /liveTrain, /liveStation, /trainSchedule, /trainSearch, /stationSearch
 */
interface IRCTCApi {

    // Correct param: "pnr"
    @GET("pnrStatus")
    suspend fun getPnrStatus(
        @Query("pnr") pnrNumber: String
    ): PnrResponse

    // Correct params: "trainNumber", "startDay"
    @GET("liveTrain")
    suspend fun getLiveTrainStatus(
        @Query("trainNumber") trainNo: String,
        @Query("startDay") startDay: String
    ): LiveTrainResponse

    // Correct params: "source" (station code), "hours"
    @GET("liveStation")
    suspend fun getLiveStation(
        @Query("source") stationCode: String,
        @Query("hours") hours: String
    ): LiveStationResponse

    // Correct param: "trainNo"
    @GET("trainSchedule")
    suspend fun getTrainSchedule(
        @Query("trainNo") trainNo: String
    ): ScheduleResponse

    @GET("trainSearch")
    suspend fun searchTrain(
        @Query("query") query: String
    ): TrainSearchResponse

    @GET("stationSearch")
    suspend fun searchStation(
        @Query("query") query: String
    ): StationSearchResponse
}

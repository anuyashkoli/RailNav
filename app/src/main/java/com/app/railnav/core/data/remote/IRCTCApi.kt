package com.app.railnav.core.data.remote

import com.app.railnav.core.data.remote.models.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the IRCTC RapidAPI (irctc-api2.p.rapidapi.com).
 * Headers (x-rapidapi-key, x-rapidapi-host) are injected by OkHttp interceptor in NetworkModule.
 */
interface IRCTCApi {

    @GET("pnrStatus")
    suspend fun getPnrStatus(
        @Query("pnrNumber") pnrNumber: String
    ): PnrResponse

    @GET("liveTrain")
    suspend fun getLiveTrainStatus(
        @Query("trainNo") trainNo: String,
        @Query("startDay") startDay: String
    ): LiveTrainResponse

    @GET("liveStation")
    suspend fun getLiveStation(
        @Query("stationCode") stationCode: String,
        @Query("hours") hours: String
    ): LiveStationResponse

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

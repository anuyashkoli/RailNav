package com.app.railnav.core.data.repository

import com.app.railnav.core.data.remote.IRCTCApi
import com.app.railnav.core.data.remote.models.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository wrapper around [IRCTCApi] that catches network exceptions
 * and returns Result<T> for safe consumption by ViewModels.
 */
@Singleton
class IRCTCRepository @Inject constructor(
    private val api: IRCTCApi
) {

    suspend fun getLiveStation(stationCode: String, hours: String): Result<LiveStationResponse> {
        return runCatching { api.getLiveStation(stationCode, hours) }
    }

    suspend fun getTrainSchedule(trainNo: String): Result<ScheduleResponse> {
        return runCatching { api.getTrainSchedule(trainNo) }
    }

    suspend fun searchTrain(query: String): Result<TrainSearchResponse> {
        return runCatching { api.searchTrain(query) }
    }

    suspend fun searchStation(query: String): Result<StationSearchResponse> {
        return runCatching { api.searchStation(query) }
    }
}

package com.app.railnav.core.data.repository

import com.app.railnav.core.data.remote.IRCTCApi
import com.app.railnav.core.data.remote.models.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository wrapping all IRCTC API calls.
 * ViewModels should depend on this, not on the raw API interface.
 */
@Singleton
class IRCTCRepository @Inject constructor(
    private val api: IRCTCApi
) {
    // 1. PNR Status
    suspend fun getPnrStatus(pnr: String): Result<PnrResponse> = runCatching {
        api.getPnrStatus(pnr)
    }

    // 2. Live Train Status
    suspend fun getLiveTrainStatus(trainNumber: String, startDay: String = "0"): Result<LiveTrainResponse> = runCatching {
        api.getLiveTrainStatus(trainNumber, startDay)
    }

    // 3. Live Station Departures (next N hours)
    suspend fun getLiveStation(stationCode: String, hours: String = "4"): Result<LiveStationResponse> = runCatching {
        api.getLiveStation(stationCode, hours)
    }

    // 4. Train Schedule (all stops)
    suspend fun getTrainSchedule(trainNumber: String): Result<TrainScheduleResponse> = runCatching {
        api.getTrainSchedule(trainNumber)
    }

    // 5. Train Name lookup from number
    suspend fun searchTrain(query: String): Result<TrainSearchResponse> = runCatching {
        api.searchTrain(query)
    }

    // 6. Station Name lookup from code
    suspend fun searchStation(query: String): Result<StationSearchResponse> = runCatching {
        api.searchStation(query)
    }
}

package com.app.railnav.data

import com.app.railnav.data.local.TrainScheduleDao
import com.app.railnav.data.local.TrainScheduleEntity
import com.app.railnav.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrainRepository(private val dao: TrainScheduleDao) {

    suspend fun getPlatformInfo(trainNumber: String, currentStationCode: String): Int? {
        return withContext(Dispatchers.IO) {

            // FIXED: Changed getStationDetails to getStationInfo to match DAO
            val localData = dao.getStationInfo(trainNumber, currentStationCode)
            if (localData != null) {
                return@withContext localData.platformNumber
            }

            try {
                val response = RetrofitClient.api.getTrainSchedule(
                    apiKey = "f1207f505dmsh7e8c7533c3f8f4dp11f1e9jsn498a3d1e06f5",
                    trainNumber = trainNumber
                )

                if (response.success) {
                    val entities = response.data.map { dto ->
                        TrainScheduleEntity(
                            trainNumber = trainNumber,
                            stationCode = dto.stationCode,
                            stationName = dto.stationName,
                            platformNumber = dto.platformNumber,
                            arrivalTimeMin = dto.stdMin,
                            dayCount = dto.day,
                            latitude = dto.lat?.toDoubleOrNull(),
                            longitude = dto.lng?.toDoubleOrNull()
                        )
                    }
                    // FIXED: Changed insertSchedule to insertAll to match DAO
                    dao.insertAll(entities)

                    val target = entities.find { it.stationCode == currentStationCode }
                    return@withContext target?.platformNumber
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }
}
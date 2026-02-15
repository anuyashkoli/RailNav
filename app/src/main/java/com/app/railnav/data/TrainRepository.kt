package com.app.railnav.data

import com.app.railnav.data.local.TrainScheduleDao
import com.app.railnav.data.local.TrainScheduleEntity
import com.app.railnav.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrainRepository(private val dao: TrainScheduleDao) {

    suspend fun getPlatformInfo(trainNumber: String, currentStationCode: String): Int? {
        return withContext(Dispatchers.IO) {
            
            // STEP 1: Check Local Database (Cache)
            val localData = dao.getStationDetails(trainNumber, currentStationCode)
            if (localData != null) {
                println("Loaded from Local DB: Platform ${localData.platformNumber}")
                return@withContext localData.platformNumber
            }

            // STEP 2: If missing, Fetch from API
            println("Local DB empty. Fetching from API...")
            try {
                val response = RetrofitClient.api.getTrainSchedule(
                    apiKey = "f1207f505dmsh7e8c7533c3f8f4dp11f1e9jsn498a3d1e06f5",
                    trainNumber = trainNumber
                )

                if (response.success) {
                    // STEP 3: Populate Local Database (Cache it!)
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
                    dao.insertSchedule(entities)

                    // STEP 4: Return the specific requested platform
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
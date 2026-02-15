package com.app.railnav.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrainScheduleDao {

    // 1. SAVE: Populates the DB with the list fetched from API
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: List<TrainScheduleEntity>)

    // 2. READ: Checks if we already have the platform info locally
    @Query("SELECT * FROM train_schedules WHERE trainNumber = :trainNo AND stationCode = :stationCode LIMIT 1")
    suspend fun getStationDetails(trainNo: String, stationCode: String): TrainScheduleEntity?
    
    // 3. CHECK CACHE: See if we have ANY data for this train
    @Query("SELECT COUNT(*) FROM train_schedules WHERE trainNumber = :trainNo")
    suspend fun hasScheduleFor(trainNo: String): Int
}
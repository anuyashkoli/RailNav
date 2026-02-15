package com.app.railnav.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrainScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<TrainScheduleEntity>)

    @Query("SELECT * FROM train_schedules WHERE trainNumber = :trainNo AND stationCode = :code LIMIT 1")
    suspend fun getStationInfo(trainNo: String, code: String): TrainScheduleEntity?
}
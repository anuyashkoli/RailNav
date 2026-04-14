package com.app.railnav.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.railnav.core.data.local.dao.SearchHistoryDao
import com.app.railnav.core.data.local.entity.SearchHistoryEntity

/**
 * Separate Room database for IRCTC feature data (search history, etc.).
 * The original RailNavDatabase handles graph/map data; this one handles
 * the new IRCTC service layer exclusively.
 */
@Database(
    entities = [SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val searchHistoryDao: SearchHistoryDao
}

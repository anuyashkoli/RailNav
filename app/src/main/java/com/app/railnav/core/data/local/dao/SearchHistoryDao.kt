package com.app.railnav.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.railnav.core.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(entity: SearchHistoryEntity)

    @Query("SELECT * FROM search_history WHERE searchType = :type ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(type: String, limit: Int): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE searchType = :type")
    suspend fun clearSearches(type: String)
}

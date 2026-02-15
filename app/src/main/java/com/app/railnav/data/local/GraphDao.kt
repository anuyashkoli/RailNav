package com.app.railnav.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GraphDao {

    // Insert Methods (Used for Seeding)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<NodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<EdgeEntity>)

    // Query Methods (Used for Navigation)

    @Query("SELECT * FROM nodes")
    suspend fun getAllNodes(): List<NodeEntity>

    @Query("SELECT * FROM edges")
    suspend fun getAllEdges(): List<EdgeEntity>

    // Search Method (Replaces SearchUtils)
    // Finds nodes where the name OR the type matches the search query.
    @Query("""
        SELECT * FROM nodes 
        WHERE name LIKE '%' || :query || '%' 
        OR type LIKE '%' || :query || '%'
    """)
    suspend fun searchNodes(query: String): List<NodeEntity>

    // Utility: Check if DB is empty to know if we need to seed data
    @Query("SELECT COUNT(*) FROM nodes")
    suspend fun getNodeCount(): Int
}
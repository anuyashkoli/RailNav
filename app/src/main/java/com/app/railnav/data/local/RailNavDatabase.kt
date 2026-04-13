package com.app.railnav.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.railnav.data.EdgeFeatureCollection
import com.app.railnav.data.NodeFeatureCollection
import kotlinx.serialization.json.Json

// 1. BUMP VERSION TO 3
@Database(entities = [NodeEntity::class, EdgeEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RailNavDatabase : RoomDatabase() {

    abstract fun graphDao(): GraphDao

    companion object {
        @Volatile
        private var INSTANCE: RailNavDatabase? = null

        fun getDatabase(context: Context): RailNavDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RailNavDatabase::class.java,
                    "railnav_database"
                )
                    .fallbackToDestructiveMigration() // 2. ADD THIS CRITICAL LINE!
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDatabaseIfEmpty(context: Context, database: RailNavDatabase) {
            val dao = database.graphDao()
            // Because destructive migration wipes the database, getNodeCount() will be 0,
            // which allows the rest of this function to run and read your new JSON files!
            if (dao.getNodeCount() > 0) return

            try {
                val json = Json { ignoreUnknownKeys = true }

                // 1. Parse & Insert Nodes
                val nodeJson = context.assets.open("nodes.geojson").bufferedReader().use { it.readText() }
                val nodeCollection = json.decodeFromString<NodeFeatureCollection>(nodeJson)
                val nodeEntities = nodeCollection.features.map { feature ->
                    NodeEntity(
                        id = feature.properties.node_id,
                        name = feature.properties.node_name,
                        type = feature.properties.node_type,
                        level = feature.properties.node_level,
                        lat = feature.geometry.coordinates[1],
                        lon = feature.geometry.coordinates[0]
                    )
                }
                dao.insertNodes(nodeEntities)

                // 2. Parse & Insert Edges
                val edgeJson = context.assets.open("edges.geojson").bufferedReader().use { it.readText() }
                val edgeCollection = json.decodeFromString<EdgeFeatureCollection>(edgeJson)
                val edgeEntities = edgeCollection.features.map { feature ->
                    EdgeEntity(
                        edgeId = feature.properties.edge_id,
                        startNodeId = feature.properties.start_id.toIntOrNull() ?: 0,
                        endNodeId = feature.properties.end_id.toIntOrNull() ?: 0,
                        distance = feature.properties.edge_distance.toDoubleOrNull() ?: 0.0,
                        edgeType = feature.properties.edge_type,
                        geometry = feature.geometry.coordinates
                    )
                }
                dao.insertEdges(edgeEntities)
            } catch (e: Exception) {
                e.printStackTrace() // Catch JSON parsing errors so it doesn't crash the app silently
            }
        }
    }
}
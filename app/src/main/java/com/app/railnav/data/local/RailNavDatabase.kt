package com.app.railnav.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.railnav.data.EdgeFeatureCollection
import com.app.railnav.data.NodeFeatureCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Database(
    entities = [NodeEntity::class, EdgeEntity::class, TrainScheduleEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RailNavDatabase : RoomDatabase() {

    abstract fun graphDao(): GraphDao
    abstract fun trainScheduleDao(): TrainScheduleDao

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
                    .addCallback(RailNavDatabaseCallback(context.applicationContext, CoroutineScope(Dispatchers.IO)))
                    // FIXED: Added 'true' to handle deprecation
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class RailNavDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.graphDao(), context)
                }
            }
        }

        suspend fun populateDatabase(graphDao: GraphDao, context: Context) {
            try {
                val json = Json { ignoreUnknownKeys = true }

                // 1. Parse Nodes
                val nodeInputStream = context.assets.open("nodes.geojson")
                val nodeJson = nodeInputStream.bufferedReader().use { it.readText() }
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
                graphDao.insertNodes(nodeEntities)

                // 2. Parse Edges
                val edgeInputStream = context.assets.open("edges.geojson")
                val edgeJson = edgeInputStream.bufferedReader().use { it.readText() }
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
                graphDao.insertEdges(edgeEntities)

                println("DEBUG: Database populated successfully.")

            } catch (e: Exception) {
                e.printStackTrace()
                println("ERROR: Failed to populate database: ${e.message}")
            }
        }
    }
}
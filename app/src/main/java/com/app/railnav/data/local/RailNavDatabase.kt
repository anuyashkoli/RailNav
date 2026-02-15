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

@Database(entities = [NodeEntity::class, EdgeEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RailNavDatabase : RoomDatabase() {

    abstract fun graphDao(): GraphDao

    companion object {
        @Volatile
        private var INSTANCE: RailNavDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): RailNavDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RailNavDatabase::class.java,
                    "railnav_database"
                )
                    .addCallback(RailNavDatabaseCallback(context, scope))
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
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.graphDao(), context)
                }
            }
        }

        suspend fun populateDatabase(graphDao: GraphDao, context: Context) {
            val json = Json { ignoreUnknownKeys = true }

            // 1. Parse Nodes
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
            graphDao.insertNodes(nodeEntities)

            // 2. Parse Edges
            val edgeJson = context.assets.open("edges.geojson").bufferedReader().use { it.readText() }
            val edgeCollection = json.decodeFromString<EdgeFeatureCollection>(edgeJson)

            val edgeEntities = edgeCollection.features.map { feature ->
                EdgeEntity(
                    edgeId = feature.properties.edge_id,
                    // SAFE CONVERSION: String -> Int
                    startNodeId = feature.properties.start_id.toIntOrNull() ?: 0,
                    endNodeId = feature.properties.end_id.toIntOrNull() ?: 0,
                    // SAFE CONVERSION: String -> Double
                    distance = feature.properties.edge_distance.toDoubleOrNull() ?: 0.0,
                    edgeType = feature.properties.edge_type,
                    geometry = feature.geometry.coordinates
                )
            }
            graphDao.insertEdges(edgeEntities)
        }
    }
}
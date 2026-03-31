package com.app.railnav.data

import android.content.Context
import com.app.railnav.data.local.RailNavDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object GraphRepository {

    suspend fun loadNodes(context: Context): List<NodeFeature> {
        val db = RailNavDatabase.getDatabase(context)

        // Force seed inline if the database is completely empty
        RailNavDatabase.seedDatabaseIfEmpty(context, db)
        val entities = db.graphDao().getAllNodes()

        return entities.map { entity ->
            NodeFeature(
                properties = NodeProperties(
                    node_id = entity.id,
                    node_name = entity.name,
                    node_type = entity.type,
                    node_level = entity.level,
                    note = null
                ),
                geometry = NodeGeometry(listOf(entity.lon, entity.lat))
            )
        }
    }

    suspend fun loadEdges(context: Context): List<EdgeFeature> {
        val db = RailNavDatabase.getDatabase(context)

        // Failsafe in case loadEdges runs before loadNodes
        RailNavDatabase.seedDatabaseIfEmpty(context, db)
        
        val entities = db.graphDao().getAllEdges()

        return entities.map { entity ->
            EdgeFeature(
                properties = EdgeProperties(
                    edge_id = entity.edgeId,
                    start_id = entity.startNodeId.toString(),
                    end_id = entity.endNodeId.toString(),
                    edge_distance = entity.distance.toString(),
                    edge_type = entity.edgeType,
                    edge_level = null,
                    edge_accessibilty = null
                ),
                geometry = EdgeGeometry(entity.geometry)
            )
        }
    }
}
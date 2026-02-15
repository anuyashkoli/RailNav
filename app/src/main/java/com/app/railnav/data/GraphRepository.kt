package com.app.railnav.data

import android.content.Context
import com.app.railnav.data.local.RailNavDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

object GraphRepository {

    suspend fun loadNodes(context: Context): List<NodeFeature> {
        // FIXED: Removed the extra argument. Only 'context' is needed.
        val db = RailNavDatabase.getDatabase(context)

        // Safety: Wait briefly if the DB is still seeding (only runs on first install)
        var attempts = 0
        while (db.graphDao().getNodeCount() == 0 && attempts < 10) {
            delay(100) // Wait 100ms
            attempts++
        }

        val entities = db.graphDao().getAllNodes()

        // Map Entity -> Feature (Bridge to existing app logic)
        return entities.map { entity ->
            NodeFeature(
                properties = NodeProperties(
                    node_id = entity.id,
                    node_name = entity.name,
                    node_type = entity.type,
                    node_level = entity.level,
                    note = null // Note is not persisted in DB yet
                ),
                geometry = NodeGeometry(listOf(entity.lon, entity.lat))
            )
        }
    }

    suspend fun loadEdges(context: Context): List<EdgeFeature> {
        // FIXED: Removed the extra argument here as well.
        val db = RailNavDatabase.getDatabase(context)
        val entities = db.graphDao().getAllEdges()

        // Map Entity -> Feature
        return entities.map { entity ->
            EdgeFeature(
                properties = EdgeProperties(
                    edge_id = entity.edgeId,
                    // Convert back to String to match legacy EdgeProperties
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
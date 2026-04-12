package com.app.railnav.data

import java.util.PriorityQueue
import kotlin.math.*

class Pathfinder(private val graph: Graph) {

    private fun heuristicDistance(nodeA: GraphNode, nodeB: GraphNode): Double {
        val lat1 = nodeA.coordinates[1]
        val lon1 = nodeA.coordinates[0]
        val lat2 = nodeB.coordinates[1]
        val lon2 = nodeB.coordinates[0]
        val earthRadius = 6371e3
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    // FIX: Added 'isAccessiblePreferred' with a default value of false
    fun findShortestPath(startNodeId: Int, endNodeId: Int, isAccessiblePreferred: Boolean = false): List<GraphNode>? {
        val startNode = graph.getNodeById(startNodeId)
        val endNode = graph.getNodeById(endNodeId)
        if (startNode == null || endNode == null) return null

        val cameFrom = mutableMapOf<GraphNode, GraphNode>()
        val costFromStart = mutableMapOf<GraphNode, Double>().withDefault { Double.POSITIVE_INFINITY }
        costFromStart[startNode] = 0.0

        val openSet = PriorityQueue<GraphNode> { a, b ->
            val fA = costFromStart.getValue(a) + heuristicDistance(a, endNode)
            val fB = costFromStart.getValue(b) + heuristicDistance(b, endNode)
            fA.compareTo(fB)
        }
        openSet.add(startNode)
        val closedSet = mutableSetOf<GraphNode>()

        while (openSet.isNotEmpty()) {
            val current = openSet.poll() ?: break
            if (current.properties.node_id == endNode.properties.node_id) {
                return reconstructPath(cameFrom, current)
            }
            if (current in closedSet) continue
            closedSet.add(current)

            current.neighbors.forEach { (neighborNode, distance, edgeProps) ->
                val currentLevel = current.properties.node_level
                val neighborLevel = neighborNode.properties.node_level
                val isVertical = edgeProps.edge_type?.let {
                    it.contains("STAIR", true) || it.contains("LIFT", true) || it.contains("ESCALATOR", true)
                } ?: false

                if (currentLevel != neighborLevel && !isVertical) return@forEach

                // =================================================================
                // NEW: ACCESSIBILITY COST LOGIC
                // =================================================================
                val weightMultiplier = if (isAccessiblePreferred) {
                    val isInaccessible = edgeProps.edge_accessibilty.equals("NO", ignoreCase = true) ||
                            edgeProps.edge_type?.contains("STAIR", true) == true ||
                            edgeProps.edge_type?.contains("ESCALATOR", true) == true
                    if (isInaccessible) 10000.0 else 1.0 // Massive 10,000x penalty to avoid stairs
                } else {
                    // Standard Mode: Just slightly penalize stairs to prefer flat paths
                    if (edgeProps.edge_type?.contains("STAIR", true) == true) 10.0 else 1.0
                }
                // =================================================================

                val tentativeGScore = costFromStart.getValue(current) + (distance * weightMultiplier)

                if (tentativeGScore < costFromStart.getValue(neighborNode)) {
                    cameFrom[neighborNode] = current
                    costFromStart[neighborNode] = tentativeGScore
                    if (neighborNode !in openSet) openSet.add(neighborNode)
                }
            }
        }
        return null
    }

    private fun reconstructPath(cameFrom: Map<GraphNode, GraphNode>, current: GraphNode): List<GraphNode> {
        val totalPath = mutableListOf(current)
        var currentStep = current
        while (currentStep in cameFrom.keys) {
            currentStep = cameFrom.getValue(currentStep)
            totalPath.add(0, currentStep)
        }
        return totalPath
    }
}
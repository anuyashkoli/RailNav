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

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun findShortestPath(startNodeId: Int, endNodeId: Int): List<GraphNode>? {
        val startNode = graph.getNodeById(startNodeId)
        val endNode = graph.getNodeById(endNodeId)

        if (startNode == null || endNode == null) return null

        val cameFrom = mutableMapOf<GraphNode, GraphNode>()
        val costFromStart = mutableMapOf<GraphNode, Double>().withDefault { Double.MAX_VALUE }
        val closedSet = mutableSetOf<GraphNode>()

        val openSet = PriorityQueue<GraphNode> { a, b ->
            (costFromStart.getValue(a) + heuristicDistance(a, endNode)).compareTo(
                costFromStart.getValue(b) + heuristicDistance(b, endNode)
            )
        }

        costFromStart[startNode] = 0.0
        openSet.add(startNode)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()!!

            if (current == endNode) return reconstructPath(cameFrom, current)

            if (current in closedSet) continue
            closedSet.add(current)

            // UPDATED: Destructure the Triple to get edgeProps
            current.neighbors.forEach { (neighborNode, distance, edgeProps) ->

                // NEW: Apply Weighted Heuristic
                // Penalize stairs to favor accessible routing as per roadmap
                val isStairway = edgeProps.edge_type?.contains("STAIR", ignoreCase = true) == true
                val weightMultiplier = if (isStairway) 10.0 else 1.0

                val weightedDistance = distance * weightMultiplier
                val tentativeGScore = costFromStart.getValue(current) + weightedDistance

                if (tentativeGScore < costFromStart.getValue(neighborNode)) {
                    cameFrom[neighborNode] = current
                    costFromStart[neighborNode] = tentativeGScore
                    if (neighborNode !in openSet) {
                        openSet.add(neighborNode)
                    }
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
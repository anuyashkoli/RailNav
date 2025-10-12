package com.app.railnav.data

import java.util.PriorityQueue
import kotlin.math.*

class Pathfinder(private val graph: Graph) {

    /**
     * Calculates the straight-line distance between two points on Earth using the Haversine formula.
     * This is our "heuristic" for the A* algorithm.
     */
    private fun heuristicDistance(nodeA: GraphNode, nodeB: GraphNode): Double {
        // Coordinates are stored as [longitude, latitude] in GeoJSON
        val lat1 = nodeA.coordinates[1]
        val lon1 = nodeA.coordinates[0]
        val lat2 = nodeB.coordinates[1]
        val lon2 = nodeB.coordinates[0]

        val earthRadius = 6371e3 // Earth radius in meters
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c // Distance in meters
    }

    /**
     * Finds the shortest path between two node IDs using the A* algorithm.
     * Returns a list of GraphNode objects, which contain coordinates for drawing.
     */
    fun findShortestPath(startNodeId: Int, endNodeId: Int): List<GraphNode>? {
        val startNode = graph.getNodeById(startNodeId)
        val endNode = graph.getNodeById(endNodeId)

        if (startNode == null || endNode == null) return null

        val cameFrom = mutableMapOf<GraphNode, GraphNode>()
        val costFromStart = mutableMapOf<GraphNode, Double>().withDefault { Double.MAX_VALUE }

        val openSet = PriorityQueue<GraphNode> { a, b ->
            (costFromStart.getValue(a) + heuristicDistance(a, endNode)).compareTo(
                costFromStart.getValue(b) + heuristicDistance(b, endNode)
            )
        }

        costFromStart[startNode] = 0.0
        openSet.add(startNode)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()!!

            if (current == endNode) {
                return reconstructPath(cameFrom, current)
            }

            current.neighbors.forEach { (neighborNode, distance) ->
                val tentativeGScore = costFromStart.getValue(current) + distance
                if (tentativeGScore < costFromStart.getValue(neighborNode)) {
                    cameFrom[neighborNode] = current
                    costFromStart[neighborNode] = tentativeGScore
                    if (neighborNode !in openSet) {
                        openSet.add(neighborNode)
                    }
                }
            }
        }

        return null // No path found
    }

    /**
     * Backtracks from the end node to the start node to build the final path.
     */
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
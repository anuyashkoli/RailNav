package com.app.railnav.data

// This class represents a single node within our graph.
// It holds its own properties AND a list of its direct neighbors.
class GraphNode(val properties: NodeProperties, val coordinates: List<Double>) {
    // A list of pairs: the neighbor node and the distance to it.
    val neighbors: MutableList<Pair<GraphNode, Double>> = mutableListOf()
}

// This class represents the entire interconnected network.
class Graph {
    // A map to easily find any node by its ID.
    private val nodes = mutableMapOf<Int, GraphNode>()

    // In Graph.kt, inside the Graph class

    // The function signature is the only part that changes
    fun build(nodeFeatures: List<NodeFeature>, edgeFeatures: List<EdgeFeature>) {
        // Step 1: Create a GraphNode for every Node Feature
        nodeFeatures.forEach { feature ->
            nodes[feature.properties.node_id] = GraphNode(feature.properties, feature.geometry.coordinates)
        }

        // Step 2: Go through each edge and create the connections.
        // We extract the properties from the full feature to build the graph.
        edgeFeatures.forEach { edgeFeature ->
            val edgeProp = edgeFeature.properties
            val startNode = nodes[edgeProp.start_id.toIntOrNull()]
            val endNode = nodes[edgeProp.end_id.toIntOrNull()]
            val distance = edgeProp.edge_distance.toDoubleOrNull()

            if (startNode != null && endNode != null && distance != null) {
                startNode.neighbors.add(Pair(endNode, distance))
            }
        }
    }

    /**
     * Finds a node by its ID and returns a formatted list of its neighbors.
     */
    fun getNeighborsForNode(nodeId: Int): List<String> {
        // Find the requested node in our map.
        val node = nodes[nodeId]

        // If the node doesn't exist, return a message.
        if (node == null) {
            return listOf("Node with ID $nodeId not found.")
        }

        // If the node has no neighbors, return a message.
        if (node.neighbors.isEmpty()) {
            return listOf("Node $nodeId has no outgoing connections.")
        }

        // If it has neighbors, create a nicely formatted string for each one.
        return node.neighbors.map { (neighborNode, distance) ->
            "-> Connects to Node ${neighborNode.properties.node_id} (Distance: $distance)"
        }
    }

    fun getNodeById(nodeId: Int): GraphNode? {
        return nodes[nodeId]
    }
}
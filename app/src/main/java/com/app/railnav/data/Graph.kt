package com.app.railnav.data

class GraphNode(val properties: NodeProperties, val coordinates: List<Double>) { // UPDATED: Store Triple(Neighbor, Distance, EdgeMetadata)

    val neighbors: MutableList<Triple<GraphNode, Double, EdgeProperties>> = mutableListOf()
}

class Graph {
    private val nodes = mutableMapOf<Int, GraphNode>()

    fun build(nodeFeatures: List<NodeFeature>, edgeFeatures: List<EdgeFeature>) {
        nodes.clear()

        // Step 1: Create GraphNodes
        nodeFeatures.forEach { feature ->
            nodes[feature.properties.node_id] = GraphNode(feature.properties, feature.geometry.coordinates)
        }

        // Step 2: Build connections with edge metadata
        edgeFeatures.forEach { edgeFeature ->
            val edgeProp = edgeFeature.properties
            val startNode = nodes[edgeProp.start_id.toIntOrNull()]
            val endNode = nodes[edgeProp.end_id.toIntOrNull()]
            val distance = edgeProp.edge_distance.toDoubleOrNull()

            if (startNode != null && endNode != null && distance != null) {
                // UPDATED: Pass the edgeProp to the neighbor list
                startNode.neighbors.add(Triple(endNode, distance, edgeProp))
            }
        }
    }

    fun getNodeById(nodeId: Int): GraphNode? = nodes[nodeId]
}
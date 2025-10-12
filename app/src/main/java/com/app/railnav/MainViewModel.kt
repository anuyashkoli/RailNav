package com.app.railnav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.railnav.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

// --- UI STATE IS UPDATED FOR SEARCH ---
data class MainUiState(
    val allNodeFeatures: List<NodeFeature> = emptyList(),
    val startNode: NodeFeature? = null,
    val endNode: NodeFeature? = null,
    val calculatedPath: List<GraphNode>? = null,
    val instructions: List<String> = emptyList(),
    val pathBoundingBox: BoundingBox? = null,
    val isLoading: Boolean = true,
    // New properties for search functionality
    val searchQuery: String = "",
    val searchResults: List<NodeFeature> = emptyList(),
    val userGpsLocation: GeoPoint? = null, // To store the user's actual location
    val nearestNodeCandidates: List<NodeFeature> = emptyList(), // To show choices
    val showNodeSelectionDialog: Boolean = false // To control the dialog
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val graph = Graph()
    private lateinit var pathfinder: Pathfinder
    private var allEdges: List<EdgeFeature> = emptyList()

    init {
        viewModelScope.launch {
            val nodeFeatures = GraphRepository.loadNodes(getApplication())
            val edgeFeatures = GraphRepository.loadEdges(getApplication())

            allEdges = edgeFeatures
            graph.build(nodeFeatures, edgeFeatures)
            pathfinder = Pathfinder(graph)

            _uiState.value = MainUiState(
                allNodeFeatures = nodeFeatures.sortedBy { it.properties.node_name },
                isLoading = false
            )
        }
    }

    // --- NEW: SEARCH LOGIC ---
    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.length > 1) { // Start searching after 2 characters
            val results = _uiState.value.allNodeFeatures.filter {
                it.properties.node_name?.contains(query, ignoreCase = true) == true
            }
            _uiState.value = _uiState.value.copy(searchResults = results)
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    // --- MODIFIED: Selects the destination AND clears the search ---
    fun onDestinationSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(
            endNode = node,
            searchQuery = node.properties.node_name ?: "", // Show selected name in bar
            searchResults = emptyList() // Hide search results
        )
    }


    fun getAllEdges(): List<EdgeFeature> = allEdges

    fun onStartNodeSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(startNode = node)
    }

    fun onEndNodeSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(endNode = node)
    }

    /**
     * Swaps the start and end nodes
     */
    fun swapNodes() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            startNode = currentState.endNode,
            endNode = currentState.startNode,
            // Clear the previous path when swapping
            calculatedPath = null,
            instructions = emptyList(),
            pathBoundingBox = null
        )
    }

    fun findPath() {
        val startId = _uiState.value.startNode?.properties?.node_id
        val endId = _uiState.value.endNode?.properties?.node_id

        if (startId != null && endId != null) {
            val path = pathfinder.findShortestPath(startId, endId)
            var boundingBox: BoundingBox? = null

            if (path != null && path.isNotEmpty()) {
                val minLat = path.minOf { it.coordinates[1] }
                val maxLat = path.maxOf { it.coordinates[1] }
                val minLon = path.minOf { it.coordinates[0] }
                val maxLon = path.maxOf { it.coordinates[0] }

                // Add padding to bounding box for better visualization
                val latPadding = (maxLat - minLat) * 0.1
                val lonPadding = (maxLon - minLon) * 0.1

                boundingBox = BoundingBox(
                    maxLat + latPadding,
                    maxLon + lonPadding,
                    minLat - latPadding,
                    minLon - lonPadding
                )
            }

            val instructions = if (path != null) {
                DirectionGenerator.generate(path)
            } else {
                listOf("No path could be found between the selected locations.")
            }

            _uiState.value = _uiState.value.copy(
                calculatedPath = path,
                instructions = instructions,
                pathBoundingBox = boundingBox
            )
        }
    }

    fun onZoomToPathComplete() {
        _uiState.value = _uiState.value.copy(pathBoundingBox = null)
    }

    fun setStartNodeByTap(tapPoint: GeoPoint) {
        val allNodes = _uiState.value.allNodeFeatures
        if (allNodes.isEmpty()) return

        val closestNode = allNodes.minByOrNull { node ->
            val nodePoint = GeoPoint(
                node.geometry.coordinates[1],
                node.geometry.coordinates[0]
            )
            nodePoint.distanceToAsDouble(tapPoint)
        }

        if (closestNode != null) {
            onStartNodeSelected(closestNode)
        }
    }

    /**
     * Handles the logic when a user taps a facility marker
     */
    fun onMarkerTapped(node: NodeFeature) {
        val currentState = _uiState.value

        when {
            // If no start point is selected yet, set it as start
            currentState.startNode == null -> {
                onStartNodeSelected(node)
            }
            // If start is selected but no end, set it as end
            currentState.endNode == null -> {
                onEndNodeSelected(node)
            }
            // If both are selected, reset and set new start
            else -> {
                onStartNodeSelected(node)
                _uiState.value = _uiState.value.copy(
                    endNode = null,
                    calculatedPath = null,
                    instructions = emptyList(),
                    pathBoundingBox = null
                )
            }
        }
    }

    /**
     * Clears the current route and selections
     */
    fun clearRoute() {
        _uiState.value = _uiState.value.copy(
            startNode = null,
            endNode = null,
            calculatedPath = null,
            instructions = emptyList(),
            pathBoundingBox = null
        )
    }

    // NEW: Called when the UI gets the user's location
    fun onLocationReceived(location: GeoPoint) {
        _uiState.value = _uiState.value.copy(userGpsLocation = location)

        val allNodes = _uiState.value.allNodeFeatures
        if (allNodes.isEmpty()) return

        // Find the top 3 closest nodes to the user's location
        val nearestNodes = allNodes
            .sortedBy { node ->
                val nodePoint = GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0])
                nodePoint.distanceToAsDouble(location)
            }
            .take(3)

        _uiState.value = _uiState.value.copy(
            nearestNodeCandidates = nearestNodes,
            showNodeSelectionDialog = true // Trigger the dialog to open
        )
    }

    // NEW: Called when the user confirms their choice from the dialog
    fun confirmStartNode(node: NodeFeature) {
        onStartNodeSelected(node)
        _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false)
    }

    // NEW: Dismisses the dialog
    fun dismissNodeSelectionDialog() {
        _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false)
    }
}
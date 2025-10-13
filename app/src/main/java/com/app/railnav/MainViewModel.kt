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

// --- UPDATED UI STATE ---
data class MainUiState(
    val allNodeFeatures: List<NodeFeature> = emptyList(),
    val startNode: NodeFeature? = null,
    val endNode: NodeFeature? = null,
    val calculatedPath: List<GraphNode>? = null,
    val instructions: List<String> = emptyList(),
    val pathBoundingBox: BoundingBox? = null,
    val isLoading: Boolean = true,

    // --- SEARCH RELATED ---
    val searchQuery: String = "",
    val searchResults: List<NodeFeature> = emptyList(),
    val userGpsLocation: GeoPoint? = null,
    val nearestNodeCandidates: List<NodeFeature> = emptyList(),
    val showNodeSelectionDialog: Boolean = false
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

    // ================================
    // ENHANCED SEARCH LOGIC
    // ================================
    // ===== UPDATE MainViewModel.kt =====
// Replace the onSearchQueryChanged method with this enhanced version

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        val allNodes = _uiState.value.allNodeFeatures
        val queryLower = query.lowercase()

        // Filter by name or type
        val matchedNodes = allNodes.filter { node ->
            val nodeName = node.properties.node_name?.lowercase() ?: ""
            val nodeType = node.properties.node_type?.lowercase() ?: ""
            nodeName.contains(queryLower) || nodeType.contains(queryLower)
        }

        // Sort by distance from START NODE if selected, otherwise by user GPS location
        val sortedResults = if (_uiState.value.startNode != null) {
            val startNodePoint = GeoPoint(
                _uiState.value.startNode!!.geometry.coordinates[1],
                _uiState.value.startNode!!.geometry.coordinates[0]
            )
            matchedNodes.sortedBy { node ->
                val nodePoint = GeoPoint(
                    node.geometry.coordinates[1],
                    node.geometry.coordinates[0]
                )
                startNodePoint.distanceToAsDouble(nodePoint)
            }
        } else if (_uiState.value.userGpsLocation != null) {
            // Fallback to user GPS location if no start node selected
            matchedNodes.sortedBy { node ->
                val nodePoint = GeoPoint(
                    node.geometry.coordinates[1],
                    node.geometry.coordinates[0]
                )
                _uiState.value.userGpsLocation!!.distanceToAsDouble(nodePoint)
            }
        } else {
            // Fallback to alphabetical sort
            matchedNodes.sortedBy { it.properties.node_name }
        }

        _uiState.value = _uiState.value.copy(searchResults = sortedResults)
    }

    fun onSearchResultSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(
            endNode = node,
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    // ================================
    // EXISTING LOGIC
    // ================================
    fun getAllEdges(): List<EdgeFeature> = allEdges

    fun onStartNodeSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(startNode = node)
    }

    fun onEndNodeSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(endNode = node)
    }

    fun swapNodes() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            startNode = currentState.endNode,
            endNode = currentState.startNode,
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

    fun onMarkerTapped(node: NodeFeature) {
        val currentState = _uiState.value

        when {
            currentState.startNode == null -> onStartNodeSelected(node)
            currentState.endNode == null -> onEndNodeSelected(node)
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

    fun clearRoute() {
        _uiState.value = _uiState.value.copy(
            startNode = null,
            endNode = null,
            calculatedPath = null,
            instructions = emptyList(),
            pathBoundingBox = null
        )
    }

    fun onLocationReceived(location: GeoPoint) {
        _uiState.value = _uiState.value.copy(userGpsLocation = location)

        val allNodes = _uiState.value.allNodeFeatures
        if (allNodes.isEmpty()) return

        val nearestNodes = allNodes
            .sortedBy {
                val nodePoint = GeoPoint(it.geometry.coordinates[1], it.geometry.coordinates[0])
                nodePoint.distanceToAsDouble(location)
            }
            .take(3)

        _uiState.value = _uiState.value.copy(
            nearestNodeCandidates = nearestNodes,
            showNodeSelectionDialog = true
        )
    }

    fun confirmStartNode(node: NodeFeature) {
        onStartNodeSelected(node)
        _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false)
    }

    fun dismissNodeSelectionDialog() {
        _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false)
    }
}

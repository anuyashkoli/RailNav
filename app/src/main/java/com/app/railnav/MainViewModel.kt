package com.app.railnav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.railnav.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import com.app.railnav.data.remote.RetrofitClient

data class MainUiState(
    val allNodeFeatures: List<NodeFeature> = emptyList(),
    val startNode: NodeFeature? = null,
    val endNode: NodeFeature? = null,
    val calculatedPath: List<GraphNode>? = null,
    val instructions: List<String> = emptyList(),
    val pathBoundingBox: BoundingBox? = null,
    val isLoading: Boolean = true,
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
            checkTrainStatus("11029");
        }
    }

    // ================================
    // SEARCH LOGIC (WITH FUZZY MATCH)
    // ================================

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        val allNodes = _uiState.value.allNodeFeatures
        val queryLower = query.lowercase()

        val matchedNodes = allNodes.filter { node ->
            val nodeName = node.properties.node_name?.lowercase() ?: ""
            val nodeType = node.properties.node_type?.lowercase() ?: ""

            // Production improvement: Standard contains check + Fuzzy Levenshtein match
            nodeName.contains(queryLower) ||
                    nodeType.contains(queryLower) ||
                    fuzzyMatch(queryLower, nodeName)
        }

        val sortedResults = if (_uiState.value.startNode != null) {
            val startNodePoint = GeoPoint(_uiState.value.startNode!!.geometry.coordinates[1], _uiState.value.startNode!!.geometry.coordinates[0])
            matchedNodes.sortedBy { node -> GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0]).distanceToAsDouble(startNodePoint) }
        } else if (_uiState.value.userGpsLocation != null) {
            matchedNodes.sortedBy { node -> GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0]).distanceToAsDouble(_uiState.value.userGpsLocation!!) }
        } else {
            matchedNodes.sortedBy { it.properties.node_name }
        }
        _uiState.value = _uiState.value.copy(searchResults = sortedResults)
    }

    private fun fuzzyMatch(query: String, target: String): Boolean {
        if (query.length < 3) return target.contains(query, ignoreCase = true)
        val maxErrors = 2
        val dp = Array(query.length + 1) { IntArray(target.length + 1) }
        for (i in 0..query.length) dp[i][0] = i
        for (j in 0..target.length) dp[0][j] = j
        for (i in 1..query.length) {
            for (j in 1..target.length) {
                val cost = if (query[i - 1] == target[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[query.length][target.length] <= maxErrors
    }

    // ================================
    // NAVIGATION & PATHFINDING
    // ================================

    fun findPath() {
        val startId = _uiState.value.startNode?.properties?.node_id
        val endId = _uiState.value.endNode?.properties?.node_id
        if (startId != null && endId != null) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.Default) {
                    val path = pathfinder.findShortestPath(startId, endId)
                    val boundingBox = calculateBoundingBox(path)
                    val instructions = if (path != null) DirectionGenerator.generate(path) else listOf("No path found.")
                    Triple(path, instructions, boundingBox)
                }
                _uiState.value = _uiState.value.copy(
                    calculatedPath = result.first,
                    instructions = result.second,
                    pathBoundingBox = result.third,
                    isLoading = false
                )
            }
        }
    }

    private fun calculateBoundingBox(path: List<GraphNode>?): BoundingBox? {
        if (path.isNullOrEmpty()) return null
        val minLat = path.minOf { it.coordinates[1] }
        val maxLat = path.maxOf { it.coordinates[1] }
        val minLon = path.minOf { it.coordinates[0] }
        val maxLon = path.maxOf { it.coordinates[0] }
        val latPadding = (maxLat - minLat) * 0.1
        val lonPadding = (maxLon - minLon) * 0.1
        return BoundingBox(maxLat + latPadding, maxLon + lonPadding, minLat - latPadding, minLon - lonPadding)
    }

    // ================================
    // INTERACTION HANDLERS
    // ================================

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

    fun setStartNodeByTap(tapPoint: GeoPoint) {
        val allNodes = _uiState.value.allNodeFeatures
        if (allNodes.isEmpty()) return
        val closestNode = allNodes.minByOrNull { node ->
            GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0]).distanceToAsDouble(tapPoint)
        }
        if (closestNode != null) onStartNodeSelected(closestNode)
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

    fun onLocationReceived(location: GeoPoint) {
        _uiState.value = _uiState.value.copy(userGpsLocation = location)
        val allNodes = _uiState.value.allNodeFeatures
        if (allNodes.isEmpty()) return
        val nearestNodes = allNodes.sortedBy { GeoPoint(it.geometry.coordinates[1], it.geometry.coordinates[0]).distanceToAsDouble(location) }.take(3)
        _uiState.value = _uiState.value.copy(nearestNodeCandidates = nearestNodes, showNodeSelectionDialog = true)
    }

    fun onSearchResultSelected(node: NodeFeature) { _uiState.value = _uiState.value.copy(endNode = node, searchQuery = "", searchResults = emptyList()) }
    fun onStartNodeSelected(node: NodeFeature) { _uiState.value = _uiState.value.copy(startNode = node) }
    fun onEndNodeSelected(node: NodeFeature) { _uiState.value = _uiState.value.copy(endNode = node) }
    fun getAllEdges(): List<EdgeFeature> = allEdges
    fun onZoomToPathComplete() { _uiState.value = _uiState.value.copy(pathBoundingBox = null) }
    fun confirmStartNode(node: NodeFeature) { onStartNodeSelected(node); _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false) }
    fun dismissNodeSelectionDialog() { _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false) }

    // ADD THIS NEW FUNCTION
    fun checkTrainStatus(trainNumber: String) {
        viewModelScope.launch {
            try {
                println("DEBUG: Starting API call for $trainNumber...") // Log start
                _uiState.value = _uiState.value.copy(isLoading = true)

                val response = RetrofitClient.api.getTrainSchedule(
                    apiKey = "f1207f505dmsh7e8c7533c3f8f4dp11f1e9jsn498a3d1e06f5",
                    trainNumber = trainNumber
                )

                // Log the raw success status
                println("DEBUG: API Response Success: ${response.success}")

                if (response.success) {
                    println("DEBUG: Train Schedule Fetched: ${response.data.size} stations")
                    // Use response.data here...
                } else {
                    println("DEBUG: API returned success=false. Check API key or quotas.")
                }

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                // Log the actual error message
                println("DEBUG: Network Error: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
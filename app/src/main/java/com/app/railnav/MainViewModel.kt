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

data class MainUiState(
    // ── existing graph/navigation state ─────────────────────────────────────
    val allNodeFeatures: List<NodeFeature> = emptyList(),
    val startNode: NodeFeature? = null,
    val endNode: NodeFeature? = null,
    val calculatedPath: List<GraphNode>? = null,
    val instructions: List<String> = emptyList(),
    val pathBoundingBox: BoundingBox? = null,
    val isLoading: Boolean = true,

    // ── legacy manual-node search (used in advanced mode) ───────────────────
    val searchQuery: String = "",
    val searchResults: List<NodeFeature> = emptyList(),

    // ── location ────────────────────────────────────────────────────────────
    val userGpsLocation: GeoPoint? = null,
    val isTrackingModeActive: Boolean = false, // <-- NEW ADDITION
    val nearestNodeCandidates: List<NodeFeature> = emptyList(),
    val showNodeSelectionDialog: Boolean = false,

    // ── train / station mode (Feature 1 & 2) ────────────────────────────────
    /** false = train-destination mode (default);  true = manual node picker */
    val isAdvancedMode: Boolean = false,
    /** What the user is typing in the "Where are you going?" field */
    val trainDestinationQuery: String = "",
    /** Station name suggestions shown as autocomplete */
    val destinationSuggestions: List<String> = emptyList(),
    /** The station the user confirmed */
    val selectedDestination: String? = null,
    /** Upcoming trains for the selected destination */
    val availableTrains: List<TrainSchedule> = emptyList(),
    /** The train the user tapped */
    val selectedTrain: TrainSchedule? = null,
    /** Whether the train-list bottom-sheet is open */
    val showTrainSheet: Boolean = false,

    // ── facilities (Feature 1 – secondary) ──────────────────────────────────
    val showFacilitiesSheet: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var forceNextLocationPrompt = false

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

    // ══════════════════════════════════════════════════════════════════════
    //  Mode toggle
    // ══════════════════════════════════════════════════════════════════════

    fun toggleAdvancedMode() {
        _uiState.value = _uiState.value.copy(
            isAdvancedMode = !_uiState.value.isAdvancedMode
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Train / station destination logic
    // ══════════════════════════════════════════════════════════════════════

    fun onTrainDestinationQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            trainDestinationQuery = query,
            destinationSuggestions = TrainRepository.searchDestinations(query),
            selectedDestination = null,
            availableTrains = emptyList(),
            selectedTrain = null,
            endNode = null
        )
    }

    fun onDestinationSelected(station: String) {
        val trains = TrainRepository.getUpcomingTrains(station)
        _uiState.value = _uiState.value.copy(
            trainDestinationQuery = station,
            destinationSuggestions = emptyList(),
            selectedDestination = station,
            availableTrains = trains,
            showTrainSheet = trains.isNotEmpty(),
            selectedTrain = null,
            endNode = null
        )
    }

    fun onTrainSelected(train: TrainSchedule) {
        val platformNodeId = TrainRepository.getPlatformNodeId(train.platformAtThane)
        val endNode = _uiState.value.allNodeFeatures.find {
            it.properties.node_id == platformNodeId
        }
        _uiState.value = _uiState.value.copy(
            selectedTrain = train,
            endNode = endNode,
            showTrainSheet = false
        )
    }

    fun openTrainSheet() {
        if (_uiState.value.availableTrains.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(showTrainSheet = true)
        }
    }

    fun dismissTrainSheet() {
        _uiState.value = _uiState.value.copy(showTrainSheet = false)
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Facilities
    // ══════════════════════════════════════════════════════════════════════

    fun showFacilities() {
        _uiState.value = _uiState.value.copy(showFacilitiesSheet = true)
    }

    fun dismissFacilities() {
        _uiState.value = _uiState.value.copy(showFacilitiesSheet = false)
    }

    fun navigateToFacility(facility: FacilityItem) {
        val node = _uiState.value.allNodeFeatures.find {
            it.properties.node_id == facility.nodeId
        }
        _uiState.value = _uiState.value.copy(
            endNode = node,
            showFacilitiesSheet = false
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Legacy manual-node search (advanced mode)
    // ══════════════════════════════════════════════════════════════════════

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        val allNodes = _uiState.value.allNodeFeatures
        val queryLower = query.lowercase()
        val matchedNodes = allNodes.filter { node ->
            val name = node.properties.node_name?.lowercase() ?: ""
            val type = node.properties.node_type?.lowercase() ?: ""
            name.contains(queryLower) || type.contains(queryLower) || fuzzyMatch(queryLower, name)
        }
        val sortedResults = sortByProximity(matchedNodes)
        _uiState.value = _uiState.value.copy(searchResults = sortedResults)
    }

    private fun sortByProximity(nodes: List<NodeFeature>): List<NodeFeature> {
        val referencePoint: GeoPoint? = _uiState.value.startNode?.let {
            GeoPoint(it.geometry.coordinates[1], it.geometry.coordinates[0])
        } ?: _uiState.value.userGpsLocation

        return if (referencePoint != null) {
            nodes
                .map { node ->
                    node to GeoPoint(
                        node.geometry.coordinates[1],
                        node.geometry.coordinates[0]
                    ).distanceToAsDouble(referencePoint)
                }
                .sortedBy { (_, dist) -> dist }
                .map { (node, _) -> node }
        } else {
            nodes.sortedBy { it.properties.node_name }
        }
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

    // ══════════════════════════════════════════════════════════════════════
    //  Pathfinding
    // ══════════════════════════════════════════════════════════════════════

    fun findPath() {
        val startId = _uiState.value.startNode?.properties?.node_id
        val endId   = _uiState.value.endNode?.properties?.node_id
        if (startId == null || endId == null) return

        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val path         = pathfinder.findShortestPath(startId, endId)
                    val boundingBox  = calculateBoundingBox(path)
                    val instructions = if (path != null) {
                        DirectionGenerator.generate(path)
                    } else {
                        listOf("No path found between the selected nodes.")
                    }
                    Triple(path, instructions, boundingBox)
                }
                _uiState.value = _uiState.value.copy(
                    calculatedPath  = result.first,
                    instructions    = result.second,
                    pathBoundingBox = result.third,
                    isLoading       = false
                )
            } catch (_: Exception) { // FIX: Removed unused 'e' parameter
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    instructions = listOf("An error occurred while calculating the route.")
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
        val latPad = (maxLat - minLat) * 0.1
        val lonPad = (maxLon - minLon) * 0.1
        return BoundingBox(maxLat + latPad, maxLon + lonPad, minLat - latPad, minLon - lonPad)
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Interaction helpers
    // ══════════════════════════════════════════════════════════════════════

    fun swapNodes() {
        val s = _uiState.value
        _uiState.value = s.copy(
            startNode       = s.endNode,
            endNode         = s.startNode,
            calculatedPath  = null,
            instructions    = emptyList(),
            pathBoundingBox = null
        )
    }

    fun setStartNodeByTap(tapPoint: GeoPoint) {
        val allNodes = _uiState.value.allNodeFeatures
        if (allNodes.isEmpty()) return
        val closest = allNodes.minByOrNull { node ->
            GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0])
                .distanceToAsDouble(tapPoint)
        }
        if (closest != null) onStartNodeSelected(closest)
    }

    fun onMarkerTapped(node: NodeFeature) {
        val s = _uiState.value
        when {
            s.startNode == null -> onStartNodeSelected(node)
            s.endNode   == null -> onEndNodeSelected(node)
            else -> {
                onStartNodeSelected(node)
                _uiState.value = _uiState.value.copy(
                    endNode         = null,
                    calculatedPath  = null,
                    instructions    = emptyList(),
                    pathBoundingBox = null
                )
            }
        }
    }

    fun onLocationReceived(location: GeoPoint) {
        _uiState.value = _uiState.value.copy(userGpsLocation = location)

        val currentState = _uiState.value

        // ====================================================================
        //  DYNAMIC REROUTING: OFF-ROUTE DETECTION
        // ====================================================================
        // Only run this if a route is active, a destination exists, and we aren't currently loading a route
        if (currentState.calculatedPath != null && currentState.endNode != null && !currentState.isLoading) {

            // 1. Calculate how far the user is from the closest point on the active path
            val minDistanceToPath = currentState.calculatedPath.minOfOrNull { pathNode ->
                GeoPoint(pathNode.coordinates[1], pathNode.coordinates[0]).distanceToAsDouble(location)
            } ?: 0.0

            // 2. If they wander more than 25 meters away from the line, they are off-route!
            if (minDistanceToPath > 25.0) {
                // Find the nearest station node to their current rogue location
                val nearestNode = currentState.allNodeFeatures.minByOrNull { node ->
                    GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0]).distanceToAsDouble(location)
                }

                if (nearestNode != null && nearestNode.properties.node_id != currentState.startNode?.properties?.node_id) {
                    // Update the start node to their new location and recalculate!
                    _uiState.value = currentState.copy(startNode = nearestNode)
                    findPath() // This automatically handles the loading state and draws the new line
                }
            }
        }
        // ====================================================================

        // If user didn't explicitly click the GPS button, and a node/path exists, stay quiet.
        if (!forceNextLocationPrompt && (currentState.startNode != null || currentState.calculatedPath != null)) {
            return
        }

        // Reset the flag since we are about to show the dialog
        forceNextLocationPrompt = false

        val allNodes = currentState.allNodeFeatures
        if (allNodes.isEmpty()) return

        val nearest = allNodes
            .sortedBy {
                GeoPoint(it.geometry.coordinates[1], it.geometry.coordinates[0]).distanceToAsDouble(location)
            }
            .take(3)

        _uiState.value = currentState.copy(
            nearestNodeCandidates = nearest,
            showNodeSelectionDialog = true
        )
    }

    fun onGpsButtonClicked() {
        forceNextLocationPrompt = true
        // Turn tracking mode ON when they explicitly hit the GPS button
        _uiState.value = _uiState.value.copy(isTrackingModeActive = true)
        _uiState.value.userGpsLocation?.let { onLocationReceived(it) }
    }

    fun disableTrackingMode() {
        // Turn tracking mode OFF (called when the user touches the map)
        if (_uiState.value.isTrackingModeActive) {
            _uiState.value = _uiState.value.copy(isTrackingModeActive = false)
        }
    }

    fun clearRoute() {
        _uiState.value = _uiState.value.copy(
            calculatedPath = null,
            instructions = emptyList(),
            pathBoundingBox = null,
            startNode = null,
            endNode = null,
            selectedTrain = null,
            selectedDestination = null
        )
    }

    fun clearStartNode() {
        _uiState.value = _uiState.value.copy(startNode = null, calculatedPath = null, instructions = emptyList())
    }

    fun clearEndNode() {
        _uiState.value = _uiState.value.copy(endNode = null, calculatedPath = null, instructions = emptyList())
    }

    fun onSearchResultSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(
            endNode     = node,
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    fun onStartNodeSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(startNode = node)
    }

    fun onEndNodeSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(endNode = node)
    }

    fun getAllEdges(): List<EdgeFeature> = allEdges

    fun onZoomToPathComplete() {
        _uiState.value = _uiState.value.copy(pathBoundingBox = null)
    }

    fun confirmStartNode(node: NodeFeature) {
        onStartNodeSelected(node)
        _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false)
    }

    fun dismissNodeSelectionDialog() {
        _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false)
    }

    // ================================
    // FACILITY QUICK CHIPS LOGIC
    // ================================

    fun routeToNearestFacility(keyword: String) {
        val currentState = _uiState.value

        // Use GPS location if available; otherwise, use the selected start node
        val referenceLocation = currentState.userGpsLocation
            ?: currentState.startNode?.let { GeoPoint(it.geometry.coordinates[1], it.geometry.coordinates[0]) }

        if (referenceLocation == null) return

        val allNodes = currentState.allNodeFeatures
        val queryLower = keyword.lowercase()

        val matchedNodes = allNodes.filter { node ->
            val nodeName = node.properties.node_name?.lowercase() ?: ""
            val nodeType = node.properties.node_type?.lowercase() ?: ""
            nodeName.contains(queryLower) || nodeType.contains(queryLower)
        }

        if (matchedNodes.isEmpty()) return

        val closestNode = matchedNodes.minByOrNull { node ->
            GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0]).distanceToAsDouble(referenceLocation)
        }

        if (closestNode != null) {
            var finalStartNode = currentState.startNode

            // FIX: Removed the redundant '!= null' warning by knowing referenceLocation enforces it.
            if (finalStartNode == null) {
                finalStartNode = allNodes.minByOrNull {
                    GeoPoint(it.geometry.coordinates[1], it.geometry.coordinates[0]).distanceToAsDouble(currentState.userGpsLocation!!)
                }
            }

            _uiState.value = currentState.copy(
                startNode = finalStartNode,
                endNode = closestNode,
                searchQuery = "",
                searchResults = emptyList()
            )

            if (finalStartNode != null) {
                findPath()
            }
        }
    }
}
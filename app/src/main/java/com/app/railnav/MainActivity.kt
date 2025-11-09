package com.app.railnav

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.railnav.data.NodeFeature
import com.app.railnav.ui.theme.RailNavTheme
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

// Main Activity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RailNavTheme {
                PathfindingScreen()
            }
        }
    }
}


// Main Screen Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfindingScreen(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel()
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    var showInstructions by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Location handling
    val locationHandler = remember {
        LocationHandler(
            context = context,
            onLocationReceived = { geoPoint -> mainViewModel.onLocationReceived(geoPoint) },
            onPermissionDenied = { showPermissionDialog = true }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) locationHandler.startLocationUpdates()
        else showPermissionDialog = true
    }

    DisposableEffect(Unit) { onDispose { locationHandler.stopLocationUpdates() } }

    LaunchedEffect(uiState.instructions) {
        if (uiState.instructions.isNotEmpty()) showInstructions = true
    }

    RailNavTheme(darkTheme = isDarkTheme) {
        Box(modifier = modifier.fillMaxSize()) {

            // ---------------- Map View ----------------
            MapView(
                modifier = Modifier.fillMaxSize(),
                path = uiState.calculatedPath,
                boundingBox = uiState.pathBoundingBox,
                onZoomComplete = { mainViewModel.onZoomToPathComplete() },
                onMapTap = { mainViewModel.setStartNodeByTap(it) },
                allEdges = mainViewModel.getAllEdges(),
                allNodes = uiState.allNodeFeatures,
                onMarkerTap = { mainViewModel.onMarkerTapped(it) },
                userGpsLocation = uiState.userGpsLocation,
                startNode = uiState.startNode
            )

            // ---------------- Search Section ----------------
            AnimatedVisibility(
                visible = !uiState.isLoading,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SearchCard(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        onStartNodeSelected = { mainViewModel.onStartNodeSelected(it) },
                        onEndNodeSelected = { mainViewModel.onEndNodeSelected(it) },
                        onFindPath = {
                            mainViewModel.findPath()
                            scope.launch { showInstructions = true }
                        },
                        onSearchQueryChanged = { mainViewModel.onSearchQueryChanged(it) }
                    )

                    if (uiState.searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchResultsList(
                            results = uiState.searchResults,
                            userLocation = uiState.userGpsLocation,
                            onSelect = { mainViewModel.onSearchResultSelected(it) }
                        )
                    }
                }
            }

            // ---------------- Map Controls ----------------
            MapControls(
                isDarkTheme = isDarkTheme,
                onToggleTheme = { isDarkTheme = !isDarkTheme },
                onMyLocationClick = {
                    if (locationHandler.hasLocationPermission())
                        locationHandler.startLocationUpdates()
                    else
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onSwapNodes = { mainViewModel.swapNodes() }, // Pass swap function here
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
            )

            // ---------------- Floating Route Button ----------------
            if (uiState.calculatedPath != null) {
                ExtendedFloatingActionButton(
                    onClick = { showInstructions = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { Icon(Icons.Default.Directions, "Show Directions") },
                    text = { Text("View Route") }
                )
            }

            // ---------------- Loading Indicator ----------------
            if (uiState.isLoading)
                CircularProgressIndicator(Modifier.align(Alignment.Center))

            // ---------------- Node Selection Dialog ----------------
            if (uiState.showNodeSelectionDialog) {
                NodeSelectionDialog(
                    nearestNodes = uiState.nearestNodeCandidates,
                    userLocation = uiState.userGpsLocation,
                    onNodeSelected = { mainViewModel.confirmStartNode(it) },
                    onDismiss = { mainViewModel.dismissNodeSelectionDialog() }
                )
            }

            // ---------------- Location Permission Dialog ----------------
            if (showPermissionDialog) {
                LocationPermissionDialog(
                    onGrant = {
                        showPermissionDialog = false
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    onDismiss = { showPermissionDialog = false }
                )
            }

            // ---------------- Instructions Sheet ----------------
            if (showInstructions && uiState.instructions.isNotEmpty()) {
                ModalBottomSheet(
                    onDismissRequest = { showInstructions = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    InstructionsSheet(
                        instructions = uiState.instructions,
                        startNode = uiState.startNode,
                        endNode = uiState.endNode,
                        onClose = { showInstructions = false }
                    )
                }
            }
        }
    }
}

// =======================================
// Search UI Composables
// =======================================
@Composable
fun SearchCard(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onStartNodeSelected: (NodeFeature) -> Unit,
    onEndNodeSelected: (NodeFeature) -> Unit,
    onFindPath: () -> Unit,
    onSearchQueryChanged: (String) -> Unit
) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start and Destination selectors are now directly in the Column
            ModernNodeSelector(
                label = "Start Point",
                icon = Icons.Default.LocationOn,
                nodes = uiState.allNodeFeatures,
                selectedNode = uiState.startNode,
                onNodeSelected = onStartNodeSelected,
                iconTint = Color(0xFF4CAF50)
            )
            ModernNodeSelector(
                label = "Destination",
                icon = Icons.Default.Flag,
                nodes = uiState.allNodeFeatures,
                selectedNode = uiState.endNode,
                onNodeSelected = onEndNodeSelected,
                iconTint = Color(0xFFF44336)
            )

            // Search input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                label = { Text("Search: Exit, Ticket, Platform...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val closest = uiState.searchResults.firstOrNull()
                        val userLocation = uiState.userGpsLocation

                        if (closest != null && userLocation != null) {
                            val nodePoint = GeoPoint(
                                closest.geometry.coordinates[1],
                                closest.geometry.coordinates[0]
                            )
                            val distance = userLocation.distanceToAsDouble(nodePoint).toInt()

                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "~${distance}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            )

            Button(
                onClick = onFindPath,
                enabled = uiState.startNode != null && uiState.endNode != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Navigation, null)
                Spacer(Modifier.width(8.dp))
                Text("Find Best Route", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SearchResultsList(
    results: List<NodeFeature>,
    userLocation: GeoPoint?,
    onSelect: (NodeFeature) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(results) { node ->
                SearchResultItem(
                    node = node,
                    userLocation = userLocation,
                    onClick = { onSelect(node) }
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    node: NodeFeature,
    userLocation: GeoPoint?,
    onClick: () -> Unit
) {
    val distance = userLocation?.let {
        val nodePoint = GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0])
        it.distanceToAsDouble(nodePoint).toInt()
    }

    val name = node.properties.node_name ?: "Unknown"
    val type = node.properties.node_type
    val note = node.properties.note

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                // 1️⃣ Node name
                Text(name, fontWeight = FontWeight.SemiBold)

                // 2️⃣ Node type (if available)
                if (!type.isNullOrBlank()) {
                    Text(type, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 3️⃣ Note (optional)
                if (!note.isNullOrBlank()) {
                    Text(
                        note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic

                    )
                }
            }

            // 4️⃣ Distance (if available)
            distance?.let {
                Text(
                    "~${it}m",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// ===================================
// Map Controls and Dialogs
// ===================================
@Composable
fun MapControls(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onMyLocationClick: () -> Unit,
    onSwapNodes: () -> Unit, // Added parameter for swapping
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MapControlButton(
            icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
            onClick = onToggleTheme
        )
        // New Swap Button added here
        MapControlButton(
            icon = Icons.Default.SwapVert,
            onClick = onSwapNodes
        )
        MapControlButton(
            icon = Icons.Default.MyLocation,
            onClick = onMyLocationClick
        )
    }
}

@Composable
fun MapControlButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun LocationPermissionDialog(onGrant: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOff, null) },
        title = { Text("Location Permission Required") },
        text = { Text("Grant location permission to show your position and navigate.") },
        confirmButton = { TextButton(onClick = onGrant) { Text("Grant Permission") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


// Node Selection & Instructions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNodeSelector(
    label: String,
    icon: ImageVector,
    nodes: List<NodeFeature>,
    selectedNode: NodeFeature?,
    onNodeSelected: (NodeFeature) -> Unit,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedNode?.properties?.node_name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null, tint = iconTint) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            nodes.forEach { node ->
                DropdownMenuItem(
                    text = { Text(node.properties.node_name ?: "Unnamed Node") },
                    onClick = {
                        onNodeSelected(node)
                        expanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.Place, null, Modifier.size(20.dp)) }
                )
            }
        }
    }
}

@Composable
fun NodeSelectionDialog(
    nearestNodes: List<NodeFeature>,
    userLocation: GeoPoint?,
    onNodeSelected: (NodeFeature) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Choose Your Starting Point", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("We found these nearby locations. Select one:", Modifier.padding(bottom = 16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(nearestNodes) { node ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onNodeSelected(node) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(node.properties.node_name ?: "Unknown", fontWeight = FontWeight.Bold)
                                    node.properties.node_type?.let {
                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    userLocation?.let {
                                        val nodePoint = GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0])
                                        val distance = it.distanceToAsDouble(nodePoint).toInt()
                                        Text("~${distance}m", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun InstructionsSheet(
    instructions: List<String>,
    startNode: NodeFeature?,
    endNode: NodeFeature?,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Route Instructions", fontWeight = FontWeight.Bold)
                Text("${startNode?.properties?.node_name} → ${endNode?.properties?.node_name}")
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
        }
        Divider()
        LazyColumn(
            modifier = Modifier.heightIn(max = 400.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(instructions) { index, instruction ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (index == instructions.lastIndex) Color(0xFFF44336)
                                else MaterialTheme.colorScheme.primary,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == instructions.lastIndex)
                            Icon(Icons.Default.Flag, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        else
                            Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(instruction, Modifier.weight(1f))
                }
            }
        }
    }
}
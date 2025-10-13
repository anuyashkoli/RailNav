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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.railnav.data.NodeFeature
import com.app.railnav.ui.theme.RailNavTheme
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

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

    // Location handler
    val locationHandler = remember {
        LocationHandler(
            context = context,
            onLocationReceived = { geoPoint ->
                mainViewModel.onLocationReceived(geoPoint)
            },
            onPermissionDenied = {
                showPermissionDialog = true
            }
        )
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            locationHandler.startLocationUpdates()
        } else {
            showPermissionDialog = true
        }
    }

    // Stop location updates when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            locationHandler.stopLocationUpdates()
        }
    }

    // Show instructions when path is calculated
    LaunchedEffect(uiState.instructions) {
        if (uiState.instructions.isNotEmpty()) {
            showInstructions = true
        }
    }

    RailNavTheme(darkTheme = isDarkTheme) {
        Box(modifier = modifier.fillMaxSize()) {
            // Map View
            MapView(
                modifier = Modifier.fillMaxSize(),
                path = uiState.calculatedPath,
                boundingBox = uiState.pathBoundingBox,
                onZoomComplete = { mainViewModel.onZoomToPathComplete() },
                onMapTap = { geoPoint -> mainViewModel.setStartNodeByTap(geoPoint) },
                allEdges = mainViewModel.getAllEdges(),
                allNodes = uiState.allNodeFeatures,
                onMarkerTap = { nodeFeature -> mainViewModel.onMarkerTapped(nodeFeature) },
                userGpsLocation = uiState.userGpsLocation,
                startNode = uiState.startNode
            )

            // Modern Top Search Card
            AnimatedVisibility(
                visible = !uiState.isLoading,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                SearchCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    uiState = uiState,
                    onStartNodeSelected = { mainViewModel.onStartNodeSelected(it) },
                    onEndNodeSelected = { mainViewModel.onEndNodeSelected(it) },
                    onFindPath = {
                        mainViewModel.findPath()
                        scope.launch { showInstructions = true }
                    },
                    onSwapNodes = { mainViewModel.swapNodes() }
                )
            }

            // Map Controls (Right side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme Toggle
                MapControlButton(
                    icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    onClick = { isDarkTheme = !isDarkTheme }
                )

                // My Location Button
                MapControlButton(
                    icon = Icons.Default.MyLocation,
                    onClick = {
                        if (locationHandler.hasLocationPermission()) {
                            locationHandler.startLocationUpdates()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                )
            }

            // Floating Action Button for Instructions
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

            // Loading Indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Node Selection Dialog (Choose nearest nodes)
            if (uiState.showNodeSelectionDialog) {
                NodeSelectionDialog(
                    nearestNodes = uiState.nearestNodeCandidates,
                    userLocation = uiState.userGpsLocation,
                    onNodeSelected = { mainViewModel.confirmStartNode(it) },
                    onDismiss = { mainViewModel.dismissNodeSelectionDialog() }
                )
            }

            // Permission Dialog
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    icon = { Icon(Icons.Default.LocationOff, contentDescription = null) },
                    title = { Text("Location Permission Required") },
                    text = {
                        Text("This app needs location permission to show your current position and help you navigate through the station.")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showPermissionDialog = false
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }) {
                            Text("Grant Permission")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Instructions Bottom Sheet
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

@Composable
fun NodeSelectionDialog(
    nearestNodes: List<NodeFeature>,
    userLocation: GeoPoint?,
    onNodeSelected: (NodeFeature) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Choose Your Starting Point",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "We found these nearby locations. Select the one closest to where you are:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(nearestNodes) { node ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNodeSelected(node) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        node.properties.node_name ?: "Unknown Location",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (node.properties.node_type != null) {
                                        Text(
                                            node.properties.node_type,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (userLocation != null) {
                                        val nodePoint = GeoPoint(
                                            node.geometry.coordinates[1],
                                            node.geometry.coordinates[0]
                                        )
                                        val distance = userLocation.distanceToAsDouble(nodePoint)
                                        Text(
                                            "~${distance.toInt()} meters away",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SearchCard(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onStartNodeSelected: (NodeFeature) -> Unit,
    onEndNodeSelected: (NodeFeature) -> Unit,
    onFindPath: () -> Unit,
    onSwapNodes: () -> Unit
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
            // Start Point Selector
            ModernNodeSelector(
                label = "Start Point",
                icon = Icons.Default.LocationOn,
                nodes = uiState.allNodeFeatures,
                selectedNode = uiState.startNode,
                onNodeSelected = onStartNodeSelected,
                iconTint = Color(0xFF4CAF50)
            )

            // Swap Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onSwapNodes,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = "Swap",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Destination Selector
            ModernNodeSelector(
                label = "Destination",
                icon = Icons.Default.Flag,
                nodes = uiState.allNodeFeatures,
                selectedNode = uiState.endNode,
                onNodeSelected = onEndNodeSelected,
                iconTint = Color(0xFFF44336)
            )

            // Find Path Button
            Button(
                onClick = onFindPath,
                enabled = uiState.startNode != null && uiState.endNode != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Find Best Route", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNodeSelector(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    nodes: List<NodeFeature>,
    selectedNode: NodeFeature?,
    onNodeSelected: (NodeFeature) -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedNode?.properties?.node_name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            nodes.forEach { node ->
                DropdownMenuItem(
                    text = {
                        Text(
                            node.properties.node_name ?: "Unnamed Node",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onNodeSelected(node)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MapControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InstructionsSheet(
    instructions: List<String>,
    startNode: NodeFeature?,
    endNode: NodeFeature?,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Route Instructions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${startNode?.properties?.node_name} → ${endNode?.properties?.node_name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close")
            }
        }

        Divider()

        // Instructions List
        LazyColumn(
            modifier = Modifier.heightIn(max = 400.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(instructions) { index, instruction ->
                InstructionItem(
                    step = index + 1,
                    instruction = instruction,
                    isLast = index == instructions.size - 1
                )
            }
        }
    }
}

@Composable
fun InstructionItem(
    step: Int,
    instruction: String,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step indicator
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isLast) Color(0xFFF44336) else MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLast) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    step.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Instruction text
        Text(
            instruction,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
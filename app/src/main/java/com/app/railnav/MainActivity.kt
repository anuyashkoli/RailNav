package com.app.railnav

import android.Manifest
import android.os.Bundle
import java.util.Calendar
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.app.railnav.data.*
import com.app.railnav.ui.theme.RailNavTheme
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.style.TextAlign
import com.google.android.gms.common.api.ResolvableApiException
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RailNavTheme { PathfindingScreen() } }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfindingScreen(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel()
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val instructionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val trainSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    var showInstructions by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val locationHandler = remember {
        LocationHandler(
            context = context.applicationContext,
            onLocationReceived = { mainViewModel.onLocationReceived(it) },
            onPermissionDenied = { showPermissionDialog = true }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) locationHandler.startLocationUpdates() else showPermissionDialog = true
    }
    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // The user clicked "Turn on" in the Google dialog!
            mainViewModel.onGpsButtonClicked()
            locationHandler.startLocationUpdates()
        }
    }
    DisposableEffect(Unit) { onDispose { locationHandler.stopLocationUpdates() } }

    LaunchedEffect(uiState.instructions) {
        if (uiState.instructions.isNotEmpty()) showInstructions = true
    }

    // FIX: Added the BackHandler to intercept the back button and clear the route map!
    BackHandler(enabled = uiState.calculatedPath != null) {
        mainViewModel.clearRoute()
        showInstructions = false
    }

    RailNavTheme(darkTheme = isDarkTheme) {
        Box(modifier = modifier.fillMaxSize()) {

            // ── Map ────────────────────────────────────────────────────────
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
                isTrackingModeActive = uiState.isTrackingModeActive,
                onDisableTracking = { mainViewModel.disableTrackingMode() },
                isDarkTheme = isDarkTheme,
                startNode = uiState.startNode
            )

            // ── Top search card ────────────────────────────────────────────
            AnimatedVisibility(
                visible = !uiState.isLoading,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    if (uiState.isAdvancedMode) {
                        AdvancedSearchCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            uiState = uiState,
                            onStartNodeSelected = { mainViewModel.onStartNodeSelected(it) },
                            onEndNodeSelected = { mainViewModel.onEndNodeSelected(it) },
                            onFindPath = {
                                mainViewModel.findPath()
                                scope.launch { showInstructions = true }
                            },
                            onSearchQueryChanged = { mainViewModel.onSearchQueryChanged(it) },
                            onSwitchToSimpleMode = { mainViewModel.toggleAdvancedMode() },
                            // FIX: Passing the viewmodel calls as lambdas to resolve Unresolved References!
                            onClearStartNode = { mainViewModel.clearStartNode() },
                            onClearEndNode = { mainViewModel.clearEndNode() },
                            mainViewModel = mainViewModel
                        )
                    } else {
                        TrainDestinationCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            uiState = uiState,
                            onQueryChange = { mainViewModel.onTrainDestinationQueryChanged(it) },
                            onShowTrains = { mainViewModel.openTrainSheet() },
                            onFindPath = {
                                mainViewModel.findPath()
                                scope.launch { showInstructions = true }
                            },
                            onSwitchToAdvancedMode = { mainViewModel.toggleAdvancedMode() },
                            onClearStartNode = { mainViewModel.clearStartNode() },
                            onOpenLiveBoard = { mainViewModel.openLiveBoard() },
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    FacilityQuickChips(
                        onChipSelected = { keyword ->
                            // FIX: Intercept the click and check if we have a starting point
                            if (uiState.startNode == null) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Please set your location by tapping the map or the GPS icon first.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                mainViewModel.routeToNearestFacility(keyword)
                            }
                        }
                    )

                    if (!uiState.isAdvancedMode && uiState.destinationSuggestions.isNotEmpty()) {
                        StationSuggestionList(
                            suggestions = uiState.destinationSuggestions,
                            onSelect = { mainViewModel.onDestinationSelected(it) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (uiState.isAdvancedMode && uiState.searchResults.isNotEmpty()) {
                        SearchResultsList(
                            results = uiState.searchResults,
                            userLocation = uiState.userGpsLocation,
                            onSelect = { mainViewModel.onSearchResultSelected(it) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // ---------------- Map Controls ----------------
            MapControls(
                isDarkTheme = isDarkTheme,
                onToggleTheme = { isDarkTheme = !isDarkTheme },
                onMyLocationClick = {
                    if (locationHandler.hasLocationPermission()) {
                        // NEW LOGIC: Ask Google to check settings
                        locationHandler.checkLocationSettingsAndStart(
                            onSuccess = {
                                // GPS is already on
                                mainViewModel.onGpsButtonClicked()
                                locationHandler.startLocationUpdates()
                            },
                            onResolutionRequired = { resolvableException ->
                                // GPS is off. Launch the beautiful Google bottom sheet dialog!
                                try {
                                    val intentSenderRequest =
                                        IntentSenderRequest.Builder(resolvableException.resolution)
                                            .build()
                                    settingResultRequest.launch(intentSenderRequest)
                                } catch (_: Exception) {
                                    // Fallback just in case Google Play Services crashes
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                }
                            }
                        )
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                onSwapNodes = { mainViewModel.swapNodes() },
                onFacilities = { mainViewModel.showFacilities() },
                showSwap = uiState.isAdvancedMode,
                isAccessiblePreferred = uiState.isAccessibleRoutePreferred,
                onToggleAccessibility = { mainViewModel.toggleAccessibilityMode() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )

            // ── Mini Live Station Board (Peek State) ────────────────────────
            AnimatedVisibility(
                // Only show if mini board is enabled, we aren't currently navigating, and the full sheet isn't open
                visible = uiState.showMiniLiveBoard && uiState.calculatedPath == null && !uiState.showLiveBoardSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MiniLiveBoardCard(
                    onExpand = { mainViewModel.openLiveBoard() },
                    onClose = { mainViewModel.closeMiniLiveBoard() },
                    onTrainSelected = { train ->
                        mainViewModel.onTrainSelected(train)
                        mainViewModel.closeMiniLiveBoard()
                    }
                )
            }

            // ── Dynamic Turn-By-Turn Banner ────────────────────────────────
            AnimatedVisibility(
                visible = uiState.calculatedPath != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                val currentText =
                    uiState.instructions.getOrNull(uiState.currentInstructionIndex)?.text
                        ?: "Loading steps..."

                // NEW: Scan the route for inaccessible steps!
                val hasInaccessibleSteps = uiState.instructions.any {
                    it.text.contains("STAIR", ignoreCase = true) ||
                            it.text.contains("ESCALATOR", ignoreCase = true)
                }
                val showWarning = uiState.isAccessibleRoutePreferred && hasInaccessibleSteps

                TurnByTurnCard(
                    currentInstruction = currentText,
                    currentIndex = uiState.currentInstructionIndex,
                    totalInstructions = uiState.instructions.size,
                    totalDistanceMeters = uiState.totalRouteDistanceMeters,
                    etaMinutes = uiState.etaMinutes,
                    showAccessibilityWarning = showWarning, // <-- PASSED DOWN
                    onNext = { mainViewModel.nextInstruction() },
                    onPrev = { mainViewModel.prevInstruction() },
                    onViewList = { showInstructions = true },
                    onExit = { mainViewModel.clearRoute() }
                )
            }

            // ── Loading ────────────────────────────────────────────────────
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            // ── Node selection dialog ──────────────────────────────────────
            if (uiState.showNodeSelectionDialog) {
                NodeSelectionDialog(
                    nearestNodes = uiState.nearestNodeCandidates,
                    userLocation = uiState.userGpsLocation,
                    onNodeSelected = { mainViewModel.confirmStartNode(it) },
                    onDismiss = { mainViewModel.dismissNodeSelectionDialog() }
                )
            }

            // ── Location permission dialog ─────────────────────────────────
            if (showPermissionDialog) {
                LocationPermissionDialog(
                    onGrant = {
                        showPermissionDialog = false
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    onDismiss = { showPermissionDialog = false }
                )
            }

            // ── Turn-by-turn instructions sheet ───────────────────────────
            if (showInstructions && uiState.instructions.isNotEmpty()) {
                ModalBottomSheet(
                    onDismissRequest = { showInstructions = false },
                    sheetState = instructionsSheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    InstructionsSheet(
                        instructions = uiState.instructions,
                        startNode = uiState.startNode,
                        endNode = uiState.endNode,
                        onClose = { showInstructions = false }
                    )
                }
            }

            // ── Train list sheet ───────────────────────────────────────────
            if (uiState.showTrainSheet) {
                ModalBottomSheet(
                    onDismissRequest = { mainViewModel.dismissTrainSheet() },
                    sheetState = trainSheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    TrainListSheet(
                        destination = uiState.selectedDestination ?: "",
                        trains = uiState.availableTrains,
                        onTrainSelected = { mainViewModel.onTrainSelected(it) },
                        onClose = { mainViewModel.dismissTrainSheet() }
                    )
                }
            }

            // ── Facilities sheet ───────────────────────────────────────────
            if (uiState.showFacilitiesSheet) {
                ModalBottomSheet(
                    onDismissRequest = { mainViewModel.dismissFacilities() },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    FacilitiesSheet(
                        facilities = TrainRepository.facilities,
                        onFacilitySelect = { mainViewModel.navigateToFacility(it) },
                        onDismiss = { mainViewModel.dismissFacilities() }
                    )
                }
            }

            // ── Live Station Dashboard Sheet ───────────────────────────────
            if (uiState.showLiveBoardSheet) {
                val liveBoardSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { mainViewModel.closeLiveBoard() },
                    sheetState = liveBoardSheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    LiveTrainDashboardSheet(
                        currentDirection = uiState.liveBoardDirection,
                        onDirectionChanged = { mainViewModel.setLiveBoardDirection(it) },
                        onClose = { mainViewModel.closeLiveBoard() },
                        onTrainSelected = { train ->
                            mainViewModel.onTrainSelected(train)
                            mainViewModel.closeLiveBoard()
                            mainViewModel.closeMiniLiveBoard()
                        },
                        // FIX: Back to the clean, adaptive height restriction
                        modifier = Modifier.fillMaxHeight(0.85f)
                    )
                }
            }

        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Facility Quick Chips
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityQuickChips(onChipSelected: (String) -> Unit) {
    val facilities = listOf(
        Pair("Ticket", Icons.Default.ConfirmationNumber),
        Pair("Exit", Icons.AutoMirrored.Filled.ExitToApp),
        Pair("Toilet", Icons.Default.Wc),
        Pair("Stairs", Icons.Default.Stairs)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 72.dp, top = 0.dp, bottom = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(facilities) { (name, icon) ->
            ElevatedFilterChip(
                selected = false,
                onClick = { onChipSelected(name) },
                label = { Text(name, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        icon,
                        contentDescription = name,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.primary,
                    iconColor = MaterialTheme.colorScheme.primary
                ),
                elevation = FilterChipDefaults.filterChipElevation(elevation = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Train destination card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TrainDestinationCard(
    uiState: MainUiState,
    onQueryChange: (String) -> Unit,
    onShowTrains: () -> Unit,
    onFindPath: () -> Unit,
    onSwitchToAdvancedMode: () -> Unit,
    onClearStartNode: () -> Unit,
    onOpenLiveBoard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🚆  Where are you going?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSwitchToAdvancedMode) {
                    Text(
                        "Advanced",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            OutlinedTextField(
                value = uiState.trainDestinationQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Station  (e.g. Kalyan, Kurla, CSMT…)", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Train, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (uiState.trainDestinationQuery.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Clear,
                                "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    // FIX: Replaced hardcoded grey with adaptive surfaceVariant
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        uiState.startNode?.properties?.node_name ?: "Tap the map to set start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.startNode == null) MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        ) else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (uiState.startNode != null) {
                    IconButton(onClick = onClearStartNode, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Clear,
                            "Clear Start Node",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (uiState.selectedTrain != null) {
                val train = uiState.selectedTrain
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowTrains() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${train.departureTimeString}  →  ${train.destination}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${train.type.displayName}  •  Platform ${train.platformAtThane}  •  ${train.trainNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Icon(Icons.Default.Edit, "Change train", modifier = Modifier.size(16.dp))
                    }
                }
            } else if (uiState.selectedDestination != null && uiState.availableTrains.isNotEmpty()) {
                OutlinedButton(
                    onClick = onShowTrains,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Schedule, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose a train to ${uiState.selectedDestination}")
                }
            }

            OutlinedButton(
                onClick = onOpenLiveBoard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.DynamicFeed, null)
                Spacer(Modifier.width(8.dp))
                Text("View Live Station Board", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onFindPath,
                enabled = uiState.startNode != null && uiState.endNode != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Navigation, null)
                Spacer(Modifier.width(8.dp))
                Text("Navigate to Platform", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Station autocomplete list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StationSuggestionList(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            contentColor = MaterialTheme.colorScheme.onSurface // <-- FIX: Force adaptive text color!
        )
    ) {
        Column {
            suggestions.forEach { station ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(station) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Train,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    // FIX: Explicitly color the text to override the alpha bug
                    Text(
                        text = station,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (station != suggestions.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Train list bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainListSheet(
    destination: String,
    trains: List<TrainSchedule>,
    onTrainSelected: (TrainSchedule) -> Unit,
    onClose: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(
                "Upcoming Trains to $destination",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
                // FIX: Removed color = Color.Black
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(trains) { train ->
                    val typeColor = Color(train.type.color) // Calculate color here
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onTrainSelected(train) },
                        shape = RoundedCornerShape(12.dp),
                        // FIX: Tint the main list cards too!
                        colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        train.departureTimeString,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = typeColor.copy(alpha = 0.2f), // Boosted to 20%
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            train.type.displayName,
                                            color = typeColor,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            ),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                // FIX: Removed Color.Black
                                Text(
                                    train.destination,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (train.via.isNotEmpty()) {
                                    // FIX: Replaced Color.DarkGray with onSurfaceVariant
                                    Text(
                                        "Via: ${train.via.joinToString()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                // FIX: Removed Color.Black
                                Text(
                                    "Platform ${train.platformAtThane}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    train.direction.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrainCard(
    train: TrainSchedule,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typeColor = Color(train.type.color)
    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isSelected) BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        train.departureTimeString,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = typeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            train.type.displayName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    train.direction.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (train.via.isNotEmpty()) {
                    val viaText = train.via.joinToString(" → ")
                    Text(
                        "via $viaText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontStyle = FontStyle.Italic
                    )
                }
                Text(
                    "Train ${train.trainNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "PF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${train.platformAtThane}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Facilities bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FacilitiesSheet(
    facilities: List<FacilityItem>,
    onFacilitySelect: (FacilityItem) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Station Facilities",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close")
            }
        }
        Text(
            "Tap a facility to navigate there",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))

        facilities.forEach { facility ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFacilitySelect(facility) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(facility.emoji, style = MaterialTheme.typography.headlineMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(facility.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        facility.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (facility != facilities.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Advanced mode card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AdvancedSearchCard(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onStartNodeSelected: (NodeFeature) -> Unit,
    onEndNodeSelected: (NodeFeature) -> Unit,
    onFindPath: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSwitchToSimpleMode: () -> Unit,
    onClearStartNode: () -> Unit,
    onClearEndNode: () -> Unit,
    mainViewModel: MainViewModel // Pass the viewModel to trigger the focus state
) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manual node selection", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onSwitchToSimpleMode) {
                    Text("← Back", style = MaterialTheme.typography.labelSmall)
                }
            }

            ModernNodeSelector(
                label = "Start Point",
                icon = Icons.Default.LocationOn,
                nodes = uiState.allNodeFeatures,
                selectedNode = uiState.startNode,
                isActive = uiState.activeSelectionField == SelectionField.START, // NEW
                onActiveClick = { mainViewModel.setActiveSelectionField(SelectionField.START) }, // NEW
                onNodeSelected = onStartNodeSelected,
                onClearSelection = onClearStartNode,
                iconTint = MaterialTheme.colorScheme.primary
            )
            ModernNodeSelector(
                label = "Destination",
                icon = Icons.Default.Flag,
                nodes = uiState.allNodeFeatures,
                selectedNode = uiState.endNode,
                isActive = uiState.activeSelectionField == SelectionField.END, // NEW
                onActiveClick = { mainViewModel.setActiveSelectionField(SelectionField.END) }, // NEW
                onNodeSelected = onEndNodeSelected,
                onClearSelection = onClearEndNode,
                iconTint = MaterialTheme.colorScheme.error
            )

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search node…", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = onFindPath,
                enabled = uiState.startNode != null && uiState.endNode != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Navigation, null)
                Spacer(Modifier.width(8.dp))
                Text("Find Route", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Map controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MapControls(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onMyLocationClick: () -> Unit,
    onSwapNodes: () -> Unit,
    onFacilities: () -> Unit,
    showSwap: Boolean,
    isAccessiblePreferred: Boolean,
    onToggleAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapControlButton(
            icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
            onClick = onToggleTheme
        )

        FloatingActionButton(
            onClick = onToggleAccessibility,
            modifier = Modifier.size(48.dp),
            containerColor = if (isAccessiblePreferred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Accessible,
                contentDescription = "Accessible Route Only",
                tint = if (isAccessiblePreferred) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }

        MapControlButton(
            icon = Icons.Default.MedicalServices,
            onClick = onFacilities
        )
        if (showSwap) {
            MapControlButton(
                icon = Icons.Default.SwapVert,
                onClick = onSwapNodes
            )
        }
        MapControlButton(
            icon = Icons.Default.MyLocation,
            onClick = onMyLocationClick
        )
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
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Existing composables kept intact below
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchResultsList(
    results: List<NodeFeature>,
    userLocation: GeoPoint?,
    onSelect: (NodeFeature) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
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
        val pt = GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0])
        it.distanceToAsDouble(pt).toInt()
    }
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
                Text(node.properties.node_name ?: "Unknown", fontWeight = FontWeight.SemiBold)
                node.properties.node_type?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                node.properties.note?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
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

@Composable
fun LocationPermissionDialog(onGrant: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOff, null) },
        title = { Text("Location Permission Required") },
        text = { Text("Grant location permission to show your position and navigate.") },
        confirmButton = { TextButton(onClick = onGrant) { Text("Grant") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNodeSelector(
    label: String,
    icon: ImageVector,
    nodes: List<NodeFeature>,
    selectedNode: NodeFeature?,
    isActive: Boolean,         // NEW: Tells the UI if this is the focused field
    onActiveClick: () -> Unit, // NEW: Tells ViewModel this field was clicked
    onNodeSelected: (NodeFeature) -> Unit,
    onClearSelection: () -> Unit,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
            if (expanded) onActiveClick() // Set focus when user taps the box
        }
    ) {
        OutlinedTextField(
            value = selectedNode?.properties?.node_name ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(label, style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (selectedNode != null) {
                    IconButton(onClick = onClearSelection, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Clear,
                            "Clear Selection",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
            modifier = modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            // Visually highlight the box if it is the currently active map-tap target
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                unfocusedContainerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // FIX: Prevent the dropdown from taking over the entire screen!
            modifier = Modifier.heightIn(max = 250.dp)
        ) {
            nodes.forEach { node ->
                DropdownMenuItem(
                    text = { Text(node.properties.node_name ?: "Unnamed") },
                    onClick = {
                        onActiveClick() // Ensure focus is set
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
                Text("We found these nearby locations:", Modifier.padding(bottom = 12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(nearestNodes) { node ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNodeSelected(node) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        node.properties.node_name ?: "Unknown",
                                        fontWeight = FontWeight.Bold
                                    )
                                    node.properties.node_type?.let {
                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    userLocation?.let {
                                        val pt = GeoPoint(
                                            node.geometry.coordinates[1],
                                            node.geometry.coordinates[0]
                                        )
                                        Text(
                                            "~${it.distanceToAsDouble(pt).toInt()}m",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsSheet(
    instructions: List<NavigationInstruction>,
    startNode: NodeFeature?,
    endNode: NodeFeature?,
    onClose: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
            // FIX: Removed .background(Color(0xFFF5F5F5)) which broke Dark Mode
        ) {
            Text(
                "Steps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
                // FIX: Removed color = Color.Black
            )

            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)) {
                itemsIndexed(instructions) { index, instruction ->
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == instructions.lastIndex)
                                Icon(
                                    Icons.Default.Place,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            else
                                Text(
                                    "${index + 1}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            instruction.text,
                            Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index < instructions.lastIndex) {
                        HorizontalDivider()
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun TurnByTurnCard(
    currentInstruction: String,
    currentIndex: Int,
    totalInstructions: Int,
    totalDistanceMeters: Double,
    etaMinutes: Int,
    showAccessibilityWarning: Boolean = false, // <-- NEW PARAMETER
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onViewList: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ==========================================
            // NEW: ACCESSIBILITY WARNING BANNER
            // ==========================================
            if (showAccessibilityWarning) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "No fully accessible route available. This path requires stairs or escalators.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // ==========================================

            // ── Top Row: Step-by-Step Instructions ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrev, enabled = currentIndex > 0) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Step",
                        tint = if (currentIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    )
                }

                Text(
                    text = currentInstruction.ifEmpty { "Arrived at destination" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                IconButton(onClick = onNext, enabled = currentIndex < totalInstructions - 1) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Step",
                        tint = if (currentIndex < totalInstructions - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
            Spacer(modifier = Modifier.height(4.dp))

            // ── Bottom Row: Quick Actions & Stats ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onExit) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Exit",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                val distanceText = if (totalDistanceMeters > 1000) {
                    String.format(java.util.Locale.US, "%.1f km", totalDistanceMeters / 1000.0)
                } else {
                    "${totalDistanceMeters.toInt()} m"
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$etaMinutes min  •  $distanceText",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                TextButton(onClick = onViewList) {
                    Icon(
                        Icons.Default.FormatListNumbered,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Steps (${currentIndex + 1}/$totalInstructions)",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationsSheet(
    destinations: List<String>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = destinations.filter { it.contains(searchQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(
                "Where are you going?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search station...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    // FIX: Replaced hardcoded grey with adaptive surfaceVariant
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            LazyColumn {
                items(filtered) { dest ->
                    ListItem(
                        headlineContent = { Text(dest, fontWeight = FontWeight.Medium) },
                        leadingContent = {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        CircleShape
                                    ), contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Train,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onSelect(dest) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Live Train Dashboard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiveTrainDashboardSheet(
    currentDirection: TrainDirection,
    onDirectionChanged: (TrainDirection) -> Unit,
    onClose: () -> Unit,
    onTrainSelected: (TrainSchedule) -> Unit, // <-- NEW PARAMETER
    modifier: Modifier = Modifier
) {
    var currentMinutes by remember { mutableIntStateOf(TrainRepository.currentMinutes()) }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = java.util.Calendar.getInstance()
            currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            kotlinx.coroutines.delay(15000)
        }
    }

    val activeTrains = remember(currentDirection, currentMinutes) {
        TrainRepository.schedule
            .filter { it.direction == currentDirection }
            .filter { train ->
                var diff = train.departureMinutes - currentMinutes
                if (diff < -720) diff += 1440
                diff > -15
            }
            .sortedBy { train ->
                var diff = train.departureMinutes - currentMinutes
                if (diff < -720) diff += 1440
                diff
            }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        LiveClockHeader(onClose = onClose)
        Spacer(Modifier.height(16.dp))

        // Direction Switcher
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                TrainDirection.entries.forEach { dir ->
                    val isSelected = dir == currentDirection
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onDirectionChanged(dir) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (dir == TrainDirection.UP) "UP (CSMT)" else "DOWN (Kalyan)",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeTrains, key = { it.trainNumber }) { train ->
                // FIX: Passed the selection callback down
                LiveTrainCard(train = train, currentMinutes = currentMinutes, onClick = { onTrainSelected(train) })
            }
        }
    }
}

@Composable
fun LiveTrainCard(train: TrainSchedule, currentMinutes: Int, onClick: () -> Unit) {
    var diff = train.departureMinutes - currentMinutes
    if (diff < -720) diff += 1440

    val (statusText, statusColor) = when {
        diff < 0 -> Pair("Departed", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        diff == 0 -> Pair("Now", MaterialTheme.colorScheme.error)
        diff <= 5 -> Pair("in $diff min", MaterialTheme.colorScheme.error)
        else -> Pair("in $diff min", MaterialTheme.colorScheme.primary)
    }

    val typeColor = Color(train.type.color)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        // FIX: Tint the entire card background to match the train type!
        colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(68.dp)) {
                Text(
                    text = train.departureTimeString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = typeColor.copy(alpha = 0.2f), // Boosted to 20%
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = train.type.displayName,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = train.destination.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (train.via.isNotEmpty()) {
                    Text(
                        text = "via ${train.via.last()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "PF ${train.platformAtThane}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun LiveClockHeader(onClose: () -> Unit) {
    var currentTimeFormatted by remember { mutableStateOf("") }

    // This loop ticks every second, but NOW it ONLY recomposes this tiny header!
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            val hh = String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY))
            val mm = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))
            val ss = String.format(Locale.US, "%02d", cal.get(Calendar.SECOND))
            currentTimeFormatted = "$hh:$mm:$ss"
            kotlinx.coroutines.delay(1000)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "THANE DEPARTURES",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Live • $currentTimeFormatted",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, "Close Board")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Mini Persistent Live Board (Peek State)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MiniLiveBoardCard(
    onExpand: () -> Unit,
    onClose: () -> Unit,
    onTrainSelected: (TrainSchedule) -> Unit, // <-- NEW PARAMETER
    modifier: Modifier = Modifier
) {
    var currentMinutes by remember { mutableIntStateOf(TrainRepository.currentMinutes()) }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = java.util.Calendar.getInstance()
            currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            kotlinx.coroutines.delay(15000)
        }
    }

    fun getNextTrain(dir: TrainDirection): TrainSchedule? {
        return TrainRepository.schedule
            .filter { it.direction == dir }
            .map { train ->
                var diff = train.departureMinutes - currentMinutes
                if (diff < -720) diff += 1440
                train to diff
            }
            .filter { it.second >= -1 }
            .minByOrNull { it.second }
            ?.first
    }

    val upTrain = remember(currentMinutes) { getNextTrain(TrainDirection.UP) }
    val downTrain = remember(currentMinutes) { getNextTrain(TrainDirection.DOWN) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            // FIX: Removed the ugly 'androidx...' prefixes since we imported them properly!
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -10) onExpand()
                    else if (dragAmount > 15) onClose()
                }
            },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onExpand() }, // Expand when tapping the header
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DragHandle, contentDescription = "Pull up", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("Live Next Departures", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Board")
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // FIX: Passed selection callback down
                MiniTrainBox(TrainDirection.UP, upTrain, currentMinutes, onTrainSelected, Modifier.weight(1f))
                MiniTrainBox(TrainDirection.DOWN, downTrain, currentMinutes, onTrainSelected, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun MiniTrainBox(direction: TrainDirection, train: TrainSchedule?, currentMinutes: Int, onClick: (TrainSchedule) -> Unit, modifier: Modifier = Modifier) {
    // FIX: Dynamically tint the entire background if a train exists!
    val bgColor = if (train != null) {
        Color(train.type.color).copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable(enabled = train != null) { train?.let { onClick(it) } }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = if (direction == TrainDirection.UP) "UP (CSMT)" else "DOWN (KALYAN)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(6.dp))

            if (train != null) {
                var diff = train.departureMinutes - currentMinutes
                if (diff < -720) diff += 1440

                val statusText = if (diff <= 0) "Now" else "in $diff min"
                val statusColor = if (diff <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                val typeColor = Color(train.type.color)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(train.departureTimeString, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = typeColor.copy(alpha = 0.2f), // Boosted to 20% so it stands out against the 8% background
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = train.type.displayName,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Text("PF ${train.platformAtThane}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text("No upcoming trains", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
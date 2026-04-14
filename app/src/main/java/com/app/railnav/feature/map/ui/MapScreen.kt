package com.app.railnav.feature.map.ui

import com.app.railnav.*
import com.app.railnav.feature.liveboard.ui.*
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

import dagger.hilt.android.AndroidEntryPoint

// ─────────────────────────────────────────────────────────────────────────────
//  Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfindingScreen(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel(),
    liveBoardViewModel: com.app.railnav.feature.liveboard.viewmodel.LiveBoardViewModel = viewModel(),
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val liveBoardUiState by liveBoardViewModel.uiState.collectAsState()
    val instructionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val trainSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    var showInstructions by remember { mutableStateOf(false) }
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
        } else {
            // The user clicked "No Thanks" — guide them to use the map instead
            android.widget.Toast.makeText(
                context,
                "No worries! Tap anywhere on the map to set your starting point.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    DisposableEffect(Unit) { onDispose { locationHandler.stopLocationUpdates() } }

    // Removed: No longer auto-show the full step list sheet.
    // The compact TurnByTurnCard at the bottom is enough.

    // BackHandler priority: clear route first, then clear selected train
    BackHandler(enabled = uiState.calculatedPath != null) {
        mainViewModel.clearRoute()
        showInstructions = false
    }

    BackHandler(enabled = uiState.calculatedPath == null && uiState.selectedTrain != null) {
        mainViewModel.clearSelectedTrain()
    }

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
                            },
                            onSearchQueryChanged = { mainViewModel.onSearchQueryChanged(it) },
                            onSwitchToSimpleMode = { mainViewModel.toggleAdvancedMode() },
                            // FIX: Passing the viewmodel calls as lambdas to resolve Unresolved References!
                            onClearStartNode = { mainViewModel.clearStartNode() },
                            onClearEndNode = { mainViewModel.clearEndNode() },
                            mainViewModel = mainViewModel,
                            onMenuClick = onOpenDrawer
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
                            },
                            onSwitchToAdvancedMode = { mainViewModel.toggleAdvancedMode() },
                            onClearStartNode = { mainViewModel.clearStartNode() },
                            onOpenLiveBoard = { liveBoardViewModel.openLiveBoard() },
                            onClearTrain = { mainViewModel.clearSelectedTrain() },
                            onMenuClick = onOpenDrawer
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

                    // ── Map Legend ──────────────────────────────────────────
                    MapLegend()

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
                onToggleTheme = onToggleTheme,
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
                visible = liveBoardUiState.showMiniLiveBoard && uiState.calculatedPath == null && !liveBoardUiState.showLiveBoardSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MiniLiveBoardCard(
                    onExpand = { liveBoardViewModel.openLiveBoard() },
                    onClose = { liveBoardViewModel.closeMiniLiveBoard() },
                    onTrainSelected = { train ->
                        mainViewModel.onTrainSelected(train)
                        liveBoardViewModel.closeMiniLiveBoard()
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
                    onDismiss = {
                        mainViewModel.dismissNodeSelectionDialog()
                        android.widget.Toast.makeText(
                            context,
                            "You can also tap directly on the map to set your start point.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // ── Location permission dialog ─────────────────────────────────
            if (showPermissionDialog) {
                LocationPermissionDialog(
                    onGrant = {
                        showPermissionDialog = false
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    onDismiss = {
                        showPermissionDialog = false
                        android.widget.Toast.makeText(
                            context,
                            "You can tap the map to set your location manually.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
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
            if (liveBoardUiState.showLiveBoardSheet) {
                val liveBoardSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { liveBoardViewModel.closeLiveBoard() },
                    sheetState = liveBoardSheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    LiveTrainDashboardSheet(
                        currentDirection = liveBoardUiState.liveBoardDirection,
                        onDirectionChanged = { liveBoardViewModel.setLiveBoardDirection(it) },
                        onClose = { liveBoardViewModel.closeLiveBoard() },
                        onTrainSelected = { train ->
                            mainViewModel.onTrainSelected(train)
                            liveBoardViewModel.closeLiveBoard()
                            liveBoardViewModel.closeMiniLiveBoard()
                        },
                        // FIX: Back to the clean, adaptive height restriction
                        modifier = Modifier.fillMaxHeight(0.85f)
                    )
                }
            }

        }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Facility Quick Chips
// ─────────────────────────────────────────────────────────────────────────────

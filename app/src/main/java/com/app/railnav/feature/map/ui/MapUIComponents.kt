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
//  Map Legend (collapsible)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = true,
        modifier = modifier.padding(start = 16.dp, end = 72.dp, bottom = 4.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.clickable { expanded = !expanded }
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Map Legend",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LegendItem(color = Color(0xFFEED202), label = "Walkway / Path")
                        LegendItem(color = Color(0xFFFF0800), label = "Stairs")
                        LegendItem(color = Color(0xFF9C27B0), label = "Escalator")
                        LegendItem(color = Color(0xFF1976D2), label = "Active Route")
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
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
    onClearTrain: () -> Unit,
    onMenuClick: () -> Unit = {},
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Where are you going?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                        Row {
                            IconButton(
                                onClick = onClearTrain,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "Clear train",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(
                                onClick = { onShowTrains() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    "Change train",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
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
fun LocationPermissionDialog(onGrant: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOff, null) },
        title = { Text("Location Permission") },
        text = { Text("Grant location to auto-detect your position, or tap the map manually to set your start point.") },
        confirmButton = { TextButton(onClick = onGrant) { Text("Grant Permission") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Use Map Instead") } }
    )
}

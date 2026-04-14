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
    mainViewModel: MainViewModel, // Pass the viewModel to trigger the focus state
    onMenuClick: () -> Unit = {}
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
                    Text("Manual node selection", style = MaterialTheme.typography.titleSmall)
                }
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tap Map Instead") } }
    )
}

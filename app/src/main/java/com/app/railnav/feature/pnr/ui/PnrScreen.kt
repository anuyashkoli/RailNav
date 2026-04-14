package com.app.railnav.feature.pnr.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.railnav.feature.pnr.viewmodel.PnrViewModel
import kotlinx.serialization.json.Json
import com.app.railnav.core.data.remote.models.PnrResponse
import com.app.railnav.core.data.remote.models.PnrData
import com.app.railnav.core.data.remote.models.PnrPassenger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PnrScreen(
    modifier: Modifier = Modifier,
    viewModel: PnrViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Assuming the ViewModel mapped the JSON string successfully for this demo.
    // In production, the ViewModel holds the raw object.
    val pnrData = uiState.result?.data

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PNR Status", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Map")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // -- Search Bar Header --
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.shadow(4.dp)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Enter 10-digit PNR") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.fetchPnrStatus() },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Search, null, Modifier.size(28.dp))
                    }
                }
            }

            // -- Content Feed --
            Box(Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else if (uiState.error != null) {
                    Text(
                        uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                } else if (pnrData != null) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { PnrHeroCard(pnrData) }
                        item {
                            Text(
                                "Passenger Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(pnrData.passengers) { passenger ->
                            PassengerCard(passenger)
                        }
                    }
                } else {
                    Text(
                        "Enter your PNR to see your coach and berth details map directly into the navigation system.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PnrHeroCard(data: PnrData) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.background(gradient).padding(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        data.trainNumber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Text(
                    data.journeyDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                data.trainName.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(Modifier.height(24.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(data.departureTime, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(data.source, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(data.duration, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    Icon(Icons.Default.Train, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text(data.travelClass, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(data.arrivalTime, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(data.destination, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
            
            Surface(
                color = if (data.chartPrepared) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (data.chartPrepared) "Chart Prepared ✓" else "Chart Not Prepared",
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PassengerCard(passenger: PnrPassenger) {
    val isConfirmed = passenger.currentStatus.contains("CNF", ignoreCase = true)
    val statusColor = if (isConfirmed) Color(0xFF4CAF50) else Color(0xFFFF9800)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Passenger ${passenger.number}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        passenger.bookingStatus,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        " → ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        passenger.currentStatus,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            if (passenger.coach != "N/A" && passenger.berth != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            passenger.coach,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(Modifier.width(32.dp).padding(vertical = 4.dp))
                        Text(
                            passenger.berth.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

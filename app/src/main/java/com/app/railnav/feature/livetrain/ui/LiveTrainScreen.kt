package com.app.railnav.feature.livetrain.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.LocationOn
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
import com.app.railnav.feature.livetrain.viewmodel.LiveTrainViewModel
import com.app.railnav.core.data.remote.models.LiveTrainData
import com.app.railnav.core.data.remote.models.CurrentLocationInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrainScreen(
    modifier: Modifier = Modifier,
    viewModel: LiveTrainViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Train Tracker", fontWeight = FontWeight.ExtraBold) },
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
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.shadow(4.dp)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.trainNumberQuery,
                        onValueChange = { viewModel.onTrainNumberChanged(it) },
                        placeholder = { Text("Enter 5-digit Train (e.g. 12321)") },
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
                        onClick = { viewModel.fetchLiveStatus() },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Search, null, Modifier.size(28.dp))
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else if (uiState.error != null) {
                    Text(
                        uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                } else if (uiState.result?.data != null) {
                    val data = uiState.result!!.data!!
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { LiveHeroCard(data) }
                        
                        item {
                            Text(
                                "Live Progress Feed",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        itemsIndexed(data.currentLocationInfo) { index, info ->
                            LocationFeedItem(info, isLast = index == data.currentLocationInfo.lastIndex)
                        }
                    }
                } else {
                    Text(
                        "Enter your Train Number to see its exact live location and upcoming stations.",
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
fun LiveHeroCard(data: LiveTrainData) {
    val isDelayed = data.delay > 0
    val gradient = Brush.linearGradient(
        colors = if (isDelayed) {
            listOf(Color(0xFFE65100), Color(0xFFFF9800)) // Warning orange
        } else {
            listOf(Color(0xFF1B5E20), Color(0xFF4CAF50)) // On time green
        }
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
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isDelayed) Icons.Default.LocationOn else Icons.Default.Train, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isDelayed) "DELAYED ${data.delay}m" else "ON TIME",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                data.trainName.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(Modifier.height(24.dp))
            
            // Progress Bar
            val progress = if (data.totalDistance > 0) (data.distanceFromSource.toFloat() / data.totalDistance.toFloat()) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${data.distanceFromSource} km", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text("${data.totalDistance} km", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(Modifier.height(16.dp))

            data.nextStoppageInfo?.let { nextStop ->
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(nextStop.nextStoppageTitle, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                            Text(nextStop.nextStoppage, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                        Text(nextStop.nextStoppageTimeDiff, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationFeedItem(info: CurrentLocationInfo, isLast: Boolean) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Tracker Line
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            Box(
                Modifier.size(16.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape))
            }
            if (!isLast) {
                Box(
                    Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        
        Spacer(Modifier.width(8.dp))
        
        // Content
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                info.label.uppercase(), 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                info.readableMessage, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                info.hint, 
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

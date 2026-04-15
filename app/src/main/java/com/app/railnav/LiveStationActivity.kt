package com.app.railnav

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.railnav.data.remote.ApiConfig
import com.app.railnav.data.remote.LiveStationData
import com.app.railnav.data.remote.LiveTrainDto
import com.app.railnav.data.remote.RetrofitClient
import com.app.railnav.ui.theme.RailNavTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Sealed UI State ──────────────────────────────────────────────────────────

sealed class LiveStationUiState {
    object Idle : LiveStationUiState()
    object Loading : LiveStationUiState()
    data class Success(val data: LiveStationData) : LiveStationUiState()
    data class Error(val message: String) : LiveStationUiState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class LiveStationViewModel : ViewModel() {
    private val _state = MutableStateFlow<LiveStationUiState>(LiveStationUiState.Idle)
    val state: StateFlow<LiveStationUiState> = _state.asStateFlow()

    fun fetchBoard(source: String, hours: String, destination: String?, context: Context) {
        val src = source.trim().uppercase()
        if (src.isBlank()) {
            _state.value = LiveStationUiState.Error("Enter a valid station code")
            return
        }
        _state.value = LiveStationUiState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getLiveStationBoard(
                    apiKey = ApiConfig.RAPID_API_KEY,
                    source = src,
                    hours = hours,
                    destination = destination?.trim()?.uppercase()?.ifBlank { null }
                )
                if (response.success && response.data != null) {
                    _state.value = LiveStationUiState.Success(response.data)
                    saveToHistory(context, src)
                } else {
                    _state.value = LiveStationUiState.Error("No data found for station $src")
                }
            } catch (e: Exception) {
                _state.value = LiveStationUiState.Error(e.message ?: "Network error. Check connection.")
            }
        }
    }

    private fun saveToHistory(context: Context, stationCode: String) {
        val prefs = context.getSharedPreferences("station_history", Context.MODE_PRIVATE)
        val current = prefs.getStringSet("history", mutableSetOf()) ?: mutableSetOf()
        val updated = (listOf(stationCode) + current).distinct().take(5).toSet()
        prefs.edit().putStringSet("history", updated).apply()
    }
}

// ── Activity ─────────────────────────────────────────────────────────────────

class LiveStationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RailNavTheme {
                LiveStationScreen(onBack = { finish() })
            }
        }
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStationScreen(
    onBack: () -> Unit,
    viewModel: LiveStationViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var stationInput by remember { mutableStateOf("") }
    var destInput by remember { mutableStateOf("") }
    var selectedHours by remember { mutableStateOf("8") }
    var showDestField by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("station_history", Context.MODE_PRIVATE) }
    var history by remember {
        mutableStateOf(prefs.getStringSet("history", emptySet())?.toList() ?: emptyList())
    }

    LaunchedEffect(state) {
        if (state is LiveStationUiState.Success) {
            history = prefs.getStringSet("history", emptySet())?.toList() ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Station Board", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Station code input ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = stationInput,
                    onValueChange = { stationInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) },
                    placeholder = { Text("Station Code (e.g. TNA)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(20.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(
                    onClick = {
                        viewModel.fetchBoard(stationInput, selectedHours, destInput.ifBlank { null }, context)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            // Optional destination filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { showDestField = !showDestField }) {
                    Icon(
                        if (showDestField) Icons.Default.Remove else Icons.Default.Add,
                        null, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (showDestField) "Remove destination filter" else "Filter by destination",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (showDestField) {
                OutlinedTextField(
                    value = destInput,
                    onValueChange = { destInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) },
                    placeholder = { Text("Destination Code (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(4.dp))
            }

            // ── Hours filter chips ────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Window:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                listOf("2", "4", "6", "8").forEach { hrs ->
                    FilterChip(
                        selected = selectedHours == hrs,
                        onClick = { selectedHours = hrs },
                        label = { Text("${hrs}h", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // ── History chips ─────────────────────────────────────────────
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    history.forEach { code ->
                        AssistChip(
                            onClick = {
                                stationInput = code
                                viewModel.fetchBoard(code, selectedHours, destInput.ifBlank { null }, context)
                            },
                            label = { Text(code, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Content ───────────────────────────────────────────────────
            when (val s = state) {
                is LiveStationUiState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.DynamicFeed, null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Enter a station code above\nto see live arrivals & departures",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is LiveStationUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator()
                            Text(
                                "Fetching live data…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                is LiveStationUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                Icons.Default.ErrorOutline, null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                s.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                is LiveStationUiState.Success -> {
                    LiveBoardResultView(data = s.data)
                }
            }
        }
    }
}

// ── Live Board Result ─────────────────────────────────────────────────────────

@Composable
private fun LiveBoardResultView(data: LiveStationData) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Summary header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            data.source,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (data.destination != null) {
                            Text(
                                "→ ${data.destination}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            "Next ${data.hours} hours",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${data.trainCount}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "trains",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Column headers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "TRAIN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(2f)
                )
                Text(
                    "ARR / DEP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    "STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Train cards
        items(data.trains) { train ->
            LiveStationTrainCard(train = train)
        }
    }
}

@Composable
private fun LiveStationTrainCard(train: LiveTrainDto) {
    val isOnTime = train.delay == "00:00" || train.delay.isBlank()
    val statusColor = if (isOnTime) Color(0xFF2E7D32) else Color(0xFFC62828)
    val statusText = if (isOnTime) "On Time" else "Delayed ${train.delay}"
    val cardBg = if (isOnTime)
        Color(0xFF2E7D32).copy(alpha = 0.05f)
    else
        Color(0xFFC62828).copy(alpha = 0.05f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Platform badge
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(width = 40.dp, height = 48.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "PF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        train.platform.ifBlank { "—" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Train name + number
            Column(modifier = Modifier.weight(2f)) {
                Text(
                    train.trainName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "#${train.trainNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            // Arr / Dep times
            Column(
                modifier = Modifier.weight(1.5f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDownward, null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        train.scheduledArrival,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward, null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Text(
                        train.scheduledDeparture,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Status
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

package com.app.railnav

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.railnav.data.remote.ApiConfig
import com.app.railnav.data.remote.RetrofitClient
import com.app.railnav.data.remote.StationDto
import com.app.railnav.ui.theme.RailNavTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Sealed UI State ──────────────────────────────────────────────────────────

sealed class ScheduleUiState {
    object Idle : ScheduleUiState()
    object Loading : ScheduleUiState()
    data class Success(val trainNumber: String, val stations: List<StationDto>) : ScheduleUiState()
    data class Error(val message: String) : ScheduleUiState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class TrainScheduleViewModel : ViewModel() {
    private val _state = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Idle)
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    fun fetchSchedule(trainNumber: String, context: Context) {
        val number = trainNumber.trim()
        // Accept 4–6 digit train numbers
        if (!number.matches(Regex("^[0-9]{4,6}$"))) {
            _state.value = ScheduleUiState.Error("Invalid train number — enter 4 to 6 digits")
            return
        }
        _state.value = ScheduleUiState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getTrainSchedule(
                    apiKey = ApiConfig.RAPID_API_KEY,
                    trainNumber = number
                )
                if (response.success && response.data.isNotEmpty()) {
                    _state.value = ScheduleUiState.Success(number, response.data)
                    saveToHistory(context, number)
                } else {
                    _state.value = ScheduleUiState.Error("No schedule found for train #$number")
                }
            } catch (e: Exception) {
                _state.value = ScheduleUiState.Error(e.message ?: "Network error. Check connection.")
            }
        }
    }

    private fun saveToHistory(context: Context, trainNumber: String) {
        val prefs = context.getSharedPreferences("schedule_history", Context.MODE_PRIVATE)
        val current = prefs.getStringSet("history", mutableSetOf()) ?: mutableSetOf()
        val updated = (listOf(trainNumber) + current).distinct().take(5).toSet()
        prefs.edit().putStringSet("history", updated).apply()
    }
}

// ── Activity ─────────────────────────────────────────────────────────────────

class TrainScheduleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RailNavTheme {
                TrainScheduleScreen(onBack = { finish() })
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatStdMin(stdMin: Int): String {
    if (stdMin <= 0) return "- - -"
    val hours = (stdMin / 60) % 24
    val minutes = stdMin % 60
    return "%02d:%02d".format(hours, minutes)
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainScheduleScreen(
    onBack: () -> Unit,
    viewModel: TrainScheduleViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var trainInput by remember { mutableStateOf("") }

    val prefs = remember { context.getSharedPreferences("schedule_history", Context.MODE_PRIVATE) }
    var history by remember {
        mutableStateOf(prefs.getStringSet("history", emptySet())?.toList() ?: emptyList())
    }

    LaunchedEffect(state) {
        if (state is ScheduleUiState.Success) {
            history = prefs.getStringSet("history", emptySet())?.toList() ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Train Schedule",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary
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

            // ── Search Row ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = trainInput,
                    onValueChange = { if (it.length <= 6) trainInput = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("Train Number (e.g. 12650)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(
                    onClick = { viewModel.fetchSchedule(trainInput, context) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            // ── History chips ─────────────────────────────────────────────
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    history.forEach { num ->
                        AssistChip(
                            onClick = {
                                trainInput = num
                                viewModel.fetchSchedule(num, context)
                            },
                            label = { Text(num, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Content ───────────────────────────────────────────────────
            when (val s = state) {
                is ScheduleUiState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Route, null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Enter a train number above\nto view the full schedule",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is ScheduleUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is ScheduleUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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

                is ScheduleUiState.Success -> {
                    ScheduleResultView(
                        trainNumber = s.trainNumber,
                        stations = s.stations
                    )
                }
            }
        }
    }
}

// ── Schedule Result ───────────────────────────────────────────────────────────

@Composable
private fun ScheduleResultView(trainNumber: String, stations: List<StationDto>) {
    val stops = stations.filter { it.isStop }
    val origin = stations.firstOrNull()
    val destination = stations.lastOrNull()

    val lineColor = MaterialTheme.colorScheme.primary
    val passColor = MaterialTheme.colorScheme.outlineVariant

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Train summary header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Train, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Train #$trainNumber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (origin != null && destination != null) {
                            Text(
                                "${origin.stationName}  →  ${destination.stationName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${stops.size} stops",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${stations.size} stations",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Station timeline
        itemsIndexed(stations) { index, station ->
            val isFirst = index == 0
            val isLast = index == stations.lastIndex
            val isStop = station.isStop
            val dotColor = if (isStop) lineColor else passColor
            val dotSize = if (isStop) 12.dp else 8.dp

            Row(modifier = Modifier.fillMaxWidth()) {
                // ── Left column: timeline line + dot ─────────────────────
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .defaultMinSize(minHeight = if (isStop) 72.dp else 48.dp)
                ) {
                    val lineColorArg = lineColor
                    val passColorArg = passColor

                    // Vertical line (except last item)
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = if (isStop) 16.dp else 12.dp)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(if (isStop) lineColorArg else passColorArg)
                        )
                    }

                    // Dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = if (isStop) 10.dp else 8.dp)
                            .size(dotSize)
                            .background(dotColor, CircleShape)
                    )
                }

                // ── Right column: station info ────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = 4.dp,
                            bottom = if (isStop) 16.dp else 8.dp,
                            end = 8.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                station.stationName,
                                style = if (isStop) MaterialTheme.typography.bodyMedium
                                        else MaterialTheme.typography.bodySmall,
                                fontWeight = if (isStop) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isStop) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                station.stationCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isStop) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            // Time
                            Text(
                                formatStdMin(station.stdMin),
                                style = if (isStop) MaterialTheme.typography.bodyMedium
                                        else MaterialTheme.typography.bodySmall,
                                fontWeight = if (isStop) FontWeight.Bold else FontWeight.Normal,
                                color = if (isStop) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                            // Platform (only for stops)
                            if (isStop && station.platformNumber > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "PF ${station.platformNumber}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            // Day badge
                            if (station.day > 1) {
                                Text(
                                    "Day ${station.day}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Amenity icons for stops
                    if (isStop && (station.isFoodAvailable || station.isHospitalAvailable)) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (station.isFoodAvailable) {
                                Surface(
                                    color = Color(0xFF1B5E20).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "🍽 Food",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                            if (station.isHospitalAvailable) {
                                Surface(
                                    color = Color(0xFFB71C1C).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "🏥 Medical",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

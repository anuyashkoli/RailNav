package com.app.railnav

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.railnav.data.remote.ApiConfig
import com.app.railnav.data.remote.LiveTrainData
import com.app.railnav.data.remote.RetrofitClient
import com.app.railnav.ui.theme.RailNavTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Sealed UI State ──────────────────────────────────────────────────────────

sealed class LiveTrainUiState {
    object Idle : LiveTrainUiState()
    object Loading : LiveTrainUiState()
    data class Success(val data: LiveTrainData) : LiveTrainUiState()
    data class Error(val message: String) : LiveTrainUiState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class LiveTrainViewModel : ViewModel() {
    private val _state = MutableStateFlow<LiveTrainUiState>(LiveTrainUiState.Idle)
    val state: StateFlow<LiveTrainUiState> = _state.asStateFlow()

    fun fetchLiveTrain(trainNumber: String, startDay: String, context: Context) {
        val number = trainNumber.trim()
        if (!number.matches(Regex("^[0-9]{4,6}$"))) {
            _state.value = LiveTrainUiState.Error("Invalid train number — enter 4 to 6 digits")
            return
        }
        _state.value = LiveTrainUiState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getLiveTrainStatus(
                    apiKey = ApiConfig.RAPID_API_KEY,
                    trainNumber = number,
                    startDay = startDay
                )
                if (response.success && response.data != null) {
                    _state.value = LiveTrainUiState.Success(response.data)
                    saveToHistory(context, number)
                } else {
                    _state.value = LiveTrainUiState.Error("No live data found for train #$number")
                }
            } catch (e: Exception) {
                _state.value = LiveTrainUiState.Error(e.message ?: "Network error. Check connection.")
            }
        }
    }

    private fun saveToHistory(context: Context, trainNumber: String) {
        val prefs = context.getSharedPreferences("live_train_history", Context.MODE_PRIVATE)
        val current = prefs.getStringSet("history", mutableSetOf()) ?: mutableSetOf()
        val updated = (listOf(trainNumber) + current).distinct().take(5).toSet()
        prefs.edit().putStringSet("history", updated).apply()
    }
}

// ── Activity ─────────────────────────────────────────────────────────────────

class LiveTrainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RailNavTheme {
                LiveTrainScreen(onBack = { finish() })
            }
        }
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrainScreen(
    onBack: () -> Unit,
    viewModel: LiveTrainViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var trainInput by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf("1") } // default: Yesterday

    val prefs = remember { context.getSharedPreferences("live_train_history", Context.MODE_PRIVATE) }
    var history by remember {
        mutableStateOf(prefs.getStringSet("history", emptySet())?.toList() ?: emptyList())
    }

    LaunchedEffect(state) {
        if (state is LiveTrainUiState.Success) {
            history = prefs.getStringSet("history", emptySet())?.toList() ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Live Train Status",
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
                    placeholder = { Text("Train Number (e.g. 12321)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(
                    onClick = { viewModel.fetchLiveTrain(trainInput, selectedDay, context) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Day selector ──────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Journey date:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                listOf("0" to "Today", "1" to "Yesterday", "2" to "Day Before").forEach { (value, label) ->
                    FilterChip(
                        selected = selectedDay == value,
                        onClick = { selectedDay = value },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
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
                    history.forEach { num ->
                        AssistChip(
                            onClick = {
                                trainInput = num
                                viewModel.fetchLiveTrain(num, selectedDay, context)
                            },
                            label = { Text(num, style = MaterialTheme.typography.labelSmall) },
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
                is LiveTrainUiState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Train, null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                            Text(
                                "Enter a train number above\nto track it live on the map",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is LiveTrainUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "Fetching live location…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                is LiveTrainUiState.Error -> {
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

                is LiveTrainUiState.Success -> {
                    LiveTrainResultView(data = s.data)
                }
            }
        }
    }
}

// ── Live Train Result ─────────────────────────────────────────────────────────

@Composable
private fun LiveTrainResultView(data: LiveTrainData) {
    val progress = if (data.totalDistance > 0)
        data.distanceFromSource.toFloat() / data.totalDistance.toFloat()
    else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    val isDelayed = data.delay > 0
    val delayColor = if (isDelayed) Color(0xFFC62828) else Color(0xFF2E7D32)
    val delayText = if (isDelayed) "Delayed ${data.delay} min" else "Running On Time"

    val statusDesc = when (data.status) {
        "T" -> "Train is Running"
        "N" -> "Not Yet Started"
        "A" -> "Arrived"
        "D" -> "Departed"
        else -> data.status
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Header card ────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                data.trainName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "#${data.trainNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Surface(
                            color = delayColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                delayText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = delayColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Current location
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Text(
                            "Near: ${data.currentStationName.trim('~').trim()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        statusDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                    )

                    Spacer(Modifier.height(14.dp))

                    // Progress bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${data.distanceFromSource} km",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "${data.totalDistance} km total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${(animatedProgress * 100).toInt()}% of journey complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                    Spacer(Modifier.height(12.dp))

                    // ETA / ETD row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "ETA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                data.eta,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Arrival",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f)
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(50.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "ETD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                data.etd,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Departure",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f)
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(50.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "AHEAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                "${data.upcomingStationsCount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "stations",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }

        // ── Next stoppage card ─────────────────────────────────────────────
        data.nextStoppageInfo?.let { next ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                next.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f)
                            )
                            Text(
                                next.nextStoppage,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                next.timeDiff,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        if (next.delay > 0) {
                            Surface(
                                color = Color(0xFFC62828).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "+${next.delay} min",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Location messages ──────────────────────────────────────────────
        if (data.currentLocationInfo.isNotEmpty()) {
            item {
                Text(
                    "Location Updates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(data.currentLocationInfo) { info ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                info.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            if (info.hint.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        info.hint,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            info.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (info.readableMessage.isNotBlank() && info.readableMessage != info.message) {
                            Text(
                                info.readableMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}

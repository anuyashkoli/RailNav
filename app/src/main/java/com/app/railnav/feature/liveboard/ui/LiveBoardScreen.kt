package com.app.railnav.feature.liveboard.ui

import com.app.railnav.*
import com.app.railnav.data.*
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

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
                            text = when (dir) {
                                TrainDirection.UP -> "UP"
                                TrainDirection.DOWN -> "DOWN"
                                TrainDirection.HARBOUR -> "HARBOUR"
                            },
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
            Column(modifier = Modifier.width(84.dp)) {
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
    val harbourTrain = remember(currentMinutes) { getNextTrain(TrainDirection.HARBOUR) }

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
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DragHandle, contentDescription = "Pull up", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("Next Departures", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Board")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTrainBox(TrainDirection.UP, upTrain, currentMinutes, onTrainSelected, Modifier.weight(1f))
                MiniTrainBox(TrainDirection.DOWN, downTrain, currentMinutes, onTrainSelected, Modifier.weight(1f))
                MiniTrainBox(TrainDirection.HARBOUR, harbourTrain, currentMinutes, onTrainSelected, Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun MiniTrainBox(direction: TrainDirection, train: TrainSchedule?, currentMinutes: Int, onClick: (TrainSchedule) -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (train != null) {
        Color(train.type.color).copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable(enabled = train != null) { train?.let { onClick(it) } }
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            if (train != null) {
                var diff = train.departureMinutes - currentMinutes
                if (diff < -720) diff += 1440

                val statusColor = if (diff <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                val typeColor = Color(train.type.color)

                // ── Top row: Destination + PF ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = train.destination,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "PF ${train.platformAtThane}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(2.dp))

                // ── Middle: Departure time ──
                Text(
                    train.departureTimeString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                // ── Bottom row: Type badge + Timer ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = typeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = train.type.displayName,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    if (diff <= 0) {
                        Text(
                            "Now",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = statusColor
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "${diff} min",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            } else {
                // ── Empty state ──
                Text(
                    text = when (direction) {
                        TrainDirection.UP -> "UP"
                        TrainDirection.DOWN -> "DOWN"
                        TrainDirection.HARBOUR -> "HARBOUR"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "No trains",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

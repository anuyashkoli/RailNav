package com.app.railnav

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.railnav.feature.map.ui.PathfindingScreen
import com.app.railnav.feature.pnr.ui.PnrScreen
import com.app.railnav.feature.livetrain.ui.LiveTrainScreen
import com.app.railnav.feature.liveStation.ui.LiveStationScreen
import com.app.railnav.feature.schedule.ui.TrainScheduleScreen
import kotlinx.coroutines.launch

private data class DrawerItem(
    val route: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector
)

private val drawerItems = listOf(
    DrawerItem("pnr", "PNR Status", "Coach & berth info", Icons.Default.ConfirmationNumber),
    DrawerItem("livetrain", "Live Train Status", "Real-time tracking", Icons.Default.GpsFixed),
    DrawerItem("livestation", "Station Departures", "Trains leaving soon", Icons.Default.DepartureBoard),
    DrawerItem("schedule", "Train Schedule", "All stops & timings", Icons.Default.Route)
)

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        "IRCTC Services",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Indian Railways Tools",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))

                    drawerItems.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = {
                                Column {
                                    Text(item.label, fontWeight = FontWeight.Bold)
                                    Text(
                                        item.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(item.route)
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = modifier
        ) {
            composable("map") {
                PathfindingScreen(onOpenDrawer = {
                    scope.launch { drawerState.open() }
                })
            }
            composable("pnr") {
                PnrScreen(onBack = { navController.popBackStack() })
            }
            composable("livetrain") {
                LiveTrainScreen(onBack = { navController.popBackStack() })
            }
            composable("livestation") {
                LiveStationScreen(onBack = { navController.popBackStack() })
            }
            composable("schedule") {
                TrainScheduleScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

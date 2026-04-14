package com.app.railnav

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.railnav.feature.map.ui.PathfindingScreen
import com.app.railnav.feature.pnr.ui.PnrScreen
import com.app.railnav.feature.livetrain.ui.LiveTrainScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen, // Only allow swipe-to-close to prevent gesture conflicts on map
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
                        "Premium Indian Railways Tools",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))

                    NavigationDrawerItem(
                        label = { Text("PNR Status Check", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("pnr")
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    NavigationDrawerItem(
                        label = { Text("Live Train Status", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("livetrain")
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
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
        }
    }
}

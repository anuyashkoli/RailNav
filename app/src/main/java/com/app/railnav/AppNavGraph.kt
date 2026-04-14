package com.app.railnav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.railnav.feature.map.ui.PathfindingScreen
import com.app.railnav.feature.pnr.ui.PnrScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "map",
        modifier = modifier
    ) {
        composable("map") {
            PathfindingScreen()
        }
        composable("pnr") {
            PnrScreen(onBack = { navController.popBackStack() })
        }
        composable("live_board") {
            // Placeholder: Live Board full screen
        }
    }
}

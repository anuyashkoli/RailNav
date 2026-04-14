package com.app.railnav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.railnav.feature.map.ui.PathfindingScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "map"
    ) {
        composable("map") {
            PathfindingScreen()
        }
        composable("live_board") {
            // Placeholder: Live Board full screen
        }
    }
}

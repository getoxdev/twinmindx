package com.twinmindx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.twinmindx.ui.dashboard.DashboardScreen
import com.twinmindx.ui.recording.RecordingScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val RECORDING = "recording/{meetingId}"

    fun recording(meetingId: String) = "recording/$meetingId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToRecording = { meetingId ->
                    navController.navigate(Routes.recording(meetingId))
                }
            )
        }

        composable(
            route = Routes.RECORDING,
            arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId") ?: return@composable
            RecordingScreen(
                meetingId = meetingId,
                onRecordingStopped = { id ->
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DASHBOARD) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

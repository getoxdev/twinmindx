package com.twinmindx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.twinmindx.ui.dashboard.DashboardScreen
import com.twinmindx.ui.recording.RecordingScreen
import com.twinmindx.ui.summary.SummaryScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val RECORDING = "recording/{meetingId}"
    const val SUMMARY = "summary/{meetingId}"

    fun recording(meetingId: String) = "recording/$meetingId"
    fun summary(meetingId: String) = "summary/$meetingId"
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
                },
                onNavigateToSummary = { meetingId ->
                    navController.navigate(Routes.summary(meetingId))
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
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Routes.SUMMARY,
            arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId") ?: return@composable
            SummaryScreen(meetingId = meetingId)
        }
    }
}

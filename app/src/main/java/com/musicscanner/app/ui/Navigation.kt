package com.musicscanner.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.musicscanner.app.ui.screens.CameraScreen
import com.musicscanner.app.ui.screens.HomeScreen
import com.musicscanner.app.ui.screens.PlaybackScreen
import com.musicscanner.app.ui.screens.ProcessingScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object Processing : Screen("processing/{imagePath}") {
        fun createRoute(imagePath: String) = "processing/$imagePath"
    }
    object Playback : Screen("playback")
}

@Composable
fun MusicScannerNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onScanClick = {
                    navController.navigate(Screen.Camera.route)
                }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { imagePath ->
                    navController.navigate(
                        Screen.Processing.createRoute(imagePath.replace("/", "~"))
                    )
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Processing.route,
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath")
                ?.replace("~", "/") ?: ""
            ProcessingScreen(
                imagePath = imagePath,
                onProcessingComplete = {
                    navController.navigate(Screen.Playback.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Playback.route) {
            PlaybackScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNewScanClick = {
                    navController.navigate(Screen.Camera.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
    }
}

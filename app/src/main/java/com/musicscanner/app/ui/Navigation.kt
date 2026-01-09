package com.musicscanner.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.musicscanner.app.ui.screens.CameraScreen
import com.musicscanner.app.ui.screens.HistoryScreen
import com.musicscanner.app.ui.screens.HomeScreen
import com.musicscanner.app.ui.screens.MultiPageScanScreen
import com.musicscanner.app.ui.screens.PlaybackScreen
import com.musicscanner.app.ui.screens.ProcessingScreen
import com.musicscanner.app.ui.viewmodel.MusicScannerViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object CameraMultiPage : Screen("camera_multipage")
    object Processing : Screen("processing/{imagePath}") {
        fun createRoute(imagePath: String) = "processing/$imagePath"
    }
    object Playback : Screen("playback")
    object History : Screen("history")
    object MultiPageScan : Screen("multipage_scan")
    object BatchProcessing : Screen("batch_processing")
}

@Composable
fun MusicScannerNavigation(
    viewModel: MusicScannerViewModel = viewModel()
) {
    val navController = rememberNavController()

    // Collect preview state for camera screen
    val previewData by viewModel.previewData.collectAsState()
    val previewSettings by viewModel.previewSettings.collectAsState()

    // Collect dark mode state
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onScanClick = {
                    navController.navigate(Screen.Camera.route)
                },
                onGalleryImageSelected = { uri ->
                    val encodedPath = Uri.encode(uri.toString())
                    navController.navigate(
                        Screen.Processing.createRoute(encodedPath)
                    )
                },
                onHistoryClick = {
                    navController.navigate(Screen.History.route)
                },
                onMultiPageScanClick = {
                    navController.navigate(Screen.MultiPageScan.route)
                },
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode() }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { imagePath ->
                    // Clear preview data when capturing
                    viewModel.clearPreviewData()
                    navController.navigate(
                        Screen.Processing.createRoute(imagePath.replace("/", "~"))
                    )
                },
                onBackClick = {
                    viewModel.clearPreviewData()
                    navController.popBackStack()
                },
                previewData = previewData,
                previewSettings = previewSettings,
                onPreviewDataUpdate = { data ->
                    viewModel.updatePreviewData(data)
                },
                onTogglePreview = {
                    viewModel.togglePreviewMode()
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

        composable(Screen.History.route) {
            HistoryScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onScoreSelected = { scoreId ->
                    navController.navigate(Screen.Playback.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.MultiPageScan.route) {
            MultiPageScanScreen(
                onNavigateToCamera = {
                    navController.navigate(Screen.CameraMultiPage.route)
                },
                onProcessComplete = {
                    navController.navigate(Screen.Playback.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CameraMultiPage.route) {
            CameraScreen(
                onImageCaptured = { imagePath ->
                    viewModel.addPageToScan(imagePath)
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                },
                previewData = previewData,
                previewSettings = previewSettings,
                onPreviewDataUpdate = { data ->
                    viewModel.updatePreviewData(data)
                },
                onTogglePreview = {
                    viewModel.togglePreviewMode()
                }
            )
        }
    }
}

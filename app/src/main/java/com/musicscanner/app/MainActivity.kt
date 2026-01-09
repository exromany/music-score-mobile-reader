package com.musicscanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicscanner.app.ui.MusicScannerNavigation
import com.musicscanner.app.ui.theme.MusicSheetScannerTheme
import com.musicscanner.app.ui.viewmodel.MusicScannerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MusicScannerViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MusicSheetScannerTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicScannerNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

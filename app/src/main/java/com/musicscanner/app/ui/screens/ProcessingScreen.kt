package com.musicscanner.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicscanner.app.data.ProcessingState
import com.musicscanner.app.ui.viewmodel.MusicScannerViewModel

@Composable
fun ProcessingScreen(
    imagePath: String,
    onProcessingComplete: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: MusicScannerViewModel = viewModel()
) {
    val processingState by viewModel.processingState.collectAsState()

    LaunchedEffect(imagePath) {
        viewModel.processImage(imagePath)
    }

    LaunchedEffect(processingState) {
        if (processingState is ProcessingState.Complete) {
            onProcessingComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when (val state = processingState) {
            is ProcessingState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetryClick = { viewModel.processImage(imagePath) },
                    onBackClick = onBackClick
                )
            }
            else -> {
                ProcessingContent(state = state)
            }
        }
    }
}

@Composable
private fun ProcessingContent(state: ProcessingState) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        // Animated icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .rotate(rotation),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Progress indicator
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Status text
        Text(
            text = getStatusText(state),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step indicators
        ProcessingSteps(currentState = state)
    }
}

@Composable
private fun ProcessingSteps(currentState: ProcessingState) {
    val steps = listOf(
        ProcessingState.LoadingImage to "Loading Image",
        ProcessingState.PreprocessingImage to "Preprocessing",
        ProcessingState.DetectingStaffLines to "Finding Staff Lines",
        ProcessingState.RecognizingNotes to "Recognizing Notes",
        ProcessingState.GeneratingMidi to "Generating Music"
    )

    val currentIndex = steps.indexOfFirst { it.first::class == currentState::class }

    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        steps.forEachIndexed { index, (_, label) ->
            StepIndicator(
                label = label,
                isCompleted = index < currentIndex,
                isCurrent = index == currentIndex
            )
        }
    }
}

@Composable
private fun StepIndicator(
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> MaterialTheme.colorScheme.secondary
                        isCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isCompleted || isCurrent -> MaterialTheme.colorScheme.onBackground
                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            },
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Processing Failed",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onBackClick) {
                Text("Go Back")
            }

            Button(onClick = onRetryClick) {
                Text("Retry")
            }
        }
    }
}

private fun getStatusText(state: ProcessingState): String {
    return when (state) {
        is ProcessingState.Idle -> "Preparing..."
        is ProcessingState.LoadingImage -> "Loading image..."
        is ProcessingState.PreprocessingImage -> "Preprocessing image..."
        is ProcessingState.DetectingStaffLines -> "Detecting staff lines..."
        is ProcessingState.RecognizingNotes -> "Recognizing musical notes..."
        is ProcessingState.GeneratingMidi -> "Generating playable music..."
        is ProcessingState.Complete -> "Processing complete!"
        is ProcessingState.Error -> "Error occurred"
    }
}

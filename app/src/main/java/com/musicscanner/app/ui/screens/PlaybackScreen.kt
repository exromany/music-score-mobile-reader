package com.musicscanner.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicscanner.app.audio.MidiExporter
import com.musicscanner.app.data.MusicNote
import com.musicscanner.app.data.Pitch
import com.musicscanner.app.ui.viewmodel.MusicScannerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    onBackClick: () -> Unit,
    onNewScanClick: () -> Unit,
    viewModel: MusicScannerViewModel = viewModel()
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentScore by viewModel.currentScore.collectAsState()
    val context = LocalContext.current

    val notes = currentScore?.getAllNotes() ?: emptyList()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showTransposeDialog by remember { mutableStateOf(false) }

    // Auto-scroll to current note
    LaunchedEffect(playbackState.currentNoteIndex) {
        if (playbackState.isPlaying && playbackState.currentNoteIndex < notes.size) {
            scope.launch {
                listState.animateScrollToItem(
                    index = playbackState.currentNoteIndex,
                    scrollOffset = -200
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = currentScore?.title ?: "Music Playback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentScore?.composer?.isNotEmpty() == true) {
                        Text(
                            text = currentScore?.composer ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.stop()
                    onBackClick()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // MIDI Export
                IconButton(onClick = {
                    currentScore?.let { score ->
                        val intent = MidiExporter.createShareIntent(context, score)
                        if (intent != null) {
                            context.startActivity(Intent.createChooser(intent, "Export MIDI"))
                        } else {
                            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Export MIDI")
                }
                IconButton(onClick = {
                    viewModel.reset()
                    onNewScanClick()
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "New Scan")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Note visualization
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notes detected",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyRow(
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(notes) { index, note ->
                            NoteCard(
                                note = note,
                                isCurrentNote = index == playbackState.currentNoteIndex && playbackState.isPlaying,
                                isPlayed = index < playbackState.currentNoteIndex
                            )
                        }
                    }
                }
            }
        }

        // Current note display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    label = "Current Note",
                    value = if (playbackState.isPlaying && notes.isNotEmpty()) {
                        notes.getOrNull(playbackState.currentNoteIndex)?.let {
                            "${it.pitch.name}${it.octave}"
                        } ?: "-"
                    } else "-"
                )
                StatItem(
                    label = "Progress",
                    value = "${playbackState.currentNoteIndex + 1}/${notes.size}"
                )
                StatItem(
                    label = "Tempo",
                    value = "${playbackState.tempo} BPM"
                )
                StatItem(
                    label = "Transpose",
                    value = when {
                        playbackState.transposeSemitones > 0 -> "+${playbackState.transposeSemitones}"
                        else -> "${playbackState.transposeSemitones}"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            LinearProgressIndicator(
                progress = playbackState.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatBeats(playbackState.currentBeat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = formatBeats(playbackState.totalBeats),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secondary controls (Loop, Metronome, Transpose)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Loop toggle
            FilterChip(
                selected = playbackState.isLooping,
                onClick = { viewModel.toggleLoop() },
                label = { Text("Loop") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // Metronome toggle
            FilterChip(
                selected = playbackState.isMetronomeEnabled,
                onClick = { viewModel.toggleMetronome() },
                label = { Text("Metronome") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // Transpose button
            FilterChip(
                selected = playbackState.transposeSemitones != 0,
                onClick = { showTransposeDialog = true },
                label = {
                    Text(
                        if (playbackState.transposeSemitones != 0)
                            "${if (playbackState.transposeSemitones > 0) "+" else ""}${playbackState.transposeSemitones}"
                        else "Key"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop button
            IconButton(
                onClick = { viewModel.stop() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(28.dp)
                )
            }

            // Play/Pause button
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (playbackState.isPlaying) 1.1f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            IconButton(
                onClick = { viewModel.togglePlayback() },
                modifier = Modifier
                    .size(80.dp)
                    .scale(if (playbackState.isPlaying) scale else 1f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }

            // Tempo button
            IconButton(
                onClick = {
                    // Cycle through tempos
                    val newTempo = when {
                        playbackState.tempo < 80 -> 80
                        playbackState.tempo < 100 -> 100
                        playbackState.tempo < 120 -> 120
                        playbackState.tempo < 140 -> 140
                        else -> 60
                    }
                    viewModel.setTempo(newTempo)
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Change Tempo",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Transpose dialog
    if (showTransposeDialog) {
        TransposeDialog(
            currentTranspose = playbackState.transposeSemitones,
            onDismiss = { showTransposeDialog = false },
            onTransposeChange = { semitones ->
                viewModel.setTranspose(semitones)
                showTransposeDialog = false
            }
        )
    }
}

@Composable
private fun TransposeDialog(
    currentTranspose: Int,
    onDismiss: () -> Unit,
    onTransposeChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentTranspose.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transpose") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        sliderValue.toInt() > 0 -> "+${sliderValue.toInt()} semitones"
                        sliderValue.toInt() < 0 -> "${sliderValue.toInt()} semitones"
                        else -> "Original key"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = -12f..12f,
                    steps = 23
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-12", style = MaterialTheme.typography.labelSmall)
                    Text("0", style = MaterialTheme.typography.labelSmall)
                    Text("+12", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onTransposeChange(sliderValue.toInt()) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NoteCard(
    note: MusicNote,
    isCurrentNote: Boolean,
    isPlayed: Boolean
) {
    val backgroundColor = when {
        isCurrentNote -> MaterialTheme.colorScheme.secondary
        isPlayed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        isCurrentNote -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentNote) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (note.isRest) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Rest",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Rest",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            } else {
                Text(
                    text = note.pitch.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = note.octave.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.duration.name.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

private fun formatBeats(beats: Float): String {
    val totalSeconds = (beats / 2).toInt() // Assuming 120 BPM default
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

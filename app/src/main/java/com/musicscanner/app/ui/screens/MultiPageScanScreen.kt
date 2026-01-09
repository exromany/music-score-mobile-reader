package com.musicscanner.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicscanner.app.recognition.PageStatus
import com.musicscanner.app.recognition.ScannedPage
import com.musicscanner.app.ui.viewmodel.MusicScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPageScanScreen(
    onNavigateToCamera: () -> Unit,
    onProcessComplete: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: MusicScannerViewModel = viewModel()
) {
    val scannedPages by viewModel.scannedPages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val autoCropEnabled by viewModel.autoCropEnabled.collectAsState()
    val perspectiveCorrectionEnabled by viewModel.perspectiveCorrectionEnabled.collectAsState()

    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addPageToScan(uri.toString())
        }
    }

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.addPageToScan(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Page Scan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Settings button
                    var showSettings by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }

                    DropdownMenu(
                        expanded = showSettings,
                        onDismissRequest = { showSettings = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = autoCropEnabled,
                                        onCheckedChange = { viewModel.toggleAutoCrop() }
                                    )
                                    Text("Auto-crop")
                                }
                            },
                            onClick = { viewModel.toggleAutoCrop() }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = perspectiveCorrectionEnabled,
                                        onCheckedChange = { viewModel.togglePerspectiveCorrection() }
                                    )
                                    Text("Fix skew")
                                }
                            },
                            onClick = { viewModel.togglePerspectiveCorrection() }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Page list
            if (scannedPages.isEmpty()) {
                EmptyPageList(
                    onAddFromCamera = onNavigateToCamera,
                    onAddFromGallery = {
                        multiPhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(scannedPages) { index, page ->
                        PageItem(
                            page = page,
                            isProcessing = currentPageIndex == page.pageNumber && page.status == PageStatus.PROCESSING,
                            onRemove = { viewModel.removePageFromScan(page.pageNumber) }
                        )
                    }

                    item {
                        AddPageButtons(
                            onAddFromCamera = onNavigateToCamera,
                            onAddFromGallery = {
                                singlePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }

                // Bottom action bar
                BottomActionBar(
                    pageCount = scannedPages.size,
                    onClearAll = { viewModel.clearScannedPages() },
                    onProcess = {
                        viewModel.processMultiPageScan()
                    },
                    isProcessing = scannedPages.any { it.status == PageStatus.PROCESSING }
                )
            }
        }
    }

    // Navigate to playback when processing completes
    val processingState by viewModel.processingState.collectAsState()
    LaunchedEffect(processingState) {
        if (processingState is com.musicscanner.app.data.ProcessingState.Complete) {
            onProcessComplete()
        }
    }
}

@Composable
private fun EmptyPageList(
    onAddFromCamera: () -> Unit,
    onAddFromGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No pages added yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add multiple pages of sheet music to scan them together as one score",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onAddFromGallery,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery")
            }

            Button(
                onClick = onAddFromCamera,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera")
            }
        }
    }
}

@Composable
private fun PageItem(
    page: ScannedPage,
    isProcessing: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(page.imagePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Page ${page.pageNumber}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Status overlay
                when (page.status) {
                    PageStatus.PROCESSING -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    PageStatus.COMPLETED -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    PageStatus.ERROR -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Error",
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Page info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Page ${page.pageNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (page.status) {
                        PageStatus.PENDING -> "Ready to process"
                        PageStatus.PROCESSING -> "Processing..."
                        PageStatus.COMPLETED -> "Completed"
                        PageStatus.ERROR -> "Failed to process"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (page.status) {
                        PageStatus.ERROR -> MaterialTheme.colorScheme.error
                        PageStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
            }

            // Remove button
            if (page.status != PageStatus.PROCESSING) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPageButtons(
    onAddFromCamera: () -> Unit,
    onAddFromGallery: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedCard(
            modifier = Modifier
                .weight(1f)
                .clickable { onAddFromGallery() },
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "From Gallery",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        OutlinedCard(
            modifier = Modifier
                .weight(1f)
                .clickable { onAddFromCamera() },
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "From Camera",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    pageCount: Int,
    onClearAll: () -> Unit,
    onProcess: () -> Unit,
    isProcessing: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$pageCount page${if (pageCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = onClearAll,
                enabled = !isProcessing
            ) {
                Text("Clear All")
            }

            Button(
                onClick = onProcess,
                enabled = pageCount > 0 && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process All")
                }
            }
        }
    }
}

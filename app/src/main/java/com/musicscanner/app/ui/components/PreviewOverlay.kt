package com.musicscanner.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.musicscanner.app.recognition.PreviewNoteHead
import com.musicscanner.app.recognition.PreviewSettings
import com.musicscanner.app.recognition.PreviewStaff
import com.musicscanner.app.recognition.RealtimePreviewData

/**
 * Overlay composable that draws detected music elements on top of camera preview
 */
@Composable
fun PreviewOverlay(
    previewData: RealtimePreviewData,
    settings: PreviewSettings,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!settings.enabled || !previewData.hasDetections) return@Canvas

            val width = size.width
            val height = size.height

            // Draw staff lines
            if (settings.showStaffLines) {
                previewData.staffLines.forEach { staff ->
                    drawStaffLines(staff, width, height)
                }
            }

            // Draw note heads
            if (settings.showNoteHeads) {
                previewData.noteHeads.forEach { note ->
                    drawNoteHead(note, width, height, settings.showPitchLabels)
                }
            }
        }

        // Show analysis stats (optional debug info)
        if (settings.enabled && previewData.hasDetections) {
            Text(
                text = "${previewData.staffLines.size} staves, ${previewData.noteHeads.size} notes",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Draw the 5 staff lines with a semi-transparent highlight
 */
private fun DrawScope.drawStaffLines(
    staff: PreviewStaff,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val lineColor = Color(0xFF4CAF50).copy(alpha = 0.8f)  // Green
    val strokeWidth = 2.dp.toPx()

    val startX = staff.leftX * canvasWidth
    val endX = staff.rightX * canvasWidth

    staff.lines.forEach { lineY ->
        val y = lineY * canvasHeight
        drawLine(
            color = lineColor,
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = strokeWidth
        )
    }

    // Draw bracket on left side to indicate staff grouping
    if (staff.lines.isNotEmpty()) {
        val topY = staff.lines.first() * canvasHeight
        val bottomY = staff.lines.last() * canvasHeight
        val bracketX = startX - 10.dp.toPx()

        // Vertical bracket line
        drawLine(
            color = lineColor,
            start = Offset(bracketX, topY),
            end = Offset(bracketX, bottomY),
            strokeWidth = strokeWidth * 1.5f
        )

        // Top hook
        drawLine(
            color = lineColor,
            start = Offset(bracketX, topY),
            end = Offset(startX, topY),
            strokeWidth = strokeWidth
        )

        // Bottom hook
        drawLine(
            color = lineColor,
            start = Offset(bracketX, bottomY),
            end = Offset(startX, bottomY),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Draw a note head with optional pitch label
 */
private fun DrawScope.drawNoteHead(
    note: PreviewNoteHead,
    canvasWidth: Float,
    canvasHeight: Float,
    showLabel: Boolean
) {
    val centerX = note.centerX * canvasWidth
    val centerY = note.centerY * canvasHeight
    val radius = maxOf(note.radius * canvasWidth, 8.dp.toPx())

    // Choose color based on filled/hollow
    val noteColor = if (note.isFilled) {
        Color(0xFFFF9800).copy(alpha = 0.9f)  // Orange for filled
    } else {
        Color(0xFF2196F3).copy(alpha = 0.9f)  // Blue for hollow
    }

    if (note.isFilled) {
        // Draw filled oval
        drawOval(
            color = noteColor,
            topLeft = Offset(centerX - radius * 1.2f, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2.4f, radius * 2f)
        )
    } else {
        // Draw hollow oval (outline only)
        drawOval(
            color = noteColor,
            topLeft = Offset(centerX - radius * 1.2f, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2.4f, radius * 2f),
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // Draw pitch label above the note
    if (showLabel) {
        val labelText = "${note.pitch.name}${note.octave}"
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 10.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                setShadowLayer(2f, 1f, 1f, android.graphics.Color.BLACK)
            }
            drawText(
                labelText,
                centerX,
                centerY - radius - 4.dp.toPx(),
                paint
            )
        }
    }

    // Draw confidence indicator (small dot in corner)
    val confidenceColor = when {
        note.confidence > 0.7f -> Color(0xFF4CAF50)  // Green
        note.confidence > 0.4f -> Color(0xFFFFEB3B)  // Yellow
        else -> Color(0xFFF44336)  // Red
    }
    drawCircle(
        color = confidenceColor,
        radius = 3.dp.toPx(),
        center = Offset(centerX + radius, centerY - radius)
    )
}

/**
 * Preview overlay with scanning animation when no detections
 */
@Composable
fun PreviewOverlayWithScanning(
    previewData: RealtimePreviewData,
    settings: PreviewSettings,
    isAnalyzing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Main overlay
        PreviewOverlay(
            previewData = previewData,
            settings = settings,
            modifier = Modifier.fillMaxSize()
        )

        // Show "Scanning..." indicator when analyzing but no detections
        if (settings.enabled && isAnalyzing && !previewData.hasDetections) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw scanning lines animation placeholder
                val lineColor = Color.Cyan.copy(alpha = 0.5f)
                val centerY = size.height / 2

                // Horizontal scan line
                drawLine(
                    color = lineColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

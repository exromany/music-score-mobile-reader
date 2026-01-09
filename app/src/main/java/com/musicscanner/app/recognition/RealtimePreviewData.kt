package com.musicscanner.app.recognition

import com.musicscanner.app.data.Clef
import com.musicscanner.app.data.Pitch

/**
 * Data classes for real-time preview overlay on camera
 * Contains lightweight representations of detected music elements
 */

/**
 * Complete preview data for a single frame analysis
 */
data class RealtimePreviewData(
    val staffLines: List<PreviewStaff> = emptyList(),
    val noteHeads: List<PreviewNoteHead> = emptyList(),
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val analysisTimeMs: Long = 0,
    val confidence: Float = 0f
) {
    val hasDetections: Boolean
        get() = staffLines.isNotEmpty() || noteHeads.isNotEmpty()
}

/**
 * Detected staff lines for preview overlay
 */
data class PreviewStaff(
    val lines: List<Float>,  // Y positions as ratio (0-1) of frame height
    val leftX: Float,        // Left edge as ratio (0-1) of frame width
    val rightX: Float,       // Right edge as ratio (0-1) of frame width
    val clef: Clef = Clef.TREBLE,
    val confidence: Float = 0f
)

/**
 * Detected note head for preview overlay
 */
data class PreviewNoteHead(
    val centerX: Float,      // X position as ratio (0-1) of frame width
    val centerY: Float,      // Y position as ratio (0-1) of frame height
    val radius: Float,       // Radius as ratio of frame width
    val isFilled: Boolean,
    val pitch: Pitch = Pitch.C,
    val octave: Int = 4,
    val confidence: Float = 0f
)

/**
 * Settings for real-time preview analysis
 */
data class PreviewSettings(
    val enabled: Boolean = false,
    val showStaffLines: Boolean = true,
    val showNoteHeads: Boolean = true,
    val showPitchLabels: Boolean = true,
    val analysisIntervalMs: Long = 100  // Throttle analysis to every N ms
)

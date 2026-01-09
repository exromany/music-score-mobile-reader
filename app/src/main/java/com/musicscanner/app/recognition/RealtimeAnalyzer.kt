package com.musicscanner.app.recognition

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.musicscanner.app.data.Clef
import com.musicscanner.app.data.Pitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Lightweight analyzer for real-time camera frame processing
 * Optimized for speed over accuracy for live preview overlay
 */
class RealtimeAnalyzer {

    private var lastAnalysisTime = 0L

    /**
     * Analyze a camera frame and return preview data
     * Uses downscaled processing for performance
     */
    suspend fun analyze(
        imageProxy: ImageProxy,
        settings: PreviewSettings
    ): RealtimePreviewData = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // Throttle analysis rate
        if (startTime - lastAnalysisTime < settings.analysisIntervalMs) {
            return@withContext RealtimePreviewData()
        }
        lastAnalysisTime = startTime

        try {
            // Convert to bitmap and downscale for performance
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap == null) {
                return@withContext RealtimePreviewData()
            }

            // Downscale for faster processing
            val scaleFactor = 4
            val scaledWidth = bitmap.width / scaleFactor
            val scaledHeight = bitmap.height / scaleFactor
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)

            // Process the scaled image
            val grayscale = toGrayscaleFast(scaledBitmap)
            val binary = binarizeFast(grayscale, scaledWidth, scaledHeight)

            // Detect staff lines
            val staffLines = detectStaffLinesFast(binary, scaledWidth, scaledHeight)

            // Detect note heads if staff lines found
            val noteHeads = if (staffLines.isNotEmpty() && settings.showNoteHeads) {
                detectNoteHeadsFast(binary, scaledWidth, scaledHeight, staffLines)
            } else {
                emptyList()
            }

            // Convert to preview data with normalized coordinates
            val previewStaffs = staffLines.map { staff ->
                PreviewStaff(
                    lines = staff.lines.map { it.toFloat() / scaledHeight },
                    leftX = 0.05f,
                    rightX = 0.95f,
                    clef = staff.detectedClef,
                    confidence = 0.7f
                )
            }

            val previewNotes = noteHeads.map { note ->
                val (pitch, octave) = estimatePitch(note, staffLines)
                PreviewNoteHead(
                    centerX = note.x.toFloat() / scaledWidth,
                    centerY = note.y.toFloat() / scaledHeight,
                    radius = (note.width.toFloat() / scaledWidth) / 2f,
                    isFilled = note.isFilled,
                    pitch = pitch,
                    octave = octave,
                    confidence = 0.6f
                )
            }

            // Clean up bitmaps
            if (bitmap != scaledBitmap) {
                bitmap.recycle()
            }
            scaledBitmap.recycle()

            val analysisTime = System.currentTimeMillis() - startTime

            RealtimePreviewData(
                staffLines = previewStaffs,
                noteHeads = previewNotes,
                frameWidth = imageProxy.width,
                frameHeight = imageProxy.height,
                analysisTimeMs = analysisTime,
                confidence = if (previewStaffs.isNotEmpty()) 0.7f else 0.3f
            )
        } catch (e: Exception) {
            RealtimePreviewData()
        }
    }

    /**
     * Convert ImageProxy to Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )

            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                Rect(0, 0, imageProxy.width, imageProxy.height),
                50, // Lower quality for speed
                out
            )

            val bytes = out.toByteArray()
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fast grayscale conversion using luminance
     */
    private fun toGrayscaleFast(bitmap: Bitmap): IntArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val grayscale = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // Fast approximation: (r + g + g + b) >> 2
            grayscale[i] = (r + g + g + b) shr 2
        }

        return grayscale
    }

    /**
     * Fast binarization with fixed threshold
     */
    private fun binarizeFast(grayscale: IntArray, width: Int, height: Int): BooleanArray {
        // Calculate quick adaptive threshold using sampling
        var sum = 0L
        val sampleStep = 16
        var sampleCount = 0
        for (i in grayscale.indices step sampleStep) {
            sum += grayscale[i]
            sampleCount++
        }
        val threshold = (sum / sampleCount).toInt()

        val binary = BooleanArray(grayscale.size)
        for (i in grayscale.indices) {
            binary[i] = grayscale[i] < threshold
        }
        return binary
    }

    /**
     * Fast staff line detection using horizontal projection
     */
    private fun detectStaffLinesFast(
        binary: BooleanArray,
        width: Int,
        height: Int
    ): List<StaffLineGroup> {
        // Horizontal projection with sampling
        val projection = IntArray(height)
        val sampleStep = 2

        for (y in 0 until height) {
            var count = 0
            for (x in 0 until width step sampleStep) {
                if (binary[y * width + x]) count++
            }
            projection[y] = count * sampleStep
        }

        // Find peaks
        val threshold = (width * 0.25).toInt()
        val peaks = mutableListOf<Int>()

        for (y in 1 until height - 1) {
            if (projection[y] > threshold &&
                projection[y] >= projection[y - 1] &&
                projection[y] >= projection[y + 1]
            ) {
                // Skip if too close to previous peak
                if (peaks.isEmpty() || y - peaks.last() > 3) {
                    peaks.add(y)
                }
            }
        }

        // Group into staves of 5 lines
        return groupIntoStavesFast(peaks, height)
    }

    /**
     * Group peaks into staff groups
     */
    private fun groupIntoStavesFast(peaks: List<Int>, imageHeight: Int): List<StaffLineGroup> {
        if (peaks.size < 5) return emptyList()

        val staffGroups = mutableListOf<StaffLineGroup>()
        var i = 0

        while (i + 4 < peaks.size) {
            val lines = listOf(peaks[i], peaks[i + 1], peaks[i + 2], peaks[i + 3], peaks[i + 4])

            // Check consistent spacing
            val spacings = (0 until 4).map { lines[it + 1] - lines[it] }
            val avgSpacing = spacings.average()
            val maxDeviation = spacings.maxOf { abs(it - avgSpacing) }

            if (maxDeviation < avgSpacing * 0.35) {
                staffGroups.add(
                    StaffLineGroup(
                        lines = lines,
                        lineSpacing = avgSpacing.roundToInt(),
                        topY = lines.first(),
                        bottomY = lines.last()
                    )
                )
                i += 5
            } else {
                i++
            }
        }

        return staffGroups
    }

    /**
     * Fast note head detection
     */
    private fun detectNoteHeadsFast(
        binary: BooleanArray,
        width: Int,
        height: Int,
        staffLines: List<StaffLineGroup>
    ): List<DetectedNoteHead> {
        val noteHeads = mutableListOf<DetectedNoteHead>()

        for (staff in staffLines) {
            val noteSize = staff.lineSpacing
            val searchTop = maxOf(0, staff.topY - noteSize * 3)
            val searchBottom = minOf(height, staff.bottomY + noteSize * 3)
            val visited = BooleanArray(width * height)

            // Scan with larger steps for speed
            val step = maxOf(1, noteSize / 3)

            for (y in searchTop until searchBottom step step) {
                for (x in 0 until width step step) {
                    val idx = y * width + x
                    if (binary[idx] && !visited[idx]) {
                        val blob = floodFillFast(binary, visited, x, y, width, height, noteSize * 3)

                        if (isNoteHeadFast(blob, noteSize)) {
                            val centerX = (blob.minX + blob.maxX) / 2
                            val centerY = (blob.minY + blob.maxY) / 2

                            noteHeads.add(
                                DetectedNoteHead(
                                    x = centerX,
                                    y = centerY,
                                    width = blob.maxX - blob.minX,
                                    height = blob.maxY - blob.minY,
                                    isFilled = blob.filledRatio > 0.5,
                                    staffPosition = calculateStaffPositionFast(centerY, staff),
                                    staffIndex = staffLines.indexOf(staff)
                                )
                            )
                        }
                    }
                }
            }
        }

        return noteHeads.sortedBy { it.x }
    }

    /**
     * Fast flood fill with size limit
     */
    private fun floodFillFast(
        binary: BooleanArray,
        visited: BooleanArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        maxSize: Int
    ): Blob {
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(startX to startY)

        var minX = startX
        var maxX = startX
        var minY = startY
        var maxY = startY
        var pixelCount = 0

        while (stack.isNotEmpty() && pixelCount < maxSize * maxSize) {
            val (x, y) = stack.removeLast()
            val idx = y * width + x

            if (x < 0 || x >= width || y < 0 || y >= height) continue
            if (visited[idx] || !binary[idx]) continue

            visited[idx] = true
            pixelCount++

            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)

            stack.addLast(x - 1 to y)
            stack.addLast(x + 1 to y)
            stack.addLast(x to y - 1)
            stack.addLast(x to y + 1)
        }

        val blobWidth = maxX - minX + 1
        val blobHeight = maxY - minY + 1
        val filledRatio = pixelCount.toFloat() / (blobWidth * blobHeight)

        return Blob(minX, minY, maxX, maxY, pixelCount, filledRatio)
    }

    /**
     * Quick note head validation
     */
    private fun isNoteHeadFast(blob: Blob, expectedSize: Int): Boolean {
        val blobWidth = blob.maxX - blob.minX
        val blobHeight = blob.maxY - blob.minY

        val sizeOk = blobWidth in (expectedSize / 2)..(expectedSize * 2) &&
                blobHeight in (expectedSize / 2)..(expectedSize * 2)

        val aspectRatio = blobWidth.toFloat() / blobHeight.coerceAtLeast(1)
        val aspectOk = aspectRatio in 0.4f..2.5f

        val pixelOk = blob.pixelCount > expectedSize * expectedSize * 0.15

        return sizeOk && aspectOk && pixelOk
    }

    /**
     * Calculate staff position
     */
    private fun calculateStaffPositionFast(noteY: Int, staff: StaffLineGroup): Int {
        val middleLineY = staff.lines[2]
        val halfSpacing = staff.lineSpacing / 2.0
        return -((noteY - middleLineY) / halfSpacing).roundToInt()
    }

    /**
     * Estimate pitch from staff position
     */
    private fun estimatePitch(
        note: DetectedNoteHead,
        staffLines: List<StaffLineGroup>
    ): Pair<Pitch, Int> {
        val staff = staffLines.getOrNull(note.staffIndex) ?: return Pitch.C to 4
        val position = note.staffPosition

        // Simplified treble clef mapping
        val pitches = listOf(
            Pitch.E to 3, Pitch.F to 3, Pitch.G to 3, Pitch.A to 3, Pitch.B to 3,
            Pitch.C to 4, Pitch.D to 4, Pitch.E to 4, Pitch.F to 4, Pitch.G to 4,
            Pitch.A to 4, Pitch.B to 4, Pitch.C to 5, Pitch.D to 5, Pitch.E to 5,
            Pitch.F to 5, Pitch.G to 5
        )

        val index = (position + 6).coerceIn(0, pitches.lastIndex)
        return pitches[index]
    }
}

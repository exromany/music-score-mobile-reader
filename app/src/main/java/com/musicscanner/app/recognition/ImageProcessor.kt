package com.musicscanner.app.recognition

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Handles image preprocessing for music sheet recognition
 */
class ImageProcessor {

    /**
     * Convert image to grayscale
     */
    fun toGrayscale(bitmap: Bitmap): IntArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val grayscale = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            // Standard luminance formula
            grayscale[i] = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt()
        }

        return grayscale
    }

    /**
     * Apply adaptive thresholding for binarization (Otsu's method)
     */
    fun binarize(grayscale: IntArray, width: Int, height: Int): BooleanArray {
        val threshold = calculateOtsuThreshold(grayscale)
        val binary = BooleanArray(grayscale.size)

        for (i in grayscale.indices) {
            binary[i] = grayscale[i] < threshold  // true = black (foreground)
        }

        return binary
    }

    /**
     * Calculate optimal threshold using Otsu's method
     */
    private fun calculateOtsuThreshold(grayscale: IntArray): Int {
        val histogram = IntArray(256)
        for (pixel in grayscale) {
            histogram[pixel.coerceIn(0, 255)]++
        }

        val total = grayscale.size
        var sum = 0.0
        for (i in 0..255) {
            sum += i * histogram[i]
        }

        var sumB = 0.0
        var wB = 0
        var wF: Int
        var maxVariance = 0.0
        var threshold = 0

        for (i in 0..255) {
            wB += histogram[i]
            if (wB == 0) continue

            wF = total - wB
            if (wF == 0) break

            sumB += i * histogram[i]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val variance = wB.toDouble() * wF * (mB - mF) * (mB - mF)

            if (variance > maxVariance) {
                maxVariance = variance
                threshold = i
            }
        }

        return threshold
    }

    /**
     * Detect horizontal lines (staff lines) using horizontal projection
     */
    fun detectStaffLines(
        binary: BooleanArray,
        width: Int,
        height: Int
    ): List<StaffLineGroup> {
        // Calculate horizontal projection
        val horizontalProjection = IntArray(height)
        for (y in 0 until height) {
            var count = 0
            for (x in 0 until width) {
                if (binary[y * width + x]) count++
            }
            horizontalProjection[y] = count
        }

        // Find peaks in horizontal projection (staff lines have high black pixel counts)
        val threshold = (width * 0.3).toInt()
        val staffLineYPositions = mutableListOf<Int>()

        for (y in 0 until height) {
            if (horizontalProjection[y] > threshold) {
                // Check if this is a local maximum
                val prevVal = if (y > 0) horizontalProjection[y - 1] else 0
                val nextVal = if (y < height - 1) horizontalProjection[y + 1] else 0
                if (horizontalProjection[y] >= prevVal && horizontalProjection[y] >= nextVal) {
                    staffLineYPositions.add(y)
                }
            }
        }

        // Group consecutive lines and find center
        val consolidatedLines = consolidateLines(staffLineYPositions)

        // Group lines into staff groups (5 lines per staff)
        return groupIntoStaves(consolidatedLines, height)
    }

    /**
     * Consolidate nearby line detections into single lines
     */
    private fun consolidateLines(lines: List<Int>): List<Int> {
        if (lines.isEmpty()) return emptyList()

        val consolidated = mutableListOf<Int>()
        var currentGroup = mutableListOf(lines[0])

        for (i in 1 until lines.size) {
            if (lines[i] - lines[i - 1] <= 3) {
                currentGroup.add(lines[i])
            } else {
                consolidated.add(currentGroup.average().roundToInt())
                currentGroup = mutableListOf(lines[i])
            }
        }
        consolidated.add(currentGroup.average().roundToInt())

        return consolidated
    }

    /**
     * Group detected lines into staff groups of 5
     */
    private fun groupIntoStaves(lines: List<Int>, imageHeight: Int): List<StaffLineGroup> {
        if (lines.size < 5) return emptyList()

        val staffGroups = mutableListOf<StaffLineGroup>()
        var i = 0

        while (i + 4 < lines.size) {
            // Check if these 5 lines have consistent spacing
            val spacing1 = lines[i + 1] - lines[i]
            val spacing2 = lines[i + 2] - lines[i + 1]
            val spacing3 = lines[i + 3] - lines[i + 2]
            val spacing4 = lines[i + 4] - lines[i + 3]

            val avgSpacing = (spacing1 + spacing2 + spacing3 + spacing4) / 4.0
            val maxDeviation = maxOf(
                abs(spacing1 - avgSpacing),
                abs(spacing2 - avgSpacing),
                abs(spacing3 - avgSpacing),
                abs(spacing4 - avgSpacing)
            )

            // If spacing is consistent, this is a valid staff
            if (maxDeviation < avgSpacing * 0.3) {
                staffGroups.add(
                    StaffLineGroup(
                        lines = listOf(lines[i], lines[i + 1], lines[i + 2], lines[i + 3], lines[i + 4]),
                        lineSpacing = avgSpacing.roundToInt(),
                        topY = lines[i],
                        bottomY = lines[i + 4]
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
     * Detect note heads using template matching / blob detection
     */
    fun detectNoteHeads(
        binary: BooleanArray,
        width: Int,
        height: Int,
        staffLines: List<StaffLineGroup>
    ): List<DetectedNoteHead> {
        val noteHeads = mutableListOf<DetectedNoteHead>()

        for (staff in staffLines) {
            val noteHeadSize = staff.lineSpacing  // Approximate note head diameter
            val searchAreaTop = staff.topY - staff.lineSpacing * 4
            val searchAreaBottom = staff.bottomY + staff.lineSpacing * 4

            // Scan for circular/elliptical blobs (note heads)
            val visited = BooleanArray(width * height)

            for (y in maxOf(0, searchAreaTop) until minOf(height, searchAreaBottom)) {
                for (x in 0 until width) {
                    val idx = y * width + x
                    if (binary[idx] && !visited[idx]) {
                        // Flood fill to find connected component
                        val blob = floodFill(binary, visited, x, y, width, height)

                        // Check if blob looks like a note head
                        if (isNoteHead(blob, noteHeadSize)) {
                            val centerX = (blob.minX + blob.maxX) / 2
                            val centerY = (blob.minY + blob.maxY) / 2

                            // Determine if filled or hollow
                            val isFilled = blob.filledRatio > 0.5

                            // Calculate pitch based on position relative to staff
                            val staffPosition = calculateStaffPosition(centerY, staff)

                            noteHeads.add(
                                DetectedNoteHead(
                                    x = centerX,
                                    y = centerY,
                                    width = blob.maxX - blob.minX,
                                    height = blob.maxY - blob.minY,
                                    isFilled = isFilled,
                                    staffPosition = staffPosition,
                                    staffIndex = staffLines.indexOf(staff)
                                )
                            )
                        }
                    }
                }
            }
        }

        return noteHeads.sortedBy { it.x }  // Sort left to right
    }

    /**
     * Flood fill to find connected component
     */
    private fun floodFill(
        binary: BooleanArray,
        visited: BooleanArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): Blob {
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(startX to startY)

        var minX = startX
        var maxX = startX
        var minY = startY
        var maxY = startY
        var pixelCount = 0

        while (stack.isNotEmpty()) {
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

            // Add neighbors
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
     * Check if blob dimensions match expected note head size
     */
    private fun isNoteHead(blob: Blob, expectedSize: Int): Boolean {
        val blobWidth = blob.maxX - blob.minX
        val blobHeight = blob.maxY - blob.minY

        // Note heads are roughly circular/elliptical
        val aspectRatio = blobWidth.toFloat() / blobHeight.coerceAtLeast(1)

        // Check size is in expected range
        val sizeInRange = blobWidth in (expectedSize / 2)..(expectedSize * 2) &&
                blobHeight in (expectedSize / 2)..(expectedSize * 2)

        // Check aspect ratio (note heads are slightly wider than tall)
        val aspectOk = aspectRatio in 0.5f..2.0f

        // Check pixel count (not too sparse)
        val pixelCountOk = blob.pixelCount > (expectedSize * expectedSize * 0.2)

        return sizeInRange && aspectOk && pixelCountOk
    }

    /**
     * Calculate staff position (0 = middle line, positive = above, negative = below)
     * Each increment is a half-step on the staff
     */
    private fun calculateStaffPosition(noteY: Int, staff: StaffLineGroup): Int {
        val middleLineY = staff.lines[2]  // Third line is middle
        val halfSpacing = staff.lineSpacing / 2.0

        return -((noteY - middleLineY) / halfSpacing).roundToInt()
    }
}

/**
 * Represents a group of 5 staff lines
 */
data class StaffLineGroup(
    val lines: List<Int>,  // Y positions of 5 lines
    val lineSpacing: Int,
    val topY: Int,
    val bottomY: Int
)

/**
 * Represents a detected note head
 */
data class DetectedNoteHead(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isFilled: Boolean,
    val staffPosition: Int,  // Position relative to middle line
    val staffIndex: Int
)

/**
 * Represents a connected component blob
 */
data class Blob(
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
    val pixelCount: Int,
    val filledRatio: Float
)

package com.musicscanner.app.recognition

import android.graphics.Bitmap
import android.graphics.Color
import com.musicscanner.app.data.Clef
import com.musicscanner.app.data.KeySignature
import com.musicscanner.app.data.NoteDuration
import com.musicscanner.app.data.TimeSignature
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
        val binary = BooleanArray(grayscale.size)

        if (grayscale.isEmpty()) {
            return binary
        }

        // Check for uniform image (all pixels same value)
        val firstValue = grayscale[0]
        val isUniform = grayscale.all { it == firstValue }

        if (isUniform) {
            // For uniform images, use 128 as midpoint threshold
            // Black (< 128) = foreground (true), White (>= 128) = background (false)
            val isForeground = firstValue < 128
            for (i in binary.indices) {
                binary[i] = isForeground
            }
            return binary
        }

        val threshold = calculateOtsuThreshold(grayscale)

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

    /**
     * Detect clef type at the beginning of each staff
     * Uses characteristic shape analysis for treble (G) and bass (F) clefs
     */
    fun detectClef(
        binary: BooleanArray,
        width: Int,
        height: Int,
        staff: StaffLineGroup
    ): DetectedClef {
        // Clef appears at the left side of the staff, within first ~15% of width
        val clefSearchWidth = (width * 0.15).toInt()
        val searchTop = staff.topY - staff.lineSpacing
        val searchBottom = staff.bottomY + staff.lineSpacing

        // Analyze the clef region
        val clefRegion = extractRegion(binary, width, 0, searchTop, clefSearchWidth, searchBottom - searchTop)

        // Count black pixels in upper and lower halves to distinguish clefs
        val midY = (searchBottom - searchTop) / 2
        var upperPixels = 0
        var lowerPixels = 0
        var totalPixels = 0

        for (y in 0 until (searchBottom - searchTop)) {
            for (x in 0 until clefSearchWidth) {
                if (clefRegion.getOrNull(y * clefSearchWidth + x) == true) {
                    totalPixels++
                    if (y < midY) upperPixels++ else lowerPixels++
                }
            }
        }

        // Analyze vertical density distribution
        val verticalProfile = IntArray(searchBottom - searchTop)
        for (y in 0 until (searchBottom - searchTop)) {
            for (x in 0 until clefSearchWidth) {
                if (clefRegion.getOrNull(y * clefSearchWidth + x) == true) {
                    verticalProfile[y]++
                }
            }
        }

        // Find the center of mass
        var centerOfMass = 0.0
        if (totalPixels > 0) {
            for (y in verticalProfile.indices) {
                centerOfMass += y * verticalProfile[y]
            }
            centerOfMass /= totalPixels
        }

        // Treble clef: has a spiral in upper part, extends above and below staff
        // Bass clef: has two dots and curve in upper half, concentrated in upper 2/3

        val clefHeight = searchBottom - searchTop
        val normalizedCenter = centerOfMass / clefHeight

        // Detect loops/curves characteristic of treble clef
        val hasUpperCurl = detectCurvedPattern(clefRegion, clefSearchWidth, searchBottom - searchTop, isUpper = true)
        val hasLowerCurl = detectCurvedPattern(clefRegion, clefSearchWidth, searchBottom - searchTop, isUpper = false)

        val detectedClef = when {
            // Treble clef has activity throughout with center of mass near middle
            hasUpperCurl && hasLowerCurl && normalizedCenter in 0.35..0.65 -> Clef.TREBLE
            // Bass clef is concentrated in upper portion with two dots
            upperPixels > lowerPixels * 1.5 && normalizedCenter < 0.45 -> Clef.BASS
            // Alto clef is centered
            normalizedCenter in 0.4..0.6 && !hasUpperCurl -> Clef.ALTO
            // Default to treble for typical sheet music
            else -> Clef.TREBLE
        }

        // Find the right edge of the clef (where music content starts)
        var clefEndX = 0
        for (x in clefSearchWidth - 1 downTo 0) {
            var hasContent = false
            for (y in 0 until (searchBottom - searchTop)) {
                if (clefRegion.getOrNull(y * clefSearchWidth + x) == true) {
                    hasContent = true
                    break
                }
            }
            if (hasContent) {
                clefEndX = x + 10 // Add small margin
                break
            }
        }

        return DetectedClef(
            clef = detectedClef,
            endX = clefEndX,
            confidence = if (totalPixels > staff.lineSpacing * 5) 0.8f else 0.5f
        )
    }

    /**
     * Detect curved patterns (spirals) characteristic of clefs
     */
    private fun detectCurvedPattern(
        region: BooleanArray,
        width: Int,
        height: Int,
        isUpper: Boolean
    ): Boolean {
        val startY = if (isUpper) 0 else height / 2
        val endY = if (isUpper) height / 2 else height

        // Look for horizontal variation indicating curves
        var hasVariation = false
        var prevCenterX = -1

        for (y in startY until endY step 3) {
            var firstX = -1
            var lastX = -1
            for (x in 0 until width) {
                if (region.getOrNull(y * width + x) == true) {
                    if (firstX == -1) firstX = x
                    lastX = x
                }
            }
            if (firstX != -1) {
                val centerX = (firstX + lastX) / 2
                if (prevCenterX != -1 && abs(centerX - prevCenterX) > 3) {
                    hasVariation = true
                }
                prevCenterX = centerX
            }
        }

        return hasVariation
    }

    /**
     * Detect time signature after clef
     */
    fun detectTimeSignature(
        binary: BooleanArray,
        width: Int,
        height: Int,
        staff: StaffLineGroup,
        clefEndX: Int
    ): DetectedTimeSignature {
        // Time signature appears after clef, typically within next 10% of width
        val searchStartX = clefEndX
        val searchEndX = minOf(width, clefEndX + (width * 0.12).toInt())
        val searchWidth = searchEndX - searchStartX

        if (searchWidth <= 0) {
            return DetectedTimeSignature(TimeSignature.COMMON_TIME, searchStartX, 0.3f)
        }

        val searchTop = staff.topY
        val searchBottom = staff.bottomY
        val staffHeight = searchBottom - searchTop

        // Extract the time signature region
        val region = extractRegion(binary, width, searchStartX, searchTop, searchWidth, staffHeight)

        // Divide into upper and lower halves (numerator and denominator)
        val midY = staffHeight / 2
        var upperPixels = 0
        var lowerPixels = 0

        for (y in 0 until staffHeight) {
            for (x in 0 until searchWidth) {
                if (region.getOrNull(y * searchWidth + x) == true) {
                    if (y < midY) upperPixels++ else lowerPixels++
                }
            }
        }

        // Analyze horizontal segments to detect numbers
        val upperSegments = countHorizontalSegments(region, searchWidth, 0, midY)
        val lowerSegments = countHorizontalSegments(region, searchWidth, midY, staffHeight)

        // Common time signatures based on segment patterns
        val timeSignature = when {
            // 4/4 - Common time (both numbers have similar complexity)
            upperSegments in 3..5 && lowerSegments in 3..5 -> TimeSignature.COMMON_TIME
            // 3/4 - Waltz time (3 has more curves, 4 has straight lines)
            upperSegments in 2..4 && lowerSegments in 3..5 -> TimeSignature.WALTZ_TIME
            // 6/8 - Compound time
            upperSegments > 5 && lowerSegments > 5 -> TimeSignature(6, 8)
            // 2/4 - March time
            upperSegments in 1..3 && lowerSegments in 3..5 -> TimeSignature(2, 4)
            // 2/2 - Cut time
            upperSegments in 1..3 && lowerSegments in 1..3 -> TimeSignature.CUT_TIME
            // Default to 4/4
            else -> TimeSignature.COMMON_TIME
        }

        // Find end of time signature
        var timeSignatureEndX = searchStartX
        for (x in searchWidth - 1 downTo 0) {
            var hasContent = false
            for (y in 0 until staffHeight) {
                if (region.getOrNull(y * searchWidth + x) == true) {
                    hasContent = true
                    break
                }
            }
            if (hasContent) {
                timeSignatureEndX = searchStartX + x + 5
                break
            }
        }

        val confidence = if (upperPixels > 0 && lowerPixels > 0) 0.7f else 0.4f

        return DetectedTimeSignature(timeSignature, timeSignatureEndX, confidence)
    }

    /**
     * Count horizontal segments in a region (helps identify numbers)
     */
    private fun countHorizontalSegments(
        region: BooleanArray,
        width: Int,
        startY: Int,
        endY: Int
    ): Int {
        var maxSegments = 0
        for (y in startY until endY) {
            var segments = 0
            var inSegment = false
            for (x in 0 until width) {
                val isBlack = region.getOrNull(y * width + x) == true
                if (isBlack && !inSegment) {
                    segments++
                    inSegment = true
                } else if (!isBlack) {
                    inSegment = false
                }
            }
            maxSegments = maxOf(maxSegments, segments)
        }
        return maxSegments
    }

    /**
     * Detect key signature (sharps or flats after clef/time signature)
     */
    fun detectKeySignature(
        binary: BooleanArray,
        width: Int,
        height: Int,
        staff: StaffLineGroup,
        startX: Int
    ): DetectedKeySignature {
        // Key signature appears after clef (and time signature if present)
        val searchEndX = minOf(width, startX + (width * 0.15).toInt())
        val searchWidth = searchEndX - startX

        if (searchWidth <= 0) {
            return DetectedKeySignature(KeySignature.C_MAJOR, startX, 0.3f)
        }

        val searchTop = staff.topY - staff.lineSpacing
        val searchBottom = staff.bottomY + staff.lineSpacing
        val searchHeight = searchBottom - searchTop

        val region = extractRegion(binary, width, startX, searchTop, searchWidth, searchHeight)

        // Count distinct vertical symbols (sharps/flats appear as vertical groups)
        val accidentalCount = countVerticalSymbols(region, searchWidth, searchHeight, staff.lineSpacing)

        // Determine if sharps or flats based on shape analysis
        val isSharp = detectSharpPattern(region, searchWidth, searchHeight)

        val keySignature = if (accidentalCount > 0) {
            KeySignature(if (isSharp) accidentalCount else -accidentalCount)
        } else {
            KeySignature.C_MAJOR
        }

        // Find end of key signature
        var keySignatureEndX = startX
        for (x in searchWidth - 1 downTo 0) {
            var hasContent = false
            for (y in 0 until searchHeight) {
                if (region.getOrNull(y * searchWidth + x) == true) {
                    hasContent = true
                    break
                }
            }
            if (hasContent) {
                keySignatureEndX = startX + x + 5
                break
            }
        }

        return DetectedKeySignature(
            keySignature = keySignature,
            endX = keySignatureEndX,
            confidence = if (accidentalCount > 0) 0.6f else 0.8f
        )
    }

    /**
     * Count vertical symbols that could be accidentals
     */
    private fun countVerticalSymbols(
        region: BooleanArray,
        width: Int,
        height: Int,
        lineSpacing: Int
    ): Int {
        // Project horizontally to find vertical symbol clusters
        val horizontalProjection = IntArray(width)
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (region.getOrNull(y * width + x) == true) {
                    horizontalProjection[x]++
                }
            }
        }

        // Find peaks (symbols)
        val threshold = height / 4
        var symbolCount = 0
        var inSymbol = false
        var symbolWidth = 0

        for (x in 0 until width) {
            if (horizontalProjection[x] > threshold) {
                if (!inSymbol) {
                    inSymbol = true
                    symbolWidth = 0
                }
                symbolWidth++
            } else if (inSymbol) {
                // End of symbol - check if it's accidental-sized
                if (symbolWidth in (lineSpacing / 2)..(lineSpacing * 2)) {
                    symbolCount++
                }
                inSymbol = false
            }
        }

        return symbolCount
    }

    /**
     * Detect if the pattern is a sharp (has cross pattern) vs flat
     */
    private fun detectSharpPattern(region: BooleanArray, width: Int, height: Int): Boolean {
        // Sharps have crossing horizontal and vertical lines
        // Flats are more curved like a 'b'

        // Count horizontal runs vs vertical density
        var horizontalRuns = 0
        for (y in height / 3 until 2 * height / 3) {
            var runLength = 0
            var maxRun = 0
            for (x in 0 until width) {
                if (region.getOrNull(y * width + x) == true) {
                    runLength++
                    maxRun = maxOf(maxRun, runLength)
                } else {
                    runLength = 0
                }
            }
            if (maxRun > width / 4) horizontalRuns++
        }

        // Sharps have distinct horizontal bars
        return horizontalRuns > height / 6
    }

    /**
     * Detect rests in the staff
     */
    fun detectRests(
        binary: BooleanArray,
        width: Int,
        height: Int,
        staff: StaffLineGroup,
        noteHeads: List<DetectedNoteHead>
    ): List<DetectedRest> {
        val rests = mutableListOf<DetectedRest>()
        val visited = BooleanArray(width * height)

        // Mark note head areas as visited
        for (note in noteHeads) {
            val margin = staff.lineSpacing
            for (y in maxOf(0, note.y - margin) until minOf(height, note.y + margin)) {
                for (x in maxOf(0, note.x - margin) until minOf(width, note.x + margin)) {
                    visited[y * width + x] = true
                }
            }
        }

        val searchTop = staff.topY - staff.lineSpacing
        val searchBottom = staff.bottomY + staff.lineSpacing

        // Scan for rest-like shapes between notes
        for (y in maxOf(0, searchTop) until minOf(height, searchBottom)) {
            for (x in 0 until width) {
                val idx = y * width + x
                if (binary[idx] && !visited[idx]) {
                    val blob = floodFill(binary, visited, x, y, width, height)
                    val restType = classifyRest(blob, staff)
                    if (restType != null) {
                        rests.add(
                            DetectedRest(
                                x = (blob.minX + blob.maxX) / 2,
                                y = (blob.minY + blob.maxY) / 2,
                                duration = restType,
                                staffIndex = 0
                            )
                        )
                    }
                }
            }
        }

        return rests.sortedBy { it.x }
    }

    /**
     * Classify a blob as a rest type based on shape characteristics
     */
    private fun classifyRest(blob: Blob, staff: StaffLineGroup): NoteDuration? {
        val blobWidth = blob.maxX - blob.minX
        val blobHeight = blob.maxY - blob.minY
        val aspectRatio = blobWidth.toFloat() / blobHeight.coerceAtLeast(1)
        val lineSpacing = staff.lineSpacing

        // Check if blob is in the staff area and appropriate size
        val blobCenterY = (blob.minY + blob.maxY) / 2
        if (blobCenterY < staff.topY - lineSpacing * 2 || blobCenterY > staff.bottomY + lineSpacing * 2) {
            return null
        }

        return when {
            // Whole rest: wide rectangle hanging from line
            blobWidth > lineSpacing && blobHeight < lineSpacing && aspectRatio > 1.5f -> NoteDuration.WHOLE
            // Half rest: wide rectangle sitting on line
            blobWidth > lineSpacing * 0.8 && blobHeight < lineSpacing && aspectRatio > 1.5f -> NoteDuration.HALF
            // Quarter rest: tall squiggly shape
            blobHeight > lineSpacing * 2 && blobWidth < lineSpacing * 1.5 && aspectRatio < 0.8f -> NoteDuration.QUARTER
            // Eighth rest: has flag-like shape
            blobHeight > lineSpacing && blobHeight < lineSpacing * 2.5 && blob.filledRatio < 0.4f -> NoteDuration.EIGHTH
            // Sixteenth rest: more compact with two flags
            blobHeight > lineSpacing * 1.5 && blob.filledRatio < 0.35f -> NoteDuration.SIXTEENTH
            else -> null
        }
    }

    /**
     * Detect stems and flags to determine note duration
     */
    fun detectNoteDuration(
        binary: BooleanArray,
        width: Int,
        height: Int,
        noteHead: DetectedNoteHead,
        staff: StaffLineGroup
    ): NoteDuration {
        val searchRadius = staff.lineSpacing * 3
        val noteX = noteHead.x
        val noteY = noteHead.y

        // Look for vertical stem
        val stem = detectStem(binary, width, height, noteX, noteY, noteHead.width, staff.lineSpacing)

        if (stem == null) {
            // No stem - likely whole note
            return NoteDuration.WHOLE
        }

        // Check if note head is filled
        if (!noteHead.isFilled) {
            // Hollow note head with stem - half note
            return NoteDuration.HALF
        }

        // Filled note head with stem - check for flags/beams
        val flagCount = detectFlags(binary, width, height, stem, staff.lineSpacing)

        return when (flagCount) {
            0 -> NoteDuration.QUARTER
            1 -> NoteDuration.EIGHTH
            2 -> NoteDuration.SIXTEENTH
            else -> NoteDuration.THIRTY_SECOND
        }
    }

    /**
     * Detect stem attached to note head
     */
    private fun detectStem(
        binary: BooleanArray,
        width: Int,
        height: Int,
        noteX: Int,
        noteY: Int,
        noteWidth: Int,
        lineSpacing: Int
    ): DetectedStem? {
        // Look for vertical line to the right or left of note head
        val stemSearchWidth = noteWidth / 2 + 3
        val stemMinLength = lineSpacing * 2

        // Check right side (stem up)
        for (startX in noteX until minOf(width, noteX + stemSearchWidth)) {
            var stemLength = 0
            var stemStartY = noteY
            var stemEndY = noteY

            // Search upward
            for (y in noteY downTo maxOf(0, noteY - lineSpacing * 4)) {
                if (binary[y * width + startX]) {
                    stemLength++
                    stemStartY = y
                } else if (stemLength > 0) {
                    break
                }
            }

            if (stemLength >= stemMinLength) {
                return DetectedStem(startX, stemStartY, stemEndY, isUp = true)
            }
        }

        // Check left side (stem down)
        for (startX in noteX downTo maxOf(0, noteX - stemSearchWidth)) {
            var stemLength = 0
            var stemStartY = noteY
            var stemEndY = noteY

            // Search downward
            for (y in noteY until minOf(height, noteY + lineSpacing * 4)) {
                if (binary[y * width + startX]) {
                    stemLength++
                    stemEndY = y
                } else if (stemLength > 0) {
                    break
                }
            }

            if (stemLength >= stemMinLength) {
                return DetectedStem(startX, stemStartY, stemEndY, isUp = false)
            }
        }

        return null
    }

    /**
     * Detect flags on a stem
     */
    private fun detectFlags(
        binary: BooleanArray,
        width: Int,
        height: Int,
        stem: DetectedStem,
        lineSpacing: Int
    ): Int {
        // Flags appear at the end of the stem, extending to the right
        val flagSearchWidth = lineSpacing * 2
        val flagSearchHeight = lineSpacing

        val stemEndY = if (stem.isUp) stem.startY else stem.endY
        val flagSearchStartY = if (stem.isUp) stemEndY else stemEndY - flagSearchHeight

        var flagCount = 0
        var lastFlagY = -100

        // Scan for horizontal extensions (flags)
        for (y in flagSearchStartY until flagSearchStartY + lineSpacing * 2) {
            if (y < 0 || y >= height) continue

            var runLength = 0
            for (x in stem.x until minOf(width, stem.x + flagSearchWidth)) {
                if (binary[y * width + x]) {
                    runLength++
                }
            }

            // Flag is a horizontal run extending from stem
            if (runLength > lineSpacing / 2 && y - lastFlagY > lineSpacing / 2) {
                flagCount++
                lastFlagY = y
            }
        }

        return flagCount
    }

    /**
     * Detect beamed note groups
     */
    fun detectBeams(
        binary: BooleanArray,
        width: Int,
        height: Int,
        noteHeads: List<DetectedNoteHead>,
        staff: StaffLineGroup
    ): List<DetectedBeam> {
        val beams = mutableListOf<DetectedBeam>()

        // Sort notes by x position
        val sortedNotes = noteHeads.sortedBy { it.x }

        // Look for horizontal beams connecting consecutive notes
        var i = 0
        while (i < sortedNotes.size - 1) {
            val startNote = sortedNotes[i]
            val beamNotes = mutableListOf(i)

            // Check if there's a beam to the next note
            var j = i + 1
            while (j < sortedNotes.size) {
                val nextNote = sortedNotes[j]

                // Notes must be close enough horizontally
                if (nextNote.x - sortedNotes[beamNotes.last()].x > staff.lineSpacing * 4) {
                    break
                }

                // Check for beam between notes
                if (hasBeamBetween(binary, width, sortedNotes[beamNotes.last()], nextNote, staff)) {
                    beamNotes.add(j)
                    j++
                } else {
                    break
                }
            }

            if (beamNotes.size > 1) {
                // Count beam layers
                val beamCount = countBeamLayers(binary, width, height, sortedNotes[beamNotes.first()], sortedNotes[beamNotes.last()], staff)
                beams.add(DetectedBeam(beamNotes.toList(), beamCount))
                i = beamNotes.last() + 1
            } else {
                i++
            }
        }

        return beams
    }

    /**
     * Check if there's a beam connecting two notes
     */
    private fun hasBeamBetween(
        binary: BooleanArray,
        width: Int,
        note1: DetectedNoteHead,
        note2: DetectedNoteHead,
        staff: StaffLineGroup
    ): Boolean {
        val startX = note1.x
        val endX = note2.x

        // Look above and below notes for horizontal beam
        for (yOffset in listOf(-staff.lineSpacing * 2, -staff.lineSpacing * 3, staff.lineSpacing * 2, staff.lineSpacing * 3)) {
            val y = (note1.y + note2.y) / 2 + yOffset
            if (y < 0 || y >= binary.size / width) continue

            var consecutiveBlack = 0
            for (x in startX until endX) {
                if (binary[y * width + x]) {
                    consecutiveBlack++
                } else {
                    consecutiveBlack = 0
                }
            }

            // Beam should span most of the distance
            if (consecutiveBlack > (endX - startX) * 0.6) {
                return true
            }
        }

        return false
    }

    /**
     * Count the number of beam layers (1 = eighth, 2 = sixteenth, etc.)
     */
    private fun countBeamLayers(
        binary: BooleanArray,
        width: Int,
        height: Int,
        startNote: DetectedNoteHead,
        endNote: DetectedNoteHead,
        staff: StaffLineGroup
    ): Int {
        val searchStartY = minOf(startNote.y, endNote.y) - staff.lineSpacing * 4
        val searchEndY = maxOf(startNote.y, endNote.y) + staff.lineSpacing * 4
        val midX = (startNote.x + endNote.x) / 2

        var beamCount = 0
        var inBeam = false
        var beamThickness = 0

        for (y in maxOf(0, searchStartY) until minOf(height, searchEndY)) {
            val isBlack = binary.getOrNull(y * width + midX) == true
            if (isBlack) {
                if (!inBeam) {
                    inBeam = true
                    beamThickness = 0
                }
                beamThickness++
            } else if (inBeam) {
                // Check if this was a beam (not a staff line)
                if (beamThickness > 2 && beamThickness < staff.lineSpacing) {
                    beamCount++
                }
                inBeam = false
            }
        }

        return beamCount
    }

    /**
     * Detect chords (multiple notes at the same horizontal position)
     */
    fun detectChords(
        noteHeads: List<DetectedNoteHead>,
        staff: StaffLineGroup
    ): List<DetectedChord> {
        val chords = mutableListOf<DetectedChord>()
        val used = BooleanArray(noteHeads.size)
        val horizontalThreshold = staff.lineSpacing // Notes within this distance are part of same chord

        for (i in noteHeads.indices) {
            if (used[i]) continue

            val chordNotes = mutableListOf(i)
            val baseNote = noteHeads[i]

            // Find other notes at similar x position
            for (j in i + 1 until noteHeads.size) {
                if (used[j]) continue
                val otherNote = noteHeads[j]

                if (abs(otherNote.x - baseNote.x) <= horizontalThreshold) {
                    chordNotes.add(j)
                    used[j] = true
                }
            }

            used[i] = true

            if (chordNotes.size > 1) {
                chords.add(DetectedChord(chordNotes.toList()))
            }
        }

        return chords
    }

    /**
     * Detect grand staff (treble + bass clef linked with bracket)
     */
    fun detectGrandStaff(
        staffLines: List<StaffLineGroup>,
        clefs: List<DetectedClef>
    ): List<GrandStaffGroup> {
        val grandStaffs = mutableListOf<GrandStaffGroup>()

        if (staffLines.size < 2 || clefs.size < 2) {
            return grandStaffs
        }

        var i = 0
        while (i < staffLines.size - 1) {
            val upperStaff = staffLines[i]
            val lowerStaff = staffLines[i + 1]

            // Check if staves are close enough to be a grand staff
            val gap = lowerStaff.topY - upperStaff.bottomY
            val avgSpacing = (upperStaff.lineSpacing + lowerStaff.lineSpacing) / 2

            // Grand staff typically has gap of 2-4 line spacings
            if (gap < avgSpacing * 5) {
                val upperClef = clefs.getOrNull(i)
                val lowerClef = clefs.getOrNull(i + 1)

                // Typically treble on top, bass on bottom
                if (upperClef?.clef == Clef.TREBLE && lowerClef?.clef == Clef.BASS) {
                    grandStaffs.add(GrandStaffGroup(i, i + 1))
                    i += 2
                    continue
                }
            }
            i++
        }

        return grandStaffs
    }

    /**
     * Extract a rectangular region from the binary image
     */
    private fun extractRegion(
        binary: BooleanArray,
        fullWidth: Int,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): BooleanArray {
        val region = BooleanArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val srcX = startX + x
                val srcY = startY + y
                if (srcX >= 0 && srcX < fullWidth && srcY >= 0) {
                    val srcIdx = srcY * fullWidth + srcX
                    if (srcIdx >= 0 && srcIdx < binary.size) {
                        region[y * width + x] = binary[srcIdx]
                    }
                }
            }
        }
        return region
    }
}

/**
 * Represents a group of 5 staff lines
 */
data class StaffLineGroup(
    val lines: List<Int>,  // Y positions of 5 lines
    val lineSpacing: Int,
    val topY: Int,
    val bottomY: Int,
    var detectedClef: Clef = Clef.TREBLE,
    var detectedTimeSignature: TimeSignature = TimeSignature.COMMON_TIME,
    var detectedKeySignature: KeySignature = KeySignature.C_MAJOR
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

/**
 * Represents a detected clef with position info
 */
data class DetectedClef(
    val clef: Clef,
    val endX: Int,  // X position where clef ends
    val confidence: Float
)

/**
 * Represents a detected time signature
 */
data class DetectedTimeSignature(
    val timeSignature: TimeSignature,
    val endX: Int,
    val confidence: Float
)

/**
 * Represents a detected key signature
 */
data class DetectedKeySignature(
    val keySignature: KeySignature,
    val endX: Int,
    val confidence: Float
)

/**
 * Represents a detected rest
 */
data class DetectedRest(
    val x: Int,
    val y: Int,
    val duration: NoteDuration,
    val staffIndex: Int
)

/**
 * Represents a detected stem
 */
data class DetectedStem(
    val x: Int,
    val startY: Int,
    val endY: Int,
    val isUp: Boolean
)

/**
 * Represents a group of beamed notes
 */
data class DetectedBeam(
    val noteIndices: List<Int>,  // Indices into noteHeads list
    val beamCount: Int  // Number of beams (1 = eighth, 2 = sixteenth, etc.)
)

/**
 * Represents a chord (multiple notes at same position)
 */
data class DetectedChord(
    val noteIndices: List<Int>  // Indices into noteHeads list
)

/**
 * Represents a grand staff (treble + bass linked)
 */
data class GrandStaffGroup(
    val trebleStaffIndex: Int,
    val bassStaffIndex: Int
)

package com.musicscanner.app.recognition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Image enhancement utilities for sheet music capture
 * Provides auto-crop, perspective correction, and image preprocessing
 */
class ImageEnhancer {

    /**
     * Result of auto-crop detection containing crop bounds and confidence
     */
    data class CropResult(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val confidence: Float,
        val originalWidth: Int,
        val originalHeight: Int
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
        val isValid: Boolean get() = width > 0 && height > 0 && confidence > 0.3f
    }

    /**
     * Result of perspective detection containing corner points
     */
    data class PerspectiveResult(
        val topLeft: Point,
        val topRight: Point,
        val bottomLeft: Point,
        val bottomRight: Point,
        val confidence: Float,
        val skewAngle: Float
    ) {
        val isSkewed: Boolean get() = abs(skewAngle) > 2.0f
        val needsCorrection: Boolean get() = confidence > 0.4f && isSkewed
    }

    data class Point(val x: Float, val y: Float)

    /**
     * Edge detection result for a single edge
     */
    private data class Edge(
        val start: Point,
        val end: Point,
        val strength: Float,
        val angle: Float
    )

    /**
     * Detect sheet music boundaries for auto-crop
     * Uses edge detection and content analysis to find the music area
     */
    fun detectCropBounds(bitmap: Bitmap): CropResult {
        val width = bitmap.width
        val height = bitmap.height

        // Convert to grayscale for analysis
        val grayscale = toGrayscale(bitmap)

        // Find content boundaries using projection analysis
        val horizontalProjection = calculateHorizontalProjection(grayscale, width, height)
        val verticalProjection = calculateVerticalProjection(grayscale, width, height)

        // Find top boundary (first row with significant content)
        val topBound = findContentStart(horizontalProjection, height)

        // Find bottom boundary (last row with significant content)
        val bottomBound = findContentEnd(horizontalProjection, height)

        // Find left boundary
        val leftBound = findContentStart(verticalProjection, width)

        // Find right boundary
        val rightBound = findContentEnd(verticalProjection, width)

        // Add small margin (2% of dimension)
        val marginX = (width * 0.02f).toInt()
        val marginY = (height * 0.02f).toInt()

        val cropLeft = max(0, leftBound - marginX)
        val cropTop = max(0, topBound - marginY)
        val cropRight = min(width, rightBound + marginX)
        val cropBottom = min(height, bottomBound + marginY)

        // Calculate confidence based on content density
        val contentRatio = calculateContentRatio(grayscale, width, height, cropLeft, cropTop, cropRight, cropBottom)
        val areaRatio = (cropRight - cropLeft).toFloat() * (cropBottom - cropTop) / (width * height)
        val confidence = contentRatio * (1 - abs(areaRatio - 0.7f)) // Best when ~70% of image is content

        return CropResult(
            left = cropLeft,
            top = cropTop,
            right = cropRight,
            bottom = cropBottom,
            confidence = confidence.coerceIn(0f, 1f),
            originalWidth = width,
            originalHeight = height
        )
    }

    /**
     * Apply auto-crop to bitmap
     */
    fun applyCrop(bitmap: Bitmap, cropResult: CropResult): Bitmap {
        if (!cropResult.isValid) {
            return bitmap
        }

        return Bitmap.createBitmap(
            bitmap,
            cropResult.left,
            cropResult.top,
            cropResult.width,
            cropResult.height
        )
    }

    /**
     * Detect perspective distortion in the image
     * Looks for skewed lines that should be horizontal (staff lines)
     */
    fun detectPerspective(bitmap: Bitmap): PerspectiveResult {
        val width = bitmap.width
        val height = bitmap.height

        // Convert to grayscale and detect edges
        val grayscale = toGrayscale(bitmap)
        val edges = detectEdges(grayscale, width, height)

        // Find dominant horizontal lines (should be staff lines)
        val horizontalLines = findHorizontalLines(edges, width, height)

        // Calculate average skew angle from detected lines
        val skewAngle = calculateSkewAngle(horizontalLines)

        // Detect corners of the document
        val corners = detectCorners(grayscale, width, height)

        val confidence = if (horizontalLines.size >= 5) {
            // More staff lines = higher confidence
            min(1f, horizontalLines.size / 10f)
        } else {
            0.3f
        }

        return PerspectiveResult(
            topLeft = corners[0],
            topRight = corners[1],
            bottomLeft = corners[2],
            bottomRight = corners[3],
            confidence = confidence,
            skewAngle = skewAngle
        )
    }

    /**
     * Correct perspective/skew distortion
     */
    fun correctPerspective(bitmap: Bitmap, perspectiveResult: PerspectiveResult): Bitmap {
        if (!perspectiveResult.needsCorrection) {
            return bitmap
        }

        // For simple skew correction, use rotation
        if (abs(perspectiveResult.skewAngle) < 15f) {
            return rotateImage(bitmap, -perspectiveResult.skewAngle)
        }

        // For more complex perspective, apply full transformation
        return applyPerspectiveTransform(bitmap, perspectiveResult)
    }

    /**
     * Rotate image by given angle (in degrees)
     */
    private fun rotateImage(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle, bitmap.width / 2f, bitmap.height / 2f)

        // Calculate new dimensions to avoid cropping
        val radians = Math.toRadians(abs(angle.toDouble()))
        val newWidth = (bitmap.width * cos(radians) + bitmap.height * sin(radians)).toInt()
        val newHeight = (bitmap.width * sin(radians) + bitmap.height * cos(radians)).toInt()

        val result = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // Center the rotated image
        val dx = (newWidth - bitmap.width) / 2f
        val dy = (newHeight - bitmap.height) / 2f
        matrix.postTranslate(dx, dy)

        canvas.drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
        return result
    }

    /**
     * Apply full perspective transformation
     */
    private fun applyPerspectiveTransform(bitmap: Bitmap, perspective: PerspectiveResult): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate target rectangle (straightened)
        val targetWidth = max(
            distance(perspective.topLeft, perspective.topRight),
            distance(perspective.bottomLeft, perspective.bottomRight)
        ).toInt()
        val targetHeight = max(
            distance(perspective.topLeft, perspective.bottomLeft),
            distance(perspective.topRight, perspective.bottomRight)
        ).toInt()

        // Create output bitmap
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

        // Use bilinear interpolation for perspective transform
        val srcPixels = IntArray(width * height)
        bitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(targetWidth * targetHeight)

        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                // Calculate source position using inverse perspective mapping
                val tx = x.toFloat() / targetWidth
                val ty = y.toFloat() / targetHeight

                // Bilinear interpolation of corners
                val srcX = bilinearInterpolate(
                    perspective.topLeft.x, perspective.topRight.x,
                    perspective.bottomLeft.x, perspective.bottomRight.x,
                    tx, ty
                )
                val srcY = bilinearInterpolate(
                    perspective.topLeft.y, perspective.topRight.y,
                    perspective.bottomLeft.y, perspective.bottomRight.y,
                    tx, ty
                )

                dstPixels[y * targetWidth + x] = sampleBilinear(srcPixels, width, height, srcX, srcY)
            }
        }

        result.setPixels(dstPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        return result
    }

    /**
     * Enhance image for better recognition
     * Applies contrast adjustment and noise reduction
     */
    fun enhanceForRecognition(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calculate histogram for adaptive enhancement
        val histogram = IntArray(256)
        for (pixel in pixels) {
            val gray = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
            histogram[gray.coerceIn(0, 255)]++
        }

        // Find min/max for contrast stretching
        var minVal = 0
        var maxVal = 255
        val threshold = (width * height * 0.01).toInt()
        var count = 0
        for (i in 0..255) {
            count += histogram[i]
            if (count > threshold) {
                minVal = i
                break
            }
        }
        count = 0
        for (i in 255 downTo 0) {
            count += histogram[i]
            if (count > threshold) {
                maxVal = i
                break
            }
        }

        // Apply contrast stretching
        val range = (maxVal - minVal).coerceAtLeast(1)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((Color.red(pixel) - minVal) * 255 / range).coerceIn(0, 255)
            val g = ((Color.green(pixel) - minVal) * 255 / range).coerceIn(0, 255)
            val b = ((Color.blue(pixel) - minVal) * 255 / range).coerceIn(0, 255)
            pixels[i] = Color.argb(255, r, g, b)
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Full preprocessing pipeline: crop, correct perspective, enhance
     */
    fun preprocess(bitmap: Bitmap, autoCrop: Boolean = true, correctSkew: Boolean = true): Bitmap {
        var result = bitmap

        if (autoCrop) {
            val cropResult = detectCropBounds(result)
            if (cropResult.isValid) {
                result = applyCrop(result, cropResult)
            }
        }

        if (correctSkew) {
            val perspectiveResult = detectPerspective(result)
            if (perspectiveResult.needsCorrection) {
                result = correctPerspective(result, perspectiveResult)
            }
        }

        // Always enhance for better recognition
        result = enhanceForRecognition(result)

        return result
    }

    // Helper functions

    private fun toGrayscale(bitmap: Bitmap): IntArray {
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
            grayscale[i] = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt()
        }

        return grayscale
    }

    private fun calculateHorizontalProjection(grayscale: IntArray, width: Int, height: Int): IntArray {
        val projection = IntArray(height)
        val threshold = 128 // Consider dark pixels as content

        for (y in 0 until height) {
            var count = 0
            for (x in 0 until width) {
                if (grayscale[y * width + x] < threshold) {
                    count++
                }
            }
            projection[y] = count
        }

        return projection
    }

    private fun calculateVerticalProjection(grayscale: IntArray, width: Int, height: Int): IntArray {
        val projection = IntArray(width)
        val threshold = 128

        for (x in 0 until width) {
            var count = 0
            for (y in 0 until height) {
                if (grayscale[y * width + x] < threshold) {
                    count++
                }
            }
            projection[x] = count
        }

        return projection
    }

    private fun findContentStart(projection: IntArray, size: Int): Int {
        val threshold = projection.average() * 0.1

        for (i in 0 until size) {
            if (projection[i] > threshold) {
                return i
            }
        }
        return 0
    }

    private fun findContentEnd(projection: IntArray, size: Int): Int {
        val threshold = projection.average() * 0.1

        for (i in size - 1 downTo 0) {
            if (projection[i] > threshold) {
                return i
            }
        }
        return size
    }

    private fun calculateContentRatio(
        grayscale: IntArray,
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Float {
        var contentPixels = 0
        var totalPixels = 0
        val threshold = 128

        for (y in top until bottom) {
            for (x in left until right) {
                totalPixels++
                if (grayscale[y * width + x] < threshold) {
                    contentPixels++
                }
            }
        }

        return if (totalPixels > 0) contentPixels.toFloat() / totalPixels else 0f
    }

    private fun detectEdges(grayscale: IntArray, width: Int, height: Int): BooleanArray {
        val edges = BooleanArray(width * height)
        val threshold = 30

        // Simple Sobel edge detection
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = -grayscale[(y - 1) * width + (x - 1)] +
                        grayscale[(y - 1) * width + (x + 1)] +
                        -2 * grayscale[y * width + (x - 1)] +
                        2 * grayscale[y * width + (x + 1)] +
                        -grayscale[(y + 1) * width + (x - 1)] +
                        grayscale[(y + 1) * width + (x + 1)]

                val gy = -grayscale[(y - 1) * width + (x - 1)] +
                        -2 * grayscale[(y - 1) * width + x] +
                        -grayscale[(y - 1) * width + (x + 1)] +
                        grayscale[(y + 1) * width + (x - 1)] +
                        2 * grayscale[(y + 1) * width + x] +
                        grayscale[(y + 1) * width + (x + 1)]

                val magnitude = sqrt((gx * gx + gy * gy).toFloat())
                edges[y * width + x] = magnitude > threshold
            }
        }

        return edges
    }

    private fun findHorizontalLines(edges: BooleanArray, width: Int, height: Int): List<Edge> {
        val lines = mutableListOf<Edge>()
        val minLineLength = width * 0.3

        for (y in 0 until height) {
            var lineStart = -1
            var lineLength = 0

            for (x in 0 until width) {
                if (edges[y * width + x]) {
                    if (lineStart == -1) {
                        lineStart = x
                    }
                    lineLength++
                } else {
                    if (lineLength > minLineLength) {
                        lines.add(Edge(
                            start = Point(lineStart.toFloat(), y.toFloat()),
                            end = Point((lineStart + lineLength).toFloat(), y.toFloat()),
                            strength = lineLength.toFloat() / width,
                            angle = 0f
                        ))
                    }
                    lineStart = -1
                    lineLength = 0
                }
            }

            if (lineLength > minLineLength) {
                lines.add(Edge(
                    start = Point(lineStart.toFloat(), y.toFloat()),
                    end = Point((lineStart + lineLength).toFloat(), y.toFloat()),
                    strength = lineLength.toFloat() / width,
                    angle = 0f
                ))
            }
        }

        return lines
    }

    private fun calculateSkewAngle(lines: List<Edge>): Float {
        if (lines.isEmpty()) return 0f

        val angles = lines.map { line ->
            val dx = line.end.x - line.start.x
            val dy = line.end.y - line.start.y
            Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        }

        // Return weighted average angle (weighted by line strength)
        var weightedSum = 0f
        var totalWeight = 0f
        for (i in lines.indices) {
            weightedSum += angles[i] * lines[i].strength
            totalWeight += lines[i].strength
        }

        return if (totalWeight > 0) weightedSum / totalWeight else 0f
    }

    private fun detectCorners(grayscale: IntArray, width: Int, height: Int): Array<Point> {
        // Simple corner detection: find content boundaries
        val hProj = calculateHorizontalProjection(grayscale, width, height)
        val vProj = calculateVerticalProjection(grayscale, width, height)

        val top = findContentStart(hProj, height)
        val bottom = findContentEnd(hProj, height)
        val left = findContentStart(vProj, width)
        val right = findContentEnd(vProj, width)

        return arrayOf(
            Point(left.toFloat(), top.toFloat()),      // top-left
            Point(right.toFloat(), top.toFloat()),     // top-right
            Point(left.toFloat(), bottom.toFloat()),   // bottom-left
            Point(right.toFloat(), bottom.toFloat())   // bottom-right
        )
    }

    private fun distance(p1: Point, p2: Point): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun bilinearInterpolate(
        topLeft: Float, topRight: Float,
        bottomLeft: Float, bottomRight: Float,
        tx: Float, ty: Float
    ): Float {
        val top = topLeft + (topRight - topLeft) * tx
        val bottom = bottomLeft + (bottomRight - bottomLeft) * tx
        return top + (bottom - top) * ty
    }

    private fun sampleBilinear(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
        val x0 = x.toInt().coerceIn(0, width - 1)
        val y0 = y.toInt().coerceIn(0, height - 1)
        val x1 = (x0 + 1).coerceIn(0, width - 1)
        val y1 = (y0 + 1).coerceIn(0, height - 1)

        val fx = x - x0
        val fy = y - y0

        val p00 = pixels[y0 * width + x0]
        val p10 = pixels[y0 * width + x1]
        val p01 = pixels[y1 * width + x0]
        val p11 = pixels[y1 * width + x1]

        val r = bilinearInterpolateInt(Color.red(p00), Color.red(p10), Color.red(p01), Color.red(p11), fx, fy)
        val g = bilinearInterpolateInt(Color.green(p00), Color.green(p10), Color.green(p01), Color.green(p11), fx, fy)
        val b = bilinearInterpolateInt(Color.blue(p00), Color.blue(p10), Color.blue(p01), Color.blue(p11), fx, fy)

        return Color.argb(255, r, g, b)
    }

    private fun bilinearInterpolateInt(v00: Int, v10: Int, v01: Int, v11: Int, fx: Float, fy: Float): Int {
        val top = v00 + (v10 - v00) * fx
        val bottom = v01 + (v11 - v01) * fx
        return (top + (bottom - top) * fy).roundToInt().coerceIn(0, 255)
    }
}

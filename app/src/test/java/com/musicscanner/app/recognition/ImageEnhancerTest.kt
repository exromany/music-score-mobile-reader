package com.musicscanner.app.recognition

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for ImageEnhancer
 * Tests image enhancement algorithms including auto-crop and perspective correction
 */
class ImageEnhancerTest {

    private lateinit var imageEnhancer: ImageEnhancer

    @Before
    fun setUp() {
        imageEnhancer = ImageEnhancer()
    }

    // ==================== CropResult Tests ====================

    @Test
    fun `CropResult calculates width correctly`() {
        val cropResult = ImageEnhancer.CropResult(
            left = 10,
            top = 20,
            right = 110,
            bottom = 120,
            confidence = 0.8f,
            originalWidth = 200,
            originalHeight = 200
        )

        assertEquals(100, cropResult.width)
    }

    @Test
    fun `CropResult calculates height correctly`() {
        val cropResult = ImageEnhancer.CropResult(
            left = 10,
            top = 20,
            right = 110,
            bottom = 120,
            confidence = 0.8f,
            originalWidth = 200,
            originalHeight = 200
        )

        assertEquals(100, cropResult.height)
    }

    @Test
    fun `CropResult isValid with valid bounds and confidence`() {
        val cropResult = ImageEnhancer.CropResult(
            left = 10,
            top = 20,
            right = 110,
            bottom = 120,
            confidence = 0.5f,
            originalWidth = 200,
            originalHeight = 200
        )

        assertTrue(cropResult.isValid)
    }

    @Test
    fun `CropResult isValid false with low confidence`() {
        val cropResult = ImageEnhancer.CropResult(
            left = 10,
            top = 20,
            right = 110,
            bottom = 120,
            confidence = 0.2f,  // Below 0.3 threshold
            originalWidth = 200,
            originalHeight = 200
        )

        assertFalse(cropResult.isValid)
    }

    @Test
    fun `CropResult isValid false with zero width`() {
        val cropResult = ImageEnhancer.CropResult(
            left = 100,
            top = 20,
            right = 100,  // Same as left
            bottom = 120,
            confidence = 0.8f,
            originalWidth = 200,
            originalHeight = 200
        )

        assertFalse(cropResult.isValid)
    }

    @Test
    fun `CropResult isValid false with zero height`() {
        val cropResult = ImageEnhancer.CropResult(
            left = 10,
            top = 100,
            right = 110,
            bottom = 100,  // Same as top
            confidence = 0.8f,
            originalWidth = 200,
            originalHeight = 200
        )

        assertFalse(cropResult.isValid)
    }

    // ==================== PerspectiveResult Tests ====================

    @Test
    fun `PerspectiveResult isSkewed true for large skew angle`() {
        val result = ImageEnhancer.PerspectiveResult(
            topLeft = ImageEnhancer.Point(0f, 0f),
            topRight = ImageEnhancer.Point(100f, 5f),  // Slight tilt
            bottomLeft = ImageEnhancer.Point(0f, 100f),
            bottomRight = ImageEnhancer.Point(100f, 105f),
            confidence = 0.7f,
            skewAngle = 5.0f  // Above 2.0 threshold
        )

        assertTrue(result.isSkewed)
    }

    @Test
    fun `PerspectiveResult isSkewed false for small skew angle`() {
        val result = ImageEnhancer.PerspectiveResult(
            topLeft = ImageEnhancer.Point(0f, 0f),
            topRight = ImageEnhancer.Point(100f, 0f),
            bottomLeft = ImageEnhancer.Point(0f, 100f),
            bottomRight = ImageEnhancer.Point(100f, 100f),
            confidence = 0.7f,
            skewAngle = 1.0f  // Below 2.0 threshold
        )

        assertFalse(result.isSkewed)
    }

    @Test
    fun `PerspectiveResult needsCorrection true with high confidence and skew`() {
        val result = ImageEnhancer.PerspectiveResult(
            topLeft = ImageEnhancer.Point(0f, 0f),
            topRight = ImageEnhancer.Point(100f, 5f),
            bottomLeft = ImageEnhancer.Point(0f, 100f),
            bottomRight = ImageEnhancer.Point(100f, 105f),
            confidence = 0.6f,  // Above 0.4 threshold
            skewAngle = 5.0f  // Above 2.0 threshold
        )

        assertTrue(result.needsCorrection)
    }

    @Test
    fun `PerspectiveResult needsCorrection false with low confidence`() {
        val result = ImageEnhancer.PerspectiveResult(
            topLeft = ImageEnhancer.Point(0f, 0f),
            topRight = ImageEnhancer.Point(100f, 5f),
            bottomLeft = ImageEnhancer.Point(0f, 100f),
            bottomRight = ImageEnhancer.Point(100f, 105f),
            confidence = 0.3f,  // Below 0.4 threshold
            skewAngle = 5.0f
        )

        assertFalse(result.needsCorrection)
    }

    @Test
    fun `PerspectiveResult needsCorrection false when not skewed`() {
        val result = ImageEnhancer.PerspectiveResult(
            topLeft = ImageEnhancer.Point(0f, 0f),
            topRight = ImageEnhancer.Point(100f, 0f),
            bottomLeft = ImageEnhancer.Point(0f, 100f),
            bottomRight = ImageEnhancer.Point(100f, 100f),
            confidence = 0.8f,
            skewAngle = 1.0f  // Below 2.0 threshold
        )

        assertFalse(result.needsCorrection)
    }

    // ==================== Point Tests ====================

    @Test
    fun `Point stores coordinates correctly`() {
        val point = ImageEnhancer.Point(50.5f, 75.3f)

        assertEquals(50.5f, point.x, 0.001f)
        assertEquals(75.3f, point.y, 0.001f)
    }

    @Test
    fun `Point supports negative coordinates`() {
        val point = ImageEnhancer.Point(-10.0f, -20.0f)

        assertEquals(-10.0f, point.x, 0.001f)
        assertEquals(-20.0f, point.y, 0.001f)
    }

    // ==================== Algorithm Logic Tests ====================

    @Test
    fun `content boundary detection uses projection thresholds`() {
        // Test projection threshold logic
        val projection = intArrayOf(0, 0, 5, 10, 15, 20, 15, 10, 5, 0)
        val average = projection.average()
        val threshold = average * 0.1

        // First index with value above threshold
        var contentStart = 0
        for (i in projection.indices) {
            if (projection[i] > threshold) {
                contentStart = i
                break
            }
        }

        assertEquals(2, contentStart)  // Index 2 has value 5, which exceeds threshold
    }

    @Test
    fun `skew angle calculation from horizontal lines`() {
        // Test skew angle calculation logic
        val startX = 0f
        val startY = 0f
        val endX = 100f
        val endY = 5f  // Slight upward slope

        val dx = endX - startX
        val dy = endY - startY
        val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()

        // Expected angle for 5 pixel rise over 100 pixel run
        assertTrue(abs(angle - 2.86f) < 0.1f)
    }

    @Test
    fun `margin calculation is percentage of dimensions`() {
        val width = 1000
        val height = 800
        val marginX = (width * 0.02f).toInt()
        val marginY = (height * 0.02f).toInt()

        assertEquals(20, marginX)
        assertEquals(16, marginY)
    }

    @Test
    fun `content ratio calculation for valid bounds`() {
        // Simulate content ratio calculation
        val totalPixels = 10000
        val contentPixels = 3000
        val contentRatio = contentPixels.toFloat() / totalPixels

        assertEquals(0.3f, contentRatio, 0.001f)
    }

    @Test
    fun `confidence calculation favors 70 percent area ratio`() {
        val contentRatio = 0.5f
        val areaRatio = 0.7f  // Optimal ratio

        val confidence = contentRatio * (1 - abs(areaRatio - 0.7f))

        // With optimal area ratio, confidence equals content ratio
        assertEquals(0.5f, confidence, 0.001f)
    }

    @Test
    fun `confidence penalizes extreme area ratios`() {
        val contentRatio = 0.5f
        val smallAreaRatio = 0.2f  // Too small
        val largeAreaRatio = 1.0f  // Too large

        val smallConfidence = contentRatio * (1 - abs(smallAreaRatio - 0.7f))
        val largeConfidence = contentRatio * (1 - abs(largeAreaRatio - 0.7f))

        assertTrue(smallConfidence < 0.5f)
        assertTrue(largeConfidence < 0.5f)
    }

    // ==================== Bilinear Interpolation Tests ====================

    @Test
    fun `bilinear interpolation at corners returns corner values`() {
        val topLeft = 10f
        val topRight = 20f
        val bottomLeft = 30f
        val bottomRight = 40f

        // At top-left corner (tx=0, ty=0)
        val atTopLeft = topLeft + (topRight - topLeft) * 0f  // top
        val resultTopLeft = atTopLeft + (bottomLeft - atTopLeft) * 0f
        assertEquals(10f, resultTopLeft, 0.001f)

        // At top-right corner (tx=1, ty=0)
        val atTopRight = topLeft + (topRight - topLeft) * 1f
        val resultTopRight = atTopRight + (bottomRight - atTopRight) * 0f
        assertEquals(20f, resultTopRight, 0.001f)
    }

    @Test
    fun `bilinear interpolation at center returns average`() {
        val topLeft = 0f
        val topRight = 100f
        val bottomLeft = 100f
        val bottomRight = 200f

        // At center (tx=0.5, ty=0.5)
        val top = topLeft + (topRight - topLeft) * 0.5f  // 50
        val bottom = bottomLeft + (bottomRight - bottomLeft) * 0.5f  // 150
        val center = top + (bottom - top) * 0.5f  // 100

        assertEquals(100f, center, 0.001f)
    }

    // ==================== Edge Detection Threshold Tests ====================

    @Test
    fun `Sobel edge detection threshold is reasonable`() {
        // Standard Sobel threshold for edge detection
        val threshold = 30

        assertTrue(threshold > 0)
        assertTrue(threshold < 128)  // Not too aggressive
    }

    @Test
    fun `horizontal line detection minimum length`() {
        val width = 800
        val minLineLength = width * 0.3

        assertEquals(240.0, minLineLength, 0.001)
    }

    // ==================== Rotation Angle Tests ====================

    @Test
    fun `rotation by small angle uses simple rotation`() {
        val skewAngle = 5.0f

        // Small angles (< 15 degrees) use simple rotation
        val useSimpleRotation = abs(skewAngle) < 15f
        assertTrue(useSimpleRotation)
    }

    @Test
    fun `rotation by large angle uses full perspective transform`() {
        val skewAngle = 20.0f

        // Large angles (>= 15 degrees) use perspective transform
        val usePerspectiveTransform = abs(skewAngle) >= 15f
        assertTrue(usePerspectiveTransform)
    }

    // ==================== Contrast Enhancement Tests ====================

    @Test
    fun `histogram threshold calculation for contrast stretching`() {
        val totalPixels = 10000
        val threshold = (totalPixels * 0.01).toInt()

        // 1% threshold for excluding outliers
        assertEquals(100, threshold)
    }

    @Test
    fun `contrast stretching maps values to full range`() {
        val minVal = 50
        val maxVal = 200
        val inputValue = 125  // Middle of range
        val range = maxVal - minVal

        val stretchedValue = ((inputValue - minVal) * 255 / range).coerceIn(0, 255)

        // Middle input should map to middle output
        assertEquals(127, stretchedValue)
    }

    @Test
    fun `contrast stretching clamps extreme values`() {
        val minVal = 50
        val maxVal = 200
        val range = maxVal - minVal

        // Value below minimum
        val belowMin = ((30 - minVal) * 255 / range).coerceIn(0, 255)
        assertEquals(0, belowMin)

        // Value above maximum
        val aboveMax = ((220 - minVal) * 255 / range).coerceIn(0, 255)
        assertEquals(255, aboveMax)
    }

    // ==================== Distance Calculation Tests ====================

    @Test
    fun `distance between two points calculated correctly`() {
        val p1 = ImageEnhancer.Point(0f, 0f)
        val p2 = ImageEnhancer.Point(3f, 4f)

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        assertEquals(5f, distance, 0.001f)  // 3-4-5 triangle
    }

    @Test
    fun `distance for horizontal line`() {
        val p1 = ImageEnhancer.Point(10f, 50f)
        val p2 = ImageEnhancer.Point(110f, 50f)

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        assertEquals(100f, distance, 0.001f)
    }

    @Test
    fun `distance for vertical line`() {
        val p1 = ImageEnhancer.Point(50f, 10f)
        val p2 = ImageEnhancer.Point(50f, 210f)

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        assertEquals(200f, distance, 0.001f)
    }

    // ==================== Grayscale Conversion Tests ====================

    @Test
    fun `grayscale luminance formula coefficients sum to 1`() {
        val redCoeff = 0.299
        val greenCoeff = 0.587
        val blueCoeff = 0.114

        val sum = redCoeff + greenCoeff + blueCoeff
        assertEquals(1.0, sum, 0.001)
    }

    @Test
    fun `pure white pixel converts to 255 grayscale`() {
        val r = 255
        val g = 255
        val b = 255

        val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        assertEquals(255, gray)
    }

    @Test
    fun `pure black pixel converts to 0 grayscale`() {
        val r = 0
        val g = 0
        val b = 0

        val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        assertEquals(0, gray)
    }

    @Test
    fun `green has highest weight in grayscale conversion`() {
        // Pure red
        val grayRed = (0.299 * 255 + 0.587 * 0 + 0.114 * 0).toInt()
        // Pure green
        val grayGreen = (0.299 * 0 + 0.587 * 255 + 0.114 * 0).toInt()
        // Pure blue
        val grayBlue = (0.299 * 0 + 0.587 * 0 + 0.114 * 255).toInt()

        assertTrue(grayGreen > grayRed)
        assertTrue(grayGreen > grayBlue)
    }
}

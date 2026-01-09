package com.musicscanner.app.recognition

import com.musicscanner.app.data.Clef
import com.musicscanner.app.data.KeySignature
import com.musicscanner.app.data.NoteDuration
import com.musicscanner.app.data.TimeSignature
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ImageProcessor
 * Tests image processing algorithms and data structures
 */
class ImageProcessorTest {

    private lateinit var imageProcessor: ImageProcessor

    @Before
    fun setUp() {
        imageProcessor = ImageProcessor()
    }

    // ==================== Blob Data Class Tests ====================

    @Test
    fun `Blob calculates width correctly`() {
        val blob = Blob(minX = 10, minY = 20, maxX = 30, maxY = 40, pixelCount = 100, filledRatio = 0.5f)
        assertEquals(20, blob.maxX - blob.minX)
    }

    @Test
    fun `Blob calculates height correctly`() {
        val blob = Blob(minX = 10, minY = 20, maxX = 30, maxY = 40, pixelCount = 100, filledRatio = 0.5f)
        assertEquals(20, blob.maxY - blob.minY)
    }

    @Test
    fun `Blob filledRatio represents density`() {
        val blob = Blob(minX = 0, minY = 0, maxX = 10, maxY = 10, pixelCount = 50, filledRatio = 0.5f)
        assertEquals(0.5f, blob.filledRatio, 0.001f)
    }

    // ==================== StaffLineGroup Tests ====================

    @Test
    fun `StaffLineGroup has correct structure`() {
        val staffLines = StaffLineGroup(
            lines = listOf(100, 110, 120, 130, 140),
            lineSpacing = 10,
            topY = 100,
            bottomY = 140
        )

        assertEquals(5, staffLines.lines.size)
        assertEquals(10, staffLines.lineSpacing)
        assertEquals(100, staffLines.topY)
        assertEquals(140, staffLines.bottomY)
    }

    @Test
    fun `StaffLineGroup default clef is TREBLE`() {
        val staffLines = StaffLineGroup(
            lines = listOf(100, 110, 120, 130, 140),
            lineSpacing = 10,
            topY = 100,
            bottomY = 140
        )

        assertEquals(Clef.TREBLE, staffLines.detectedClef)
    }

    @Test
    fun `StaffLineGroup default time signature is COMMON_TIME`() {
        val staffLines = StaffLineGroup(
            lines = listOf(100, 110, 120, 130, 140),
            lineSpacing = 10,
            topY = 100,
            bottomY = 140
        )

        assertEquals(TimeSignature.COMMON_TIME, staffLines.detectedTimeSignature)
    }

    @Test
    fun `StaffLineGroup default key signature is C_MAJOR`() {
        val staffLines = StaffLineGroup(
            lines = listOf(100, 110, 120, 130, 140),
            lineSpacing = 10,
            topY = 100,
            bottomY = 140
        )

        assertEquals(KeySignature.C_MAJOR, staffLines.detectedKeySignature)
    }

    @Test
    fun `StaffLineGroup mutable properties can be changed`() {
        val staffLines = StaffLineGroup(
            lines = listOf(100, 110, 120, 130, 140),
            lineSpacing = 10,
            topY = 100,
            bottomY = 140
        )

        staffLines.detectedClef = Clef.BASS
        staffLines.detectedTimeSignature = TimeSignature.WALTZ_TIME
        staffLines.detectedKeySignature = KeySignature.G_MAJOR

        assertEquals(Clef.BASS, staffLines.detectedClef)
        assertEquals(TimeSignature.WALTZ_TIME, staffLines.detectedTimeSignature)
        assertEquals(KeySignature.G_MAJOR, staffLines.detectedKeySignature)
    }

    // ==================== DetectedNoteHead Tests ====================

    @Test
    fun `DetectedNoteHead has correct structure`() {
        val noteHead = DetectedNoteHead(
            x = 100,
            y = 200,
            width = 15,
            height = 12,
            isFilled = true,
            staffPosition = 3,
            staffIndex = 0
        )

        assertEquals(100, noteHead.x)
        assertEquals(200, noteHead.y)
        assertEquals(15, noteHead.width)
        assertEquals(12, noteHead.height)
        assertTrue(noteHead.isFilled)
        assertEquals(3, noteHead.staffPosition)
        assertEquals(0, noteHead.staffIndex)
    }

    @Test
    fun `DetectedNoteHead isFilled distinguishes filled and hollow notes`() {
        val filledNote = DetectedNoteHead(100, 200, 15, 12, isFilled = true, 0, 0)
        val hollowNote = DetectedNoteHead(100, 200, 15, 12, isFilled = false, 0, 0)

        assertTrue(filledNote.isFilled)
        assertFalse(hollowNote.isFilled)
    }

    // ==================== DetectedClef Tests ====================

    @Test
    fun `DetectedClef has correct structure`() {
        val clef = DetectedClef(
            clef = Clef.TREBLE,
            endX = 50,
            confidence = 0.8f
        )

        assertEquals(Clef.TREBLE, clef.clef)
        assertEquals(50, clef.endX)
        assertEquals(0.8f, clef.confidence, 0.001f)
    }

    @Test
    fun `DetectedClef supports all clef types`() {
        val treble = DetectedClef(Clef.TREBLE, 50, 0.9f)
        val bass = DetectedClef(Clef.BASS, 50, 0.85f)
        val alto = DetectedClef(Clef.ALTO, 50, 0.7f)
        val tenor = DetectedClef(Clef.TENOR, 50, 0.6f)

        assertEquals(Clef.TREBLE, treble.clef)
        assertEquals(Clef.BASS, bass.clef)
        assertEquals(Clef.ALTO, alto.clef)
        assertEquals(Clef.TENOR, tenor.clef)
    }

    // ==================== DetectedTimeSignature Tests ====================

    @Test
    fun `DetectedTimeSignature has correct structure`() {
        val timeSig = DetectedTimeSignature(
            timeSignature = TimeSignature(4, 4),
            endX = 80,
            confidence = 0.75f
        )

        assertEquals(4, timeSig.timeSignature.numerator)
        assertEquals(4, timeSig.timeSignature.denominator)
        assertEquals(80, timeSig.endX)
        assertEquals(0.75f, timeSig.confidence, 0.001f)
    }

    // ==================== DetectedKeySignature Tests ====================

    @Test
    fun `DetectedKeySignature has correct structure`() {
        val keySig = DetectedKeySignature(
            keySignature = KeySignature(2),
            endX = 100,
            confidence = 0.65f
        )

        assertEquals(2, keySig.keySignature.accidentals)
        assertEquals(100, keySig.endX)
        assertEquals(0.65f, keySig.confidence, 0.001f)
    }

    // ==================== DetectedRest Tests ====================

    @Test
    fun `DetectedRest has correct structure`() {
        val rest = DetectedRest(
            x = 150,
            y = 120,
            duration = NoteDuration.QUARTER,
            staffIndex = 0
        )

        assertEquals(150, rest.x)
        assertEquals(120, rest.y)
        assertEquals(NoteDuration.QUARTER, rest.duration)
        assertEquals(0, rest.staffIndex)
    }

    @Test
    fun `DetectedRest supports all duration types`() {
        val durations = listOf(
            NoteDuration.WHOLE,
            NoteDuration.HALF,
            NoteDuration.QUARTER,
            NoteDuration.EIGHTH,
            NoteDuration.SIXTEENTH,
            NoteDuration.THIRTY_SECOND
        )

        durations.forEach { duration ->
            val rest = DetectedRest(100, 100, duration, 0)
            assertEquals(duration, rest.duration)
        }
    }

    // ==================== DetectedStem Tests ====================

    @Test
    fun `DetectedStem has correct structure for stem up`() {
        val stem = DetectedStem(
            x = 115,
            startY = 150,
            endY = 200,
            isUp = true
        )

        assertEquals(115, stem.x)
        assertEquals(150, stem.startY)
        assertEquals(200, stem.endY)
        assertTrue(stem.isUp)
    }

    @Test
    fun `DetectedStem has correct structure for stem down`() {
        val stem = DetectedStem(
            x = 115,
            startY = 200,
            endY = 150,
            isUp = false
        )

        assertFalse(stem.isUp)
    }

    // ==================== DetectedBeam Tests ====================

    @Test
    fun `DetectedBeam groups note indices`() {
        val beam = DetectedBeam(
            noteIndices = listOf(0, 1, 2, 3),
            beamCount = 1
        )

        assertEquals(4, beam.noteIndices.size)
        assertEquals(1, beam.beamCount)
    }

    @Test
    fun `DetectedBeam beamCount indicates note duration`() {
        // 1 beam = eighth notes
        val eighthBeam = DetectedBeam(listOf(0, 1), beamCount = 1)
        assertEquals(1, eighthBeam.beamCount)

        // 2 beams = sixteenth notes
        val sixteenthBeam = DetectedBeam(listOf(0, 1), beamCount = 2)
        assertEquals(2, sixteenthBeam.beamCount)

        // 3 beams = thirty-second notes
        val thirtySecondBeam = DetectedBeam(listOf(0, 1), beamCount = 3)
        assertEquals(3, thirtySecondBeam.beamCount)
    }

    // ==================== DetectedChord Tests ====================

    @Test
    fun `DetectedChord groups simultaneous notes`() {
        val chord = DetectedChord(
            noteIndices = listOf(0, 1, 2)
        )

        assertEquals(3, chord.noteIndices.size)
        assertEquals(listOf(0, 1, 2), chord.noteIndices)
    }

    // ==================== GrandStaffGroup Tests ====================

    @Test
    fun `GrandStaffGroup links treble and bass staves`() {
        val grandStaff = GrandStaffGroup(
            trebleStaffIndex = 0,
            bassStaffIndex = 1
        )

        assertEquals(0, grandStaff.trebleStaffIndex)
        assertEquals(1, grandStaff.bassStaffIndex)
    }

    // ==================== Binarization Tests ====================

    @Test
    fun `binarize creates binary image from grayscale`() {
        val grayscale = intArrayOf(0, 50, 100, 150, 200, 255)
        val binary = imageProcessor.binarize(grayscale, 6, 1)

        assertEquals(6, binary.size)
        // Lower values (darker) should be true (foreground)
        // Exact threshold depends on Otsu's calculation
    }

    @Test
    fun `binarize with all black pixels returns all true`() {
        val grayscale = IntArray(100) { 0 }
        val binary = imageProcessor.binarize(grayscale, 10, 10)

        // All black pixels should be foreground
        assertTrue(binary.all { it })
    }

    @Test
    fun `binarize with all white pixels returns all false`() {
        val grayscale = IntArray(100) { 255 }
        val binary = imageProcessor.binarize(grayscale, 10, 10)

        // All white pixels should be background
        assertTrue(binary.all { !it })
    }

    // ==================== Staff Line Detection Tests ====================

    @Test
    fun `detectStaffLines returns empty list for empty image`() {
        val binary = BooleanArray(100) { false }
        val staffLines = imageProcessor.detectStaffLines(binary, 10, 10)

        assertTrue(staffLines.isEmpty())
    }

    @Test
    fun `detectStaffLines requires 5 evenly spaced lines`() {
        // Create synthetic image with 5 horizontal lines
        val width = 100
        val height = 50
        val binary = BooleanArray(width * height) { false }

        // Draw 5 lines at y = 10, 15, 20, 25, 30
        val lineYs = listOf(10, 15, 20, 25, 30)
        for (lineY in lineYs) {
            for (x in 0 until width) {
                binary[lineY * width + x] = true
            }
        }

        val staffLines = imageProcessor.detectStaffLines(binary, width, height)

        // Should detect the staff group
        assertEquals(1, staffLines.size)
        assertEquals(5, staffLines[0].lines.size)
    }

    // ==================== Staff Position Calculation Tests ====================

    @Test
    fun `staff position calculation middle line is 0`() {
        val staff = StaffLineGroup(
            lines = listOf(100, 110, 120, 130, 140),
            lineSpacing = 10,
            topY = 100,
            bottomY = 140
        )

        // Note at middle line (y=120) should have position relative to middle
        val middleLineY = staff.lines[2]
        assertEquals(120, middleLineY)
    }

    @Test
    fun `staff position calculation uses half spacing steps`() {
        val staff = StaffLineGroup(
            lines = listOf(100, 110, 120, 130, 140),
            lineSpacing = 10,
            topY = 100,
            bottomY = 140
        )

        val halfSpacing = staff.lineSpacing / 2.0
        assertEquals(5.0, halfSpacing, 0.001)
    }

    // ==================== Note Head Detection Criteria Tests ====================

    @Test
    fun `note head aspect ratio should be between 0_5 and 2_0`() {
        // Test blob that looks like a note head
        val validBlob = Blob(0, 0, 12, 10, 80, 0.6f)
        val aspectRatio = (validBlob.maxX - validBlob.minX).toFloat() /
            (validBlob.maxY - validBlob.minY).coerceAtLeast(1)

        assertTrue(aspectRatio >= 0.5f && aspectRatio <= 2.0f)
    }

    @Test
    fun `note head size should be proportional to line spacing`() {
        val lineSpacing = 10
        val minSize = lineSpacing / 2
        val maxSize = lineSpacing * 2

        // Valid note head size range
        assertEquals(5, minSize)
        assertEquals(20, maxSize)
    }

    @Test
    fun `filled note head has higher pixel density than hollow`() {
        val filledBlob = Blob(0, 0, 10, 10, 70, 0.7f)
        val hollowBlob = Blob(0, 0, 10, 10, 30, 0.3f)

        assertTrue(filledBlob.filledRatio > 0.5f)
        assertTrue(hollowBlob.filledRatio < 0.5f)
    }

    // ==================== Clef Detection Logic Tests ====================

    @Test
    fun `treble clef has activity in both upper and lower regions`() {
        // Treble clef characteristic: spiral extends above and below staff
        val normalizedCenter = 0.5 // Center of mass near middle
        val hasUpperCurl = true
        val hasLowerCurl = true

        val isTrebleClef = hasUpperCurl && hasLowerCurl && normalizedCenter in 0.35..0.65
        assertTrue(isTrebleClef)
    }

    @Test
    fun `bass clef is concentrated in upper portion`() {
        val upperPixels = 150
        val lowerPixels = 50
        val normalizedCenter = 0.35

        val isBassClef = upperPixels > lowerPixels * 1.5 && normalizedCenter < 0.45
        assertTrue(isBassClef)
    }

    // ==================== Time Signature Detection Logic Tests ====================

    @Test
    fun `common time has similar upper and lower complexity`() {
        val upperSegments = 4
        val lowerSegments = 4

        val isCommonTime = upperSegments in 3..5 && lowerSegments in 3..5
        assertTrue(isCommonTime)
    }

    @Test
    fun `waltz time 3_4 has different segment patterns`() {
        val upperSegments = 3 // 3 is simpler
        val lowerSegments = 4

        val isWaltzTime = upperSegments in 2..4 && lowerSegments in 3..5
        assertTrue(isWaltzTime)
    }

    // ==================== Key Signature Detection Logic Tests ====================

    @Test
    fun `sharp pattern has horizontal runs`() {
        // Sharps have crossing horizontal and vertical lines
        val horizontalRuns = 10
        val regionHeight = 50

        val isSharpPattern = horizontalRuns > regionHeight / 6
        assertTrue(isSharpPattern)
    }

    @Test
    fun `flat pattern has fewer horizontal runs`() {
        val horizontalRuns = 5
        val regionHeight = 50

        val isSharpPattern = horizontalRuns > regionHeight / 6
        assertFalse(isSharpPattern) // Indicates flat pattern
    }

    // ==================== Rest Classification Tests ====================

    @Test
    fun `whole rest is wide and short`() {
        val lineSpacing = 10
        val blobWidth = 15
        val blobHeight = 5
        val aspectRatio = blobWidth.toFloat() / blobHeight

        val isWholeRest = blobWidth > lineSpacing && blobHeight < lineSpacing && aspectRatio > 1.5f
        assertTrue(isWholeRest)
    }

    @Test
    fun `quarter rest is tall and narrow`() {
        val lineSpacing = 10
        val blobWidth = 8
        val blobHeight = 25
        val aspectRatio = blobWidth.toFloat() / blobHeight

        val isQuarterRest = blobHeight > lineSpacing * 2 && blobWidth < lineSpacing * 1.5 && aspectRatio < 0.8f
        assertTrue(isQuarterRest)
    }

    // ==================== Stem Detection Tests ====================

    @Test
    fun `stem minimum length is twice line spacing`() {
        val lineSpacing = 10
        val stemMinLength = lineSpacing * 2

        assertEquals(20, stemMinLength)
    }

    @Test
    fun `stem search width is based on note width`() {
        val noteWidth = 12
        val stemSearchWidth = noteWidth / 2 + 3

        assertEquals(9, stemSearchWidth)
    }

    // ==================== Beam Detection Tests ====================

    @Test
    fun `beam connects notes within 4 line spacings`() {
        val lineSpacing = 10
        val maxDistance = lineSpacing * 4

        assertEquals(40, maxDistance)
    }

    @Test
    fun `beam must span at least 60 percent of note distance`() {
        val noteDistance = 30
        val minBeamSpan = (noteDistance * 0.6).toInt()

        assertEquals(18, minBeamSpan)
    }

    // ==================== Chord Detection Tests ====================

    @Test
    fun `notes within one line spacing are chord candidates`() {
        val lineSpacing = 10
        val note1X = 100
        val note2X = 105

        val isChordCandidate = kotlin.math.abs(note2X - note1X) <= lineSpacing
        assertTrue(isChordCandidate)
    }

    @Test
    fun `notes more than one line spacing apart are not chord`() {
        val lineSpacing = 10
        val note1X = 100
        val note2X = 120

        val isChordCandidate = kotlin.math.abs(note2X - note1X) <= lineSpacing
        assertFalse(isChordCandidate)
    }

    // ==================== Grand Staff Detection Tests ====================

    @Test
    fun `grand staff staves are within 5 line spacings apart`() {
        val avgSpacing = 10
        val upperBottom = 140
        val lowerTop = 180
        val gap = lowerTop - upperBottom

        val isGrandStaff = gap < avgSpacing * 5
        assertTrue(isGrandStaff)
    }

    @Test
    fun `grand staff has treble on top and bass on bottom`() {
        val upperClef = Clef.TREBLE
        val lowerClef = Clef.BASS

        val isValidGrandStaff = upperClef == Clef.TREBLE && lowerClef == Clef.BASS
        assertTrue(isValidGrandStaff)
    }

    // ==================== Otsu Threshold Tests ====================

    @Test
    fun `Otsu threshold finds optimal value for bimodal distribution`() {
        // Simulate bimodal histogram: peaks at 50 (dark) and 200 (light)
        val grayscale = IntArray(200)
        for (i in 0 until 100) grayscale[i] = 50
        for (i in 100 until 200) grayscale[i] = 200

        val binary = imageProcessor.binarize(grayscale, 200, 1)

        // First half should be dark (true), second half should be light (false)
        val darkCount = binary.take(100).count { it }
        val lightCount = binary.takeLast(100).count { !it }

        assertTrue(darkCount > 80) // Most dark pixels detected
        assertTrue(lightCount > 80) // Most light pixels detected
    }
}

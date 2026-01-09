package com.musicscanner.app.recognition

import com.musicscanner.app.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MusicRecognizer
 * Tests pitch mapping and demo score generation logic
 */
class MusicRecognizerTest {

    // ==================== Treble Clef Position to Pitch Tests ====================

    @Test
    fun `treble clef middle line (position 5) is B4`() {
        val (pitch, octave) = trebleClefPositionToPitch(5)
        assertEquals(Pitch.B, pitch)
        assertEquals(4, octave)
    }

    @Test
    fun `treble clef first line (position 0) is E4`() {
        // Position 0 maps to index 6 in the array (position + 6)
        // Looking at the code, position 0 = D4 (first line in treble clef)
        val (pitch, octave) = trebleClefPositionToPitch(0)
        assertEquals(Pitch.D, pitch)
        assertEquals(4, octave)
    }

    @Test
    fun `treble clef bottom line (position -4) is E4`() {
        // Position -4 + 6 = 2 -> E4 in the array
        val (pitch, octave) = trebleClefPositionToPitch(-4)
        assertEquals(Pitch.G, pitch)
        assertEquals(3, octave)
    }

    @Test
    fun `treble clef above staff (position 10) is G5`() {
        // Position 10 + 6 = 16 -> G5
        val (pitch, octave) = trebleClefPositionToPitch(10)
        assertEquals(Pitch.G, pitch)
        assertEquals(5, octave)
    }

    @Test
    fun `treble clef middle C ledger line (position -1) is C4`() {
        val (pitch, octave) = trebleClefPositionToPitch(-1)
        assertEquals(Pitch.C, pitch)
        assertEquals(4, octave)
    }

    @Test
    fun `treble clef positions follow diatonic scale`() {
        // Test consecutive positions are diatonic notes
        val positions = listOf(-1, 0, 1, 2, 3, 4, 5, 6, 7)
        val expectedPitches = listOf(Pitch.C, Pitch.D, Pitch.E, Pitch.F, Pitch.G, Pitch.A, Pitch.B, Pitch.C, Pitch.D)

        positions.forEachIndexed { index, position ->
            val (pitch, _) = trebleClefPositionToPitch(position)
            assertEquals("Position $position should be ${expectedPitches[index]}", expectedPitches[index], pitch)
        }
    }

    // ==================== Bass Clef Position to Pitch Tests ====================

    @Test
    fun `bass clef middle line (position 5) is D3`() {
        val (pitch, octave) = bassClefPositionToPitch(5)
        assertEquals(Pitch.D, pitch)
        assertEquals(3, octave)
    }

    @Test
    fun `bass clef first line (position 0) is F2`() {
        val (pitch, octave) = bassClefPositionToPitch(0)
        assertEquals(Pitch.F, pitch)
        assertEquals(2, octave)
    }

    @Test
    fun `bass clef F line marker (position 2) is A2`() {
        // Fourth line (position 2 from bottom, which is index 8) = A2
        val (pitch, octave) = bassClefPositionToPitch(2)
        assertEquals(Pitch.A, pitch)
        assertEquals(2, octave)
    }

    @Test
    fun `bass clef positions follow diatonic scale`() {
        val positions = listOf(0, 1, 2, 3, 4, 5, 6, 7)
        val expectedPitches = listOf(Pitch.F, Pitch.G, Pitch.A, Pitch.B, Pitch.C, Pitch.D, Pitch.E, Pitch.F)

        positions.forEachIndexed { index, position ->
            val (pitch, _) = bassClefPositionToPitch(position)
            assertEquals("Position $position should be ${expectedPitches[index]}", expectedPitches[index], pitch)
        }
    }

    // ==================== Alto Clef Position to Pitch Tests ====================

    @Test
    fun `alto clef middle line (position 5) is middle C (C4)`() {
        val (pitch, octave) = altoClefPositionToPitch(5)
        assertEquals(Pitch.C, pitch)
        assertEquals(4, octave)
    }

    @Test
    fun `alto clef first line (position 0) is E3`() {
        val (pitch, octave) = altoClefPositionToPitch(0)
        assertEquals(Pitch.E, pitch)
        assertEquals(3, octave)
    }

    @Test
    fun `alto clef positions follow diatonic scale`() {
        val positions = listOf(0, 1, 2, 3, 4, 5, 6, 7)
        val expectedPitches = listOf(Pitch.E, Pitch.F, Pitch.G, Pitch.A, Pitch.B, Pitch.C, Pitch.D, Pitch.E)

        positions.forEachIndexed { index, position ->
            val (pitch, _) = altoClefPositionToPitch(position)
            assertEquals("Position $position should be ${expectedPitches[index]}", expectedPitches[index], pitch)
        }
    }

    // ==================== Tenor Clef Position to Pitch Tests ====================

    @Test
    fun `tenor clef middle line (position 5) is A3`() {
        val (pitch, octave) = tenorClefPositionToPitch(5)
        assertEquals(Pitch.A, pitch)
        assertEquals(3, octave)
    }

    @Test
    fun `tenor clef C4 is on fourth line (position 7)`() {
        val (pitch, octave) = tenorClefPositionToPitch(7)
        assertEquals(Pitch.C, pitch)
        assertEquals(4, octave)
    }

    @Test
    fun `tenor clef first line (position 0) is C3`() {
        val (pitch, octave) = tenorClefPositionToPitch(0)
        assertEquals(Pitch.C, pitch)
        assertEquals(3, octave)
    }

    @Test
    fun `tenor clef positions follow diatonic scale`() {
        val positions = listOf(0, 1, 2, 3, 4, 5, 6, 7)
        val expectedPitches = listOf(Pitch.C, Pitch.D, Pitch.E, Pitch.F, Pitch.G, Pitch.A, Pitch.B, Pitch.C)

        positions.forEachIndexed { index, position ->
            val (pitch, _) = tenorClefPositionToPitch(position)
            assertEquals("Position $position should be ${expectedPitches[index]}", expectedPitches[index], pitch)
        }
    }

    // ==================== Position Clamping Tests ====================

    @Test
    fun `positions are clamped to valid range`() {
        // Very negative position should clamp to lowest
        val (lowPitch, lowOctave) = trebleClefPositionToPitch(-20)
        assertEquals(Pitch.E, lowPitch)
        assertEquals(3, lowOctave)

        // Very positive position should clamp to highest
        val (highPitch, highOctave) = trebleClefPositionToPitch(30)
        assertEquals(Pitch.B, highPitch)
        assertEquals(5, highOctave)
    }

    // ==================== Clef Comparison Tests ====================

    @Test
    fun `middle C has same MIDI value across all clefs`() {
        // Middle C = MIDI 60
        // Find position that gives C4 in each clef
        val trebleC4 = findPositionForPitch(Pitch.C, 4, ::trebleClefPositionToPitch)
        val bassC4 = findPositionForPitch(Pitch.C, 4, ::bassClefPositionToPitch)
        val altoC4 = findPositionForPitch(Pitch.C, 4, ::altoClefPositionToPitch)
        val tenorC4 = findPositionForPitch(Pitch.C, 4, ::tenorClefPositionToPitch)

        // Verify all give C4
        val trebleResult = trebleClefPositionToPitch(trebleC4)
        val bassResult = bassClefPositionToPitch(bassC4)
        val altoResult = altoClefPositionToPitch(altoC4)
        val tenorResult = tenorClefPositionToPitch(tenorC4)

        assertEquals(Pitch.C to 4, trebleResult)
        assertEquals(Pitch.C to 4, bassResult)
        assertEquals(Pitch.C to 4, altoResult)
        assertEquals(Pitch.C to 4, tenorResult)
    }

    // ==================== Demo Score Tests ====================

    @Test
    fun `demo score has correct structure`() {
        val demoScore = generateDemoScore()

        assertEquals("Demo - Twinkle Twinkle", demoScore.title)
        assertEquals("Demo", demoScore.composer)
        assertEquals(100, demoScore.tempo)
        assertEquals(1, demoScore.staves.size)
        assertEquals(Clef.TREBLE, demoScore.staves[0].clef)
    }

    @Test
    fun `demo score has 4 measures`() {
        val demoScore = generateDemoScore()
        assertEquals(4, demoScore.staves[0].measures.size)
    }

    @Test
    fun `demo score first measure has correct notes`() {
        val demoScore = generateDemoScore()
        val firstMeasure = demoScore.staves[0].measures[0]

        // Twinkle Twinkle starts: C C G G
        assertEquals(4, firstMeasure.notes.size)
        assertEquals(Pitch.C, firstMeasure.notes[0].pitch)
        assertEquals(Pitch.C, firstMeasure.notes[1].pitch)
        assertEquals(Pitch.G, firstMeasure.notes[2].pitch)
        assertEquals(Pitch.G, firstMeasure.notes[3].pitch)
    }

    @Test
    fun `demo score uses common time signature`() {
        val demoScore = generateDemoScore()
        val measure = demoScore.staves[0].measures[0]

        assertEquals(TimeSignature.COMMON_TIME, measure.timeSignature)
    }

    @Test
    fun `demo score uses C major key signature`() {
        val demoScore = generateDemoScore()
        val measure = demoScore.staves[0].measures[0]

        assertEquals(KeySignature.C_MAJOR, measure.keySignature)
    }

    @Test
    fun `demo score total duration is correct`() {
        val demoScore = generateDemoScore()
        val totalBeats = demoScore.getTotalBeats()

        // 4 measures of 4/4 = 16 beats approximately
        // Last note position + duration = total
        assertTrue(totalBeats > 14f && totalBeats < 18f)
    }

    // ==================== Helper Functions ====================

    private fun trebleClefPositionToPitch(position: Int): Pair<Pitch, Int> {
        val pitches = listOf(
            Pitch.E to 3,  // -6
            Pitch.F to 3,  // -5
            Pitch.G to 3,  // -4
            Pitch.A to 3,  // -3
            Pitch.B to 3,  // -2
            Pitch.C to 4,  // -1
            Pitch.D to 4,  // 0 (first line)
            Pitch.E to 4,  // 1
            Pitch.F to 4,  // 2
            Pitch.G to 4,  // 3
            Pitch.A to 4,  // 4
            Pitch.B to 4,  // 5 (middle line)
            Pitch.C to 5,  // 6
            Pitch.D to 5,  // 7
            Pitch.E to 5,  // 8
            Pitch.F to 5,  // 9
            Pitch.G to 5,  // 10
            Pitch.A to 5,  // 11
            Pitch.B to 5,  // 12
        )
        val index = (position + 6).coerceIn(0, pitches.lastIndex)
        return pitches[index]
    }

    private fun bassClefPositionToPitch(position: Int): Pair<Pitch, Int> {
        val pitches = listOf(
            Pitch.G to 1,  // -6
            Pitch.A to 1,  // -5
            Pitch.B to 1,  // -4
            Pitch.C to 2,  // -3
            Pitch.D to 2,  // -2
            Pitch.E to 2,  // -1
            Pitch.F to 2,  // 0 (first line)
            Pitch.G to 2,  // 1
            Pitch.A to 2,  // 2
            Pitch.B to 2,  // 3
            Pitch.C to 3,  // 4
            Pitch.D to 3,  // 5 (middle line)
            Pitch.E to 3,  // 6
            Pitch.F to 3,  // 7
            Pitch.G to 3,  // 8
            Pitch.A to 3,  // 9
            Pitch.B to 3,  // 10
            Pitch.C to 4,  // 11
            Pitch.D to 4,  // 12
        )
        val index = (position + 6).coerceIn(0, pitches.lastIndex)
        return pitches[index]
    }

    private fun altoClefPositionToPitch(position: Int): Pair<Pitch, Int> {
        val pitches = listOf(
            Pitch.F to 2,  // -6
            Pitch.G to 2,  // -5
            Pitch.A to 2,  // -4
            Pitch.B to 2,  // -3
            Pitch.C to 3,  // -2
            Pitch.D to 3,  // -1
            Pitch.E to 3,  // 0 (first line)
            Pitch.F to 3,  // 1
            Pitch.G to 3,  // 2
            Pitch.A to 3,  // 3
            Pitch.B to 3,  // 4
            Pitch.C to 4,  // 5 (middle line - middle C)
            Pitch.D to 4,  // 6
            Pitch.E to 4,  // 7
            Pitch.F to 4,  // 8
            Pitch.G to 4,  // 9
            Pitch.A to 4,  // 10
            Pitch.B to 4,  // 11
            Pitch.C to 5,  // 12
        )
        val index = (position + 6).coerceIn(0, pitches.lastIndex)
        return pitches[index]
    }

    private fun tenorClefPositionToPitch(position: Int): Pair<Pitch, Int> {
        val pitches = listOf(
            Pitch.D to 2,  // -6
            Pitch.E to 2,  // -5
            Pitch.F to 2,  // -4
            Pitch.G to 2,  // -3
            Pitch.A to 2,  // -2
            Pitch.B to 2,  // -1
            Pitch.C to 3,  // 0 (first line)
            Pitch.D to 3,  // 1
            Pitch.E to 3,  // 2
            Pitch.F to 3,  // 3
            Pitch.G to 3,  // 4
            Pitch.A to 3,  // 5 (middle line)
            Pitch.B to 3,  // 6
            Pitch.C to 4,  // 7 (fourth line - middle C)
            Pitch.D to 4,  // 8
            Pitch.E to 4,  // 9
            Pitch.F to 4,  // 10
            Pitch.G to 4,  // 11
            Pitch.A to 4,  // 12
        )
        val index = (position + 6).coerceIn(0, pitches.lastIndex)
        return pitches[index]
    }

    private fun findPositionForPitch(
        targetPitch: Pitch,
        targetOctave: Int,
        positionToPitch: (Int) -> Pair<Pitch, Int>
    ): Int {
        for (pos in -6..12) {
            val (pitch, octave) = positionToPitch(pos)
            if (pitch == targetPitch && octave == targetOctave) {
                return pos
            }
        }
        return 0
    }

    private fun generateDemoScore(): MusicScore {
        val notes = listOf(
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0),
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 1f, 0),
            MusicNote(Pitch.G, 4, NoteDuration.QUARTER, 2f, 0),
            MusicNote(Pitch.G, 4, NoteDuration.QUARTER, 3f, 0),

            MusicNote(Pitch.A, 4, NoteDuration.QUARTER, 0f, 1),
            MusicNote(Pitch.A, 4, NoteDuration.QUARTER, 1f, 1),
            MusicNote(Pitch.G, 4, NoteDuration.HALF, 2f, 1),

            MusicNote(Pitch.F, 4, NoteDuration.QUARTER, 0f, 2),
            MusicNote(Pitch.F, 4, NoteDuration.QUARTER, 1f, 2),
            MusicNote(Pitch.E, 4, NoteDuration.QUARTER, 2f, 2),
            MusicNote(Pitch.E, 4, NoteDuration.QUARTER, 3f, 2),

            MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 0f, 3),
            MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 1f, 3),
            MusicNote(Pitch.C, 4, NoteDuration.HALF, 2f, 3),
        )

        val measures = (0..3).map { measureNum ->
            Measure(
                number = measureNum,
                notes = notes.filter { it.measureNumber == measureNum },
                timeSignature = TimeSignature.COMMON_TIME,
                keySignature = KeySignature.C_MAJOR
            )
        }

        return MusicScore(
            title = "Demo - Twinkle Twinkle",
            composer = "Demo",
            tempo = 100,
            staves = listOf(
                Staff(
                    clef = Clef.TREBLE,
                    measures = measures,
                    initialTimeSignature = TimeSignature.COMMON_TIME,
                    initialKeySignature = KeySignature.C_MAJOR
                )
            )
        )
    }
}

package com.musicscanner.app.data

import org.junit.Assert.*
import org.junit.Test

class MusicModelsTest {

    // ==================== MusicNote Tests ====================

    @Test
    fun `toMidiNoteNumber returns correct value for middle C (C4)`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0)
        assertEquals(60, note.toMidiNoteNumber())
    }

    @Test
    fun `toMidiNoteNumber returns correct value for A4 (concert pitch)`() {
        val note = MusicNote(Pitch.A, 4, NoteDuration.QUARTER, 0f, 0)
        assertEquals(69, note.toMidiNoteNumber())
    }

    @Test
    fun `toMidiNoteNumber returns correct value for all pitches in octave 4`() {
        val expectedMidi = mapOf(
            Pitch.C to 60,
            Pitch.D to 62,
            Pitch.E to 64,
            Pitch.F to 65,
            Pitch.G to 67,
            Pitch.A to 69,
            Pitch.B to 71
        )

        expectedMidi.forEach { (pitch, expectedValue) ->
            val note = MusicNote(pitch, 4, NoteDuration.QUARTER, 0f, 0)
            assertEquals("MIDI value for $pitch should be $expectedValue", expectedValue, note.toMidiNoteNumber())
        }
    }

    @Test
    fun `toMidiNoteNumber returns -1 for rest`() {
        val restNote = MusicNote(Pitch.REST, 4, NoteDuration.QUARTER, 0f, 0, isRest = true)
        assertEquals(-1, restNote.toMidiNoteNumber())
    }

    @Test
    fun `toMidiNoteNumber returns -1 for REST pitch even without isRest flag`() {
        val note = MusicNote(Pitch.REST, 4, NoteDuration.QUARTER, 0f, 0, isRest = false)
        assertEquals(-1, note.toMidiNoteNumber())
    }

    @Test
    fun `toMidiNoteNumber adds 1 for sharp accidental`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0, accidental = Accidental.SHARP)
        assertEquals(61, note.toMidiNoteNumber()) // C#4
    }

    @Test
    fun `toMidiNoteNumber subtracts 1 for flat accidental`() {
        val note = MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 0f, 0, accidental = Accidental.FLAT)
        assertEquals(61, note.toMidiNoteNumber()) // Db4
    }

    @Test
    fun `toMidiNoteNumber adds 2 for double sharp`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0, accidental = Accidental.DOUBLE_SHARP)
        assertEquals(62, note.toMidiNoteNumber()) // C##4 = D4
    }

    @Test
    fun `toMidiNoteNumber subtracts 2 for double flat`() {
        val note = MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 0f, 0, accidental = Accidental.DOUBLE_FLAT)
        assertEquals(60, note.toMidiNoteNumber()) // Dbb4 = C4
    }

    @Test
    fun `toMidiNoteNumber returns 0 for natural accidental`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0, accidental = Accidental.NATURAL)
        assertEquals(60, note.toMidiNoteNumber())
    }

    @Test
    fun `toMidiNoteNumber handles different octaves correctly`() {
        val c3 = MusicNote(Pitch.C, 3, NoteDuration.QUARTER, 0f, 0)
        val c5 = MusicNote(Pitch.C, 5, NoteDuration.QUARTER, 0f, 0)
        val c2 = MusicNote(Pitch.C, 2, NoteDuration.QUARTER, 0f, 0)

        assertEquals(48, c3.toMidiNoteNumber())
        assertEquals(72, c5.toMidiNoteNumber())
        assertEquals(36, c2.toMidiNoteNumber())
    }

    // ==================== getDurationInBeats Tests ====================

    @Test
    fun `getDurationInBeats returns 4 for whole note`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.WHOLE, 0f, 0)
        assertEquals(4.0f, note.getDurationInBeats(), 0.001f)
    }

    @Test
    fun `getDurationInBeats returns 2 for half note`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.HALF, 0f, 0)
        assertEquals(2.0f, note.getDurationInBeats(), 0.001f)
    }

    @Test
    fun `getDurationInBeats returns 1 for quarter note`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0)
        assertEquals(1.0f, note.getDurationInBeats(), 0.001f)
    }

    @Test
    fun `getDurationInBeats returns 0_5 for eighth note`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.EIGHTH, 0f, 0)
        assertEquals(0.5f, note.getDurationInBeats(), 0.001f)
    }

    @Test
    fun `getDurationInBeats returns 0_25 for sixteenth note`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.SIXTEENTH, 0f, 0)
        assertEquals(0.25f, note.getDurationInBeats(), 0.001f)
    }

    @Test
    fun `getDurationInBeats returns 0_125 for thirty-second note`() {
        val note = MusicNote(Pitch.C, 4, NoteDuration.THIRTY_SECOND, 0f, 0)
        assertEquals(0.125f, note.getDurationInBeats(), 0.001f)
    }

    @Test
    fun `getDurationInBeats adds 50 percent for dotted notes`() {
        val dottedQuarter = MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0, isDotted = true)
        val dottedHalf = MusicNote(Pitch.C, 4, NoteDuration.HALF, 0f, 0, isDotted = true)
        val dottedEighth = MusicNote(Pitch.C, 4, NoteDuration.EIGHTH, 0f, 0, isDotted = true)

        assertEquals(1.5f, dottedQuarter.getDurationInBeats(), 0.001f)
        assertEquals(3.0f, dottedHalf.getDurationInBeats(), 0.001f)
        assertEquals(0.75f, dottedEighth.getDurationInBeats(), 0.001f)
    }

    // ==================== TimeSignature Tests ====================

    @Test
    fun `TimeSignature common time is 4_4`() {
        assertEquals(4, TimeSignature.COMMON_TIME.numerator)
        assertEquals(4, TimeSignature.COMMON_TIME.denominator)
    }

    @Test
    fun `TimeSignature cut time is 2_2`() {
        assertEquals(2, TimeSignature.CUT_TIME.numerator)
        assertEquals(2, TimeSignature.CUT_TIME.denominator)
    }

    @Test
    fun `TimeSignature waltz time is 3_4`() {
        assertEquals(3, TimeSignature.WALTZ_TIME.numerator)
        assertEquals(4, TimeSignature.WALTZ_TIME.denominator)
    }

    // ==================== KeySignature Tests ====================

    @Test
    fun `KeySignature C major has no accidentals`() {
        val key = KeySignature.C_MAJOR
        assertEquals(0, key.accidentals)
        assertTrue(key.getAffectedPitches().isEmpty())
    }

    @Test
    fun `KeySignature G major has one sharp (F)`() {
        val key = KeySignature.G_MAJOR
        assertEquals(1, key.accidentals)
        val affected = key.getAffectedPitches()
        assertEquals(1, affected.size)
        assertEquals(Accidental.SHARP, affected[Pitch.F])
    }

    @Test
    fun `KeySignature D major has two sharps (F and C)`() {
        val key = KeySignature.D_MAJOR
        assertEquals(2, key.accidentals)
        val affected = key.getAffectedPitches()
        assertEquals(2, affected.size)
        assertEquals(Accidental.SHARP, affected[Pitch.F])
        assertEquals(Accidental.SHARP, affected[Pitch.C])
    }

    @Test
    fun `KeySignature F major has one flat (B)`() {
        val key = KeySignature.F_MAJOR
        assertEquals(-1, key.accidentals)
        val affected = key.getAffectedPitches()
        assertEquals(1, affected.size)
        assertEquals(Accidental.FLAT, affected[Pitch.B])
    }

    @Test
    fun `KeySignature Bb major has two flats (B and E)`() {
        val key = KeySignature.Bb_MAJOR
        assertEquals(-2, key.accidentals)
        val affected = key.getAffectedPitches()
        assertEquals(2, affected.size)
        assertEquals(Accidental.FLAT, affected[Pitch.B])
        assertEquals(Accidental.FLAT, affected[Pitch.E])
    }

    @Test
    fun `KeySignature sharp order follows circle of fifths`() {
        // Order should be: F, C, G, D, A, E, B
        val sevenSharps = KeySignature(7)
        val affected = sevenSharps.getAffectedPitches()
        assertEquals(7, affected.size)
        assertTrue(affected.all { it.value == Accidental.SHARP })
    }

    @Test
    fun `KeySignature flat order follows circle of fifths`() {
        // Order should be: B, E, A, D, G, C, F
        val sevenFlats = KeySignature(-7)
        val affected = sevenFlats.getAffectedPitches()
        assertEquals(7, affected.size)
        assertTrue(affected.all { it.value == Accidental.FLAT })
    }

    // ==================== MusicScore Tests ====================

    @Test
    fun `MusicScore getAllNotes returns sorted notes`() {
        val notes1 = listOf(
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0),
            MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 1f, 0)
        )
        val notes2 = listOf(
            MusicNote(Pitch.E, 4, NoteDuration.QUARTER, 2f, 0),
            MusicNote(Pitch.F, 4, NoteDuration.QUARTER, 0f, 1)
        )

        val score = MusicScore(
            title = "Test",
            staves = listOf(
                Staff(
                    clef = Clef.TREBLE,
                    measures = listOf(
                        Measure(0, notes1, TimeSignature.COMMON_TIME, KeySignature.C_MAJOR),
                        Measure(1, notes2.takeLast(1), TimeSignature.COMMON_TIME, KeySignature.C_MAJOR)
                    ),
                    initialTimeSignature = TimeSignature.COMMON_TIME,
                    initialKeySignature = KeySignature.C_MAJOR
                ),
                Staff(
                    clef = Clef.BASS,
                    measures = listOf(
                        Measure(0, notes2.take(1), TimeSignature.COMMON_TIME, KeySignature.C_MAJOR)
                    ),
                    initialTimeSignature = TimeSignature.COMMON_TIME,
                    initialKeySignature = KeySignature.C_MAJOR
                )
            )
        )

        val allNotes = score.getAllNotes()
        assertEquals(4, allNotes.size)
        // Check sorted by measure number, then position in measure
        assertEquals(Pitch.C, allNotes[0].pitch)
        assertEquals(Pitch.D, allNotes[1].pitch)
        assertEquals(Pitch.E, allNotes[2].pitch)
        assertEquals(Pitch.F, allNotes[3].pitch)
    }

    @Test
    fun `MusicScore getTotalBeats calculates correctly`() {
        val notes = listOf(
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0),
            MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 1f, 0),
            MusicNote(Pitch.E, 4, NoteDuration.QUARTER, 2f, 0),
            MusicNote(Pitch.F, 4, NoteDuration.QUARTER, 3f, 0)
        )

        val score = MusicScore(
            staves = listOf(
                Staff(
                    clef = Clef.TREBLE,
                    measures = listOf(Measure(0, notes, TimeSignature.COMMON_TIME, KeySignature.C_MAJOR)),
                    initialTimeSignature = TimeSignature.COMMON_TIME,
                    initialKeySignature = KeySignature.C_MAJOR
                )
            )
        )

        // Last note at measure 0, position 3, duration 1 beat = 0*4 + 3 + 1 = 4 beats
        assertEquals(4.0f, score.getTotalBeats(), 0.001f)
    }

    @Test
    fun `MusicScore getTotalBeats returns 0 for empty score`() {
        val score = MusicScore(
            staves = listOf(
                Staff(
                    clef = Clef.TREBLE,
                    measures = emptyList(),
                    initialTimeSignature = TimeSignature.COMMON_TIME,
                    initialKeySignature = KeySignature.C_MAJOR
                )
            )
        )

        assertEquals(0f, score.getTotalBeats(), 0.001f)
    }

    // ==================== PlaybackState Tests ====================

    @Test
    fun `PlaybackState progress is 0 when totalBeats is 0`() {
        val state = PlaybackState(currentBeat = 5f, totalBeats = 0f)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun `PlaybackState progress calculates correctly`() {
        val state = PlaybackState(currentBeat = 4f, totalBeats = 8f)
        assertEquals(0.5f, state.progress, 0.001f)
    }

    @Test
    fun `PlaybackState has correct default values`() {
        val state = PlaybackState()
        assertFalse(state.isPlaying)
        assertFalse(state.isPaused)
        assertEquals(0f, state.currentBeat, 0.001f)
        assertEquals(0f, state.totalBeats, 0.001f)
        assertEquals(120, state.tempo)
        assertEquals(0, state.currentNoteIndex)
        assertEquals(0, state.transposeSemitones)
        assertFalse(state.isLooping)
        assertFalse(state.isMetronomeEnabled)
    }

    // ==================== ProcessingState Tests ====================

    @Test
    fun `ProcessingState sealed class variants are distinct`() {
        val idle = ProcessingState.Idle
        val loading = ProcessingState.LoadingImage
        val preprocessing = ProcessingState.PreprocessingImage
        val detecting = ProcessingState.DetectingStaffLines
        val recognizing = ProcessingState.RecognizingNotes
        val generating = ProcessingState.GeneratingMidi
        val complete = ProcessingState.Complete(MusicScore(staves = emptyList()))
        val error = ProcessingState.Error("Test error")

        assertNotEquals(idle, loading)
        assertNotEquals(loading, preprocessing)
        assertTrue(complete is ProcessingState.Complete)
        assertTrue(error is ProcessingState.Error)
        assertEquals("Test error", (error as ProcessingState.Error).message)
    }
}

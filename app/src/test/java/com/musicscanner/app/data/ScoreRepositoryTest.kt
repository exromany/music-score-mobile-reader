package com.musicscanner.app.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ScoreRepository serialization helpers
 * Tests the serializable data classes and conversion functions
 */
class ScoreRepositoryTest {

    // ==================== SerializableNote Tests ====================

    @Test
    fun `SerializableNote converts to MusicNote correctly`() {
        val serializable = SerializableNote(
            pitch = "C",
            octave = 4,
            duration = "QUARTER",
            positionInMeasure = 0f,
            measureNumber = 0,
            isRest = false,
            accidental = "NONE",
            isDotted = false,
            tieStart = false,
            tieEnd = false
        )

        val musicNote = serializable.toMusicNote()

        assertEquals(Pitch.C, musicNote.pitch)
        assertEquals(4, musicNote.octave)
        assertEquals(NoteDuration.QUARTER, musicNote.duration)
        assertEquals(0f, musicNote.positionInMeasure, 0.001f)
        assertEquals(0, musicNote.measureNumber)
        assertFalse(musicNote.isRest)
        assertEquals(Accidental.NONE, musicNote.accidental)
        assertFalse(musicNote.isDotted)
        assertFalse(musicNote.tieStart)
        assertFalse(musicNote.tieEnd)
    }

    @Test
    fun `SerializableNote converts rest correctly`() {
        val serializable = SerializableNote(
            pitch = "REST",
            octave = 4,
            duration = "WHOLE",
            positionInMeasure = 0f,
            measureNumber = 0,
            isRest = true,
            accidental = "NONE",
            isDotted = false,
            tieStart = false,
            tieEnd = false
        )

        val musicNote = serializable.toMusicNote()

        assertEquals(Pitch.REST, musicNote.pitch)
        assertTrue(musicNote.isRest)
    }

    @Test
    fun `SerializableNote converts accidentals correctly`() {
        val sharpNote = SerializableNote(
            pitch = "F",
            octave = 4,
            duration = "QUARTER",
            positionInMeasure = 0f,
            measureNumber = 0,
            isRest = false,
            accidental = "SHARP",
            isDotted = false,
            tieStart = false,
            tieEnd = false
        )

        val flatNote = SerializableNote(
            pitch = "B",
            octave = 4,
            duration = "QUARTER",
            positionInMeasure = 1f,
            measureNumber = 0,
            isRest = false,
            accidental = "FLAT",
            isDotted = false,
            tieStart = false,
            tieEnd = false
        )

        assertEquals(Accidental.SHARP, sharpNote.toMusicNote().accidental)
        assertEquals(Accidental.FLAT, flatNote.toMusicNote().accidental)
    }

    @Test
    fun `SerializableNote converts dotted notes correctly`() {
        val dottedNote = SerializableNote(
            pitch = "C",
            octave = 4,
            duration = "HALF",
            positionInMeasure = 0f,
            measureNumber = 0,
            isRest = false,
            accidental = "NONE",
            isDotted = true,
            tieStart = false,
            tieEnd = false
        )

        val musicNote = dottedNote.toMusicNote()
        assertTrue(musicNote.isDotted)
        assertEquals(3.0f, musicNote.getDurationInBeats(), 0.001f) // Half note (2) * 1.5 = 3
    }

    @Test
    fun `SerializableNote converts ties correctly`() {
        val tiedNote = SerializableNote(
            pitch = "C",
            octave = 4,
            duration = "QUARTER",
            positionInMeasure = 0f,
            measureNumber = 0,
            isRest = false,
            accidental = "NONE",
            isDotted = false,
            tieStart = true,
            tieEnd = false
        )

        val musicNote = tiedNote.toMusicNote()
        assertTrue(musicNote.tieStart)
        assertFalse(musicNote.tieEnd)
    }

    // ==================== MusicNote to SerializableNote Tests ====================

    @Test
    fun `MusicNote converts to SerializableNote correctly`() {
        val musicNote = MusicNote(
            pitch = Pitch.G,
            octave = 5,
            duration = NoteDuration.EIGHTH,
            positionInMeasure = 2.5f,
            measureNumber = 3,
            isRest = false,
            accidental = Accidental.SHARP,
            isDotted = true,
            tieStart = true,
            tieEnd = false
        )

        val serializable = musicNote.toSerializable()

        assertEquals("G", serializable.pitch)
        assertEquals(5, serializable.octave)
        assertEquals("EIGHTH", serializable.duration)
        assertEquals(2.5f, serializable.positionInMeasure, 0.001f)
        assertEquals(3, serializable.measureNumber)
        assertFalse(serializable.isRest)
        assertEquals("SHARP", serializable.accidental)
        assertTrue(serializable.isDotted)
        assertTrue(serializable.tieStart)
        assertFalse(serializable.tieEnd)
    }

    @Test
    fun `round trip conversion preserves all note properties`() {
        val original = MusicNote(
            pitch = Pitch.A,
            octave = 3,
            duration = NoteDuration.SIXTEENTH,
            positionInMeasure = 1.25f,
            measureNumber = 2,
            isRest = false,
            accidental = Accidental.DOUBLE_FLAT,
            isDotted = false,
            tieStart = false,
            tieEnd = true
        )

        val serializable = original.toSerializable()
        val restored = serializable.toMusicNote()

        assertEquals(original.pitch, restored.pitch)
        assertEquals(original.octave, restored.octave)
        assertEquals(original.duration, restored.duration)
        assertEquals(original.positionInMeasure, restored.positionInMeasure, 0.001f)
        assertEquals(original.measureNumber, restored.measureNumber)
        assertEquals(original.isRest, restored.isRest)
        assertEquals(original.accidental, restored.accidental)
        assertEquals(original.isDotted, restored.isDotted)
        assertEquals(original.tieStart, restored.tieStart)
        assertEquals(original.tieEnd, restored.tieEnd)
    }

    // ==================== SerializableMeasure Tests ====================

    @Test
    fun `SerializableMeasure converts to Measure correctly`() {
        val notes = listOf(
            SerializableNote("C", 4, "QUARTER", 0f, 0, false, "NONE", false, false, false),
            SerializableNote("D", 4, "QUARTER", 1f, 0, false, "NONE", false, false, false)
        )

        val serializable = SerializableMeasure(
            number = 0,
            notes = notes,
            timeSignatureNum = 4,
            timeSignatureDen = 4,
            keyAccidentals = 0
        )

        val measure = serializable.toMeasure()

        assertEquals(0, measure.number)
        assertEquals(2, measure.notes.size)
        assertEquals(4, measure.timeSignature.numerator)
        assertEquals(4, measure.timeSignature.denominator)
        assertEquals(0, measure.keySignature.accidentals)
    }

    @Test
    fun `Measure converts to SerializableMeasure correctly`() {
        val notes = listOf(
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0),
            MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 1f, 0)
        )

        val measure = Measure(
            number = 1,
            notes = notes,
            timeSignature = TimeSignature(3, 4),
            keySignature = KeySignature(2)
        )

        val serializable = measure.toSerializable()

        assertEquals(1, serializable.number)
        assertEquals(2, serializable.notes.size)
        assertEquals(3, serializable.timeSignatureNum)
        assertEquals(4, serializable.timeSignatureDen)
        assertEquals(2, serializable.keyAccidentals)
    }

    // ==================== SerializableStaff Tests ====================

    @Test
    fun `SerializableStaff converts to Staff correctly`() {
        val measure = SerializableMeasure(
            number = 0,
            notes = listOf(
                SerializableNote("C", 4, "QUARTER", 0f, 0, false, "NONE", false, false, false)
            ),
            timeSignatureNum = 4,
            timeSignatureDen = 4,
            keyAccidentals = 0
        )

        val serializable = SerializableStaff(
            clef = "TREBLE",
            measures = listOf(measure),
            timeSignatureNum = 4,
            timeSignatureDen = 4,
            keyAccidentals = 0
        )

        val staff = serializable.toStaff()

        assertEquals(Clef.TREBLE, staff.clef)
        assertEquals(1, staff.measures.size)
        assertEquals(4, staff.initialTimeSignature.numerator)
        assertEquals(4, staff.initialTimeSignature.denominator)
        assertEquals(0, staff.initialKeySignature.accidentals)
    }

    @Test
    fun `Staff converts to SerializableStaff correctly`() {
        val notes = listOf(
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0)
        )

        val staff = Staff(
            clef = Clef.BASS,
            measures = listOf(
                Measure(0, notes, TimeSignature(6, 8), KeySignature(-2))
            ),
            initialTimeSignature = TimeSignature(6, 8),
            initialKeySignature = KeySignature(-2)
        )

        val serializable = staff.toSerializable()

        assertEquals("BASS", serializable.clef)
        assertEquals(1, serializable.measures.size)
        assertEquals(6, serializable.timeSignatureNum)
        assertEquals(8, serializable.timeSignatureDen)
        assertEquals(-2, serializable.keyAccidentals)
    }

    @Test
    fun `all clef types serialize correctly`() {
        val clefs = listOf(Clef.TREBLE, Clef.BASS, Clef.ALTO, Clef.TENOR)

        clefs.forEach { clef ->
            val staff = Staff(
                clef = clef,
                measures = emptyList(),
                initialTimeSignature = TimeSignature.COMMON_TIME,
                initialKeySignature = KeySignature.C_MAJOR
            )

            val serializable = staff.toSerializable()
            val restored = serializable.toStaff()

            assertEquals(clef, restored.clef)
        }
    }

    // ==================== SerializableMusicScore Tests ====================

    @Test
    fun `SerializableMusicScore converts to MusicScore correctly`() {
        val notes = listOf(
            SerializableNote("C", 4, "QUARTER", 0f, 0, false, "NONE", false, false, false)
        )

        val measure = SerializableMeasure(0, notes, 4, 4, 0)
        val staff = SerializableStaff("TREBLE", listOf(measure), 4, 4, 0)

        val serializable = SerializableMusicScore(
            title = "Test Score",
            composer = "Test Composer",
            tempo = 100,
            staves = listOf(staff)
        )

        val score = serializable.toMusicScore()

        assertEquals("Test Score", score.title)
        assertEquals("Test Composer", score.composer)
        assertEquals(100, score.tempo)
        assertEquals(1, score.staves.size)
    }

    @Test
    fun `MusicScore converts to SerializableMusicScore correctly`() {
        val notes = listOf(
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0)
        )

        val score = MusicScore(
            title = "My Score",
            composer = "Me",
            tempo = 140,
            staves = listOf(
                Staff(
                    clef = Clef.TREBLE,
                    measures = listOf(
                        Measure(0, notes, TimeSignature.COMMON_TIME, KeySignature.C_MAJOR)
                    ),
                    initialTimeSignature = TimeSignature.COMMON_TIME,
                    initialKeySignature = KeySignature.C_MAJOR
                )
            )
        )

        val serializable = score.toSerializable()

        assertEquals("My Score", serializable.title)
        assertEquals("Me", serializable.composer)
        assertEquals(140, serializable.tempo)
        assertEquals(1, serializable.staves.size)
    }

    @Test
    fun `complete score round trip preserves all data`() {
        val notes = listOf(
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0, accidental = Accidental.SHARP),
            MusicNote(Pitch.D, 4, NoteDuration.HALF, 1f, 0, isDotted = true),
            MusicNote(Pitch.REST, 4, NoteDuration.QUARTER, 3f, 0, isRest = true),
            MusicNote(Pitch.E, 4, NoteDuration.EIGHTH, 0f, 1)
        )

        val original = MusicScore(
            title = "Complete Test",
            composer = "Tester",
            tempo = 88,
            staves = listOf(
                Staff(
                    clef = Clef.TREBLE,
                    measures = listOf(
                        Measure(0, notes.take(3), TimeSignature(4, 4), KeySignature(1)),
                        Measure(1, notes.takeLast(1), TimeSignature(4, 4), KeySignature(1))
                    ),
                    initialTimeSignature = TimeSignature(4, 4),
                    initialKeySignature = KeySignature(1)
                ),
                Staff(
                    clef = Clef.BASS,
                    measures = listOf(
                        Measure(0, listOf(MusicNote(Pitch.C, 3, NoteDuration.WHOLE, 0f, 0)),
                            TimeSignature(4, 4), KeySignature(1))
                    ),
                    initialTimeSignature = TimeSignature(4, 4),
                    initialKeySignature = KeySignature(1)
                )
            )
        )

        val serializable = original.toSerializable()
        val restored = serializable.toMusicScore()

        assertEquals(original.title, restored.title)
        assertEquals(original.composer, restored.composer)
        assertEquals(original.tempo, restored.tempo)
        assertEquals(original.staves.size, restored.staves.size)

        // Verify first staff
        val originalStaff = original.staves[0]
        val restoredStaff = restored.staves[0]
        assertEquals(originalStaff.clef, restoredStaff.clef)
        assertEquals(originalStaff.measures.size, restoredStaff.measures.size)

        // Verify notes
        val originalNotes = original.getAllNotes()
        val restoredNotes = restored.getAllNotes()
        assertEquals(originalNotes.size, restoredNotes.size)
    }

    // ==================== ScoreHistoryEntry Tests ====================

    @Test
    fun `ScoreHistoryEntry has correct structure`() {
        val entry = ScoreHistoryEntry(
            id = "12345",
            title = "Test Score",
            composer = "Test Composer",
            tempo = 120,
            noteCount = 50,
            timestamp = System.currentTimeMillis(),
            imagePath = "/path/to/image.jpg",
            scoreData = "{}"
        )

        assertEquals("12345", entry.id)
        assertEquals("Test Score", entry.title)
        assertEquals("Test Composer", entry.composer)
        assertEquals(120, entry.tempo)
        assertEquals(50, entry.noteCount)
        assertNotNull(entry.timestamp)
        assertEquals("/path/to/image.jpg", entry.imagePath)
        assertEquals("{}", entry.scoreData)
    }

    @Test
    fun `ScoreHistoryEntry allows null imagePath`() {
        val entry = ScoreHistoryEntry(
            id = "12345",
            title = "Test",
            composer = "",
            tempo = 120,
            noteCount = 10,
            timestamp = 0L,
            imagePath = null,
            scoreData = "{}"
        )

        assertNull(entry.imagePath)
    }

    // ==================== Enum Serialization Tests ====================

    @Test
    fun `all Pitch values can be serialized and deserialized`() {
        Pitch.values().forEach { pitch ->
            val note = MusicNote(pitch, 4, NoteDuration.QUARTER, 0f, 0, isRest = pitch == Pitch.REST)
            val serializable = note.toSerializable()
            val restored = serializable.toMusicNote()
            assertEquals(pitch, restored.pitch)
        }
    }

    @Test
    fun `all NoteDuration values can be serialized and deserialized`() {
        NoteDuration.values().forEach { duration ->
            val note = MusicNote(Pitch.C, 4, duration, 0f, 0)
            val serializable = note.toSerializable()
            val restored = serializable.toMusicNote()
            assertEquals(duration, restored.duration)
        }
    }

    @Test
    fun `all Accidental values can be serialized and deserialized`() {
        Accidental.values().forEach { accidental ->
            val note = MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0, accidental = accidental)
            val serializable = note.toSerializable()
            val restored = serializable.toMusicNote()
            assertEquals(accidental, restored.accidental)
        }
    }
}

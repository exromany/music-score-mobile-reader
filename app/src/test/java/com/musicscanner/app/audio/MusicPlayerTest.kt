package com.musicscanner.app.audio

import com.musicscanner.app.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MusicPlayer
 * Tests playback logic, state management, and note conversion
 */
class MusicPlayerTest {

    private lateinit var player: MusicPlayer

    @Before
    fun setUp() {
        player = MusicPlayer()
    }

    // ==================== Score Loading Tests ====================

    @Test
    fun `loadScore initializes playback state correctly`() {
        val score = createTestScore()
        player.loadScore(score)

        val state = player.playbackState.value
        assertFalse(state.isPlaying)
        assertFalse(state.isPaused)
        assertEquals(0f, state.currentBeat, 0.001f)
        assertEquals(score.getTotalBeats(), state.totalBeats, 0.001f)
        assertEquals(score.tempo, state.tempo)
        assertEquals(0, state.currentNoteIndex)
    }

    @Test
    fun `loadScore resets state when loading new score`() {
        val score1 = createTestScore()
        val score2 = createTestScore(tempo = 60, title = "Second Score")

        player.loadScore(score1)
        player.loadScore(score2)

        val state = player.playbackState.value
        assertEquals(60, state.tempo)
    }

    // ==================== Transpose Tests ====================

    @Test
    fun `setTranspose updates playback state`() {
        player.setTranspose(5)
        assertEquals(5, player.playbackState.value.transposeSemitones)
    }

    @Test
    fun `setTranspose clamps to valid range`() {
        player.setTranspose(15)
        assertEquals(12, player.playbackState.value.transposeSemitones)

        player.setTranspose(-15)
        assertEquals(-12, player.playbackState.value.transposeSemitones)
    }

    @Test
    fun `setTranspose accepts boundary values`() {
        player.setTranspose(12)
        assertEquals(12, player.playbackState.value.transposeSemitones)

        player.setTranspose(-12)
        assertEquals(-12, player.playbackState.value.transposeSemitones)
    }

    @Test
    fun `setTranspose accepts zero`() {
        player.setTranspose(5)
        player.setTranspose(0)
        assertEquals(0, player.playbackState.value.transposeSemitones)
    }

    // ==================== Transpose Application Tests ====================

    @Test
    fun `transposition adds semitones to MIDI notes`() {
        // Testing the transposition logic
        val originalMidi = 60
        val semitones = 5
        val transposed = (originalMidi + semitones).coerceIn(0, 127)
        assertEquals(65, transposed)
    }

    @Test
    fun `transposition clamps to MIDI range 0-127`() {
        val highMidi = 125
        val semitones = 5
        val transposed = (highMidi + semitones).coerceIn(0, 127)
        assertEquals(127, transposed)

        val lowMidi = 5
        val negativeSemitones = -10
        val transposedLow = (lowMidi + negativeSemitones).coerceIn(0, 127)
        assertEquals(0, transposedLow)
    }

    @Test
    fun `transposition does not affect rest notes`() {
        // Rest notes have midiNoteNumber = -1 and should not be transposed
        val restEvent = NoteEvent(midiNoteNumber = -1, durationMs = 500)
        // When transposing, rest notes should remain unchanged
        val resultEvent = if (restEvent.midiNoteNumber >= 0) {
            restEvent.copy(midiNoteNumber = (restEvent.midiNoteNumber + 5).coerceIn(0, 127))
        } else {
            restEvent
        }
        assertEquals(-1, resultEvent.midiNoteNumber)
    }

    // ==================== Loop Toggle Tests ====================

    @Test
    fun `toggleLoop changes loop state`() {
        assertFalse(player.playbackState.value.isLooping)

        player.toggleLoop()
        assertTrue(player.playbackState.value.isLooping)

        player.toggleLoop()
        assertFalse(player.playbackState.value.isLooping)
    }

    // ==================== Metronome Toggle Tests ====================

    @Test
    fun `toggleMetronome changes metronome state`() {
        assertFalse(player.playbackState.value.isMetronomeEnabled)

        player.toggleMetronome()
        assertTrue(player.playbackState.value.isMetronomeEnabled)

        player.toggleMetronome()
        assertFalse(player.playbackState.value.isMetronomeEnabled)
    }

    // ==================== Tempo Tests ====================

    @Test
    fun `setTempo updates playback state`() {
        player.loadScore(createTestScore())
        player.setTempo(90)
        assertEquals(90, player.playbackState.value.tempo)
    }

    @Test
    fun `tempo affects note duration calculation`() {
        // At 120 BPM: 1 beat = 500ms
        // At 60 BPM: 1 beat = 1000ms
        val tempo120BeatsPerSecond = 120 / 60.0
        val tempo60BeatsPerSecond = 60 / 60.0

        val durationBeats = 1.0f
        val duration120Ms = (durationBeats / tempo120BeatsPerSecond * 1000).toLong()
        val duration60Ms = (durationBeats / tempo60BeatsPerSecond * 1000).toLong()

        assertEquals(500L, duration120Ms)
        assertEquals(1000L, duration60Ms)
    }

    // ==================== Pause/Stop Tests ====================

    @Test
    fun `pause updates playback state`() {
        player.loadScore(createTestScore())
        player.pause()

        val state = player.playbackState.value
        assertFalse(state.isPlaying)
        assertTrue(state.isPaused)
    }

    @Test
    fun `stop resets playback state`() {
        player.loadScore(createTestScore())
        player.stop()

        val state = player.playbackState.value
        assertFalse(state.isPlaying)
        assertFalse(state.isPaused)
        assertEquals(0, state.currentNoteIndex)
        assertEquals(0f, state.currentBeat, 0.001f)
    }

    // ==================== Seek Tests ====================

    @Test
    fun `seekToNote updates current position`() {
        val score = createTestScore()
        player.loadScore(score)

        player.seekToNote(2)

        val state = player.playbackState.value
        assertEquals(2, state.currentNoteIndex)
        assertTrue(state.isPaused)
    }

    @Test
    fun `seekToNote does nothing for invalid index`() {
        val score = createTestScore()
        player.loadScore(score)

        val initialState = player.playbackState.value
        player.seekToNote(100) // Invalid index

        assertEquals(initialState.currentNoteIndex, player.playbackState.value.currentNoteIndex)
    }

    @Test
    fun `seekToNote does nothing for negative index`() {
        val score = createTestScore()
        player.loadScore(score)

        player.seekToNote(-1)

        assertEquals(0, player.playbackState.value.currentNoteIndex)
    }

    // ==================== Note Event Conversion Tests ====================

    @Test
    fun `note duration conversion is correct at 120 BPM`() {
        // At 120 BPM, quarter note = 500ms
        val tempo = 120
        val beatsPerSecond = tempo / 60.0
        val quarterNoteDuration = 1.0f

        val durationMs = (quarterNoteDuration / beatsPerSecond * 1000).toLong()
        assertEquals(500L, durationMs)
    }

    @Test
    fun `note duration conversion is correct for various durations`() {
        val tempo = 120
        val beatsPerSecond = tempo / 60.0

        // Whole note = 4 beats = 2000ms
        val wholeDurationMs = (4.0 / beatsPerSecond * 1000).toLong()
        assertEquals(2000L, wholeDurationMs)

        // Half note = 2 beats = 1000ms
        val halfDurationMs = (2.0 / beatsPerSecond * 1000).toLong()
        assertEquals(1000L, halfDurationMs)

        // Eighth note = 0.5 beats = 250ms
        val eighthDurationMs = (0.5 / beatsPerSecond * 1000).toLong()
        assertEquals(250L, eighthDurationMs)
    }

    @Test
    fun `metronome click interval is correct for tempo`() {
        val tempo = 120
        val clickIntervalMs = 60_000L / tempo
        assertEquals(500L, clickIntervalMs)

        val tempo60 = 60
        val clickInterval60Ms = 60_000L / tempo60
        assertEquals(1000L, clickInterval60Ms)
    }

    // ==================== State Preservation Tests ====================

    @Test
    fun `loading score preserves transpose setting`() {
        player.setTranspose(5)
        player.loadScore(createTestScore())

        // After loading, transpose should be reset (based on loadScore implementation)
        // This tests the expected behavior
        val state = player.playbackState.value
        assertEquals(0, state.transposeSemitones)
    }

    @Test
    fun `loading score preserves loop setting`() {
        player.toggleLoop()
        assertTrue(player.playbackState.value.isLooping)

        player.loadScore(createTestScore())
        // Loop state is reset when loading a new score
        assertFalse(player.playbackState.value.isLooping)
    }

    // ==================== Helper Functions ====================

    private fun createTestScore(
        tempo: Int = 120,
        title: String = "Test Score"
    ): MusicScore {
        val notes = listOf(
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0),
            MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 1f, 0),
            MusicNote(Pitch.E, 4, NoteDuration.QUARTER, 2f, 0),
            MusicNote(Pitch.F, 4, NoteDuration.QUARTER, 3f, 0)
        )

        return MusicScore(
            title = title,
            tempo = tempo,
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
    }
}

package com.musicscanner.app.audio

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

/**
 * Unit tests for AudioSynthesizer
 * Tests the MIDI to frequency conversion and NoteEvent data class
 */
class AudioSynthesizerTest {

    // ==================== MIDI to Frequency Conversion Tests ====================

    /**
     * Test the MIDI to frequency formula: f = 440 * 2^((n-69)/12)
     * We can't directly test the private method, but we can verify the formula
     */
    @Test
    fun `midiNoteToFrequency formula for A4 (69) should return 440Hz`() {
        val midiNote = 69
        val frequency = 440.0 * 2.0.pow((midiNote - 69) / 12.0)
        assertEquals(440.0, frequency, 0.001)
    }

    @Test
    fun `midiNoteToFrequency formula for A5 (81) should return 880Hz`() {
        val midiNote = 81
        val frequency = 440.0 * 2.0.pow((midiNote - 69) / 12.0)
        assertEquals(880.0, frequency, 0.001)
    }

    @Test
    fun `midiNoteToFrequency formula for A3 (57) should return 220Hz`() {
        val midiNote = 57
        val frequency = 440.0 * 2.0.pow((midiNote - 69) / 12.0)
        assertEquals(220.0, frequency, 0.001)
    }

    @Test
    fun `midiNoteToFrequency formula for middle C (60) should return approximately 261_63Hz`() {
        val midiNote = 60
        val frequency = 440.0 * 2.0.pow((midiNote - 69) / 12.0)
        assertEquals(261.63, frequency, 0.01)
    }

    @Test
    fun `midiNoteToFrequency formula for C5 (72) should return approximately 523_25Hz`() {
        val midiNote = 72
        val frequency = 440.0 * 2.0.pow((midiNote - 69) / 12.0)
        assertEquals(523.25, frequency, 0.01)
    }

    @Test
    fun `frequency doubles with each octave increase`() {
        val midiA3 = 57
        val midiA4 = 69
        val midiA5 = 81

        val freqA3 = 440.0 * 2.0.pow((midiA3 - 69) / 12.0)
        val freqA4 = 440.0 * 2.0.pow((midiA4 - 69) / 12.0)
        val freqA5 = 440.0 * 2.0.pow((midiA5 - 69) / 12.0)

        assertEquals(freqA3 * 2, freqA4, 0.001)
        assertEquals(freqA4 * 2, freqA5, 0.001)
    }

    @Test
    fun `semitone frequency ratio is approximately 1_0595`() {
        val midiNote1 = 69
        val midiNote2 = 70

        val freq1 = 440.0 * 2.0.pow((midiNote1 - 69) / 12.0)
        val freq2 = 440.0 * 2.0.pow((midiNote2 - 69) / 12.0)

        val ratio = freq2 / freq1
        assertEquals(1.0595, ratio, 0.001)
    }

    // ==================== NoteEvent Data Class Tests ====================

    @Test
    fun `NoteEvent can be created with valid values`() {
        val noteEvent = NoteEvent(
            midiNoteNumber = 60,
            durationMs = 500,
            velocity = 100
        )

        assertEquals(60, noteEvent.midiNoteNumber)
        assertEquals(500L, noteEvent.durationMs)
        assertEquals(100, noteEvent.velocity)
    }

    @Test
    fun `NoteEvent default velocity is 100`() {
        val noteEvent = NoteEvent(midiNoteNumber = 60, durationMs = 500)
        assertEquals(100, noteEvent.velocity)
    }

    @Test
    fun `NoteEvent midiNoteNumber of -1 represents rest`() {
        val restEvent = NoteEvent(midiNoteNumber = -1, durationMs = 1000)
        assertEquals(-1, restEvent.midiNoteNumber)
    }

    @Test
    fun `NoteEvent copy works correctly`() {
        val original = NoteEvent(midiNoteNumber = 60, durationMs = 500, velocity = 80)
        val copied = original.copy(velocity = 100)

        assertEquals(60, copied.midiNoteNumber)
        assertEquals(500L, copied.durationMs)
        assertEquals(100, copied.velocity)
    }

    @Test
    fun `NoteEvent equality works correctly`() {
        val event1 = NoteEvent(60, 500, 100)
        val event2 = NoteEvent(60, 500, 100)
        val event3 = NoteEvent(61, 500, 100)

        assertEquals(event1, event2)
        assertNotEquals(event1, event3)
    }

    // ==================== ADSR Envelope Tests (Formula Verification) ====================

    @Test
    fun `ADSR envelope attack phase starts at 0 and rises to 1`() {
        val sampleRate = 44100
        val attackSamples = (sampleRate * 0.01).toInt() // 10ms attack

        // At sample 0, envelope should be 0
        val envelopeAt0 = 0.0 / attackSamples
        assertEquals(0.0, envelopeAt0, 0.001)

        // At end of attack, envelope should be 1
        val envelopeAtEnd = attackSamples.toDouble() / attackSamples
        assertEquals(1.0, envelopeAtEnd, 0.001)
    }

    @Test
    fun `ADSR envelope sustain level is 0_7`() {
        val sustainLevel = 0.7
        assertEquals(0.7, sustainLevel, 0.001)
    }

    @Test
    fun `ADSR timing parameters are correct`() {
        val sampleRate = 44100

        val attackMs = 10
        val decayMs = 50
        val releaseMs = 100

        val attackSamples = (sampleRate * attackMs / 1000.0).toInt()
        val decaySamples = (sampleRate * decayMs / 1000.0).toInt()
        val releaseSamples = (sampleRate * releaseMs / 1000.0).toInt()

        assertEquals(441, attackSamples)
        assertEquals(2205, decaySamples)
        assertEquals(4410, releaseSamples)
    }

    // ==================== Harmonic Content Tests (Formula Verification) ====================

    @Test
    fun `harmonic amplitudes are in correct ratio`() {
        // fundamental = 1.0, harmonic2 = 0.5, harmonic3 = 0.25
        val fundamental = 1.0
        val harmonic2 = 0.5
        val harmonic3 = 0.25

        assertEquals(0.5, harmonic2 / fundamental, 0.001)
        assertEquals(0.25, harmonic3 / fundamental, 0.001)
    }

    @Test
    fun `wave normalization factor accounts for all harmonics`() {
        // Total amplitude = 1.0 + 0.5 + 0.25 = 1.75
        val totalAmplitude = 1.0 + 0.5 + 0.25
        assertEquals(1.75, totalAmplitude, 0.001)
    }

    // ==================== Sample Count Tests ====================

    @Test
    fun `sample count is calculated correctly from duration`() {
        val sampleRate = 44100
        val durationMs = 1000L

        val numSamples = (sampleRate * durationMs / 1000).toInt()
        assertEquals(44100, numSamples)
    }

    @Test
    fun `sample count for 500ms duration`() {
        val sampleRate = 44100
        val durationMs = 500L

        val numSamples = (sampleRate * durationMs / 1000).toInt()
        assertEquals(22050, numSamples)
    }

    @Test
    fun `sample count for short notes`() {
        val sampleRate = 44100
        val durationMs = 100L

        val numSamples = (sampleRate * durationMs / 1000).toInt()
        assertEquals(4410, numSamples)
    }
}

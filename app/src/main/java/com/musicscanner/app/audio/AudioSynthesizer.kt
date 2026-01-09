package com.musicscanner.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * Simple audio synthesizer for playing musical notes
 * Uses sine wave synthesis with envelope shaping
 */
class AudioSynthesizer {

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val sampleRate = 44100

    /**
     * Play a single note
     * @param midiNoteNumber MIDI note number (60 = middle C)
     * @param durationMs Duration in milliseconds
     * @param velocity Volume (0-127)
     */
    suspend fun playNote(
        midiNoteNumber: Int,
        durationMs: Long,
        velocity: Int = 100
    ) = withContext(Dispatchers.IO) {
        if (midiNoteNumber < 0) return@withContext  // Rest

        val frequency = midiNoteToFrequency(midiNoteNumber)
        val samples = generateNoteSamples(frequency, durationMs, velocity)

        val bufferSize = samples.size * 2  // 16-bit samples

        audioTrack?.release()
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.write(samples, 0, samples.size)
        audioTrack?.play()

        delay(durationMs)
        audioTrack?.stop()
    }

    /**
     * Play multiple notes in sequence
     */
    suspend fun playSequence(
        notes: List<NoteEvent>,
        onNoteStart: (Int) -> Unit = {},
        onComplete: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        for ((index, note) in notes.withIndex()) {
            if (!isActive) break

            onNoteStart(index)

            if (note.midiNoteNumber >= 0) {
                playNote(note.midiNoteNumber, note.durationMs, note.velocity)
            } else {
                // Rest - just wait
                delay(note.durationMs)
            }
        }
        onComplete()
    }

    /**
     * Start playing a sequence with control
     */
    fun startPlayback(
        notes: List<NoteEvent>,
        scope: CoroutineScope,
        onNoteStart: (Int) -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        stopPlayback()
        playbackJob = scope.launch {
            playSequence(notes, onNoteStart, onComplete)
        }
    }

    /**
     * Stop current playback
     */
    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean = playbackJob?.isActive == true

    /**
     * Convert MIDI note number to frequency in Hz
     * Formula: f = 440 * 2^((n-69)/12)
     * where 69 = A4 = 440Hz
     */
    private fun midiNoteToFrequency(midiNote: Int): Double {
        return 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)
    }

    /**
     * Generate audio samples for a note with ADSR envelope
     */
    private fun generateNoteSamples(
        frequency: Double,
        durationMs: Long,
        velocity: Int
    ): ShortArray {
        val numSamples = (sampleRate * durationMs / 1000).toInt()
        val samples = ShortArray(numSamples)

        val amplitude = (velocity / 127.0) * Short.MAX_VALUE * 0.8
        val angularFrequency = 2.0 * PI * frequency / sampleRate

        // ADSR envelope parameters (in samples)
        val attackSamples = (sampleRate * 0.01).toInt()  // 10ms attack
        val decaySamples = (sampleRate * 0.05).toInt()   // 50ms decay
        val sustainLevel = 0.7
        val releaseSamples = (sampleRate * 0.1).toInt()  // 100ms release

        for (i in 0 until numSamples) {
            // Calculate envelope
            val envelope = when {
                // Attack phase
                i < attackSamples -> i.toDouble() / attackSamples

                // Decay phase
                i < attackSamples + decaySamples -> {
                    val decayProgress = (i - attackSamples).toDouble() / decaySamples
                    1.0 - (1.0 - sustainLevel) * decayProgress
                }

                // Release phase (last part of note)
                i > numSamples - releaseSamples -> {
                    val releaseProgress = (numSamples - i).toDouble() / releaseSamples
                    sustainLevel * releaseProgress
                }

                // Sustain phase
                else -> sustainLevel
            }

            // Generate sample with harmonics for richer sound
            val fundamental = sin(angularFrequency * i)
            val harmonic2 = 0.5 * sin(2 * angularFrequency * i)
            val harmonic3 = 0.25 * sin(3 * angularFrequency * i)

            val wave = fundamental + harmonic2 + harmonic3
            val normalizedWave = wave / 1.75  // Normalize

            samples[i] = (amplitude * envelope * normalizedWave).toInt().toShort()
        }

        return samples
    }

    /**
     * Release resources
     */
    fun release() {
        stopPlayback()
    }
}

/**
 * Represents a note event for playback
 */
data class NoteEvent(
    val midiNoteNumber: Int,  // -1 for rest
    val durationMs: Long,
    val velocity: Int = 100
)

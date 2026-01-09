package com.musicscanner.app.audio

import com.musicscanner.app.data.MusicNote
import com.musicscanner.app.data.MusicScore
import com.musicscanner.app.data.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * High-level music player that handles MusicScore playback
 */
class MusicPlayer {

    private val synthesizer = AudioSynthesizer()
    private val metronomeSynthesizer = AudioSynthesizer()
    private var metronomeJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentScore: MusicScore? = null
    private var noteEvents: List<NoteEvent> = emptyList()
    private var notes: List<MusicNote> = emptyList()

    /**
     * Load a music score for playback
     */
    fun loadScore(score: MusicScore) {
        currentScore = score
        notes = score.getAllNotes()
        noteEvents = convertToNoteEvents(score)

        _playbackState.value = PlaybackState(
            isPlaying = false,
            isPaused = false,
            currentBeat = 0f,
            totalBeats = score.getTotalBeats(),
            tempo = score.tempo,
            currentNoteIndex = 0
        )
    }

    /**
     * Start or resume playback
     */
    fun play(scope: CoroutineScope) {
        if (noteEvents.isEmpty()) return

        val startIndex = if (_playbackState.value.isPaused) {
            _playbackState.value.currentNoteIndex
        } else {
            0
        }

        // Apply transposition to note events
        val transposedEvents = applyTransposition(noteEvents, _playbackState.value.transposeSemitones)
        val eventsToPlay = transposedEvents.drop(startIndex)

        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isPaused = false
        )

        // Start metronome if enabled
        if (_playbackState.value.isMetronomeEnabled) {
            startMetronome(scope)
        }

        synthesizer.startPlayback(
            notes = eventsToPlay,
            scope = scope,
            onNoteStart = { relativeIndex ->
                val absoluteIndex = startIndex + relativeIndex
                if (absoluteIndex < notes.size) {
                    val note = notes[absoluteIndex]
                    _playbackState.value = _playbackState.value.copy(
                        currentNoteIndex = absoluteIndex,
                        currentBeat = note.measureNumber * 4f + note.positionInMeasure
                    )
                }
            },
            onComplete = {
                stopMetronome()
                if (_playbackState.value.isLooping) {
                    // Loop: restart playback from beginning
                    _playbackState.value = _playbackState.value.copy(
                        currentNoteIndex = 0,
                        currentBeat = 0f
                    )
                    play(scope)
                } else {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isPaused = false,
                        currentNoteIndex = 0,
                        currentBeat = 0f
                    )
                }
            }
        )
    }

    /**
     * Apply transposition to note events
     */
    private fun applyTransposition(events: List<NoteEvent>, semitones: Int): List<NoteEvent> {
        if (semitones == 0) return events
        return events.map { event ->
            if (event.midiNoteNumber >= 0) {
                event.copy(midiNoteNumber = (event.midiNoteNumber + semitones).coerceIn(0, 127))
            } else {
                event // Rest note, don't transpose
            }
        }
    }

    /**
     * Start metronome click track
     */
    private fun startMetronome(scope: CoroutineScope) {
        stopMetronome()
        val tempo = _playbackState.value.tempo
        val clickIntervalMs = (60_000L / tempo)

        metronomeJob = scope.launch {
            while (true) {
                // Play a short click (high-pitched short note)
                metronomeSynthesizer.playNote(
                    midiNoteNumber = 76, // High E - woodblock-like
                    durationMs = 30,
                    velocity = 60
                )
                kotlinx.coroutines.delay(clickIntervalMs - 30)
            }
        }
    }

    /**
     * Stop metronome
     */
    private fun stopMetronome() {
        metronomeJob?.cancel()
        metronomeJob = null
        metronomeSynthesizer.stopPlayback()
    }

    /**
     * Pause playback
     */
    fun pause() {
        synthesizer.stopPlayback()
        stopMetronome()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = true
        )
    }

    /**
     * Stop playback and reset to beginning
     */
    fun stop() {
        synthesizer.stopPlayback()
        stopMetronome()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false,
            currentNoteIndex = 0,
            currentBeat = 0f
        )
    }

    /**
     * Set playback tempo
     */
    fun setTempo(bpm: Int) {
        _playbackState.value = _playbackState.value.copy(tempo = bpm)
        // Regenerate note events with new tempo
        currentScore?.let { score ->
            noteEvents = convertToNoteEvents(score.copy(tempo = bpm))
        }
    }

    /**
     * Set transposition in semitones (-12 to +12)
     */
    fun setTranspose(semitones: Int) {
        val clamped = semitones.coerceIn(-12, 12)
        _playbackState.value = _playbackState.value.copy(transposeSemitones = clamped)
    }

    /**
     * Toggle loop mode
     */
    fun toggleLoop() {
        _playbackState.value = _playbackState.value.copy(
            isLooping = !_playbackState.value.isLooping
        )
    }

    /**
     * Toggle metronome
     */
    fun toggleMetronome() {
        val newState = !_playbackState.value.isMetronomeEnabled
        _playbackState.value = _playbackState.value.copy(isMetronomeEnabled = newState)
    }

    /**
     * Seek to specific position
     */
    fun seekToNote(noteIndex: Int) {
        if (noteIndex in notes.indices) {
            val wasPlaying = _playbackState.value.isPlaying
            if (wasPlaying) {
                pause()
            }
            _playbackState.value = _playbackState.value.copy(
                currentNoteIndex = noteIndex,
                currentBeat = notes[noteIndex].measureNumber * 4f + notes[noteIndex].positionInMeasure,
                isPaused = true
            )
        }
    }

    /**
     * Convert MusicScore to playable NoteEvents
     */
    private fun convertToNoteEvents(score: MusicScore): List<NoteEvent> {
        val tempo = score.tempo
        val beatsPerSecond = tempo / 60.0

        return score.getAllNotes().map { note ->
            val durationBeats = note.getDurationInBeats()
            val durationMs = (durationBeats / beatsPerSecond * 1000).toLong()

            NoteEvent(
                midiNoteNumber = note.toMidiNoteNumber(),
                durationMs = durationMs,
                velocity = 100
            )
        }
    }

    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean = synthesizer.isPlaying()

    /**
     * Release resources
     */
    fun release() {
        stopMetronome()
        synthesizer.release()
        metronomeSynthesizer.release()
    }
}

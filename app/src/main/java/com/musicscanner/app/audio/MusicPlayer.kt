package com.musicscanner.app.audio

import com.musicscanner.app.data.MusicNote
import com.musicscanner.app.data.MusicScore
import com.musicscanner.app.data.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level music player that handles MusicScore playback
 */
class MusicPlayer {

    private val synthesizer = AudioSynthesizer()

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

        val eventsToPlay = noteEvents.drop(startIndex)

        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isPaused = false
        )

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
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    isPaused = false,
                    currentNoteIndex = 0,
                    currentBeat = 0f
                )
            }
        )
    }

    /**
     * Pause playback
     */
    fun pause() {
        synthesizer.stopPlayback()
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
        synthesizer.release()
    }
}

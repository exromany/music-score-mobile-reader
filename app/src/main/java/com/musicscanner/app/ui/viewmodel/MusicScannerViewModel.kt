package com.musicscanner.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicscanner.app.audio.MusicPlayer
import com.musicscanner.app.data.MusicScore
import com.musicscanner.app.data.PlaybackState
import com.musicscanner.app.data.ProcessingState
import com.musicscanner.app.recognition.MusicRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing music scanning and playback state
 */
class MusicScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRecognizer = MusicRecognizer(application)
    private val musicPlayer = MusicPlayer()

    // Processing state
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    // Current score
    private val _currentScore = MutableStateFlow<MusicScore?>(null)
    val currentScore: StateFlow<MusicScore?> = _currentScore.asStateFlow()

    // Playback state
    val playbackState: StateFlow<PlaybackState> = musicPlayer.playbackState

    // Captured image path
    private val _capturedImagePath = MutableStateFlow<String?>(null)
    val capturedImagePath: StateFlow<String?> = _capturedImagePath.asStateFlow()

    /**
     * Process a captured image
     */
    fun processImage(imagePath: String) {
        _capturedImagePath.value = imagePath

        viewModelScope.launch {
            try {
                val score = musicRecognizer.recognize(imagePath) { state ->
                    _processingState.value = state
                }
                _currentScore.value = score
                musicPlayer.loadScore(score)
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(
                    e.message ?: "Failed to process image"
                )
            }
        }
    }

    /**
     * Start or resume playback
     */
    fun play() {
        musicPlayer.play(viewModelScope)
    }

    /**
     * Pause playback
     */
    fun pause() {
        musicPlayer.pause()
    }

    /**
     * Stop playback
     */
    fun stop() {
        musicPlayer.stop()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayback() {
        if (playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Set playback tempo
     */
    fun setTempo(bpm: Int) {
        musicPlayer.setTempo(bpm)
    }

    /**
     * Seek to specific note
     */
    fun seekToNote(index: Int) {
        musicPlayer.seekToNote(index)
    }

    /**
     * Reset state for new scan
     */
    fun reset() {
        stop()
        _processingState.value = ProcessingState.Idle
        _currentScore.value = null
        _capturedImagePath.value = null
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
    }
}

package com.musicscanner.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicscanner.app.audio.MusicPlayer
import com.musicscanner.app.data.MusicScore
import com.musicscanner.app.data.PlaybackState
import com.musicscanner.app.data.ProcessingState
import com.musicscanner.app.data.ScoreRepository
import com.musicscanner.app.recognition.MusicRecognizer
import com.musicscanner.app.recognition.PreviewSettings
import com.musicscanner.app.recognition.RealtimePreviewData
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
    private val scoreRepository = ScoreRepository(application)

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

    // Real-time preview state
    private val _previewData = MutableStateFlow(RealtimePreviewData())
    val previewData: StateFlow<RealtimePreviewData> = _previewData.asStateFlow()

    private val _previewSettings = MutableStateFlow(PreviewSettings())
    val previewSettings: StateFlow<PreviewSettings> = _previewSettings.asStateFlow()

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
                // Save to history
                scoreRepository.saveScore(score, imagePath)
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(
                    e.message ?: "Failed to process image"
                )
            }
        }
    }

    /**
     * Load a score from history
     */
    fun loadScoreFromHistory(score: MusicScore) {
        _currentScore.value = score
        musicPlayer.loadScore(score)
        _processingState.value = ProcessingState.Complete(score)
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
     * Set transposition in semitones (-12 to +12)
     */
    fun setTranspose(semitones: Int) {
        musicPlayer.setTranspose(semitones)
    }

    /**
     * Toggle loop mode
     */
    fun toggleLoop() {
        musicPlayer.toggleLoop()
    }

    /**
     * Toggle metronome
     */
    fun toggleMetronome() {
        musicPlayer.toggleMetronome()
    }

    /**
     * Update real-time preview data from camera frame analysis
     */
    fun updatePreviewData(data: RealtimePreviewData) {
        _previewData.value = data
    }

    /**
     * Toggle real-time preview mode
     */
    fun togglePreviewMode() {
        _previewSettings.value = _previewSettings.value.copy(
            enabled = !_previewSettings.value.enabled
        )
    }

    /**
     * Enable or disable real-time preview
     */
    fun setPreviewEnabled(enabled: Boolean) {
        _previewSettings.value = _previewSettings.value.copy(enabled = enabled)
    }

    /**
     * Update preview settings
     */
    fun updatePreviewSettings(settings: PreviewSettings) {
        _previewSettings.value = settings
    }

    /**
     * Clear preview data
     */
    fun clearPreviewData() {
        _previewData.value = RealtimePreviewData()
    }

    /**
     * Reset state for new scan
     */
    fun reset() {
        stop()
        _processingState.value = ProcessingState.Idle
        _currentScore.value = null
        _capturedImagePath.value = null
        _previewData.value = RealtimePreviewData()
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
    }
}

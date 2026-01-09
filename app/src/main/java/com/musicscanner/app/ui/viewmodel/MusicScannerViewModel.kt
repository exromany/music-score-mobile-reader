package com.musicscanner.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicscanner.app.audio.MusicPlayer
import com.musicscanner.app.data.MusicScore
import com.musicscanner.app.data.PlaybackState
import com.musicscanner.app.data.PreferencesRepository
import com.musicscanner.app.data.ProcessingState
import com.musicscanner.app.data.ScoreRepository
import com.musicscanner.app.recognition.MusicRecognizer
import com.musicscanner.app.recognition.PageStatus
import com.musicscanner.app.recognition.PreviewSettings
import com.musicscanner.app.recognition.RealtimePreviewData
import com.musicscanner.app.recognition.ScannedPage
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
    private val preferencesRepository = PreferencesRepository(application)

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

    // Theme preference
    val isDarkMode: StateFlow<Boolean> = preferencesRepository.isDarkMode

    // Multi-page scanning state
    private val _multiPageMode = MutableStateFlow(false)
    val multiPageMode: StateFlow<Boolean> = _multiPageMode.asStateFlow()

    private val _scannedPages = MutableStateFlow<List<ScannedPage>>(emptyList())
    val scannedPages: StateFlow<List<ScannedPage>> = _scannedPages.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    // Batch processing state
    private val _batchQueue = MutableStateFlow<List<String>>(emptyList())
    val batchQueue: StateFlow<List<String>> = _batchQueue.asStateFlow()

    private val _batchProcessing = MutableStateFlow(false)
    val batchProcessing: StateFlow<Boolean> = _batchProcessing.asStateFlow()

    private val _batchProgress = MutableStateFlow(0f)
    val batchProgress: StateFlow<Float> = _batchProgress.asStateFlow()

    // Image enhancement settings
    private val _autoCropEnabled = MutableStateFlow(true)
    val autoCropEnabled: StateFlow<Boolean> = _autoCropEnabled.asStateFlow()

    private val _perspectiveCorrectionEnabled = MutableStateFlow(true)
    val perspectiveCorrectionEnabled: StateFlow<Boolean> = _perspectiveCorrectionEnabled.asStateFlow()

    /**
     * Process a captured image
     */
    fun processImage(imagePath: String) {
        _capturedImagePath.value = imagePath

        viewModelScope.launch {
            try {
                val preprocessingSettings = MusicRecognizer.PreprocessingSettings(
                    autoCrop = _autoCropEnabled.value,
                    correctPerspective = _perspectiveCorrectionEnabled.value,
                    enhanceContrast = true
                )
                val score = musicRecognizer.recognize(imagePath, { state ->
                    _processingState.value = state
                }, preprocessingSettings)
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
     * Toggle multi-page scanning mode
     */
    fun toggleMultiPageMode() {
        _multiPageMode.value = !_multiPageMode.value
        if (!_multiPageMode.value) {
            clearScannedPages()
        }
    }

    /**
     * Add a page to multi-page scan
     */
    fun addPageToScan(imagePath: String) {
        val pageNumber = _scannedPages.value.size + 1
        val newPage = ScannedPage(pageNumber, imagePath, null, PageStatus.PENDING)
        _scannedPages.value = _scannedPages.value + newPage
    }

    /**
     * Remove a page from multi-page scan
     */
    fun removePageFromScan(pageNumber: Int) {
        _scannedPages.value = _scannedPages.value
            .filter { it.pageNumber != pageNumber }
            .mapIndexed { index, page -> page.copy(pageNumber = index + 1) }
    }

    /**
     * Process all scanned pages
     */
    fun processMultiPageScan() {
        if (_scannedPages.value.isEmpty()) return

        viewModelScope.launch {
            try {
                val imagePaths = _scannedPages.value.map { it.imagePath }
                val preprocessingSettings = MusicRecognizer.PreprocessingSettings(
                    autoCrop = _autoCropEnabled.value,
                    correctPerspective = _perspectiveCorrectionEnabled.value,
                    enhanceContrast = true
                )

                val score = musicRecognizer.recognizeMultiPage(
                    imagePaths = imagePaths,
                    onProgressUpdate = { state ->
                        _processingState.value = state
                    },
                    onPageProcessed = { pageNum, total, page ->
                        _currentPageIndex.value = pageNum
                        _scannedPages.value = _scannedPages.value.map {
                            if (it.pageNumber == pageNum) page else it
                        }
                    },
                    preprocessingSettings = preprocessingSettings
                )

                _currentScore.value = score
                musicPlayer.loadScore(score)
                _processingState.value = ProcessingState.Complete(score)

                // Save merged score to history
                scoreRepository.saveScore(score, _scannedPages.value.first().imagePath)
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(
                    e.message ?: "Failed to process multi-page scan"
                )
            }
        }
    }

    /**
     * Clear all scanned pages
     */
    fun clearScannedPages() {
        _scannedPages.value = emptyList()
        _currentPageIndex.value = 0
    }

    /**
     * Add images to batch queue
     */
    fun addToBatchQueue(imagePaths: List<String>) {
        _batchQueue.value = _batchQueue.value + imagePaths
    }

    /**
     * Remove image from batch queue
     */
    fun removeFromBatchQueue(imagePath: String) {
        _batchQueue.value = _batchQueue.value.filter { it != imagePath }
    }

    /**
     * Clear batch queue
     */
    fun clearBatchQueue() {
        _batchQueue.value = emptyList()
        _batchProgress.value = 0f
    }

    /**
     * Process all images in batch queue
     */
    fun processBatchQueue() {
        if (_batchQueue.value.isEmpty() || _batchProcessing.value) return

        _batchProcessing.value = true
        _batchProgress.value = 0f

        viewModelScope.launch {
            val queue = _batchQueue.value.toList()
            val preprocessingSettings = MusicRecognizer.PreprocessingSettings(
                autoCrop = _autoCropEnabled.value,
                correctPerspective = _perspectiveCorrectionEnabled.value,
                enhanceContrast = true
            )

            for ((index, imagePath) in queue.withIndex()) {
                try {
                    val score = musicRecognizer.recognize(imagePath, { state ->
                        _processingState.value = state
                    }, preprocessingSettings)

                    // Save each processed score to history
                    scoreRepository.saveScore(score, imagePath)
                } catch (e: Exception) {
                    // Continue with next image on error
                }

                _batchProgress.value = (index + 1).toFloat() / queue.size
            }

            _batchProcessing.value = false
            _batchQueue.value = emptyList()
        }
    }

    /**
     * Toggle auto-crop setting
     */
    fun toggleAutoCrop() {
        _autoCropEnabled.value = !_autoCropEnabled.value
    }

    /**
     * Toggle perspective correction setting
     */
    fun togglePerspectiveCorrection() {
        _perspectiveCorrectionEnabled.value = !_perspectiveCorrectionEnabled.value
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
     * Toggle dark mode
     */
    fun toggleDarkMode() {
        preferencesRepository.toggleDarkMode()
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
        _multiPageMode.value = false
        _scannedPages.value = emptyList()
        _currentPageIndex.value = 0
        _batchQueue.value = emptyList()
        _batchProcessing.value = false
        _batchProgress.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
    }
}

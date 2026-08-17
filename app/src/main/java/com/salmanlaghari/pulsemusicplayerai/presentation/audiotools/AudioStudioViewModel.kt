package com.salmanlaghari.pulsemusicplayerai.presentation.audiotools

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salmanlaghari.pulsemusicplayerai.core.service.AudioStudioProcessor
import com.salmanlaghari.pulsemusicplayerai.domain.model.AudioFormat
import com.salmanlaghari.pulsemusicplayerai.domain.model.CompressionPreset
import com.salmanlaghari.pulsemusicplayerai.domain.model.ExportedFile
import com.salmanlaghari.pulsemusicplayerai.domain.model.VisualizerVideoConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioStudioViewModel(private val context: Context) : ViewModel() {

    private val processor = AudioStudioProcessor(context)
    private var activeJob: Job? = null

    // Processing state flows
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _recentExports = MutableStateFlow<List<ExportedFile>>(emptyList())
    val recentExports: StateFlow<List<ExportedFile>> = _recentExports.asStateFlow()

    // File selection states
    private val _selectedFiles = MutableStateFlow<List<Uri>>(emptyList())
    val selectedFiles: StateFlow<List<Uri>> = _selectedFiles.asStateFlow()

    // Dialog state management
    private val _showResultDialog = MutableStateFlow<Pair<Boolean, ExportedFile?>?>(null) // (Success, File)
    val showResultDialog: StateFlow<Pair<Boolean, ExportedFile?>?> = _showResultDialog.asStateFlow()

    init {
        loadRecentExports()
    }

    fun loadRecentExports() {
        viewModelScope.launch {
            _recentExports.value = processor.fetchRecentExports()
        }
    }

    fun selectFiles(uris: List<Uri>) {
        _selectedFiles.value = uris
    }

    fun clearSelection() {
        _selectedFiles.value = emptyList()
    }

    fun cancelActiveOperation() {
        activeJob?.cancel()
        activeJob = null
        _isProcessing.value = false
        _progress.value = 0
        _statusMessage.value = "Operation cancelled."
    }

    fun closeResultDialog() {
        _showResultDialog.value = null
    }

    // --- Core Operations ---

    fun cutAudio(sourceUri: Uri, outputName: String, startMs: Float, endMs: Float) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        _progress.value = 0
        _statusMessage.value = "Scanning sound waveform details..."

        activeJob = viewModelScope.launch {
            try {
                val result = processor.cutAudio(sourceUri, outputName, startMs, endMs) { prog ->
                    _progress.value = prog
                    _statusMessage.value = "Trimming Audio Track: $prog%"
                }
                _isProcessing.value = false
                if (result != null) {
                    _showResultDialog.value = Pair(true, result)
                    loadRecentExports()
                } else {
                    _showResultDialog.value = Pair(false, null)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                _showResultDialog.value = Pair(false, null)
            }
        }
    }

    fun mergeAudio(sourceUris: List<Uri>, outputName: String) {
        if (_isProcessing.value || sourceUris.isEmpty()) return
        _isProcessing.value = true
        _progress.value = 0
        _statusMessage.value = "Queuing audio streams..."

        activeJob = viewModelScope.launch {
            try {
                val result = processor.mergeAudio(sourceUris, outputName) { prog ->
                    _progress.value = prog
                    _statusMessage.value = "Merging Tracks: $prog%"
                }
                _isProcessing.value = false
                if (result != null) {
                    _showResultDialog.value = Pair(true, result)
                    loadRecentExports()
                } else {
                    _showResultDialog.value = Pair(false, null)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                _showResultDialog.value = Pair(false, null)
            }
        }
    }

    fun convertAudio(sourceUri: Uri, outputName: String, format: AudioFormat) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        _progress.value = 0
        _statusMessage.value = "Preparing decoder..."

        activeJob = viewModelScope.launch {
            try {
                val result = processor.convertAudio(sourceUri, outputName, format) { prog ->
                    _progress.value = prog
                    _statusMessage.value = "Converting format to ${format.name}: $prog%"
                }
                _isProcessing.value = false
                if (result != null) {
                    _showResultDialog.value = Pair(true, result)
                    loadRecentExports()
                } else {
                    _showResultDialog.value = Pair(false, null)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                _showResultDialog.value = Pair(false, null)
            }
        }
    }

    fun extractAudio(sourceUri: Uri, outputName: String, outputFormat: String) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        _progress.value = 0
        _statusMessage.value = "Demuxing video container..."

        activeJob = viewModelScope.launch {
            try {
                val result = processor.extractAudio(sourceUri, outputName, outputFormat) { prog ->
                    _progress.value = prog
                    _statusMessage.value = "Extracting Audio Stream: $prog%"
                }
                _isProcessing.value = false
                if (result != null) {
                    _showResultDialog.value = Pair(true, result)
                    loadRecentExports()
                } else {
                    _showResultDialog.value = Pair(false, null)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                _showResultDialog.value = Pair(false, null)
            }
        }
    }

    fun compressAudio(sourceUri: Uri, outputName: String, preset: CompressionPreset) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        _progress.value = 0
        _statusMessage.value = "Configuring bitrate settings..."

        activeJob = viewModelScope.launch {
            try {
                val result = processor.compressAudio(sourceUri, outputName, preset) { prog ->
                    _progress.value = prog
                    _statusMessage.value = "Applying ${preset.name} Compression: $prog%"
                }
                _isProcessing.value = false
                if (result != null) {
                    _showResultDialog.value = Pair(true, result)
                    loadRecentExports()
                } else {
                    _showResultDialog.value = Pair(false, null)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                _showResultDialog.value = Pair(false, null)
            }
        }
    }

    fun changeSpeedAndPitch(sourceUri: Uri, outputName: String, speed: Float, pitch: Float) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        _progress.value = 0
        _statusMessage.value = "Initializing DSP filters..."

        activeJob = viewModelScope.launch {
            try {
                val result = processor.changeSpeedAndPitch(sourceUri, outputName, speed, pitch) { prog ->
                    _progress.value = prog
                    _statusMessage.value = "Resampling pitch/speed: $prog%"
                }
                _isProcessing.value = false
                if (result != null) {
                    _showResultDialog.value = Pair(true, result)
                    loadRecentExports()
                } else {
                    _showResultDialog.value = Pair(false, null)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                _showResultDialog.value = Pair(false, null)
            }
        }
    }

    fun exportVisualizerVideo(sourceUri: Uri, outputName: String) {
        exportVisualizerVideo(sourceUri, VisualizerVideoConfig(outputName = outputName))
    }

    /**
     * Real MP3 -> MP4 export driven by the same config object the live preview
     * uses, so every selected option is reflected in the output file.
     */
    fun exportVisualizerVideo(sourceUri: Uri, config: VisualizerVideoConfig) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        _progress.value = 0
        _statusMessage.value = "Decoding audio for visualizer analysis..."

        activeJob = viewModelScope.launch {
            try {
                val result = processor.exportVisualizerVideo(sourceUri, config) { prog ->
                    _progress.value = prog
                    _statusMessage.value = when {
                        prog < 20 -> "Decoding audio track: $prog%"
                        prog < 80 -> "Rendering ${config.preset.displayName} frames (${config.videoWidth}x${config.videoHeight} @ ${config.fps}fps): $prog%"
                        prog < 92 -> "Encoding AAC audio track: $prog%"
                        else -> "Muxing final MP4: $prog%"
                    }
                }
                _isProcessing.value = false
                if (result != null) {
                    _showResultDialog.value = Pair(true, result)
                    loadRecentExports()
                } else {
                    _showResultDialog.value = Pair(false, null)
                }
            } catch (e: CancellationException) {
                _isProcessing.value = false
                throw e
            } catch (e: Exception) {
                _isProcessing.value = false
                _showResultDialog.value = Pair(false, null)
            }
        }
    }

    // --- Live preview analysis ---

    private val _spectrumTrack = MutableStateFlow<AudioStudioProcessor.SpectrumTrack?>(null)
    val spectrumTrack: StateFlow<AudioStudioProcessor.SpectrumTrack?> = _spectrumTrack.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisProgress = MutableStateFlow(0)
    val analysisProgress: StateFlow<Int> = _analysisProgress.asStateFlow()

    private var analysisJob: Job? = null

    /**
     * Precomputes the real spectrum data for the selected track so the live
     * preview reacts to the actual audio content.
     */
    fun analyzeForPreview(sourceUri: Uri, fps: Int = 30) {
        analysisJob?.cancel()
        _spectrumTrack.value = null
        _isAnalyzing.value = true
        _analysisProgress.value = 0
        analysisJob = viewModelScope.launch {
            try {
                val track = processor.analyzeSpectrum(sourceUri, fps) { p ->
                    _analysisProgress.value = p
                }
                _spectrumTrack.value = track
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _spectrumTrack.value = null
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clearPreviewAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        _spectrumTrack.value = null
        _isAnalyzing.value = false
        _analysisProgress.value = 0
    }

    fun renameExport(file: ExportedFile, newName: String) {
        viewModelScope.launch {
            if (processor.renameExport(file, newName)) {
                loadRecentExports()
            }
        }
    }

    fun deleteExport(file: ExportedFile) {
        viewModelScope.launch {
            if (processor.deleteExport(file)) {
                loadRecentExports()
            }
        }
    }

    fun shareExport(file: ExportedFile) {
        try {
            val isVideo = file.format.equals("MP4", ignoreCase = true)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (isVideo) "video/mp4" else "audio/*"
                putExtra(Intent.EXTRA_STREAM, file.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, if (isVideo) "Share Exported Video" else "Share Exported Audio").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class AudioStudioViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioStudioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioStudioViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

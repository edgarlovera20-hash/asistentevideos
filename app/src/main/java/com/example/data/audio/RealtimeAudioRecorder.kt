package com.example.data.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class RealtimeAudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "RealtimeAudioRecorder"
    }

    enum class RecordingState {
        IDLE,
        RECORDING,
        PAUSED,
        STOPPED,
        ERROR
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private var recorderScope = CoroutineScope(Dispatchers.Default)
    private var samplerJob: Job? = null

    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(customOutputFile: File? = null): Boolean {
        if (!hasMicrophonePermission()) {
            Log.e(TAG, "Cannot start recording: RECORD_AUDIO permission missing")
            _recordingState.value = RecordingState.ERROR
            return false
        }

        try {
            stopRecordingInternal()

            val outputFile = customOutputFile ?: createTempAudioFile()
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _recordingState.value = RecordingState.RECORDING
            _durationSeconds.value = 0
            _amplitudeHistory.value = emptyList()

            startSamplingLoop()
            Log.d(TAG, "MediaRecorder started successfully: ${outputFile.absolutePath}")
            return true

        } catch (e: IOException) {
            Log.e(TAG, "IOException setting up MediaRecorder", e)
            _recordingState.value = RecordingState.ERROR
            currentOutputFile = null
            return false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException starting MediaRecorder", e)
            _recordingState.value = RecordingState.ERROR
            currentOutputFile = null
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error starting MediaRecorder", e)
            _recordingState.value = RecordingState.ERROR
            currentOutputFile = null
            return false
        }
    }

    fun pauseRecording(): Boolean {
        if (_recordingState.value != RecordingState.RECORDING) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null) {
                mediaRecorder?.pause()
            }
            _recordingState.value = RecordingState.PAUSED
            Log.d(TAG, "Recording paused")
            true
        } catch (e: Exception) {
            // Leave state as RECORDING — MediaRecorder never actually paused, don't lie to the caller.
            Log.e(TAG, "Error pausing recorder", e)
            false
        }
    }

    fun resumeRecording(): Boolean {
        if (_recordingState.value != RecordingState.PAUSED) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null) {
                mediaRecorder?.resume()
            }
            _recordingState.value = RecordingState.RECORDING
            Log.d(TAG, "Recording resumed")
            true
        } catch (e: Exception) {
            // Leave state as PAUSED — MediaRecorder never actually resumed, don't lie to the caller.
            Log.e(TAG, "Error resuming recorder", e)
            false
        }
    }

    fun stopRecording(): File? {
        val fileToReturn = currentOutputFile
        stopRecordingInternal()
        _recordingState.value = RecordingState.STOPPED
        Log.d(TAG, "Recording stopped. Saved file: ${fileToReturn?.absolutePath}")
        return fileToReturn
    }

    private fun stopRecordingInternal() {
        samplerJob?.cancel()
        samplerJob = null

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    Log.w(TAG, "Stop called before audio frames received or already stopped")
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        } finally {
            mediaRecorder = null
        }
    }

    private fun startSamplingLoop() {
        samplerJob?.cancel()
        samplerJob = recorderScope.launch {
            var counter = 0
            while (isActive && (_recordingState.value == RecordingState.RECORDING || _recordingState.value == RecordingState.PAUSED)) {
                delay(100)
                if (_recordingState.value == RecordingState.RECORDING) {
                    counter++
                    if (counter % 10 == 0) {
                        _durationSeconds.value += 1
                    }

                    // ponytail: maxAmplitude == 0 is real silence, not missing data — don't fake a level for it.
                    val amp: Float = try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        (maxAmp.toFloat() / 32767f).coerceIn(0f, 1.0f)
                    } catch (e: Exception) {
                        0f
                    }

                    _currentAmplitude.value = amp
                    val history = (_amplitudeHistory.value + amp).takeLast(40)
                    _amplitudeHistory.value = history
                }
            }
        }
    }

    private fun createTempAudioFile(): File {
        val cacheDir = context.cacheDir
        val audioDir = File(cacheDir, "audio_recordings").apply { if (!exists()) mkdirs() }
        return File(audioDir, "meeting_rec_${System.currentTimeMillis()}.m4a")
    }

    fun release() {
        stopRecordingInternal()
        _recordingState.value = RecordingState.IDLE
    }
}

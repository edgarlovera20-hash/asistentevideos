package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class SpeechCaptureEvent {
    data class FinalResult(val text: String) : SpeechCaptureEvent()
    data class Amplitude(val normalized: Float) : SpeechCaptureEvent()
}

/**
 * Wraps the platform [SpeechRecognizer] for continuous live capture: real microphone
 * amplitude (from onRmsChanged) and real transcribed text (no more scripted demo lines).
 *
 * ponytail: SpeechRecognizer has no speaker-diarization API, so multi-speaker tagging
 * isn't possible from a single mic stream — everything is attributed to one speaker.
 * Upgrade to a diarizing STT backend if per-speaker attribution is required.
 */
class SpeechCaptureController(private val context: Context) {

    fun events(): Flow<SpeechCaptureEvent> = callbackFlow {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            close(SecurityException("Permiso de micrófono no concedido"))
            return@callbackFlow
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            close(IllegalStateException("Reconocimiento de voz no disponible en este dispositivo"))
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        var shouldRestart = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        fun restart() {
            if (shouldRestart) recognizer.startListening(intent)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onRmsChanged(rmsdB: Float) {
                // ponytail: rmsdB is an uncalibrated relative level (~ -2..10), not real dB SPL.
                trySend(SpeechCaptureEvent.Amplitude(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)))
            }

            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { trySend(SpeechCaptureEvent.FinalResult(it)) }
                restart()
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onError(error: Int) {
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    shouldRestart = false
                    close(SecurityException("Permiso de micrófono revocado"))
                    return
                }
                // No speech / timeout errors are normal in a quiet room — just keep listening.
                restart()
            }
        })

        restart()

        awaitClose {
            shouldRestart = false
            recognizer.stopListening()
            recognizer.destroy()
        }
    }
}

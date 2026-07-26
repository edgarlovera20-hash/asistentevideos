package com.example.data.api

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

data class AudioTranscriptSegment(
    val speakerTag: String,
    val speakerName: String,
    val text: String,
    val timestampMs: Long
)

data class AudioTranscriptionResult(
    val fullText: String,
    val segments: List<AudioTranscriptSegment>,
    val detectedLanguage: String = "es",
    val confidenceScore: Float = 0.95f
)

/**
 * Transcribes recorded meeting audio via the same Gemini REST endpoint used by
 * [GeminiMeetingService] (RetrofitClient) — audio goes in as base64 inlineData.
 *
 * ponytail: the earlier version of this file called the firebase-ai SDK, which needs a
 * google-services.json this project never had, so every real call failed and silently
 * fell back to canned text. Reusing the already-working REST client avoids that failure
 * mode entirely instead of adding a second, unconfigured AI client.
 */
class GeminiAudioTranscriptionService {

    companion object {
        private const val TAG = "GeminiAudioTranscription"
    }

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

    private val hasApiKey: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    /**
     * Transcribes a recorded audio file with speaker diarization.
     */
    suspend fun transcribeAudioFile(
        audioFile: File,
        mimeType: String = "audio/mp4",
        meetingTitle: String = "Reunión Grabada",
        participants: List<String> = emptyList()
    ): AudioTranscriptionResult = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext generateFallbackFileResult(meetingTitle, participants)
        }

        val audioBytes = try {
            FileInputStream(audioFile).use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading audio file: ${audioFile.absolutePath}", e)
            ByteArray(0)
        }

        if (audioBytes.isNotEmpty() && hasApiKey) {
            try {
                val participantsStr = if (participants.isNotEmpty()) participants.joinToString(", ") else "Participantes no especificados"
                val promptText = """
                    Eres Heavenly AI Audio Transcriber.
                    Analiza y transcribe este archivo de audio de la reunión "$meetingTitle".
                    Participantes conocidos: $participantsStr.

                    Proporciona una transcripción completa con diarización de hablantes.
                    Para cada segmento, usa la siguiente estructura en líneas separadas:
                    [00:00] Persona 1 (Nombre): Texto transcrito.
                """.trimIndent()

                val response = retryIO {
                    RetrofitClient.api.generateContent(
                        apiKey = apiKey,
                        request = GeminiRequest(
                            contents = listOf(
                                GeminiContent(
                                    parts = listOf(
                                        GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = Base64.encodeToString(audioBytes, Base64.NO_WRAP))),
                                        GeminiPart(text = promptText)
                                    )
                                )
                            )
                        )
                    )
                }
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!responseText.isNullOrBlank()) {
                    val segments = parseFullTranscriptText(responseText, participants)
                    val fullText = segments.joinToString("\n") { "${it.speakerTag} (${it.speakerName}): ${it.text}" }
                    return@withContext AudioTranscriptionResult(
                        fullText = fullText.ifBlank { responseText },
                        segments = segments,
                        detectedLanguage = "es",
                        confidenceScore = 0.96f
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini audio transcription failed, using fallback: ${e.message}")
            }
        }

        generateFallbackFileResult(meetingTitle, participants)
    }

    private fun parseFullTranscriptText(
        text: String,
        participants: List<String>
    ): List<AudioTranscriptSegment> {
        val segments = mutableListOf<AudioTranscriptSegment>()
        val lines = text.lines()
        var currentMs = 0L

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            var speakerName = participants.firstOrNull() ?: "Edgar Gomez"
            var speakerTag = "Persona 1"
            var segmentText = trimmed

            if (trimmed.contains(":")) {
                val header = trimmed.substringBefore(":")
                segmentText = trimmed.substringAfter(":").trim()

                if (header.contains("Persona", true) || header.contains("Hablante", true) || header.contains("(")) {
                    speakerTag = header.substringBefore("(").trim()
                    if (header.contains("(")) {
                        speakerName = header.substringAfter("(").substringBefore(")").trim()
                    }
                } else if (header.isNotBlank()) {
                    speakerName = header.replace(Regex("""\[.*?\]"""), "").trim()
                }
            }

            segments.add(
                AudioTranscriptSegment(
                    speakerTag = speakerTag.ifBlank { "Persona 1" },
                    speakerName = speakerName.ifBlank { "Edgar Gomez" },
                    text = segmentText,
                    timestampMs = currentMs
                )
            )
            currentMs += 5000L
        }

        return if (segments.isNotEmpty()) segments else listOf(
            AudioTranscriptSegment("Persona 1", participants.getOrNull(0) ?: "Edgar Gomez", text, 0L)
        )
    }

    private fun generateFallbackFileResult(
        title: String,
        participants: List<String>
    ): AudioTranscriptionResult {
        val p1 = participants.getOrNull(0) ?: "Edgar Gomez"

        val segments = listOf(
            AudioTranscriptSegment("Persona 1", p1, "No se pudo transcribir el audio (sin API key configurada o sin conexión). Revisa .env y vuelve a intentarlo.", 0L)
        )

        return AudioTranscriptionResult(
            fullText = segments.joinToString("\n") { "${it.speakerTag} (${it.speakerName}): ${it.text}" },
            segments = segments,
            detectedLanguage = "es",
            confidenceScore = 0.0f
        )
    }
}

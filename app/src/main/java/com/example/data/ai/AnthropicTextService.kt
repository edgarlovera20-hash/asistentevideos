package com.example.data.ai

import com.example.data.api.MeetingAnalysisResult
import com.example.data.api.retryIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Anthropic Messages API — the one provider here with its own request/response shape. */
class AnthropicTextService(
    private val apiKey: String,
    private val model: String = "claude-sonnet-4-5"
) : AiTextService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private suspend fun complete(prompt: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        }
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Anthropic API error ${response.code}: $text")
            val content = JSONObject(text).getJSONArray("content")
            content.getJSONObject(0).getString("text")
        }
    }

    override suspend fun analyzeMeeting(transcript: String, participants: String, meetingTitle: String): MeetingAnalysisResult =
        try {
            parseMeetingAnalysisText(retryIO { complete(buildAnalysisPrompt(transcript, participants, meetingTitle)) })
        } catch (e: Exception) {
            fallbackAnalysis("Anthropic Claude", meetingTitle, com.example.data.api.describeError(e))
        }

    override suspend fun chat(meetingContext: String, userQuestion: String, chatHistory: List<Pair<String, String>>): String =
        try {
            retryIO { complete(buildChatPrompt(meetingContext, userQuestion, chatHistory)) }
        } catch (e: Exception) {
            fallbackChatResponse("Anthropic Claude", com.example.data.api.describeError(e))
        }

    override suspend fun generateDocument(meetingTitle: String, transcript: String, analysis: String, formatType: String): String =
        try {
            retryIO { complete(buildDocumentPrompt(meetingTitle, transcript, analysis, formatType)) }
        } catch (e: Exception) {
            fallbackDocument("Anthropic Claude", meetingTitle, com.example.data.api.describeError(e))
        }

    override suspend fun translate(text: String, targetLanguage: String): String =
        try {
            retryIO { complete(buildTranslatePrompt(text, targetLanguage)) }
        } catch (e: Exception) {
            "No se pudo traducir con Anthropic Claude (sin conexión o configuración inválida). Detalle: ${com.example.data.api.describeError(e)}"
        }
}

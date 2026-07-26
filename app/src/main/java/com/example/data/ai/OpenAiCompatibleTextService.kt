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

/**
 * One client for every provider that speaks the OpenAI `/chat/completions` shape:
 * OpenAI itself, Kimi (Moonshot), and self-hosted Ollama / LM Studio (both deliberately
 * emulate this API). [baseUrl] must include the path up to and excluding
 * "/chat/completions", e.g. "https://api.openai.com/v1" or "http://192.168.1.20:11434/v1".
 * [apiKey] may be blank for local servers that don't require one.
 */
class OpenAiCompatibleTextService(
    private val providerLabel: String,
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : AiTextService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private suspend fun complete(prompt: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        }
        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("content-type", "application/json")
        if (apiKey.isNotBlank()) requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        val request = requestBuilder.post(body.toString().toRequestBody("application/json".toMediaType())).build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("$providerLabel API error ${response.code}: $text")
            JSONObject(text).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    override suspend fun analyzeMeeting(transcript: String, participants: String, meetingTitle: String): MeetingAnalysisResult =
        try {
            parseMeetingAnalysisText(retryIO { complete(buildAnalysisPrompt(transcript, participants, meetingTitle)) })
        } catch (e: Exception) {
            fallbackAnalysis(providerLabel, meetingTitle)
        }

    override suspend fun chat(meetingContext: String, userQuestion: String, chatHistory: List<Pair<String, String>>): String =
        try {
            retryIO { complete(buildChatPrompt(meetingContext, userQuestion, chatHistory)) }
        } catch (e: Exception) {
            fallbackChatResponse(providerLabel)
        }

    override suspend fun generateDocument(meetingTitle: String, transcript: String, analysis: String, formatType: String): String =
        try {
            retryIO { complete(buildDocumentPrompt(meetingTitle, transcript, analysis, formatType)) }
        } catch (e: Exception) {
            fallbackDocument(providerLabel, meetingTitle)
        }

    override suspend fun translate(text: String, targetLanguage: String): String =
        try {
            retryIO { complete(buildTranslatePrompt(text, targetLanguage)) }
        } catch (e: Exception) {
            "No se pudo traducir con $providerLabel (sin conexión o configuración inválida)."
        }
}

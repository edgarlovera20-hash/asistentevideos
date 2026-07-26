package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * NVIDIA NIM hosted text-to-image endpoint (FLUX.1-schnell). Cold-start generation can take
 * well over a minute, so this uses long timeouts and a single attempt — no retryIO, since
 * retrying would just multiply the wait on an already-slow call.
 *
 * ponytail: response parsing tries several field names NVIDIA's genai endpoints are known to
 * use ("image", "images[0]", "artifacts[0].base64", "b64_json") since the exact shape for this
 * specific model couldn't be confirmed from this environment (network timeouts reaching
 * ai.api.nvidia.com). If generation succeeds but decoding fails, the raw JSON keys are included
 * in the exception message so the real shape can be added here in one line.
 */
class NvidiaImageService(private val apiKey: String) {

    companion object {
        private const val ENDPOINT = "https://ai.api.nvidia.com/v1/genai/black-forest-labs/flux.1-schnell"
        private val VALID_DIMENSIONS = setOf(768, 832, 896, 960, 1024, 1088, 1152, 1216, 1280, 1344)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateImage(
        prompt: String,
        width: Int = 1024,
        height: Int = 1024,
        steps: Int = 4,
        seed: Int = 0
    ): ByteArray = withContext(Dispatchers.IO) {
        require(width in VALID_DIMENSIONS && height in VALID_DIMENSIONS) {
            "width/height deben ser uno de: $VALID_DIMENSIONS"
        }
        val body = JSONObject().apply {
            put("prompt", prompt)
            put("width", width)
            put("height", height)
            put("steps", steps)
            put("seed", seed)
        }
        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("NVIDIA API error ${response.code}: ${text.take(300)}")
            extractBase64Image(text)
        }
    }

    private fun extractBase64Image(rawJson: String): ByteArray {
        val json = JSONObject(rawJson)
        val base64 = when {
            json.has("image") -> json.getString("image")
            json.has("b64_json") -> json.getString("b64_json")
            json.optJSONArray("images")?.length()?.let { it > 0 } == true ->
                json.getJSONArray("images").getString(0)
            json.optJSONArray("artifacts")?.length()?.let { it > 0 } == true ->
                json.getJSONArray("artifacts").getJSONObject(0).getString("base64")
            json.optJSONArray("data")?.length()?.let { it > 0 } == true ->
                json.getJSONArray("data").getJSONObject(0).getString("b64_json")
            else -> throw Exception("Formato de respuesta no reconocido. Claves recibidas: ${json.keys().asSequence().toList()}")
        }
        val stripped = if (base64.startsWith("data:")) base64.substringAfter(",") else base64
        return android.util.Base64.decode(stripped, android.util.Base64.DEFAULT)
    }
}

package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Creates a plain text file in the user's Google Drive (via the "multipart" upload shape
 * Drive API v3 expects: a JSON metadata part + a media part). Plain OkHttp instead of
 * Retrofit here — Retrofit's @Multipart annotations don't map cleanly onto Drive's
 * "one JSON part + one arbitrary-type media part" upload, so raw MultipartBody is simpler.
 */
class GoogleDriveService {

    companion object {
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Returns the new file's Drive id, or throws on failure. */
    suspend fun createTextFile(accessToken: String, title: String, content: String): String =
        withContext(Dispatchers.IO) {
            val metadataJson = """{"name":"${escapeJson(title)}.txt","mimeType":"text/plain"}"""

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(MultipartBody.Part.create(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())))
                .addPart(MultipartBody.Part.create(content.toRequestBody("text/plain; charset=UTF-8".toMediaType())))
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Drive upload failed: ${response.code} $responseBody")
                }
                Regex(""""id":\s*"([^"]+)"""").find(responseBody)?.groupValues?.get(1)
                    ?: throw IOException("Drive upload succeeded but no file id in response")
            }
        }

    private fun escapeJson(text: String) = text.replace("\\", "\\\\").replace("\"", "\\\"")
}

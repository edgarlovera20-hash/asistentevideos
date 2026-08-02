package com.example.data.api

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttpClient instances for the non-Gemini AI/image services (Anthropic,
 * OpenAiCompatible, NvidiaImage). Each of those used to build its own client per instance —
 * wasteful, since an OkHttpClient owns its own connection pool and dispatcher thread pool and
 * is meant to be reused. Gemini's RetrofitClient already does this correctly via its own
 * singleton `okHttpClient`, kept separate since it's wired through Retrofit rather than raw calls.
 */
object SharedHttpClients {
    /** Ordinary text completion calls (Anthropic, OpenAI-compatible). */
    val standard: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** Image generation — cold-start diffusion can take well over a minute. */
    val longRunning: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(150, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

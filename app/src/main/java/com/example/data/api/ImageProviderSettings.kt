package com.example.data.api

import android.content.Context
import android.content.SharedPreferences

/** Stores the NVIDIA NIM API key used for real image generation in Visual IA. Single provider today, so no enum needed — see AiProviderSettings for the multi-provider pattern this would grow into. */
class ImageProviderSettings(context: Context) {
    companion object {
        private const val PREFS_NAME = "image_provider_settings"
        private const val KEY_API_KEY = "nvidia_api_key"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""
    fun setApiKey(key: String) = prefs.edit().putString(KEY_API_KEY, key).apply()
}

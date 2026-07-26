package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences

enum class AiProvider(val label: String, val defaultModel: String) {
    GEMINI("Gemini", "gemini-flash-latest"),
    ANTHROPIC("Anthropic Claude", "claude-sonnet-4-5"),
    OPENAI("OpenAI", "gpt-4o-mini"),
    KIMI("Kimi (Moonshot)", "moonshot-v1-8k"),
    NVIDIA_NIM("NVIDIA NIM", "moonshotai/kimi-k2.6"),
    OLLAMA("Ollama (local)", "llama3"),
    LMSTUDIO("LM Studio (local)", "local-model")
}

/**
 * Stores the active AI provider + its API key/base URL, one row per provider so switching
 * back and forth doesn't lose credentials. Same plain-SharedPreferences pattern as
 * GoogleAuthManager — see its ponytail comment for why that's fine here too (personal
 * sideloaded app, keys the user typed themselves, not a shared/managed device).
 */
class AiProviderSettings(context: Context) {

    companion object {
        private const val PREFS_NAME = "ai_provider_settings"
        private const val KEY_ACTIVE_PROVIDER = "active_provider"
        private fun keyFor(provider: AiProvider) = "key_${provider.name}"
        private fun urlFor(provider: AiProvider) = "url_${provider.name}"
        private fun modelFor(provider: AiProvider) = "model_${provider.name}"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var activeProvider: AiProvider
        get() = AiProvider.entries.find { it.name == prefs.getString(KEY_ACTIVE_PROVIDER, null) } ?: AiProvider.GEMINI
        set(value) = prefs.edit().putString(KEY_ACTIVE_PROVIDER, value.name).apply()

    fun getApiKey(provider: AiProvider): String = prefs.getString(keyFor(provider), "") ?: ""
    fun setApiKey(provider: AiProvider, key: String) = prefs.edit().putString(keyFor(provider), key).apply()

    /** Base URL — only meaningful for Ollama/LM Studio (self-hosted, e.g. "http://192.168.1.20:11434/v1"). */
    fun getBaseUrl(provider: AiProvider): String = prefs.getString(urlFor(provider), "") ?: ""
    fun setBaseUrl(provider: AiProvider, url: String) = prefs.edit().putString(urlFor(provider), url).apply()

    fun getModel(provider: AiProvider): String = prefs.getString(modelFor(provider), provider.defaultModel) ?: provider.defaultModel
    fun setModel(provider: AiProvider, model: String) = prefs.edit().putString(modelFor(provider), model).apply()
}

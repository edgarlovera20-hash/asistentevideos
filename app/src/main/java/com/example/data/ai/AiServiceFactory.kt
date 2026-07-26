package com.example.data.ai

import android.content.Context

object AiServiceFactory {
    fun create(context: Context): AiTextService {
        val settings = AiProviderSettings(context)
        return createFor(settings, settings.activeProvider)
    }

    /** Builds a service for a specific provider regardless of which one is active — used by the "Probar conexión" button in Ajustes IA. */
    fun createFor(context: Context, provider: AiProvider): AiTextService =
        createFor(AiProviderSettings(context), provider)

    private fun createFor(settings: AiProviderSettings, provider: AiProvider): AiTextService = when (provider) {
        AiProvider.GEMINI -> GeminiTextService(settings.getApiKey(provider).ifBlank { null })
        AiProvider.ANTHROPIC -> AnthropicTextService(
            apiKey = settings.getApiKey(provider),
            model = settings.getModel(provider)
        )
        AiProvider.OPENAI -> OpenAiCompatibleTextService(
            providerLabel = provider.label,
            baseUrl = "https://api.openai.com/v1",
            apiKey = settings.getApiKey(provider),
            model = settings.getModel(provider)
        )
        AiProvider.KIMI -> OpenAiCompatibleTextService(
            providerLabel = provider.label,
            baseUrl = "https://api.moonshot.cn/v1",
            apiKey = settings.getApiKey(provider),
            model = settings.getModel(provider)
        )
        AiProvider.OLLAMA, AiProvider.LMSTUDIO -> OpenAiCompatibleTextService(
            providerLabel = provider.label,
            baseUrl = settings.getBaseUrl(provider).ifBlank {
                if (provider == AiProvider.OLLAMA) "http://localhost:11434/v1" else "http://localhost:1234/v1"
            },
            apiKey = settings.getApiKey(provider),
            model = settings.getModel(provider)
        )
    }
}

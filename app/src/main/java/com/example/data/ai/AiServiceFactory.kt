package com.example.data.ai

import android.content.Context

object AiServiceFactory {
    fun create(context: Context): AiTextService {
        val settings = AiProviderSettings(context)
        val provider = settings.activeProvider
        return when (provider) {
            AiProvider.GEMINI -> GeminiTextService()
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
}

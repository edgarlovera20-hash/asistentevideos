package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.ai.AiProvider
import com.example.data.ai.AiProviderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AiSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = AiProviderSettings(application)

    private val _activeProvider = MutableStateFlow(settings.activeProvider)
    val activeProvider: StateFlow<AiProvider> = _activeProvider.asStateFlow()

    fun selectProvider(provider: AiProvider) {
        settings.activeProvider = provider
        _activeProvider.value = provider
    }

    fun getApiKey(provider: AiProvider) = settings.getApiKey(provider)
    fun getBaseUrl(provider: AiProvider) = settings.getBaseUrl(provider)
    fun getModel(provider: AiProvider) = settings.getModel(provider)

    fun saveApiKey(provider: AiProvider, key: String) = settings.setApiKey(provider, key)
    fun saveBaseUrl(provider: AiProvider, url: String) = settings.setBaseUrl(provider, url)
    fun saveModel(provider: AiProvider, model: String) = settings.setModel(provider, model)
}

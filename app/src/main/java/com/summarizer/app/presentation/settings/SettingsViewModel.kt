package com.summarizer.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.summarizer.app.core.di.ServiceLocator
import com.summarizer.app.data.preferences.PreferenceManager
import com.summarizer.app.domain.usecase.GetSettingsUseCase
import com.summarizer.app.domain.usecase.SaveSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State holding user configuration inputs.
 */
data class SettingsUiState(
    val apiKey: String = "",
    val customPrompt: String = "",
    val isLoading: Boolean = false,
    val isSavedSuccessfully: Boolean = false
)

/**
 * ViewModel orchestrating user settings changes (saving/resetting prompt).
 */
class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Reads configured API credentials and prompt templates asynchronously.
     */
    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSavedSuccessfully = false) }
            val settings = getSettingsUseCase()
            _uiState.update {
                it.copy(
                    apiKey = settings.apiKey,
                    customPrompt = settings.customPrompt,
                    isLoading = false
                )
            }
        }
    }

    fun onApiKeyChanged(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey, isSavedSuccessfully = false) }
    }

    fun onCustomPromptChanged(newPrompt: String) {
        _uiState.update { it.copy(customPrompt = newPrompt, isSavedSuccessfully = false) }
    }

    /**
     * Persists updated configuration into the hardware encrypted local vault.
     */
    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            saveSettingsUseCase(
                apiKey = _uiState.value.apiKey,
                customPrompt = _uiState.value.customPrompt
            )
            _uiState.update { it.copy(isLoading = false, isSavedSuccessfully = true) }
        }
    }

    /**
     * Resets the local prompt buffer to the highly-optimized default.
     */
    fun resetPromptToDefault() {
        _uiState.update { it.copy(customPrompt = PreferenceManager.DEFAULT_PROMPT, isSavedSuccessfully = false) }
    }

    /**
     * Clean Android Factory to initialize ViewModel with dependencies from the Service Locator.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                getSettingsUseCase = ServiceLocator.provideGetSettingsUseCase(context),
                saveSettingsUseCase = ServiceLocator.provideSaveSettingsUseCase(context)
            ) as T
        }
    }
}

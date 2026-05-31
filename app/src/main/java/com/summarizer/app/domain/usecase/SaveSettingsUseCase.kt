package com.summarizer.app.domain.usecase

import com.summarizer.app.domain.repository.SummaryRepository

/**
 * UseCase to update API Credentials and custom prompt templates.
 */
class SaveSettingsUseCase(private val repository: SummaryRepository) {

    suspend operator fun invoke(apiKey: String, customPrompt: String) {
        repository.saveApiKey(apiKey)
        repository.saveCustomPrompt(customPrompt)
    }
}

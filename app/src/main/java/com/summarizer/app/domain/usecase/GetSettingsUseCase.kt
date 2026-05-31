package com.summarizer.app.domain.usecase

import com.summarizer.app.domain.repository.SummaryRepository

data class AppSettings(
    val apiKey: String,
    val customPrompt: String
)

/**
 * UseCase to safely fetch API credentials and custom prompts in a unified model.
 */
class GetSettingsUseCase(private val repository: SummaryRepository) {

    suspend operator fun invoke(): AppSettings {
        return AppSettings(
            apiKey = repository.getApiKey(),
            customPrompt = repository.getCustomPrompt()
        )
    }
}

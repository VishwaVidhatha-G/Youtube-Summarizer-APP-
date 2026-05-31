package com.summarizer.app.domain.usecase

import com.summarizer.app.domain.repository.SummaryRepository

/**
 * UseCase to delete a specific cached video summary.
 */
class DeleteSummaryUseCase(private val repository: SummaryRepository) {

    suspend operator fun invoke(videoId: String) {
        repository.deleteSummary(videoId)
    }
}

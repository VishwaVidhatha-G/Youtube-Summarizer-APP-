package com.summarizer.app.domain.usecase

import com.summarizer.app.domain.model.SummaryItem
import com.summarizer.app.domain.repository.SummaryRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase to retrieve a reactive stream of completed summaries from offline cache.
 */
class GetHistoryUseCase(private val repository: SummaryRepository) {

    operator fun invoke(): Flow<List<SummaryItem>> {
        return repository.getHistoryFlow()
    }
}

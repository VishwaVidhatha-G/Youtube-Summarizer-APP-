package com.summarizer.app.domain.usecase

import com.summarizer.app.domain.model.SummaryItem
import com.summarizer.app.domain.repository.SummaryRepository

/**
 * UseCase representing the core summarization pipeline.
 * Clean domain boundaries: orchestrates checking the cache, parsing transcripts,
 * and calling the AI engine sequentially.
 */
class GetSummaryUseCase(private val repository: SummaryRepository) {

    /**
     * Executes the summarization pipeline for a given YouTube video.
     * Checks the local SQLite cache first to optimize speed and API quotas.
     *
     * @param videoId The 11-character YouTube video ID.
     * @param title The title of the video (usually received from the share event).
     * @param forceRefresh If true, skips the cache and forces a fresh scrape + AI generation.
     */
    suspend operator fun invoke(
        videoId: String,
        title: String,
        forceRefresh: Boolean = false
    ): Result<SummaryItem> {
        if (!forceRefresh) {
            // Step 1: Query the local SQLite database to see if we already generated this summary
            val cachedSummary = repository.getLocalSummary(videoId)
            if (cachedSummary != null) {
                return Result.success(cachedSummary)
            }
        }

        // Step 2: Extract the transcript directly from the client side
        val transcriptResult = repository.fetchTranscript(videoId)
        if (transcriptResult.isFailure) {
            return Result.failure(
                transcriptResult.exceptionOrNull() ?: Exception("Unknown error extracting YouTube transcript.")
            )
        }

        val transcript = transcriptResult.getOrThrow()
        val transcriptText = transcript.getFullText()
        if (transcriptText.isBlank()) {
            return Result.failure(Exception("The extracted transcript is empty. Subtitles might be disabled on this video."))
        }

        // Step 3: Run the Gemini summarizer over the clean text
        return repository.generateSummary(videoId, title, transcriptText)
    }
}

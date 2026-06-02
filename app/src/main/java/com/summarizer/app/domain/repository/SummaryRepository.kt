package com.summarizer.app.domain.repository

import com.summarizer.app.domain.model.SummaryItem
import com.summarizer.app.domain.model.Transcript
import com.summarizer.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface defining all operations for summarization, transcript scraping,
 * offline history caching, and secure setting profiles.
 */
interface SummaryRepository {

    /**
     * Retrieve a summary directly from local offline database, if available.
     */
    suspend fun getLocalSummary(videoId: String): SummaryItem?

    /**
     * Scrape the caption tracks from YouTube and return a parsed [Transcript] object.
     */
    suspend fun fetchTranscript(videoId: String): Result<Transcript>

    /**
     * Query the Gemini LLM to generate a summary for a transcript text using the current prompt template.
     */
    suspend fun generateSummary(videoId: String, title: String, transcriptText: String): Result<SummaryItem>

    /**
     * Query the Gemini LLM to generate a streaming summary, emitting text chunks dynamically.
     */
    fun generateSummaryStream(videoId: String, title: String, transcriptText: String): Flow<String>

    /**
     * Queries the Gemini LLM to generate a streaming response to a user's question, given the video transcript and conversation history context.
     */
    fun generateChatStream(
        transcriptText: String,
        history: List<ChatMessage>,
        question: String
    ): Flow<String>

    /**
     * Store a completed summary in the local database.
     */
    suspend fun saveSummary(summaryItem: SummaryItem)

    /**
     * Listen to reactive changes in the local database history.
     */
    fun getHistoryFlow(): Flow<List<SummaryItem>>

    /**
     * Delete a cached summary by its video ID.
     */
    suspend fun deleteSummary(videoId: String)

    /**
     * Securely store the Gemini API Key inside local storage.
     */
    suspend fun saveApiKey(apiKey: String)

    /**
     * Retrieve the securely stored Gemini API Key. Returns an empty string if unset.
     */
    suspend fun getApiKey(): String

    /**
     * Save the user's custom summarization template prompt.
     */
    suspend fun saveCustomPrompt(prompt: String)

    /**
     * Retrieve the custom prompt template. Returns a default template if none is set.
     */
    suspend fun getCustomPrompt(): String
}

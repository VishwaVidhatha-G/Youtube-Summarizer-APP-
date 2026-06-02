package com.summarizer.app.data.repository

import com.summarizer.app.data.database.SummaryDao
import com.summarizer.app.data.database.SummaryEntity
import com.summarizer.app.data.model.GeminiRequest
import com.summarizer.app.data.network.GeminiApiService
import com.summarizer.app.data.network.YoutubeTranscriptScraper
import com.summarizer.app.data.preferences.PreferenceManager
import com.summarizer.app.domain.model.SummaryItem
import com.summarizer.app.domain.model.Transcript
import com.summarizer.app.domain.model.ChatMessage
import com.summarizer.app.domain.repository.SummaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of the [SummaryRepository] interface.
 * Serves as the single source of truth for managing all transcription and summarization operations.
 */
class SummaryRepositoryImpl(
    private val summaryDao: SummaryDao,
    private val transcriptScraper: YoutubeTranscriptScraper,
    private val geminiApiService: GeminiApiService,
    private val preferenceManager: PreferenceManager
) : SummaryRepository {

    override suspend fun getLocalSummary(videoId: String): SummaryItem? = withContext(Dispatchers.IO) {
        summaryDao.getSummaryById(videoId)?.toDomain()
    }

    override suspend fun fetchTranscript(videoId: String): Result<Transcript> = withContext(Dispatchers.IO) {
        transcriptScraper.fetchTranscript(videoId)
    }

    override suspend fun generateSummary(
        videoId: String,
        title: String,
        transcriptText: String
    ): Result<SummaryItem> = withContext(Dispatchers.IO) {
        try {
            val apiKey = preferenceManager.getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    Exception("Gemini API Key is not set. Please open the app's settings and input your Google AI Studio API Key first.")
                )
            }

            val customPrompt = preferenceManager.getCustomPrompt()
            // Construct the ultimate prompt for Gemini
            val fullPrompt = "$customPrompt\n\nVideo Title: $title\n\nTranscript Content:\n$transcriptText"

            val request = GeminiRequest.createSimpleRequest(fullPrompt)
            val response = geminiApiService.generateContent(apiKey, request)

            val summaryContent = response.getGeneratedText()
                ?: return@withContext Result.failure(
                    Exception("Gemini returned an empty response. Please verify your API Key or try again.")
                )

            val summaryItem = SummaryItem(
                videoId = videoId,
                title = title,
                summary = summaryContent,
                timestamp = System.currentTimeMillis()
            )

            // Cache in local offline storage instantly
            saveSummary(summaryItem)

            Result.success(summaryItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun generateSummaryStream(
        videoId: String,
        title: String,
        transcriptText: String
    ): Flow<String> = flow {
        val apiKey = preferenceManager.getApiKey()
        if (apiKey.isBlank()) {
            throw Exception("Gemini API Key is not set. Please open the app's settings and input your Google AI Studio API Key first.")
        }

        val customPrompt = preferenceManager.getCustomPrompt()
        val fullPrompt = "$customPrompt\n\nVideo Title: $title\n\nTranscript Content:\n$transcriptText"

        val request = GeminiRequest.createSimpleRequest(fullPrompt)
        val response = geminiApiService.streamGenerateContent(apiKey, request)
        if (!response.isSuccessful) {
            throw Exception("Failed to connect to Gemini API: ${response.code()}")
        }
        val responseBody = response.body() ?: throw Exception("Empty response body from Gemini API.")
        
        val gson = com.google.gson.Gson()
        var fullText = ""
        
        val source = responseBody.source()
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data: ")) {
                val jsonStr = line.substring(6).trim()
                if (jsonStr == "[DONE]") break
                try {
                    val chunk = gson.fromJson(jsonStr, com.summarizer.app.data.model.GeminiResponse::class.java)
                    val text = chunk.getGeneratedText()
                    if (text != null) {
                        fullText += text
                        emit(fullText)
                    }
                } catch (e: Exception) {
                    // ignore malformed chunks
                }
            }
        }
        
        if (fullText.isNotBlank()) {
            val summaryItem = SummaryItem(
                videoId = videoId,
                title = title,
                summary = fullText,
                timestamp = System.currentTimeMillis()
            )
            saveSummary(summaryItem)
        }
    }.flowOn(Dispatchers.IO)

    override fun generateChatStream(
        transcriptText: String,
        history: List<ChatMessage>,
        question: String
    ): Flow<String> = flow {
        val apiKey = preferenceManager.getApiKey()
        if (apiKey.isBlank()) {
            throw Exception("Gemini API Key is not set. Please open the app's settings and input your Google AI Studio API Key first.")
        }

        val historyPrompt = history.joinToString("\n") { msg ->
            if (msg.isUser) "User: ${msg.content}" else "Assistant: ${msg.content}"
        }

        val prompt = """
            You are a helpful, expert AI assistant answering questions about a YouTube video.
            You are given the full video transcript below. Use the transcript to answer the user's question with high accuracy, detail, and context.
            
            CRITICAL RULES:
            1. LANGUAGE: Keep the language simple, plain, and easy to understand. Avoid complex jargon, but do NOT make the answers vague, brief, or incomplete. Be comprehensive and thorough in your explanations.
            2. GROUNDING: Answer using ONLY the facts and information stated in the video transcript. Do not assume, extrapolate, or bring in outside knowledge. If the video does not cover the answer, state "I cannot find this information in the video transcript."
            3. EXPLANATION: Reference specific details, names, steps, or explanations from the video transcript to answer the user's question fully.

            Transcript:
            $transcriptText

            Conversation History:
            $historyPrompt

            User Question:
            $question

            Assistant:
        """.trimIndent()

        val request = GeminiRequest.createSimpleRequest(prompt)
        val response = geminiApiService.streamGenerateContent(apiKey, request)
        if (!response.isSuccessful) {
            throw Exception("Failed to connect to Gemini API: ${response.code()}")
        }
        val responseBody = response.body() ?: throw Exception("Empty response body from Gemini API.")
        
        val gson = com.google.gson.Gson()
        var fullText = ""
        
        val source = responseBody.source()
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data: ")) {
                val jsonStr = line.substring(6).trim()
                if (jsonStr == "[DONE]") break
                try {
                    val chunk = gson.fromJson(jsonStr, com.summarizer.app.data.model.GeminiResponse::class.java)
                    val text = chunk.getGeneratedText()
                    if (text != null) {
                        fullText += text
                        emit(fullText)
                    }
                } catch (e: Exception) {
                    // ignore malformed chunks
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveSummary(summaryItem: SummaryItem) = withContext(Dispatchers.IO) {
        summaryDao.insertSummary(SummaryEntity.fromDomain(summaryItem))
    }

    override fun getHistoryFlow(): Flow<List<SummaryItem>> {
        return summaryDao.getAllSummariesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteSummary(videoId: String) = withContext(Dispatchers.IO) {
        summaryDao.deleteSummaryById(videoId)
    }

    override suspend fun saveApiKey(apiKey: String) = withContext(Dispatchers.IO) {
        preferenceManager.saveApiKey(apiKey)
    }

    override suspend fun getApiKey(): String = withContext(Dispatchers.IO) {
        preferenceManager.getApiKey()
    }

    override suspend fun saveCustomPrompt(prompt: String) = withContext(Dispatchers.IO) {
        preferenceManager.saveCustomPrompt(prompt)
    }

    override suspend fun getCustomPrompt(): String = withContext(Dispatchers.IO) {
        preferenceManager.getCustomPrompt()
    }
}

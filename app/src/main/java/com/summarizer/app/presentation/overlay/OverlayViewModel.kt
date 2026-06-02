package com.summarizer.app.presentation.overlay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.summarizer.app.core.di.ServiceLocator
import com.summarizer.app.core.utils.YoutubeParser
import com.summarizer.app.domain.model.SummaryItem
import com.summarizer.app.domain.model.ChatMessage
import com.summarizer.app.domain.repository.SummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State holding the real-time processing variables for the share bottom sheet.
 */
data class OverlayUiState(
    val videoId: String? = null,
    val title: String = "YouTube Video",
    val summary: SummaryItem? = null,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val statusMessage: String = "",
    val error: String? = null,
    val isApiKeyMissing: Boolean = false,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage("Hi! I'm your video assistant. Ask me anything about this video, and I'll answer using the transcript!", isUser = false)
    ),
    val isChatLoading: Boolean = false,
    val chatInputText: String = ""
)

/**
 * ViewModel orchestrating sequential loading and API submission.
 */
class OverlayViewModel(
    private val repository: SummaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    private var activeTranscriptText: String? = null

    /**
     * Executes the background loading sequence, notifying UI at each step.
     */
    fun loadSummary(sharedText: String?, forceRegenerate: Boolean = false) {
        if (sharedText.isNullOrBlank()) {
            _uiState.update { it.copy(error = "No shared link detected. Tap share in YouTube.") }
            return
        }

        val videoId = YoutubeParser.extractVideoId(sharedText)
        if (videoId == null) {
            _uiState.update { it.copy(error = "Could not extract a valid YouTube video ID from the shared content.") }
            return
        }

        val sharedTitle = YoutubeParser.extractTitle(sharedText)
        _uiState.update { 
            it.copy(
                videoId = videoId,
                title = sharedTitle,
                isLoading = true,
                isGenerating = false,
                error = null,
                isApiKeyMissing = false,
                summary = null,
                chatMessages = listOf(
                    ChatMessage("Hi! I'm your video assistant. Ask me anything about this video, and I'll answer using the transcript!", isUser = false)
                ),
                chatInputText = ""
            )
        }
        activeTranscriptText = null

        viewModelScope.launch {
            try {
                // Safeguard: Check if API Key has been configured
                val apiKey = repository.getApiKey()
                if (apiKey.isBlank()) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isApiKeyMissing = true,
                            error = "Google AI Studio API Key is not set. Please open the main app to configure your key."
                        )
                    }
                    return@launch
                }

                // Step 1: Check local cache first (if not forcing regeneration)
                if (!forceRegenerate) {
                    _uiState.update { it.copy(statusMessage = "Checking local database...") }
                    val cached = repository.getLocalSummary(videoId)
                    if (cached != null) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                summary = cached,
                                title = cached.title,
                                statusMessage = "Loaded from cache!"
                            )
                        }
                        return@launch
                    }
                }

                // Step 2: Native HTML extraction
                _uiState.update { it.copy(statusMessage = "Scraping subtitle tracks...") }
                val transcriptResult = repository.fetchTranscript(videoId)
                if (transcriptResult.isFailure) {
                    val ex = transcriptResult.exceptionOrNull()
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = ex?.message ?: "No subtitle tracks available for this video. Make sure captions are enabled."
                        )
                    }
                    return@launch
                }

                val transcript = transcriptResult.getOrThrow()
                activeTranscriptText = transcript.getFullText()
                // Prefer exact title scraped from YouTube header over guessed share title
                val actualTitle = if (transcript.title.isNotBlank()) transcript.title else sharedTitle
                
                // Step 3: Run Gemini AI Content Generation Stream
                _uiState.update { 
                    it.copy(
                        title = actualTitle,
                        statusMessage = "Generating...",
                        isLoading = false,
                        isGenerating = true
                    ) 
                }
                
                // Set an initial empty summary item so the UI shows the markdown text container
                var currentSummary = SummaryItem(videoId, actualTitle, "", System.currentTimeMillis())
                _uiState.update { it.copy(summary = currentSummary) }
                
                try {
                    repository.generateSummaryStream(videoId, actualTitle, transcript.getFullText())
                        .collect { textChunk ->
                            currentSummary = currentSummary.copy(summary = textChunk)
                            _uiState.update { 
                                it.copy(summary = currentSummary)
                            }
                        }
                        
                    _uiState.update { 
                        it.copy(
                            isGenerating = false,
                            statusMessage = "Completed!"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { 
                        it.copy(
                            isGenerating = false,
                            error = e.message ?: "AI summarization failed. Please verify your internet connection or API Key."
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isGenerating = false,
                        error = e.message ?: "An unexpected error occurred."
                    )
                }
            }
        }
    }

    fun updateChatInput(text: String) {
        _uiState.update { it.copy(chatInputText = text) }
    }

    fun clearChatHistory() {
        _uiState.update { 
            it.copy(
                chatMessages = listOf(
                    ChatMessage("Hi! I'm your video assistant. Ask me anything about this video, and I'll answer using the transcript!", isUser = false)
                )
            )
        }
    }

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        val videoId = _uiState.value.videoId ?: return

        val userMessage = ChatMessage(content = message, isUser = true)
        val updatedMessages = _uiState.value.chatMessages + userMessage
        _uiState.update { 
            it.copy(
                chatMessages = updatedMessages,
                chatInputText = ""
            ) 
        }

        viewModelScope.launch {
            try {
                // Safeguard: Ensure we have transcript text in memory
                if (activeTranscriptText == null) {
                    _uiState.update { it.copy(isChatLoading = true) }
                    val transcriptResult = repository.fetchTranscript(videoId)
                    if (transcriptResult.isFailure) {
                        val errMsg = transcriptResult.exceptionOrNull()?.message 
                            ?: "Failed to scrape transcripts. Make sure captions/subtitles are available for this video."
                        _uiState.update { 
                            it.copy(
                                isChatLoading = false,
                                chatMessages = it.chatMessages + ChatMessage(
                                    content = "Could not fetch transcript context: $errMsg",
                                    isUser = false
                                )
                            )
                        }
                        return@launch
                    }
                    val transcript = transcriptResult.getOrThrow()
                    activeTranscriptText = transcript.getFullText()
                    _uiState.update { it.copy(isChatLoading = false) }
                }

                val transcriptText = activeTranscriptText ?: return@launch

                // Append initial streaming response placeholder
                val aiPlaceholder = ChatMessage(content = "", isUser = false, isStreaming = true)
                _uiState.update { it.copy(chatMessages = it.chatMessages + aiPlaceholder) }

                try {
                    repository.generateChatStream(transcriptText, updatedMessages, message)
                        .collect { textChunk ->
                            _uiState.update { state ->
                                val messages = state.chatMessages.toMutableList()
                                if (messages.isNotEmpty()) {
                                    val lastIndex = messages.lastIndex
                                    messages[lastIndex] = messages[lastIndex].copy(content = textChunk)
                                }
                                state.copy(chatMessages = messages)
                            }
                        }

                    _uiState.update { state ->
                        val messages = state.chatMessages.toMutableList()
                        if (messages.isNotEmpty()) {
                            val lastIndex = messages.lastIndex
                            messages[lastIndex] = messages[lastIndex].copy(isStreaming = false)
                        }
                        state.copy(chatMessages = messages)
                    }
                } catch (e: Exception) {
                    _uiState.update { state ->
                        val messages = state.chatMessages.toMutableList()
                        if (messages.isNotEmpty()) {
                            val lastIndex = messages.lastIndex
                            messages[lastIndex] = messages[lastIndex].copy(
                                content = "AI reply interrupted: ${e.message ?: "Failed to generate answer. Check API key."}",
                                isStreaming = false
                            )
                        }
                        state.copy(chatMessages = messages)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages + ChatMessage(
                            content = "An unexpected error occurred: ${e.message}",
                            isUser = false
                        )
                    )
                }
            }
        }
    }

    /**
     * Clean Factory pattern utilizing ServiceLocator dependencies.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OverlayViewModel(
                repository = ServiceLocator.getRepository(context)
            ) as T
        }
    }
}

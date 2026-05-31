package com.summarizer.app.presentation.overlay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.summarizer.app.core.di.ServiceLocator
import com.summarizer.app.core.utils.YoutubeParser
import com.summarizer.app.domain.model.SummaryItem
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
    val statusMessage: String = "",
    val error: String? = null,
    val isApiKeyMissing: Boolean = false
)

/**
 * ViewModel orchestrating sequential loading and API submission.
 */
class OverlayViewModel(
    private val repository: SummaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

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
                error = null,
                isApiKeyMissing = false,
                summary = null
            )
        }

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
                // Prefer exact title scraped from YouTube header over guessed share title
                val actualTitle = if (transcript.title.isNotBlank()) transcript.title else sharedTitle
                
                // Step 3: Run Gemini AI Content Generation Stream
                _uiState.update { 
                    it.copy(
                        title = actualTitle,
                        statusMessage = "Generating..."
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
                            isLoading = false,
                            statusMessage = "Completed!"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "AI summarization failed. Please verify your internet connection or API Key."
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred."
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

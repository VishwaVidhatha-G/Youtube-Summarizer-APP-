package com.summarizer.app.presentation.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.summarizer.app.core.di.ServiceLocator
import com.summarizer.app.domain.model.SummaryItem
import com.summarizer.app.domain.usecase.DeleteSummaryUseCase
import com.summarizer.app.domain.usecase.GetHistoryUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state representing cached history summaries and search status.
 */
data class HistoryUiState(
    val summaries: List<SummaryItem> = emptyList(),
    val filteredSummaries: List<SummaryItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

/**
 * ViewModel orchestrating reactive list updates and search query filters.
 */
class HistoryViewModel(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val deleteSummaryUseCase: DeleteSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Collects reactive DB Flow updates, feeding state flows safely.
     */
    private fun loadHistory() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getHistoryUseCase().collect { items ->
                _uiState.update { state ->
                    state.copy(
                        summaries = items,
                        filteredSummaries = filterSummaries(items, state.searchQuery),
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Updates the active list view based on the search query.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredSummaries = filterSummaries(state.summaries, query)
            )
        }
    }

    /**
     * Asynchronously deletes a cached summary by its video ID.
     */
    fun deleteSummary(videoId: String) {
        viewModelScope.launch {
            deleteSummaryUseCase(videoId)
        }
    }

    private fun filterSummaries(list: List<SummaryItem>, query: String): List<SummaryItem> {
        if (query.isBlank()) return list
        return list.filter {
            it.title.contains(query, ignoreCase = true) || it.summary.contains(query, ignoreCase = true)
        }
    }

    /**
     * Clean Factory pattern utilizing ServiceLocator dependencies.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(
                getHistoryUseCase = ServiceLocator.provideGetHistoryUseCase(context),
                deleteSummaryUseCase = ServiceLocator.provideDeleteSummaryUseCase(context)
            ) as T
        }
    }
}

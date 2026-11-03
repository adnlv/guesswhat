package com.adnlv.guesswhat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adnlv.guesswhat.data.NewsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = merge(
        _searchQuery
            .debounce { query -> if (query.isBlank()) 0L else 300L }
            .map { it.trim() }
            .distinctUntilChanged(),
        retryTrigger.map { _searchQuery.value.trim() }
    )
        .flatMapLatest { query ->
            flow {
                emit(HomeUiState.Loading)
                val result = if (query.isEmpty()) {
                    repository.getTopHeadlines()
                } else {
                    repository.searchNews(query)
                }
                result
                    .onSuccess { articles ->
                        emit(HomeUiState.Success(articles))
                    }
                    .onFailure { throwable ->
                        emit(
                            HomeUiState.Error(
                                throwable.message ?: "Failed to load news"
                            )
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeUiState.Loading
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    fun loadNews() {
        retry()
    }

    companion object {
        fun provideFactory(repository: NewsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(repository)
            }
        }
    }
}

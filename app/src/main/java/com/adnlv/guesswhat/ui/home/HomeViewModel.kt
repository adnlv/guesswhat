package com.adnlv.guesswhat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.adnlv.guesswhat.data.NewsRepository
import com.adnlv.guesswhat.model.Article
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class HomeViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val articles: Flow<PagingData<Article>> = _searchQuery
        .debounce { query -> if (query.isBlank()) 0L else 300L }
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getTopHeadlinesStream()
            } else {
                repository.searchNewsStream(query)
            }
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    companion object {
        fun provideFactory(repository: NewsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(repository)
            }
        }
    }
}

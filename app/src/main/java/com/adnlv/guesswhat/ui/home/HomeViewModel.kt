package com.adnlv.guesswhat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adnlv.guesswhat.data.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            repository.getTopHeadlines()
                .onSuccess { articles ->
                    _uiState.value = HomeUiState.Success(articles)
                }
                .onFailure { throwable ->
                    _uiState.value = HomeUiState.Error(
                        throwable.message ?: "Failed to load news"
                    )
                }
        }
    }

    companion object {
        fun provideFactory(repository: NewsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(repository)
            }
        }
    }
}

package com.adnlv.guesswhat.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adnlv.guesswhat.data.NewsRepository
import com.adnlv.guesswhat.model.NewsCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

class CategoriesViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(NewsCategory.DEFAULT)
    val selectedCategory: StateFlow<NewsCategory> = _selectedCategory.asStateFlow()

    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CategoriesUiState> = merge(
        _selectedCategory,
        retryTrigger.map { _selectedCategory.value }
    )
        .flatMapLatest { category ->
            flow {
                emit(CategoriesUiState.Loading)
                val result = repository.getTopHeadlines(category = category.apiValue)
                result
                    .onSuccess { articles ->
                        emit(CategoriesUiState.Success(articles))
                    }
                    .onFailure { throwable ->
                        emit(
                            CategoriesUiState.Error(
                                throwable.message ?: "Failed to load news"
                            )
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CategoriesUiState.Loading
        )

    fun selectCategory(category: NewsCategory) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
        }
    }

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    companion object {
        fun provideFactory(repository: NewsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CategoriesViewModel(repository)
            }
        }
    }
}

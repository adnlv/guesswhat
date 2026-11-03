package com.adnlv.guesswhat.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.adnlv.guesswhat.data.NewsRepository
import com.adnlv.guesswhat.model.Article
import com.adnlv.guesswhat.model.NewsCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class CategoriesViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(NewsCategory.DEFAULT)
    val selectedCategory: StateFlow<NewsCategory> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val articles: Flow<PagingData<Article>> = _selectedCategory
        .flatMapLatest { category ->
            repository.getTopHeadlinesStream(category = category.apiValue)
        }
        .cachedIn(viewModelScope)

    fun selectCategory(category: NewsCategory) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
        }
    }

    companion object {
        fun provideFactory(repository: NewsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CategoriesViewModel(repository)
            }
        }
    }
}

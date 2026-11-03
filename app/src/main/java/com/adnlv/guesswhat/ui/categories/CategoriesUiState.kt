package com.adnlv.guesswhat.ui.categories

import com.adnlv.guesswhat.model.Article

sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState
    data class Success(val articles: List<Article>) : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}

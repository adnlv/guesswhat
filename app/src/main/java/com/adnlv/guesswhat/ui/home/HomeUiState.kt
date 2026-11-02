package com.adnlv.guesswhat.ui.home

import com.adnlv.guesswhat.model.Article

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val articles: List<Article>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

package com.adnlv.guesswhat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewsResponseDto(
    val status: String,
    val totalResults: Int? = null,
    val articles: List<ArticleDto> = emptyList(),
    val code: String? = null,
    val message: String? = null
)

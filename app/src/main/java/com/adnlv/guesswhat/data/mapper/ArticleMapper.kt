package com.adnlv.guesswhat.data.mapper

import com.adnlv.guesswhat.data.remote.dto.ArticleDto
import com.adnlv.guesswhat.model.Article

fun ArticleDto.toDomain(): Article? {
    val cleanTitle = title?.trim() ?: return null
    if (cleanTitle.equals("[Removed]", ignoreCase = true) || cleanTitle.isBlank()) {
        return null
    }
    if (url.isBlank()) {
        return null
    }

    return Article(
        title = cleanTitle,
        description = description?.trim().orEmpty(),
        url = url.trim(),
        imageUrl = urlToImage?.trim()?.takeIf { it.isNotBlank() },
        sourceName = source?.name?.trim() ?: "Unknown Source",
        publishedAt = publishedAt?.trim().orEmpty()
    )
}

fun List<ArticleDto>.toDomain(): List<Article> = mapNotNull { it.toDomain() }

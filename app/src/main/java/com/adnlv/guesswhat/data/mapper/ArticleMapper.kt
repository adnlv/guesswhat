package com.adnlv.guesswhat.data.mapper

import com.adnlv.guesswhat.data.local.ArticleEntity
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

fun ArticleDto.toEntity(feedKey: String, position: Int): ArticleEntity? {
    val article = toDomain() ?: return null
    return ArticleEntity(
        feedKey = feedKey,
        url = article.url,
        position = position,
        title = article.title,
        description = article.description,
        imageUrl = article.imageUrl,
        sourceName = article.sourceName,
        publishedAt = article.publishedAt
    )
}

fun List<ArticleDto>.toEntity(feedKey: String, startPosition: Int = 0): List<ArticleEntity> {
    var currentPosition = startPosition
    return mapNotNull { dto ->
        dto.toEntity(feedKey = feedKey, position = currentPosition)?.also { currentPosition++ }
    }
}

fun ArticleEntity.toDomain(): Article = Article(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    sourceName = sourceName,
    publishedAt = publishedAt
)

@JvmName("entityListToDomain")
fun List<ArticleEntity>.toDomain(): List<Article> = map { it.toDomain() }

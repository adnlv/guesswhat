package com.adnlv.guesswhat.data

import com.adnlv.guesswhat.data.mapper.toDomain
import com.adnlv.guesswhat.data.remote.NewsApi
import com.adnlv.guesswhat.model.Article

class NewsRepository(private val newsApi: NewsApi) {

    suspend fun getTopHeadlines(country: String = "us", category: String? = null): Result<List<Article>> = runCatching {
        val response = newsApi.getTopHeadlines(
            country = country,
            category = category
        )
        if (response.status != "ok") {
            val message = response.message ?: "Failed to fetch news"
            throw IllegalStateException(message)
        }
        response.articles.toDomain()
    }

    suspend fun searchNews(query: String): Result<List<Article>> = runCatching {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return getTopHeadlines()
        }
        val response = newsApi.searchNews(query = trimmedQuery)
        if (response.status != "ok") {
            val message = response.message ?: "Failed to search news"
            throw IllegalStateException(message)
        }
        response.articles.toDomain()
    }
}

package com.adnlv.guesswhat.data

import com.adnlv.guesswhat.data.local.ArticleDao
import com.adnlv.guesswhat.data.mapper.toDomain
import com.adnlv.guesswhat.data.mapper.toEntity
import com.adnlv.guesswhat.data.remote.NewsApi
import com.adnlv.guesswhat.model.Article
import kotlinx.coroutines.CancellationException

class NewsRepository(private val newsApi: NewsApi, private val articleDao: ArticleDao) {

    suspend fun getTopHeadlines(country: String = "us", category: String? = null): Result<List<Article>> = try {
        val response = newsApi.getTopHeadlines(
            country = country,
            category = category
        )
        if (response.status != "ok") {
            val message = response.message ?: "Failed to fetch news"
            throw IllegalStateException(message)
        }
        val entities = response.articles.toEntity(category = category)
        articleDao.deleteHeadlines(category = category)
        articleDao.insertArticles(entities)
        Result.success(articleDao.getHeadlines(category = category).toDomain())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        val cached = articleDao.getHeadlines(category = category).toDomain()
        if (cached.isNotEmpty()) {
            Result.success(cached)
        } else {
            Result.failure(e)
        }
    }

    suspend fun searchNews(query: String): Result<List<Article>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return getTopHeadlines()
        }
        val normalizedQuery = trimmedQuery.lowercase()
        return try {
            val response = newsApi.searchNews(query = trimmedQuery)
            if (response.status != "ok") {
                val message = response.message ?: "Failed to search news"
                throw IllegalStateException(message)
            }
            val entities = response.articles.toEntity(searchQuery = normalizedQuery)
            articleDao.deleteSearchArticles(query = normalizedQuery)
            articleDao.insertArticles(entities)
            Result.success(articleDao.searchArticles(query = normalizedQuery).toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val cached = articleDao.searchArticles(query = normalizedQuery).toDomain()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }
}

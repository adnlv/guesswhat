package com.adnlv.guesswhat.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.adnlv.guesswhat.data.local.ArticleDao
import com.adnlv.guesswhat.data.local.NewsDatabase
import com.adnlv.guesswhat.data.local.RemoteKeys
import com.adnlv.guesswhat.data.local.RemoteKeysDao
import com.adnlv.guesswhat.data.mapper.toDomain
import com.adnlv.guesswhat.data.mapper.toEntity
import com.adnlv.guesswhat.data.mediator.NewsRemoteMediator
import com.adnlv.guesswhat.data.remote.NewsApi
import com.adnlv.guesswhat.model.Article
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NewsRepository(
    private val newsApi: NewsApi,
    private val articleDao: ArticleDao,
    private val remoteKeysDao: RemoteKeysDao = NoOpRemoteKeysDao,
    private val withTransaction: suspend (suspend () -> Unit) -> Unit = { it() }
) {

    constructor(
        newsApi: NewsApi,
        database: NewsDatabase
    ) : this(
        newsApi = newsApi,
        articleDao = database.articleDao(),
        remoteKeysDao = database.remoteKeysDao(),
        withTransaction = { block -> database.withTransaction(block) }
    )

    @OptIn(ExperimentalPagingApi::class)
    fun getTopHeadlinesStream(country: String = "us", category: String? = null): Flow<PagingData<Article>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            enablePlaceholders = false
        ),
        remoteMediator = NewsRemoteMediator(
            newsApi = newsApi,
            articleDao = articleDao,
            remoteKeysDao = remoteKeysDao,
            category = category,
            searchQuery = null,
            country = country,
            withTransaction = withTransaction
        ),
        pagingSourceFactory = {
            articleDao.getHeadlinesPagingSource(category = category)
        }
    ).flow.map { pagingData ->
        pagingData.map { entity -> entity.toDomain() }
    }

    @OptIn(ExperimentalPagingApi::class)
    fun searchNewsStream(query: String): Flow<PagingData<Article>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return getTopHeadlinesStream()
        }
        val normalizedQuery = trimmedQuery.lowercase()
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = NewsRemoteMediator(
                newsApi = newsApi,
                articleDao = articleDao,
                remoteKeysDao = remoteKeysDao,
                category = null,
                searchQuery = trimmedQuery,
                withTransaction = withTransaction
            ),
            pagingSourceFactory = {
                articleDao.searchArticlesPagingSource(query = normalizedQuery)
            }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

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

    private object NoOpRemoteKeysDao : RemoteKeysDao {
        override suspend fun insertAll(remoteKey: List<RemoteKeys>) = Unit
        override suspend fun getRemoteKeys(url: String): RemoteKeys? = null
        override suspend fun deleteHeadlinesKeys(category: String?) = Unit
        override suspend fun deleteSearchKeys(query: String) = Unit
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}

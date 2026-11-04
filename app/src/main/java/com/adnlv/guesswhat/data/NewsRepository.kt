package com.adnlv.guesswhat.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.adnlv.guesswhat.data.local.ArticleDao
import com.adnlv.guesswhat.data.local.RemoteKeysDao
import com.adnlv.guesswhat.data.mapper.toDomain
import com.adnlv.guesswhat.data.mediator.NewsRemoteMediator
import com.adnlv.guesswhat.data.remote.NewsApi
import com.adnlv.guesswhat.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NewsRepository(
    private val newsApi: NewsApi,
    private val articleDao: ArticleDao,
    private val remoteKeysDao: RemoteKeysDao,
    private val withTransaction: suspend (suspend () -> Unit) -> Unit
) {

    @OptIn(ExperimentalPagingApi::class)
    fun getTopHeadlinesStream(country: String = "us", category: String? = null): Flow<PagingData<Article>> {
        val feedKey = FeedKeys.headlines(country = country, category = category)
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = NewsRemoteMediator(
                feedKey = feedKey,
                newsApi = newsApi,
                articleDao = articleDao,
                remoteKeysDao = remoteKeysDao,
                withTransaction = withTransaction
            ),
            pagingSourceFactory = {
                articleDao.pagingSource(feedKey = feedKey)
            }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    fun searchNewsStream(query: String): Flow<PagingData<Article>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return getTopHeadlinesStream()
        }
        val feedKey = FeedKeys.search(trimmedQuery)
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = NewsRemoteMediator(
                feedKey = feedKey,
                newsApi = newsApi,
                articleDao = articleDao,
                remoteKeysDao = remoteKeysDao,
                withTransaction = withTransaction
            ),
            pagingSourceFactory = {
                articleDao.pagingSource(feedKey = feedKey)
            }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}

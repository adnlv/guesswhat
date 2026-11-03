package com.adnlv.guesswhat.data.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.adnlv.guesswhat.data.local.ArticleDao
import com.adnlv.guesswhat.data.local.ArticleEntity
import com.adnlv.guesswhat.data.local.RemoteKeys
import com.adnlv.guesswhat.data.local.RemoteKeysDao
import com.adnlv.guesswhat.data.remote.NewsApi
import com.adnlv.guesswhat.data.remote.dto.ArticleDto
import com.adnlv.guesswhat.data.remote.dto.NewsResponseDto
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediatorTest {

    private class FakeNewsApi(
        var responseToReturn: NewsResponseDto? = null,
        var exceptionToThrow: Throwable? = null,
        var lastCategory: String? = null,
        var lastSearchQuery: String? = null,
        var lastPage: Int? = null
    ) : NewsApi {
        override suspend fun getTopHeadlines(
            country: String?,
            category: String?,
            query: String?,
            page: Int?,
            pageSize: Int?
        ): NewsResponseDto {
            lastCategory = category
            lastPage = page
            exceptionToThrow?.let { throw it }
            return responseToReturn ?: NewsResponseDto(status = "ok")
        }

        override suspend fun searchNews(query: String, language: String?, page: Int?, pageSize: Int?): NewsResponseDto {
            lastSearchQuery = query
            lastPage = page
            exceptionToThrow?.let { throw it }
            return responseToReturn ?: NewsResponseDto(status = "ok")
        }
    }

    private class FakeArticleDao : ArticleDao {
        val articles = LinkedHashMap<String, ArticleEntity>()

        override fun getHeadlinesPagingSource(category: String?): PagingSource<Int, ArticleEntity> =
            throw UnsupportedOperationException()

        override fun searchArticlesPagingSource(query: String): PagingSource<Int, ArticleEntity> =
            throw UnsupportedOperationException()

        override suspend fun getHeadlines(category: String?): List<ArticleEntity> = articles.values.filter {
            (category == null && it.category == null && it.searchQuery == null) ||
                (it.category == category && it.searchQuery == null)
        }

        override suspend fun searchArticles(query: String): List<ArticleEntity> = articles.values.filter {
            it.searchQuery == query
        }

        override suspend fun insertArticles(articles: List<ArticleEntity>) {
            articles.forEach { this.articles[it.url] = it }
        }

        override suspend fun deleteHeadlines(category: String?) {
            val urlsToRemove = getHeadlines(category).map { it.url }
            urlsToRemove.forEach { articles.remove(it) }
        }

        override suspend fun deleteSearchArticles(query: String) {
            val urlsToRemove = articles.values.filter { it.searchQuery == query }.map { it.url }
            urlsToRemove.forEach { articles.remove(it) }
        }
    }

    private class FakeRemoteKeysDao : RemoteKeysDao {
        val keys = LinkedHashMap<String, RemoteKeys>()

        override suspend fun insertAll(remoteKey: List<RemoteKeys>) {
            remoteKey.forEach { keys[it.articleUrl] = it }
        }

        override suspend fun getRemoteKeys(url: String): RemoteKeys? = keys[url]

        override suspend fun deleteHeadlinesKeys(category: String?) {
            val urlsToRemove = keys.values.filter {
                (category == null && it.category == null && it.searchQuery == null) ||
                    (it.category == category && it.searchQuery == null)
            }.map { it.articleUrl }
            urlsToRemove.forEach { keys.remove(it) }
        }

        override suspend fun deleteSearchKeys(query: String) {
            val urlsToRemove = keys.values.filter { it.searchQuery == query }.map { it.articleUrl }
            urlsToRemove.forEach { keys.remove(it) }
        }
    }

    private fun emptyPagingState(): PagingState<Int, ArticleEntity> = PagingState(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0
    )

    @Test
    fun load_prepend_returnsSuccessWithEndOfPaginationTrue() = runBlocking {
        val mediator = NewsRemoteMediator(
            newsApi = FakeNewsApi(),
            articleDao = FakeArticleDao(),
            remoteKeysDao = FakeRemoteKeysDao()
        )

        val result = mediator.load(LoadType.PREPEND, emptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun load_refreshWithArticles_insertsEntitiesAndKeysInTransaction() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "ok",
                totalResults = 2,
                articles = listOf(
                    ArticleDto(title = "Article 1", url = "https://example.com/1"),
                    ArticleDto(title = "Article 2", url = "https://example.com/2")
                )
            )
        )
        val fakeArticleDao = FakeArticleDao()
        val fakeKeysDao = FakeRemoteKeysDao()
        var transactionCalled = false

        val mediator = NewsRemoteMediator(
            newsApi = fakeApi,
            articleDao = fakeArticleDao,
            remoteKeysDao = fakeKeysDao,
            withTransaction = { block ->
                transactionCalled = true
                block()
            }
        )

        val result = mediator.load(LoadType.REFRESH, emptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertTrue(transactionCalled)
        assertEquals(2, fakeArticleDao.getHeadlines(null).size)
        assertEquals(2, fakeKeysDao.getRemoteKeys("https://example.com/1")?.nextKey)
        assertEquals(null, fakeKeysDao.getRemoteKeys("https://example.com/1")?.prevKey)
    }

    @Test
    fun load_refreshWithEmptyArticles_returnsEndOfPaginationTrue() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "ok",
                totalResults = 0,
                articles = emptyList()
            )
        )
        val mediator = NewsRemoteMediator(
            newsApi = fakeApi,
            articleDao = FakeArticleDao(),
            remoteKeysDao = FakeRemoteKeysDao()
        )

        val result = mediator.load(LoadType.REFRESH, emptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun load_refreshOnNetworkException_returnsMediatorError() = runBlocking {
        val fakeApi = FakeNewsApi(
            exceptionToThrow = IOException("No network")
        )
        val mediator = NewsRemoteMediator(
            newsApi = fakeApi,
            articleDao = FakeArticleDao(),
            remoteKeysDao = FakeRemoteKeysDao()
        )

        val result = mediator.load(LoadType.REFRESH, emptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals("No network", (result as RemoteMediator.MediatorResult.Error).throwable.message)
    }

    @Test
    fun load_appendWithNextKey_loadsNextPageAndStoresUpdatedKeys() = runBlocking {
        val firstEntity = ArticleEntity(
            url = "https://example.com/1",
            title = "Page 1 Article",
            description = "Desc",
            imageUrl = null,
            sourceName = "Source",
            publishedAt = "2026-11-03T06:00:00Z"
        )
        val fakeArticleDao = FakeArticleDao().apply {
            insertArticles(listOf(firstEntity))
        }
        val fakeKeysDao = FakeRemoteKeysDao().apply {
            insertAll(
                listOf(
                    RemoteKeys(
                        articleUrl = "https://example.com/1",
                        prevKey = null,
                        nextKey = 2
                    )
                )
            )
        }
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "ok",
                totalResults = 2,
                articles = listOf(
                    ArticleDto(title = "Page 2 Article", url = "https://example.com/2")
                )
            )
        )
        val mediator = NewsRemoteMediator(
            newsApi = fakeApi,
            articleDao = fakeArticleDao,
            remoteKeysDao = fakeKeysDao
        )
        val pagingState = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(firstEntity),
                    prevKey = null,
                    nextKey = 2
                )
            ),
            anchorPosition = 0,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )

        val result = mediator.load(LoadType.APPEND, pagingState)

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(2, fakeApi.lastPage)
        assertEquals(3, fakeKeysDao.getRemoteKeys("https://example.com/2")?.nextKey)
        assertEquals(1, fakeKeysDao.getRemoteKeys("https://example.com/2")?.prevKey)
    }

    @Test
    fun load_withSearchQuery_callsSearchEndpointAndStoresNormalizedQuery() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "ok",
                totalResults = 1,
                articles = listOf(
                    ArticleDto(title = "Kotlin Paging", url = "https://example.com/paging")
                )
            )
        )
        val fakeArticleDao = FakeArticleDao()
        val fakeKeysDao = FakeRemoteKeysDao()
        val mediator = NewsRemoteMediator(
            newsApi = fakeApi,
            articleDao = fakeArticleDao,
            remoteKeysDao = fakeKeysDao,
            searchQuery = "  Kotlin  "
        )

        val result = mediator.load(LoadType.REFRESH, emptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals("Kotlin", fakeApi.lastSearchQuery)
        assertEquals(1, fakeArticleDao.searchArticles("kotlin").size)
        assertEquals("kotlin", fakeKeysDao.getRemoteKeys("https://example.com/paging")?.searchQuery)
    }
}

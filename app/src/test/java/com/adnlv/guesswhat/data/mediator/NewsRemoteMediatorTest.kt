package com.adnlv.guesswhat.data.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.adnlv.guesswhat.data.FeedKeys
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
        val articles = LinkedHashMap<Pair<String, String>, ArticleEntity>()

        override fun pagingSource(feedKey: String): PagingSource<Int, ArticleEntity> = createPagingSource {
            articles.values
                .filter { it.feedKey == feedKey }
                .sortedBy { it.position }
        }

        fun getFeedArticles(feedKey: String): List<ArticleEntity> = articles.values
            .filter { it.feedKey == feedKey }
            .sortedBy { it.position }

        override suspend fun insertArticles(articles: List<ArticleEntity>) {
            articles.forEach { this.articles[Pair(it.feedKey, it.url)] = it }
        }

        override suspend fun deleteFeed(feedKey: String) {
            val keysToRemove = articles.keys.filter { it.first == feedKey }
            keysToRemove.forEach { articles.remove(it) }
        }

        private fun createPagingSource(itemsProvider: () -> List<ArticleEntity>): PagingSource<Int, ArticleEntity> =
            object : PagingSource<Int, ArticleEntity>() {
                override fun getRefreshKey(state: PagingState<Int, ArticleEntity>): Int? = null

                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ArticleEntity> = LoadResult.Page(
                    data = itemsProvider(),
                    prevKey = null,
                    nextKey = null
                )
            }
    }

    private class FakeRemoteKeysDao : RemoteKeysDao {
        val keys = LinkedHashMap<Pair<String, String>, RemoteKeys>()

        override suspend fun insertAll(remoteKeys: List<RemoteKeys>) {
            remoteKeys.forEach { keys[Pair(it.feedKey, it.articleUrl)] = it }
        }

        override suspend fun getRemoteKeys(feedKey: String, url: String): RemoteKeys? = keys[Pair(feedKey, url)]

        override suspend fun deleteFeedKeys(feedKey: String) {
            val keysToRemove = keys.keys.filter { it.first == feedKey }
            keysToRemove.forEach { keys.remove(it) }
        }
    }

    private val noTransaction: suspend (suspend () -> Unit) -> Unit = { it() }

    private fun emptyPagingState(): PagingState<Int, ArticleEntity> = PagingState(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0
    )

    @Test
    fun load_prepend_returnsSuccessWithEndOfPaginationTrue() = runBlocking {
        val mediator = NewsRemoteMediator(
            feedKey = FeedKeys.headlines(),
            newsApi = FakeNewsApi(),
            articleDao = FakeArticleDao(),
            remoteKeysDao = FakeRemoteKeysDao(),
            withTransaction = noTransaction
        )

        val result = mediator.load(LoadType.PREPEND, emptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun load_refreshWithArticles_insertsEntitiesAndKeysInTransaction() = runBlocking {
        val defaultFeedKey = FeedKeys.headlines()
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
            feedKey = defaultFeedKey,
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
        assertEquals(2, fakeArticleDao.getFeedArticles(defaultFeedKey).size)
        assertEquals(2, fakeKeysDao.getRemoteKeys(defaultFeedKey, "https://example.com/1")?.nextKey)
        assertEquals(null, fakeKeysDao.getRemoteKeys(defaultFeedKey, "https://example.com/1")?.prevKey)
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
            feedKey = FeedKeys.headlines(),
            newsApi = fakeApi,
            articleDao = FakeArticleDao(),
            remoteKeysDao = FakeRemoteKeysDao(),
            withTransaction = noTransaction
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
            feedKey = FeedKeys.headlines(),
            newsApi = fakeApi,
            articleDao = FakeArticleDao(),
            remoteKeysDao = FakeRemoteKeysDao(),
            withTransaction = noTransaction
        )

        val result = mediator.load(LoadType.REFRESH, emptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals("No network", (result as RemoteMediator.MediatorResult.Error).throwable.message)
    }

    @Test
    fun load_appendWithNextKey_loadsNextPageAndStoresUpdatedKeys() = runBlocking {
        val defaultFeedKey = FeedKeys.headlines()
        val firstEntity = ArticleEntity(
            feedKey = defaultFeedKey,
            url = "https://example.com/1",
            position = 0,
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
                        feedKey = defaultFeedKey,
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
            feedKey = defaultFeedKey,
            newsApi = fakeApi,
            articleDao = fakeArticleDao,
            remoteKeysDao = fakeKeysDao,
            withTransaction = noTransaction
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
        assertEquals(3, fakeKeysDao.getRemoteKeys(defaultFeedKey, "https://example.com/2")?.nextKey)
        assertEquals(1, fakeKeysDao.getRemoteKeys(defaultFeedKey, "https://example.com/2")?.prevKey)
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
        val feedKey = FeedKeys.search("  Kotlin  ")
        val mediator = NewsRemoteMediator(
            feedKey = feedKey,
            newsApi = fakeApi,
            articleDao = fakeArticleDao,
            remoteKeysDao = fakeKeysDao,
            withTransaction = noTransaction
        )

        val result = mediator.load(LoadType.REFRESH, emptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals("kotlin", fakeApi.lastSearchQuery)
        assertEquals(1, fakeArticleDao.getFeedArticles(feedKey).size)
        assertEquals(feedKey, fakeKeysDao.getRemoteKeys(feedKey, "https://example.com/paging")?.feedKey)
    }
}

package com.adnlv.guesswhat.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.asSnapshot
import com.adnlv.guesswhat.data.local.ArticleDao
import com.adnlv.guesswhat.data.local.ArticleEntity
import com.adnlv.guesswhat.data.local.RemoteKeys
import com.adnlv.guesswhat.data.local.RemoteKeysDao
import com.adnlv.guesswhat.data.remote.NewsApi
import com.adnlv.guesswhat.data.remote.dto.ArticleDto
import com.adnlv.guesswhat.data.remote.dto.NewsResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRepositoryTest {

    private class FakeNewsApi(
        var responseToReturn: NewsResponseDto? = null,
        var exceptionToThrow: Throwable? = null,
        var lastSearchQuery: String? = null,
        var lastCategory: String? = null,
        var getTopHeadlinesCalled: Boolean = false
    ) : NewsApi {
        override suspend fun getTopHeadlines(
            country: String?,
            category: String?,
            query: String?,
            page: Int?,
            pageSize: Int?
        ): NewsResponseDto {
            getTopHeadlinesCalled = true
            lastCategory = category
            exceptionToThrow?.let { throw it }
            if (page != null && page > 1) {
                return NewsResponseDto(status = "ok", totalResults = 0, articles = emptyList())
            }
            return responseToReturn ?: NewsResponseDto(status = "ok")
        }

        override suspend fun searchNews(query: String, language: String?, page: Int?, pageSize: Int?): NewsResponseDto {
            lastSearchQuery = query
            exceptionToThrow?.let { throw it }
            if (page != null && page > 1) {
                return NewsResponseDto(status = "ok", totalResults = 0, articles = emptyList())
            }
            return responseToReturn ?: NewsResponseDto(status = "ok")
        }
    }

    private class FakeArticleDao(initialArticles: List<ArticleEntity> = emptyList()) : ArticleDao {
        private val articles = LinkedHashMap<Pair<String, String>, ArticleEntity>().apply {
            initialArticles.forEach { put(Pair(it.feedKey, it.url), it) }
        }
        private var currentPagingSource: PagingSource<Int, ArticleEntity>? = null

        override fun pagingSource(feedKey: String): PagingSource<Int, ArticleEntity> = createPagingSource {
            articles.values
                .filter { it.feedKey == feedKey }
                .sortedBy { it.position }
        }

        fun getFeedArticles(feedKey: String): List<ArticleEntity> = articles.values
            .filter { it.feedKey == feedKey }
            .sortedBy { it.position }

        override suspend fun insertArticles(articles: List<ArticleEntity>) {
            if (articles.isNotEmpty()) {
                articles.forEach { this.articles[Pair(it.feedKey, it.url)] = it }
                currentPagingSource?.invalidate()
            }
        }

        override suspend fun deleteFeed(feedKey: String) {
            val keysToRemove = articles.keys.filter { it.first == feedKey }
            if (keysToRemove.isNotEmpty()) {
                keysToRemove.forEach { articles.remove(it) }
                currentPagingSource?.invalidate()
            }
        }

        private fun createPagingSource(itemsProvider: () -> List<ArticleEntity>): PagingSource<Int, ArticleEntity> =
            object : PagingSource<Int, ArticleEntity>() {
                override fun getRefreshKey(state: PagingState<Int, ArticleEntity>): Int? = null

                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ArticleEntity> = LoadResult.Page(
                    data = itemsProvider(),
                    prevKey = null,
                    nextKey = null
                )
            }.also { currentPagingSource = it }
    }

    private class FakeRemoteKeysDao : RemoteKeysDao {
        private val keys = LinkedHashMap<Pair<String, String>, RemoteKeys>()

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

    @Test
    fun getTopHeadlinesStream_onSuccess_cachesAndEmitsMappedArticles() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "ok",
                totalResults = 1,
                articles = listOf(
                    ArticleDto(
                        title = "Ukrainian Tech News",
                        url = "https://example.com/ua-tech"
                    )
                )
            )
        )
        val fakeDao = FakeArticleDao()
        val fakeKeysDao = FakeRemoteKeysDao()
        val repository = NewsRepository(fakeApi, fakeDao, fakeKeysDao, noTransaction)

        val articles = repository.getTopHeadlinesStream().asSnapshot()

        assertEquals(1, articles.size)
        assertEquals("Ukrainian Tech News", articles.first().title)
        val feedKey = FeedKeys.headlines(country = "us", category = null)
        assertEquals(1, fakeDao.getFeedArticles(feedKey).size)
    }

    @Test
    fun getTopHeadlinesStream_withCategory_passesCategoryToApi() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(status = "ok")
        )
        val repository = NewsRepository(fakeApi, FakeArticleDao(), FakeRemoteKeysDao(), noTransaction)

        val articles = repository.getTopHeadlinesStream(category = "technology").asSnapshot()

        assertTrue(articles.isEmpty())
        assertTrue(fakeApi.getTopHeadlinesCalled)
        assertEquals("technology", fakeApi.lastCategory)
    }

    @Test
    fun searchNewsStream_withQuery_cachesAndEmitsMatchingArticles() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "ok",
                totalResults = 1,
                articles = listOf(
                    ArticleDto(
                        title = "Kotlin 2.0 Released",
                        url = "https://example.com/kotlin-2"
                    )
                )
            )
        )
        val fakeDao = FakeArticleDao()
        val fakeKeysDao = FakeRemoteKeysDao()
        val repository = NewsRepository(fakeApi, fakeDao, fakeKeysDao, noTransaction)

        val articles = repository.searchNewsStream("  Kotlin  ").asSnapshot()

        assertEquals(1, articles.size)
        assertEquals("Kotlin 2.0 Released", articles.first().title)
        assertEquals("kotlin", fakeApi.lastSearchQuery)
        val feedKey = FeedKeys.search("Kotlin")
        assertEquals(1, fakeDao.getFeedArticles(feedKey).size)
    }

    @Test
    fun searchNewsStream_withBlankQuery_revertsToTopHeadlinesStream() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "ok",
                totalResults = 1,
                articles = listOf(
                    ArticleDto(
                        title = "Top Headline News",
                        url = "https://example.com/top"
                    )
                )
            )
        )
        val repository = NewsRepository(fakeApi, FakeArticleDao(), FakeRemoteKeysDao(), noTransaction)

        val articles = repository.searchNewsStream("   ").asSnapshot()

        assertEquals(1, articles.size)
        assertTrue(fakeApi.getTopHeadlinesCalled)
        assertEquals(null, fakeApi.lastSearchQuery)
    }
}

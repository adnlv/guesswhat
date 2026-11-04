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
        private val articles = LinkedHashMap<String, ArticleEntity>().apply {
            initialArticles.forEach { put(it.url, it) }
        }
        private var currentPagingSource: PagingSource<Int, ArticleEntity>? = null

        override fun getHeadlinesPagingSource(category: String?): PagingSource<Int, ArticleEntity> =
            createPagingSource {
                articles.values.filter {
                    (category == null && it.category == null && it.searchQuery == null) ||
                        (it.category == category && it.searchQuery == null)
                }
            }

        override fun searchArticlesPagingSource(query: String): PagingSource<Int, ArticleEntity> = createPagingSource {
            articles.values.filter {
                it.searchQuery == query ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
            }
        }

        suspend fun getHeadlines(category: String?): List<ArticleEntity> = articles.values.filter {
            (category == null && it.category == null && it.searchQuery == null) ||
                (it.category == category && it.searchQuery == null)
        }

        suspend fun searchArticles(query: String): List<ArticleEntity> = articles.values.filter {
            it.searchQuery == query ||
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }

        override suspend fun insertArticles(articles: List<ArticleEntity>) {
            if (articles.isNotEmpty()) {
                articles.forEach { this.articles[it.url] = it }
                currentPagingSource?.invalidate()
            }
        }

        override suspend fun deleteHeadlines(category: String?) {
            val urlsToRemove = getHeadlines(category).map { it.url }
            if (urlsToRemove.isNotEmpty()) {
                urlsToRemove.forEach { articles.remove(it) }
                currentPagingSource?.invalidate()
            }
        }

        override suspend fun deleteSearchArticles(query: String) {
            val urlsToRemove = articles.values.filter { it.searchQuery == query }.map { it.url }
            if (urlsToRemove.isNotEmpty()) {
                urlsToRemove.forEach { articles.remove(it) }
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
        private val keys = LinkedHashMap<String, RemoteKeys>()

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
        val repository = NewsRepository(fakeApi, fakeDao, fakeKeysDao)

        val articles = repository.getTopHeadlinesStream().asSnapshot()

        assertEquals(1, articles.size)
        assertEquals("Ukrainian Tech News", articles.first().title)
        assertEquals(1, fakeDao.getHeadlines(null).size)
    }

    @Test
    fun getTopHeadlinesStream_withCategory_passesCategoryToApi() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(status = "ok")
        )
        val repository = NewsRepository(fakeApi, FakeArticleDao(), FakeRemoteKeysDao())

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
        val repository = NewsRepository(fakeApi, fakeDao, fakeKeysDao)

        val articles = repository.searchNewsStream("  Kotlin  ").asSnapshot()

        assertEquals(1, articles.size)
        assertEquals("Kotlin 2.0 Released", articles.first().title)
        assertEquals("Kotlin", fakeApi.lastSearchQuery)
        assertEquals(1, fakeDao.searchArticles("kotlin").size)
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
        val repository = NewsRepository(fakeApi, FakeArticleDao(), FakeRemoteKeysDao())

        val articles = repository.searchNewsStream("   ").asSnapshot()

        assertEquals(1, articles.size)
        assertTrue(fakeApi.getTopHeadlinesCalled)
        assertEquals(null, fakeApi.lastSearchQuery)
    }
}

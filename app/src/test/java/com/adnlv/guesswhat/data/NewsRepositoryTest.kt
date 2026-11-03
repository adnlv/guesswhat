package com.adnlv.guesswhat.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adnlv.guesswhat.data.local.ArticleDao
import com.adnlv.guesswhat.data.local.ArticleEntity
import com.adnlv.guesswhat.data.remote.NewsApi
import com.adnlv.guesswhat.data.remote.dto.ArticleDto
import com.adnlv.guesswhat.data.remote.dto.NewsResponseDto
import java.io.IOException
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
            return responseToReturn ?: NewsResponseDto(status = "ok")
        }

        override suspend fun searchNews(query: String, language: String?, page: Int?, pageSize: Int?): NewsResponseDto {
            lastSearchQuery = query
            exceptionToThrow?.let { throw it }
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

        override suspend fun getHeadlines(category: String?): List<ArticleEntity> = articles.values.filter {
            (category == null && it.category == null && it.searchQuery == null) ||
                (it.category == category && it.searchQuery == null)
        }

        override suspend fun searchArticles(query: String): List<ArticleEntity> = articles.values.filter {
            it.searchQuery == query ||
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }

        override suspend fun insertArticles(articles: List<ArticleEntity>) {
            articles.forEach { this.articles[it.url] = it }
            currentPagingSource?.invalidate()
        }

        override suspend fun deleteHeadlines(category: String?) {
            val urlsToRemove = getHeadlines(category).map { it.url }
            urlsToRemove.forEach { articles.remove(it) }
            currentPagingSource?.invalidate()
        }

        override suspend fun deleteSearchArticles(query: String) {
            val urlsToRemove = articles.values.filter { it.searchQuery == query }.map { it.url }
            urlsToRemove.forEach { articles.remove(it) }
            currentPagingSource?.invalidate()
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

    @Test
    fun getTopHeadlines_onSuccess_cachesAndReturnsMappedArticles() = runBlocking {
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
        val repository = NewsRepository(fakeApi, fakeDao)

        val result = repository.getTopHeadlines()

        assertTrue(result.isSuccess)
        val articles = result.getOrNull()
        assertEquals(1, articles?.size)
        assertEquals("Ukrainian Tech News", articles?.first()?.title)
        assertEquals(1, fakeDao.getHeadlines(null).size)
    }

    @Test
    fun getTopHeadlines_onErrorStatusWithoutCache_returnsFailure() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "error",
                code = "apiKeyInvalid",
                message = "Your API key is invalid."
            )
        )
        val repository = NewsRepository(fakeApi, FakeArticleDao())

        val result = repository.getTopHeadlines()

        assertTrue(result.isFailure)
        assertEquals("Your API key is invalid.", result.exceptionOrNull()?.message)
    }

    @Test
    fun getTopHeadlines_onNetworkExceptionWithoutCache_returnsFailure() = runBlocking {
        val fakeApi = FakeNewsApi(
            exceptionToThrow = IOException("No network connection")
        )
        val repository = NewsRepository(fakeApi, FakeArticleDao())

        val result = repository.getTopHeadlines()

        assertTrue(result.isFailure)
        assertEquals("No network connection", result.exceptionOrNull()?.message)
    }

    @Test
    fun getTopHeadlines_onNetworkExceptionWithCache_returnsCachedArticles() = runBlocking {
        val cachedEntity = ArticleEntity(
            url = "https://example.com/cached",
            title = "Cached Headline",
            description = "Offline content",
            imageUrl = null,
            sourceName = "Local Source",
            publishedAt = "2026-11-03T06:00:00Z",
            category = "business"
        )
        val fakeApi = FakeNewsApi(
            exceptionToThrow = IOException("Offline")
        )
        val fakeDao = FakeArticleDao(listOf(cachedEntity))
        val repository = NewsRepository(fakeApi, fakeDao)

        val result = repository.getTopHeadlines(category = "business")

        assertTrue(result.isSuccess)
        val articles = result.getOrNull()
        assertEquals(1, articles?.size)
        assertEquals("Cached Headline", articles?.first()?.title)
    }

    @Test
    fun getTopHeadlines_withCategory_passesCategoryToApi() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(status = "ok")
        )
        val repository = NewsRepository(fakeApi, FakeArticleDao())

        val result = repository.getTopHeadlines(category = "technology")

        assertTrue(result.isSuccess)
        assertTrue(fakeApi.getTopHeadlinesCalled)
        assertEquals("technology", fakeApi.lastCategory)
    }

    @Test
    fun searchNews_withQuery_cachesAndReturnsMatchingArticles() = runBlocking {
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
        val repository = NewsRepository(fakeApi, fakeDao)

        val result = repository.searchNews("Kotlin")

        assertTrue(result.isSuccess)
        val articles = result.getOrNull()
        assertEquals(1, articles?.size)
        assertEquals("Kotlin 2.0 Released", articles?.first()?.title)
        assertEquals("Kotlin", fakeApi.lastSearchQuery)
        assertEquals(1, fakeDao.searchArticles("kotlin").size)
    }

    @Test
    fun searchNews_withBlankQuery_revertsToTopHeadlines() = runBlocking {
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
        val repository = NewsRepository(fakeApi, FakeArticleDao())

        val result = repository.searchNews("   ")

        assertTrue(result.isSuccess)
        assertTrue(fakeApi.getTopHeadlinesCalled)
        assertEquals(null, fakeApi.lastSearchQuery)
    }

    @Test
    fun searchNews_onErrorStatusWithoutCache_returnsFailure() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "error",
                code = "rateLimited",
                message = "Too many requests"
            )
        )
        val repository = NewsRepository(fakeApi, FakeArticleDao())

        val result = repository.searchNews("Android")

        assertTrue(result.isFailure)
        assertEquals("Too many requests", result.exceptionOrNull()?.message)
    }

    @Test
    fun searchNews_onNetworkExceptionWithoutCache_returnsFailure() = runBlocking {
        val fakeApi = FakeNewsApi(
            exceptionToThrow = IOException("Timeout")
        )
        val repository = NewsRepository(fakeApi, FakeArticleDao())

        val result = repository.searchNews("Android")

        assertTrue(result.isFailure)
        assertEquals("Timeout", result.exceptionOrNull()?.message)
    }

    @Test
    fun searchNews_onNetworkExceptionWithCache_returnsCachedSearchArticles() = runBlocking {
        val cachedEntity = ArticleEntity(
            url = "https://example.com/android-room",
            title = "Android Room Guide",
            description = "Learn Room persistence",
            imageUrl = null,
            sourceName = "Android Dev",
            publishedAt = "2026-11-03T06:00:00Z",
            searchQuery = "android"
        )
        val fakeApi = FakeNewsApi(
            exceptionToThrow = IOException("Offline")
        )
        val fakeDao = FakeArticleDao(listOf(cachedEntity))
        val repository = NewsRepository(fakeApi, fakeDao)

        val result = repository.searchNews("Android")

        assertTrue(result.isSuccess)
        val articles = result.getOrNull()
        assertEquals(1, articles?.size)
        assertEquals("Android Room Guide", articles?.first()?.title)
    }
}

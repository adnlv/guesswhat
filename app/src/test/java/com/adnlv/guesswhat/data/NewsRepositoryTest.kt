package com.adnlv.guesswhat.data

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
            exceptionToThrow?.let { throw it }
            return responseToReturn ?: NewsResponseDto(status = "ok")
        }

        override suspend fun searchNews(query: String, language: String?, page: Int?, pageSize: Int?): NewsResponseDto {
            lastSearchQuery = query
            exceptionToThrow?.let { throw it }
            return responseToReturn ?: NewsResponseDto(status = "ok")
        }
    }

    @Test
    fun getTopHeadlines_onSuccess_returnsMappedArticles() = runBlocking {
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
        val repository = NewsRepository(fakeApi)

        val result = repository.getTopHeadlines()

        assertTrue(result.isSuccess)
        val articles = result.getOrNull()
        assertEquals(1, articles?.size)
        assertEquals("Ukrainian Tech News", articles?.first()?.title)
    }

    @Test
    fun getTopHeadlines_onErrorStatus_returnsFailure() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "error",
                code = "apiKeyInvalid",
                message = "Your API key is invalid."
            )
        )
        val repository = NewsRepository(fakeApi)

        val result = repository.getTopHeadlines()

        assertTrue(result.isFailure)
        assertEquals("Your API key is invalid.", result.exceptionOrNull()?.message)
    }

    @Test
    fun getTopHeadlines_onNetworkException_returnsFailure() = runBlocking {
        val fakeApi = FakeNewsApi(
            exceptionToThrow = IOException("No network connection")
        )
        val repository = NewsRepository(fakeApi)

        val result = repository.getTopHeadlines()

        assertTrue(result.isFailure)
        assertEquals("No network connection", result.exceptionOrNull()?.message)
    }

    @Test
    fun searchNews_withQuery_returnsMatchingArticles() = runBlocking {
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
        val repository = NewsRepository(fakeApi)

        val result = repository.searchNews("Kotlin")

        assertTrue(result.isSuccess)
        val articles = result.getOrNull()
        assertEquals(1, articles?.size)
        assertEquals("Kotlin 2.0 Released", articles?.first()?.title)
        assertEquals("Kotlin", fakeApi.lastSearchQuery)
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
        val repository = NewsRepository(fakeApi)

        val result = repository.searchNews("   ")

        assertTrue(result.isSuccess)
        assertTrue(fakeApi.getTopHeadlinesCalled)
        assertEquals(null, fakeApi.lastSearchQuery)
    }

    @Test
    fun searchNews_onErrorStatus_returnsFailure() = runBlocking {
        val fakeApi = FakeNewsApi(
            responseToReturn = NewsResponseDto(
                status = "error",
                code = "rateLimited",
                message = "Too many requests"
            )
        )
        val repository = NewsRepository(fakeApi)

        val result = repository.searchNews("Android")

        assertTrue(result.isFailure)
        assertEquals("Too many requests", result.exceptionOrNull()?.message)
    }

    @Test
    fun searchNews_onNetworkException_returnsFailure() = runBlocking {
        val fakeApi = FakeNewsApi(
            exceptionToThrow = IOException("Timeout")
        )
        val repository = NewsRepository(fakeApi)

        val result = repository.searchNews("Android")

        assertTrue(result.isFailure)
        assertEquals("Timeout", result.exceptionOrNull()?.message)
    }
}

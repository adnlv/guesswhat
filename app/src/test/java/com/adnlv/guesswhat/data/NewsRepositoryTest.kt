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

    private class FakeNewsApi(var responseToReturn: NewsResponseDto? = null, var exceptionToThrow: Throwable? = null) :
        NewsApi {
        override suspend fun getTopHeadlines(
            country: String?,
            category: String?,
            query: String?,
            page: Int?,
            pageSize: Int?
        ): NewsResponseDto {
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

        val result = repository.getTopHeadlines(country = "ua")

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

        val result = repository.getTopHeadlines(country = "ua")

        assertTrue(result.isFailure)
        assertEquals("Your API key is invalid.", result.exceptionOrNull()?.message)
    }

    @Test
    fun getTopHeadlines_onNetworkException_returnsFailure() = runBlocking {
        val fakeApi = FakeNewsApi(
            exceptionToThrow = IOException("No network connection")
        )
        val repository = NewsRepository(fakeApi)

        val result = repository.getTopHeadlines(country = "ua")

        assertTrue(result.isFailure)
        assertEquals("No network connection", result.exceptionOrNull()?.message)
    }
}

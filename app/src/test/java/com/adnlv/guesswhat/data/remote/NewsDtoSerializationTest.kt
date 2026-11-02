package com.adnlv.guesswhat.data.remote

import com.adnlv.guesswhat.data.remote.dto.NewsResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NewsDtoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun parseNewsResponseDto_withValidJson_returnsPopulatedDto() {
        val jsonString = """
            {
                "status": "ok",
                "totalResults": 1,
                "articles": [
                    {
                        "source": { "id": "1", "name": "BBC News" },
                        "author": "BBC",
                        "title": "Breaking News",
                        "description": "News description",
                        "url": "https://example.com/news",
                        "urlToImage": "https://example.com/image.png",
                        "publishedAt": "2026-11-02T12:00:00Z",
                        "content": "Article content"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<NewsResponseDto>(jsonString)

        assertEquals("ok", response.status)
        assertEquals(1, response.totalResults)
        assertEquals(1, response.articles.size)

        val article = response.articles[0]
        assertEquals("Breaking News", article.title)
        assertEquals("https://example.com/news", article.url)
        assertNotNull(article.source)
        assertEquals("BBC News", article.source?.name)
    }

    @Test
    fun parseNewsResponseDto_withErrorJson_returnsErrorFields() {
        val jsonString = """
            {
                "status": "error",
                "code": "apiKeyMissing",
                "message": "Your API key is missing."
            }
        """.trimIndent()

        val response = json.decodeFromString<NewsResponseDto>(jsonString)

        assertEquals("error", response.status)
        assertEquals("apiKeyMissing", response.code)
        assertEquals("Your API key is missing.", response.message)
        assertEquals(0, response.articles.size)
    }
}

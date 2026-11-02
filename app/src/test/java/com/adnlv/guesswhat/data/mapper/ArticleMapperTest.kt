package com.adnlv.guesswhat.data.mapper

import com.adnlv.guesswhat.data.remote.dto.ArticleDto
import com.adnlv.guesswhat.data.remote.dto.SourceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleMapperTest {

    @Test
    fun toDomain_withValidDto_mapsAllFieldsCorrectly() {
        val dto = ArticleDto(
            source = SourceDto(name = "Kyiv Independent"),
            author = "Staff",
            title = "Major Update",
            description = "Detailed news summary",
            url = "https://example.com/article",
            urlToImage = "https://example.com/photo.jpg",
            publishedAt = "2026-11-02T10:00:00Z",
            content = "Full content"
        )

        val article = dto.toDomain()

        assertNotNull(article)
        assertEquals("Major Update", article?.title)
        assertEquals("Detailed news summary", article?.description)
        assertEquals("https://example.com/article", article?.url)
        assertEquals("https://example.com/photo.jpg", article?.imageUrl)
        assertEquals("Kyiv Independent", article?.sourceName)
        assertEquals("2026-11-02T10:00:00Z", article?.publishedAt)
    }

    @Test
    fun toDomain_withRemovedOrBlankTitle_returnsNull() {
        val removedDto = ArticleDto(
            title = "[Removed]",
            url = "https://example.com/removed"
        )
        val blankTitleDto = ArticleDto(
            title = "   ",
            url = "https://example.com/blank"
        )
        val nullTitleDto = ArticleDto(
            title = null,
            url = "https://example.com/null"
        )

        assertNull(removedDto.toDomain())
        assertNull(blankTitleDto.toDomain())
        assertNull(nullTitleDto.toDomain())
    }

    @Test
    fun toDomain_withNullFields_usesFallbackValues() {
        val dto = ArticleDto(
            source = null,
            title = "Valid Title",
            description = null,
            url = "https://example.com/article",
            urlToImage = null,
            publishedAt = null
        )

        val article = dto.toDomain()

        assertNotNull(article)
        assertEquals("Unknown Source", article?.sourceName)
        assertEquals("", article?.description)
        assertNull(article?.imageUrl)
        assertEquals("", article?.publishedAt)
    }

    @Test
    fun listToDomain_filtersOutInvalidArticles() {
        val list = listOf(
            ArticleDto(title = "Valid 1", url = "https://example.com/1"),
            ArticleDto(title = "[Removed]", url = "https://example.com/2"),
            ArticleDto(title = "Valid 2", url = "https://example.com/3")
        )

        val result = list.toDomain()

        assertEquals(2, result.size)
        assertEquals("Valid 1", result[0].title)
        assertEquals("Valid 2", result[1].title)
    }
}

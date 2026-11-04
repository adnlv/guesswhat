package com.adnlv.guesswhat.data.mapper

import com.adnlv.guesswhat.data.local.ArticleEntity
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

    @Test
    fun toEntity_withValidDto_mapsAllFieldsAndDiscriminators() {
        val dto = ArticleDto(
            source = SourceDto(name = "Tech News"),
            title = "Room Caching",
            description = "SQLite storage",
            url = "https://example.com/room",
            urlToImage = "https://example.com/room.jpg",
            publishedAt = "2026-11-03T08:00:00Z"
        )

        val entity = dto.toEntity(feedKey = "headlines:us:technology", position = 5)

        assertNotNull(entity)
        assertEquals("headlines:us:technology", entity?.feedKey)
        assertEquals("https://example.com/room", entity?.url)
        assertEquals(5, entity?.position)
        assertEquals("Room Caching", entity?.title)
        assertEquals("SQLite storage", entity?.description)
        assertEquals("https://example.com/room.jpg", entity?.imageUrl)
        assertEquals("Tech News", entity?.sourceName)
        assertEquals("2026-11-03T08:00:00Z", entity?.publishedAt)
    }

    @Test
    fun toEntity_withInvalidDto_returnsNull() {
        val invalidDto = ArticleDto(
            title = "[Removed]",
            url = "https://example.com/removed"
        )

        assertNull(invalidDto.toEntity(feedKey = "headlines:us:business", position = 0))
    }

    @Test
    fun listToEntity_incrementsPositionsCorrectly() {
        val dtos = listOf(
            ArticleDto(title = "Valid 1", url = "https://example.com/1"),
            ArticleDto(title = "[Removed]", url = "https://example.com/2"),
            ArticleDto(title = "Valid 2", url = "https://example.com/3")
        )

        val entities = dtos.toEntity(feedKey = "headlines:us:", startPosition = 10)

        assertEquals(2, entities.size)
        assertEquals(10, entities[0].position)
        assertEquals("https://example.com/1", entities[0].url)
        assertEquals(11, entities[1].position)
        assertEquals("https://example.com/3", entities[1].url)
    }

    @Test
    fun entityListToDomain_mapsAllEntities() {
        val entities = listOf(
            ArticleEntity(
                feedKey = "headlines:us:science",
                url = "https://example.com/1",
                position = 0,
                title = "Cached Title",
                description = "Cached Description",
                imageUrl = "https://example.com/1.jpg",
                sourceName = "Cached Source",
                publishedAt = "2026-11-03T09:00:00Z"
            )
        )

        val articles = entities.toDomain()

        assertEquals(1, articles.size)
        assertEquals("Cached Title", articles[0].title)
        assertEquals("Cached Description", articles[0].description)
        assertEquals("https://example.com/1", articles[0].url)
        assertEquals("https://example.com/1.jpg", articles[0].imageUrl)
        assertEquals("Cached Source", articles[0].sourceName)
        assertEquals("2026-11-03T09:00:00Z", articles[0].publishedAt)
    }
}

package com.adnlv.guesswhat.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsCategoryTest {

    @Test
    fun newsCategory_containsAllRequiredCategories() {
        val expectedCategories = listOf(
            "business",
            "entertainment",
            "general",
            "health",
            "science",
            "sports",
            "technology"
        )
        val actualCategories = NewsCategory.entries.map { it.apiValue }
        assertEquals(expectedCategories, actualCategories)
    }

    @Test
    fun newsCategory_defaultCategoryIsTechnology() {
        assertEquals(NewsCategory.TECHNOLOGY, NewsCategory.DEFAULT)
    }

    @Test
    fun newsCategory_allEntriesHaveValidTitleRes() {
        for (category in NewsCategory.entries) {
            assertTrue(category.titleRes > 0)
        }
    }
}

package com.adnlv.guesswhat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArticleDao {
    @Query(
        """
        SELECT * FROM articles
        WHERE (:category IS NULL AND category IS NULL AND searchQuery IS NULL)
        OR (category = :category AND searchQuery IS NULL)
        """
    )
    suspend fun getHeadlines(category: String?): List<ArticleEntity>

    @Query(
        """
        SELECT * FROM articles
        WHERE searchQuery = :query
        OR title LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        """
    )
    suspend fun searchArticles(query: String): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query(
        """
        DELETE FROM articles
        WHERE (:category IS NULL AND category IS NULL AND searchQuery IS NULL)
        OR (category = :category AND searchQuery IS NULL)
        """
    )
    suspend fun deleteHeadlines(category: String?)

    @Query("DELETE FROM articles WHERE searchQuery = :query")
    suspend fun deleteSearchArticles(query: String)
}

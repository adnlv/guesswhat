package com.adnlv.guesswhat.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE feedKey = :feedKey ORDER BY position ASC")
    fun pagingSource(feedKey: String): PagingSource<Int, ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE feedKey = :feedKey")
    suspend fun deleteFeed(feedKey: String)
}

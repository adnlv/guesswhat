package com.adnlv.guesswhat.data.local

import androidx.room.Entity

@Entity(tableName = "articles", primaryKeys = ["feedKey", "url"])
data class ArticleEntity(
    val feedKey: String,
    val url: String,
    val position: Int,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAt: String
)

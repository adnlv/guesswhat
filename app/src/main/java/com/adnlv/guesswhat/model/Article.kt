package com.adnlv.guesswhat.model

data class Article(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAt: String
)

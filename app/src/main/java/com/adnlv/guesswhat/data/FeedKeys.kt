package com.adnlv.guesswhat.data

sealed interface FeedType {
    data class Headlines(val country: String, val category: String?) : FeedType
    data class Search(val query: String) : FeedType
}

object FeedKeys {
    private const val PREFIX_HEADLINES = "headlines"
    private const val PREFIX_SEARCH = "search"

    fun headlines(country: String = "us", category: String? = null): String =
        "$PREFIX_HEADLINES:$country:${category?.lowercase().orEmpty()}"

    fun search(query: String): String = "$PREFIX_SEARCH:${query.trim().lowercase()}"

    fun parse(feedKey: String): FeedType = if (feedKey.startsWith("$PREFIX_SEARCH:")) {
        FeedType.Search(feedKey.removePrefix("$PREFIX_SEARCH:"))
    } else {
        val parts = feedKey.split(":")
        val country = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: "us"
        val category = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }
        FeedType.Headlines(country = country, category = category)
    }
}

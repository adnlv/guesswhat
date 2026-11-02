package com.adnlv.guesswhat.data.remote

import com.adnlv.guesswhat.data.remote.dto.NewsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String? = "us",
        @Query("category") category: String? = null,
        @Query("q") query: String? = null,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null
    ): NewsResponseDto
}

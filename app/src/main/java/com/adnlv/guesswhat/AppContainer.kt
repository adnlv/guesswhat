package com.adnlv.guesswhat

import android.content.Context
import androidx.room.Room
import com.adnlv.guesswhat.data.NewsRepository
import com.adnlv.guesswhat.data.local.NewsDatabase
import com.adnlv.guesswhat.data.remote.AuthInterceptor
import com.adnlv.guesswhat.data.remote.NewsApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

interface AppContainer {
    val newsRepository: NewsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(BuildConfig.NEWS_API_KEY))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://newsapi.org/v2/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    private val newsApi: NewsApi by lazy {
        retrofit.create(NewsApi::class.java)
    }

    private val database: NewsDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            NewsDatabase::class.java,
            "news_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    override val newsRepository: NewsRepository by lazy {
        NewsRepository(newsApi, database)
    }
}

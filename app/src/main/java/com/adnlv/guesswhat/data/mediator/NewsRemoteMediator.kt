package com.adnlv.guesswhat.data.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.adnlv.guesswhat.data.local.ArticleDao
import com.adnlv.guesswhat.data.local.ArticleEntity
import com.adnlv.guesswhat.data.local.RemoteKeys
import com.adnlv.guesswhat.data.local.RemoteKeysDao
import com.adnlv.guesswhat.data.mapper.toEntity
import com.adnlv.guesswhat.data.remote.NewsApi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediator(
    private val newsApi: NewsApi,
    private val articleDao: ArticleDao,
    private val remoteKeysDao: RemoteKeysDao,
    private val category: String? = null,
    private val searchQuery: String? = null,
    private val country: String = "us",
    private val withTransaction: suspend (suspend () -> Unit) -> Unit
) : RemoteMediator<Int, ArticleEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, ArticleEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: STARTING_PAGE_INDEX
            }
            LoadType.PREPEND -> {
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                nextKey
            }
        }

        val trimmedQuery = searchQuery?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedQuery = trimmedQuery?.lowercase()

        return try {
            val response = if (trimmedQuery == null) {
                newsApi.getTopHeadlines(
                    country = country,
                    category = category,
                    page = page,
                    pageSize = state.config.pageSize
                )
            } else {
                newsApi.searchNews(
                    query = trimmedQuery,
                    page = page,
                    pageSize = state.config.pageSize
                )
            }

            if (response.status != "ok") {
                if (response.code == "maximumResultsReached") {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                val message = response.message ?: "Failed to load news"
                return MediatorResult.Error(IllegalStateException(message))
            }

            val entities = response.articles.toEntity(
                category = if (normalizedQuery == null) category else null,
                searchQuery = normalizedQuery
            )
            val endOfPaginationReached = entities.isEmpty()

            withTransaction {
                if (loadType == LoadType.REFRESH) {
                    if (normalizedQuery == null) {
                        remoteKeysDao.deleteHeadlinesKeys(category = category)
                        articleDao.deleteHeadlines(category = category)
                    } else {
                        remoteKeysDao.deleteSearchKeys(query = normalizedQuery)
                        articleDao.deleteSearchArticles(query = normalizedQuery)
                    }
                }
                if (entities.isNotEmpty()) {
                    val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
                    val nextKey = if (endOfPaginationReached) null else page + 1
                    val keys = entities.map { article ->
                        RemoteKeys(
                            articleUrl = article.url,
                            prevKey = prevKey,
                            nextKey = nextKey,
                            category = if (normalizedQuery == null) category else null,
                            searchQuery = normalizedQuery
                        )
                    }
                    remoteKeysDao.insertAll(keys)
                    articleDao.insertArticles(entities)
                }
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (loadType == LoadType.APPEND && e.code() == HTTP_UPGRADE_REQUIRED) {
                MediatorResult.Success(endOfPaginationReached = true)
            } else {
                MediatorResult.Error(e)
            }
        } catch (e: Throwable) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, ArticleEntity>): RemoteKeys? =
        state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { article -> remoteKeysDao.getRemoteKeys(article.url) }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, ArticleEntity>): RemoteKeys? =
        state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.url?.let { url ->
                remoteKeysDao.getRemoteKeys(url)
            }
        }

    companion object {
        private const val STARTING_PAGE_INDEX = 1
        private const val HTTP_UPGRADE_REQUIRED = 426
    }
}

package com.adnlv.guesswhat.data.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.adnlv.guesswhat.data.FeedKeys
import com.adnlv.guesswhat.data.FeedType
import com.adnlv.guesswhat.data.local.ArticleDao
import com.adnlv.guesswhat.data.local.ArticleEntity
import com.adnlv.guesswhat.data.local.RemoteKeys
import com.adnlv.guesswhat.data.local.RemoteKeysDao
import com.adnlv.guesswhat.data.mapper.toEntity
import com.adnlv.guesswhat.data.remote.NewsApi
import com.adnlv.guesswhat.data.remote.dto.NewsResponseDto
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediator(
    private val feedKey: String,
    private val newsApi: NewsApi,
    private val articleDao: ArticleDao,
    private val remoteKeysDao: RemoteKeysDao,
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

        return try {
            val response = fetchResponse(page = page, pageSize = state.config.pageSize)

            if (response.status != "ok") {
                if (response.code == "maximumResultsReached") {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                val message = response.message ?: "Failed to load news"
                return MediatorResult.Error(IllegalStateException(message))
            }

            val startPosition = (page - STARTING_PAGE_INDEX) * state.config.pageSize
            val entities = response.articles.toEntity(
                feedKey = feedKey,
                startPosition = startPosition
            )
            val endOfPaginationReached = entities.isEmpty()

            withTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKeysDao.deleteFeedKeys(feedKey = feedKey)
                    articleDao.deleteFeed(feedKey = feedKey)
                }
                if (entities.isNotEmpty()) {
                    val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
                    val nextKey = if (endOfPaginationReached) null else page + 1
                    val keys = entities.map { article ->
                        RemoteKeys(
                            feedKey = feedKey,
                            articleUrl = article.url,
                            prevKey = prevKey,
                            nextKey = nextKey
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

    private suspend fun fetchResponse(page: Int, pageSize: Int): NewsResponseDto =
        when (val feed = FeedKeys.parse(feedKey)) {
            is FeedType.Search -> newsApi.searchNews(
                query = feed.query,
                page = page,
                pageSize = pageSize
            )
            is FeedType.Headlines -> newsApi.getTopHeadlines(
                country = feed.country,
                category = feed.category,
                page = page,
                pageSize = pageSize
            )
        }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, ArticleEntity>): RemoteKeys? =
        state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { article -> remoteKeysDao.getRemoteKeys(feedKey = feedKey, url = article.url) }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, ArticleEntity>): RemoteKeys? =
        state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { article ->
                remoteKeysDao.getRemoteKeys(feedKey = feedKey, url = article.url)
            }
        }

    companion object {
        private const val STARTING_PAGE_INDEX = 1
        private const val HTTP_UPGRADE_REQUIRED = 426
    }
}

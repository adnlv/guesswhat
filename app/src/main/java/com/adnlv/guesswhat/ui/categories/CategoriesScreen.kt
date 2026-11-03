package com.adnlv.guesswhat.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.adnlv.guesswhat.R
import com.adnlv.guesswhat.model.Article
import com.adnlv.guesswhat.model.NewsCategory
import com.adnlv.guesswhat.ui.components.ArticleCard
import com.adnlv.guesswhat.ui.theme.GuessWhatTheme
import kotlinx.coroutines.flow.flowOf

@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel? = null, modifier: Modifier = Modifier) {
    if (viewModel != null) {
        val articles = viewModel.articles.collectAsLazyPagingItems()
        val selectedCategory by viewModel.selectedCategory.collectAsState()
        CategoriesScreenContent(
            articles = articles,
            selectedCategory = selectedCategory,
            onCategorySelected = viewModel::selectCategory,
            modifier = modifier
        )
    } else {
        val loadingArticles = remember {
            flowOf(
                PagingData.from(
                    data = emptyList<Article>(),
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.Loading,
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true)
                    )
                )
            )
        }.collectAsLazyPagingItems()
        CategoriesScreenContent(
            articles = loadingArticles,
            selectedCategory = NewsCategory.DEFAULT,
            onCategorySelected = {},
            modifier = modifier
        )
    }
}

@Composable
fun CategoriesScreenContent(
    articles: LazyPagingItems<Article>,
    selectedCategory: NewsCategory,
    onCategorySelected: (NewsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.categories_screen_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        CategoryFilterChips(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CategoriesNewsListContent(
            articles = articles,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterChips(
    selectedCategory: NewsCategory,
    onCategorySelected: (NewsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = NewsCategory.entries,
            key = { it.apiValue }
        ) { category ->
            val selected = category == selectedCategory
            FilterChip(
                selected = selected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(text = stringResource(category.titleRes))
                },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
fun CategoriesNewsListContent(articles: LazyPagingItems<Article>, modifier: Modifier = Modifier) {
    val refreshState = articles.loadState.refresh
    val appendState = articles.loadState.append

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (articles.itemCount == 0) {
            when (refreshState) {
                is LoadState.Loading -> {
                    CircularProgressIndicator()
                }
                is LoadState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = refreshState.error.message ?: stringResource(R.string.empty_news),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { articles.retry() }) {
                            Text(text = stringResource(R.string.retry_button))
                        }
                    }
                }
                is LoadState.NotLoading -> {
                    Text(
                        text = stringResource(R.string.empty_news),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = articles.itemCount,
                    key = articles.itemKey { it.url }
                ) { index ->
                    val article = articles[index]
                    if (article != null) {
                        ArticleCard(article = article)
                    }
                }

                when (appendState) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = appendState.error.message ?: stringResource(R.string.empty_news),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { articles.retry() }) {
                                    Text(text = stringResource(R.string.retry_button))
                                }
                            }
                        }
                    }
                    is LoadState.NotLoading -> Unit
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenSuccessPreview() {
    GuessWhatTheme {
        val articles = remember {
            flowOf(
                PagingData.from(
                    data = listOf(
                        Article(
                            title = "New AI Model Breakthrough",
                            description = "Researchers demonstrate next generation reasoning system.",
                            url = "https://example.com/tech-news",
                            imageUrl = null,
                            sourceName = "Tech World",
                            publishedAt = "2026-11-03T05:00:00Z"
                        )
                    ),
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.NotLoading(endOfPaginationReached = true),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true)
                    )
                )
            )
        }.collectAsLazyPagingItems()
        CategoriesScreenContent(
            articles = articles,
            selectedCategory = NewsCategory.BUSINESS,
            onCategorySelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenLoadingPreview() {
    GuessWhatTheme {
        val articles = remember {
            flowOf(
                PagingData.from(
                    data = emptyList<Article>(),
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.Loading,
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true)
                    )
                )
            )
        }.collectAsLazyPagingItems()
        CategoriesScreenContent(
            articles = articles,
            selectedCategory = NewsCategory.BUSINESS,
            onCategorySelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenErrorPreview() {
    GuessWhatTheme {
        val articles = remember {
            flowOf(
                PagingData.from(
                    data = emptyList<Article>(),
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.Error(IllegalStateException("Network connection lost")),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true)
                    )
                )
            )
        }.collectAsLazyPagingItems()
        CategoriesScreenContent(
            articles = articles,
            selectedCategory = NewsCategory.BUSINESS,
            onCategorySelected = {}
        )
    }
}

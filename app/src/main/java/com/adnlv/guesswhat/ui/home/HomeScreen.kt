package com.adnlv.guesswhat.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.adnlv.guesswhat.ui.components.ArticleCard
import com.adnlv.guesswhat.ui.theme.GuessWhatTheme
import kotlinx.coroutines.flow.flowOf

@Composable
fun HomeScreen(viewModel: HomeViewModel?, modifier: Modifier = Modifier) {
    if (viewModel != null) {
        val articles = viewModel.articles.collectAsLazyPagingItems()
        val searchQuery by viewModel.searchQuery.collectAsState()
        HomeScreenContent(
            articles = articles,
            searchQuery = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onClearQuery = viewModel::clearSearchQuery,
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
        HomeScreenContent(
            articles = loadingArticles,
            searchQuery = "",
            onQueryChange = {},
            onClearQuery = {},
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    articles: LazyPagingItems<Article>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = onQueryChange,
                    onSearch = { expanded = false },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = {
                        Text(text = stringResource(R.string.search_placeholder))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear_search)
                                )
                            }
                        }
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (expanded) 0.dp else 16.dp, vertical = 8.dp)
        ) {
            NewsListContent(
                articles = articles,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!expanded) {
            NewsListContent(
                articles = articles,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun NewsListContent(articles: LazyPagingItems<Article>, modifier: Modifier = Modifier) {
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
private fun HomeScreenSuccessPreview() {
    GuessWhatTheme {
        val articles = remember {
            flowOf(
                PagingData.from(
                    data = listOf(
                        Article(
                            title = "Sample News Headline from Ukraine",
                            description = "This is a brief preview description for the news card.",
                            url = "https://example.com/1",
                            imageUrl = null,
                            sourceName = "Kyiv Post",
                            publishedAt = "2026-11-02T18:00:00Z"
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
        HomeScreenContent(
            articles = articles,
            searchQuery = "",
            onQueryChange = {},
            onClearQuery = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
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
        HomeScreenContent(
            articles = articles,
            searchQuery = "",
            onQueryChange = {},
            onClearQuery = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    GuessWhatTheme {
        val articles = remember {
            flowOf(
                PagingData.from(
                    data = emptyList<Article>(),
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.Error(IllegalStateException("Failed to fetch news")),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true)
                    )
                )
            )
        }.collectAsLazyPagingItems()
        HomeScreenContent(
            articles = articles,
            searchQuery = "",
            onQueryChange = {},
            onClearQuery = {}
        )
    }
}

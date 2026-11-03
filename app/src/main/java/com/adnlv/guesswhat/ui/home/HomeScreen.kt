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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adnlv.guesswhat.R
import com.adnlv.guesswhat.model.Article
import com.adnlv.guesswhat.ui.components.ArticleCard
import com.adnlv.guesswhat.ui.theme.GuessWhatTheme

@Composable
fun HomeScreen(viewModel: HomeViewModel?, modifier: Modifier = Modifier) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        HomeScreenContent(
            uiState = uiState,
            searchQuery = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onClearQuery = viewModel::clearSearchQuery,
            onRetry = viewModel::retry,
            modifier = modifier
        )
    } else {
        HomeScreenContent(
            uiState = HomeUiState.Loading,
            searchQuery = "",
            onQueryChange = {},
            onClearQuery = {},
            onRetry = {},
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
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
                uiState = uiState,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!expanded) {
            NewsListContent(
                uiState = uiState,
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun NewsListContent(uiState: HomeUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is HomeUiState.Loading -> {
                CircularProgressIndicator()
            }
            is HomeUiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text(text = stringResource(R.string.retry_button))
                    }
                }
            }
            is HomeUiState.Success -> {
                if (uiState.articles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.empty_news),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.articles,
                            key = { it.url }
                        ) { article ->
                            ArticleCard(article = article)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenSuccessPreview() {
    GuessWhatTheme {
        HomeScreenContent(
            uiState = HomeUiState.Success(
                articles = listOf(
                    Article(
                        title = "Sample News Headline from Ukraine",
                        description = "This is a brief preview description for the news card.",
                        url = "https://example.com/1",
                        imageUrl = null,
                        sourceName = "Kyiv Post",
                        publishedAt = "2026-11-02T18:00:00Z"
                    )
                )
            ),
            searchQuery = "",
            onQueryChange = {},
            onClearQuery = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    GuessWhatTheme {
        HomeScreenContent(
            uiState = HomeUiState.Loading,
            searchQuery = "",
            onQueryChange = {},
            onClearQuery = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    GuessWhatTheme {
        HomeScreenContent(
            uiState = HomeUiState.Error("Failed to fetch news"),
            searchQuery = "",
            onQueryChange = {},
            onClearQuery = {},
            onRetry = {}
        )
    }
}

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adnlv.guesswhat.R
import com.adnlv.guesswhat.model.Article
import com.adnlv.guesswhat.model.NewsCategory
import com.adnlv.guesswhat.ui.components.ArticleCard
import com.adnlv.guesswhat.ui.theme.GuessWhatTheme

@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel? = null, modifier: Modifier = Modifier) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()
        val selectedCategory by viewModel.selectedCategory.collectAsState()
        CategoriesScreenContent(
            uiState = uiState,
            selectedCategory = selectedCategory,
            onCategorySelected = viewModel::selectCategory,
            onRetry = viewModel::retry,
            modifier = modifier
        )
    } else {
        CategoriesScreenContent(
            uiState = CategoriesUiState.Loading,
            selectedCategory = NewsCategory.DEFAULT,
            onCategorySelected = {},
            onRetry = {},
            modifier = modifier
        )
    }
}

@Composable
fun CategoriesScreenContent(
    uiState: CategoriesUiState,
    selectedCategory: NewsCategory,
    onCategorySelected: (NewsCategory) -> Unit,
    onRetry: () -> Unit,
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
            uiState = uiState,
            onRetry = onRetry,
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
fun CategoriesNewsListContent(uiState: CategoriesUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is CategoriesUiState.Loading -> {
                CircularProgressIndicator()
            }
            is CategoriesUiState.Error -> {
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
            is CategoriesUiState.Success -> {
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
private fun CategoriesScreenSuccessPreview() {
    GuessWhatTheme {
        CategoriesScreenContent(
            uiState = CategoriesUiState.Success(
                articles = listOf(
                    Article(
                        title = "New AI Model Breakthrough",
                        description = "Researchers demonstrate next generation reasoning system.",
                        url = "https://example.com/tech-news",
                        imageUrl = null,
                        sourceName = "Tech World",
                        publishedAt = "2026-11-03T05:00:00Z"
                    )
                )
            ),
            selectedCategory = NewsCategory.BUSINESS,
            onCategorySelected = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenLoadingPreview() {
    GuessWhatTheme {
        CategoriesScreenContent(
            uiState = CategoriesUiState.Loading,
            selectedCategory = NewsCategory.BUSINESS,
            onCategorySelected = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenErrorPreview() {
    GuessWhatTheme {
        CategoriesScreenContent(
            uiState = CategoriesUiState.Error("Network connection lost"),
            selectedCategory = NewsCategory.BUSINESS,
            onCategorySelected = {},
            onRetry = {}
        )
    }
}

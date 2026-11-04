package com.adnlv.guesswhat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.adnlv.guesswhat.model.Article
import com.adnlv.guesswhat.ui.theme.GuessWhatTheme

@Composable
fun ArticleCard(article: Article, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!article.imageUrl.isNullOrBlank()) {
                val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                val errorPainter = ColorPainter(MaterialTheme.colorScheme.errorContainer)
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    placeholder = placeholderPainter,
                    error = errorPainter,
                    fallback = placeholderPainter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = article.sourceName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (article.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = article.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleCardWithImagePreview() {
    GuessWhatTheme {
        ArticleCard(
            article = Article(
                title = "Sample News Headline with Image",
                description = "This preview verifies the card layout when an image URL is present.",
                url = "https://example.com/with-image",
                imageUrl = "https://example.com/image.jpg",
                sourceName = "Kyiv Post",
                publishedAt = "2026-11-02T18:00:00Z"
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleCardWithoutImagePreview() {
    GuessWhatTheme {
        ArticleCard(
            article = Article(
                title = "Sample News Headline without Image",
                description = "This preview verifies the card layout when the image URL is null.",
                url = "https://example.com/without-image",
                imageUrl = null,
                sourceName = "Kyiv Post",
                publishedAt = "2026-11-02T18:00:00Z"
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

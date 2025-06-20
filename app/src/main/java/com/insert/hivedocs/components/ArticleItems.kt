package com.insert.hivedocs.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.insert.hivedocs.data.Article

@Composable
fun ArticleListItem(article: Article, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = article.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Por: ${article.author} (${article.year})", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val averageRating = if (article.ratingCount > 0) (article.ratingSum / article.ratingCount).toFloat() else 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRatingDisplay(rating = averageRating, starSize = 20.dp)
                Spacer(modifier = Modifier.width(8.dp))
                if (article.ratingCount > 0) {
                    Text(text = "(${article.ratingCount})", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun PendingArticleItem(article: Article, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = article.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Por: ${article.author} (${article.year})", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = article.resume, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onReject, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Close, contentDescription = "Recusar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recusar")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onApprove) {
                    Icon(Icons.Default.Check, contentDescription = "Aprovar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aprovar")
                }
            }
        }
    }
}
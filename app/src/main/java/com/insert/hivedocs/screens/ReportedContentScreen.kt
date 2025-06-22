package com.insert.hivedocs.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.insert.hivedocs.data.Reply
import com.insert.hivedocs.data.Review
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface ReportedItem {
    val id: String
    val textContent: String
    val authorName: String
    val authorId: String
    val timestamp: Timestamp?
}

data class ReportedReview(
    val review: Review,
    val articleId: String
) : ReportedItem {
    override val id: String get() = review.id
    override val textContent: String get() = review.comment
    override val authorName: String get() = review.userName
    override val authorId: String get() = review.userId
    override val timestamp: Timestamp? get() = review.timestamp
}

data class ReportedReply(
    val reply: Reply,
    val articleId: String,
    val reviewId: String
) : ReportedItem {
    override val id: String get() = reply.id
    override val textContent: String get() = reply.replyText
    override val authorName: String get() = reply.userName
    override val authorId: String get() = reply.userId
    override val timestamp: Timestamp? get() = reply.timestamp
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedContentScreen(navController: NavController) {
    var reportedItems by remember { mutableStateOf<List<ReportedItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isBanning by remember { mutableStateOf(false) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    suspend fun banUser(userId: String) {
        isBanning = true
        try {
            val batch = firestore.batch()

            val userRef = firestore.collection("users").document(userId)
            batch.update(userRef, "isBanned", true)

            val reviewsSnapshot = firestore.collectionGroup("reviews").whereEqualTo("userId", userId).get().await()
            for (document in reviewsSnapshot.documents) {
                batch.delete(document.reference)
            }

            val repliesSnapshot = firestore.collectionGroup("replies").whereEqualTo("userId", userId).get().await()
            for (document in repliesSnapshot.documents) {
                batch.delete(document.reference)
            }

            batch.commit().await()

            Toast.makeText(context, "Usuário banido e todo o seu conteúdo foi removido.", Toast.LENGTH_LONG).show()
            reportedItems = reportedItems.filterNot { it.authorId == userId }

        } catch (e: Exception) {
            Toast.makeText(context, "Falha na operação de banimento: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ModerationScreen", "Erro ao banir usuário e limpar conteúdo", e)
        } finally {
            isBanning = false
        }
    }

    fun deleteReview(item: ReportedReview) {
        firestore.runTransaction { transaction ->
            val articleRef = firestore.collection("articles").document(item.articleId)
            val reviewRef = articleRef.collection("reviews").document(item.review.id)

            val articleSnapshot = transaction.get(articleRef)
            val ratingCount = articleSnapshot.getLong("ratingCount") ?: 0L
            val ratingSum = articleSnapshot.getDouble("ratingSum") ?: 0.0

            if (ratingCount > 0) {
                transaction.update(articleRef, "ratingCount", ratingCount - 1)
                transaction.update(articleRef, "ratingSum", ratingSum - item.review.rating)
            }
            transaction.delete(reviewRef)
        }.addOnSuccessListener {
            Toast.makeText(context, "Avaliação excluída.", Toast.LENGTH_SHORT).show()
            reportedItems = reportedItems.filterNot { it.id == item.id }
        }
    }

    fun deleteReply(item: ReportedReply) {
        firestore.collection("articles").document(item.articleId)
            .collection("reviews").document(item.reviewId)
            .collection("replies").document(item.reply.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Resposta excluída.", Toast.LENGTH_SHORT).show()
                reportedItems = reportedItems.filterNot { it.id == item.id }
            }
    }

    LaunchedEffect(Unit) {
        val reviewsQuery = firestore.collectionGroup("reviews").whereEqualTo("isReported", true).get()
        val repliesQuery = firestore.collectionGroup("replies").whereEqualTo("isReported", true).get()

        val allReportedItems = mutableListOf<ReportedItem>()
        var queriesFinished = 0

        fun checkCompletion() {
            queriesFinished++
            if (queriesFinished == 2) {
                reportedItems = allReportedItems.sortedByDescending { it.timestamp }
                isLoading = false
            }
        }

        reviewsQuery.addOnSuccessListener { snapshot ->
            val items = snapshot.documents.mapNotNull { doc ->
                val review = doc.toObject<Review>()
                val articleId = doc.reference.parent.parent?.id
                if (review != null && articleId != null) ReportedReview(review, articleId) else null
            }
            allReportedItems.addAll(items)
            checkCompletion()
        }.addOnFailureListener { e ->
            Log.e("ReportedContentScreen", "Erro ao buscar reviews denunciadas", e)
            checkCompletion()
        }

        repliesQuery.addOnSuccessListener { snapshot ->
            val items = snapshot.documents.mapNotNull { doc ->
                val reply = doc.toObject<Reply>()
                val reviewId = doc.reference.parent.parent?.id
                val articleId = doc.reference.parent.parent?.parent?.parent?.id
                if (reply != null && reviewId != null && articleId != null) ReportedReply(reply, articleId, reviewId) else null
            }
            allReportedItems.addAll(items)
            checkCompletion()
        }.addOnFailureListener { e ->
            Log.e("ReportedContentScreen", "Erro ao buscar replies denunciadas", e)
            checkCompletion()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conteúdo Denunciado") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (reportedItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum conteúdo denunciado.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(reportedItems, key = { it.id }) { item ->
                        val isBanningThisUser = isBanning && item.authorId == (item as? ReportedItem)?.authorId

                        ReportedItemCard(
                            itemType = if (item is ReportedReview) "Avaliação Denunciada" else "Resposta Denunciada",
                            itemText = item.textContent,
                            authorInfo = "Autor: ${item.authorName} (ID: ${item.authorId})",
                            onDelete = {
                                when (item) {
                                    is ReportedReview -> deleteReview(item)
                                    is ReportedReply -> deleteReply(item)
                                }
                            },
                            onBan = {
                                coroutineScope.launch {
                                    banUser(item.authorId)
                                }
                            },
                            isActionLoading = isBanningThisUser
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportedItemCard(
    itemType: String,
    itemText: String,
    authorInfo: String,
    onDelete: () -> Unit,
    onBan: () -> Unit,
    isActionLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(itemType, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(authorInfo, style = MaterialTheme.typography.bodySmall)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(text = "\"$itemText\"", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onBan, enabled = !isActionLoading) { Text("Banir Usuário") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDelete, enabled = !isActionLoading, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Excluir Conteúdo")
                }
            }
        }
    }
}
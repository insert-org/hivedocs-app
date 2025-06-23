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
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.insert.hivedocs.data.Reply
import com.insert.hivedocs.data.Report
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
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }

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

    fun deleteReportedContent(report: Report) {
        val docRef = if (report.contentType == "review") {
            firestore.collection("articles").document(report.articleId)
                .collection("reviews").document(report.reviewId)
        } else {
            firestore.collection("articles").document(report.articleId)
                .collection("reviews").document(report.reviewId)
                .collection("replies").document(report.replyId!!)
        }

        firestore.batch().apply {
            delete(docRef)
            delete(firestore.collection("reports").document(report.id))
        }.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "Conteúdo excluído e denúncia resolvida.", Toast.LENGTH_SHORT).show()
                reports = reports.filterNot { it.id == report.id }
            }
    }

    LaunchedEffect(Unit) {
        firestore.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) {
                    Log.e("ModerationScreen", "Erro ao buscar denúncias", e)
                    return@addSnapshotListener
                }
                reports = snapshot?.documents?.mapNotNull { it.toObject<Report>() } ?: emptyList()
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            if (isLoading) {
                // ...
            } else if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum conteúdo denunciado.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(reports, key = { it.id }) { report ->
                        ReportedItemCard(
                            report = report,
                            onDelete = { deleteReportedContent(report) },
                            onBan = {
                                coroutineScope.launch {
                                    banUser(report.contentOwnerId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportedItemCard(
    report: Report,
    onDelete: () -> Unit,
    onBan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if(report.contentType == "review") "Avaliação Denunciada" else "Resposta Denunciada",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Motivo: ${report.reason}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text("Conteúdo: \"${report.contentText}\"", style = MaterialTheme.typography.bodyMedium)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Autor do conteúdo: ${report.contentOwnerId}", style = MaterialTheme.typography.bodySmall)
            Text("Denunciado por: ${report.reporterName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onBan) { Text("Banir Autor") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Excluir Conteúdo")
                }
            }
        }
    }
}
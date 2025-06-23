package com.insert.hivedocs.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.insert.hivedocs.components.StarRatingDisplay
import com.insert.hivedocs.data.Article
import com.insert.hivedocs.data.Reply
import com.insert.hivedocs.data.Review
import java.text.SimpleDateFormat
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import com.insert.hivedocs.R
import com.insert.hivedocs.components.StarRatingInput
import com.insert.hivedocs.data.Report
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String,
    navController: NavController,
    isAdmin: Boolean,
    currentUserId: String?
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var article by remember { mutableStateOf<Article?>(null) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var userHasAlreadyReviewed by remember { mutableStateOf(false) }

    var userRating by remember { mutableFloatStateOf(3.0f) }
    var userComment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(articleId, auth.currentUser) {
        firestore.collection("articles").document(articleId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ArticleDetail", "Listen failed.", e)
                return@addSnapshotListener
            }
            article = snapshot?.toObject<Article>()
        }

        firestore.collection("articles").document(articleId).collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.w("ArticleDetail", "Reviews listen failed.", e); return@addSnapshotListener }
                reviews = snapshot?.documents?.mapNotNull { it.toObject<Review>() } ?: emptyList()
                auth.currentUser?.let { user ->
                    userHasAlreadyReviewed = reviews.any { it.userId == user.uid }
                }
            }
    }

    fun submitReport(
        contentType: String,
        contentText: String,
        contentOwnerId: String,
        articleId: String,
        reviewId: String,
        replyId: String? = null,
        reason: String
    ) {
        val currentUser = auth.currentUser ?: return
        val report = Report(
            contentType = contentType,
            contentText = contentText,
            contentOwnerId = contentOwnerId,
            articleId = articleId,
            reviewId = reviewId,
            replyId = replyId,
            reporterId = currentUser.uid,
            reporterName = auth.getDisplayName(),
            reason = reason
        )

        firestore.collection("reports").add(report)
            .addOnSuccessListener { Toast.makeText(context, "Denúncia enviada para moderação.", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(context, "Erro ao enviar denúncia: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    fun postReply(reviewId: String, replyText: String) {
        if (replyText.isBlank()) return
        val currentUser = auth.currentUser ?: return
        val newReply = Reply(
            userId = currentUser.uid,
            userName = auth.getDisplayName(),
            replyText = replyText
        )
        firestore.collection("articles").document(articleId)
            .collection("reviews").document(reviewId)
            .collection("replies").add(newReply)
            .addOnSuccessListener {
                Toast.makeText(context, "Resposta enviada!", Toast.LENGTH_SHORT).show()
            }
    }

    fun deleteReply(reviewId: String, replyId: String) {
        firestore.collection("articles").document(articleId)
            .collection("reviews").document(reviewId)
            .collection("replies").document(replyId).delete()
            .addOnSuccessListener { Toast.makeText(context, "Resposta excluída.", Toast.LENGTH_SHORT).show() }
    }

    fun editReply(reviewId: String, replyId: String, newText: String) {
        firestore.collection("articles").document(articleId)
            .collection("reviews").document(reviewId)
            .collection("replies").document(replyId).update("replyText", newText)
            .addOnSuccessListener { Toast.makeText(context, "Resposta atualizada.", Toast.LENGTH_SHORT).show() }
    }

    fun deleteReview(reviewToDelete: Review) {
        firestore.runTransaction { transaction ->
            val articleRef = firestore.collection("articles").document(articleId)
            val reviewRef = articleRef.collection("reviews").document(reviewToDelete.id)

            val articleSnapshot = transaction.get(articleRef)
            val currentRatingCount = articleSnapshot.getLong("ratingCount") ?: 0L
            val currentRatingSum = articleSnapshot.getDouble("ratingSum") ?: 0.0

            if (currentRatingCount > 0) {
                val newRatingCount = currentRatingCount - 1
                val newRatingSum = currentRatingSum - reviewToDelete.rating
                transaction.update(articleRef, "ratingCount", newRatingCount)
                transaction.update(articleRef, "ratingSum", newRatingSum)
            }

            transaction.delete(reviewRef)
            null
        }.addOnSuccessListener {
            Toast.makeText(context, "Avaliação excluída.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun editReview(reviewToEdit: Review, newRating: Float, newComment: String) {
        firestore.runTransaction { transaction ->
            val articleRef = firestore.collection("articles").document(articleId)
            val reviewRef = articleRef.collection("reviews").document(reviewToEdit.id)

            val articleSnapshot = transaction.get(articleRef)
            val currentRatingSum = articleSnapshot.getDouble("ratingSum") ?: 0.0

            val newRatingSum = currentRatingSum - reviewToEdit.rating + newRating
            transaction.update(articleRef, "ratingSum", newRatingSum)

            transaction.update(reviewRef, mapOf(
                "rating" to newRating,
                "comment" to newComment
            ))
            null
        }.addOnSuccessListener {
            Toast.makeText(context, "Avaliação atualizada.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun submitReview() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Você precisa estar logado para avaliar.", Toast.LENGTH_SHORT).show()
            return
        }
        isSubmitting = true
        val newReview = Review(
            userId = currentUser.uid,
            userName = auth.getDisplayName(),
            rating = userRating,
            comment = userComment
        )

        firestore.runTransaction { transaction ->
            val articleRef = firestore.collection("articles").document(articleId)
            val reviewRef = articleRef.collection("reviews").document(currentUser.uid)

            val snapshot = transaction.get(articleRef)
            val currentRatingCount = snapshot.getLong("ratingCount") ?: 0L
            val currentRatingSum = snapshot.getDouble("ratingSum") ?: 0.0
            val newRatingCount = currentRatingCount + 1
            val newRatingSum = currentRatingSum + userRating

            transaction.update(articleRef, "ratingCount", newRatingCount)
            transaction.update(articleRef, "ratingSum", newRatingSum)
            transaction.set(reviewRef, newReview)
            null
        }.addOnSuccessListener {
            Toast.makeText(context, "Avaliação enviada!", Toast.LENGTH_SHORT).show()

            article?.let { currentArticle ->
                val newRatingCount = currentArticle.ratingCount + 1
                val newRatingSum = currentArticle.ratingSum + userRating
                article = currentArticle.copy(
                    ratingCount = newRatingCount,
                    ratingSum = newRatingSum
                )
            }

            userComment = ""
            userRating = 3.0f
            isSubmitting = false
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Erro ao enviar avaliação: ${e.message}", Toast.LENGTH_LONG).show()
            isSubmitting = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article?.title ?: "Carregando...", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                if (article == null) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val averageRating = if (article!!.ratingCount > 0) (article!!.ratingSum / article!!.ratingCount).toFloat() else 0f
                    Text(text = article!!.title, style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Por: ${article!!.author} (${article!!.year})", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarRatingDisplay(rating = averageRating)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "(${String.format("%.1f", averageRating)} de ${article!!.ratingCount} avaliações)", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (article!!.articleUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.clickable {
                                try {
                                    uriHandler.openUri(article!!.articleUrl)
                                } catch (e: Exception) {
                                    Log.e("ArticleDetailScreen", "Não foi possível abrir a URL", e)
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.link_solid),
                                contentDescription = "Link para o artigo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Acessar artigo original",
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Resumo", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = article!!.resume, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (!userHasAlreadyReviewed) {
                item {
                    Divider()
                    Text("Deixe sua avaliação", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        StarRatingInput(
                            rating = userRating,
                            onRatingChange = { newRating -> userRating = newRating }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "Sua nota: ${String.format("%.1f", userRating)}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = userComment,
                        onValueChange = { userComment = it },
                        label = { Text("Seu comentário") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { submitReview() }, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth()) {
                        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Enviar Avaliação")
                    }
                }
            }

            item {
                Divider()
                Text("Avaliações", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 16.dp))
            }
            if (reviews.isEmpty()) {
                item { Text("Nenhuma avaliação ainda. Seja o primeiro!") }
            } else {
                items(reviews, key = { it.id }) { review ->
                    ReviewItem(
                        review = review,
                        articleId = articleId,
                        currentUserId = currentUserId,
                        isAdmin = isAdmin,
                        onDelete = { deleteReview(review) },
                        onEdit = { newRating, newComment -> editReview(review, newRating, newComment) },
                        onPostReply = { reviewId, replyText -> postReply(reviewId, replyText) },
                        onDeleteReply = { reviewId, replyId -> deleteReply(reviewId, replyId) },
                        onEditReply = { reviewId, replyId, newText -> editReply(reviewId, replyId, newText) },
                        onReport = { reason ->
                            submitReport(
                                contentType = "review",
                                contentText = review.comment,
                                contentOwnerId = review.userId,
                                articleId = articleId,
                                reviewId = review.id,
                                reason = reason
                            )
                        },
                        onReportReply = { reply, reason ->
                            submitReport(
                                contentType = "reply",
                                contentText = reply.replyText,
                                contentOwnerId = reply.userId,
                                articleId = articleId,
                                reviewId = review.id,
                                replyId = reply.id,
                                reason = reason
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun FirebaseAuth.getDisplayName(): String {
    val currentUser = this.currentUser ?: return "Usuário Anônimo"
    return currentUser.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
        ?: "Usuário Anônimo"
}

@Composable
fun ReplyItem(
    reply: Reply,
    currentUserId: String?,
    isAdmin: Boolean,
    onDelete: () -> Unit,
    onEdit: (newText: String) -> Unit,
    onReport: (reason: String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(reply.replyText) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val isOwner = reply.userId.isNotBlank() && reply.userId == currentUserId
    val canDelete = isAdmin || isOwner

    if (showReportDialog) {
        ReportDialog(
            reportedContent = reply.replyText,
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                onReport(reason)
                showReportDialog = false
            }
        )
    }

    if (isEditing) {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                label = { Text("Editando resposta...") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { isEditing = false }) {
                Icon(
                    painter = painterResource(R.drawable.x_solid),
                    contentDescription = "Cancelar Edição",
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = {
                onEdit(editedText)
                isEditing = false
            }) {
                Icon(
                    painter = painterResource(R.drawable.check_solid),
                    contentDescription = "Salvar Resposta",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reply.userName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(text = reply.replyText, style = MaterialTheme.typography.bodyMedium)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opções da resposta")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (isOwner) {
                        DropdownMenuItem(text = { Text("Editar") }, onClick = { isEditing = true; menuExpanded = false })
                    }
                    if (canDelete) {
                        DropdownMenuItem(text = { Text("Excluir") }, onClick = { onDelete(); menuExpanded = false })
                    }
                    if (!isOwner) {
                        DropdownMenuItem(text = { Text("Denunciar") }, onClick = { showReportDialog = true; menuExpanded = false })
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItem(
    review: Review,
    articleId: String,
    currentUserId: String?,
    isAdmin: Boolean,
    onDelete: () -> Unit,
    onEdit: (newRating: Float, newComment: String) -> Unit,
    onReport: (reason: String) -> Unit,
    onPostReply: (reviewId: String, replyText: String) -> Unit,
    onDeleteReply: (reviewId: String, replyId: String) -> Unit,
    onEditReply: (reviewId: String, replyId: String, newText: String) -> Unit,
    onReportReply: (reply: Reply, reason: String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedRating by remember { mutableFloatStateOf(review.rating) }
    var editedComment by remember { mutableStateOf(review.comment) }
    var menuExpanded by remember { mutableStateOf(false) }

    var replies by remember { mutableStateOf<List<Reply>>(emptyList()) }
    var showReplies by remember { mutableStateOf(false) }
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    val isOwner = review.userId.isNotBlank() && review.userId == currentUserId
    val canDelete = isAdmin || isOwner
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        ReportDialog(
            reportedContent = review.comment,
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                onReport(reason)
                showReportDialog = false
            }
        )
    }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("articles").document(articleId).collection("reviews")
            .document(review.id).collection("replies")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                replies = snapshot?.documents?.mapNotNull { it.toObject<Reply>() } ?: emptyList()
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        if (isEditing) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Editando sua avaliação", style = MaterialTheme.typography.titleMedium)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    StarRatingInput(rating = editedRating, onRatingChange = { editedRating = it })
                }
                OutlinedTextField(
                    value = editedComment,
                    onValueChange = { editedComment = it },
                    label = { Text("Seu comentário") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = { isEditing = false }) { Text("Cancelar") }
                    Button(onClick = {
                        onEdit(editedRating, editedComment)
                        isEditing = false
                    }) { Text("Salvar") }
                }
            }
        } else {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = review.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opções da avaliação")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            if (isOwner) {
                                DropdownMenuItem(text = { Text("Editar") }, onClick = {
                                    isEditing = true
                                    menuExpanded = false
                                })
                            }
                            if (canDelete) {
                                DropdownMenuItem(text = { Text("Excluir", color = MaterialTheme.colorScheme.error) }, onClick = {
                                    onDelete()
                                    menuExpanded = false
                                })
                            }
                            if (!isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Denunciar") },
                                    onClick = {
                                        showReportDialog = true
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                StarRatingDisplay(rating = review.rating, starSize = 16.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = review.comment, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    TextButton(onClick = { showReplyInput = !showReplyInput }) { Text("Responder") }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (replies.isNotEmpty()) {
                        TextButton(onClick = { showReplies = !showReplies }) {
                            Text(if (showReplies) "Ocultar Respostas (${replies.size})" else "Ver Respostas (${replies.size})")
                        }
                    }
                }

                AnimatedVisibility(visible = showReplyInput) {
                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = replyText, onValueChange = { replyText = it }, label = { Text("Sua resposta...") }, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            onPostReply(review.id, replyText)
                            replyText = ""
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar Resposta")
                        }
                    }
                }

                AnimatedVisibility(visible = showReplies) {
                    Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp).fillMaxWidth()) {
                        replies.forEach { reply ->
                            ReplyItem(
                                reply = reply,
                                currentUserId = currentUserId,
                                isAdmin = isAdmin,
                                onDelete = { onDeleteReply(review.id, reply.id) },
                                onEdit = { newText -> onEditReply(review.id, reply.id, newText) },
                                onReport = { reason -> onReportReply(reply, reason) }
                            )
                            Divider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportDialog(
    reportedContent: String,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Denunciar Conteúdo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conteúdo a ser denunciado:", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "\"$reportedContent\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo da denúncia") },
                    placeholder = { Text("Ex: Spam, discurso de ódio...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    onConfirm(reason)
                },
                enabled = reason.isNotBlank() && !isLoading
            ) {
                if(isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        }
    )
}
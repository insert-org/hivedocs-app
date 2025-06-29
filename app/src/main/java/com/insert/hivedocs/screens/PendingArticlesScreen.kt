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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.insert.hivedocs.components.PendingArticleItem
import com.insert.hivedocs.data.Article

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingArticlesScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var pendingArticles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("articles")
            .whereEqualTo("approved", false)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) {
                    Log.w("PendingArticlesScreen", "Listen failed.", e)
                    return@addSnapshotListener
                }
                pendingArticles = snapshot?.documents?.mapNotNull { it.toObject<Article>() } ?: emptyList()
            }
    }

    fun onApprove(articleToApprove: Article) {
        pendingArticles = pendingArticles.filterNot { it.id == articleToApprove.id }
        firestore.collection("articles").document(articleToApprove.id).update("approved", true)
            .addOnSuccessListener { Toast.makeText(context, "Artigo Aprovado!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Log.e("PendingArticlesScreen", "Falha ao aprovar artigo: ${it.message}") }
    }

    fun onReject(articleToReject: Article) {
        pendingArticles = pendingArticles.filterNot { it.id == articleToReject.id }
        firestore.collection("articles").document(articleToReject.id).delete()
            .addOnSuccessListener { Toast.makeText(context, "Artigo Recusado!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Log.e("PendingArticlesScreen", "Falha ao recusar artigo: ${it.message}") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aprovação de Artigos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (pendingArticles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum artigo pendente de aprovação.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(pendingArticles, key = { it.id }) { article ->
                        PendingArticleItem(
                            article = article,
                            onApprove = { onApprove(article) },
                            onReject = { onReject(article) }
                        )
                    }
                }
            }
        }
    }
}
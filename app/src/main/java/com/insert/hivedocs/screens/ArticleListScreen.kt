package com.insert.hivedocs.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.insert.hivedocs.R
import com.insert.hivedocs.components.ArticleListItem
import com.insert.hivedocs.data.Article
import com.insert.hivedocs.navigation.BottomNavItem

@Composable
fun ArticleListScreen(navController: NavController) {
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("articles")
            .whereEqualTo("approved", true)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) {
                    Log.w("ArticleListScreen", "Listen failed.", e)
                    return@addSnapshotListener
                }
                articles = snapshot?.documents?.mapNotNull { it.toObject<Article>() } ?: emptyList()
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Artigos Recentes",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = {
                navController.navigate(BottomNavItem.NewArticle.route)
            }) {
                Icon(
                    painter = painterResource(R.drawable.plus_solid),
                    contentDescription = "Adicionar Novo Artigo",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (articles.isEmpty()) {
            Text("Nenhum artigo publicado encontrado.", modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(articles) { article ->
                    ArticleListItem(article = article) {
                        navController.navigate("article_detail/${article.id}")
                    }
                }
            }
        }
    }
}
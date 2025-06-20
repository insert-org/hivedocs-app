package com.insert.hivedocs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.insert.hivedocs.data.Article
import com.insert.hivedocs.navigation.BottomNavItem
import java.util.Calendar

@Composable
fun NewArticleScreen(navController: NavController) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var resume by remember { mutableStateOf(TextFieldValue("")) }
    var author by remember { mutableStateOf(TextFieldValue("")) }
    var year by remember { mutableStateOf(TextFieldValue(Calendar.getInstance().get(Calendar.YEAR).toString())) }
    var articleUrl by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun submitArticle() {
        if (title.text.isBlank() || resume.text.isBlank() || year.text.isBlank() || author.text.isBlank()) {
            Toast.makeText(context, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        val article = Article(
            title = title.text,
            resume = resume.text,
            author = author.text,
            year = year.text.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR),
            approved = false,
            articleUrl = articleUrl.text
        )

        FirebaseFirestore.getInstance().collection("articles").add(article)
            .addOnSuccessListener {
                isLoading = false
                Toast.makeText(context, "Artigo enviado para aprovação!", Toast.LENGTH_LONG).show()
                navController.navigate(BottomNavItem.ArticleList.route) {
                    popUpTo(BottomNavItem.ArticleList.route) { inclusive = true }
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Erro ao enviar artigo: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enviar Novo Artigo", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Autor") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Ano") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = articleUrl,
            onValueChange = { articleUrl = it },
            label = { Text("Link para o Artigo (URL)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = resume, onValueChange = { resume = it }, label = { Text("Resumo") }, modifier = Modifier.fillMaxWidth().height(150.dp))
        Spacer(Modifier.height(24.dp))
        Button(onClick = { submitArticle() }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Enviar para Aprovação")
        }
    }
}
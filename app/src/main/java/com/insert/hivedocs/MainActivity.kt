package com.insert.hivedocs

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.insert.hivedocs.ui.theme.HiveDocsTheme
import java.util.Calendar

class MainActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HiveDocsTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val auth = FirebaseAuth.getInstance()
    var isAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
    var isAdmin by remember { mutableStateOf<Boolean?>(null) }

    DisposableEffect(auth) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            isAuthenticated = user != null
            if (user != null) {
                checkUserRole(user.uid) { role ->
                    isAdmin = role
                }
            } else {
                isAdmin = null
            }
        }
        auth.addAuthStateListener(authStateListener)
        onDispose { auth.removeAuthStateListener(authStateListener) }
    }

    if (isAuthenticated) {
        if (isAdmin == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text("Verificando permissões...", modifier = Modifier.padding(top = 60.dp))
            }
        } else {
            MainAppScreen(isAdmin = isAdmin!!)
        }
    } else {
        // A tela de Login é chamada aqui quando o usuário não está autenticado
        LoginScreen(onLoginSuccess = {
            isAuthenticated = true
        })
    }
}

fun checkUserRole(uid: String, onResult: (Boolean) -> Unit) {
    Log.d("checkUserRole", "Iniciando verificação para UID: $uid")
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(uid).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                // SUA IDEIA EM AÇÃO: Lendo o campo booleano diretamente!
                // O '?: false' garante que se o campo não existir por algum motivo, ele retorna false.
                val isAdminResult = document.getBoolean("isAdmin") ?: false

                Log.d("checkUserRole", "SUCESSO na leitura direta! Valor de 'isAdmin': $isAdminResult")
                onResult(isAdminResult)

            } else {
                Log.w("checkUserRole", "Documento não encontrado para o UID: $uid")
                onResult(false)
            }
        }
        .addOnFailureListener { exception ->
            Log.e("checkUserRole", "FALHA AO BUSCAR DOCUMENTO!", exception)
            onResult(false)
        }
}

// --- TELA DE LOGIN ---
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun handleEmailLogin() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Preencha e-mail e senha.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Toast.makeText(context, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    val error = (task.exception as? FirebaseAuthException)?.errorCode
                    val message = when (error) {
                        "ERROR_WRONG_PASSWORD" -> "Senha incorreta."
                        "ERROR_USER_NOT_FOUND" -> "Usuário não encontrado."
                        "ERROR_INVALID_EMAIL" -> "Formato de e-mail inválido."
                        else -> "Falha no login."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun handleEmailSignUp() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Preencha e-mail e senha para registrar.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // SUCESSO! Agora crie o documento no Firestore.
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        val newUserProfile = UserProfile(isAdmin = false) // Novos usuários nunca são admins
                        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.uid)
                            .set(newUserProfile)
                            .addOnSuccessListener {
                                // Tudo pronto, pode prosseguir
                                isLoading = false
                                Toast.makeText(context, "Registro bem-sucedido!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Log.e("SignUp", "Erro ao criar perfil de usuário.", e)
                                Toast.makeText(context, "Erro ao finalizar registro.", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // Falha no registro
                    isLoading = false
                    val error = (task.exception as? FirebaseAuthException)?.errorCode
                    val message = when (error) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este e-mail já está em uso."
                        "ERROR_WEAK_PASSWORD" -> "A senha deve ter no mínimo 6 caracteres."
                        "ERROR_INVALID_EMAIL" -> "Formato de e-mail inválido."
                        else -> "Falha no registro."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isLoading = true
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                isLoading = false
                if (authTask.isSuccessful) {
                    onLoginSuccess()
                } else {
                    Toast.makeText(context, "Erro ao autenticar com Google.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            isLoading = false
            Toast.makeText(context, "Login com Google cancelado ou falhou.", Toast.LENGTH_SHORT).show()
            Log.e("LoginScreen", "Google sign in failed", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bem-vindo ao HiveDocs", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { handleEmailLogin() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Entrar")
            }
            Button(
                onClick = { handleEmailSignUp() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Registrar")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("OU", modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Button(
            onClick = {
                isLoading = true
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                launcher.launch(googleSignInClient.signInIntent)
            },
            enabled = !isLoading
        ) {
            Text("Login com Google")
        }
    }
}


// --- MODELOS DE DADOS E NAVEGAÇÃO ---

data class Article(
    @DocumentId val id: String = "",
    val title: String = "",
    val resume: String = "",
    val author: String = "",
    val approved: Boolean = false,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    @Suppress("unused")
    constructor() : this("", "", "", "", false, 2024)
}

    data class UserProfile(
        val isAdmin: Boolean = false
    )

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object ArticleList : BottomNavItem("article_list", Icons.Default.List, "Artigos")
    object NewArticle : BottomNavItem("new_article", Icons.Default.Add, "Novo Artigo")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Perfil")
    object Approval : BottomNavItem("approval", Icons.Default.Check, "Aprovações")
}


// --- TELAS PRINCIPAIS DO APP ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(isAdmin: Boolean) {
    val navController = rememberNavController()

    val navItems = if (isAdmin) {
        listOf(BottomNavItem.ArticleList, BottomNavItem.Approval, BottomNavItem.Profile)
    } else {
        listOf(BottomNavItem.ArticleList, BottomNavItem.NewArticle, BottomNavItem.Profile)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.secondary,
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.ArticleList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.ArticleList.route) {
                ArticleListScreen(navController = navController)
            }
            composable(BottomNavItem.NewArticle.route) {
                NewArticleScreen(navController = navController)
            }
            composable(BottomNavItem.Approval.route) {
                ApprovalScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(auth = FirebaseAuth.getInstance(), isAdmin = isAdmin)
            }
            composable("article_detail/{articleId}") { backStackEntry ->
                val articleId = backStackEntry.arguments?.getString("articleId")
                ArticleDetailLoader(articleId = articleId, navController = navController)
            }
        }
    }
}


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
        Text(text = "Artigos Recentes", style = MaterialTheme.typography.headlineMedium)
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


@Composable
fun ApprovalScreen() {
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
                    Log.w("ApprovalScreen", "Listen failed.", e)
                    return@addSnapshotListener
                }
                // O listener ainda é útil para carregar a lista inicial e receber
                // atualizações externas, se houver.
                pendingArticles = snapshot?.documents?.mapNotNull { it.toObject<Article>() } ?: emptyList()
            }
    }

    // --- FUNÇÕES ATUALIZADAS ---
    fun onApprove(articleToApprove: Article) {
        // 1. ATUALIZAÇÃO OTIMISTA: Remove o item da lista local imediatamente.
        pendingArticles = pendingArticles.filterNot { it.id == articleToApprove.id }

        // 2. Ação no Firestore continua em segundo plano.
        firestore.collection("articles").document(articleToApprove.id).update("approved", true)
            .addOnSuccessListener { Toast.makeText(context, "Artigo Aprovado!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener {
                // Em caso de falha, seria ideal adicionar o item de volta à lista,
                // mas para simplificar, vamos apenas logar o erro.
                Log.e("ApprovalScreen", "Falha ao aprovar artigo: ${it.message}")
            }
    }

    fun onReject(articleToReject: Article) {
        // 1. ATUALIZAÇÃO OTIMISTA: Remove o item da lista local imediatamente.
        pendingArticles = pendingArticles.filterNot { it.id == articleToReject.id }

        // 2. Ação no Firestore continua em segundo plano.
        firestore.collection("articles").document(articleToReject.id).delete()
            .addOnSuccessListener { Toast.makeText(context, "Artigo Recusado!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener {
                Log.e("ApprovalScreen", "Falha ao recusar artigo: ${it.message}")
            }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(text = "Artigos Pendentes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (pendingArticles.isEmpty()) {
            Text("Nenhum artigo pendente de aprovação.", modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    items = pendingArticles,
                    key = { article -> article.id } // Adicionar uma chave melhora a performance
                ) { article ->
                    PendingArticleItem(
                        article = article,
                        // --- CHAMADAS ATUALIZADAS ---
                        onApprove = { onApprove(article) },
                        onReject = { onReject(article) }
                    )
                }
            }
        }
    }
}

@Composable
fun NewArticleScreen(navController: NavController) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var resume by remember { mutableStateOf(TextFieldValue("")) }
    var author by remember { mutableStateOf(TextFieldValue("")) }
    var year by remember { mutableStateOf(TextFieldValue(Calendar.getInstance().get(Calendar.YEAR).toString())) }
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
            approved = false
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Enviar Novo Artigo", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Autor") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = resume, onValueChange = { resume = it }, label = { Text("Resumo") }, modifier = Modifier.fillMaxWidth().height(150.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Ano") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Button(onClick = { submitArticle() }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Enviar para Aprovação")
        }
    }
}

@Composable
fun ProfileScreen(auth: FirebaseAuth, isAdmin: Boolean) {
    val currentUser = auth.currentUser
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (currentUser != null) {
            Text("Logado como:", style = MaterialTheme.typography.bodyLarge)
            Text(currentUser.email ?: "Email não disponível", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Função: ${if (isAdmin) "Administrador" else "Usuário"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(onClick = { auth.signOut() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Sair")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sair da Conta")
        }
    }
}

@Composable
fun ArticleListItem(article: Article, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = article.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Por: ${article.author} (${article.year})", style = MaterialTheme.typography.bodyMedium)
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
            Text(text = article.resume, style = MaterialTheme.typography.bodySmall)
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


@Composable
fun ArticleDetailLoader(articleId: String?, navController: NavController) {
    var article by remember { mutableStateOf<Article?>(null) }
    LaunchedEffect(articleId) {
        if (articleId != null) {
            FirebaseFirestore.getInstance().collection("articles").document(articleId).get()
                .addOnSuccessListener { document -> article = document.toObject<Article>() }
                .addOnFailureListener { Log.e("ArticleDetail", "Erro ao buscar artigo: ${it.message}") }
        }
    }
    if (article != null) {
        ArticleDetailScreen(article = article!!, navController = navController)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(article: Article, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Text(text = article.title, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Por: ${article.author} (${article.year})", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Resumo", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = article.resume, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
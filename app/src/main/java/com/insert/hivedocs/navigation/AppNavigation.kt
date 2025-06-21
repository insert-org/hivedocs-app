package com.insert.hivedocs.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.insert.hivedocs.R
import com.insert.hivedocs.screens.*
import com.insert.hivedocs.util.checkUserRole


sealed class BottomNavItem(val route: String, @DrawableRes val iconResId: Int, val label: String) {
    object ArticleList : BottomNavItem("article_list", R.drawable.list_ul_solid, "Artigos")
    object NewArticle : BottomNavItem("new_article", R.drawable.plus_solid, "Novo Artigo")
    object Profile : BottomNavItem("profile", R.drawable.user_solid, "Perfil")
    object Approval : BottomNavItem("approval", R.drawable.check_solid, "Aprovações")
    object Chatbot : BottomNavItem("chatbot", R.drawable.robot_solid, "Chatbot")
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
        LoginScreen(onLoginSuccess = {
            isAuthenticated = true
        })
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(isAdmin: Boolean) {
    val navController = rememberNavController()

    val navItems = if (isAdmin) {
        listOf(BottomNavItem.ArticleList, BottomNavItem.Approval, BottomNavItem.Chatbot, BottomNavItem.Profile)
    } else {
        listOf(BottomNavItem.ArticleList, BottomNavItem.NewArticle, BottomNavItem.Chatbot, BottomNavItem.Profile)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.iconResId),
                                contentDescription = screen.label,
                                modifier = Modifier.size(18.dp)
                            )
                        },
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
                if (articleId != null) {
                    ArticleDetailScreen(
                        articleId = articleId,
                        navController = navController,
                        isAdmin = isAdmin
                    )
                }
            }
            composable(BottomNavItem.Chatbot.route) {
                ChatbotScreen()
            }
        }
    }
}
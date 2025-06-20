package com.insert.hivedocs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.insert.hivedocs.navigation.AppNavigator // <-- Importante: Importa o AppNavigator do novo pacote
import com.insert.hivedocs.ui.theme.HiveDocsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HiveDocsTheme {
                // A MainActivity agora só precisa chamar o AppNavigator.
                // Ele cuidará de toda a lógica de mostrar a tela de login ou a tela principal.
                AppNavigator()
            }
        }
    }
}
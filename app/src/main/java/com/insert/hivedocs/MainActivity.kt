package com.insert.hivedocs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.insert.hivedocs.navigation.AppNavigator
import com.insert.hivedocs.ui.theme.HiveDocsTheme

class MainActivity : ComponentActivity() {
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
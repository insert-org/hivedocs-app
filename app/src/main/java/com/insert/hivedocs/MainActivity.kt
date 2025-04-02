package com.insert.hivedocs

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val myWebView = WebView(this)
        setContentView(myWebView)

        myWebView.settings.javaScriptEnabled = true
        myWebView.loadUrl("https://hivedocs.vercel.app/")
    }
}
package com.trinhquyetchien.markdowntray

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.screens.MainScreen
import com.trinhquyetchien.markdowntray.ui.theme.MarkdownTrayTheme

class MainActivity : ComponentActivity() {
    private lateinit var storageManager: StorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        storageManager = StorageManager(applicationContext)

        setContent {
            MarkdownTrayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(storageManager = storageManager)
                }
            }
        }
    }
}

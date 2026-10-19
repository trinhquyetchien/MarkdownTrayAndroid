package com.trinhquyetchien.markdowntray.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.trinhquyetchien.markdowntray.data.StorageManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlineScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var rawContent by remember { mutableStateOf("") }
    var currentFile by remember { mutableStateOf(storageManager.getDeadlineFile()) }

    fun loadContent() {
        currentFile = storageManager.getDeadlineFile()
        if (currentFile.exists()) {
            rawContent = storageManager.readText(currentFile)
        } else {
            rawContent = ""
        }
    }

    LaunchedEffect(storageManager.deadlineFilePath) {
        loadContent()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = storageManager.deadlineAlias,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                IconButton(onClick = {
                    loadContent()
                    Toast.makeText(context, "Deadline note refreshed", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        MarkdownViewer(
            title = storageManager.deadlineAlias,
            rawContent = rawContent,
            onToggleTask = { lineIndex, isChecked ->
                val success = storageManager.toggleTask(currentFile, lineIndex, isChecked)
                if (success) {
                    loadContent()
                } else {
                    Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show()
                }
            },
            onAddTask = { taskText ->
                val success = storageManager.appendTask(currentFile, taskText)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Deadline task added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to add task", Toast.LENGTH_SHORT).show()
                }
            },
            onSaveRawContent = { newContent ->
                val success = storageManager.writeText(currentFile, newContent)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Deadline note saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save note", Toast.LENGTH_SHORT).show()
                }
            },
            onRefresh = {
                loadContent()
                Toast.makeText(context, "Refreshed", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f)
        )
    }
}

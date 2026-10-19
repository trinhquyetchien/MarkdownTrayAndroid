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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
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
    var syncInfo by remember { mutableStateOf<SyncStatusInfo?>(null) }

    fun loadContent() {
        currentFile = storageManager.getDeadlineFile()
        if (!currentFile.exists() || currentFile.length() == 0L) {
            currentFile = storageManager.ensureDeadlineFileExists()
        }

        rawContent = if (currentFile.exists()) {
            storageManager.readText(currentFile)
        } else {
            ""
        }
        syncInfo = storageManager.getFileSyncInfo(currentFile)
    }

    LaunchedEffect(storageManager.deadlineFilePath) {
        loadContent()
    }

    // Auto-reload via DirectoryObserver when Syncthing syncs changes to Deadline.md
    DisposableEffect(storageManager.deadlineFilePath) {
        val parentDir = currentFile.parentFile ?: File(storageManager.dailyNotesDir)
        val observer = storageManager.createDirectoryObserver(parentDir) { changedPath ->
            if (changedPath == null || changedPath == currentFile.name) {
                loadContent()
            }
        }
        observer.start()
        onDispose {
            observer.stop()
        }
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
                    Toast.makeText(context, "Đã làm mới Deadline", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Làm mới",
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
            syncInfo = syncInfo,
            onToggleTask = { lineIndex, isChecked ->
                val success = storageManager.toggleTask(currentFile, lineIndex, isChecked)
                if (success) {
                    loadContent()
                } else {
                    Toast.makeText(context, "Không thể cập nhật task", Toast.LENGTH_SHORT).show()
                }
            },
            onAddTask = { taskText ->
                val success = storageManager.appendTask(currentFile, taskText)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Đã thêm deadline", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi thêm task", Toast.LENGTH_SHORT).show()
                }
            },
            onSaveRawContent = { newContent ->
                val success = storageManager.writeText(currentFile, newContent)
                if (success) {
                    loadContent()
                    Toast.makeText(context, "Đã lưu Deadline", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi lưu file", Toast.LENGTH_SHORT).show()
                }
            },
            onRefresh = {
                loadContent()
                Toast.makeText(context, "Đã kiểm tra đồng bộ", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f)
        )
    }
}
